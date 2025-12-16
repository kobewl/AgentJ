package com.wangliang.agentj.rag.textsplitting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 递归字符文本分割器
 * 使用递归方式尝试不同的分隔符，优先保持语义完整性
 */
public class RecursiveCharacterSplitter extends AbstractTextSplitter {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveCharacterSplitter.class);
    
    // 默认分隔符优先级列表
    private static final List<String> DEFAULT_SEPARATORS = Arrays.asList(
        "\n\n",     // 段落分隔符（最高优先级）
        "\n",       // 换行符
        "。",       // 中文句号
        "！",       // 中文感叹号
        "？",       // 中文问号
        ".",        // 英文句号
        "!",        // 英文感叹号
        "?",        // 英文问号
        "；",       // 中文分号
        ";",        // 英文分号
        "，",       // 中文逗号
        ",",        // 英文逗号
        " "         // 空格（最低优先级）
    );
    
    private List<String> separators;
    private boolean keepSeparator;
    private int maxRecursionDepth;
    
    /**
     * 默认构造函数
     */
    public RecursiveCharacterSplitter() {
        super();
        this.name = "Recursive Character Splitter";
        this.description = "Recursively splits text using different separators to maintain semantic integrity";
        this.separators = new ArrayList<>(DEFAULT_SEPARATORS);
        this.keepSeparator = true;
        this.maxRecursionDepth = 5;
    }
    
    /**
     * 带配置的构造函数
     */
    public RecursiveCharacterSplitter(SplitterConfig config) {
        super(config);
        this.name = "Recursive Character Splitter";
        this.description = "Recursively splits text using different separators to maintain semantic integrity";
        this.separators = config.getSeparators() != null ? config.getSeparators() : new ArrayList<>(DEFAULT_SEPARATORS);
        this.keepSeparator = config.isPreserveSentenceBoundary() || config.isPreserveParagraphBoundary();
        this.maxRecursionDepth = 5;
    }
    
    @Override
    protected List<TextChunk> performSplit(String text, String documentId, Object metadata) {
        logger.info("Starting recursive character text splitting for document: {}", documentId);
        
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Text is null or empty");
            return Collections.emptyList();
        }
        
        logger.info("Text length: {} characters", text.length());
        logger.info("Using separators: {}", separators);
        
        // 使用递归分割
        List<String> splitTexts = recursiveSplit(text, 0);
        logger.info("Recursive splitting produced {} text segments", splitTexts.size());
        
        // 合并相邻的小段，确保块大小合适
        List<String> mergedTexts = mergeSmallChunks(splitTexts);
        logger.info("After merging: {} text segments", mergedTexts.size());
        
        // 创建文本块
        return createRecursiveChunks(mergedTexts, documentId, metadata, text);
    }
    
    /**
     * 递归分割文本
     */
    private List<String> recursiveSplit(String text, int separatorIndex) {
        // 递归终止条件
        if (separatorIndex >= separators.size() || text.length() <= config.getChunkSize()) {
            return Arrays.asList(text);
        }
        
        String separator = separators.get(separatorIndex);
        logger.debug("Trying separator '{}' at depth {}", separator, separatorIndex);
        
        // 使用当前分隔符分割文本
        List<String> splits = splitBySeparator(text, separator);
        logger.debug("Split by '{}' produced {} segments", separator, splits.size());
        
        // 检查结果
        List<String> result = new ArrayList<>();
        for (String split : splits) {
            if (split.length() > config.getMaxChunkSize()) {
                // 如果段���仍然太大，递归使用下一个分隔符
                logger.debug("Segment too large ({} chars), recursing with next separator", split.length());
                List<String> subSplits = recursiveSplit(split, separatorIndex + 1);
                result.addAll(subSplits);
            } else if (split.length() < config.getMinChunkSize() && separatorIndex > 0) {
                // 如果片段太小，尝试合并相邻片段
                logger.debug("Segment too small ({} chars), will try to merge", split.length());
                result.add(split);
            } else {
                // 大小合适，直接添加
                result.add(split);
            }
        }
        
        return result;
    }
    
    /**
     * 使用指定分隔符分割文本
     */
    private List<String> splitBySeparator(String text, String separator) {
        if (separator == null || separator.isEmpty()) {
            return Arrays.asList(text);
        }
        
        String[] parts = text.split(Pattern.quote(separator));
        List<String> result = new ArrayList<>();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            
            if (!part.isEmpty()) {
                if (keepSeparator && i < parts.length - 1) {
                    // 保留分隔符（除了最后一个）
                    part = part + separator;
                }
                result.add(part);
            }
        }
        
        // 处理最后一个部分的分隔符
        if (keepSeparator && parts.length > 0 && text.endsWith(separator)) {
            String lastPart = result.get(result.size() - 1);
            if (!lastPart.endsWith(separator)) {
                result.set(result.size() - 1, lastPart + separator);
            }
        }
        
        return result;
    }
    
    /**
     * 合并相邻的小段
     */
    private List<String> mergeSmallChunks(List<String> chunks) {
        if (chunks.isEmpty()) {
            return chunks;
        }
        
        List<String> merged = new ArrayList<>();
        StringBuilder currentMerge = new StringBuilder();
        int currentLength = 0;
        
        for (String chunk : chunks) {
            int chunkLength = chunk.length();
            
            // 如果当前合并为空，开始新的合并
            if (currentMerge.length() == 0) {
                currentMerge.append(chunk);
                currentLength = chunkLength;
                continue;
            }
            
            // 计算合并后的长度
            int mergedLength = currentLength + chunkLength;
            
            // 决定是否合并
            if (mergedLength <= config.getChunkSize() * 1.2) { // 允许稍微超过目标大小
                // 合并
                currentMerge.append(chunk);
                currentLength = mergedLength;
            } else if (currentLength < config.getMinChunkSize()) {
                // 当前合并太小，必须合并
                currentMerge.append(chunk);
                currentLength = mergedLength;
            } else {
                // 不合并，保存当前合并并开始新的合并
                merged.add(currentMerge.toString());
                currentMerge = new StringBuilder(chunk);
                currentLength = chunkLength;
            }
        }
        
        // 添加最后一个合并
        if (currentMerge.length() > 0) {
            merged.add(currentMerge.toString());
        }
        
        return merged;
    }
    
    /**
     * 创建递归分割的文本块
     */
    private List<TextChunk> createRecursiveChunks(List<String> mergedTexts, String documentId, 
                                                 Object metadata, String originalText) {
        List<TextChunk> chunks = new ArrayList<>();
        
        int currentPosition = 0;
        
        for (int i = 0; i < mergedTexts.size(); i++) {
            String content = mergedTexts.get(i);
            
            // 在原始文本中找到位置
            int startPos = originalText.indexOf(content, currentPosition);
            if (startPos == -1) {
                // 如果找不到，估算位置
                startPos = currentPosition;
            }
            int endPos = startPos + content.length();
            
            // 创建文本块
            Map<String, Object> chunkMetadata = createChunkMetadata(
                i, content.length(), startPos, endPos, countSeparators(content)
            );
            
            TextChunk chunk = createChunk(content, documentId, i, startPos, endPos, chunkMetadata);
            chunks.add(chunk);
            
            currentPosition = endPos;
        }
        
        // 应用重叠逻辑
        return applyOverlap(chunks);
    }
    
    /**
     * 计算文本中的分隔符数量
     */
    private int countSeparators(String text) {
        int count = 0;
        for (String separator : separators) {
            if (!separator.isEmpty()) {
                int pos = 0;
                while ((pos = text.indexOf(separator, pos)) != -1) {
                    count++;
                    pos += separator.length();
                }
            }
        }
        return count;
    }
    
    /**
     * 应用重叠逻辑
     */
    private List<TextChunk> applyOverlap(List<TextChunk> chunks) {
        if (chunks.isEmpty() || config.getChunkOverlap() <= 0) {
            return chunks;
        }
        
        int overlap = config.getChunkOverlap();
        List<TextChunk> result = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk currentChunk = chunks.get(i);
            
            if (i > 0) {
                // 从上一个块中添加重叠内容
                TextChunk previousChunk = chunks.get(i - 1);
                String overlapContent = extractOverlapContent(previousChunk.getContent(), overlap);
                
                if (!overlapContent.isEmpty()) {
                    String newContent = overlapContent + currentChunk.getContent();
                    
                    // 创建新的文本块
                    Map<String, Object> newMetadata = new HashMap<>(currentChunk.getMetadata());
                    newMetadata.put("overlap_from_previous", overlapContent.length());
                    newMetadata.put("has_overlap", true);
                    
                    TextChunk newChunk = createChunk(
                        newContent,
                        currentChunk.getDocumentId(),
                        currentChunk.getChunkIndex(),
                        currentChunk.getStartPosition(),
                        currentChunk.getEndPosition(),
                        newMetadata
                    );
                    
                    result.add(newChunk);
                } else {
                    result.add(currentChunk);
                }
            } else {
                result.add(currentChunk);
            }
        }
        
        return result;
    }
    
    /**
     * 从文本末尾提取重叠内容
     */
    private String extractOverlapContent(String content, int overlapLength) {
        if (content.length() <= overlapLength) {
            return content;
        }
        
        // 尝试在合适的位置分割（优先使用分隔符）
        for (String separator : separators) {
            int pos = content.lastIndexOf(separator);
            if (pos != -1 && pos >= content.length() - overlapLength * 2) {
                return content.substring(pos);
            }
        }
        
        // 如果没有找到合适的分隔符位置，直接截取
        return content.substring(content.length() - overlapLength);
    }
    
    /**
     * 创建块元数据
     */
    private Map<String, Object> createChunkMetadata(int chunkIndex, int contentLength, 
                                                   int startPos, int endPos, int separatorCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunk_index", chunkIndex);
        metadata.put("content_length", contentLength);
        metadata.put("start_position", startPos);
        metadata.put("end_position", endPos);
        metadata.put("recursive_splitter", true);
        metadata.put("separator_count", separatorCount);
        metadata.put("keep_separator", keepSeparator);
        
        // 计算块的质量分数
        double qualityScore = calculateQualityScore(contentLength, separatorCount);
        metadata.put("quality_score", qualityScore);
        
        return metadata;
    }
    
    /**
     * 计算块的质量分数
     */
    private double calculateQualityScore(int contentLength, int separatorCount) {
        if (contentLength == 0) {
            return 0.0;
        }
        
        // 基于大小和分隔符数量的质量评估
        double sizeScore = Math.min(1.0, (double) contentLength / config.getChunkSize());
        double separatorScore = Math.min(1.0, (double) separatorCount * 10 / contentLength);
        
        return (sizeScore * 0.8) + (separatorScore * 0.2);
    }
    
    /**
     * 设置分隔符列表
     */
    public void setSeparators(List<String> separators) {
        this.separators = new ArrayList<>(separators);
    }
    
    /**
     * 添加分隔符
     */
    public void addSeparator(String separator) {
        if (!this.separators.contains(separator)) {
            this.separators.add(separator);
        }
    }
    
    /**
     * 设置是否保留分隔符
     */
    public void setKeepSeparator(boolean keepSeparator) {
        this.keepSeparator = keepSeparator;
    }
    
    /**
     * 获取分割器类型
     */
    @Override
    public String getType() {
        return "RECURSIVE_CHARACTER";
    }
    
    /**
     * 获取分割器版本
     */
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    /**
     * 获取分割器支持的文档类型
     */
    @Override
    public String[] getSupportedTypes() {
        return new String[]{"text/plain", "text/html", "text/markdown", "application/json", "text/xml"};
    }
    
    /**
     * 获取分割器详细信息
     */
    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> info = super.getInfo();
        info.put("type", getType());
        info.put("version", getVersion());
        info.put("separators", separators);
        info.put("keep_separator", keepSeparator);
        info.put("max_recursion_depth", maxRecursionDepth);
        info.put("separator_count", separators.size());
        return info;
    }
}