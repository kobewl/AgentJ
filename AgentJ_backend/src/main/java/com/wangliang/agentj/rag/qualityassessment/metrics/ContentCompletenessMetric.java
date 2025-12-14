package com.wangliang.agentj.rag.qualityassessment.metrics;

import com.wangliang.agentj.rag.common.models.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 内容完整性评估指标
 * 评估文档内容的完整性和丰富程度
 */
public class ContentCompletenessMetric implements QualityMetric {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentCompletenessMetric.class);
    
    private double weight = 1.0;
    private double threshold = 0.6;
    
    // 配置参数
    private int minWordCount = 100;
    private int minSentenceCount = 5;
    private int minParagraphCount = 2;
    private double minContentDensity = 0.7; // 内容密度阈值
    
    public ContentCompletenessMetric() {
        this(1.0, 0.6);
    }
    
    public ContentCompletenessMetric(double weight, double threshold) {
        this.weight = weight;
        this.threshold = threshold;
    }
    
    @Override
    public Map<String, Object> evaluate(Document document) {
        logger.info("开始内容完整性评估: document={}", document.getId());
        
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
        
        // 基础统计
        int wordCount = countWords(content);
        int sentenceCount = countSentences(content);
        int paragraphCount = countParagraphs(content);
        double contentDensity = calculateContentDensity(content);
        
        logger.debug("基础统计: words={}, sentences={}, paragraphs={}, density={}", 
                    wordCount, sentenceCount, paragraphCount, String.format("%.3f", contentDensity));
        
        // 计算评分
        double score = calculateCompletenessScore(wordCount, sentenceCount, paragraphCount, contentDensity);
        
        // 生成反馈
        StringBuilder feedback = new StringBuilder();
        feedback.append("内容完整性评估结果: ");
        feedback.append(String.format("字数 %d, 句子数 %d, 段落数 %d, 内容密度 %.2f%%", 
                                    wordCount, sentenceCount, paragraphCount, contentDensity * 100));
        
        // 检查问题
        if (wordCount < minWordCount) {
            issues.add(String.format("字数不足（当前 %d，要求 %d）", wordCount, minWordCount));
            suggestions.add("建议增加更多内容，提供更详细的信息");
        }
        
        if (sentenceCount < minSentenceCount) {
            issues.add(String.format("句子数量不足（当前 %d，要求 %d）", sentenceCount, minSentenceCount));
            suggestions.add("建议增加更多句子，丰富内容表达");
        }
        
        if (paragraphCount < minParagraphCount) {
            issues.add(String.format("段落数量不足（当前 %d，要求 %d）", paragraphCount, minParagraphCount));
            suggestions.add("建议合理分段，提高内容可读性");
        }
        
        if (contentDensity < minContentDensity) {
            issues.add(String.format("内容密度偏低（当前 %.2f%%，要求 %.2f%%）", 
                                   contentDensity * 100, minContentDensity * 100));
            suggestions.add("建议减少无意义的重复内容，提高信息密度");
        }
        
        result.put("score", score);
        result.put("feedback", feedback.toString());
        result.put("issues", issues);
        result.put("suggestions", suggestions);
        
        // 添加详细统计
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("word_count", wordCount);
        statistics.put("sentence_count", sentenceCount);
        statistics.put("paragraph_count", paragraphCount);
        statistics.put("content_density", contentDensity);
        result.put("statistics", statistics);
        
        logger.info("内容完整性评估完成: score={}", String.format("%.3f", score));
        return result;
    }
    
    @Override
    public boolean supports(Document document) {
        return document != null && document.getContent() != null;
    }
    
    @Override
    public String getName() {
        return "Content Completeness Metric";
    }
    
    @Override
    public String getDescription() {
        return "Evaluates the completeness and richness of document content";
    }
    
    @Override
    public String getDimension() {
        return "completeness";
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
        params.put("min_word_count", minWordCount);
        params.put("min_sentence_count", minSentenceCount);
        params.put("min_paragraph_count", minParagraphCount);
        params.put("min_content_density", minContentDensity);
        return params;
    }
    
    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("min_word_count")) {
            this.minWordCount = ((Number) parameters.get("min_word_count")).intValue();
        }
        if (parameters.containsKey("min_sentence_count")) {
            this.minSentenceCount = ((Number) parameters.get("min_sentence_count")).intValue();
        }
        if (parameters.containsKey("min_paragraph_count")) {
            this.minParagraphCount = ((Number) parameters.get("min_paragraph_count")).intValue();
        }
        if (parameters.containsKey("min_content_density")) {
            this.minContentDensity = ((Number) parameters.get("min_content_density")).doubleValue();
        }
    }
    
    @Override
    public List<String> getSupportedIssueTypes() {
        return Arrays.asList("字数不足", "句子数量不足", "段落数量不足", "内容密度偏低");
    }
    
    private double calculateCompletenessScore(int wordCount, int sentenceCount, int paragraphCount, double contentDensity) {
        double wordScore = Math.min(1.0, (double) wordCount / minWordCount);
        double sentenceScore = Math.min(1.0, (double) sentenceCount / minSentenceCount);
        double paragraphScore = Math.min(1.0, (double) paragraphCount / minParagraphCount);
        double densityScore = Math.min(1.0, contentDensity / minContentDensity);
        
        // 加权平均
        return (wordScore * 0.4 + sentenceScore * 0.2 + paragraphScore * 0.2 + densityScore * 0.2);
    }
    
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // 移除标点符号并分词
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
    
    private int countSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // 简单的句子计数（按句号、问号、感叹号分割）
        String[] sentences = text.split("[。！？.!?]");
        
        int count = 0;
        for (String sentence : sentences) {
            if (sentence.trim().length() > 5) { // 过滤掉太短的片段
                count++;
            }
        }
        
        return Math.max(1, count); // 至少1个句子
    }
    
    private int countParagraphs(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // 按空行分割段落
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        int count = 0;
        for (String paragraph : paragraphs) {
            if (paragraph.trim().length() > 10) { // 过滤掉太短的段落
                count++;
            }
        }
        
        return Math.max(1, count); // 至少1个段落
    }
    
    private double calculateContentDensity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        // 移除空白字符
        String cleanText = text.replaceAll("\\s+", "");
        int totalChars = text.length();
        int contentChars = cleanText.length();
        
        return totalChars > 0 ? (double) contentChars / totalChars : 0.0;
    }
}