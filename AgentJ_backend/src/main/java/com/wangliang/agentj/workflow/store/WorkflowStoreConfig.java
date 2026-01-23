package com.wangliang.agentj.workflow.store;

import com.alibaba.cloud.ai.graph.store.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流长期内存（Store）配置
 *
 * 根据官方文档实现跨会话的长期数据存储：
 * - MemoryStore: 内存存储（默认）
 * - RedisStore: Redis持久化
 * - PostgreSqlStore: PostgreSQL持久化
 *
 * 使用场景：
 * - 用户偏好存储
 * - 跨会话缓存
 * - 用户档案数据
 *
 * @author AgentJ
 * @see <a href="https://java2ai.com/en/docs/frameworks/graph-core/core/memory">官方文档</a>
 */
@Slf4j
@Configuration
public class WorkflowStoreConfig {

    /**
     * 默认内存Store实现
     */
    @Bean
    public Store workflowMemoryStore() {
        log.info("Initializing default WorkflowStore with MemoryStore");
        return new MemoryWorkflowStore();
    }

    /**
     * Redis Store配置
     */
    @Configuration
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnProperty(name = "agentj.workflow.store.type", havingValue = "redis")
    public static class RedisStoreConfiguration {

        @Bean
        public RedisTemplate<String, Object> storeRedisTemplate(
                RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        public Store workflowRedisStore(RedisTemplate<String, Object> redisTemplate) {
            log.info("Initializing Redis WorkflowStore");
            return new RedisWorkflowStore(redisTemplate);
        }
    }

    /**
     * PostgreSQL Store配置
     */
    @Configuration
    @ConditionalOnClass(DataSource.class)
    @ConditionalOnProperty(name = "agentj.workflow.store.type", havingValue = "postgresql")
    public static class PostgreSqlStoreConfiguration {

        @Bean
        public Store workflowPostgreSqlStore(DataSource dataSource) {
            log.info("Initializing PostgreSQL WorkflowStore");
            initStoreTable(dataSource);
            return new PostgreSqlWorkflowStore(dataSource);
        }

        private void initStoreTable(DataSource dataSource) {
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getTables(null, null, "workflow_store", null)) {
                    if (!rs.next()) {
                        String sql = """
                            CREATE TABLE workflow_store (
                                id BIGSERIAL PRIMARY KEY,
                                namespace VARCHAR(255) NOT NULL,
                                key VARCHAR(255) NOT NULL,
                                value BYTEA,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE (namespace, key)
                            )
                            """;
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql);
                        }
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute("CREATE INDEX idx_store_namespace ON workflow_store(namespace)");
                        }
                        log.info("Created workflow_store table");
                    }
                }
            } catch (SQLException e) {
                log.error("Failed to initialize store table", e);
            }
        }
    }

    /**
     * 内存Store实现
     */
    public static class MemoryWorkflowStore implements Store {
        protected final Map<NamespaceKey, StoreItem> store = new ConcurrentHashMap<>();
        protected final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public Optional<StoreItem> getItem(List<String> namespace, String key) {
            NamespaceKey nsKey = new NamespaceKey(namespace, key);
            return Optional.ofNullable(store.get(nsKey));
        }

        @Override
        public void putItem(StoreItem item) {
            List<String> namespace = item.getNamespace();
            String key = item.getKey();
            NamespaceKey nsKey = new NamespaceKey(namespace, key);
            store.put(nsKey, item);
            log.debug("Stored item in memory: namespace={}, key={}", namespace, key);
        }

        @Override
        public boolean deleteItem(List<String> namespace, String key) {
            NamespaceKey nsKey = new NamespaceKey(namespace, key);
            StoreItem removed = store.remove(nsKey);
            log.debug("Deleted item from memory: namespace={}, key={}, removed={}", namespace, key, removed != null);
            return removed != null;
        }

        @Override
        public StoreSearchResult searchItems(StoreSearchRequest searchRequest) {
            List<StoreItem> results = new ArrayList<>();
            List<String> searchNamespace = searchRequest.getNamespace();

            for (Map.Entry<NamespaceKey, StoreItem> entry : store.entrySet()) {
                StoreItem item = entry.getValue();
                List<String> itemNamespace = item.getNamespace();

                // Filter by namespace
                if (!searchNamespace.isEmpty() && !namespaceMatches(itemNamespace, searchNamespace)) {
                    continue;
                }

                // Filter by query text
                String query = searchRequest.getQuery();
                if (query != null && !query.isEmpty()) {
                    String key = item.getKey();
                    Map<String, Object> value = item.getValue();
                    String valueStr = value != null ? value.toString() : "";
                    if (!key.contains(query) && !valueStr.contains(query)) {
                        continue;
                    }
                }

                // Filter by custom filters
                Map<String, Object> filters = searchRequest.getFilter();
                if (!filters.isEmpty()) {
                    Map<String, Object> itemValue = item.getValue();
                    boolean match = true;
                    for (Map.Entry<String, Object> filter : filters.entrySet()) {
                        Object itemFieldValue = itemValue.get(filter.getKey());
                        if (!Objects.equals(itemFieldValue, filter.getValue())) {
                            match = false;
                            break;
                        }
                    }
                    if (!match) {
                        continue;
                    }
                }

                results.add(item);
            }

            // Apply pagination
            int offset = searchRequest.getOffset();
            int limit = searchRequest.getLimit();
            int total = results.size();
            List<StoreItem> pagedResults = results.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();

            return new StoreSearchResult(pagedResults, total, offset, limit);
        }

        @Override
        public List<String> listNamespaces(NamespaceListRequest namespaceRequest) {
            Set<String> namespaces = new HashSet<>();
            List<String> prefix = namespaceRequest.getNamespace();

            for (NamespaceKey nsKey : store.keySet()) {
                List<String> itemNamespace = nsKey.namespace;
                if (prefix.isEmpty() || namespaceMatches(itemNamespace, prefix)) {
                    // Add namespace at appropriate depth
                    int depth = namespaceRequest.getMaxDepth();
                    if (depth < 0 || itemNamespace.size() <= prefix.size() + depth) {
                        namespaces.add(String.join(":", itemNamespace));
                    }
                }
            }

            List<String> result = new ArrayList<>(namespaces);
            // Apply pagination
            int offset = namespaceRequest.getOffset();
            int limit = namespaceRequest.getLimit();
            if (offset >= result.size()) {
                return Collections.emptyList();
            }
            return result.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public void clear() {
            store.clear();
        }

        @Override
        public long size() {
            return store.size();
        }

        @Override
        public boolean isEmpty() {
            return store.isEmpty();
        }

        private boolean namespaceMatches(List<String> itemNamespace, List<String> prefix) {
            if (itemNamespace.size() < prefix.size()) {
                return false;
            }
            for (int i = 0; i < prefix.size(); i++) {
                if (!itemNamespace.get(i).equals(prefix.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Data
        protected static class NamespaceKey {
            private final List<String> namespace;
            private final String key;

            public NamespaceKey(List<String> namespace, String key) {
                this.namespace = new ArrayList<>(namespace);
                this.key = key;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                NamespaceKey that = (NamespaceKey) o;
                return Objects.equals(namespace, that.namespace) && Objects.equals(key, that.key);
            }

            @Override
            public int hashCode() {
                return Objects.hash(namespace, key);
            }
        }
    }

    /**
     * Redis Store实现
     */
    public static class RedisWorkflowStore implements Store {
        private final RedisTemplate<String, Object> redisTemplate;
        private static final String STORE_PREFIX = "workflow:store:";

        public RedisWorkflowStore(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @Override
        public Optional<StoreItem> getItem(List<String> namespace, String key) {
            String redisKey = buildKey(namespace, key);
            Object data = redisTemplate.opsForValue().get(redisKey);

            if (data instanceof StoreItem) {
                return Optional.of((StoreItem) data);
            }
            return Optional.empty();
        }

        @Override
        public void putItem(StoreItem item) {
            List<String> namespace = item.getNamespace();
            String key = item.getKey();
            String redisKey = buildKey(namespace, key);
            redisTemplate.opsForValue().set(redisKey, item, Duration.ofDays(1));
            log.debug("Stored item in Redis: namespace={}, key={}", namespace, key);
        }

        @Override
        public boolean deleteItem(List<String> namespace, String key) {
            String redisKey = buildKey(namespace, key);
            Boolean deleted = redisTemplate.delete(redisKey);
            log.debug("Deleted item from Redis: namespace={}, key={}, deleted={}", namespace, key, deleted);
            return Boolean.TRUE.equals(deleted);
        }

        @Override
        public StoreSearchResult searchItems(StoreSearchRequest searchRequest) {
            String pattern = STORE_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return new StoreSearchResult(Collections.emptyList(), 0, 0, 100);
            }

            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            List<StoreItem> allItems = new ArrayList<>();

            for (Object value : values) {
                if (value instanceof StoreItem) {
                    allItems.add((StoreItem) value);
                }
            }

            // Apply filters (simplified version)
            List<StoreItem> results = filterItems(allItems, searchRequest);

            int offset = searchRequest.getOffset();
            int limit = searchRequest.getLimit();
            int total = results.size();
            List<StoreItem> pagedResults = results.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();

            return new StoreSearchResult(pagedResults, total, offset, limit);
        }

        @Override
        public List<String> listNamespaces(NamespaceListRequest namespaceRequest) {
            Set<String> namespaces = new HashSet<>();
            Set<String> keys = redisTemplate.keys(STORE_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }

            for (String key : keys) {
                // Extract namespace from key
                String namespacePart = key.substring(STORE_PREFIX.length());
                String[] parts = namespacePart.split(":");
                List<String> namespace = Arrays.asList(parts);
                namespaces.add(String.join(":", namespace));
            }

            List<String> result = new ArrayList<>(namespaces);
            int offset = namespaceRequest.getOffset();
            int limit = namespaceRequest.getLimit();

            if (offset >= result.size()) {
                return Collections.emptyList();
            }
            return result.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public void clear() {
            Set<String> keys = redisTemplate.keys(STORE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

        @Override
        public long size() {
            Set<String> keys = redisTemplate.keys(STORE_PREFIX + "*");
            return keys == null ? 0 : keys.size();
        }

        @Override
        public boolean isEmpty() {
            Set<String> keys = redisTemplate.keys(STORE_PREFIX + "*");
            return keys == null || keys.isEmpty();
        }

        private String buildKey(List<String> namespace, String key) {
            return STORE_PREFIX + String.join(":", namespace) + ":" + key;
        }

        private List<StoreItem> filterItems(List<StoreItem> items, StoreSearchRequest request) {
            List<StoreItem> results = new ArrayList<>();
            List<String> searchNamespace = request.getNamespace();

            for (StoreItem item : items) {
                List<String> itemNamespace = item.getNamespace();

                if (!searchNamespace.isEmpty() && !namespaceMatches(itemNamespace, searchNamespace)) {
                    continue;
                }

                String query = request.getQuery();
                if (query != null && !query.isEmpty()) {
                    String key = item.getKey();
                    Map<String, Object> value = item.getValue();
                    String valueStr = value != null ? value.toString() : "";
                    if (!key.contains(query) && !valueStr.contains(query)) {
                        continue;
                    }
                }

                results.add(item);
            }
            return results;
        }

        private boolean namespaceMatches(List<String> itemNamespace, List<String> prefix) {
            if (itemNamespace.size() < prefix.size()) {
                return false;
            }
            for (int i = 0; i < prefix.size(); i++) {
                if (!itemNamespace.get(i).equals(prefix.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * PostgreSQL Store实现
     */
    public static class PostgreSqlWorkflowStore implements Store {
        private final DataSource dataSource;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public PostgreSqlWorkflowStore(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Optional<StoreItem> getItem(List<String> namespace, String key) {
            String sql = "SELECT value FROM workflow_store WHERE namespace = ? AND key = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, String.join(":", namespace));
                ps.setString(2, key);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        byte[] data = rs.getBytes("value");
                        return Optional.of(objectMapper.readValue(data, StoreItem.class));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to get item from PostgreSQL store", e);
            }
            return Optional.empty();
        }

        @Override
        public void putItem(StoreItem item) {
            List<String> namespace = item.getNamespace();
            String key = item.getKey();
            String sql = """
                INSERT INTO workflow_store (namespace, key, value)
                VALUES (?, ?, ?)
                ON CONFLICT (namespace, key) DO UPDATE SET
                    value = EXCLUDED.value,
                    updated_at = CURRENT_TIMESTAMP
                """;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, String.join(":", namespace));
                ps.setString(2, key);
                ps.setBytes(3, objectMapper.writeValueAsBytes(item));
                ps.executeUpdate();
                log.debug("Stored item in PostgreSQL: namespace={}, key={}", namespace, key);
            } catch (Exception e) {
                log.error("Failed to put item to PostgreSQL store", e);
            }
        }

        @Override
        public boolean deleteItem(List<String> namespace, String key) {
            String sql = "DELETE FROM workflow_store WHERE namespace = ? AND key = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, String.join(":", namespace));
                ps.setString(2, key);
                int rows = ps.executeUpdate();
                log.debug("Deleted item from PostgreSQL: namespace={}, key={}, rows={}", namespace, key, rows);
                return rows > 0;
            } catch (Exception e) {
                log.error("Failed to delete from PostgreSQL store", e);
            }
            return false;
        }

        @Override
        public StoreSearchResult searchItems(StoreSearchRequest searchRequest) {
            String sql = "SELECT value FROM workflow_store";
            List<StoreItem> results = new ArrayList<>();

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    byte[] data = rs.getBytes("value");
                    results.add(objectMapper.readValue(data, StoreItem.class));
                }
            } catch (Exception e) {
                log.error("Failed to search PostgreSQL store", e);
            }

            // Apply filters (simplified)
            List<StoreItem> filtered = filterItems(results, searchRequest);

            int offset = searchRequest.getOffset();
            int limit = searchRequest.getLimit();
            int total = filtered.size();
            List<StoreItem> pagedResults = filtered.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();

            return new StoreSearchResult(pagedResults, total, offset, limit);
        }

        @Override
        public List<String> listNamespaces(NamespaceListRequest namespaceRequest) {
            String sql = "SELECT DISTINCT namespace FROM workflow_store";
            List<String> namespaces = new ArrayList<>();

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    namespaces.add(rs.getString("namespace"));
                }
            } catch (Exception e) {
                log.error("Failed to list namespaces from PostgreSQL store", e);
            }

            int offset = namespaceRequest.getOffset();
            int limit = namespaceRequest.getLimit();

            if (offset >= namespaces.size()) {
                return Collections.emptyList();
            }
            return namespaces.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public void clear() {
            String sql = "DELETE FROM workflow_store";

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (Exception e) {
                log.error("Failed to clear PostgreSQL store", e);
            }
        }

        @Override
        public long size() {
            String sql = "SELECT COUNT(*) FROM workflow_store";

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            } catch (Exception e) {
                log.error("Failed to get PostgreSQL store size", e);
            }
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        private List<StoreItem> filterItems(List<StoreItem> items, StoreSearchRequest request) {
            List<StoreItem> results = new ArrayList<>();
            List<String> searchNamespace = request.getNamespace();

            for (StoreItem item : items) {
                List<String> itemNamespace = item.getNamespace();

                if (!searchNamespace.isEmpty() && !namespaceMatches(itemNamespace, searchNamespace)) {
                    continue;
                }

                results.add(item);
            }
            return results;
        }

        private boolean namespaceMatches(List<String> itemNamespace, List<String> prefix) {
            if (itemNamespace.size() < prefix.size()) {
                return false;
            }
            for (int i = 0; i < prefix.size(); i++) {
                if (!itemNamespace.get(i).equals(prefix.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Store工具类，简化Store操作
     */
    public static class StoreHelper {
        private final Store store;

        public StoreHelper(Store store) {
            this.store = store;
        }

        /**
         * 存储用户偏好
         */
        public void saveUserPreferences(String userId, Map<String, Object> preferences) {
            StoreItem item = StoreItem.of(
                    List.of("user", "preferences"),
                    userId,
                    preferences
            );
            store.putItem(item);
        }

        /**
         * 获取用户偏好
         */
        public Optional<Map<String, Object>> getUserPreferences(String userId) {
            Optional<StoreItem> item = store.getItem(List.of("user", "preferences"), userId);
            return item.map(StoreItem::getValue);
        }

        /**
         * 存储用户档案
         */
        public void saveUserProfile(String userId, Map<String, Object> profile) {
            StoreItem item = StoreItem.of(
                    List.of("user", "profile"),
                    userId,
                    profile
            );
            store.putItem(item);
        }

        /**
         * 获取用户档案
         */
        public Optional<Map<String, Object>> getUserProfile(String userId) {
            Optional<StoreItem> item = store.getItem(List.of("user", "profile"), userId);
            return item.map(StoreItem::getValue);
        }

        /**
         * 存储缓存数据
         */
        public void putCache(String cacheKey, Object value) {
            // Value must be a Map<String, Object> for StoreItem
            @SuppressWarnings("unchecked")
            Map<String, Object> valueMap = value instanceof Map
                ? (Map<String, Object>) value
                : Map.of("data", value);

            StoreItem item = StoreItem.of(
                    List.of("cache"),
                    cacheKey,
                    valueMap
            );
            store.putItem(item);
        }

        /**
         * 获取缓存数据
         */
        public Optional<Map<String, Object>> getCache(String cacheKey) {
            Optional<StoreItem> item = store.getItem(List.of("cache"), cacheKey);
            return item.map(StoreItem::getValue);
        }
    }
}
