package com.wangliang.agentj.rag.queryenhancement.intentrecognizers;

/**
 * 意图识别结果
 * 包含识别的意图、置信度和相关信息
 */
public class IntentRecognitionResult {
    
    private String intent;
    private String domain;
    private double confidence;
    private String description;
    
    public IntentRecognitionResult() {
        this.confidence = 0.0;
    }
    
    public IntentRecognitionResult(String intent, double confidence) {
        this.intent = intent;
        this.confidence = confidence;
        this.domain = "general";
    }
    
    public IntentRecognitionResult(String intent, String domain, double confidence) {
        this.intent = intent;
        this.domain = domain;
        this.confidence = confidence;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isConfident() {
        return confidence >= 0.7; // 默认置信度阈值
    }
    
    @Override
    public String toString() {
        return "IntentRecognitionResult{" +
                "intent='" + intent + '\'' +
                ", domain='" + domain + '\'' +
                ", confidence=" + confidence +
                ", description='" + description + '\'' +
                '}';
    }
}