package com.wangliang.agentj.rag;

/**
 * RAG系统状态信息
 */
public class RAGSystemStatus {
    
    private boolean initialized;
    private boolean ready;
    private String statusMessage;
    private long lastUpdateTime;
    private int loadedDocuments;
    private int totalQueries;
    private double averageResponseTime;
    
    public RAGSystemStatus() {
        this.initialized = false;
        this.ready = false;
        this.statusMessage = "Not initialized";
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        updateLastUpdateTime();
    }
    
    public boolean isReady() {
        return ready;
    }
    
    public void setReady(boolean ready) {
        this.ready = ready;
        updateLastUpdateTime();
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        updateLastUpdateTime();
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public int getLoadedDocuments() {
        return loadedDocuments;
    }
    
    public void setLoadedDocuments(int loadedDocuments) {
        this.loadedDocuments = loadedDocuments;
        updateLastUpdateTime();
    }
    
    public int getTotalQueries() {
        return totalQueries;
    }
    
    public void setTotalQueries(int totalQueries) {
        this.totalQueries = totalQueries;
        updateLastUpdateTime();
    }
    
    public double getAverageResponseTime() {
        return averageResponseTime;
    }
    
    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
        updateLastUpdateTime();
    }
    
    private void updateLastUpdateTime() {
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "RAGSystemStatus{" +
                "initialized=" + initialized +
                ", ready=" + ready +
                ", statusMessage='" + statusMessage + '\'' +
                ", lastUpdateTime=" + lastUpdateTime +
                ", loadedDocuments=" + loadedDocuments +
                ", totalQueries=" + totalQueries +
                ", averageResponseTime=" + averageResponseTime +
                '}';
    }
}