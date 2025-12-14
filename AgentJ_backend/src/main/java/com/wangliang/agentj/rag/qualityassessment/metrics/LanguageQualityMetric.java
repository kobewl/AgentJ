package com.wangliang.agentj.rag.qualityassessment.metrics;

import com.wangliang.agentj.rag.common.models.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 语言质量评估指标
 * 评估文档的语言质量和语法正确性
 */
public class LanguageQualityMetric implements QualityMetric {
    
    private static final Logger logger = LoggerFactory.getLogger(LanguageQualityMetric.class);
    
    private double weight = 1.0;
    private double threshold = 0.6;
    
    public LanguageQualityMetric() {
        this(1.0, 0.6);
    }
    
    public LanguageQualityMetric(double weight, double threshold) {
        this.weight = weight;
        this.threshold = threshold;
    }
    
    @Override
    public Map<String, Object> evaluate(Document document) {
        logger.info("开始语言质量评估: document={}", document.getId());
        
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
        
        // 简单的语言质量评估
        double grammarScore = checkGrammar(content);
        double spellingScore = checkSpelling(content);
        double punctuationScore = checkPunctuation(content);
        
        // 计算评分
        double score = (grammarScore * 0.4 + spellingScore * 0.3 + punctuationScore * 0.3);
        
        // 生成反馈
        StringBuilder feedback = new StringBuilder();
        feedback.append("语言质量评估结果: ");
        feedback.append(String.format("语法: %.1f%%, 拼写: %.1f%%, 标点: %.1f%%", 
                                   grammarScore * 100, spellingScore * 100, punctuationScore * 100));
        
        // 检查问题
        if (grammarScore < 0.8) {
            issues.add("语法错误较多");
            suggestions.add("建议检查语法并进行修正");
        }
        
        if (spellingScore < 0.9) {
            issues.add("拼写错误较多");
            suggestions.add("建议使用拼写检查工具");
        }
        
        if (punctuationScore < 0.8) {
            issues.add("标点符号使用不当");
            suggestions.add("建议检查标点符号的使用");
        }
        
        result.put("score", score);
        result.put("feedback", feedback.toString());
        result.put("issues", issues);
        result.put("suggestions", suggestions);
        
        logger.info("语言质量评估完成: score={}", String.format("%.3f", score));
        return result;
    }
    
    @Override
    public boolean supports(Document document) {
        return document != null && document.getContent() != null;
    }
    
    @Override
    public String getName() {
        return "Language Quality Metric";
    }
    
    @Override
    public String getDescription() {
        return "Evaluates the language quality and grammatical correctness of documents";
    }
    
    @Override
    public String getDimension() {
        return "language";
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
        return Arrays.asList("语法错误较多", "拼写错误较多", "标点符号使用不当");
    }
    
    private double checkGrammar(String text) {
        // 简单的语法检查（检查常见的语法错误模式）
        int errorCount = 0;
        
        // 检查重复的标点符号
        if (text.matches(".*[。！？.!?]{2,}.*")) {
            errorCount++;
        }
        
        // 检查空格使用
        if (text.matches(".*\\s{2,}.*")) {
            errorCount++;
        }
        
        // 检查句首空格
        String[] lines = text.split("\\n");
        for (String line : lines) {
            if (line.trim().length() > 0 && line.startsWith(" ")) {
                errorCount++;
            }
        }
        
        // 基于错误数量计算分数
        int totalChecks = lines.length + 2;
        return Math.max(0.0, 1.0 - (double) errorCount / totalChecks);
    }
    
    private double checkSpelling(String text) {
        // 简单的拼写检查（检查常见的拼写错误）
        String[] words = text.replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        int totalWords = 0;
        int suspiciousWords = 0;
        
        for (String word : words) {
            if (word.length() > 3) {
                totalWords++;
                
                // 检查是否有重复的字母（可能是拼写错误）
                if (word.matches(".*(.)\\1{2,}.*")) {
                    suspiciousWords++;
                }
                
                // 检查是否有不合理的字符组合
                if (word.matches(".*[bcdfghjklmnpqrstvwxyz]{4,}.*")) {
                    suspiciousWords++;
                }
            }
        }
        
        return totalWords > 0 ? Math.max(0.0, 1.0 - (double) suspiciousWords / totalWords) : 1.0;
    }
    
    private double checkPunctuation(String text) {
        // 简单的标点符号检查
        int punctuationCount = 0;
        int sentenceCount = 0;
        
        String[] sentences = text.split("[。！？.!?]");
        
        for (String sentence : sentences) {
            if (sentence.trim().length() > 10) {
                sentenceCount++;
                // 检查句子末尾是否有标点
                if (sentence.trim().length() > 0) {
                    punctuationCount++;
                }
            }
        }
        
        return sentenceCount > 0 ? (double) punctuationCount / sentenceCount : 1.0;
    }
}