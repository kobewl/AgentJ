package com.wangliang.agentj.rag.relevancescoring.algorithms;

import com.wangliang.agentj.rag.common.models.Document;
import com.wangliang.agentj.rag.relevancescoring.models.RelevanceScore;
import com.wangliang.agentj.rag.queryenhancement.models.Query;

import java.util.List;
import java.util.Map;

/**
 * 相关性评分算法接口
 * 定义文档与查询相关性评分的标准接口
 */
public interface RelevanceScoringAlgorithm {
    
    /**
     * 计算文档与查询的相关性评分
     * @param document 文档对象
     * @param query 查询对象
     * @return 相关性评分结果
     */
    RelevanceScore score(Document document, Query query);
    
    /**
     * 批量计算文档与查询的相关性评分
     * @param documents 文档对象列表
     * @param query 查询对象
     * @return 相关性评分结果列表
     */
    List<RelevanceScore> score(List<Document> documents, Query query);
    
    /**
     * 检查算法是否支持指定的文档和查询类型
     * @param document 文档对象
     * @param query 查询对象
     * @return 是否支持
     */
    boolean supports(Document document, Query query);
    
    /**
     * 获取算法名称
     * @return 算法名称
     */
    String getName();
    
    /**
     * 获取算法描述
     * @return 算法描述
     */
    String getDescription();
    
    /**
     * 获取算法的配置参数
     * @return 配置参数映射
     */
    Map<String, Object> getParameters();
    
    /**
     * 设置算法参数
     * @param parameters 参数映射
     */
    void setParameters(Map<String, Object> parameters);
}