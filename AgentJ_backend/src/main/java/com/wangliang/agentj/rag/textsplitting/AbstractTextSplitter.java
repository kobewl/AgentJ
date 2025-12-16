package com.wangliang.agentj.rag.textsplitting;

import com.wangliang.agentj.rag.common.models.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 文本分割器抽象基类
 * 提供通用的分割功能和工具方法
 */
public abstract class AbstractTextSplitter implements TextSplitter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTextSplitter.class);
    
    protected SplitterConfig config;
    protected String name;
    protected String description;
    protected String[] supportedTypes;
    
    /**
     * 默认构造函数
     */
    public AbstractTextSplitter() {
        this.config = new SplitterConfig();
        this.supportedTypes = new String[]{"text", "txt", "md", "markdown", "json", "xml", "html"};
    }
    
    /**
     * 带配置的构造函数
     */
    public AbstractTextSplitter(SplitterConfig config) {
        this.config = config != null ? config : new SplitterConfig();
        this.supportedTypes = new String[]{"text", "txt", "md", "markdown", "json", "xml", "html"};
    }
    
    @Override
    public List<TextChunk> split(Document document) {
        if (document == null || document.getContent() == null) {
            logger.warn("Document or document content is null");
            return Collections.emptyList();
        }
        
        logger.info("Starting to split document: {}", document.getId());
        
        String content = document.getContent();
        Object metadata = document.getMetadata();
        String documentId = document.getId();
        
        return split(content, documentId, metadata);
    }
    
    @Override
    public List<TextChunk> split(String text, Object metadata) {
        return split(text, null, metadata);
    }
    
    @Override
    public List<TextChunk> split(String text, String documentId, Object metadata) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Text content is null or empty");
            return Collections.emptyList();
        }
        
        // 验证配置
        config.validate();
        
        logger.info("Starting text splitting with config: {}", config);
        
        // 预处理文本
        String processedText = preprocessText(text);
        
        // 执行分割（由子类实现）
        List<TextChunk> chunks = performSplit(processedText, documentId, metadata);
        
        // 后处理
        chunks = postprocessChunks(chunks);
        
        logger.info("Text splitting completed. Generated {} chunks", chunks.size());
        
        return chunks;
    }
    
    /**
     * 执行具体的分割逻辑（由子类实现）
     */
    protected abstract List<TextChunk> performSplit(String text, String documentId, Object metadata);
    
    /**
     * 预处理文本
     */
    protected String preprocessText(String text) {
        if (config.isTrimWhitespace()) {
            text = text.trim();
        }
        
        // 标准化换行符
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        
        return text;
    }
    
    /**
     * 后处理文本块
     */
    protected List<TextChunk> postprocessChunks(List<TextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<TextChunk> processedChunks = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            
            if (chunk == null || chunk.isEmpty()) {
                if (!config.isRemoveEmptyChunks()) {
                    processedChunks.add(chunk);
                }
                continue;
            }
            
            // 更新索引
            chunk.setChunkIndex(i);
            
            // 添加元数据
            if (config.isAddMetadata()) {
                addChunkMetadata(chunk, i, chunks.size());
            }
            
            processedChunks.add(chunk);
        }
        
        return processedChunks;
    }
    
    /**
     * 添加文本块元数据
     */
    protected void addChunkMetadata(TextChunk chunk, int index, int totalChunks) {
        chunk.addMetadata("chunk_index", index);
        chunk.addMetadata("total_chunks", totalChunks);
        chunk.addMetadata("splitter_type", this.getClass().getSimpleName());
        chunk.addMetadata("chunk_size", chunk.size());
        chunk.addMetadata("word_count", chunk.getWordCount());
        
        if (index == 0) {
            chunk.addMetadata("is_first_chunk", true);
        }
        
        if (index == totalChunks - 1) {
            chunk.addMetadata("is_last_chunk", true);
        }
        
        // 添加自定义元数据
        if (config.getCustomParameters() != null) {
            config.getCustomParameters().forEach(chunk::addMetadata);
        }
    }
    
    /**
     * 创建文本块
     */
    protected TextChunk createChunk(String content, String documentId, int chunkIndex, 
                                   int startPos, int endPos, Map<String, Object> metadata) {
        String chunkId = generateChunkId(documentId, chunkIndex);
        
        return new TextChunk(
            chunkId,
            content,
            documentId,
            chunkIndex,
            startPos,
            endPos,
            metadata
        );
    }
    
    /**
     * 生成文本块ID
     */
    protected String generateChunkId(String documentId, int chunkIndex) {
        if (documentId != null) {
            return documentId + "_chunk_" + chunkIndex;
        } else {
            return "chunk_" + System.currentTimeMillis() + "_" + chunkIndex;
        }
    }
    
    /**
     * 查找最佳分割位置
     */
    protected int findSplitPosition(String text, int targetPosition, boolean lookBackward) {
        List<String> separators = config.getSeparators();
        
        if (separators == null || separators.isEmpty()) {
            return targetPosition;
        }
        
        int bestPosition = targetPosition;
        int minDistance = Integer.MAX_VALUE;
        
        for (String separator : separators) {
            int position;
            
            if (lookBackward) {
                position = text.lastIndexOf(separator, targetPosition);
            } else {
                position = text.indexOf(separator, targetPosition);
            }
            
            if (position != -1) {
                int distance = Math.abs(position - targetPosition);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPosition = position + separator.length();
                }
            }
        }
        
        // 如果找不到合适的分割点，返回原始位置
        if (minDistance == Integer.MAX_VALUE) {
            return targetPosition;
        }
        
        return bestPosition;
    }
    
    /**
     * 检查是否为句子边界
     */
    protected boolean isSentenceBoundary(String text, int position) {
        if (position <= 0 || position >= text.length()) {
            return true;
        }
        
        char prevChar = text.charAt(position - 1);
        char currentChar = text.charAt(position);
        
        // 检查句子结束标点符号
        boolean isSentenceEnd = ".!?。！？".indexOf(prevChar) != -1;
        
        // 检查是否为新行的开始
        boolean isNewLine = currentChar == '\n' || (position > 1 && text.charAt(position - 2) == '\n');
        
        return isSentenceEnd || isNewLine;
    }
    
    /**
     * 检查是否为段落边界
     */
    protected boolean isParagraphBoundary(String text, int position) {
        if (position <= 0 || position >= text.length() - 1) {
            return true;
        }
        
        // 检查双换行（段落分隔符）
        return position >= 2 && text.charAt(position - 2) == '\n' && text.charAt(position - 1) == '\n';
    }
    
    /**
     * 计算文本长度（考虑字符编码）
     */
    protected int calculateTextLength(String text) {
        if (text == null) {
            return 0;
        }
        return text.length();
    }
    
    /**
     * 计算重叠文本
     */
    protected String calculateOverlap(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return "";
        }
        
        int overlapSize = config.getChunkOverlap();
        if (overlapSize <= 0) {
            return "";
        }
        
        // 从text1的末尾提取重叠部分
        int startPos = Math.max(0, text1.length() - overlapSize);
        return text1.substring(startPos);
    }
    
    // Getters and Setters
    @Override
    public String getName() {
        return name != null ? name : this.getClass().getSimpleName();
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getDescription() {
        return description != null ? description : "Text splitter for processing documents";
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public SplitterConfig getConfig() {
        return config;
    }
    
    @Override
    public void setConfig(SplitterConfig config) {
        this.config = config != null ? config : new SplitterConfig();
    }
    
    @Override
    public boolean supports(String documentType) {
        if (documentType == null || supportedTypes == null) {
            return false;
        }
        
        return Arrays.stream(supportedTypes)
                .anyMatch(type -> type.equalsIgnoreCase(documentType));
    }
    
    @Override
    public String[] getSupportedTypes() {
        return supportedTypes != null ? supportedTypes.clone() : new String[0];
    }
    
    public void setSupportedTypes(String[] supportedTypes) {
        this.supportedTypes = supportedTypes != null ? supportedTypes.clone() : new String[0];
    }
}