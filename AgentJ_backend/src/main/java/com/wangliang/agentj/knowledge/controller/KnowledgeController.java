package com.wangliang.agentj.knowledge.controller;

import com.wangliang.agentj.knowledge.model.CreateKnowledgeBaseRequest;
import com.wangliang.agentj.knowledge.model.KnowledgeChatRequest;
import com.wangliang.agentj.knowledge.model.KnowledgeItemView;
import com.wangliang.agentj.knowledge.service.KnowledgeService;
import com.wangliang.agentj.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = "*")
public class KnowledgeController {

	private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

	private final KnowledgeService knowledgeService;

	private final UserService userService;

	public KnowledgeController(KnowledgeService knowledgeService, UserService userService) {
		this.knowledgeService = knowledgeService;
		this.userService = userService;
	}

	@GetMapping("/bases")
	public ResponseEntity<?> listKnowledgeBases() {
		Long userId = userService.currentUserId();
		List<KnowledgeItemView> items = knowledgeService.listKnowledgeBases(userId);
		return ResponseEntity.ok(Map.of("success", true, "data", items));
	}

	@PostMapping("/bases")
	public ResponseEntity<?> createKnowledgeBase(@RequestBody CreateKnowledgeBaseRequest request) {
		Long userId = userService.currentUserId();
		try {
			KnowledgeItemView view = knowledgeService.createKnowledgeBase(request.getName(), userId);
			return ResponseEntity.ok(Map.of("success", true, "data", view));
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	@GetMapping("/bases/{id}/files")
	public ResponseEntity<?> listFiles(@PathVariable("id") String knowledgeBaseId) {
		Long userId = userService.currentUserId();
		List<KnowledgeItemView> files = knowledgeService.listFiles(knowledgeBaseId, userId);
		return ResponseEntity.ok(Map.of("success", true, "data", files));
	}

	@PostMapping("/bases/{id}/files")
	public ResponseEntity<?> uploadFile(@PathVariable("id") String knowledgeBaseId,
			@RequestParam("file") MultipartFile file) {
		Long userId = userService.currentUserId();
		try {
			KnowledgeItemView view = knowledgeService.uploadFile(knowledgeBaseId, file, userId);
			return ResponseEntity.ok(Map.of("success", true, "data", view));
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
		catch (IOException e) {
			log.error("上传文件失败", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("success", false, "message", "上传失败: " + e.getMessage()));
		}
	}

	@DeleteMapping("/items/{id}")
	public ResponseEntity<?> deleteItem(@PathVariable("id") String itemId) {
		Long userId = userService.currentUserId();
		try {
			knowledgeService.softDelete(itemId, userId);
			return ResponseEntity.ok(Map.of("success", true));
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	@PostMapping("/bases/{id}/chat")
	public ResponseEntity<?> chat(@PathVariable("id") String knowledgeBaseId,
			@RequestBody KnowledgeChatRequest request) {
		if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "问题不能为空"));
		}
		String answer = knowledgeService.chat(knowledgeBaseId, request.getQuestion());
		return ResponseEntity.ok(Map.of("success", true, "data", Map.of("answer", answer)));
	}

}
