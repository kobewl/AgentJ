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
package com.wangliang.agentj.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.user.model.po.UserPersonalMemoryEntity;
import com.wangliang.agentj.user.model.vo.UserPersonalMemory;
import com.wangliang.agentj.user.repository.UserPersonalMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserPersonalMemoryService {

	private static final Logger log = LoggerFactory.getLogger(UserPersonalMemoryService.class);

	private final UserPersonalMemoryRepository repository;

	private final ObjectMapper objectMapper;

	private final LlmService llmService;

	@Autowired
	public UserPersonalMemoryService(UserPersonalMemoryRepository repository, ObjectMapper objectMapper,
			LlmService llmService) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.llmService = llmService;
	}

	public List<UserPersonalMemory> listByUser(Long userId) {
		return repository.findByUserId(userId).stream().map(this::toVo).collect(Collectors.toList());
	}

	public Optional<UserPersonalMemory> getByUserAndKey(Long userId, String memoryKey) {
		return repository.findByUserIdAndMemoryKey(userId, memoryKey).map(this::toVo);
	}

	public UserPersonalMemory saveOrUpdate(UserPersonalMemory memory) {
		if (memory.getUserId() == null || !StringUtils.hasText(memory.getMemoryKey())) {
			throw new IllegalArgumentException("userId and memoryKey are required");
		}
		var entityOpt = repository.findByUserIdAndMemoryKey(memory.getUserId(), memory.getMemoryKey());
		UserPersonalMemoryEntity entity = entityOpt.orElseGet(UserPersonalMemoryEntity::new);
		BeanUtils.copyProperties(memory, entity, "id", "createdAt", "updatedAt");
		if (entity.getId() == null) {
			entity.setCreatedAt(LocalDateTime.now());
		}
		entity.setUpdatedAt(LocalDateTime.now());
		UserPersonalMemoryEntity saved = repository.save(entity);
		return toVo(saved);
	}

	public void delete(Long userId, String memoryKey) {
		repository.deleteByUserIdAndMemoryKey(userId, memoryKey);
	}

	public void markUsed(Long userId, String memoryKey) {
		repository.findByUserIdAndMemoryKey(userId, memoryKey).ifPresent(entity -> {
			entity.setLastUsedAt(LocalDateTime.now());
			repository.save(entity);
		});
	}

	/**
	 * Let LLM decide whether to persist a piece of information as long-term memory.
	 * Runs best-effort and swallows errors to avoid blocking chat.
	 */
	public void autoCaptureFromDialog(Long userId, String userText, String assistantText) {
		if (userId == null) {
			return;
		}
		if (!StringUtils.hasText(userText) && !StringUtils.hasText(assistantText)) {
			return;
		}
		try {
			String sysPrompt = """
					你是一个“记忆提炼器”，从对话中抽取值得长期记忆的用户画像或偏好。
					输出 JSON，格式：
					{
					  "action": "save" | "ignore",
					  "memory_key": "nickname/job/custom_instruction/interest/other",
					  "title": "用于列表的简短标题",
					  "content": "适合长期保存的内容，中文为主",
					  "importance": 1-10,
					  "confidence": 0.0-1.0,
					  "tags": ["tag1","tag2"]
					}
					仅当内容对未来多轮对话有价值时 action=save。
					""";

			// Simple user prompt with latest round context
			ObjectNode ctx = objectMapper.createObjectNode();
			ctx.put("user_said", userText == null ? "" : userText);
			ctx.put("assistant_replied", assistantText == null ? "" : assistantText);

			ChatClient chatClient = llmService.getDiaChatClient();
			String raw = chatClient.prompt()
				.messages(new SystemMessage(sysPrompt), new UserMessage(ctx.toPrettyString()))
				.call()
				.content();
			if (!StringUtils.hasText(raw)) {
				return;
			}
			JsonNode node = objectMapper.readTree(raw);
			String action = node.path("action").asText("");
			if (!"save".equalsIgnoreCase(action)) {
				log.debug("AI decided not to save personal memory: {}", raw);
				return;
			}

			String memoryKey = node.path("memory_key").asText("other");
			UserPersonalMemory memory = new UserPersonalMemory();
			memory.setUserId(userId);
			memory.setMemoryKey(memoryKey);
			memory.setTitle(node.path("title").asText(memoryKey));
			// Store original JSON content for flexibility
			ObjectNode contentJson = objectMapper.createObjectNode();
			contentJson.put("content", node.path("content").asText(""));
			contentJson.put("raw", raw);
			memory.setContentJson(objectMapper.writeValueAsString(contentJson));
			memory.setSource("AI");
			if (node.hasNonNull("confidence")) {
				memory.setConfidence(BigDecimal.valueOf(node.get("confidence").asDouble()));
			}
			if (node.hasNonNull("importance")) {
				memory.setImportance(node.get("importance").asInt());
			}
			if (node.has("tags")) {
				memory.setTags(objectMapper.writeValueAsString(node.get("tags")));
			}
			saveOrUpdate(memory);
			log.info("Auto-saved personal memory for user {} with key {}", userId, memoryKey);
		}
		catch (Exception e) {
			log.warn("Auto capture personal memory failed for user {}", userId, e);
		}
	}

	private UserPersonalMemory toVo(UserPersonalMemoryEntity entity) {
		UserPersonalMemory vo = new UserPersonalMemory();
		BeanUtils.copyProperties(entity, vo);
		return vo;
	}

}
