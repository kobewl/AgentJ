package com.wangliang.agentj.rag.textsplitting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 文本分割器手动测试类
 * 用于快速验证分割器功能和调试
 */
public class TextSplitterManualTest {

    private static final Logger logger = LoggerFactory.getLogger(TextSplitterManualTest.class);
    
    public static void main(String[] args) {
        logger.info("Starting Text Splitter Manual Test...");
        
        TextSplitterManualTest test = new TextSplitterManualTest();
        
        try {
            // 运行各种测试
            test.testAllSplitters();
            test.testFactory();
            test.testPerformance();
            test.testEdgeCases();
            
            logger.info("All manual tests completed successfully!");
            
        } catch (Exception e) {
            logger.error("Manual test failed", e);
        }
    }
    
    /**
     * 测试所有分割器类型
     */
    private void testAllSplitters() {
        logger.info("=== Testing All Splitter Types ===");
        
        // 测试文本
        String chineseText = "人工智能是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。" +
                           "该领域的研究包括机器人、语言识别、图像识别、自然语言处理和专家系统等。" +
                           "人工智能从诞生以来，理论和技术日益成熟，应用领域也不断扩大。";
        
        String englishText = "Artificial intelligence (AI) is intelligence demonstrated by machines, in contrast to the natural " +
                           "intelligence displayed by humans and animals. Leading AI textbooks define the field as the study " +
                           "of intelligent agents: any device that perceives its environment and takes actions that maximize " +
                           "its chance of successfully achieving its goals.";
        
        String[] splitterTypes = {"semantic", "fixed", "token", "recursive"};
        
        for (String type : splitterTypes) {
            logger.info("Testing {} splitter...", type);
            
            try {
                TextSplitterFactory factory = TextSplitterFactory.getInstance();
                TextSplitter splitter = factory.createSplitter(type);
                
                // 测试中文文本
                List<TextChunk> chineseChunks = splitter.split(chineseText, "chinese_test", null);
                logger.info("{} splitter created {} Chinese chunks", type, chineseChunks.size());
                
                // 测试英文文本
                List<TextChunk> englishChunks = splitter.split(englishText, "english_test", null);
                logger.info("{} splitter created {} English chunks", type, englishChunks.size());
                
                // 显示第一个块的信息
                if (!chineseChunks.isEmpty()) {
                    TextChunk firstChunk = chineseChunks.get(0);
                    logger.info("First Chinese chunk: {} chars, metadata: {}", 
                               firstChunk.getContent().length(), firstChunk.getMetadata());
                }
                
                if (!englishChunks.isEmpty()) {
                    TextChunk firstChunk = englishChunks.get(0);
                    logger.info("First English chunk: {} chars, metadata: {}", 
                               firstChunk.getContent().length(), firstChunk.getMetadata());
                }
                
            } catch (Exception e) {
                logger.error("Error testing {} splitter: {}", type, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 测试工厂功能
     */
    private void testFactory() {
        logger.info("=== Testing Factory Functionality ===");
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        
        // 测试可用类型
        Set<String> availableTypes = factory.getAvailableTypes();
        logger.info("Available splitter types: {}", availableTypes);
        
        // 测试获取分割器信息
        for (String type : availableTypes) {
            Map<String, Object> info = factory.getSplitterInfo(type);
            logger.info("{} splitter info: {}", type, info);
        }
        
        // 测试批量创建
        List<TextSplitter> splitters = factory.createSplitters(new ArrayList<>(availableTypes));
        logger.info("Created {} splitters", splitters.size());
        
        // 测试工厂统计信息
        Map<String, Object> stats = factory.getFactoryStats();
        logger.info("Factory stats: {}", stats);
    }
    
    /**
     * 测试性能
     */
    private void testPerformance() {
        logger.info("=== Testing Performance ===");
        
        // 生成大文本
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            largeText.append("人工智能是计算机科学的一个分支，它企图了解智能的实质。");
            largeText.append("Artificial intelligence is a branch of computer science that attempts to understand the essence of intelligence. ");
            largeText.append("机器学习Machine Learning是人工智能的核心技术之一。");
            largeText.append("\n\n");
        }
        String largeContent = largeText.toString();
        
        logger.info("Large text size: {} characters", largeContent.length());
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        String[] types = {"semantic", "fixed", "token", "recursive"};
        
        for (String type : types) {
            try {
                TextSplitter splitter = factory.createSplitter(type);
                
                long startTime = System.currentTimeMillis();
                List<TextChunk> chunks = splitter.split(largeContent, "perf_test_" + type, null);
                long endTime = System.currentTimeMillis();
                
                logger.info("{} splitter: {} chunks in {} ms (avg: {:.2f} ms/chunk)", 
                           type, chunks.size(), (endTime - startTime), 
                           (double) (endTime - startTime) / chunks.size());
                
            } catch (Exception e) {
                logger.error("Performance test failed for {}: {}", type, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 测试边界情况
     */
    private void testEdgeCases() {
        logger.info("=== Testing Edge Cases ===");
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        TextSplitter splitter = factory.createSplitter("fixed");
        
        // 测试空文本
        logger.info("Testing empty text...");
        List<TextChunk> emptyChunks = splitter.split("", "empty_test", null);
        logger.info("Empty text result: {} chunks", emptyChunks.size());
        
        // 测试null文本
        logger.info("Testing null text...");
        List<TextChunk> nullChunks = splitter.split(null, "null_test", null);
        logger.info("Null text result: {} chunks", nullChunks.size());
        
        // 测试空白文本
        logger.info("Testing whitespace text...");
        List<TextChunk> whitespaceChunks = splitter.split("   \n\n  \t  ", "whitespace_test", null);
        logger.info("Whitespace text result: {} chunks", whitespaceChunks.size());
        
        // 测试单行文本
        logger.info("Testing single line text...");
        String singleLine = "This is a single line of text with no breaks.";
        List<TextChunk> singleLineChunks = splitter.split(singleLine, "single_line_test", null);
        logger.info("Single line text result: {} chunks", singleLineChunks.size());
        
        // 显示单行文本的分割结果
        if (!singleLineChunks.isEmpty()) {
            for (int i = 0; i < singleLineChunks.size(); i++) {
                TextChunk chunk = singleLineChunks.get(i);
                logger.info("Chunk {}: '{}' ({} chars)", i, chunk.getContent(), chunk.getContent().length());
            }
        }
        
        // 测试非常长的单词
        logger.info("Testing very long word...");
        String longWord = "supercalifragilisticexpialidocious";
        List<TextChunk> longWordChunks = splitter.split(longWord, "long_word_test", null);
        logger.info("Long word result: {} chunks", longWordChunks.size());
        
        // 测试特殊字符
        logger.info("Testing special characters...");
        String specialChars = "Hello! How are you? I'm fine. Thanks! 你好！你好吗？我很好。谢谢！";
        List<TextChunk> specialChunks = splitter.split(specialChars, "special_chars_test", null);
        logger.info("Special characters result: {} chunks", specialChunks.size());
        
        // 显示特殊字符的分割结果
        if (!specialChunks.isEmpty()) {
            for (int i = 0; i < specialChunks.size(); i++) {
                TextChunk chunk = specialChunks.get(i);
                logger.info("Chunk {}: '{}' ({} chars)", i, chunk.getContent(), chunk.getContent().length());
            }
        }
    }
    
    /**
     * 测试自定义配置
     */
    private void testCustomConfiguration() {
        logger.info("=== Testing Custom Configuration ===");
        
        // 创建自定义配置
        SplitterConfig customConfig = new SplitterConfig();
        customConfig.setChunkSize(500);
        customConfig.setChunkOverlap(100);
        customConfig.setMaxChunkSize(800);
        customConfig.setMinChunkSize(50);
        customConfig.setSemanticThreshold(0.8);
        customConfig.setPreserveSentenceBoundary(true);
        customConfig.setPreserveParagraphBoundary(true);
        
        // 自定义分隔符
        List<String> customSeparators = Arrays.asList(
            "\n\n",     // 段落
            "\n",       // 换行
            "。",       // 中文句号
            ".",        // 英文句号
            "!",        // 感叹号
            "?"         // 问号
        );
        customConfig.setSeparators(customSeparators);
        
        logger.info("Custom configuration: {}", customConfig);
        
        // 使用自定义配置创建分割器
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        TextSplitter customSplitter = factory.createSplitter("semantic", customConfig);
        
        String testText = "这是第一段。This is the first paragraph.\n\n" +
                         "这是第二段！This is the second paragraph!\n\n" +
                         "这是第三段？This is the third paragraph?\n\n" +
                         "这是最后一段。This is the final paragraph.";
        
        List<TextChunk> chunks = customSplitter.split(testText, "custom_config_test", null);
        logger.info("Custom config result: {} chunks", chunks.size());
        
        // 显示详细结果
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            logger.info("Chunk {}: '{}' ({} chars, metadata: {})", 
                       i, chunk.getContent(), chunk.getContent().length(), chunk.getMetadata());
        }
    }
    
    /**
     * 测试元数据和统计信息
     */
    private void testMetadataAndStatistics() {
        logger.info("=== Testing Metadata and Statistics ===");
        
        TextSplitterFactory factory = TextSplitterFactory.getInstance();
        
        String[] types = {"semantic", "fixed", "token", "recursive"};
        
        for (String type : types) {
            try {
                TextSplitter splitter = factory.createSplitter(type);
                
                String testText = "人工智能AI是计算机科学的一个分支。" +
                                 "Artificial Intelligence is a branch of computer science." +
                                 "机器学习Machine Learning是核心技术之一。";
                
                List<TextChunk> chunks = splitter.split(testText, "metadata_test_" + type, null);
                
                logger.info("=== {} Splitter Metadata ===", type.toUpperCase());
                
                // 显示分割器信息
                Map<String, Object> splitterInfo = splitter.getInfo();
                logger.info("Splitter Info: {}", splitterInfo);
                
                // 显示每个块的元数据
                for (int i = 0; i < chunks.size(); i++) {
                    TextChunk chunk = chunks.get(i);
                    Map<String, Object> metadata = chunk.getMetadata();
                    
                    logger.info("Chunk {}: {} chars, {} words, metadata keys: {}", 
                               i, chunk.getCharacterCount(), chunk.getWordCount(), metadata.keySet());
                    
                    // 显示特定的元数据
                    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                        logger.info("  {}: {}", entry.getKey(), entry.getValue());
                    }
                }
                
            } catch (Exception e) {
                logger.error("Metadata test failed for {}: {}", type, e.getMessage(), e);
            }
        }
    }
}