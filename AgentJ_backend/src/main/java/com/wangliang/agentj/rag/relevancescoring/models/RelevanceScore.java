package com.wangliang.agentj.rag.relevancescoring.models;

import java.util.Map;
import java.util.HashMap;

/**
 * 相关性评分结果
 * 包含文档与查询的相关性评分信息
 */
public class RelevanceScore {
    
    private String documentId;
    private String queryId;
    private double score;
    private String algorithm;
    private Map<String, Double> componentScores;
    private String explanation;
    private long timestamp;
    
    public RelevanceScore() {
        this.componentScores = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public RelevanceScore(String documentId, String queryId, double score, String algorithm) {
        this();
        this.documentId = documentId;
        this.queryId = queryId;
        this.score = score;
        this.algorithm = algorithm;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getQueryId() {
        return queryId;
    }
    
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    public Map<String, Double> getComponentScores() {
        return componentScores;
    }
    
    public void setComponentScores(Map<String, Double> componentScores) {
        this.componentScores = componentScores;
    }
    
    public void addComponentScore(String component, double score) {
        this.componentScores.put(component, score);
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRelevant() {
        return score >= 0.5; // 默认相关性阈值
    }
    
    @Override
    public String toString() {
        return "RelevanceScore{" +
                "documentId='" + documentId + '\'' +
                ", queryId='" + queryId + '\'' +
                ", score=" + score +
                ", algorithm='" + algorithm + '\'' +
                ", componentScores=" + componentScores.size() +
                ", explanation='" + explanation + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}