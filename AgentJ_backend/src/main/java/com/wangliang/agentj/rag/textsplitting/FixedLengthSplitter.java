package com.wangliang.agentj.rag.textsplitting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 固定长度文本分割器
 * 按照固定的字符长度进行文本分割，支持重叠和边界保持
 */
public class FixedLengthSplitter extends AbstractTextSplitter {

    private static final Logger logger = LoggerFactory.getLogger(FixedLengthSplitter.class);
    
    /**
     * 默认构造函数
     */
    public FixedLengthSplitter() {
        super();
        this.name = "Fixed Length Splitter";
        this.description = "Splits text into fixed-length chunks with configurable overlap";
    }
    
    /**
     * 带配置的构造函数
     */
    public FixedLengthSplitter(SplitterConfig config) {
        super(config);
        this.name = "Fixed Length Splitter";
        this.description = "Splits text into fixed-length chunks with configurable overlap";
    }
    
    @Override
    protected List<TextChunk> performSplit(String text, String documentId, Object metadata) {
        logger.info("Starting fixed-length text splitting for document: {}", documentId);
        
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Text is null or empty");
            return Collections.emptyList();
        }
        
        int textLength = text.length();
        logger.info("Text length: {} characters", textLength);
        
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();
        
        // 验证配置参数
        if (chunkSize <= 0) {
            logger.error("Invalid chunk size: {}", chunkSize);
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        
        if (overlap >= chunkSize) {
            logger.warn("Overlap {} is greater than or equal to chunk size {}, adjusting overlap", overlap, chunkSize);
            overlap = chunkSize / 4; // 默认重叠为块大小的1/4
        }
        
        List<TextChunk> chunks = new ArrayList<>();
        int currentPosition = 0;
        int chunkIndex = 0;
        
        logger.info("Splitting with chunk size: {} and overlap: {}", chunkSize, overlap);
        
        while (currentPosition < textLength) {
            // 计算当前块的结束位置
            int endPosition = Math.min(currentPosition + chunkSize, textLength);
            
            // 如果这不是最后一个块，尝试找到更好的分割点
            if (endPosition < textLength) {
                endPosition = findOptimalSplitPosition(text, currentPosition, endPosition);
            }
            
            // 提取块内容
            String chunkContent = text.substring(currentPosition, endPosition);
            
            // 创建文本块
            TextChunk chunk = createChunk(
                chunkContent,
                documentId,
                chunkIndex,
                currentPosition,
                endPosition,
                createChunkMetadata(chunkIndex, chunkContent.length(), currentPosition, endPosition)
            );
            
            chunks.add(chunk);
            logger.debug("Created chunk {}: position {}-{}, length: {}", 
                        chunkIndex, currentPosition, endPosition, chunkContent.length());
            
            // 移动到下一个位置（考虑重叠）
            int nextPosition = endPosition - overlap;
            
            // 防止无限循环
            if (nextPosition <= currentPosition) {
                nextPosition = endPosition;
            }
            
            // 如果剩余内容太少，提前结束
            if (textLength - nextPosition < chunkSize * 0.2) { // 剩余内容少于块大小的20%
                if (nextPosition < textLength) {
                    // 创建最后一个块包含剩余所有内容
                    String finalChunkContent = text.substring(nextPosition);
                    TextChunk finalChunk = createChunk(
                        finalChunkContent,
                        documentId,
                        chunkIndex + 1,
                        nextPosition,
                        textLength,
                        createChunkMetadata(chunkIndex + 1, finalChunkContent.length(), nextPosition, textLength)
                    );
                    chunks.add(finalChunk);
                    logger.debug("Created final chunk {}: position {}-{}", 
                                chunkIndex + 1, nextPosition, textLength);
                }
                break;
            }
            
            currentPosition = nextPosition;
            chunkIndex++;
        }
        
        logger.info("Fixed-length splitting completed. Generated {} chunks", chunks.size());
        return chunks;
    }
    
    /**
     * 寻找最佳分割位置
     */
    private int findOptimalSplitPosition(String text, int startPosition, int targetEndPosition) {
        
        // 如果配置要求保持边界，优先使用分隔符
        if (config.isPreserveSentenceBoundary() || config.isPreserveParagraphBoundary()) {
            int optimalPosition = findSplitPositionBySeparators(text, startPosition, targetEndPosition);
            if (optimalPosition > startPosition && optimalPosition < text.length()) {
                return optimalPosition;
            }
        }
        
        // 如果没有找到合适的分隔符位置，使用字符边界
        return findSplitPositionByCharacterBoundary(text, targetEndPosition);
    }
    
    /**
     * 根据分隔符寻找分割位置
     */
    private int findSplitPositionBySeparators(String text, int startPosition, int targetEndPosition) {
        List<String> separators = config.getSeparators();
        
        if (separators == null || separators.isEmpty()) {
            return targetEndPosition;
        }
        
        // 优先使用段落分隔符
        if (config.isPreserveParagraphBoundary()) {
            for (String separator : separators) {
                if ("\n\n".equals(separator)) {
                    int position = findLastSeparatorPosition(text, startPosition, targetEndPosition, separator);
                    if (position > startPosition) {
                        return position;
                    }
                }
            }
        }
        
        // 然后使用句子分隔符
        if (config.isPreserveSentenceBoundary()) {
            String[] sentenceSeparators = {"。", "。", "！", "!", "？", "?"};
            for (String separator : sentenceSeparators) {
                int position = findLastSeparatorPosition(text, startPosition, targetEndPosition, separator);
                if (position > startPosition) {
                    return position;
                }
            }
        }
        
        // 最后使用普通分隔符
        for (String separator : separators) {
            if (!"\n\n".equals(separator)) {
                int position = findLastSeparatorPosition(text, startPosition, targetEndPosition, separator);
                if (position > startPosition) {
                    return position;
                }
            }
        }
        
        return targetEndPosition;
    }
    
    /**
     * 寻找最后一个分隔符位置
     */
    private int findLastSeparatorPosition(String text, int startPosition, int targetEndPosition, String separator) {
        // 在目标位置附近寻找最后一个分隔符
        int searchEnd = Math.min(targetEndPosition + 50, text.length()); // 在目标位置后50字符内寻找
        int searchStart = Math.max(targetEndPosition - 100, startPosition); // 在目标位置前100字符内寻找
        
        int lastPosition = -1;
        int currentPosition = searchStart;
        
        while (currentPosition < searchEnd) {
            int pos = text.indexOf(separator, currentPosition);
            if (pos == -1 || pos >= searchEnd) {
                break;
            }
            
            // 更新最后一个位置（包含分隔符长度）
            lastPosition = pos + separator.length();
            currentPosition = pos + 1;
        }
        
        return lastPosition != -1 ? lastPosition : targetEndPosition;
    }
    
    /**
     * 根据字符边界寻找分割位置
     */
    private int findSplitPositionByCharacterBoundary(String text, int targetEndPosition) {
        // 确保不在单词中间分割（简单实现）
        if (targetEndPosition >= text.length()) {
            return text.length();
        }
        
        // 如果在空格或标点符号处，直接返回
        char currentChar = text.charAt(targetEndPosition - 1);
        if (Character.isWhitespace(currentChar) || isPunctuation(currentChar)) {
            return targetEndPosition;
        }
        
        // 向前寻找最近的空格或标点符号
        for (int i = targetEndPosition - 1; i >= Math.max(0, targetEndPosition - 20); i--) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch) || isPunctuation(ch)) {
                return i + 1;
            }
        }
        
        // 如果找不到，返回原始位置
        return targetEndPosition;
    }
    
    /**
     * 检查字符是否为标点符号
     */
    private boolean isPunctuation(char ch) {
        return ".,;:!?。，；：！？\"'".indexOf(ch) != -1;
    }
    
    /**
     * 创建块元数据
     */
    private Map<String, Object> createChunkMetadata(int chunkIndex, int contentLength, 
                                                     int startPosition, int endPosition) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunk_index", chunkIndex);
        metadata.put("content_length", contentLength);
        metadata.put("start_position", startPosition);
        metadata.put("end_position", endPosition);
        metadata.put("fixed_length_splitter", true);
        metadata.put("chunk_size_config", config.getChunkSize());
        metadata.put("overlap_config", config.getChunkOverlap());
        
        // 计算覆盖率
        double coverage = (double) contentLength / config.getChunkSize();
        metadata.put("coverage_ratio", coverage);
        
        return metadata;
    }
    
    /**
     * 获取分割器类型
     */
    public String getType() {
        return "FIXED_LENGTH";
    }
    
    /**
     * 获取分割器版本
     */
    public String getVersion() {
        return "1.0.0";
    }
    
    /**
     * 获取分割器详细信息
     */
    public Map<String, Object> getInfo() {
        Map<String, Object> info = super.getInfo();
        info.put("type", getType());
        info.put("version", getVersion());
        info.put("chunk_size", config.getChunkSize());
        info.put("overlap", config.getChunkOverlap());
        info.put("preserve_boundaries", config.isPreserveSentenceBoundary() || config.isPreserveParagraphBoundary());
        return info;
    }
}
