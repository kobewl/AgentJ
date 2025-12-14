package com.wangliang.agentj.rag.qualityassessment.metrics;

import com.wangliang.agentj.rag.common.models.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 文本可读性评估指标
 * 评估文档文本的可读性和流畅程度
 */
public class TextReadabilityMetric implements QualityMetric {
    
    private static final Logger logger = LoggerFactory.getLogger(TextReadabilityMetric.class);
    
    private double weight = 1.0;
    private double threshold = 0.6;
    
    public TextReadabilityMetric() {
        this(1.0, 0.6);
    }
    
    public TextReadabilityMetric(double weight, double threshold) {
        this.weight = weight;
        this.threshold = threshold;
    }
    
    @Override
    public Map<String, Object> evaluate(Document document) {
        logger.info("开始文本可读性评估: document={}", document.getId());
        
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
        
        // 简单的可读性评估
        double avgSentenceLength = calculateAverageSentenceLength(content);
        double avgWordLength = calculateAverageWordLength(content);
        
        // 计算评分（基于句子长度和词汇长度）
        double score = calculateReadabilityScore(avgSentenceLength, avgWordLength);
        
        // 生成反馈
        StringBuilder feedback = new StringBuilder();
        feedback.append("文本可读性评估结果: ");
        feedback.append(String.format("平均句子长度 %.1f 词, 平均词汇长度 %.1f 字符", 
                                   avgSentenceLength, avgWordLength));
        
        // 检查问题
        if (avgSentenceLength > 25) {
            issues.add("句子过长，可能影响可读性");
            suggestions.add("建议将长句拆分成多个短句");
        }
        
        if (avgWordLength > 8) {
            issues.add("词汇过长，可能影响理解");
            suggestions.add("建议使用更简单的词汇");
        }
        
        result.put("score", score);
        result.put("feedback", feedback.toString());
        result.put("issues", issues);
        result.put("suggestions", suggestions);
        
        logger.info("文本可读性评估完成: score={}", String.format("%.3f", score));
        return result;
    }
    
    @Override
    public boolean supports(Document document) {
        return document != null && document.getContent() != null;
    }
    
    @Override
    public String getName() {
        return "Text Readability Metric";
    }
    
    @Override
    public String getDescription() {
        return "Evaluates the readability and fluency of document text";
    }
    
    @Override
    public String getDimension() {
        return "readability";
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
        return Arrays.asList("句子过长", "词汇过长");
    }
    
    private double calculateReadabilityScore(double avgSentenceLength, double avgWordLength) {
        // 简单的可读性评分算法
        double sentenceScore = Math.max(0.0, 1.0 - (avgSentenceLength - 15) / 30.0);
        double wordScore = Math.max(0.0, 1.0 - (avgWordLength - 5) / 10.0);
        
        return (sentenceScore * 0.6 + wordScore * 0.4);
    }
    
    private double calculateAverageSentenceLength(String text) {
        String[] sentences = text.split("[。！？.!?]");
        int totalWords = 0;
        int validSentences = 0;
        
        for (String sentence : sentences) {
            if (sentence.trim().length() > 5) {
                int words = countWords(sentence);
                totalWords += words;
                validSentences++;
            }
        }
        
        return validSentences > 0 ? (double) totalWords / validSentences : 0.0;
    }
    
    private double calculateAverageWordLength(String text) {
        String[] words = text.replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        int totalLength = 0;
        int validWords = 0;
        
        for (String word : words) {
            if (word.length() > 1) {
                totalLength += word.length();
                validWords++;
            }
        }
        
        return validWords > 0 ? (double) totalLength / validWords : 0.0;
    }
    
    private int countWords(String text) {
        String[] words = text.replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        int count = 0;
        for (String word : words) {
            if (word.length() > 1) {
                count++;
            }
        }
        
        return count;
    }
}