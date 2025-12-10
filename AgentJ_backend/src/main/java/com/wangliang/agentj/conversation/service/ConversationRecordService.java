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
package com.wangliang.agentj.conversation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.conversation.entity.dto.ConversationMessageRequest;
import com.wangliang.agentj.conversation.entity.dto.ConversationSessionRequest;
import com.wangliang.agentj.conversation.entity.po.ConversationMessageEntity;
import com.wangliang.agentj.conversation.entity.po.ConversationSessionEntity;
import com.wangliang.agentj.conversation.entity.vo.ConversationMessageView;
import com.wangliang.agentj.conversation.entity.vo.ConversationSessionView;
import com.wangliang.agentj.conversation.entity.vo.PagedResult;
import com.wangliang.agentj.conversation.repository.ConversationMessageRepository;
import com.wangliang.agentj.conversation.repository.ConversationSessionRepository;
import com.wangliang.agentj.conversation.entity.dto.ConversationTitleRequest;
import com.wangliang.agentj.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConversationRecordService {

	private static final Logger log = LoggerFactory.getLogger(ConversationRecordService.class);

	private final ConversationSessionRepository sessionRepository;

	private final ConversationMessageRepository messageRepository;

	private final ObjectMapper objectMapper;

	private final LlmService llmService;

	public ConversationRecordService(ConversationSessionRepository sessionRepository,
			ConversationMessageRepository messageRepository, ObjectMapper objectMapper, LlmService llmService) {
		this.sessionRepository = sessionRepository;
		this.messageRepository = messageRepository;
		this.objectMapper = objectMapper;
		this.llmService = llmService;
	}

	public String generateTitle(Long userId, ConversationTitleRequest request) {
		String userContent = request.getUserContent();
		String assistantContent = request.getAssistantContent();
		if (!StringUtils.hasText(userContent) && !StringUtils.hasText(assistantContent)) {
			return "新的对话";
		}

		String prompt = """
				请根据以下对话生成一个中文标题，最多6个字，要求简洁且能概括主题，不要标点符号，不要输出解释。

				用户: %s
				助手: %s
				""".formatted(
				StringUtils.hasText(userContent) ? userContent : "",
				StringUtils.hasText(assistantContent) ? assistantContent : "");

		try {
			ChatClient chatClient = llmService.getDefaultDynamicAgentChatClient();
			String title = chatClient.prompt()
					.system("你是标题生成助手，输出不超过6个汉字的简洁标题，不要解释。")
					.user(prompt)
					.call()
					.content();

			title = postProcessTitle(title);
			return StringUtils.hasText(title) ? title : "新的对话";
		} catch (Exception e) {
			log.warn("生成会话标题失败，使用兜底: {}", e.getMessage());
			return fallbackTitle(userContent, assistantContent);
		}
	}

	private String postProcessTitle(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String cleaned = raw.replaceAll("\\s+", "")
				.replaceAll("[`~!@#$%^&*()\\-_=+\\[\\]{};:'\",.<>/?\\\\|]", "");
		return cleaned.length() > 6 ? cleaned.substring(0, 6) : cleaned;
	}

	private String fallbackTitle(String userContent, String assistantContent) {
		String base = StringUtils.hasText(userContent) ? userContent : assistantContent;
		return postProcessTitle(base);
	}

	public ConversationSessionView createSession(Long userId, ConversationSessionRequest request) {
		ConversationSessionEntity entity = new ConversationSessionEntity();
		entity.setId(StringUtils.hasText(request.getId()) ? request.getId() : UUID.randomUUID().toString());
		entity.setUserId(userId);
		entity.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "新的对话");
		entity.setSummary(request.getSummary());
		entity.setModelName(StringUtils.hasText(request.getModelName()) ? request.getModelName()
				: ConversationSessionEntity.DEFAULT_MODEL_NAME);
		entity.setIsDeleted(Boolean.FALSE);
		LocalDateTime now = LocalDateTime.now();
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		ConversationSessionEntity saved = sessionRepository.save(entity);
		return ConversationSessionView.fromEntity(saved);
	}

	public ConversationSessionView updateSession(Long userId, String sessionId, ConversationSessionRequest request) {
		ConversationSessionEntity entity = sessionRepository.findByIdAndUserId(sessionId, userId)
				.orElseThrow(() -> new IllegalArgumentException("会话不存在或无权访问"));
		if (StringUtils.hasText(request.getTitle())) {
			entity.setTitle(request.getTitle());
		}
		if (StringUtils.hasText(request.getSummary())) {
			entity.setSummary(request.getSummary());
		}
		if (StringUtils.hasText(request.getModelName())) {
			entity.setModelName(request.getModelName());
		}
		entity.setUpdatedAt(LocalDateTime.now());
		return ConversationSessionView.fromEntity(sessionRepository.save(entity));
	}

	public void softDeleteSession(Long userId, String sessionId) {
		int affected = sessionRepository.softDelete(sessionId, userId);
		if (affected == 0) {
			throw new IllegalArgumentException("会话不存在或无权访问");
		}
		messageRepository.softDeleteByConversationId(sessionId);
	}

	public void restoreSession(Long userId, String sessionId) {
		int affected = sessionRepository.restore(sessionId, userId);
		if (affected == 0) {
			throw new IllegalArgumentException("会话不存在或无权访问");
		}
		messageRepository.restoreByConversationId(sessionId);
	}

	public PagedResult<ConversationSessionView> listSessions(Long userId, String keyword, boolean includeDeleted,
			int page, int size) {
		Page<ConversationSessionEntity> pageData = sessionRepository.search(userId,
				StringUtils.hasText(keyword) ? keyword : null, includeDeleted, PageRequest.of(Math.max(page - 1, 0),
						Math.max(size, 1)));
		List<ConversationSessionView> views = pageData.getContent().stream()
				.map(ConversationSessionView::fromEntity)
				.collect(Collectors.toList());
		return new PagedResult<>(views, pageData.getTotalElements(), pageData.getNumber() + 1, pageData.getSize());
	}

	public ConversationSessionView getSession(Long userId, String sessionId) {
		ConversationSessionEntity entity = sessionRepository.findByIdAndUserId(sessionId, userId)
				.orElseThrow(() -> new IllegalArgumentException("会话不存在或无权访问"));
		return ConversationSessionView.fromEntity(entity);
	}

	public ConversationSessionView ensureSessionExists(Long userId, String sessionId, String title, String modelName) {
		if (StringUtils.hasText(sessionId)) {
			return sessionRepository.findByIdAndUserId(sessionId, userId)
					.map(ConversationSessionView::fromEntity)
					.orElseGet(() -> {
						ConversationSessionRequest req = new ConversationSessionRequest();
						req.setId(sessionId);
						req.setTitle(title);
						req.setModelName(modelName);
						return createSession(userId, req);
					});
		}
		ConversationSessionRequest req = new ConversationSessionRequest();
		req.setTitle(title);
		req.setModelName(modelName);
		return createSession(userId, req);
	}

	public ConversationMessageView createMessage(Long userId, ConversationMessageRequest request) {
		if (!StringUtils.hasText(request.getConversationId())) {
			throw new IllegalArgumentException("conversationId 不能为空");
		}
		if (!StringUtils.hasText(request.getRole())) {
			throw new IllegalArgumentException("role 不能为空");
		}
		if (!StringUtils.hasText(request.getContent())) {
			throw new IllegalArgumentException("content 不能为空");
		}

		ConversationSessionEntity session = sessionRepository.findByIdAndUserId(request.getConversationId(), userId)
				.orElseThrow(() -> new IllegalArgumentException("会话不存在或无权访问"));

		ConversationMessageEntity entity = new ConversationMessageEntity();
		entity.setId(UUID.randomUUID().toString());
		entity.setConversationId(session.getId());
		// AI 消息 userId 允许为空
		if ("assistant".equalsIgnoreCase(request.getRole())) {
			entity.setUserId(null);
		} else {
			entity.setUserId(userId);
		}
		entity.setRole(request.getRole());
		entity.setContent(request.getContent());
		entity.setModelName(
				StringUtils.hasText(request.getModelName()) ? request.getModelName() : session.getModelName());
		entity.setTokensUsed(request.getTokensUsed() != null ? request.getTokensUsed() : 0);
		entity.setInputTokens(request.getInputTokens() != null ? request.getInputTokens() : 0);
		entity.setCompletionTokens(request.getCompletionTokens() != null ? request.getCompletionTokens() : 0);
		if (!CollectionUtils.isEmpty(request.getImages())) {
			try {
				entity.setImagesJson(objectMapper.writeValueAsString(request.getImages()));
			} catch (Exception e) {
				log.warn("无法序列化 images，已忽略", e);
			}
		}
		LocalDateTime now = LocalDateTime.now();
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		entity.setIsDeleted(Boolean.FALSE);

		ConversationMessageEntity saved = messageRepository.save(entity);
		session.setLastMessageAt(now);
		session.setUpdatedAt(now);
		sessionRepository.save(session);
		return ConversationMessageView.fromEntity(saved, objectMapper);
	}

	public PagedResult<ConversationMessageView> listMessages(Long userId, String conversationId, boolean includeDeleted,
			int page, int size) {
		sessionRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new IllegalArgumentException("会话不存在或无权访问"));

		PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
		Page<ConversationMessageEntity> pageData;
		if (includeDeleted) {
			pageData = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageRequest);
		} else {
			pageData = messageRepository.findByConversationIdAndIsDeletedOrderByCreatedAtAsc(conversationId, false,
					pageRequest);
		}
		List<ConversationMessageView> views = pageData.getContent().stream()
				.map(entity -> ConversationMessageView.fromEntity(entity, objectMapper))
				.collect(Collectors.toList());
		return new PagedResult<>(views, pageData.getTotalElements(), pageData.getNumber() + 1, pageData.getSize());
	}

	public void softDeleteMessage(Long userId, String conversationId, String messageId) {
		sessionRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new IllegalArgumentException("会话不存在或无权访问"));
		int affected = messageRepository.softDelete(messageId, conversationId);
		if (affected == 0) {
			throw new IllegalArgumentException("消息不存在或无权访问");
		}
	}

	public void restoreMessage(Long userId, String conversationId, String messageId) {
		sessionRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new IllegalArgumentException("会话不存在或无权访问"));
		int affected = messageRepository.restore(messageId, conversationId);
		if (affected == 0) {
			throw new IllegalArgumentException("消息不存在或无权访问");
		}
	}

	public PagedResult<ConversationMessageView> searchMessages(Long userId, String keyword, String conversationId,
			boolean includeDeleted, int page, int size) {
		if (!StringUtils.hasText(keyword)) {
			throw new IllegalArgumentException("搜索关键字不能为空");
		}
		Page<ConversationMessageEntity> pageData = messageRepository.searchByContent(userId,
				StringUtils.hasText(conversationId) ? conversationId : null, keyword, includeDeleted,
				PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1)));
		List<ConversationMessageView> views = pageData.getContent().stream()
				.map(entity -> ConversationMessageView.fromEntity(entity, objectMapper))
				.collect(Collectors.toList());
		return new PagedResult<>(views, pageData.getTotalElements(), pageData.getNumber() + 1, pageData.getSize());
	}
}
