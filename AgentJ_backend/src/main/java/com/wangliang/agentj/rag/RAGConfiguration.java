package com.wangliang.agentj.rag;

import java.util.Map;
import java.util.HashMap;

/**
 * RAG系统配置
 */
public class RAGConfiguration {
    
    private Map<String, Object> parameters;
    
    public RAGConfiguration() {
        this.parameters = new HashMap<>();
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // 默认配置参数
        parameters.put("max_documents", 10000);
        parameters.put("max_query_length", 512);
        parameters.put("max_response_length", 2048);
        parameters.put("top_k_retrieval", 10);
        parameters.put("quality_threshold", 0.7);
        parameters.put("relevance_threshold", 0.5);
        parameters.put("enable_quality_assessment", true);
        parameters.put("enable_query_enhancement", true);
        parameters.put("enable_caching", true);
        parameters.put("cache_ttl_seconds", 3600);
    }
    
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(parameters);
    }
    
    public void setParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
    
    public Object getParameter(String key, Object defaultValue) {
        return this.parameters.getOrDefault(key, defaultValue);
    }
    
    public int getMaxDocuments() {
        return (Integer) getParameter("max_documents", 10000);
    }
    
    public void setMaxDocuments(int maxDocuments) {
        setParameter("max_documents", maxDocuments);
    }
    
    public int getMaxQueryLength() {
        return (Integer) getParameter("max_query_length", 512);
    }
    
    public void setMaxQueryLength(int maxQueryLength) {
        setParameter("max_query_length", maxQueryLength);
    }
    
    public int getMaxResponseLength() {
        return (Integer) getParameter("max_response_length", 2048);
    }
    
    public void setMaxResponseLength(int maxResponseLength) {
        setParameter("max_response_length", maxResponseLength);
    }
    
    public int getTopKRetrieval() {
        return (Integer) getParameter("top_k_retrieval", 10);
    }
    
    public void setTopKRetrieval(int topKRetrieval) {
        setParameter("top_k_retrieval", topKRetrieval);
    }
    
    public double getQualityThreshold() {
        return (Double) getParameter("quality_threshold", 0.7);
    }
    
    public void setQualityThreshold(double qualityThreshold) {
        setParameter("quality_threshold", qualityThreshold);
    }
    
    public double getRelevanceThreshold() {
        return (Double) getParameter("relevance_threshold", 0.5);
    }
    
    public void setRelevanceThreshold(double relevanceThreshold) {
        setParameter("relevance_threshold", relevanceThreshold);
    }
    
    public boolean isQualityAssessmentEnabled() {
        return (Boolean) getParameter("enable_quality_assessment", true);
    }
    
    public void setQualityAssessmentEnabled(boolean enabled) {
        setParameter("enable_quality_assessment", enabled);
    }
    
    public boolean isQueryEnhancementEnabled() {
        return (Boolean) getParameter("enable_query_enhancement", true);
    }
    
    public void setQueryEnhancementEnabled(boolean enabled) {
        setParameter("enable_query_enhancement", enabled);
    }
    
    public boolean isCachingEnabled() {
        return (Boolean) getParameter("enable_caching", true);
    }
    
    public void setCachingEnabled(boolean enabled) {
        setParameter("enable_caching", enabled);
    }
    
    public int getCacheTTLSeconds() {
        return (Integer) getParameter("cache_ttl_seconds", 3600);
    }
    
    public void setCacheTTLSeconds(int cacheTTLSeconds) {
        setParameter("cache_ttl_seconds", cacheTTLSeconds);
    }
    
    @Override
    public String toString() {
        return "RAGConfiguration{" +
                "parameters=" + parameters +
                '}';
    }
}