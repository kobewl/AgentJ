package com.wangliang.agentj.controller;

import com.wangliang.agentj.conversation.entity.dto.ConversationMessageRequest;
import com.wangliang.agentj.conversation.entity.dto.ConversationSessionRequest;
import com.wangliang.agentj.conversation.entity.dto.ConversationTitleRequest;
import com.wangliang.agentj.conversation.entity.vo.ConversationMessageView;
import com.wangliang.agentj.conversation.entity.vo.ConversationSessionView;
import com.wangliang.agentj.conversation.entity.vo.PagedResult;
import com.wangliang.agentj.conversation.service.ConversationRecordService;
import com.wangliang.agentj.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 对话控制器
 * Conversation Controller
 */
@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

	private final ConversationRecordService conversationRecordService;

	private final UserService userService;

	/**
	 * 列出当前用户的会话（默认过滤已删除）
	 */
	@GetMapping
	public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "false") boolean includeDeleted) {
		Long userId = userService.currentUserId();
		PagedResult<ConversationSessionView> result = conversationRecordService.listSessions(userId, keyword,
				includeDeleted, page, size);
		return ResponseEntity.ok(Map.of("success", true, "data", result));
	}

	/**
	 * 获取单个会话
	 */
	@GetMapping("/{id}")
	public ResponseEntity<?> detail(@PathVariable("id") String id) {
		Long userId = userService.currentUserId();
		try {
			ConversationSessionView view = conversationRecordService.getSession(userId, id);
			return ResponseEntity.ok(Map.of("success", true, "data", view));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		}
	}

	/**
	 * 创建会话
	 */
	@PostMapping
	public ResponseEntity<?> create(@RequestBody ConversationSessionRequest request) {
		Long userId = userService.currentUserId();
		try {
			ConversationSessionView view = conversationRecordService.createSession(userId, request);
			return ResponseEntity.ok(Map.of("success", true, "data", view));
		} catch (Exception e) {
			log.error("创建会话失败", e);
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	/**
	 * 更新会话
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable("id") String id, @RequestBody ConversationSessionRequest request) {
		Long userId = userService.currentUserId();
		try {
			ConversationSessionView view = conversationRecordService.updateSession(userId, id, request);
			return ResponseEntity.ok(Map.of("success", true, "data", view));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		} catch (Exception e) {
			log.error("更新会话失败", e);
			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "message", "更新失败：" + e.getMessage()));
		}
	}

	/**
	 * 生成会话标题（最多6个字）
	 */
	@PostMapping("/title/generate")
	public ResponseEntity<?> generateTitle(@RequestBody ConversationTitleRequest request) {
		Long userId = userService.currentUserId();
		String title = conversationRecordService.generateTitle(userId, request);
		return ResponseEntity.ok(Map.of("success", true, "data", Map.of("title", title)));
	}

	/**
	 * 软删除会话
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<?> softDelete(@PathVariable("id") String id) {
		Long userId = userService.currentUserId();
		try {
			conversationRecordService.softDeleteSession(userId, id);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		}
	}

	/**
	 * 恢复会话
	 */
	@PostMapping("/{id}/restore")
	public ResponseEntity<?> restore(@PathVariable("id") String id) {
		Long userId = userService.currentUserId();
		try {
			conversationRecordService.restoreSession(userId, id);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		}
	}

	/**
	 * 会话下的消息列表（默认过滤已删除，按时间升序）
	 */
	@GetMapping("/{id}/messages")
	public ResponseEntity<?> listMessages(@PathVariable("id") String conversationId,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int size,
			@RequestParam(defaultValue = "false") boolean includeDeleted) {
		Long userId = userService.currentUserId();
		try {
			PagedResult<ConversationMessageView> result = conversationRecordService.listMessages(userId, conversationId,
					includeDeleted, page, size);
			return ResponseEntity.ok(Map.of("success", true, "data", result));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		}
	}

	/**
	 * 创建消息
	 */
	@PostMapping("/{id}/messages")
	public ResponseEntity<?> createMessage(@PathVariable("id") String conversationId,
			@RequestBody ConversationMessageRequest request) {
		Long userId = userService.currentUserId();
		try {
			request.setConversationId(conversationId);
			ConversationMessageView view = conversationRecordService.createMessage(userId, request);
			return ResponseEntity.ok(Map.of("success", true, "data", view));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		} catch (Exception e) {
			log.error("创建消息失败", e);
			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "message", "创建消息失败：" + e.getMessage()));
		}
	}

	/**
	 * 软删除消息
	 */
	@DeleteMapping("/{conversationId}/messages/{messageId}")
	public ResponseEntity<?> deleteMessage(@PathVariable String conversationId, @PathVariable String messageId) {
		Long userId = userService.currentUserId();
		try {
			conversationRecordService.softDeleteMessage(userId, conversationId, messageId);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		}
	}

	/**
	 * 恢复消息
	 */
	@PostMapping("/{conversationId}/messages/{messageId}/restore")
	public ResponseEntity<?> restoreMessage(@PathVariable String conversationId, @PathVariable String messageId) {
		Long userId = userService.currentUserId();
		try {
			conversationRecordService.restoreMessage(userId, conversationId, messageId);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		}
	}

	/**
	 * 消息全文搜索
	 */
	@GetMapping("/messages/search")
	public ResponseEntity<?> searchMessages(@RequestParam String keyword,
			@RequestParam(required = false) String conversationId, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "false") boolean includeDeleted) {
		Long userId = userService.currentUserId();
		try {
			PagedResult<ConversationMessageView> result = conversationRecordService.searchMessages(userId, keyword,
					conversationId, includeDeleted, page, size);
			return ResponseEntity.ok(Map.of("success", true, "data", result));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
		}
	}

}