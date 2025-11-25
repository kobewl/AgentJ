/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wangliang.agentj.llm;


import com.wangliang.agentj.advisor.MyLoggerAdvisor;
import com.wangliang.agentj.advisor.ReReadingAdvisor;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LlmService {

	private ChatClient chatClient;

	// Cached concurrent map for ChatClient instances with modelName as key
	private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();

	private ChatMemory conversationMemory;

	private static final String SYSTEM_PROMPT = """
			你是一个全能的博士，专门帮助用户解决各种的疑难杂症。
			""";

	public LlmService(ChatModel dashScopeModel) {

		// 基于内存的对话记忆
		conversationMemory = MessageWindowChatMemory.builder().build();

		chatClient = ChatClient.builder(dashScopeModel)
				.defaultSystem(SYSTEM_PROMPT)
				.defaultAdvisors(
						MessageChatMemoryAdvisor.builder(conversationMemory).build(),
						// 自定义日志拦截器
						new MyLoggerAdvisor(),
						// Re2 拦截器
						new ReReadingAdvisor()
				)
				.build();
	}

	public String doChat(String message, String chatId){
		ChatResponse chatResponse = chatClient.prompt()
				.user(message)
				.advisors(a -> a.param(chatId, 10))
				.call()
				.chatResponse();
		String content = chatResponse.getResult().getOutput().getText();
		return content;
	}


}
