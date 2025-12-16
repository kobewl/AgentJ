package com.wangliang.agentj.rag.textsplitting;

import java.util.Map;
import java.util.HashMap;

/**
 * 文本块模型类
 * 表示分割后的文本片段，包含内容和相关元数据
 */
public class TextChunk {
    
    private String id;
    private String content;
    private String documentId;
    private int chunkIndex;
    private int startPosition;
    private int endPosition;
    private int wordCount;
    private int characterCount;
    private Map<String, Object> metadata;
    
    /**
     * 默认构造函数
     */
    public TextChunk() {
        this.metadata = new HashMap<>();
    }
    
    /**
     * 全参数构造函数
     */
    public TextChunk(String id, String content, String documentId, int chunkIndex, 
                     int startPosition, int endPosition, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        
        // 计算统计信息
        this.wordCount = countWords(content);
        this.characterCount = content != null ? content.length() : 0;
    }
    
    /**
     * 简化构造函数
     */
    public TextChunk(String content, String documentId, int chunkIndex) {
        this(null, content, documentId, chunkIndex, 0, 0, null);
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        // 更新统计信息
        this.wordCount = countWords(content);
        this.characterCount = content != null ? content.length() : 0;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public int getChunkIndex() {
        return chunkIndex;
    }
    
    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
    
    public int getStartPosition() {
        return startPosition;
    }
    
    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }
    
    public int getEndPosition() {
        return endPosition;
    }
    
    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }
    
    public int getWordCount() {
        return wordCount;
    }
    
    public int getCharacterCount() {
        return characterCount;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * 获取元数据值
     */
    public Object getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
    
    /**
     * 计算词汇数量
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // 移除标点符号并分词
        String[] words = text.replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        int count = 0;
        for (String word : words) {
            if (word.length() > 0) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 获取文本块的摘要（前N个字符）
     */
    public String getSummary(int maxLength) {
        if (content == null) {
            return "";
        }
        
        if (content.length() <= maxLength) {
            return content;
        }
        
        return content.substring(0, maxLength) + "...";
    }
    
    /**
     * 检查文本块是否为空
     */
    public boolean isEmpty() {
        return content == null || content.trim().isEmpty();
    }
    
    /**
     * 获取文本块的大小（字符数）
     */
    public int size() {
        return characterCount;
    }
    
    @Override
    public String toString() {
        return String.format("TextChunk{id='%s', documentId='%s', chunkIndex=%d, wordCount=%d, characterCount=%d, content='%s'}",
                id, documentId, chunkIndex, wordCount, characterCount, getSummary(50));
    }
}