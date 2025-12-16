package com.wangliang.agentj.rag.textsplitting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本分割器测试类
 * 测试各种文本分割器的功能和性能
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Text Splitter Tests")
public class TextSplitterTest {

    private static final Logger logger = LoggerFactory.getLogger(TextSplitterTest.class);
    
    // 测试文本
    private String chineseText;
    private String englishText;
    private String mixedText;
    private String longText;
    
    @BeforeEach
    void setUp() {
        // 初始化测试文本
        chineseText = "人工智能是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。" +
                     "该领域的研究包括机器人、语言识别、图像识别、自然语言处理和专家系统等。" +
                     "人工智能从诞生以来，理论和技术日益成熟，应用领域也不断扩大。" +
                     "可以设想，未来人工智能带来的科技产品，将会是人类智慧的\"容器\"。" +
                     "人工智能可以对人的意识、思维的信息过程的模拟。" +
                     "人工智能不是人的智能，但能像人那样思考、也可能超过人的智能。";
        
        englishText = "Artificial intelligence (AI) is intelligence demonstrated by machines, in contrast to the natural " +
                     "intelligence displayed by humans and animals. Leading AI textbooks define the field as the study " +
                     "of \"intelligent agents\": any device that perceives its environment and takes actions that maximize " +
                     "its chance of successfully achieving its goals. Colloquially, the term \"artificial intelligence\" " +
                     "is often used to describe machines that mimic \"cognitive\" functions that humans associate with " +
                     "the human mind, such as \"learning\" and \"problem solving\".";
        
        mixedText = "机器学习Machine Learning是人工智能AI的核心技术之一。" +
                   "Deep Learning深度学习在图像识别Image Recognition领域取得了巨大成功。" +
                   "Natural Language Processing自然语言处理让计算机能够理解人类语言。" +
                   "Computer Vision计算机视觉技术广泛应用于自动驾驶Autonomous Driving。";
        
        // 生成长文本
        StringBuilder longTextBuilder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            longTextBuilder.append(chineseText).append("\n\n");
            longTextBuilder.append(englishText).append("\n\n");
            longTextBuilder.append(mixedText).append("\n\n");
        }
        longText = longTextBuilder.toString();
    }
    
    @Test
    @DisplayName("Test Semantic Text Splitter")
    void testSemanticTextSplitter() {
        logger.info("Testing SemanticTextSplitter...");
        
        TextSplitter splitter = new SemanticTextSplitter();
        assertNotNull(splitter);
        
        // 测试基本分割
        List<TextChunk> chunks = splitter.split(chineseText, "doc1", null);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("Semantic splitter created {} chunks", chunks.size());
        
        // 验证每个块
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            assertNotNull(chunk);
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
            assertEquals("doc1", chunk.getDocumentId());
            assertEquals(i, chunk.getChunkIndex());
            
            logger.info("Chunk {}: {} chars, content: {}", 
                       i, chunk.getContent().length(), 
                       chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())));
        }
        
        // 测试元数据
        Map<String, Object> metadata = splitter.getInfo();
        assertNotNull(metadata);
        assertEquals("Semantic Text Splitter", metadata.get("name"));
    }
    
    @Test
    @DisplayName("Test Fixed Length Splitter")
    void testFixedLengthSplitter() {
        logger.info("Testing FixedLengthSplitter...");
        
        SplitterConfig config = SplitterConfig.fixedLengthConfig();
        config.setChunkSize(200); // 设置较小的块大小以便测试
        config.setChunkOverlap(50);
        
        TextSplitter splitter = new FixedLengthSplitter(config);
        assertNotNull(splitter);
        
        // 测试基本分割
        List<TextChunk> chunks = splitter.split(englishText, "doc2", null);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("Fixed length splitter created {} chunks", chunks.size());
        
        // 验证块大小
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            assertNotNull(chunk);
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
            
            // 检查块大小是否在合理范围内
            int contentLength = chunk.getContent().length();
            assertTrue(contentLength <= config.getChunkSize() * 1.2); // 允许稍微超过目标大小
            
            logger.info("Chunk {}: {} chars (target: {})", i, contentLength, config.getChunkSize());
        }
        
        // 测试重叠
        if (chunks.size() > 1) {
            TextChunk chunk1 = chunks.get(0);
            TextChunk chunk2 = chunks.get(1);
            
            // 检查是否有重叠内容（简化检查）
            boolean hasOverlap = chunk2.getContent().contains(chunk1.getContent().substring(chunk1.getContent().length() - 20));
            logger.info("Overlap detected: {}", hasOverlap);
        }
    }
    
    @Test
    @DisplayName("Test Token Based Splitter")
    void testTokenBasedSplitter() {
        logger.info("Testing TokenBasedSplitter...");
        
        SplitterConfig config = SplitterConfig.tokenBasedConfig();
        TextSplitter splitter = new TokenBasedSplitter(config);
        assertNotNull(splitter);
        
        // 测试中文文本
        List<TextChunk> chineseChunks = splitter.split(chineseText, "doc3", null);
        assertNotNull(chineseChunks);
        assertFalse(chineseChunks.isEmpty());
        
        logger.info("Token splitter created {} chunks for Chinese text", chineseChunks.size());
        
        // 测试英文文本
        List<TextChunk> englishChunks = splitter.split(englishText, "doc4", null);
        assertNotNull(englishChunks);
        assertFalse(englishChunks.isEmpty());
        
        logger.info("Token splitter created {} chunks for English text", englishChunks.size());
        
        // 测试混合文本
        List<TextChunk> mixedChunks = splitter.split(mixedText, "doc5", null);
        assertNotNull(mixedChunks);
        assertFalse(mixedChunks.isEmpty());
        
        logger.info("Token splitter created {} chunks for mixed text", mixedChunks.size());
        
        // 验证Token统计信息
        for (TextChunk chunk : mixedChunks) {
            Map<String, Object> chunkMetadata = chunk.getMetadata();
            assertNotNull(chunkMetadata);
            
            assertTrue(chunkMetadata.containsKey("total_tokens"));
            assertTrue(chunkMetadata.containsKey("chinese_tokens"));
            assertTrue(chunkMetadata.containsKey("english_tokens"));
            
            logger.info("Chunk {}: {} total tokens (CN: {}, EN: {})", 
                       chunk.getChunkIndex(),
                       chunkMetadata.get("total_tokens"),
                       chunkMetadata.get("chinese_tokens"),
                       chunkMetadata.get("english_tokens"));
        }
    }
    
    @Test
    @DisplayName("Test Recursive Character Splitter")
    void testRecursiveCharacterSplitter() {
        logger.info("Testing RecursiveCharacterSplitter...");
        
        TextSplitter splitter = new RecursiveCharacterSplitter();
        assertNotNull(splitter);
        
        // 测试基本分割
        List<TextChunk> chunks = splitter.split(longText, "doc6", null);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("Recursive splitter created {} chunks", chunks.size());
        
        // 验证分割质量
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            assertNotNull(chunk);
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
            
            Map<String, Object> metadata = chunk.getMetadata();
            assertNotNull(metadata);
            
            // 检查质量分数
            assertTrue(metadata.containsKey("quality_score"));
            double qualityScore = (Double) metadata.get("quality_score");
            assertTrue(qualityScore >= 0.0 && qualityScore <= 1.0);
            
            logger.info("Chunk {}: {} chars, quality score: {}", 
                       i, chunk.getContent().length(), qualityScore);
        }
    }
    
    @Test
    @DisplayName("Test Text Splitter Factory")
    void testTextSplitterFactory() {
        logger.info("Testing TextSplitterFactory...");
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        assertNotNull(factory);
        
        // 测试单例模式
        TextSplitterFactory factory2 = TextSplitterFactory.getInstance();
        assertSame(factory, factory2);
        
        // 测试创建不同类型的分割器
        String[] types = {"semantic", "fixed", "token", "recursive"};
        
        for (String type : types) {
            TextSplitter splitter = factory.createSplitter(type);
            assertNotNull(splitter, "Failed to create splitter of type: " + type);
            
            logger.info("Created {} splitter: {}", type, splitter.getName());
            
            // 测试分割功能
            List<TextChunk> chunks = splitter.split(chineseText, "factory_test_" + type, null);
            assertNotNull(chunks);
            assertFalse(chunks.isEmpty());
        }
        
        // 测试批量创建
        List<TextSplitter> splitters = factory.createSplitters(types);
        assertEquals(types.length, splitters.size());
        
        // 测试获取分割器信息
        Map<String, Object> info = factory.getSplitterInfo("semantic");
        assertNotNull(info);
        assertEquals("semantic", info.get("type"));
        assertEquals("Semantic Text Splitter", info.get("name"));
        
        logger.info("Factory test completed successfully");
    }
    
    @Test
    @DisplayName("Test Empty and Null Text")
    void testEmptyAndNullText() {
        logger.info("Testing empty and null text handling...");
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        TextSplitter splitter = factory.createSplitter("fixed");
        
        // 测试空文本
        List<TextChunk> emptyChunks = splitter.split("", "empty_doc", null);
        assertNotNull(emptyChunks);
        assertTrue(emptyChunks.isEmpty());
        
        // 测试null文本
        List<TextChunk> nullChunks = splitter.split(null, "null_doc", null);
        assertNotNull(nullChunks);
        assertTrue(nullChunks.isEmpty());
        
        // 测试空白文本
        List<TextChunk> whitespaceChunks = splitter.split("   \n\n  ", "whitespace_doc", null);
        assertNotNull(whitespaceChunks);
        assertTrue(whitespaceChunks.isEmpty());
        
        logger.info("Empty and null text handling test completed");
    }
    
    @Test
    @DisplayName("Test Configuration Validation")
    void testConfigurationValidation() {
        logger.info("Testing configuration validation...");
        
        // 测试无效配置
        SplitterConfig invalidConfig = new SplitterConfig();
        invalidConfig.setChunkSize(0); // 无效的块大小
        invalidConfig.setChunkOverlap(-1); // 无效的重叠大小
        
        assertThrows(IllegalArgumentException.class, () -> {
            invalidConfig.validate();
        });
        
        // 测试有效配置
        SplitterConfig validConfig = new SplitterConfig();
        validConfig.setChunkSize(1000);
        validConfig.setChunkOverlap(200);
        
        assertDoesNotThrow(() -> {
            validConfig.validate();
        });
        
        logger.info("Configuration validation test completed");
    }
    
    @Test
    @DisplayName("Test Performance and Large Text")
    void testPerformanceAndLargeText() {
        logger.info("Testing performance with large text...");
        
        // 生成大文本
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeText.append(chineseText).append("\n");
            largeText.append(englishText).append("\n");
        }
        String largeContent = largeText.toString();
        
        logger.info("Large text size: {} characters", largeContent.length());
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        
        // 测试不同分割器的性能
        String[] types = {"semantic", "fixed", "token", "recursive"};
        
        for (String type : types) {
            TextSplitter splitter = factory.createSplitter(type);
            
            long startTime = System.currentTimeMillis();
            List<TextChunk> chunks = splitter.split(largeContent, "performance_test_" + type, null);
            long endTime = System.currentTimeMillis();
            
            assertNotNull(chunks);
            assertFalse(chunks.isEmpty());
            
            logger.info("{} splitter: {} chunks in {} ms (avg: {} ms/chunk)", 
                       type, chunks.size(), (endTime - startTime), 
                       (double) (endTime - startTime) / chunks.size());
        }
    }
    
    @Test
    @DisplayName("Test Metadata and Statistics")
    void testMetadataAndStatistics() {
        logger.info("Testing metadata and statistics...");
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        TextSplitter splitter = factory.createSplitter("semantic");
        
        List<TextChunk> chunks = splitter.split(mixedText, "metadata_test", null);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // 验证每个块的统计信息
        for (TextChunk chunk : chunks) {
            assertTrue(chunk.getWordCount() >= 0);
            assertTrue(chunk.getCharacterCount() > 0);
            assertEquals(mixedText.length() >= chunk.getCharacterCount(), true);
            
            logger.info("Chunk {}: {} words, {} characters", 
                       chunk.getChunkIndex(), chunk.getWordCount(), chunk.getCharacterCount());
        }
        
        // 测试分割器信息
        Map<String, Object> info = splitter.getInfo();
        assertNotNull(info);
        assertTrue(info.containsKey("name"));
        assertTrue(info.containsKey("description"));
        assertTrue(info.containsKey("type"));
        
        logger.info("Splitter info: {}", info);
    }
}