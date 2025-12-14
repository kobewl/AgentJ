package com.wangliang.agentj.rag.qualityassessment.models;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 文档质量评估结果
 * 包含文档质量评估的详细信息和评分
 */
public class QualityAssessmentResult {
    
    private String documentId;
    private double overallScore;
    private String qualityLevel;
    private Map<String, Double> dimensionScores;
    private Map<String, String> dimensionFeedbacks;
    private List<String> issues;
    private List<String> suggestions;
    private boolean passed;
    private String assessmentMethod;
    private long timestamp;
    
    public QualityAssessmentResult() {
        this.dimensionScores = new HashMap<>();
        this.dimensionFeedbacks = new HashMap<>();
        this.issues = new ArrayList<>();
        this.suggestions = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.passed = true;
    }
    
    public QualityAssessmentResult(String documentId, double overallScore) {
        this();
        this.documentId = documentId;
        this.overallScore = overallScore;
        this.qualityLevel = calculateQualityLevel(overallScore);
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public double getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
        this.qualityLevel = calculateQualityLevel(overallScore);
    }
    
    public String getQualityLevel() {
        return qualityLevel;
    }
    
    public void setQualityLevel(String qualityLevel) {
        this.qualityLevel = qualityLevel;
    }
    
    public Map<String, Double> getDimensionScores() {
        return dimensionScores;
    }
    
    public void setDimensionScores(Map<String, Double> dimensionScores) {
        this.dimensionScores = dimensionScores;
    }
    
    public void addDimensionScore(String dimension, double score) {
        this.dimensionScores.put(dimension, score);
    }
    
    public Map<String, String> getDimensionFeedbacks() {
        return dimensionFeedbacks;
    }
    
    public void setDimensionFeedbacks(Map<String, String> dimensionFeedbacks) {
        this.dimensionFeedbacks = dimensionFeedbacks;
    }
    
    public void addDimensionFeedback(String dimension, String feedback) {
        this.dimensionFeedbacks.put(dimension, feedback);
    }
    
    public List<String> getIssues() {
        return issues;
    }
    
    public void setIssues(List<String> issues) {
        this.issues = issues;
    }
    
    public void addIssue(String issue) {
        this.issues.add(issue);
        this.passed = false;
    }
    
    public List<String> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
    
    public void addSuggestion(String suggestion) {
        this.suggestions.add(suggestion);
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    
    public String getAssessmentMethod() {
        return assessmentMethod;
    }
    
    public void setAssessmentMethod(String assessmentMethod) {
        this.assessmentMethod = assessmentMethod;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    private String calculateQualityLevel(double score) {
        if (score >= 0.9) {
            return "优秀";
        } else if (score >= 0.8) {
            return "良好";
        } else if (score >= 0.7) {
            return "中等";
        } else if (score >= 0.6) {
            return "及格";
        } else {
            return "不及格";
        }
    }
    
    @Override
    public String toString() {
        return "QualityAssessmentResult{" +
                "documentId='" + documentId + '\'' +
                ", overallScore=" + overallScore +
                ", qualityLevel='" + qualityLevel + '\'' +
                ", dimensionScores=" + dimensionScores.size() +
                ", issues=" + issues.size() +
                ", suggestions=" + suggestions.size() +
                ", passed=" + passed +
                ", assessmentMethod='" + assessmentMethod + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}