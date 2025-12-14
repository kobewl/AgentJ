package com.wangliang.agentj.rag.queryenhancement.models;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 查询对象模型
 * 用于表示用户查询及其增强信息
 */
public class Query {
    
    private String originalQuery;
    private String enhancedQuery;
    private List<String> keywords;
    private String intent;
    private String domain;
    private double confidence;
    private Map<String, Object> context;
    private List<String> expandedTerms;
    private String language;
    private long timestamp;
    
    public Query() {
        this.keywords = new ArrayList<>();
        this.context = new HashMap<>();
        this.expandedTerms = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.confidence = 0.0;
    }
    
    public Query(String originalQuery) {
        this();
        this.originalQuery = originalQuery;
        this.enhancedQuery = originalQuery;
    }
    
    public String getOriginalQuery() {
        return originalQuery;
    }
    
    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }
    
    public String getEnhancedQuery() {
        return enhancedQuery;
    }
    
    public void setEnhancedQuery(String enhancedQuery) {
        this.enhancedQuery = enhancedQuery;
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
    
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    public void addContext(String key, Object value) {
        this.context.put(key, value);
    }
    
    public List<String> getExpandedTerms() {
        return expandedTerms;
    }
    
    public void setExpandedTerms(List<String> expandedTerms) {
        this.expandedTerms = expandedTerms;
    }
    
    public void addExpandedTerm(String term) {
        this.expandedTerms.add(term);
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Query{" +
                "originalQuery='" + originalQuery + '\'' +
                ", enhancedQuery='" + enhancedQuery + '\'' +
                ", keywords=" + keywords.size() +
                ", intent='" + intent + '\'' +
                ", domain='" + domain + '\'' +
                ", confidence=" + confidence +
                ", expandedTerms=" + expandedTerms.size() +
                ", language='" + language + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}