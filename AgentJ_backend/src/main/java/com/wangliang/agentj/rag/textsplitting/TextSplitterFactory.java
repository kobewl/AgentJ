package com.wangliang.agentj.rag.textsplitting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 文本分割器工厂类
 * 负责创建和管理各种文本分割器实例
 */
public class TextSplitterFactory {

    private static final Logger logger = LoggerFactory.getLogger(TextSplitterFactory.class);
    
    // 单例实例
    private static volatile TextSplitterFactory instance;
    
    // 注册的分割器类型
    private final Map<String, Class<? extends TextSplitter>> splitterRegistry;
    
    // 默认配置缓存
    private final Map<String, SplitterConfig> defaultConfigs;
    
    /**
     * 私有构造函数（单例模式）
     */
    private TextSplitterFactory() {
        this.splitterRegistry = new HashMap<>();
        this.defaultConfigs = new HashMap<>();
        
        // 注册默认的分割器类型
        registerDefaultSplitters();
        
        logger.info("TextSplitterFactory initialized with {} splitter types", splitterRegistry.size());
    }
    
    /**
     * 获取工厂实例（单例模式）
     */
    public static TextSplitterFactory getInstance() {
        if (instance == null) {
            synchronized (TextSplitterFactory.class) {
                if (instance == null) {
                    instance = new TextSplitterFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册默认的文本分割器类型
     */
    private void registerDefaultSplitters() {
        // 注册语义分割器
        registerSplitter("semantic", SemanticTextSplitter.class, SplitterConfig.semanticConfig());
        
        // 注册固定长度分割器
        registerSplitter("fixed", FixedLengthSplitter.class, SplitterConfig.fixedLengthConfig());
        
        // 注册Token基础分割器
        registerSplitter("token", TokenBasedSplitter.class, SplitterConfig.tokenBasedConfig());
        
        // 注册递归字符分割器
        registerSplitter("recursive", RecursiveCharacterSplitter.class, SplitterConfig.recursiveConfig());
        
        logger.info("Registered default splitter types: {}", splitterRegistry.keySet());
    }
    
    /**
     * 注册文本分割器类型
     */
    public void registerSplitter(String type, Class<? extends TextSplitter> splitterClass, SplitterConfig defaultConfig) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Splitter type cannot be null or empty");
        }
        
        if (splitterClass == null) {
            throw new IllegalArgumentException("Splitter class cannot be null");
        }
        
        // 验证类是否实现了TextSplitter接口
        if (!TextSplitter.class.isAssignableFrom(splitterClass)) {
            throw new IllegalArgumentException("Splitter class must implement TextSplitter interface");
        }
        
        splitterRegistry.put(type.toLowerCase(), splitterClass);
        
        if (defaultConfig != null) {
            defaultConfigs.put(type.toLowerCase(), defaultConfig);
        }
        
        logger.info("Registered splitter type '{}' with class {}", type, splitterClass.getSimpleName());
    }
    
    /**
     * 创建文本分割器实例
     */
    public TextSplitter createSplitter(String type) {
        return createSplitter(type, null);
    }
    
    /**
     * 创建文本分割器实例（带配置）
     */
    public TextSplitter createSplitter(String type, SplitterConfig config) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Splitter type cannot be null or empty");
        }
        
        String typeKey = type.toLowerCase();
        Class<? extends TextSplitter> splitterClass = splitterRegistry.get(typeKey);
        
        if (splitterClass == null) {
            throw new IllegalArgumentException("Unknown splitter type: " + type);
        }
        
        try {
            TextSplitter splitter;
            
            if (config != null) {
                // 使用指定的配置
                splitter = splitterClass.getConstructor(SplitterConfig.class).newInstance(config);
                logger.info("Created {} instance with custom config", splitterClass.getSimpleName());
            } else {
                // 使用默认配置
                SplitterConfig defaultConfig = defaultConfigs.get(typeKey);
                if (defaultConfig != null) {
                    splitter = splitterClass.getConstructor(SplitterConfig.class).newInstance(defaultConfig);
                    logger.info("Created {} instance with default config", splitterClass.getSimpleName());
                } else {
                    // 使用无参构造函数
                    splitter = splitterClass.getConstructor().newInstance();
                    logger.info("Created {} instance with no config", splitterClass.getSimpleName());
                }
            }
            
            return splitter;
            
        } catch (Exception e) {
            String errorMsg = "Failed to create splitter instance for type: " + type;
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * 批量创建多个分割器实例
     */
    public List<TextSplitter> createSplitters(String... types) {
        return createSplitters(Arrays.asList(types));
    }
    
    /**
     * 批量创建多个分割器实例
     */
    public List<TextSplitter> createSplitters(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<TextSplitter> splitters = new ArrayList<>();
        
        for (String type : types) {
            try {
                TextSplitter splitter = createSplitter(type);
                splitters.add(splitter);
            } catch (Exception e) {
                logger.warn("Failed to create splitter for type '{}': {}", type, e.getMessage());
                // 继续处理其他类型
            }
        }
        
        logger.info("Created {} splitters out of {} requested types", splitters.size(), types.size());
        return splitters;
    }
    
    /**
     * 获取所有可用的分割器类型
     */
    public Set<String> getAvailableTypes() {
        return Collections.unmodifiableSet(splitterRegistry.keySet());
    }
    
    /**
     * 获取分割器类型的信息
     */
    public Map<String, Object> getSplitterInfo(String type) {
        if (type == null || type.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        String typeKey = type.toLowerCase();
        Class<? extends TextSplitter> splitterClass = splitterRegistry.get(typeKey);
        
        if (splitterClass == null) {
            return Collections.emptyMap();
        }
        
        try {
            // 创建临时实例获取信息
            TextSplitter tempSplitter = createSplitter(type);
            
            Map<String, Object> info = new HashMap<>();
            info.put("type", type);
            info.put("name", tempSplitter.getName());
            info.put("description", tempSplitter.getDescription());
            info.put("class", splitterClass.getSimpleName());
            info.put("supported_types", Arrays.asList(tempSplitter.getSupportedTypes()));
            
            // 获取默认配置信息
            SplitterConfig defaultConfig = defaultConfigs.get(typeKey);
            if (defaultConfig != null) {
                info.put("default_config", getConfigInfo(defaultConfig));
            }
            
            return info;
            
        } catch (Exception e) {
            logger.error("Failed to get info for splitter type '{}': {}", type, e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * 获取所有分割器的信息
     */
    public List<Map<String, Object>> getAllSplitterInfo() {
        List<Map<String, Object>> allInfo = new ArrayList<>();
        
        for (String type : splitterRegistry.keySet()) {
            Map<String, Object> info = getSplitterInfo(type);
            if (!info.isEmpty()) {
                allInfo.add(info);
            }
        }
        
        return allInfo;
    }
    
    /**
     * 获取配置信息
     */
    private Map<String, Object> getConfigInfo(SplitterConfig config) {
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("chunk_size", config.getChunkSize());
        configInfo.put("chunk_overlap", config.getChunkOverlap());
        configInfo.put("max_chunk_size", config.getMaxChunkSize());
        configInfo.put("min_chunk_size", config.getMinChunkSize());
        configInfo.put("semantic_threshold", config.getSemanticThreshold());
        configInfo.put("semantic_window_size", config.getSemanticWindowSize());
        configInfo.put("preserve_sentence_boundary", config.isPreserveSentenceBoundary());
        configInfo.put("preserve_paragraph_boundary", config.isPreserveParagraphBoundary());
        configInfo.put("separators", config.getSeparators());
        return configInfo;
    }
    
    /**
     * 检查分割器类型是否可用
     */
    public boolean isTypeAvailable(String type) {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }
        return splitterRegistry.containsKey(type.toLowerCase());
    }
    
    /**
     * 移除分割器类型
     */
    public void unregisterSplitter(String type) {
        if (type == null || type.trim().isEmpty()) {
            return;
        }
        
        String typeKey = type.toLowerCase();
        Class<? extends TextSplitter> removedClass = splitterRegistry.remove(typeKey);
        SplitterConfig removedConfig = defaultConfigs.remove(typeKey);
        
        if (removedClass != null) {
            logger.info("Unregistered splitter type '{}' (class: {})", type, removedClass.getSimpleName());
        }
    }
    
    /**
     * 清除所有注册的分割器
     */
    public void clearAllSplitters() {
        splitterRegistry.clear();
        defaultConfigs.clear();
        logger.info("Cleared all registered splitters");
    }
    
    /**
     * 重新注册默认分割器
     */
    public void reinitializeDefaults() {
        clearAllSplitters();
        registerDefaultSplitters();
        logger.info("Reinitialized default splitters");
    }
    
    /**
     * 获取工厂实例的统计信息
     */
    public Map<String, Object> getFactoryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("registered_types", splitterRegistry.size());
        stats.put("default_configs", defaultConfigs.size());
        stats.put("available_types", new ArrayList<>(splitterRegistry.keySet()));
        stats.put("factory_class", this.getClass().getSimpleName());
        stats.put("timestamp", new Date());
        return stats;
    }
}