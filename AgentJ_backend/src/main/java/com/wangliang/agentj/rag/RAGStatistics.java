package com.wangliang.agentj.rag;

/**
 * RAG系统统计信息
 */
public class RAGStatistics {
    
    private long totalQueries;
    private long successfulQueries;
    private long failedQueries;
    private double averageResponseTime;
    private double averageRelevanceScore;
    private double averageQualityScore;
    private int totalDocuments;
    private int highQualityDocuments;
    private int lowQualityDocuments;
    private long lastUpdateTime;
    
    public RAGStatistics() {
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public long getTotalQueries() {
        return totalQueries;
    }
    
    public void setTotalQueries(long totalQueries) {
        this.totalQueries = totalQueries;
    }
    
    public long getSuccessfulQueries() {
        return successfulQueries;
    }
    
    public void setSuccessfulQueries(long successfulQueries) {
        this.successfulQueries = successfulQueries;
    }
    
    public long getFailedQueries() {
        return failedQueries;
    }
    
    public void setFailedQueries(long failedQueries) {
        this.failedQueries = failedQueries;
    }
    
    public double getAverageResponseTime() {
        return averageResponseTime;
    }
    
    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }
    
    public double getAverageRelevanceScore() {
        return averageRelevanceScore;
    }
    
    public void setAverageRelevanceScore(double averageRelevanceScore) {
        this.averageRelevanceScore = averageRelevanceScore;
    }
    
    public double getAverageQualityScore() {
        return averageQualityScore;
    }
    
    public void setAverageQualityScore(double averageQualityScore) {
        this.averageQualityScore = averageQualityScore;
    }
    
    public int getTotalDocuments() {
        return totalDocuments;
    }
    
    public void setTotalDocuments(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }
    
    public int getHighQualityDocuments() {
        return highQualityDocuments;
    }
    
    public void setHighQualityDocuments(int highQualityDocuments) {
        this.highQualityDocuments = highQualityDocuments;
    }
    
    public int getLowQualityDocuments() {
        return lowQualityDocuments;
    }
    
    public void setLowQualityDocuments(int lowQualityDocuments) {
        this.lowQualityDocuments = lowQualityDocuments;
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public double getSuccessRate() {
        return totalQueries > 0 ? (double) successfulQueries / totalQueries : 0.0;
    }
    
    public double getQualityDistribution() {
        return totalDocuments > 0 ? (double) highQualityDocuments / totalDocuments : 0.0;
    }
    
    @Override
    public String toString() {
        return "RAGStatistics{" +
                "totalQueries=" + totalQueries +
                ", successfulQueries=" + successfulQueries +
                ", failedQueries=" + failedQueries +
                ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) +
                ", averageResponseTime=" + String.format("%.2fms", averageResponseTime) +
                ", averageRelevanceScore=" + String.format("%.3f", averageRelevanceScore) +
                ", averageQualityScore=" + String.format("%.3f", averageQualityScore) +
                ", totalDocuments=" + totalDocuments +
                ", highQualityDocuments=" + highQualityDocuments +
                ", lowQualityDocuments=" + lowQualityDocuments +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }
}