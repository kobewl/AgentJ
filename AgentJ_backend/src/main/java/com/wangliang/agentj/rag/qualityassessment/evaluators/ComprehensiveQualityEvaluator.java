package com.wangliang.agentj.rag.qualityassessment.evaluators;

import com.wangliang.agentj.rag.common.models.Document;
import com.wangliang.agentj.rag.qualityassessment.models.QualityAssessmentResult;
import com.wangliang.agentj.rag.qualityassessment.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 综合文档质量评估器
 * 集成多个质量维度进行综合评估
 */
public class ComprehensiveQualityEvaluator implements QualityEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveQualityEvaluator.class);
    
    private double qualityThreshold = 0.7;
    private Map<String, Double> dimensionWeights;
    private List<QualityMetric> qualityMetrics;
    
    public ComprehensiveQualityEvaluator() {
        this.qualityMetrics = new ArrayList<>();
        this.dimensionWeights = new HashMap<>();
        initializeDefaultMetrics();
    }
    
    private void initializeDefaultMetrics() {
        // 添加默认的质量评估指标
        qualityMetrics.add(new ContentCompletenessMetric());
        qualityMetrics.add(new TextReadabilityMetric());
        qualityMetrics.add(new DocumentStructureMetric());
        qualityMetrics.add(new LanguageQualityMetric());
        qualityMetrics.add(new ContentRelevanceMetric());
        
        // 设置默认权重
        dimensionWeights.put("completeness", 0.25);
        dimensionWeights.put("readability", 0.20);
        dimensionWeights.put("structure", 0.20);
        dimensionWeights.put("language", 0.15);
        dimensionWeights.put("relevance", 0.20);
    }
    
    @Override
    public QualityAssessmentResult evaluate(Document document) {
        logger.info("开始综合质量评估: document={}", document.getId());
        
        if (document == null || document.getContent() == null) {
            QualityAssessmentResult result = new QualityAssessmentResult("unknown", 0.0);
            result.setPassed(false);
            result.addIssue("文档内容为空");
            return result;
        }
        
        QualityAssessmentResult result = new QualityAssessmentResult(document.getId(), 0.0);
        result.setAssessmentMethod("Comprehensive Quality Assessment");
        
        double totalScore = 0.0;
        double totalWeight = 0.0;
        
        // 评估各个维度
        for (QualityMetric metric : qualityMetrics) {
            try {
                if (metric.supports(document)) {
                    Map<String, Object> metricResult = metric.evaluate(document);
                    
                    String dimension = metric.getDimension();
                    double score = (Double) metricResult.getOrDefault("score", 0.0);
                    String feedback = (String) metricResult.getOrDefault("feedback", "");
                    List<String> issues = (List<String>) metricResult.getOrDefault("issues", new ArrayList<>());
                    List<String> suggestions = (List<String>) metricResult.getOrDefault("suggestions", new ArrayList<>());
                    
                    double weight = dimensionWeights.getOrDefault(dimension, 0.0);
                    
                    result.addDimensionScore(dimension, score);
                    result.addDimensionFeedback(dimension, feedback);
                    
                    for (String issue : issues) {
                        result.addIssue(issue);
                    }
                    
                    for (String suggestion : suggestions) {
                        result.addSuggestion(suggestion);
                    }
                    
                    totalScore += score * weight;
                    totalWeight += weight;
                    
                    logger.debug("维度评估完成: dimension={}, score={}, weight={}", 
                                dimension, String.format("%.3f", score), weight);
                }
            } catch (Exception e) {
                logger.warn("评估维度失败: dimension={}, error={}", metric.getDimension(), e.getMessage());
                result.addIssue("评估维度 " + metric.getDimension() + " 失败: " + e.getMessage());
            }
        }
        
        // 计算总体评分
        double overallScore = totalWeight > 0 ? totalScore / totalWeight : 0.0;
        result.setOverallScore(overallScore);
        result.setPassed(overallScore >= qualityThreshold);
        
        logger.info("综合质量评估完成: overallScore={}, passed={}, issues={}", 
                   String.format("%.3f", overallScore), result.isPassed(), result.getIssues().size());
        
        return result;
    }
    
    @Override
    public List<QualityAssessmentResult> evaluate(List<Document> documents) {
        logger.info("批量综合质量评估: {} 个文档", documents.size());
        
        List<QualityAssessmentResult> results = new ArrayList<>();
        
        for (Document document : documents) {
            QualityAssessmentResult result = evaluate(document);
            results.add(result);
        }
        
        // 按评分排序（降序）
        results.sort((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()));
        
        logger.info("批量综合质量评估完成: {} 个文档", results.size());
        return results;
    }
    
    @Override
    public boolean supports(Document document) {
        return document != null && document.getContent() != null;
    }
    
    @Override
    public String getName() {
        return "Comprehensive Quality Evaluator";
    }
    
    @Override
    public String getDescription() {
        return "Comprehensive document quality assessment using multiple dimensions";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("quality_threshold", qualityThreshold);
        params.put("dimension_weights", new HashMap<>(dimensionWeights));
        params.put("metrics_count", qualityMetrics.size());
        return params;
    }
    
    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("quality_threshold")) {
            this.qualityThreshold = ((Number) parameters.get("quality_threshold")).doubleValue();
        }
        if (parameters.containsKey("dimension_weights")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> weights = (Map<String, Double>) parameters.get("dimension_weights");
            this.dimensionWeights.putAll(weights);
        }
    }
    
    @Override
    public double getQualityThreshold() {
        return qualityThreshold;
    }
    
    @Override
    public void setQualityThreshold(double threshold) {
        this.qualityThreshold = threshold;
    }
    
    @Override
    public List<String> getSupportedDimensions() {
        List<String> dimensions = new ArrayList<>();
        for (QualityMetric metric : qualityMetrics) {
            dimensions.add(metric.getDimension());
        }
        return dimensions;
    }
    
    @Override
    public boolean needsReevaluation(Document document, QualityAssessmentResult lastResult) {
        if (lastResult == null) {
            return true;
        }
        
        // 检查文档是否发生变化
        long currentTime = System.currentTimeMillis();
        long lastEvaluationTime = lastResult.getTimestamp();
        
        // 如果超过24小时，重新评估
        if (currentTime - lastEvaluationTime > 24 * 60 * 60 * 1000) {
            return true;
        }
        
        // 如果上次评估未通过，重新评估
        if (!lastResult.isPassed()) {
            return true;
        }
        
        return false;
    }
    
    public void addQualityMetric(QualityMetric metric) {
        this.qualityMetrics.add(metric);
    }
    
    public void removeQualityMetric(QualityMetric metric) {
        this.qualityMetrics.remove(metric);
    }
    
    public void setDimensionWeight(String dimension, double weight) {
        this.dimensionWeights.put(dimension, weight);
    }
    
    public double getDimensionWeight(String dimension) {
        return this.dimensionWeights.getOrDefault(dimension, 0.0);
    }
}