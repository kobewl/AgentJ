package com.wangliang.agentj.rag;

import com.wangliang.agentj.rag.common.models.Document;
import com.wangliang.agentj.rag.queryenhancement.models.Query;
import com.wangliang.agentj.rag.relevancescoring.models.RelevanceScore;
import com.wangliang.agentj.rag.qualityassessment.models.QualityAssessmentResult;

import java.util.List;

/**
 * RAG系统核心接口
 * 定义检索增强生成系统的标准操作接口
 */
public interface RAGSystem {
    
    /**
     * 加载文档到系统
     * @param documents 要加载的文档列表
     * @return 加载成功的文档数量
     */
    int loadDocuments(List<Document> documents);
    
    /**
     * 处理用户查询
     * @param query 用户查询
     * @return 处理后的查询结果
     */
    Query processQuery(String query);
    
    /**
     * 检索相关文档
     * @param query 处理后的查询
     * @param topK 返回前K个最相关的文档
     * @return 相关性评分结果列表
     */
    List<RelevanceScore> retrieveDocuments(Query query, int topK);
    
    /**
     * 评估文档质量
     * @param documents 要评估的文档列表
     * @return 质量评估结果列表
     */
    List<QualityAssessmentResult> assessDocumentQuality(List<Document> documents);
    
    /**
     * 生成回答
     * @param query 用户查询
     * @param relevantDocuments 相关文档
     * @return 生成的回答
     */
    String generateAnswer(String query, List<Document> relevantDocuments);
    
    /**
     * 执行完整的RAG流程
     * @param userQuery 用户查询
     * @param topK 检索文档数量
     * @return 生成的回答
     */
    String executeRAG(String userQuery, int topK);
    
    /**
     * 获取系统状态
     * @return 系统状态信息
     */
    RAGSystemStatus getStatus();
    
    /**
     * 更新系统配置
     * @param configuration 配置参数
     */
    void updateConfiguration(RAGConfiguration configuration);
    
    /**
     * 获取系统统计信息
     * @return 统计信息
     */
    RAGStatistics getStatistics();
}