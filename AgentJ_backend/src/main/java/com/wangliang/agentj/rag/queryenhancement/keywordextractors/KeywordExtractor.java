package com.wangliang.agentj.rag.queryenhancement.keywordextractors;

import com.wangliang.agentj.rag.queryenhancement.models.Query;
import java.util.List;

/**
 * 关键词提取器接口
 * 定义从查询中提取关键词的标准接口
 */
public interface KeywordExtractor {
    
    /**
     * 从查询中提取关键词
     * @param query 查询对象
     * @return 提取的关键词列表
     */
    List<String> extractKeywords(Query query);
    
    /**
     * 从文本中提取关键词
     * @param text 文本内容
     * @return 提取的关键词列表
     */
    List<String> extractKeywords(String text);
    
    /**
     * 检查提取器是否支持指定的查询类型
     * @param query 查询对象
     * @return 是否支持该查询类型
     */
    boolean supports(Query query);
    
    /**
     * 获取提取器名称
     * @return 提取器名称
     */
    String getName();
    
    /**
     * 获取提取器描述
     * @return 提取器描述
     */
    String getDescription();
    
    /**
     * 获取关键词提取的置信度阈值
     * @return 置信度阈值
     */
    double getConfidenceThreshold();
}