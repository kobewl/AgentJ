package com.wangliang.agentj.llm;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LlmServiceTest {

    @Resource
    LlmService llmService;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        String firstContent = llmService.doChat("你好，我是王梁", chatId);
        System.out.println("第一次输出：" + firstContent);
        String secondContent = llmService.doChat("我是谁？", chatId);
        System.out.println("第二次输出：" + secondContent);
    }
}