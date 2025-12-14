package com.wangliang.agentj.rag;

import com.wangliang.agentj.rag.common.models.Document;
import com.wangliang.agentj.rag.documentloader.loaders.DocumentLoader;
import com.wangliang.agentj.rag.documentloader.parsers.*;
import com.wangliang.agentj.rag.queryenhancement.models.Query;
import com.wangliang.agentj.rag.queryenhancement.rewriters.SynonymQueryRewriter;
import com.wangliang.agentj.rag.queryenhancement.keywordextractors.TfIdfKeywordExtractor;
import com.wangliang.agentj.rag.relevancescoring.algorithms.BM25ScoringAlgorithm;
import com.wangliang.agentj.rag.relevancescoring.algorithms.CosineSimilarityScoringAlgorithm;
import com.wangliang.agentj.rag.relevancescoring.models.RelevanceScore;
import com.wangliang.agentj.rag.qualityassessment.evaluators.ComprehensiveQualityEvaluator;
import com.wangliang.agentj.rag.qualityassessment.models.QualityAssessmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * RAG系统使用示例
 * 演示如何配置和使用完整的RAG系统
 */
public class RAGSystemExample {
    
    private static final Logger logger = LoggerFactory.getLogger(RAGSystemExample.class);
    
    public static void main(String[] args) {
        logger.info("开始RAG系统演示");
        
        try {
            // 1. 创建RAG系统
            RAGSystem ragSystem = createRAGSystem();
            
            // 2. 加载示例文档
            loadSampleDocuments(ragSystem);
            
            // 3. 执行查询测试
            testRAGSystem(ragSystem);
            
            // 4. 显示系统统计信息
            displaySystemStatistics(ragSystem);
            
            logger.info("RAG系统演示完成");
            
        } catch (Exception e) {
            logger.error("RAG系统演示失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建并配置RAG系统
     */
    private static RAGSystem createRAGSystem() {
        logger.info("创建RAG系统");
        
        DefaultRAGSystem ragSystem = new DefaultRAGSystem();
        
        // 配置系统
        RAGConfiguration config = new RAGConfiguration();
        config.setQueryEnhancementEnabled(true);
        config.setQualityAssessmentEnabled(true);
        config.setCachingEnabled(true);
        config.setParameter("bm25_k1", 1.2);
        config.setParameter("bm25_b", 0.75);
        config.setParameter("max_keywords", 10);
        config.setParameter("min_quality_score", 0.6);
        
        ragSystem.updateConfiguration(config);
        
        // 设置查询处理组件
        ragSystem.setQueryRewriter(new SynonymQueryRewriter());
        ragSystem.setKeywordExtractor(new TfIdfKeywordExtractor());
        
        // 设置评分算法
        ragSystem.addScoringAlgorithm(new BM25ScoringAlgorithm());
        ragSystem.addScoringAlgorithm(new CosineSimilarityScoringAlgorithm());
        
        // 设置质量评估器
        ragSystem.setQualityEvaluator(new ComprehensiveQualityEvaluator());
        
        logger.info("RAG系统创建完成");
        return ragSystem;
    }
    
    /**
     * 加载示例文档
     */
    private static void loadSampleDocuments(RAGSystem ragSystem) {
        logger.info("加载示例文档");
        
        List<Document> documents = new ArrayList<>();
        
        // 创建示例文档
        Document doc1 = new Document();
        doc1.setId("doc001");
        doc1.setTitle("人工智能简介");
        doc1.setContent("人工智能是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。" +
                       "这些任务包括学习、推理、问题解决、感知和语言理解。人工智能技术在医疗、金融、" +
                       "交通、教育等领域都有广泛应用。机器学习是人工智能的一个重要子领域。");
        doc1.setFileType("text");
        doc1.setLanguage("zh");
        doc1.setCreatedTime(LocalDateTime.now());
        documents.add(doc1);
        
        Document doc2 = new Document();
        doc2.setId("doc002");
        doc2.setTitle("机器学习基础");
        doc2.setContent("机器学习是一种人工智能技术，它使计算机能够从数据中学习并做出预测或决策，" +
                       "而不需要明确编程。机器学习算法可以分为监督学习、无监督学习和强化学习。" +
                       "深度学习是机器学习的一个分支，使用神经网络来模拟人脑的学习过程。");
        doc2.setFileType("text");
        doc2.setLanguage("zh");
        doc2.setCreatedTime(LocalDateTime.now());
        documents.add(doc2);
        
        Document doc3 = new Document();
        doc3.setId("doc003");
        doc3.setTitle("深度学习概述");
        doc3.setContent("深度学习是机器学习的一个子领域，它使用多层神经网络来学习数据的复杂模式。" +
                       "深度学习在图像识别、自然语言处理、语音识别等领域取得了突破性进展。" +
                       "卷积神经网络(CNN)和循环神经网络(RNN)是两种常见的深度学习架构。");
        doc3.setFileType("text");
        doc3.setLanguage("zh");
        doc3.setCreatedTime(LocalDateTime.now());
        documents.add(doc3);
        
        // 加载文档到系统
        int loadedCount = ragSystem.loadDocuments(documents);
        logger.info("成功加载 {} 个文档", loadedCount);
    }
    
    /**
     * 测试RAG系统
     */
    private static void testRAGSystem(RAGSystem ragSystem) {
        logger.info("开始测试RAG系统");
        
        // 测试查询
        String[] testQueries = {
            "什么是人工智能",
            "机器学习的基本概念",
            "深度学习的应用",
            "AI和机器学习的关系"
        };
        
        for (String query : testQueries) {
            logger.info("\n=== 测试查询: {} ===", query);
            
            // 执行RAG流程
            String answer = ragSystem.executeRAG(query, 3);
            
            logger.info("查询: {}", query);
            logger.info("回答: {}", answer);
            
            // 显示查询处理详情
            displayQueryProcessingDetails(ragSystem, query);
        }
    }
    
    /**
     * 显示查询处理详情
     */
    private static void displayQueryProcessingDetails(RAGSystem ragSystem, String query) {
        logger.info("\n--- 查询处理详情 ---");
        
        // 处理查询以获取详细信息
        Query processedQuery = ragSystem.processQuery(query);
        
        logger.info("原始查询: {}", processedQuery.getOriginalQuery());
        logger.info("增强查询: {}", processedQuery.getEnhancedQuery());
        logger.info("关键词: {}", processedQuery.getKeywords());
        logger.info("意图: {}", processedQuery.getIntent());
        logger.info("置信度: {}", processedQuery.getConfidence());
        
        // 检索相关文档
        List<RelevanceScore> relevanceScores = ragSystem.retrieveDocuments(processedQuery, 3);
        logger.info("检索到 {} 个相关文档", relevanceScores.size());
        
        for (int i = 0; i < relevanceScores.size(); i++) {
            RelevanceScore score = relevanceScores.get(i);
            logger.info("  文档 {}: {} (评分: {:.4f})", 
                       i + 1, score.getDocumentId(), score.getScore());
            logger.info("  解释: {}", score.getExplanation());
        }
        
        // 质量评估
        List<Document> documents = new ArrayList<>();
        for (RelevanceScore score : relevanceScores) {
            // 这里应该根据documentId获取实际的文档对象
            // 为了演示，我们创建简化的文档
            Document doc = new Document();
            doc.setId(score.getDocumentId());
            doc.setContent("示例文档内容");
            documents.add(doc);
        }
        
        List<QualityAssessmentResult> qualityResults = ragSystem.assessDocumentQuality(documents);
        logger.info("质量评估结果:");
        for (int i = 0; i < qualityResults.size(); i++) {
            QualityAssessmentResult quality = qualityResults.get(i);
            logger.info("  文档 {}: 综合评分 {:.2f}", i + 1, quality.getOverallScore());
        }
    }
    
    /**
     * 显示系统统计信息
     */
    private static void displaySystemStatistics(RAGSystem ragSystem) {
        logger.info("\n=== 系统统计信息 ===");
        
        RAGStatistics stats = ragSystem.getStatistics();
        RAGSystemStatus status = ragSystem.getStatus();
        
        logger.info("系统状态: {}", status.getStatusMessage());
        logger.info("是否初始化: {}", status.isInitialized());
        logger.info("是否就绪: {}", status.isReady());
        logger.info("已加载文档: {}", status.getLoadedDocuments());
        
        logger.info("总查询数: {}", stats.getTotalQueries());
        logger.info("成功查询: {}", stats.getSuccessfulQueries());
        logger.info("失败查询: {}", stats.getFailedQueries());
        logger.info("平均响应时间: {} ms", stats.getAverageResponseTime());
        logger.info("总文档数: {}", stats.getTotalDocuments());
        logger.info("平均质量分数: {}", stats.getAverageQualityScore());
        
        logger.info("成功率: {:.2%}", stats.getSuccessRate());
        logger.info("高质量文档比例: {:.2%}", stats.getQualityDistribution());
    }
}