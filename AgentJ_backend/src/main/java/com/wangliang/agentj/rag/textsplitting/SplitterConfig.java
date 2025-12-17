package com.wangliang.agentj.rag.textsplitting;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

/**
 * 文本分割器配置类
 * 用于配置各种分割器的参数和行为
 */
public class SplitterConfig {
    
    // 基础配置
    private int chunkSize = 1000;                    // 块大小（字符数）
    private int chunkOverlap = 200;                  // 块重叠大小
    private int maxChunkSize = 2000;                 // 最大块大小
    private int minChunkSize = 100;                  // 最小块大小
    
    // 语义分割配置
    private double semanticThreshold = 0.7;          // 语义相似度阈值
    private int semanticWindowSize = 3;              // 语义窗口大小
    private boolean preserveSentenceBoundary = true; // 保持句子边界
    private boolean preserveParagraphBoundary = true; // 保持段落边界
    
    // 分隔符配置
    private List<String> separators = Arrays.asList(
        "\n\n",  // 段落
        "\n",     // 换行
        "。",     // 中文句号
        "！",     // 中文感叹号
        "？",     // 中文问号
        ".",      // 英文句号
        "!",      // 英文感叹号
        "?",      // 英文问号
        "；",     // 中文分号
        ";",      // 英文分号
        "，",     // 中文逗号
        ",",      // 英文逗号
        " "       // 空格
    );
    
    // Token配置
    private int maxTokens = 512;                     // 最大token数
    private int tokenOverlap = 50;                   // token重叠数
    private String tokenizationModel = "default";    // 分词模型
    
    // Spring AI TokenTextSplitter 配置
    private int defaultChunkSize = 800;              // 默认块大小（token数）
    private int minChunkSizeChars = 350;             // 最小块大小（字符数）
    private int minChunkLengthToEmbed = 5;           // 最小嵌入长度
    private int maxNumChunks = 10000;                // 最大块数
    private boolean keepSeparator = true;            // 保持分隔符
    
    // 高级配置
    private boolean removeEmptyChunks = true;        // 移除空块
    private boolean trimWhitespace = true;           // 修剪空白字符
    private boolean addMetadata = true;              // 添加元数据
    private boolean calculateStatistics = true;      // 计算统计信息
    
    // 自定义参数
    private Map<String, Object> customParameters = new HashMap<>();
    
    /**
     * 默认构造函数
     */
    public SplitterConfig() {
    }
    
    /**
     * 基础配置构造函数
     */
    public SplitterConfig(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }
    
    /**
     * 全参数构造函数
     */
    public SplitterConfig(int chunkSize, int chunkOverlap, double semanticThreshold, 
                         List<String> separators, int maxTokens) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.semanticThreshold = semanticThreshold;
        this.separators = separators;
        this.maxTokens = maxTokens;
    }
    
    // Getters and Setters
    public int getChunkSize() {
        return chunkSize;
    }
    
    public void setChunkSize(int chunkSize) {
        this.chunkSize = Math.max(minChunkSize, Math.min(maxChunkSize, chunkSize));
    }
    
    public int getChunkOverlap() {
        return chunkOverlap;
    }
    
    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = Math.max(0, Math.min(chunkSize / 2, chunkOverlap));
    }
    
    public int getMaxChunkSize() {
        return maxChunkSize;
    }
    
    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        // 确保当前chunkSize在有效范围内
        setChunkSize(this.chunkSize);
    }
    
    public int getMinChunkSize() {
        return minChunkSize;
    }
    
    public void setMinChunkSize(int minChunkSize) {
        this.minChunkSize = minChunkSize;
        // 确保当前chunkSize在有效范围内
        setChunkSize(this.chunkSize);
    }
    
    public double getSemanticThreshold() {
        return semanticThreshold;
    }
    
    public void setSemanticThreshold(double semanticThreshold) {
        this.semanticThreshold = Math.max(0.0, Math.min(1.0, semanticThreshold));
    }
    
    public int getSemanticWindowSize() {
        return semanticWindowSize;
    }
    
    public void setSemanticWindowSize(int semanticWindowSize) {
        this.semanticWindowSize = Math.max(1, semanticWindowSize);
    }
    
    public boolean isPreserveSentenceBoundary() {
        return preserveSentenceBoundary;
    }
    
    public void setPreserveSentenceBoundary(boolean preserveSentenceBoundary) {
        this.preserveSentenceBoundary = preserveSentenceBoundary;
    }
    
    public boolean isPreserveParagraphBoundary() {
        return preserveParagraphBoundary;
    }
    
    public void setPreserveParagraphBoundary(boolean preserveParagraphBoundary) {
        this.preserveParagraphBoundary = preserveParagraphBoundary;
    }
    
    public List<String> getSeparators() {
        return separators;
    }
    
    public void setSeparators(List<String> separators) {
        this.separators = separators != null ? separators : new ArrayList<>();
    }
    
    public void addSeparator(String separator) {
        if (this.separators == null) {
            this.separators = new ArrayList<>();
        }
        if (!this.separators.contains(separator)) {
            this.separators.add(separator);
        }
    }
    
    public void removeSeparator(String separator) {
        if (this.separators != null) {
            this.separators.remove(separator);
        }
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = Math.max(1, maxTokens);
    }
    
    public int getTokenOverlap() {
        return tokenOverlap;
    }
    
    public void setTokenOverlap(int tokenOverlap) {
        this.tokenOverlap = Math.max(0, Math.min(maxTokens / 4, tokenOverlap));
    }
    
    public String getTokenizationModel() {
        return tokenizationModel;
    }
    
    public void setTokenizationModel(String tokenizationModel) {
        this.tokenizationModel = tokenizationModel != null ? tokenizationModel : "default";
    }
    
    // Spring AI TokenTextSplitter 配置方法
    public int getDefaultChunkSize() {
        return defaultChunkSize;
    }
    
    public void setDefaultChunkSize(int defaultChunkSize) {
        this.defaultChunkSize = Math.max(100, Math.min(4000, defaultChunkSize));
    }
    
    public int getMinChunkSizeChars() {
        return minChunkSizeChars;
    }
    
    public void setMinChunkSizeChars(int minChunkSizeChars) {
        this.minChunkSizeChars = Math.max(50, Math.min(1000, minChunkSizeChars));
    }
    
    public int getMinChunkLengthToEmbed() {
        return minChunkLengthToEmbed;
    }
    
    public void setMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
        this.minChunkLengthToEmbed = Math.max(1, Math.min(100, minChunkLengthToEmbed));
    }
    
    public int getMaxNumChunks() {
        return maxNumChunks;
    }
    
    public void setMaxNumChunks(int maxNumChunks) {
        this.maxNumChunks = Math.max(10, Math.min(50000, maxNumChunks));
    }
    
    public boolean isKeepSeparator() {
        return keepSeparator;
    }
    
    public void setKeepSeparator(boolean keepSeparator) {
        this.keepSeparator = keepSeparator;
    }
    
    public boolean isRemoveEmptyChunks() {
        return removeEmptyChunks;
    }
    
    public void setRemoveEmptyChunks(boolean removeEmptyChunks) {
        this.removeEmptyChunks = removeEmptyChunks;
    }
    
    public boolean isTrimWhitespace() {
        return trimWhitespace;
    }
    
    public void setTrimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
    }
    
    public boolean isAddMetadata() {
        return addMetadata;
    }
    
    public void setAddMetadata(boolean addMetadata) {
        this.addMetadata = addMetadata;
    }
    
    public boolean isCalculateStatistics() {
        return calculateStatistics;
    }
    
    public void setCalculateStatistics(boolean calculateStatistics) {
        this.calculateStatistics = calculateStatistics;
    }
    
    public Map<String, Object> getCustomParameters() {
        return customParameters;
    }

    /**
     * 兼容工厂调用的递归配置便捷方法。
     */
    public static SplitterConfig recursiveConfig() {
        return new SplitterConfig();
    }
    
    public void setCustomParameters(Map<String, Object> customParameters) {
        this.customParameters = customParameters != null ? customParameters : new HashMap<>();
    }
    
    public void addCustomParameter(String key, Object value) {
        if (this.customParameters == null) {
            this.customParameters = new HashMap<>();
        }
        this.customParameters.put(key, value);
    }
    
    public Object getCustomParameter(String key) {
        return this.customParameters != null ? this.customParameters.get(key) : null;
    }
    
    /**
     * 验证配置参数的有效性
     * 
     * @return 是否有效
     */
    public boolean validate() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be non-negative and less than chunk size");
        }
        if (semanticThreshold < 0.0 || semanticThreshold > 1.0) {
            throw new IllegalArgumentException("Semantic threshold must be between 0.0 and 1.0");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Max tokens must be positive");
        }
        return true;
    }
    
    /**
     * 创建默认配置
     */
    public static SplitterConfig defaultConfig() {
        return new SplitterConfig();
    }
    
    /**
     * 创建语义分割配置
     */
    public static SplitterConfig semanticConfig() {
        SplitterConfig config = new SplitterConfig();
        config.setChunkSize(800);
        config.setChunkOverlap(150);
        config.setSemanticThreshold(0.75);
        config.setPreserveSentenceBoundary(true);
        config.setPreserveParagraphBoundary(true);
        return config;
    }
    
    /**
     * 创建固定长度分割配置
     */
    public static SplitterConfig fixedLengthConfig() {
        SplitterConfig config = new SplitterConfig();
        config.setChunkSize(1000);
        config.setChunkOverlap(200);
        config.setPreserveSentenceBoundary(false);
        config.setPreserveParagraphBoundary(false);
        return config;
    }
    
    /**
     * 创建Token基础分割配置（基于Spring AI TokenTextSplitter）
     */
    public static SplitterConfig tokenBasedConfig() {
        SplitterConfig config = new SplitterConfig();
        config.setDefaultChunkSize(800);      // Spring AI默认800 tokens
        config.setMinChunkSizeChars(350);     // Spring AI默认350字符
        config.setMinChunkLengthToEmbed(5);   // Spring AI默认5字符
        config.setMaxNumChunks(10000);        // Spring AI默认10000块
        config.setKeepSeparator(true);        // Spring AI默认保持分隔符
        config.setChunkSize(2000);            // 字符数限制作为备用
        config.setChunkOverlap(200);          // 字符重叠
        return config;
    }
    
    @Override
    public String toString() {
        return String.format("SplitterConfig{chunkSize=%d, chunkOverlap=%d, semanticThreshold=%.2f, maxTokens=%d}",
                chunkSize, chunkOverlap, semanticThreshold, maxTokens);
    }
}
