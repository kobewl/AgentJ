package com.wangliang.agentj.rag.documentloader.parsers;

import com.wangliang.agentj.rag.common.models.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Word文档解析器
 * 支持解析.docx和.doc格式的Word文档
 */
public class WordDocumentParser implements DocumentParser {
    
    private static final Logger logger = LoggerFactory.getLogger(WordDocumentParser.class);
    
    private static final List<String> SUPPORTED_TYPES = Arrays.asList("docx");
    
    @Override
    public Document parse(InputStream inputStream, String fileName) throws IOException {
        logger.info("开始解析Word文件: {}", fileName);
        
        String fileType = getFileExtension(fileName);
        Document document = new Document();
        
        try {
            if ("docx".equalsIgnoreCase(fileType)) {
                parseDocx(inputStream, document, fileName);
            } else {
                throw new IOException("不支持的Word文件格式: " + fileType + "，目前只支持.docx格式");
            }
            
            logger.info("Word文件解析完成: {}, 字符数: {}", fileName, 
                       document.getContent() != null ? document.getContent().length() : 0);
            
            return document;
            
        } catch (Exception e) {
            logger.error("解析Word文件失败: {}", fileName, e);
            throw new IOException("Failed to parse Word file: " + fileName, e);
        }
    }
    
    @Override
    public Document parse(String content, String fileName) throws IOException {
        // Word文档需要通过输入流解析
        throw new UnsupportedOperationException("Word parser requires InputStream");
    }
    
    private LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void parseDocx(InputStream inputStream, Document document, String fileName) throws IOException {
        try (XWPFDocument docxDocument = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            
            // 提取所有段落文本
            List<XWPFParagraph> paragraphs = docxDocument.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }
            
            // 设置文档基本信息
            document.setId(generateDocumentId(fileName));
            document.setContent(content.toString());
            document.setTitle(extractTitleFromFileName(fileName));
            document.setSource(fileName);
            document.setFileType("docx");
            document.setFileSize(content.length());
            document.setChecksum(calculateChecksum(content.toString()));
            
            // 提取文档属性
            var properties = docxDocument.getProperties();
            if (properties != null && properties.getCoreProperties() != null) {
                var coreProps = properties.getCoreProperties();
                document.addMetadata("creator", coreProps.getCreator());
                document.addMetadata("description", coreProps.getDescription());
                document.addMetadata("identifier", coreProps.getIdentifier());
                document.addMetadata("subject", coreProps.getSubject());
                document.addMetadata("title", coreProps.getTitle());
                
                if (coreProps.getCreated() != null) {
                    document.setCreatedTime(convertDateToLocalDateTime(coreProps.getCreated()));
                }
                if (coreProps.getModified() != null) {
                    document.setModifiedTime(convertDateToLocalDateTime(coreProps.getModified()));
                }
            }
            
            // 提取段落数
            document.addMetadata("paragraphCount", paragraphs.size());
        }
    }
    
    @Override
    public boolean supports(String fileType) {
        return SUPPORTED_TYPES.contains(fileType.toLowerCase());
    }
    
    @Override
    public String[] getSupportedTypes() {
        return SUPPORTED_TYPES.toArray(new String[0]);
    }
    
    @Override
    public String getName() {
        return "Word Document Parser";
    }
    
    @Override
    public String getDescription() {
        return "Parser for Microsoft Word documents (.docx) using Apache POI";
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    private String extractTitleFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "Untitled Document";
        }
        
        // 移除文件扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
    
    private String generateDocumentId(String fileName) {
        return "doc_" + System.currentTimeMillis() + "_" + fileName.hashCode();
    }
    
    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.warn("计算文档校验和失败", e);
            return "";
        }
    }
}