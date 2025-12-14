package com.wangliang.agentj.rag.qualityassessment.metrics;

import com.wangliang.agentj.rag.common.models.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 内容相关性评估指标
 * 评估文档内容的相关性和聚焦程度
 */
public class ContentRelevanceMetric implements QualityMetric {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentRelevanceMetric.class);
    
    private double weight = 1.0;
    private double threshold = 0.6;
    
    public ContentRelevanceMetric() {
        this(1.0, 0.6);
    }
    
    public ContentRelevanceMetric(double weight, double threshold) {
        this.weight = weight;
        this.threshold = threshold;
    }
    
    @Override
    public Map<String, Object> evaluate(Document document) {
        logger.info("开始内容相关性评估: document={}", document.getId());
        
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        String content = document.getContent();
        if (content == null || content.trim().isEmpty()) {
            result.put("score", 0.0);
            result.put("feedback", "文档内容为空");
            result.put("issues", Arrays.asList("文档内容为空"));
            result.put("suggestions", Arrays.asList("请添加文档内容"));
            return result;
        }
        
        // 简单的相关性评估（基于关键词密度和主题一致性）
        double keywordDensity = calculateKeywordDensity(content);
        double topicConsistency = calculateTopicConsistency(content);
        
        // 计算评分
        double score = (keywordDensity * 0.6 + topicConsistency * 0.4);
        
        // 生成反馈
        StringBuilder feedback = new StringBuilder();
        feedback.append("内容相关性评估结果: ");
        feedback.append(String.format("关键词密度: %.2f%%, 主题一致性: %.2f%%", 
                                   keywordDensity * 100, topicConsistency * 100));
        
        // 检查问题
        if (keywordDensity < 0.1) {
            issues.add("关键词密度偏低");
            suggestions.add("建议增加核心关键词的使用");
        }
        
        if (keywordDensity > 0.5) {
            issues.add("关键词密度过高，可能存在关键词堆砌");
            suggestions.add("建议自然使用关键词，避免过度重复");
        }
        
        if (topicConsistency < 0.7) {
            issues.add("主题一致性较低");
            suggestions.add("建议保持内容聚焦，避免偏离主题");
        }
        
        result.put("score", score);
        result.put("feedback", feedback.toString());
        result.put("issues", issues);
        result.put("suggestions", suggestions);
        
        logger.info("内容相关性评估完成: score={}", String.format("%.3f", score));
        return result;
    }
    
    @Override
    public boolean supports(Document document) {
        return document != null && document.getContent() != null;
    }
    
    @Override
    public String getName() {
        return "Content Relevance Metric";
    }
    
    @Override
    public String getDescription() {
        return "Evaluates the relevance and focus of document content";
    }
    
    @Override
    public String getDimension() {
        return "relevance";
    }
    
    @Override
    public double getWeight() {
        return weight;
    }
    
    @Override
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    @Override
    public double getThreshold() {
        return threshold;
    }
    
    @Override
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        return params;
    }
    
    @Override
    public void setParameters(Map<String, Object> parameters) {
        // 无特殊参数
    }
    
    @Override
    public List<String> getSupportedIssueTypes() {
        return Arrays.asList("关键词密度偏低", "关键词密度过高，可能存在关键词堆砌", "主题一致性较低");
    }
    
    private double calculateKeywordDensity(String text) {
        // 简单的关键词密度计算
        String[] words = text.toLowerCase()
                            .replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        // 提取关键词（频率最高的词）
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String word : words) {
            if (word.length() > 2) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }
        }
        
        // 计算关键词密度（基于高频词）
        int totalWords = words.length;
        int keywordWords = 0;
        
        // 取频率最高的前20%的词作为关键词
        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordFrequency.entrySet());
        sortedWords.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int keywordCount = Math.max(1, sortedWords.size() / 5);
        for (int i = 0; i < keywordCount && i < sortedWords.size(); i++) {
            keywordWords += sortedWords.get(i).getValue();
        }
        
        return totalWords > 0 ? (double) keywordWords / totalWords : 0.0;
    }
    
    private double calculateTopicConsistency(String text) {
        // 简单的主题一致性计算（基于词汇重复率）
        String[] sentences = text.split("[。！？.!?]");
        if (sentences.length < 2) {
            return 1.0;
        }
        
        // 提取每个句子的关键词
        List<Set<String>> sentenceKeywords = new ArrayList<>();
        for (String sentence : sentences) {
            if (sentence.trim().length() > 5) {
                Set<String> keywords = extractKeywords(sentence);
                sentenceKeywords.add(keywords);
            }
        }
        
        // 计算句子间的相似度
        double totalSimilarity = 0.0;
        int comparisonCount = 0;
        
        for (int i = 0; i < sentenceKeywords.size() - 1; i++) {
            for (int j = i + 1; j < sentenceKeywords.size(); j++) {
                double similarity = calculateSetSimilarity(sentenceKeywords.get(i), sentenceKeywords.get(j));
                totalSimilarity += similarity;
                comparisonCount++;
            }
        }
        
        return comparisonCount > 0 ? totalSimilarity / comparisonCount : 1.0;
    }
    
    private Set<String> extractKeywords(String text) {
        String[] words = text.toLowerCase()
                            .replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        Set<String> keywords = new HashSet<>();
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        for (String word : words) {
            if (word.length() > 2) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }
        }
        
        // 取频率最高的词作为关键词
        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordFrequency.entrySet());
        sortedWords.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int keywordCount = Math.min(5, sortedWords.size());
        for (int i = 0; i < keywordCount && i < sortedWords.size(); i++) {
            keywords.add(sortedWords.get(i).getKey());
        }
        
        return keywords;
    }
    
    private double calculateSetSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.size() > 0 ? (double) intersection.size() / union.size() : 0.0;
    }
}