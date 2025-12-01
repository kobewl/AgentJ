package com.wangliang.agentj.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载器，读取文档
 */
@Component
@Slf4j
public class DocumentReader {

    private final ResourceLoader resourceLoader;

    public DocumentReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 加载指定目录下的所有markdown文档
     */
    public List<Document> loadAllMarkdownFromDirectory() {
        List<Document> allDocuments = new ArrayList<>();
        String directoryPath = "classpath:document";

        try {
            // 获取目录资源
            Resource directoryResource = resourceLoader.getResource(directoryPath);
            if (directoryResource.exists()) {
                // 获取目录下的所有文件
                File directory = directoryResource.getFile();
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".md")) {
                            // 为每个markdown文件创建Resource
                            Resource fileResource = resourceLoader.getResource("classpath:document/" + file.getName());

                            // 创建配置，添加文件名作为元数据
                            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                                    .withHorizontalRuleCreateDocument(true)
                                    .withIncludeCodeBlock(false)
                                    .withIncludeBlockquote(false)
                                    .withAdditionalMetadata("filename", file.getName())
                                    .build();

                            // 读取文档
                            MarkdownDocumentReader reader = new MarkdownDocumentReader(fileResource, config);
                            allDocuments.addAll(reader.get());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取目录失败: " + directoryPath + ", 错误: " + e.getMessage());
        }

        return allDocuments;
    }

}
