package com.wangliang.agentj.workflow.store;

import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
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
 * @see <a href="https://java2ai.com/docs/frameworks/graph-core/core/memory">官方文档</a>
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
                        // 创建索引
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
            NamespaceKey nsKey = new NamespaceKey(item.namespace(), item.key());
            store.put(nsKey, item);
            log.debug("Stored item in memory: namespace={}, key={}", item.namespace(), item.key());
        }

        @Override
        public List<StoreItem> search(List<String> namespace) {
            List<StoreItem> results = new ArrayList<>();
            String namespacePrefix = namespacePrefix(namespace);

            for (Map.Entry<NamespaceKey, StoreItem> entry : store.entrySet()) {
                if (entry.getKey().toString().startsWith(namespacePrefix)) {
                    results.add(entry.getValue());
                }
            }
            return results;
        }

        @Override
        public void delete(List<String> namespace, String key) {
            NamespaceKey nsKey = new NamespaceKey(namespace, key);
            store.remove(nsKey);
            log.debug("Deleted item from memory: namespace={}, key={}", namespace, key);
        }

        @Override
        public void clear() {
            store.clear();
        }

        protected String namespacePrefix(List<String> namespace) {
            return String.join(":", namespace) + ":";
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
            public String toString() {
                return namespacePrefix() + key;
            }

            private String namespacePrefix() {
                return String.join(":", namespace) + ":";
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
        @Value("${agentj.workflow.store.ttl:86400}")
        private int ttlSeconds; // 默认24小时

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
            String redisKey = buildKey(item.namespace(), item.key());
            redisTemplate.opsForValue().set(redisKey, item, Duration.ofSeconds(ttlSeconds));
            log.debug("Stored item in Redis: namespace={}, key={}, ttl={}s", item.namespace(), item.key(), ttlSeconds);
        }

        @Override
        public List<StoreItem> search(List<String> namespace) {
            String pattern = STORE_PREFIX + String.join(":", namespace) + ":*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }

            List<Object> values = redisTemplate.opsForValue().multi(new ArrayList<>(keys));
            List<StoreItem> results = new ArrayList<>();

            for (Object value : values) {
                if (value instanceof StoreItem) {
                    results.add((StoreItem) value);
                }
            }
            return results;
        }

        @Override
        public void delete(List<String> namespace, String key) {
            String redisKey = buildKey(namespace, key);
            redisTemplate.delete(redisKey);
            log.debug("Deleted item from Redis: namespace={}, key={}", namespace, key);
        }

        @Override
        public void clear() {
            Set<String> keys = redisTemplate.keys(STORE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

        private String buildKey(List<String> namespace, String key) {
            return STORE_PREFIX + String.join(":", namespace) + ":" + key;
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
            String sql = """
                INSERT INTO workflow_store (namespace, key, value)
                VALUES (?, ?, ?)
                ON CONFLICT (namespace, key) DO UPDATE SET
                    value = EXCLUDED.value,
                    updated_at = CURRENT_TIMESTAMP
                """;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, String.join(":", item.namespace()));
                ps.setString(2, item.key());
                ps.setBytes(3, objectMapper.writeValueAsBytes(item));
                ps.executeUpdate();
                log.debug("Stored item in PostgreSQL: namespace={}, key={}", item.namespace(), item.key());
            } catch (Exception e) {
                log.error("Failed to put item to PostgreSQL store", e);
            }
        }

        @Override
        public List<StoreItem> search(List<String> namespace) {
            String sql = "SELECT value FROM workflow_store WHERE namespace = ?";
            List<StoreItem> results = new ArrayList<>();

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, String.join(":", namespace));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        byte[] data = rs.getBytes("value");
                        results.add(objectMapper.readValue(data, StoreItem.class));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to search PostgreSQL store", e);
            }
            return results;
        }

        @Override
        public void delete(List<String> namespace, String key) {
            String sql = "DELETE FROM workflow_store WHERE namespace = ? AND key = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, String.join(":", namespace));
                ps.setString(2, key);
                ps.executeUpdate();
                log.debug("Deleted item from PostgreSQL: namespace={}, key={}", namespace, key);
            } catch (Exception e) {
                log.error("Failed to delete from PostgreSQL store", e);
            }
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
            return item.map(StoreItem::getValue).map(value -> (Map<String, Object>) value);
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
            return item.map(StoreItem::getValue).map(value -> (Map<String, Object>) value);
        }

        /**
         * 存储缓存数据
         */
        public void putCache(String cacheKey, Object value) {
            StoreItem item = StoreItem.of(
                    List.of("cache"),
                    cacheKey,
                    value
            );
            store.putItem(item);
        }

        /**
         * 获取缓存数据
         */
        public Optional<Object> getCache(String cacheKey) {
            Optional<StoreItem> item = store.getItem(List.of("cache"), cacheKey);
            return item.map(StoreItem::getValue);
        }
    }
}
