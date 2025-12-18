# Text Splitter Module Usage Guide

## Overview

The Text Splitter module provides intelligent text segmentation capabilities for RAG (Retrieval-Augmented Generation) systems. It supports multiple splitting strategies to maintain semantic coherence and optimize chunk sizes for different use cases.

## Features

- **Multiple Splitting Strategies**: Semantic, Fixed Length, Token-based, and Recursive Character splitting
- **Multi-language Support**: Optimized for Chinese, English, and mixed-language texts
- **Configurable Parameters**: Flexible chunk size, overlap, and boundary preservation settings
- **Factory Pattern**: Easy creation and management of different splitter types
- **Comprehensive Metadata**: Detailed chunk information and statistics
- **Performance Optimized**: Efficient processing of large texts

## Architecture

### Core Components

1. **TextSplitter Interface**: Defines the contract for all text splitters
2. **AbstractTextSplitter**: Base class providing common functionality
3. **TextChunk**: Model class representing split text segments
4. **SplitterConfig**: Configuration class for all splitting parameters
5. **TextSplitterFactory**: Factory class for creating splitter instances

### Splitter Types

#### 1. SemanticTextSplitter
- **Strategy**: Groups text based on semantic similarity
- **Best for**: Maintaining topic coherence and semantic integrity
- **Features**: Keyword extraction, topic analysis, intelligent grouping

#### 2. FixedLengthSplitter
- **Strategy**: Splits text into fixed-size chunks
- **Best for**: Consistent chunk sizes for uniform processing
- **Features**: Boundary preservation, smart overlap handling

#### 3. TokenBasedSplitter
- **Strategy**: Splits based on token count rather than character count
- **Best for**: AI/ML applications requiring consistent token counts
- **Features**: Multi-language token calculation, token type statistics

#### 4. RecursiveCharacterSplitter
- **Strategy**: Recursively tries different separators to find optimal splits
- **Best for**: Complex documents requiring hierarchical splitting
- **Features**: Multi-level separator hierarchy, quality scoring

## Quick Start

### Basic Usage

```java
import com.wangliang.agentj.rag.textsplitting.*;

// Create a factory instance
TextSplitterFactory factory = TextSplitterFactory.getInstance();

// Create a semantic splitter
TextSplitter splitter = factory.createSplitter("semantic");

// Split text
String text = "Your long text content here...";
List<TextChunk> chunks = splitter.split(text, "document_id", null);

// Process chunks
for (TextChunk chunk : chunks) {
    System.out.println("Chunk " + chunk.getChunkIndex() + ": " + chunk.getContent());
}
```

### Using Different Splitter Types

```java
// Semantic splitting (maintains topic coherence)
TextSplitter semanticSplitter = factory.createSplitter("semantic");

// Fixed length splitting (consistent sizes)
TextSplitter fixedSplitter = factory.createSplitter("fixed");

// Token-based splitting (AI/ML optimized)
TextSplitter tokenSplitter = factory.createSplitter("token");

// Recursive character splitting (hierarchical)
TextSplitter recursiveSplitter = factory.createSplitter("recursive");
```

### Custom Configuration

```java
// Create custom configuration
SplitterConfig config = new SplitterConfig();
config.setChunkSize(800);
config.setChunkOverlap(150);
config.setSemanticThreshold(0.75);
config.setPreserveSentenceBoundary(true);
config.setPreserveParagraphBoundary(true);

// Create splitter with custom config
TextSplitter customSplitter = factory.createSplitter("semantic", config);
```

## Configuration Options

### Basic Parameters

| Parameter | Description | Default | Range |
|-----------|-------------|---------|--------|
| chunkSize | Target chunk size (characters/tokens) | 1000 | 50-5000 |
| chunkOverlap | Overlap between chunks | 200 | 0-1000 |
| maxChunkSize | Maximum allowed chunk size | 2000 | 100-10000 |
| minChunkSize | Minimum allowed chunk size | 100 | 10-500 |

### Semantic Parameters

| Parameter | Description | Default | Range |
|-----------|-------------|---------|--------|
| semanticThreshold | Similarity threshold for grouping | 0.7 | 0.0-1.0 |
| semanticWindowSize | Context window for semantic analysis | 3 | 1-10 |

### Boundary Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| preserveSentenceBoundary | Keep sentence boundaries | true |
| preserveParagraphBoundary | Keep paragraph boundaries | true |

### Separator Configuration

```java
// Custom separators for different languages
List<String> customSeparators = Arrays.asList(
    "\n\n",     // Paragraph
    "\n",       // Line break
    "。",       // Chinese period
    ".",        // English period
    "!",        // Exclamation
    "?",        // Question mark
    " "         // Space
);

config.setSeparators(customSeparators);
```

## Advanced Usage

### Multi-language Support

```java
// For Chinese text
SplitterConfig chineseConfig = SplitterConfig.semanticConfig();
chineseConfig.setChunkSize(600); // Chinese characters are more information-dense
TextSplitter chineseSplitter = factory.createSplitter("semantic", chineseConfig);

// For English text
SplitterConfig englishConfig = SplitterConfig.fixedLengthConfig();
englishConfig.setChunkSize(1000);
TextSplitter englishSplitter = factory.createSplitter("fixed", englishConfig);

// For mixed text
SplitterConfig mixedConfig = SplitterConfig.tokenBasedConfig();
mixedConfig.setChunkSize(256); // Token-based for mixed content
TextSplitter mixedSplitter = factory.createSplitter("token", mixedConfig);
```

### Batch Processing

```java
// Create multiple splitters
List<TextSplitter> splitters = factory.createSplitters("semantic", "fixed", "token");

// Process text with different strategies
String document = "Your document content...";
Map<String, List<TextChunk>> results = new HashMap<>();

for (TextSplitter splitter : splitters) {
    List<TextChunk> chunks = splitter.split(document, "doc_id", null);
    results.put(splitter.getName(), chunks);
}
```

### Custom Metadata

```java
// Create metadata for document processing
Map<String, Object> metadata = new HashMap<>();
metadata.put("document_type", "research_paper");
metadata.put("language", "chinese");
metadata.put("domain", "technology");
metadata.put("processing_timestamp", System.currentTimeMillis());

// Split with metadata
List<TextChunk> chunks = splitter.split(text, "doc_id", metadata);

// Access chunk metadata
for (TextChunk chunk : chunks) {
    Map<String, Object> chunkMetadata = chunk.getMetadata();
    System.out.println("Tokens: " + chunkMetadata.get("total_tokens"));
    System.out.println("Quality: " + chunkMetadata.get("quality_score"));
}
```

### Performance Optimization

```java
// For large documents
SplitterConfig performanceConfig = new SplitterConfig();
performanceConfig.setChunkSize(2000); // Larger chunks for better performance
performanceConfig.setChunkOverlap(100); // Minimal overlap
performanceConfig.setPreserveSentenceBoundary(false); // Skip boundary checking for speed

TextSplitter performanceSplitter = factory.createSplitter("fixed", performanceConfig);
```

## Configuration Presets

### Semantic Configuration
```java
SplitterConfig semanticConfig = SplitterConfig.semanticConfig();
// Optimized for: 800 chars, 150 overlap, 0.75 threshold
```

### Fixed Length Configuration
```java
SplitterConfig fixedConfig = SplitterConfig.fixedLengthConfig();
// Optimized for: 1000 chars, 200 overlap
```

### Token-based Configuration
```java
SplitterConfig tokenConfig = SplitterConfig.tokenBasedConfig();
// Optimized for: 256 tokens, 50 token overlap
```

### Recursive Configuration
```java
SplitterConfig recursiveConfig = SplitterConfig.recursiveConfig();
// Optimized for: hierarchical splitting with quality scoring
```

## Error Handling

```java
try {
    TextSplitter splitter = factory.createSplitter("unknown_type");
} catch (IllegalArgumentException e) {
    logger.error("Unknown splitter type: {}", e.getMessage());
}

try {
    SplitterConfig invalidConfig = new SplitterConfig();
    invalidConfig.setChunkSize(0); // Invalid size
    invalidConfig.validate(); // Will throw exception
} catch (IllegalArgumentException e) {
    logger.error("Invalid configuration: {}", e.getMessage());
}
```

## Best Practices

### 1. Choose the Right Splitter
- **SemanticTextSplitter**: Use for content where topic coherence is critical
- **FixedLengthSplitter**: Use for consistent processing requirements
- **TokenBasedSplitter**: Use for AI/ML applications with token limits
- **RecursiveCharacterSplitter**: Use for complex documents with mixed content types

### 2. Configure Appropriately
- Start with default configurations
- Adjust chunk size based on your target model's limitations
- Use overlap to maintain context between chunks
- Enable boundary preservation for better readability

### 3. Handle Different Languages
- Use smaller chunk sizes for Chinese (information-dense)
- Use larger chunk sizes for English
- Enable mixed-language support for diverse content

### 4. Monitor Performance
- Log chunk creation statistics
- Monitor processing time for large documents
- Use quality scores to evaluate splitting effectiveness

## Testing

Run the comprehensive test suite:

```bash
mvn test -Dtest=TextSplitterTest
```

Key test scenarios:
- Basic functionality for all splitter types
- Multi-language support
- Large text processing
- Configuration validation
- Performance benchmarking
- Error handling

## Examples

### Example 1: Research Paper Processing

```java
// Process academic papers with semantic splitting
SplitterConfig researchConfig = SplitterConfig.semanticConfig();
researchConfig.setChunkSize(1000);
researchConfig.setSemanticThreshold(0.8);
researchConfig.setPreserveParagraphBoundary(true);

TextSplitter researchSplitter = factory.createSplitter("semantic", researchConfig);

String researchPaper = loadResearchPaper();
List<TextChunk> sections = researchSplitter.split(researchPaper, "paper_001", null);

// Process each section
for (TextChunk section : sections) {
    processResearchSection(section);
}
```

### Example 2: Chatbot Training Data

```java
// Prepare training data with token-based splitting
SplitterConfig chatbotConfig = SplitterConfig.tokenBasedConfig();
chatbotConfig.setChunkSize(512); // GPT model token limit
chatbotConfig.setTokenOverlap(50);

TextSplitter chatbotSplitter = factory.createSplitter("token", chatbotConfig);

String trainingData = loadTrainingData();
List<TextChunk> trainingChunks = chatbotSplitter.split(trainingData, "training_001", null);

// Prepare for model training
List<String> trainingTexts = trainingChunks.stream()
    .map(TextChunk::getContent)
    .collect(Collectors.toList());
```

### Example 3: Document Search Indexing

```java
// Create search index with recursive splitting
SplitterConfig searchConfig = SplitterConfig.recursiveConfig();
searchConfig.setChunkSize(800);
searchConfig.setPreserveSentenceBoundary(true);

TextSplitter searchSplitter = factory.createSplitter("recursive", searchConfig);

List<Document> documents = loadDocuments();
List<TextChunk> allChunks = new ArrayList<>();

for (Document doc : documents) {
    List<TextChunk> docChunks = searchSplitter.split(doc.getContent(), doc.getId(), doc.getMetadata());
    allChunks.addAll(docChunks);
}

// Create search index
createSearchIndex(allChunks);
```

## Troubleshooting

### Common Issues

1. **Chunks too large/small**: Adjust `chunkSize` and `maxChunkSize` parameters
2. **Poor semantic coherence**: Increase `semanticThreshold` or use semantic splitter
3. **Slow performance**: Use larger chunk sizes or fixed-length splitter
4. **Language detection issues**: Set language-specific configurations
5. **Memory issues**: Process documents in batches

### Debug Information

```java
// Enable detailed logging
logger.info("Splitter info: {}", splitter.getInfo());
logger.info("Configuration: {}", config);

// Log chunk statistics
for (TextChunk chunk : chunks) {
    logger.info("Chunk {}: {} chars, {} tokens, metadata: {}",
               chunk.getChunkIndex(),
               chunk.getCharacterCount(),
               chunk.getMetadata().get("total_tokens"),
               chunk.getMetadata());
}
```

## Conclusion

The Text Splitter module provides a comprehensive solution for text segmentation in RAG systems. By choosing the appropriate splitting strategy and configuration, you can optimize your text processing pipeline for better semantic understanding and retrieval performance.

For more examples and advanced usage patterns, refer to the test cases and source code documentation.