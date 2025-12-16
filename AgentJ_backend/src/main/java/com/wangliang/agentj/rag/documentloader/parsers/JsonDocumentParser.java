package com.wangliang.agentj.rag.documentloader.parsers;

import com.wangliang.agentj.rag.common.models.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
class JsonDocumentParser implements DocumentParser {

    private static final Logger logger  = LoggerFactory.getLogger(JsonDocumentParser.class);

    @Override
    public Document parse(InputStream inputStream, String fileName) throws IOException {
        logger.info("Starting to parse JSON file: {}", fileName);

        try {
            // Read the entire input stream into a string
            String content = readInputStream(inputStream);
            return parseContent(content, fileName);
            
        } catch (Exception e) {
            logger.error("Error parsing JSON file {}: {}", fileName, e.getMessage());
            throw new IOException("Failed to parse JSON file: " + fileName, e);
        }
    }

    @Override
    public Document parse(String content, String fileName) throws IOException {
        logger.info("Starting to parse JSON content: {}", fileName);
        return parseContent(content, fileName);
    }
    
    /**
     * Parse JSON content and convert to Document format
     */
    private Document parseContent(String content, String fileName) throws IOException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(content);
            
            // Extract content from JSON - try different common fields
            String extractedContent = extractContentFromJson(rootNode);
            
            if (extractedContent == null || extractedContent.trim().isEmpty()) {
                logger.warn("JSON content {} has no extractable content", fileName);
                return null;
            }
            
            // Create metadata from JSON structure
            Map<String, Object> metadata = extractMetadataFromJson(rootNode);
            
            // Convert to our Document format
            Document document = new Document();
            document.setId(UUID.randomUUID().toString());
            document.setContent(extractedContent);
            document.setTitle(fileName);
            document.setSource(fileName);
            document.setFileType("json");
            document.setMetadata(metadata);
            
            logger.info("Successfully parsed JSON content: {}, generated document ID: {}", fileName, document.getId());
            return document;
            
        } catch (Exception e) {
            logger.error("Error parsing JSON content {}: {}", fileName, e.getMessage());
            throw new IOException("Failed to parse JSON content: " + fileName, e);
        }
    }

    @Override
    public boolean supports(String fileType) {
        return "json".equalsIgnoreCase(fileType);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{"json"};
    }

    @Override
    public String getName() {
        return "JSON Document Parser";
    }

    @Override
    public String getDescription() {
        return "Parser for JSON format documents, supports converting JSON content to standard document format";
    }
    
    /**
     * Read input stream into string
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            content.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return content.toString();
    }
    
    /**
     * Extract content from JSON node - try common text fields
     */
    private String extractContentFromJson(JsonNode rootNode) {
        // Try different common content fields
        String[] contentFields = {"content", "text", "body", "description", "summary", "data"};
        
        for (String field : contentFields) {
            JsonNode contentNode = rootNode.get(field);
            if (contentNode != null && contentNode.isTextual() && !contentNode.asText().trim().isEmpty()) {
                return contentNode.asText();
            }
        }
        
        // If no specific content field found, convert entire JSON to string
        return rootNode.toPrettyString();
    }
    
    /**
     * Extract metadata from JSON structure
     */
    private Map<String, Object> extractMetadataFromJson(JsonNode rootNode) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Add common metadata fields
        String[] metaFields = {"title", "author", "date", "version", "type", "category"};
        for (String field : metaFields) {
            JsonNode node = rootNode.get(field);
            if (node != null && !node.isNull()) {
                metadata.put(field, node.asText());
            }
        }
        
        // Add JSON structure info
        metadata.put("json_type", rootNode.getNodeType().toString());
        metadata.put("has_children", rootNode.size() > 0);
        
        return metadata;
    }
}