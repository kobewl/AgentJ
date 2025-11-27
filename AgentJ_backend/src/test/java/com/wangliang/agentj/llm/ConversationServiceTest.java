package com.wangliang.agentj.llm;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class ConversationServiceTest {

    @Resource
    ConversationService conversationService;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        String firstContent = conversationService.doChat("你好，我是王梁", chatId);
        System.out.println("第一次输出：" + firstContent);
        String secondContent = conversationService.doChat("我是谁？", chatId);
        System.out.println("第二次输出：" + secondContent);
    }

    @Test
    void doChatWithRag() {

        String chatId = UUID.randomUUID().toString();
        String firstContent = conversationService.doChatWithRag("什么是Java", chatId);
        System.out.println("第一次输出：" + firstContent);
    }

    @Test
    void doChatWithTools() {
        String chatId = UUID.randomUUID().toString();
        String dataTimeToolContent = conversationService.doChatWithTools("帮我搜索一下 今天 nba 的勇士有什么比赛，以及比赛的具体信息？", chatId);
        System.out.println("搜索工具输出：" + dataTimeToolContent);
    }
}