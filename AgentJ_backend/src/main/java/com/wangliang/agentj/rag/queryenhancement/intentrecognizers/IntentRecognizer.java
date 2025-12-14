package com.wangliang.agentj.rag.queryenhancement.intentrecognizers;

import com.wangliang.agentj.rag.queryenhancement.models.Query;

/**
 * 意图识别器接口
 * 定义识别用户查询意图的标准接口
 */
public interface IntentRecognizer {
    
    /**
     * 识别查询意图
     * @param query 查询对象
     * @return 识别结果
     */
    IntentRecognitionResult recognize(Query query);
    
    /**
     * 从文本中识别意图
     * @param text 文本内容
     * @return 识别结果
     */
    IntentRecognitionResult recognize(String text);
    
    /**
     * 检查识别器是否支持指定的查询类型
     * @param query 查询对象
     * @return 是否支持该查询类型
     */
    boolean supports(Query query);
    
    /**
     * 获取识别器名称
     * @return 识别器名称
     */
    String getName();
    
    /**
     * 获取识别器描述
     * @return 识别器描述
     */
    String getDescription();
    
    /**
     * 获取意图识别的置信度阈值
     * @return 置信度阈值
     */
    double getConfidenceThreshold();
}