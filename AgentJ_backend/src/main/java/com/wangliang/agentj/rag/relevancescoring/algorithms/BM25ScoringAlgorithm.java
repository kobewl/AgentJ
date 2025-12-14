package com.wangliang.agentj.rag.relevancescoring.algorithms;

import com.wangliang.agentj.rag.common.models.Document;
import com.wangliang.agentj.rag.relevancescoring.models.RelevanceScore;
import com.wangliang.agentj.rag.queryenhancement.models.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * BM25相关性评分算法
 * 实现BM25算法计算文档与查询的相关性
 */
public class BM25ScoringAlgorithm implements RelevanceScoringAlgorithm {
    
    private static final Logger logger = LoggerFactory.getLogger(BM25ScoringAlgorithm.class);
    
    // BM25参数
    private double k1 = 1.2;  // 控制词频饱和度的参数
    private double b = 0.75;  // 控制文档长度归一化的参数
    private double k3 = 8.0;  // 控制查询词频的参数
    
    // 文档集合统计信息
    private int totalDocuments = 0;
    private Map<String, Integer> documentFrequency = new HashMap<>();
    private double averageDocumentLength = 0.0;
    
    public BM25ScoringAlgorithm() {
        this(1.2, 0.75, 8.0);
    }
    
    public BM25ScoringAlgorithm(double k1, double b, double k3) {
        this.k1 = k1;
        this.b = b;
        this.k3 = k3;
    }
    
    @Override
    public RelevanceScore score(Document document, Query query) {
        logger.info("使用BM25算法计算相关性评分: document={}, query={}", 
                   document.getId(), query.getOriginalQuery());
        
        if (document == null || query == null || document.getContent() == null || query.getOriginalQuery() == null) {
            return new RelevanceScore(document != null ? document.getId() : "unknown", 
                                    query != null ? query.getOriginalQuery() : "unknown", 
                                    0.0, "BM25");
        }
        
        String docContent = document.getContent().toLowerCase();
        String queryText = query.getOriginalQuery().toLowerCase();
        
        // 分词
        List<String> docTerms = tokenize(docContent);
        List<String> queryTerms = tokenize(queryText);
        
        // 计算BM25评分
        double score = calculateBM25Score(docTerms, queryTerms, document);
        
        RelevanceScore relevanceScore = new RelevanceScore(
            document.getId(), 
            query.getOriginalQuery(), 
            score, 
            "BM25"
        );
        
        // 添加组件评分
        relevanceScore.addComponentScore("bm25_score", score);
        relevanceScore.addComponentScore("document_length", (double) docTerms.size());
        relevanceScore.addComponentScore("query_term_count", (double) queryTerms.size());
        
        relevanceScore.setExplanation(String.format(
            "BM25 score: %.4f (k1=%.2f, b=%.2f, k3=%.2f)", score, k1, b, k3
        ));
        
        logger.info("BM25评分完成: score={}", score);
        return relevanceScore;
    }
    
    @Override
    public List<RelevanceScore> score(List<Document> documents, Query query) {
        logger.info("批量计算BM25相关性评分: documents={}, query={}", documents.size(), query.getOriginalQuery());
        
        List<RelevanceScore> scores = new ArrayList<>();
        
        // 更新文档集合统计信息
        updateCollectionStatistics(documents);
        
        for (Document document : documents) {
            RelevanceScore score = score(document, query);
            scores.add(score);
        }
        
        // 按评分排序（降序）
        scores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        logger.info("批量BM25评分完成: {} 个文档", scores.size());
        return scores;
    }
    
    @Override
    public boolean supports(Document document, Query query) {
        return document != null && query != null && 
               document.getContent() != null && query.getOriginalQuery() != null &&
               !document.getContent().trim().isEmpty() && !query.getOriginalQuery().trim().isEmpty();
    }
    
    @Override
    public String getName() {
        return "BM25 Scoring Algorithm";
    }
    
    @Override
    public String getDescription() {
        return "BM25 algorithm for document relevance scoring";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("k1", k1);
        params.put("b", b);
        params.put("k3", k3);
        params.put("total_documents", totalDocuments);
        params.put("avg_doc_length", averageDocumentLength);
        return params;
    }
    
    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("k1")) {
            this.k1 = ((Number) parameters.get("k1")).doubleValue();
        }
        if (parameters.containsKey("b")) {
            this.b = ((Number) parameters.get("b")).doubleValue();
        }
        if (parameters.containsKey("k3")) {
            this.k3 = ((Number) parameters.get("k3")).doubleValue();
        }
    }
    
    private double calculateBM25Score(List<String> docTerms, List<String> queryTerms, Document document) {
        double score = 0.0;
        int docLength = docTerms.size();
        
        Map<String, Integer> docTermFrequency = calculateTermFrequency(docTerms);
        Map<String, Integer> queryTermFrequency = calculateTermFrequency(queryTerms);
        
        for (Map.Entry<String, Integer> entry : queryTermFrequency.entrySet()) {
            String term = entry.getKey();
            int queryTf = entry.getValue();
            
            if (documentFrequency.containsKey(term)) {
                // IDF计算
                double idf = Math.log((totalDocuments - documentFrequency.get(term) + 0.5) / 
                                     (documentFrequency.get(term) + 0.5));
                
                // 文档词频
                int docTf = docTermFrequency.getOrDefault(term, 0);
                
                // BM25公式
                double tfComponent = (docTf * (k1 + 1)) / 
                                   (docTf + k1 * (1 - b + b * docLength / averageDocumentLength));
                
                double queryTfComponent = (queryTf * (k3 + 1)) / (queryTf + k3);
                
                score += idf * tfComponent * queryTfComponent;
            }
        }
        
        // 归一化到0-1范围
        return Math.max(0.0, Math.min(1.0, score / 10.0));
    }
    
    private Map<String, Integer> calculateTermFrequency(List<String> terms) {
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String term : terms) {
            termFrequency.put(term, termFrequency.getOrDefault(term, 0) + 1);
        }
        return termFrequency;
    }
    
    private void updateCollectionStatistics(List<Document> documents) {
        totalDocuments = documents.size();
        
        // 计算文档频率和平均文档长度
        Map<String, Integer> termDocumentCount = new HashMap<>();
        int totalLength = 0;
        
        for (Document document : documents) {
            if (document.getContent() != null) {
                List<String> terms = tokenize(document.getContent().toLowerCase());
                totalLength += terms.size();
                
                // 计算词项文档频率
                Set<String> uniqueTerms = new HashSet<>(terms);
                for (String term : uniqueTerms) {
                    termDocumentCount.put(term, termDocumentCount.getOrDefault(term, 0) + 1);
                }
            }
        }
        
        averageDocumentLength = totalDocuments > 0 ? (double) totalLength / totalDocuments : 0.0;
        documentFrequency = termDocumentCount;
        
        logger.info("集合统计信息更新完成: total_docs={}, avg_length={}", 
                   totalDocuments, String.format("%.2f", averageDocumentLength));
    }
    
    private List<String> tokenize(String text) {
        // 简单的分词实现
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 移除标点符号并分词
        String[] words = text.toLowerCase()
                            .replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        List<String> terms = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 1) { // 过滤掉单个字符
                terms.add(word);
            }
        }
        
        return terms;
    }
}