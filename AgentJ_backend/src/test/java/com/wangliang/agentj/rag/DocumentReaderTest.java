package com.wangliang.agentj.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentReaderTest {

    @Resource
    DocumentReader documentReader;

    @Test
    void loadAllMarkdownFromDirectory() {
        documentReader.loadAllMarkdownFromDirectory();
    }
}