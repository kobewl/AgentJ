package com.wangliang.agentj.rag;

import com.wangliang.agentj.service.rag.DocumentReader;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocumentReaderTest {

    @Resource
    DocumentReader documentReader;

    @Test
    void loadAllMarkdownFromDirectory() {
        documentReader.loadAllMarkdownFromDirectory();
    }
}