package com.wangliang.agentj.rag.textsplitting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 语义文本分割器
 * 基于语义相似度进行智能分割，保持语义连贯性
 */
public class SemanticTextSplitter extends AbstractTextSplitter {

    private static final Logger logger = LoggerFactory.getLogger(SemanticTextSplitter.class);
    
    // 语义分析相关参数
    private double similarityThreshold;
    private int windowSize;
    private boolean preserveBoundaries;
    
    /**
     * 默认构造函数
     */
    public SemanticTextSplitter() {
        super();
        this.name = "Semantic Text Splitter";
        this.description = "Splits text based on semantic similarity to maintain semantic coherence";
        this.similarityThreshold = 0.7;
        this.windowSize = 3;
        this.preserveBoundaries = true;
    }
    
    /**
     * 带配置的构造函数
     */
    public SemanticTextSplitter(SplitterConfig config) {
        super(config);
        this.name = "Semantic Text Splitter";
        this.description = "Splits text based on semantic similarity to maintain semantic coherence";
        this.similarityThreshold = config.getSemanticThreshold();
        this.windowSize = config.getSemanticWindowSize();
        this.preserveBoundaries = config.isPreserveSentenceBoundary();
    }
    
    @Override
    protected List<TextChunk> performSplit(String text, String documentId, Object metadata) {
        logger.info("Starting semantic text splitting for document: {}", documentId);
        
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Text is null or empty");
            return Collections.emptyList();
        }
        
        // 将文本分割成句子或段落
        List<String> segments = segmentText(text);
        logger.info("Segmented text into {} segments", segments.size());
        
        if (segments.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 执行语义分割
        List<List<String>> semanticGroups = performSemanticGrouping(segments);
        logger.info("Created {} semantic groups", semanticGroups.size());
        
        // 将语义组转换为文本块
        return createSemanticChunks(semanticGroups, documentId, metadata, text);
    }
    
    /**
     * 将文本分割成基础段落或句子
     */
    private List<String> segmentText(String text) {
        List<String> segments = new ArrayList<>();
        
        if (preserveBoundaries) {
            // 优先按段落分割
            String[] paragraphs = text.split("\n\\s*\\n");
            
            for (String paragraph : paragraphs) {
                paragraph = paragraph.trim();
                if (!paragraph.isEmpty()) {
                    // 如果段落太长，进一步按句子分割
                    if (paragraph.length() > config.getChunkSize() * 0.8) {
                        List<String> sentences = splitIntoSentences(paragraph);
                        segments.addAll(sentences);
                    } else {
                        segments.add(paragraph);
                    }
                }
            }
        } else {
            // 直接按句子分割
            segments = splitIntoSentences(text);
        }
        
        return segments;
    }
    
    /**
     * 将文本分割成句子
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // 定义句子结束标点
        String sentencePattern = "[.!?。！？]";
        String[] parts = text.split(sentencePattern);
        
        int currentPos = 0;
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // 找到对应的结束标点
                int endPos = text.indexOf(part, currentPos) + part.length();
                if (endPos < text.length()) {
                    char endChar = text.charAt(endPos);
                    if (".!?。！？".indexOf(endChar) != -1) {
                        part += endChar;
                    }
                }
                sentences.add(part);
            }
        }
        
        return sentences;
    }
    
    /**
     * 执行语义分组
     */
    private List<List<String>> performSemanticGrouping(List<String> segments) {
        List<List<String>> groups = new ArrayList<>();
        
        if (segments.isEmpty()) {
            return groups;
        }
        
        List<String> currentGroup = new ArrayList<>();
        currentGroup.add(segments.get(0));
        
        for (int i = 1; i < segments.size(); i++) {
            String currentSegment = segments.get(i);
            String previousSegment = segments.get(i - 1);
            
            // 计算语义相似度（简化实现）
            double similarity = calculateSemanticSimilarity(currentSegment, previousSegment);
            
            if (similarity >= similarityThreshold) {
                // 相似度高，加入当前组
                currentGroup.add(currentSegment);
            } else {
                // 相似度低，开始新组
                if (!currentGroup.isEmpty()) {
                    groups.add(new ArrayList<>(currentGroup));
                }
                currentGroup = new ArrayList<>();
                currentGroup.add(currentSegment);
            }
        }
        
        // 添加最后一组
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        
        return groups;
    }
    
    /**
     * 计算语义相似度（简化实现）
     * 实际应用中可以使用更复杂的NLP技术
     */
    private double calculateSemanticSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        // 1. 词汇重叠度
        Set<String> words1 = extractKeyWords(text1);
        Set<String> words2 = extractKeyWords(text2);
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        // 计算交集
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        double overlapScore = (double) intersection.size() / Math.max(words1.size(), words2.size());
        
        // 2. 主题词匹配
        double topicScore = calculateTopicSimilarity(text1, text2);
        
        // 3. 长度相似度
        double lengthScore = calculateLengthSimilarity(text1, text2);
        
        // 综合评分
        return overlapScore * 0.5 + topicScore * 0.3 + lengthScore * 0.2;
    }
    
    /**
     * 提取关键词
     */
    private Set<String> extractKeyWords(String text) {
        Set<String> keywords = new HashSet<>();
        
        if (text == null || text.trim().isEmpty()) {
            return keywords;
        }
        
        // 简单的关键词提取：移除停用词和短词
        String[] words = text.toLowerCase()
                            .replaceAll("[^a-zA-Z0-9\\s\\u4e00-\\u9fff]", " ")
                            .split("\\s+");
        
        Set<String> stopWords = getStopWords();
        
        for (String word : words) {
            word = word.trim();
            if (word.length() > 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
    
    /**
     * 计算主题相似度
     */
    private double calculateTopicSimilarity(String text1, String text2) {
        // 简化的主题词提取
        List<String> topicWords1 = extractTopicWords(text1);
        List<String> topicWords2 = extractTopicWords(text2);
        
        if (topicWords1.isEmpty() || topicWords2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> topics1 = new HashSet<>(topicWords1);
        Set<String> topics2 = new HashSet<>(topicWords2);
        
        Set<String> intersection = new HashSet<>(topics1);
        intersection.retainAll(topics2);
        
        return (double) intersection.size() / Math.max(topics1.size(), topics2.size());
    }
    
    /**
     * 提取主题词
     */
    private List<String> extractTopicWords(String text) {
        List<String> topicWords = new ArrayList<>();
        
        if (text == null || text.length() < 10) {
            return topicWords;
        }
        
        // 提取名词和关键短语（简化实现）
        String[] sentences = text.split("[.!?。！？]");
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() > 5) {
                // 提取句子中的关键词
                String[] words = sentence.split("\\s+");
                for (String word : words) {
                    word = word.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "");
                    if (word.length() > 3) {
                        topicWords.add(word.toLowerCase());
                    }
                }
            }
        }
        
        return topicWords;
    }
    
    /**
     * 计算长度相似度
     */
    private double calculateLengthSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        int len1 = text1.length();
        int len2 = text2.length();
        
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }
        
        int maxLen = Math.max(len1, len2);
        int minLen = Math.min(len1, len2);
        
        return (double) minLen / maxLen;
    }
    
    /**
     * 创建语义文本块
     */
    private List<TextChunk> createSemanticChunks(List<List<String>> semanticGroups, 
                                                 String documentId, Object metadata, String originalText) {
        List<TextChunk> chunks = new ArrayList<>();
        
        int currentPos = 0;
        
        for (int i = 0; i < semanticGroups.size(); i++) {
            List<String> group = semanticGroups.get(i);
            
            // 合并组内的段落
            StringBuilder groupContent = new StringBuilder();
            for (String segment : group) {
                if (groupContent.length() > 0) {
                    groupContent.append("\n");
                }
                groupContent.append(segment);
            }
            
            String content = groupContent.toString();
            
            // 检查块大小
            if (content.length() > config.getMaxChunkSize()) {
                // 如果太大，进一步分割
                List<TextChunk> subChunks = splitLargeSemanticGroup(content, documentId, i, metadata);
                chunks.addAll(subChunks);
            } else {
                // 创建单个块
                int startPos = originalText.indexOf(content, currentPos);
                int endPos = startPos + content.length();
                
                TextChunk chunk = createChunk(content, documentId, chunks.size(), startPos, endPos, 
                                              createChunkMetadata(i, group.size(), content.length()));
                chunks.add(chunk);
                
                currentPos = endPos;
            }
        }
        
        return chunks;
    }
    
    /**
     * 分割过大的语义组
     */
    private List<TextChunk> splitLargeSemanticGroup(String content, String documentId, 
                                                     int groupIndex, Object metadata) {
        List<TextChunk> subChunks = new ArrayList<>();
        
        int targetSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();
        
        int start = 0;
        int chunkIndex = 0;
        
        while (start < content.length()) {
            int end = Math.min(start + targetSize, content.length());
            
            // 寻找合适的分割点
            if (end < content.length()) {
                end = findSplitPosition(content, end, true);
            }
            
            String chunkContent = content.substring(start, end);
            
            // 创建子块
            TextChunk subChunk = createChunk(chunkContent, documentId, 
                                           subChunks.size(), start, end,
                                           createSubChunkMetadata(groupIndex, chunkIndex, chunkContent.length()));
            subChunks.add(subChunk);
            
            // 移动到下一个位置（考虑重叠）
            start = end - overlap;
            if (start >= end) { // 防止无限循环
                start = end;
            }
            chunkIndex++;
        }
        
        return subChunks;
    }
    
    /**
     * 创建块元数据
     */
    private Map<String, Object> createChunkMetadata(int groupIndex, int groupSize, int contentLength) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("semantic_group_index", groupIndex);
        metadata.put("semantic_group_size", groupSize);
        metadata.put("semantic_splitter", true);
        metadata.put("content_length", contentLength);
        return metadata;
    }
    
    /**
     * 创建子块元数据
     */
    private Map<String, Object> createSubChunkMetadata(int groupIndex, int subIndex, int contentLength) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("semantic_group_index", groupIndex);
        metadata.put("sub_chunk_index", subIndex);
        metadata.put("semantic_splitter", true);
        metadata.put("large_group_split", true);
        metadata.put("content_length", contentLength);
        return metadata;
    }
    
    /**
     * 获取停用词列表
     */
    private Set<String> getStopWords() {
        Set<String> stopWords = new HashSet<>();
        
        // 英文停用词
        stopWords.addAll(Arrays.asList("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should"));
        
        // 中文停用词
        stopWords.addAll(Arrays.asList("的", "了", "在", "是", "我", "你", "他", "她", "它", "我们", "你们", "他们", "这", "那", "这些", "那些", "和", "与", "或", "但", "而"));
        
        return stopWords;
    }
}