package com.wangliang.agentj.rag.documentloader.parsers;

import com.wangliang.agentj.rag.common.models.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Markdown文档解析器
 * 支持解析Markdown文件内容并提取标题和元数据
 */
public class MarkdownDocumentParser implements DocumentParser {
    
    private static final Logger logger = LoggerFactory.getLogger(MarkdownDocumentParser.class);
    
    private static final List<String> SUPPORTED_TYPES = Arrays.asList("md", "markdown", "mdown", "mkd", "mkdn");
    
    // Markdown标题正则表达式
    private static final Pattern TITLE_PATTERN = Pattern.compile("^#+\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern YAML_FRONT_MATTER_PATTERN = Pattern.compile("^---\\n(.*?)\\n---\\n", Pattern.DOTALL);
    
    @Override
    public Document parse(InputStream inputStream, String fileName) throws IOException {
        logger.info("开始解析Markdown文件: {}", fileName);
        
        // 读取输入流内容
        String content;
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            content = scanner.useDelimiter("\\A").next();
        }
        
        return parseContent(content, fileName);
    }
    
    @Override
    public Document parse(String content, String fileName) throws IOException {
        logger.info("开始解析 Markdown 内容: {}", fileName);
        return parseContent(content, fileName);
    }
    
    private Document parseContent(String content, String fileName) {
        Document document = new Document();
        
        // 设置基本信息
        document.setId(generateDocumentId(fileName));
        document.setContent(content);
        document.setSource(fileName);
        document.setFileType("markdown");
        document.setFileSize(content.length());
        document.setChecksum(calculateChecksum(content));
        
        // 提取标题
        String title = extractTitle(content);
        document.setTitle(title);
        
        // 提取YAML前置元数据（如果存在）
        extractYamlFrontMatter(content, document);
        
        // 提取关键词（从标题和内容中提取）
        List<String> keywords = extractKeywords(content);
        document.setKeywords(keywords);
        
        // 检测语言
        String language = detectLanguage(content);
        document.setLanguage(language);
        
        logger.info("Markdown文件解析完成: {}, 标题: {}, 字符数: {}", 
                   fileName, title, content.length());
        
        return document;
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
        return "Markdown Document Parser";
    }
    
    @Override
    public String getDescription() {
        return "Parser for Markdown documents with YAML front matter support";
    }
    
    private String extractTitle(String content) {
        // 首先尝试从YAML前置元数据中获取标题
        var yamlMatcher = YAML_FRONT_MATTER_PATTERN.matcher(content);
        if (yamlMatcher.find()) {
            String yamlContent = yamlMatcher.group(1);
            // 简单的YAML标题提取
            var lines = yamlContent.split("\\n");
            for (String line : lines) {
                if (line.trim().startsWith("title:")) {
                    return line.substring(line.indexOf(":") + 1).trim();
                }
            }
        }
        
        // 然后从Markdown标题中提取
        var matcher = TITLE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 最后返回文件名作为标题
        return extractTitleFromFileName(content);
    }
    
    private void extractYamlFrontMatter(String content, Document document) {
        var matcher = YAML_FRONT_MATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            String yamlContent = matcher.group(1);
            var lines = yamlContent.split("\\n");
            
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        document.addMetadata(key, value);
                        
                        // 特殊处理一些常用字段
                        if ("date".equals(key) || "created".equals(key)) {
                            try {
                                document.setCreatedTime(LocalDateTime.parse(value));
                            } catch (Exception e) {
                                logger.debug("解析日期失败: {}", value);
                            }
                        }
                        if ("modified".equals(key) || "updated".equals(key)) {
                            try {
                                document.setModifiedTime(LocalDateTime.parse(value));
                            } catch (Exception e) {
                                logger.debug("解析修改日期失败: {}", value);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private List<String> extractKeywords(String content) {
        // 简单的关键词提取：从标题和粗体文本中提取
        List<String> keywords = new ArrayList<>();
        
        // 提取Markdown粗体和斜体文本
        Pattern boldPattern = Pattern.compile("\\*\\*([^*]+)\\*\\*");
        Pattern italicPattern = Pattern.compile("\\*([^*]+)\\*");
        
        var boldMatcher = boldPattern.matcher(content);
        while (boldMatcher.find()) {
            keywords.add(boldMatcher.group(1).trim());
        }
        
        var italicMatcher = italicPattern.matcher(content);
        while (italicMatcher.find()) {
            keywords.add(italicMatcher.group(1).trim());
        }
        
        return keywords;
    }
    
    private String detectLanguage(String content) {
        // 简单的语言检测：基于中文字符比例
        int chineseChars = 0;
        int totalChars = Math.min(content.length(), 1000); // 只检查前1000个字符
        
        for (int i = 0; i < totalChars; i++) {
            char c = content.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseChars++;
            }
        }
        
        double chineseRatio = (double) chineseChars / totalChars;
        return chineseRatio > 0.1 ? "zh" : "en";
    }
    
    private String extractTitleFromFileName(String content) {
        // 简单的标题提取：取第一行非空内容
        var lines = content.split("\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                return line.trim().substring(0, Math.min(line.length(), 100));
            }
        }
        return "Untitled Document";
    }
    
    private String generateDocumentId(String fileName) {
        return "doc_" + System.currentTimeMillis() + "_" + fileName.hashCode();
    }
    
    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
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