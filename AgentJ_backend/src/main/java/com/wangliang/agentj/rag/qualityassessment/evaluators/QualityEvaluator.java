package com.wangliang.agentj.rag.qualityassessment.evaluators;

import com.wangliang.agentj.rag.common.models.Document;
import com.wangliang.agentj.rag.qualityassessment.models.QualityAssessmentResult;

import java.util.List;
import java.util.Map;

/**
 * 文档质量评估接口
 * 定义文档质量评估的标准接口
 */
public interface QualityEvaluator {
    
    /**
     * 评估单个文档的质量
     * @param document 待评估的文档
     * @return 质量评估结果
     */
    QualityAssessmentResult evaluate(Document document);
    
    /**
     * 批量评估文档质量
     * @param documents 待评估的文档列表
     * @return 质量评估结果列表
     */
    List<QualityAssessmentResult> evaluate(List<Document> documents);
    
    /**
     * 检查评估器是否支持指定的文档类型
     * @param document 文档对象
     * @return 是否支持
     */
    boolean supports(Document document);
    
    /**
     * 获取评估器名称
     * @return 评估器名称
     */
    String getName();
    
    /**
     * 获取评估器描述
     * @return 评估器描述
     */
    String getDescription();
    
    /**
     * 获取评估器的配置参数
     * @return 配置参数映射
     */
    Map<String, Object> getParameters();
    
    /**
     * 设置评估器参数
     * @param parameters 参数映射
     */
    void setParameters(Map<String, Object> parameters);
    
    /**
     * 获取质量评估的阈值
     * @return 质量阈值（0-1之间）
     */
    double getQualityThreshold();
    
    /**
     * 设置质量评估的阈值
     * @param threshold 质量阈值（0-1之间）
     */
    void setQualityThreshold(double threshold);
    
    /**
     * 获取支持的评估维度
     * @return 评估维度列表
     */
    List<String> getSupportedDimensions();
    
    /**
     * 检查是否需要重新评估
     * @param document 文档对象
     * @param lastResult 上次评估结果
     * @return 是否需要重新评估
     */
    boolean needsReevaluation(Document document, QualityAssessmentResult lastResult);
}