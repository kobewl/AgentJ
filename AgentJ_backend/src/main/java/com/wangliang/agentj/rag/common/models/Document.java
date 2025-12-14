package com.wangliang.agentj.rag.common.models;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 标准化文档对象
 * 用于统一表示从不同数据源加载的文档
 */
public class Document {
    
    private String id;
    private String content;
    private String title;
    private String source;
    private String fileType;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
    private Map<String, Object> metadata;
    private List<String> keywords;
    private String language;
    private long fileSize;
    private String checksum;
    
    public Document() {
        this.metadata = new HashMap<>();
        this.keywords = new ArrayList<>();
        this.createdTime = LocalDateTime.now();
        this.modifiedTime = LocalDateTime.now();
    }
    
    public Document(String id, String content, String title) {
        this();
        this.id = id;
        this.content = content;
        this.title = title;
    }
    
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
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getModifiedTime() {
        return modifiedTime;
    }
    
    public void setModifiedTime(LocalDateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public List<String> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
    
    public void addKeyword(String keyword) {
        this.keywords.add(keyword);
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", source='" + source + '\'' +
                ", fileType='" + fileType + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", keywords=" + keywords.size() +
                ", language='" + language + '\'' +
                '}';
    }
}