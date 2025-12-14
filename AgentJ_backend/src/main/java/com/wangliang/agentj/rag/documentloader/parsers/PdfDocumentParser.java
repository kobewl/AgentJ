package com.wangliang.agentj.rag.documentloader.parsers;

import com.wangliang.agentj.rag.common.models.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * PDF文档解析器
 * 支持解析PDF文件内容并提取元数据
 */
public class PdfDocumentParser implements DocumentParser {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfDocumentParser.class);
    
    private static final List<String> SUPPORTED_TYPES = Arrays.asList("pdf");
    
    @Override
    public Document parse(InputStream inputStream, String fileName) throws IOException {
        logger.info("开始解析PDF文件: {}", fileName);
        
        try (PDDocument pdfDocument = PDDocument.load(inputStream)) {
            Document document = new Document();
            
            // 提取文本内容
            PDFTextStripper textStripper = new PDFTextStripper();
            String content = textStripper.getText(pdfDocument);
            document.setContent(content);
            
            // 设置基本信息
            document.setId(generateDocumentId(fileName));
            document.setTitle(extractTitleFromFileName(fileName));
            document.setSource(fileName);
            document.setFileType("pdf");
            document.setFileSize(content.length());
            document.setChecksum(calculateChecksum(content));
            
            // 提取PDF元数据
            if (pdfDocument.getDocumentInformation() != null) {
                var pdfInfo = pdfDocument.getDocumentInformation();
                document.addMetadata("author", pdfInfo.getAuthor());
                document.addMetadata("creator", pdfInfo.getCreator());
                document.addMetadata("producer", pdfInfo.getProducer());
                document.addMetadata("subject", pdfInfo.getSubject());
                
                if (pdfInfo.getCreationDate() != null) {
                    document.setCreatedTime(LocalDateTime.ofInstant(
                        pdfInfo.getCreationDate().toInstant(), 
                        pdfInfo.getCreationDate().getTimeZone().toZoneId()
                    ));
                }
                
                if (pdfInfo.getModificationDate() != null) {
                    document.setModifiedTime(LocalDateTime.ofInstant(
                        pdfInfo.getModificationDate().toInstant(), 
                        pdfInfo.getModificationDate().getTimeZone().toZoneId()
                    ));
                }
            }
            
            // 提取页数信息
            document.addMetadata("pageCount", pdfDocument.getNumberOfPages());
            
            logger.info("PDF文件解析完成: {}, 页数: {}, 字符数: {}", 
                       fileName, pdfDocument.getNumberOfPages(), content.length());
            
            return document;
            
        } catch (Exception e) {
            logger.error("解析PDF文件失败: {}", fileName, e);
            throw new IOException("Failed to parse PDF file: " + fileName, e);
        }
    }
    
    @Override
    public Document parse(String content, String fileName) throws IOException {
        // PDF内容通常不能直接作为字符串解析，需要通过输入流
        throw new UnsupportedOperationException("PDF parser requires InputStream");
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
        return "PDF Document Parser";
    }
    
    @Override
    public String getDescription() {
        return "Parser for PDF documents using Apache PDFBox";
    }
    
    private String generateDocumentId(String fileName) {
        return "doc_" + System.currentTimeMillis() + "_" + fileName.hashCode();
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