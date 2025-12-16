package com.wangliang.agentj.rag.documentloader.parsers;

import com.wangliang.agentj.rag.common.models.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonDocumentParser测试类
 */
class JsonDocumentParserTest {

    private static final Logger logger = LoggerFactory.getLogger(JsonDocumentParserTest.class);
    private JsonDocumentParser parser;

    @BeforeEach
    void setUp() {
        parser = new JsonDocumentParser();
    }

    @Test
    void testSupportsJsonFileType() {
        assertTrue(parser.supports("json"));
        assertTrue(parser.supports("JSON"));
        assertFalse(parser.supports("txt"));
        assertFalse(parser.supports("pdf"));
    }

    @Test
    void testGetSupportedTypes() {
        String[] supportedTypes = parser.getSupportedTypes();
        assertEquals(1, supportedTypes.length);
        assertEquals("json", supportedTypes[0]);
    }

    @Test
    void testGetNameAndDescription() {
        assertEquals("JSON Document Parser", parser.getName());
        assertNotNull(parser.getDescription());
        assertFalse(parser.getDescription().isEmpty());
    }

    @Test
    void testParseSimpleJsonWithContentField() throws IOException {
        String jsonContent = "{\"title\": \"Test Document\", \"content\": \"This is the main content of the document.\", \"author\": \"John Doe\"}";
        
        Document document = parser.parse(jsonContent, "test.json");
        
        assertNotNull(document);
        assertEquals("This is the main content of the document.", document.getContent());
        assertEquals("test.json", document.getTitle());
        assertEquals("test.json", document.getSource());
        assertEquals("json", document.getFileType());
        assertNotNull(document.getMetadata());
        assertEquals("John Doe", document.getMetadata().get("author"));
        assertEquals("Test Document", document.getMetadata().get("title"));
    }

    @Test
    void testParseJsonWithTextField() throws IOException {
        String jsonContent = "{\"text\": \"This is text content\", \"description\": \"A description\"}";
        
        Document document = parser.parse(jsonContent, "text-test.json");
        
        assertNotNull(document);
        assertEquals("This is text content", document.getContent());
    }

    @Test
    void testParseJsonWithBodyField() throws IOException {
        String jsonContent = "{\"body\": \"This is body content\", \"title\": \"Article Title\"}";
        
        Document document = parser.parse(jsonContent, "body-test.json");
        
        assertNotNull(document);
        assertEquals("This is body content", document.getContent());
    }

    @Test
    void testParseJsonWithDescriptionField() throws IOException {
        String jsonContent = "{\"description\": \"This is a description\", \"type\": \"info\"}";
        
        Document document = parser.parse(jsonContent, "desc-test.json");
        
        assertNotNull(document);
        assertEquals("This is a description", document.getContent());
    }

    @Test
    void testParseJsonWithoutContentField() throws IOException {
        String jsonContent = "{\"name\": \"Test\", \"value\": 123, \"active\": true}";
        
        Document document = parser.parse(jsonContent, "no-content-test.json");
        
        assertNotNull(document);
        assertNotNull(document.getContent());
        // When no content field is found, it should convert the entire JSON to pretty string
        assertTrue(document.getContent().contains("\"name\" : \"Test\""));
        assertTrue(document.getContent().contains("\"value\" : 123"));
    }

    @Test
    void testParseComplexNestedJson() throws IOException {
        String jsonContent = "{\"article\": {\"title\": \"Nested Article\", \"content\": \"Nested content here\"}, \"metadata\": {\"author\": \"Jane\", \"date\": \"2024-01-01\"}}";
        
        Document document = parser.parse(jsonContent, "complex-test.json");
        
        assertNotNull(document);
        // Should extract content from nested structure
        assertNotNull(document.getContent());
        assertFalse(document.getContent().isEmpty());
    }

    @Test
    void testParseJsonArray() throws IOException {
        String jsonContent = "[{\"content\": \"First item\"}, {\"content\": \"Second item\"}]";
        
        Document document = parser.parse(jsonContent, "array-test.json");
        
        assertNotNull(document);
        assertNotNull(document.getContent());
        assertFalse(document.getContent().isEmpty());
    }

    @Test
    void testParseEmptyJson() throws IOException {
        String jsonContent = "{}";
        
        Document document = parser.parse(jsonContent, "empty-test.json");
        
        assertNotNull(document);
        assertNotNull(document.getContent());
        assertEquals("{}", document.getContent().trim());
    }

    @Test
    void testParseInvalidJson() {
        String invalidJson = "{invalid json";
        
        IOException exception = assertThrows(IOException.class, () -> {
            parser.parse(invalidJson, "invalid-test.json");
        });
        
        assertTrue(exception.getMessage().contains("Failed to parse JSON content"));
    }

    @Test
    void testParseFromInputStream() throws IOException {
        String jsonContent = "{\"content\": \"Stream content\", \"title\": \"Stream Test\"}";
        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        
        Document document = parser.parse(inputStream, "stream-test.json");
        
        assertNotNull(document);
        assertEquals("Stream content", document.getContent());
        assertEquals("stream-test.json", document.getTitle());
    }

    @Test
    void testLargeJsonContent() throws IOException {
        // Create a large JSON with many fields
        StringBuilder largeJson = new StringBuilder("{");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeJson.append(",");
            largeJson.append("\"field").append(i).append("\": \"value").append(i).append("\"");
        }
        largeJson.append(", \"content\": \"This is the main content in a large JSON\"}");
        
        Document document = parser.parse(largeJson.toString(), "large-test.json");
        
        assertNotNull(document);
        assertEquals("This is the main content in a large JSON", document.getContent());
    }
}