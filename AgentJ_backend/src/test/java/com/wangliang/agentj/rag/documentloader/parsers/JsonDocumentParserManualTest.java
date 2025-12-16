package com.wangliang.agentj.rag.documentloader.parsers;

import com.wangliang.agentj.rag.common.models.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * JsonDocumentParser手动测试类
 * 用于快速验证JsonDocumentParser的功能
 */
public class JsonDocumentParserManualTest {

    public static void main(String[] args) {
        JsonDocumentParser parser = new JsonDocumentParser();
        
        System.out.println("=== JsonDocumentParser Manual Test ===");
        System.out.println("Parser Name: " + parser.getName());
        System.out.println("Parser Description: " + parser.getDescription());
        System.out.println("Supports JSON: " + parser.supports("json"));
        System.out.println("Supports TXT: " + parser.supports("txt"));
        System.out.println("Supported Types: " + String.join(", ", parser.getSupportedTypes()));
        System.out.println();
        
        // 测试用例1：包含content字段的JSON
        testCase1(parser);
        
        // 测试用例2：包含text字段的JSON
        testCase2(parser);
        
        // 测试用例3：包含body字段的JSON
        testCase3(parser);
        
        // 测试用例4：没有标准内容字段的JSON
        testCase4(parser);
        
        // 测试用例5：复杂的嵌套JSON
        testCase5(parser);
        
        // 测试用例6：从输入流解析
        testCase6(parser);
        
        // 测试用例7：JSON数组
        testCase7(parser);
        
        // 测试用例8：错误处理
        testCase8(parser);
    }
    
    private static void testCase1(JsonDocumentParser parser) {
        System.out.println("--- Test Case 1: JSON with content field ---");
        String json = "{\"title\": \"Test Article\", \"content\": \"This is the main content of the article.\", \"author\": \"John Doe\", \"date\": \"2024-01-15\"}";
        try {
            Document doc = parser.parse(json, "test1.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testCase2(JsonDocumentParser parser) {
        System.out.println("--- Test Case 2: JSON with text field ---");
        String json = "{\"text\": \"This is text content\", \"description\": \"A short description\"}";
        try {
            Document doc = parser.parse(json, "test2.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testCase3(JsonDocumentParser parser) {
        System.out.println("--- Test Case 3: JSON with body field ---");
        String json = "{\"body\": \"This is the body content\", \"title\": \"Article Title\", \"category\": \"technology\"}";
        try {
            Document doc = parser.parse(json, "test3.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testCase4(JsonDocumentParser parser) {
        System.out.println("--- Test Case 4: JSON without standard content fields ---");
        String json = "{\"name\": \"Configuration\", \"value\": 42, \"active\": true, \"tags\": [\"important\", \"system\"]}";
        try {
            Document doc = parser.parse(json, "test4.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testCase5(JsonDocumentParser parser) {
        System.out.println("--- Test Case 5: Complex nested JSON ---");
        String json = "{\"article\": {\"title\": \"Nested Article\", \"content\": \"This is nested content\", \"metadata\": {\"author\": \"Jane Smith\", \"date\": \"2024-01-01\"}}, \"status\": \"published\"}";
        try {
            Document doc = parser.parse(json, "test5.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testCase6(JsonDocumentParser parser) {
        System.out.println("--- Test Case 6: Parse from InputStream ---");
        String json = "{\"content\": \"Stream content\", \"title\": \"Stream Test\", \"version\": \"1.0\"}";
        try {
            InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            Document doc = parser.parse(inputStream, "test6.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testCase7(JsonDocumentParser parser) {
        System.out.println("--- Test Case 7: JSON Array ---");
        String json = "[{\"content\": \"First item\"}, {\"content\": \"Second item\"}, {\"content\": \"Third item\"}]";
        try {
            Document doc = parser.parse(json, "test7.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testCase8(JsonDocumentParser parser) {
        System.out.println("--- Test Case 8: Invalid JSON (Error Handling) ---");
        String invalidJson = "{invalid json";
        try {
            Document doc = parser.parse(invalidJson, "invalid-test.json");
            printDocumentInfo(doc);
        } catch (IOException e) {
            System.out.println("Expected error caught: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void printDocumentInfo(Document doc) {
        if (doc == null) {
            System.out.println("Document is null");
            return;
        }
        
        System.out.println("Document ID: " + doc.getId());
        System.out.println("Title: " + doc.getTitle());
        System.out.println("Source: " + doc.getSource());
        System.out.println("File Type: " + doc.getFileType());
        System.out.println("Content Length: " + (doc.getContent() != null ? doc.getContent().length() : 0));
        System.out.println("Content Preview: " + (doc.getContent() != null ? 
            (doc.getContent().length() > 100 ? doc.getContent().substring(0, 100) + "..." : doc.getContent()) : "null"));
        
        if (doc.getMetadata() != null && !doc.getMetadata().isEmpty()) {
            System.out.println("Metadata:");
            doc.getMetadata().forEach((key, value) -> {
                System.out.println("  " + key + ": " + value);
            });
        } else {
            System.out.println("Metadata: empty");
        }
    }
}