package com.wangliang.agentj.rag.relevancescoring.algorithms;

import com.wangliang.agentj.rag.common.models.Document;
import com.wangliang.agentj.rag.relevancescoring.models.RelevanceScore;
import com.wangliang.agentj.rag.queryenhancement.models.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 余弦相似度评分算法
 * 基于向量空间模型计算文档与查询的余弦相似度
 */
public class CosineSimilarityScoringAlgorithm implements RelevanceScoringAlgorithm {
    
    private static final Logger logger = LoggerFactory.getLogger(CosineSimilarityScoringAlgorithm.class);
    
    private boolean useTfIdfWeighting = true;
    private boolean normalizeVectors = true;
    
    public CosineSimilarityScoringAlgorithm() {
        this(true, true);
    }
    
    public CosineSimilarityScoringAlgorithm(boolean useTfIdfWeighting, boolean normalizeVectors) {
        this.useTfIdfWeighting = useTfIdfWeighting;
        this.normalizeVectors = normalizeVectors;
    }
    
    @Override
    public RelevanceScore score(Document document, Query query) {
        logger.info("使用余弦相似度算法计算相关性评分: document={}, query={}", 
                   document.getId(), query.getOriginalQuery());
        
        if (document == null || query == null || document.getContent() == null || query.getOriginalQuery() == null) {
            return new RelevanceScore(document != null ? document.getId() : "unknown", 
                                    query != null ? query.getOriginalQuery() : "unknown", 
                                    0.0, "CosineSimilarity");
        }
        
        String docContent = document.getContent().toLowerCase();
        String queryText = query.getOriginalQuery().toLowerCase();
        
        // 分词
        List<String> docTerms = tokenize(docContent);
        List<String> queryTerms = tokenize(queryText);
        
        // 计算词频
        Map<String, Double> docVector = createVector(docTerms);
        Map<String, Double> queryVector = createVector(queryTerms);
        
        // 计算余弦相似度
        double similarity = calculateCosineSimilarity(docVector, queryVector);
        
        RelevanceScore relevanceScore = new RelevanceScore(
            document.getId(), 
            query.getOriginalQuery(), 
            similarity, 
            "CosineSimilarity"
        );
        
        // 添加组件评分
        relevanceScore.addComponentScore("cosine_similarity", similarity);
        relevanceScore.addComponentScore("document_vector_size", (double) docVector.size());
        relevanceScore.addComponentScore("query_vector_size", (double) queryVector.size());
        relevanceScore.addComponentScore("common_terms", (double) getCommonTerms(docVector, queryVector).size());
        
        relevanceScore.setExplanation(String.format(
            "Cosine similarity: %.4f (tf-idf=%b, normalized=%b)", 
            similarity, useTfIdfWeighting, normalizeVectors
        ));
        
        logger.info("余弦相似度评分完成: similarity={}", similarity);
        return relevanceScore;
    }
    
    @Override
    public List<RelevanceScore> score(List<Document> documents, Query query) {
        logger.info("批量计算余弦相似度相关性评分: documents={}, query={}", documents.size(), query.getOriginalQuery());
        
        List<RelevanceScore> scores = new ArrayList<>();
        
        for (Document document : documents) {
            RelevanceScore score = score(document, query);
            scores.add(score);
        }
        
        // 按评分排序（降序）
        scores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        logger.info("批量余弦相似度评分完成: {} 个文档", scores.size());
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
        return "Cosine Similarity Scoring Algorithm";
    }
    
    @Override
    public String getDescription() {
        return "Cosine similarity algorithm for document relevance scoring";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("use_tf_idf", useTfIdfWeighting);
        params.put("normalize_vectors", normalizeVectors);
        return params;
    }
    
    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("use_tf_idf")) {
            this.useTfIdfWeighting = (Boolean) parameters.get("use_tf_idf");
        }
        if (parameters.containsKey("normalize_vectors")) {
            this.normalizeVectors = (Boolean) parameters.get("normalize_vectors");
        }
    }
    
    private Map<String, Double> createVector(List<String> terms) {
        Map<String, Double> vector = new HashMap<>();
        
        // 计算词频
        Map<String, Integer> termFrequency = calculateTermFrequency(terms);
        
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();
            
            double weight;
            if (useTfIdfWeighting) {
                // 简单的TF-IDF权重（在实际应用中应该使用真实的IDF值）
                double tf = 1 + Math.log(frequency);
                double idf = 1.0; // 简化处理
                weight = tf * idf;
            } else {
                // 简单的词频权重
                weight = frequency;
            }
            
            vector.put(term, weight);
        }
        
        // 归一化向量
        if (normalizeVectors) {
            normalizeVector(vector);
        }
        
        return vector;
    }
    
    private double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        if (vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }
        
        // 计算点积
        double dotProduct = 0.0;
        Set<String> commonTerms = getCommonTerms(vector1, vector2);
        
        for (String term : commonTerms) {
            dotProduct += vector1.get(term) * vector2.get(term);
        }
        
        // 计算向量的模
        double magnitude1 = calculateMagnitude(vector1);
        double magnitude2 = calculateMagnitude(vector2);
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (magnitude1 * magnitude2);
    }
    
    private Set<String> getCommonTerms(Map<String, Double> vector1, Map<String, Double> vector2) {
        Set<String> commonTerms = new HashSet<>(vector1.keySet());
        commonTerms.retainAll(vector2.keySet());
        return commonTerms;
    }
    
    private double calculateMagnitude(Map<String, Double> vector) {
        double sumOfSquares = 0.0;
        for (double value : vector.values()) {
            sumOfSquares += value * value;
        }
        return Math.sqrt(sumOfSquares);
    }
    
    private void normalizeVector(Map<String, Double> vector) {
        double magnitude = calculateMagnitude(vector);
        if (magnitude > 0) {
            for (Map.Entry<String, Double> entry : vector.entrySet()) {
                entry.setValue(entry.getValue() / magnitude);
            }
        }
    }
    
    private Map<String, Integer> calculateTermFrequency(List<String> terms) {
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String term : terms) {
            termFrequency.put(term, termFrequency.getOrDefault(term, 0) + 1);
        }
        return termFrequency;
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