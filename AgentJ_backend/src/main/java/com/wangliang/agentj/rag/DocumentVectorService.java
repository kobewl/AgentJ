package com.wangliang.agentj.rag;

import com.wangliang.agentj.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.logging.Logger;

/**
 * 文档向量服务，负责将文档转换为向量并存储到向量数据库
 */
@Service
@Slf4j
public class DocumentVectorService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DocumentVectorService.class);

    private final DocumentReader documentReader;
    private final VectorStore vectorStore;
    private static final int MAX_BATCH_SIZE = 10;

    public DocumentVectorService(DocumentReader documentReader, VectorStore vectorStore) {
        this.documentReader = documentReader;
        this.vectorStore = vectorStore;
    }

    /**
     * 应用启动时将文档写入 Qdrant。
     */
    @PostConstruct
    public void loadDocumentsToVectorStore() {
        List<Document> documents = documentReader.loadAllMarkdownFromDirectory();
        if (documents.isEmpty()) {
            logger.warn("未发现可加载的 Markdown 文档，跳过向量写入");
            return;
        }
        logger.info("开始向 Qdrant 写入 {} 条文档", documents.size());
        for (int start = 0; start < documents.size(); start += MAX_BATCH_SIZE) {
            int end = Math.min(start + MAX_BATCH_SIZE, documents.size());
            List<Document> batch = List.copyOf(documents.subList(start, end));
            vectorStore.add(batch);
            logger.info("已写入文档批次: {} - {}", start + 1, end);
        }
        logger.info("文档向量写入完成");
    }
}
