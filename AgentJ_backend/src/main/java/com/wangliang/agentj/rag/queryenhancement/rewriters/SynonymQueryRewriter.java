package com.wangliang.agentj.rag.queryenhancement.rewriters;

import com.wangliang.agentj.rag.queryenhancement.models.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 同义词查询重写器
 * 通过同义词扩展来增强查询
 */
public class SynonymQueryRewriter implements QueryRewriter {
    
    private static final Logger logger = LoggerFactory.getLogger(SynonymQueryRewriter.class);
    
    private final Map<String, Set<String>> synonymDictionary;
    private final int priority;
    
    public SynonymQueryRewriter() {
        this(100);
    }
    
    public SynonymQueryRewriter(int priority) {
        this.priority = priority;
        this.synonymDictionary = new HashMap<>();
        initializeSynonymDictionary();
    }
    
    @Override
    public Query rewrite(Query query) {
        logger.info("使用同义词重写查询: {}", query.getOriginalQuery());
        
        String originalQuery = query.getOriginalQuery();
        String[] words = originalQuery.toLowerCase().split("\\s+");
        
        Set<String> expandedTerms = new HashSet<>();
        expandedTerms.addAll(Arrays.asList(words));
        
        // 为每个词添加同义词
        for (String word : words) {
            Set<String> synonyms = getSynonyms(word);
            expandedTerms.addAll(synonyms);
            
            // 添加到查询的扩展词列表中
            for (String synonym : synonyms) {
                query.addExpandedTerm(synonym);
            }
        }
        
        // 构建增强查询
        String enhancedQuery = String.join(" ", expandedTerms);
        query.setEnhancedQuery(enhancedQuery);
        
        logger.info("查询重写完成: {} -> {}", originalQuery, enhancedQuery);
        return query;
    }
    
    @Override
    public boolean supports(Query query) {
        // 支持所有类型的查询
        return query != null && query.getOriginalQuery() != null && !query.getOriginalQuery().trim().isEmpty();
    }
    
    @Override
    public String getName() {
        return "Synonym Query Rewriter";
    }
    
    @Override
    public String getDescription() {
        return "Expands queries using synonym dictionary";
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    private void initializeSynonymDictionary() {
        // 添加一些常用的同义词
        addSynonymGroup("search", "find", "look", "seek", "query");
        addSynonymGroup("document", "file", "paper", "text", "article");
        addSynonymGroup("information", "data", "content", "knowledge", "details");
        addSynonymGroup("system", "platform", "framework", "application");
        addSynonymGroup("user", "person", "individual", "customer");
        addSynonymGroup("help", "assist", "support", "aid", "guide");
        addSynonymGroup("problem", "issue", "error", "bug", "fault");
        addSynonymGroup("solution", "answer", "fix", "resolution", "remedy");
        addSynonymGroup("feature", "function", "capability", "option");
        addSynonymGroup("performance", "speed", "efficiency", "optimization");
    }
    
    private void addSynonymGroup(String... words) {
        Set<String> synonymSet = new HashSet<>(Arrays.asList(words));
        for (String word : words) {
            synonymDictionary.put(word.toLowerCase(), synonymSet);
        }
    }
    
    private Set<String> getSynonyms(String word) {
        return synonymDictionary.getOrDefault(word.toLowerCase(), Collections.emptySet());
    }
    
    public void addSynonym(String word, String synonym) {
        synonymDictionary.computeIfAbsent(word.toLowerCase(), k -> new HashSet<>())
                        .add(synonym.toLowerCase());
    }
    
    public void removeSynonym(String word, String synonym) {
        Set<String> synonyms = synonymDictionary.get(word.toLowerCase());
        if (synonyms != null) {
            synonyms.remove(synonym.toLowerCase());
            if (synonyms.isEmpty()) {
                synonymDictionary.remove(word.toLowerCase());
            }
        }
    }
}