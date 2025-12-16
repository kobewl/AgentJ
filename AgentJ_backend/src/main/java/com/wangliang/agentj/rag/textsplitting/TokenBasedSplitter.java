package com.wangliang.agentj.rag.textsplitting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.document.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于Token的文本分割器
 * 基于Spring AI的TokenTextSplitter实现，支持中英文混合文本
 */
public class TokenBasedSplitter extends AbstractTextSplitter {

    private static final Logger logger = LoggerFactory.getLogger(TokenBasedSplitter.class);
    
    // Spring AI TokenTextSplitter实例
    private TokenTextSplitter tokenTextSplitter;
    
    // 配置参数
    private int defaultChunkSize;
    private int minChunkSizeChars;
    private int minChunkLengthToEmbed;
    private int maxNumChunks;
    private boolean keepSeparator;
    
    /**
     * 默认构造函数
     */
    public TokenBasedSplitter() {
        super();
        this.name = "Token Based Splitter";
        this.description = "Splits text based on token count using Spring AI TokenTextSplitter";
        
        // 默认参数配置
        this.defaultChunkSize = 800;      // 默认每块800个token
        this.minChunkSizeChars = 350;     // 最小块大小（字符）
        this.minChunkLengthToEmbed = 5;   // 最小嵌入长度
        this.maxNumChunks = 10000;        // 最大块数
        this.keepSeparator = true;        // 保持分隔符
        
        initializeTokenTextSplitter();
    }
    
    /**
     * 带配置的构造函数
     */
    public TokenBasedSplitter(SplitterConfig config) {
        super(config);
        this.name = "Token Based Splitter";
        this.description = "Splits text based on token count using Spring AI TokenTextSplitter";
        
        // 从配置中读取参数，使用合理默认值
        this.defaultChunkSize = config.getChunkSize() > 0 ? config.getChunkSize() : 800;
        this.minChunkSizeChars = 350;     // 固定值，适合大多数场景
        this.minChunkLengthToEmbed = 5;   // 固定值，避免块过小
        this.maxNumChunks = 10000;        // 固定值，防止内存溢出
        this.keepSeparator = true;        // 保持分隔符，保持语义完整性
        
        initializeTokenTextSplitter();
    }
    
    @Override
    protected List<TextChunk> performSplit(String text, String documentId, Object metadata) {
        logger.info("Starting token-based text splitting for document: {} using Spring AI TokenTextSplitter", documentId);
        
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Text is null or empty");
            return Collections.emptyList();
        }
        
        try {
            // 创建Spring AI Document对象
            Document originalDocument = new Document(text);
            
            // 设置文档ID到metadata
            if (documentId != null) {
                originalDocument.getMetadata().put("document_id", documentId);
            }
            
            // 添加用户提供的metadata
            if (metadata instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = (Map<String, Object>) metadata;
                originalDocument.getMetadata().putAll(metadataMap);
            }
            
            logger.info("Original document created with {} characters", text.length());
            
            // 使用Spring AI TokenTextSplitter进行分割
            List<Document> splitDocuments = tokenTextSplitter.apply(List.of(originalDocument));
            logger.info("TokenTextSplitter produced {} documents", splitDocuments.size());
            
            // 将Spring AI Document转换为我们自己的TextChunk格式
            return convertToTextChunks(splitDocuments, documentId, text);
            
        } catch (Exception e) {
            logger.error("Error during token-based text splitting for document: {}", documentId, e);
            throw new RuntimeException("Failed to split text using TokenTextSplitter", e);
        }
    }
    
    /**
     * 初始化Spring AI TokenTextSplitter
     */
    private void initializeTokenTextSplitter() {
        try {
            this.tokenTextSplitter = new TokenTextSplitter(
                defaultChunkSize,
                minChunkSizeChars,
                minChunkLengthToEmbed,
                maxNumChunks,
                keepSeparator
            );
            logger.info("Initialized TokenTextSplitter with chunkSize: {}, minChunkSizeChars: {}, " +
                       "minChunkLengthToEmbed: {}, maxNumChunks: {}, keepSeparator: {}",
                       defaultChunkSize, minChunkSizeChars, minChunkLengthToEmbed, 
                       maxNumChunks, keepSeparator);
        } catch (Exception e) {
            logger.error("Failed to initialize TokenTextSplitter", e);
            throw new RuntimeException("Failed to initialize TokenTextSplitter", e);
        }
    }
    
    /**
     * 将文本分割成Token单元
     */
    private List<TokenUnit> tokenizeText(String text, boolean isChineseText) {
        List<TokenUnit> tokenUnits = new ArrayList<>();
        
        if (isChineseText) {
            // 中文模式：按字符和词语分割
            tokenUnits = tokenizeChineseText(text);
        } else {
            // 英文模式：按单词和标点分割
            tokenUnits = tokenizeEnglishText(text);
        }
        
        return tokenUnits;
    }
    
    /**
     * 将Spring AI Document转换为TextChunk
     */
    private List<TextChunk> convertToTextChunks(List<Document> documents, String documentId, String originalText) {
        List<TextChunk> chunks = new ArrayList<>();
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getContent();
            
            // 计算在原始文本中的位置
            int startPos = i == 0 ? 0 : findPositionInOriginalText(originalText, content, chunks, i);
            int endPos = startPos + content.length();
            
            // 创建元数据
            Map<String, Object> chunkMetadata = createChunkMetadata(
                i, content.length(), startPos, endPos, doc.getMetadata()
            );
            
            // 创建TextChunk
            TextChunk chunk = createChunk(content, documentId, i, startPos, endPos, chunkMetadata);
            chunks.add(chunk);
            
            logger.debug("Converted chunk {}: {} characters, position {}-{}", 
                        i, content.length(), startPos, endPos);
        }
        
        return chunks;
    }
    
    /**
     * 在原始文本中找到内容的位置
     */
    private int findPositionInOriginalText(String originalText, String content, 
                                         List<TextChunk> existingChunks, int currentIndex) {
        if (existingChunks.isEmpty()) {
            return 0;
        }
        
        // 尝试从上一个块的结束位置开始查找
        int searchStart = existingChunks.get(existingChunks.size() - 1).getEndPosition();
        
        // 查找内容在原始文本中的位置
        int position = originalText.indexOf(content, searchStart);
        
        // 如果找不到，尝试从文本开头查找（处理重叠情况）
        if (position == -1) {
            position = originalText.indexOf(content);
        }
        
        // 如果还是找不到，使用估算位置
        if (position == -1) {
            position = searchStart;
        }
        
        return position;
    }
    
    /**
     * 创建块元数据（整合Spring AI的metadata）
     */
    private Map<String, Object> createChunkMetadata(int chunkIndex, int contentLength, 
                                                     int startPos, int endPos, 
                                                     Map<String, Object> springAiMetadata) {
        Map<String, Object> metadata = new HashMap<>();
        
        // 基础元数据
        metadata.put("chunk_index", chunkIndex);
        metadata.put("content_length", contentLength);
        metadata.put("start_position", startPos);
        metadata.put("end_position", endPos);
        metadata.put("token_based_splitter", true);
        metadata.put("spring_ai_token_splitter", true);
        
        // Spring AI TokenTextSplitter的配置参数
        metadata.put("default_chunk_size", defaultChunkSize);
        metadata.put("min_chunk_size_chars", minChunkSizeChars);
        metadata.put("min_chunk_length_to_embed", minChunkLengthToEmbed);
        metadata.put("max_num_chunks", maxNumChunks);
        metadata.put("keep_separator", keepSeparator);
        
        // 整合Spring AI的metadata
        if (springAiMetadata != null) {
            metadata.putAll(springAiMetadata);
        }
        
        return metadata;
    }
    
    /**
     * 获取分割器类型
     */
    @Override
    public String getType() {
        return "TOKEN_BASED";
    }
    
    /**
     * 获取分割器版本
     */
    @Override
    public String getVersion() {
        return "2.0.0";
    }
    
    /**
     * 获取分割器支持的文档类型
     */
    @Override
    public String[] getSupportedTypes() {
        return new String[]{"text/plain", "text/html", "text/markdown", "application/json"};
    }
    
    /**
     * 获取分割器详细信息
     */
    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> info = super.getInfo();
        info.put("type", getType());
        info.put("version", getVersion());
        info.put("spring_ai_based", true);
        info.put("default_chunk_size", defaultChunkSize);
        info.put("min_chunk_size_chars", minChunkSizeChars);
        info.put("min_chunk_length_to_embed", minChunkLengthToEmbed);
        info.put("max_num_chunks", maxNumChunks);
        info.put("keep_separator", keepSeparator);
        info.put("implementation", "Spring AI TokenTextSplitter");
        return info;
    }
}