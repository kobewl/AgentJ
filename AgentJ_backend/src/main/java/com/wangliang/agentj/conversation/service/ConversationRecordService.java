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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
		String userContent = truncateSafe(request.getUserContent(), 280);
		String assistantContent = truncateSafe(request.getAssistantContent(), 280);
		if (!StringUtils.hasText(userContent) && !StringUtils.hasText(assistantContent)) {
			return "新对话";
		}

		String prompt = """
				根据最近一轮问答生成一个中文标题：
				- 4~6 个字，优先 5~6 字
				- 用名词短语概括核心主题，避免空泛词（如 对话、聊天、问答、助手、提问）
				- 不要标点和引号，不要解释
				- 保留关键技术/专有名词（如 RAG、Java）

				用户: %s
				助手: %s
				""".formatted(
				StringUtils.hasText(userContent) ? userContent : "",
				StringUtils.hasText(assistantContent) ? assistantContent : "");

		try {
			ChatClient chatClient = llmService.getDefaultDynamicAgentChatClient();
			String title = chatClient.prompt()
					.system("你是标题生成助手，只输出一个中文标题，突出主题名词，不要出现“对话/聊天/问答/助手”等空泛词。")
					.user(prompt)
					.options(OpenAiChatOptions.builder().temperature(0.25).build())
					.call()
					.content();

			title = postProcessTitle(title);
			if (isGenericTitle(title)) {
				title = fallbackTitle(userContent, assistantContent);
			}
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
		String candidate = postProcessTitle(base);
		return StringUtils.hasText(candidate) ? candidate : "新的对话";
	}

	private boolean isGenericTitle(String title) {
		if (!StringUtils.hasText(title)) {
			return true;
		}
		Set<String> bad = Set.of("对话", "聊天", "问答", "交流", "讨论", "新对话", "新的对话");
		String normalized = title.strip();
		return bad.contains(normalized) || normalized.length() < 2;
	}

	private String truncateSafe(String text, int maxLen) {
		if (!StringUtils.hasText(text)) {
			return "";
		}
		return text.length() <= maxLen ? text : text.substring(0, maxLen);
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
