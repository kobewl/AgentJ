package com.wangliang.agentj.rag.queryenhancement.keywordextractors;

import com.wangliang.agentj.rag.queryenhancement.models.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TF-IDF关键词提取器
 * 基于TF-IDF算法从查询中提取关键词
 */
public class TfIdfKeywordExtractor implements KeywordExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(TfIdfKeywordExtractor.class);
    
    // 停用词列表
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "must", "can", "this", "that", "these", "those",
        "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his", "her", "its", "our", "their"
    ));
    
    // 中文停用词
    private static final Set<String> CHINESE_STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"
    ));
    
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    
    private final double confidenceThreshold;
    private final Map<String, Double> idfValues;
    
    public TfIdfKeywordExtractor() {
        this(0.1);
    }
    
    public TfIdfKeywordExtractor(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
        this.idfValues = new HashMap<>();
        initializeDefaultIdfValues();
    }
    
    @Override
    public List<String> extractKeywords(Query query) {
        if (query == null || query.getOriginalQuery() == null) {
            return Collections.emptyList();
        }
        return extractKeywords(query.getOriginalQuery());
    }
    
    @Override
    public List<String> extractKeywords(String text) {
        logger.info("使用TF-IDF提取关键词: {}", text);
        
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 分词
        List<String> words = tokenize(text);
        
        // 计算TF-IDF值
        Map<String, Double> tfIdfScores = calculateTfIdf(words);
        
        // 过滤和排序
        List<String> keywords = tfIdfScores.entrySet().stream()
            .filter(entry -> entry.getValue() >= confidenceThreshold)
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        logger.info("提取到 {} 个关键词: {}", keywords.size(), keywords);
        return keywords;
    }
    
    @Override
    public boolean supports(Query query) {
        return query != null && query.getOriginalQuery() != null && !query.getOriginalQuery().trim().isEmpty();
    }
    
    @Override
    public String getName() {
        return "TF-IDF Keyword Extractor";
    }
    
    @Override
    public String getDescription() {
        return "Extracts keywords using TF-IDF algorithm";
    }
    
    @Override
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    private void initializeDefaultIdfValues() {
        // 初始化一些常用词的IDF值
        // 在实际应用中，这些值应该基于大规模语料库计算
        idfValues.put("search", 2.5);
        idfValues.put("information", 2.3);
        idfValues.put("document", 2.1);
        idfValues.put("system", 2.0);
        idfValues.put("data", 1.9);
        idfValues.put("analysis", 2.4);
        idfValues.put("algorithm", 2.6);
        idfValues.put("optimization", 2.7);
        idfValues.put("performance", 2.2);
        idfValues.put("framework", 2.3);
    }
    
    private List<String> tokenize(String text) {
        List<String> words = new ArrayList<>();
        
        // 提取英文单词
        var englishMatcher = WORD_PATTERN.matcher(text.toLowerCase());
        while (englishMatcher.find()) {
            String word = englishMatcher.group();
            if (!STOP_WORDS.contains(word) && word.length() > 2) {
                words.add(word);
            }
        }
        
        // 提取中文字符
        var chineseMatcher = CHINESE_PATTERN.matcher(text);
        while (chineseMatcher.find()) {
            String chinese = chineseMatcher.group();
            if (!CHINESE_STOP_WORDS.contains(chinese)) {
                words.add(chinese);
            }
        }
        
        return words;
    }
    
    private Map<String, Double> calculateTfIdf(List<String> words) {
        Map<String, Double> tfScores = calculateTf(words);
        Map<String, Double> tfIdfScores = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : tfScores.entrySet()) {
            String word = entry.getKey();
            double tf = entry.getValue();
            double idf = idfValues.getOrDefault(word, 1.0);
            tfIdfScores.put(word, tf * idf);
        }
        
        return tfIdfScores;
    }
    
    private Map<String, Double> calculateTf(List<String> words) {
        Map<String, Integer> wordCount = new HashMap<>();
        
        // 计算词频
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        
        // 计算TF值
        Map<String, Double> tfScores = new HashMap<>();
        int totalWords = words.size();
        
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();
            double tf = (double) count / totalWords;
            tfScores.put(word, tf);
        }
        
        return tfScores;
    }
    
    public void updateIdfValue(String word, double idfValue) {
        idfValues.put(word, idfValue);
    }
    
    public void removeIdfValue(String word) {
        idfValues.remove(word);
    }
}