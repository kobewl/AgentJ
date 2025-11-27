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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.advisor.MyLoggerAdvisor;
import com.wangliang.agentj.advisor.ReReadingAdvisor;
import com.wangliang.agentj.tools.searchAPI.WebSearch;
import com.wangliang.agentj.tools.time.DateTimeTools;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ConversationService {

	private ChatClient chatClient;

	// Cached concurrent map for ChatClient instances with modelName as key
	private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();

	private final ObjectMapper objectMapper;

	private ChatMemory conversationMemory;

	private static final String SYSTEM_PROMPT = """
			你是一个全能的博士，专门帮助用户解决各种的疑难杂症。
			""";

	public ConversationService(ChatModel dashScopeModel, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;

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

	/**
	 * 带记忆的聊天
	 */
	public String doChat(String message, String chatId){
		ChatResponse chatResponse = chatClient.prompt()
				.user(message)
				.advisors(a -> a.param(chatId, 10))
				.call()
				.chatResponse();
		String content = chatResponse.getResult().getOutput().getText();
		return content;
	}

	/**
	 * 基于知识库的 RAG 问答
	 */
	@Autowired
	VectorStore vectorStore;

	Advisor retrievalAugmentationAdvisor;

	@PostConstruct
	public void init() {
		if (vectorStore != null) {
			retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
					.documentRetriever(VectorStoreDocumentRetriever.builder()
							.similarityThreshold(0.50)
							.vectorStore(vectorStore)
							.build())
					.build();
		}
	}

	public String doChatWithRag(String message, String chatId){
		if (retrievalAugmentationAdvisor == null) {
			return "RAG功能尚未正确配置，请检查VectorStore配置";
		}
		
		String answer = chatClient.prompt()
				.advisors(retrievalAugmentationAdvisor)
				.user(message)
				.call()
				.content();
		return answer;
	}

	/**
	 * 带工具调用的聊天 - 使用FunctionToolCallback正确注册工具
	 */
	public String doChatWithTools(String message, String chatId){
		// 创建WebSearch工具回调
		ToolCallback webSearchToolCallback = FunctionToolCallback
				.builder("web_search", new WebSearch(objectMapper))
				.description("Perform a web search using SerpApi and return relevant search results. Use this tool when you need to find information on the web, get up-to-date data, or research specific topics. The tool returns search results including snippets, links, and other relevant information.")
				.inputType(WebSearch.WebSearchInput.class)
				.build();

		ChatResponse chatResponse = chatClient.prompt()
				.user(message)
				.advisors(a -> a.param(chatId, 10))
				.tools(new DateTimeTools())
				.toolCallbacks(webSearchToolCallback)
				.call()
				.chatResponse();
		String content = chatResponse.getResult().getOutput().getText();
		return content;
	}


}
