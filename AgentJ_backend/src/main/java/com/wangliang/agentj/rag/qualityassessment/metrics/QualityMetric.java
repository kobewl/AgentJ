package com.wangliang.agentj.rag.qualityassessment.metrics;

import com.wangliang.agentj.rag.common.models.Document;

import java.util.Map;
import java.util.List;

/**
 * 质量评估指标接口
 * 定义文档质量评估指标的标准接口
 */
public interface QualityMetric {
    
    /**
     * 评估文档的特定质量维度
     * @param document 待评估的文档
     * @return 评估结果映射，包含评分、反馈、问题等信息
     */
    Map<String, Object> evaluate(Document document);
    
    /**
     * 检查指标是否支持指定的文档类型
     * @param document 文档对象
     * @return 是否支持
     */
    boolean supports(Document document);
    
    /**
     * 获取指标名称
     * @return 指标名称
     */
    String getName();
    
    /**
     * 获取指标描述
     * @return 指标描述
     */
    String getDescription();
    
    /**
     * 获取评估维度
     * @return 评估维度
     */
    String getDimension();
    
    /**
     * 获取指标的权重
     * @return 权重值
     */
    double getWeight();
    
    /**
     * 设置指标的权重
     * @param weight 权重值
     */
    void setWeight(double weight);
    
    /**
     * 获取指标的阈值
     * @return 阈值
     */
    double getThreshold();
    
    /**
     * 设置指标的阈值
     * @param threshold 阈值
     */
    void setThreshold(double threshold);
    
    /**
     * 获取指标的配置参数
     * @return 配置参数映射
     */
    Map<String, Object> getParameters();
    
    /**
     * 设置指标参数
     * @param parameters 参数映射
     */
    void setParameters(Map<String, Object> parameters);
    
    /**
     * 获取支持的问题类型
     * @return 问题类型列表
     */
    List<String> getSupportedIssueTypes();
}