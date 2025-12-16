package com.wangliang.agentj.rag.textsplitting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenBasedSplitter测试类
 * 测试基于Spring AI TokenTextSplitter的文本分割功能
 */
public class TokenBasedSplitterTest {

    private static final Logger logger = LoggerFactory.getLogger(TokenBasedSplitterTest.class);
    
    private TokenBasedSplitter splitter;
    private SplitterConfig config;
    
    @BeforeEach
    void setUp() {
        config = SplitterConfig.tokenBasedConfig();
        splitter = new TokenBasedSplitter(config);
    }
    
    @Test
    @DisplayName("测试基本文本分割")
    void testBasicTextSplitting() {
        logger.info("测试基本文本分割...");
        
        String text = "这是一个测试文本。它包含多个句子。每个句子都应该被正确处理。" +
                     "这是另一个段落。它有更多的内容。我们可以测试分割效果。" +
                     "最后一段，用于验证分割器是否能正确处理不同长度的文本。";
        
        List<TextChunk> chunks = splitter.split(text, "test-doc-1", null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("分割产生了 {} 个块", chunks.size());
        
        // 验证每个块
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            assertNotNull(chunk);
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
            assertEquals("test-doc-1", chunk.getDocumentId());
            assertEquals(i, chunk.getChunkIndex());
            
            logger.info("块 {}: {} 字符, 内容: {}", 
                       i, chunk.getContent().length(), 
                       chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())));
            
            // 验证元数据
            Map<String, Object> metadata = chunk.getMetadata();
            assertNotNull(metadata);
            assertTrue((Boolean) metadata.get("token_based_splitter"));
            assertTrue((Boolean) metadata.get("spring_ai_token_splitter"));
            assertNotNull(metadata.get("chunk_index"));
            assertNotNull(metadata.get("content_length"));
            assertNotNull(metadata.get("start_position"));
            assertNotNull(metadata.get("end_position"));
        }
    }
    
    @Test
    @DisplayName("测试英文文本分割")
    void testEnglishTextSplitting() {
        logger.info("测试英文文本分割...");
        
        String text = "This is a test document. It contains multiple sentences. " +
                     "Each sentence should be processed correctly. " +
                     "This is another paragraph with more content. " +
                     "We can test the splitting effectiveness. " +
                     "The final paragraph is used to verify that the splitter can handle texts of different lengths.";
        
        List<TextChunk> chunks = splitter.split(text, "test-doc-2", null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("分割产生了 {} 个块", chunks.size());
        
        // 验证内容完整性
        StringBuilder reconstructed = new StringBuilder();
        for (TextChunk chunk : chunks) {
            reconstructed.append(chunk.getContent());
        }
        
        // 由于可能有重叠，我们检查原始文本是否包含重建的内容
        assertTrue(text.contains(reconstructed.toString().trim()));
    }
    
    @Test
    @DisplayName("测试中英文混合文本")
    void testMixedLanguageText() {
        logger.info("测试中英文混合文本...");
        
        String text = "这是一个混合语言的测试。This is a mixed language test. " +
                     "我们可以测试中英文混合的情况。We can test mixed Chinese and English scenarios. " +
                     "分割器应该能正确处理。The splitter should handle this correctly.";
        
        List<TextChunk> chunks = splitter.split(text, "test-doc-3", null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("分割产生了 {} 个块", chunks.size());
        
        // 验证每个块都包含有效内容
        for (TextChunk chunk : chunks) {
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().trim().isEmpty());
            logger.info("块内容: {}", chunk.getContent().substring(0, Math.min(60, chunk.getContent().length())));
        }
    }
    
    @Test
    @DisplayName("测试长文本分割")
    void testLongTextSplitting() {
        logger.info("测试长文本分割...");
        
        // 生成长文本
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("这是第 ").append(i + 1).append(" 段文本。")
                   .append("它包含一些内容用于测试长文本的分割效果。")
                   .append("我们可以验证分割器是否能处理大量文本。")
                   .append("每段都应该被正确处理。\n\n");
        }
        
        String text = longText.toString();
        logger.info("长文本总长度: {} 字符", text.length());
        
        List<TextChunk> chunks = splitter.split(text, "test-doc-4", null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("长文本分割产生了 {} 个块", chunks.size());
        
        // 验证分割的合理性
        assertTrue(chunks.size() > 1, "长文本应该被分割成多个块");
        
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            assertTrue(chunk.getContent().length() > 0, "每个块都应该有内容");
            assertEquals(i, chunk.getChunkIndex());
            
            logger.debug("块 {}: {} 字符", i, chunk.getContent().length());
        }
    }
    
    @Test
    @DisplayName("测试自定义配置")
    void testCustomConfiguration() {
        logger.info("测试自定义配置...");
        
        // 创建自定义配置
        SplitterConfig customConfig = new SplitterConfig();
        customConfig.setDefaultChunkSize(500);        // 较小的token块
        customConfig.setMinChunkSizeChars(200);       // 较小的最小字符数
        customConfig.setMinChunkLengthToEmbed(10);  // 较大的最小嵌入长度
        customConfig.setMaxNumChunks(100);          // 较少的最大块数
        customConfig.setKeepSeparator(false);         // 不保持分隔符
        
        TokenBasedSplitter customSplitter = new TokenBasedSplitter(customConfig);
        
        String text = "这是一个测试自定义配置的文本。" +
                     "我们可以验证不同的配置参数如何影响分割结果。" +
                     "较小的块大小应该产生更多的块。";
        
        List<TextChunk> chunks = customSplitter.split(text, "test-doc-5", null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        logger.info("自定义配置分割产生了 {} 个块", chunks.size());
        
        // 验证配置参数被正确应用
        Map<String, Object> info = customSplitter.getInfo();
        assertEquals(500, info.get("default_chunk_size"));
        assertEquals(200, info.get("min_chunk_size_chars"));
        assertEquals(10, info.get("min_chunk_length_to_embed"));
        assertEquals(100, info.get("max_num_chunks"));
        assertEquals(false, info.get("keep_separator"));
    }
    
    @Test
    @DisplayName("测试空文本和边界情况")
    void testEmptyAndEdgeCases() {
        logger.info("测试空文本和边界情况...");
        
        // 测试空文本
        List<TextChunk> emptyChunks = splitter.split("", "test-empty", null);
        assertNotNull(emptyChunks);
        assertTrue(emptyChunks.isEmpty());
        
        // 测试null文本
        List<TextChunk> nullChunks = splitter.split(null, "test-null", null);
        assertNotNull(nullChunks);
        assertTrue(nullChunks.isEmpty());
        
        // 测试空白文本
        List<TextChunk> whitespaceChunks = splitter.split("   \n\n  ", "test-whitespace", null);
        assertNotNull(whitespaceChunks);
        
        logger.info("边界情况测试通过");
    }
    
    @Test
    @DisplayName("测试元数据处理")
    void testMetadataHandling() {
        logger.info("测试元数据处理...");
        
        String text = "这是一个测试元数据处理的文本。";
        
        // 创建自定义元数据
        Map<String, Object> customMetadata = Map.of(
            "author", "测试作者",
            "category", "测试分类",
            "tags", List.of("测试", "元数据")
        );
        
        List<TextChunk> chunks = splitter.split(text, "test-metadata", customMetadata);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // 验证元数据被正确传递
        for (TextChunk chunk : chunks) {
            Map<String, Object> metadata = chunk.getMetadata();
            
            // 检查自定义元数据
            assertEquals("测试作者", metadata.get("author"));
            assertEquals("测试分类", metadata.get("category"));
            assertEquals(List.of("测试", "元数据"), metadata.get("tags"));
            
            // 检查自动生成的元数据
            assertEquals("test-metadata", metadata.get("document_id"));
            assertTrue((Boolean) metadata.get("token_based_splitter"));
            assertTrue((Boolean) metadata.get("spring_ai_token_splitter"));
        }
        
        logger.info("元数据处理测试通过");
    }
    
    @Test
    @DisplayName("测试分割器信息获取")
    void testSplitterInfo() {
        logger.info("测试分割器信息获取...");
        
        Map<String, Object> info = splitter.getInfo();
        
        assertNotNull(info);
        assertEquals("Token Based Splitter", info.get("name"));
        assertEquals("TOKEN_BASED", info.get("type"));
        assertEquals("2.0.0", info.get("version"));
        assertTrue((Boolean) info.get("spring_ai_based"));
        assertEquals("Spring AI TokenTextSplitter", info.get("implementation"));
        
        // 检查配置参数
        assertEquals(800, info.get("default_chunk_size"));
        assertEquals(350, info.get("min_chunk_size_chars"));
        assertEquals(5, info.get("min_chunk_length_to_embed"));
        assertEquals(10000, info.get("max_num_chunks"));
        assertEquals(true, info.get("keep_separator"));
        
        logger.info("分割器信息: {}", info);
    }
}