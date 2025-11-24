package com.wangliang.agentj;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringAITest {

    @Resource
    private ChatModel dashScopeModel;

    @Test
    public void test() {
        AssistantMessage assistantMessage = dashScopeModel.call(new Prompt("你好，我是wangliang"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage);
    }
}
