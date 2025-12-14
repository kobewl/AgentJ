package com.wangliang.agentj.rag.qualityassessment.metrics;

import com.wangliang.agentj.rag.common.models.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 文档结构评估指标
 * 评估文档的结构完整性和组织合理性
 */
public class DocumentStructureMetric implements QualityMetric {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentStructureMetric.class);
    
    private double weight = 1.0;
    private double threshold = 0.6;
    
    public DocumentStructureMetric() {
        this(1.0, 0.6);
    }
    
    public DocumentStructureMetric(double weight, double threshold) {
        this.weight = weight;
        this.threshold = threshold;
    }
    
    @Override
    public Map<String, Object> evaluate(Document document) {
        logger.info("开始文档结构评估: document={}", document.getId());
        
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
        
        // 简单的结构评估
        boolean hasTitle = checkHasTitle(content);
        boolean hasSections = checkHasSections(content);
        boolean hasConclusion = checkHasConclusion(content);
        
        // 计算评分
        double score = calculateStructureScore(hasTitle, hasSections, hasConclusion);
        
        // 生成反馈
        StringBuilder feedback = new StringBuilder();
        feedback.append("文档结构评估结果: ");
        feedback.append(String.format("标题: %s, 章节: %s, 结论: %s", 
                                   hasTitle ? "有" : "无", 
                                   hasSections ? "有" : "无", 
                                   hasConclusion ? "有" : "无"));
        
        // 检查问题
        if (!hasTitle) {
            issues.add("缺少标题");
            suggestions.add("建议添加清晰的标题");
        }
        
        if (!hasSections) {
            issues.add("缺少章节结构");
            suggestions.add("建议使用标题或分段来组织内容");
        }
        
        if (!hasConclusion) {
            issues.add("缺少结论或总结");
            suggestions.add("建议添加结论或总结部分");
        }
        
        result.put("score", score);
        result.put("feedback", feedback.toString());
        result.put("issues", issues);
        result.put("suggestions", suggestions);
        
        logger.info("文档结构评估完成: score={}", String.format("%.3f", score));
        return result;
    }
    
    @Override
    public boolean supports(Document document) {
        return document != null && document.getContent() != null;
    }
    
    @Override
    public String getName() {
        return "Document Structure Metric";
    }
    
    @Override
    public String getDescription() {
        return "Evaluates the structural integrity and organization of documents";
    }
    
    @Override
    public String getDimension() {
        return "structure";
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
        return Arrays.asList("缺少标题", "缺少章节结构", "缺少结论或总结");
    }
    
    private double calculateStructureScore(boolean hasTitle, boolean hasSections, boolean hasConclusion) {
        double titleScore = hasTitle ? 0.4 : 0.0;
        double sectionScore = hasSections ? 0.3 : 0.0;
        double conclusionScore = hasConclusion ? 0.3 : 0.0;
        
        return titleScore + sectionScore + conclusionScore;
    }
    
    private boolean checkHasTitle(String content) {
        // 简单的标题检测（检查开头是否有大段文字）
        String firstLine = content.split("\\n")[0].trim();
        return firstLine.length() > 5 && firstLine.length() < 100;
    }
    
    private boolean checkHasSections(String content) {
        // 检查是否有章节结构（多段落或标题）
        String[] lines = content.split("\\n");
        return lines.length > 3; // 至少3行
    }
    
    private boolean checkHasConclusion(String content) {
        // 简单的结论检测（检查末尾是否有总结性内容）
        String[] lines = content.split("\\n");
        if (lines.length > 0) {
            String lastParagraph = lines[lines.length - 1].trim();
            return lastParagraph.length() > 20;
        }
        return false;
    }
}