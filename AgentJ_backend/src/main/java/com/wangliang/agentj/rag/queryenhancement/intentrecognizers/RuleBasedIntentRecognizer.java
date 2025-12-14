package com.wangliang.agentj.rag.queryenhancement.intentrecognizers;

import com.wangliang.agentj.rag.queryenhancement.models.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 基于规则的意图识别器
 * 使用预定义的规则和模式来识别用户查询意图
 */
public class RuleBasedIntentRecognizer implements IntentRecognizer {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleBasedIntentRecognizer.class);
    
    private final Map<String, List<IntentRule>> intentRules;
    private final double confidenceThreshold;
    
    public RuleBasedIntentRecognizer() {
        this(0.6);
    }
    
    public RuleBasedIntentRecognizer(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
        this.intentRules = new HashMap<>();
        initializeDefaultRules();
    }
    
    @Override
    public IntentRecognitionResult recognize(Query query) {
        if (query == null || query.getOriginalQuery() == null) {
            return new IntentRecognitionResult("unknown", 0.0);
        }
        return recognize(query.getOriginalQuery());
    }
    
    @Override
    public IntentRecognitionResult recognize(String text) {
        logger.info("识别查询意图: {}", text);
        
        if (text == null || text.trim().isEmpty()) {
            return new IntentRecognitionResult("unknown", 0.0);
        }
        
        String lowerText = text.toLowerCase();
        Map<String, Double> intentScores = new HashMap<>();
        
        // 为每个意图计算匹配分数
        for (Map.Entry<String, List<IntentRule>> entry : intentRules.entrySet()) {
            String intent = entry.getKey();
            List<IntentRule> rules = entry.getValue();
            
            double maxScore = 0.0;
            for (IntentRule rule : rules) {
                double score = rule.match(lowerText);
                if (score > maxScore) {
                    maxScore = score;
                }
            }
            
            if (maxScore > 0) {
                intentScores.put(intent, maxScore);
            }
        }
        
        // 找到最高分数的意图
        if (intentScores.isEmpty()) {
            return new IntentRecognitionResult("general", 0.3);
        }
        
        Map.Entry<String, Double> bestIntent = intentScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);
        
        if (bestIntent != null && bestIntent.getValue() >= confidenceThreshold) {
            IntentRecognitionResult result = new IntentRecognitionResult(
                bestIntent.getKey(), 
                bestIntent.getValue()
            );
            result.setDescription("Rule-based intent recognition");
            
            logger.info("意图识别完成: {} (置信度: {})", bestIntent.getKey(), bestIntent.getValue());
            return result;
        }
        
        return new IntentRecognitionResult("general", 0.3);
    }
    
    @Override
    public boolean supports(Query query) {
        return query != null && query.getOriginalQuery() != null && !query.getOriginalQuery().trim().isEmpty();
    }
    
    @Override
    public String getName() {
        return "Rule-Based Intent Recognizer";
    }
    
    @Override
    public String getDescription() {
        return "Recognizes user intent using predefined rules and patterns";
    }
    
    @Override
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    private void initializeDefaultRules() {
        // 信息查询意图
        addIntentRule("information_retrieval", 
            new KeywordRule(Arrays.asList("搜索", "查找", "查询", "search", "find", "lookup", "query"), 0.8),
            new KeywordRule(Arrays.asList("信息", "资料", "数据", "information", "data", "material"), 0.7),
            new PatternRule(".*?(搜索|查找|查询).*?", 0.8)
        );
        
        // 问题解答意图
        addIntentRule("question_answering",
            new KeywordRule(Arrays.asList("什么", "怎么", "如何", "为什么", "哪里", "when", "where", "how", "what", "why"), 0.9),
            new KeywordRule(Arrays.asList("?", "？"), 0.6),
            new PatternRule(".*?[？?].*?", 0.7)
        );
        
        // 文档处理意图
        addIntentRule("document_processing",
            new KeywordRule(Arrays.asList("文档", "文件", "file", "document", "pdf", "word", "excel"), 0.8),
            new KeywordRule(Arrays.asList("上传", "下载", "导入", "导出", "upload", "download", "import", "export"), 0.7),
            new PatternRule(".*?(文档|文件|pdf|word).*?", 0.8)
        );
        
        // 系统帮助意图
        addIntentRule("system_help",
            new KeywordRule(Arrays.asList("帮助", "协助", "支持", "help", "support", "assist"), 0.8),
            new KeywordRule(Arrays.asList("教程", "指南", "说明", "guide", "tutorial", "manual"), 0.7),
            new PatternRule(".*?(帮助|教程|指南).*?", 0.8)
        );
        
        // 分析比较意图
        addIntentRule("analysis_comparison",
            new KeywordRule(Arrays.asList("分析", "比较", "对比", "evaluate", "compare", "analyze"), 0.8),
            new KeywordRule(Arrays.asList("vs", "versus", "比较", "对比"), 0.7),
            new PatternRule(".*?(分析|比较|对比).*?", 0.8)
        );
        
        // 配置设置意图
        addIntentRule("configuration_setup",
            new KeywordRule(Arrays.asList("配置", "设置", "setup", "configure", "setting"), 0.8),
            new KeywordRule(Arrays.asList("参数", "选项", "option", "parameter"), 0.7),
            new PatternRule(".*?(配置|设置).*?", 0.8)
        );
    }
    
    public void addIntentRule(String intent, IntentRule... rules) {
        intentRules.computeIfAbsent(intent, k -> new ArrayList<>()).addAll(Arrays.asList(rules));
    }
    
    public void removeIntentRule(String intent) {
        intentRules.remove(intent);
    }
    
    public void clearAllRules() {
        intentRules.clear();
    }
    
    // 意图规则接口
    private interface IntentRule {
        double match(String text);
    }
    
    // 关键词规则
    private static class KeywordRule implements IntentRule {
        private final List<String> keywords;
        private final double weight;
        
        public KeywordRule(List<String> keywords, double weight) {
            this.keywords = keywords;
            this.weight = weight;
        }
        
        @Override
        public double match(String text) {
            int matchCount = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            return matchCount > 0 ? Math.min(1.0, weight * matchCount / keywords.size()) : 0.0;
        }
    }
    
    // 模式规则
    private static class PatternRule implements IntentRule {
        private final Pattern pattern;
        private final double weight;
        
        public PatternRule(String regex, double weight) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.weight = weight;
        }
        
        @Override
        public double match(String text) {
            return pattern.matcher(text).find() ? weight : 0.0;
        }
    }
}