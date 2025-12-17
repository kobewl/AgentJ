package com.wangliang.agentj.knowledge.service;

import com.wangliang.agentj.knowledge.entity.KnowledgeItemEntity;
import com.wangliang.agentj.knowledge.entity.KnowledgeItemType;
import com.wangliang.agentj.knowledge.model.KnowledgeItemView;
import com.wangliang.agentj.knowledge.repository.KnowledgeItemRepository;
import com.wangliang.agentj.rag.HybridRagService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

	private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

	private final KnowledgeItemRepository repository;

	private final VectorStore vectorStore;

	private final HybridRagService hybridRagService;

	public KnowledgeService(KnowledgeItemRepository repository, VectorStore vectorStore, HybridRagService hybridRagService) {
		this.repository = repository;
		this.vectorStore = vectorStore;
		this.hybridRagService = hybridRagService;
	}

	public List<KnowledgeItemView> listKnowledgeBases(Long userId) {
		return repository.findByUserIdAndTypeAndIsDeletedFalse(userId, KnowledgeItemType.KNOWLEDGE_BASE)
			.stream()
			.map(KnowledgeItemView::fromEntity)
			.collect(Collectors.toList());
	}

	public List<KnowledgeItemView> listFiles(String knowledgeBaseId, Long userId) {
		return repository.findByKnowledgeBaseIdAndTypeAndIsDeletedFalse(knowledgeBaseId, KnowledgeItemType.KNOWLEDGE_FILE)
			.stream()
			.filter(item -> Objects.equals(userId, item.getUserId()))
			.map(KnowledgeItemView::fromEntity)
			.collect(Collectors.toList());
	}

	public KnowledgeItemView createKnowledgeBase(String name, Long userId) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("知识库名称不能为空");
		}
		KnowledgeItemEntity entity = new KnowledgeItemEntity();
		entity.setId(UUID.randomUUID().toString());
		entity.setName(name.trim());
		entity.setType(KnowledgeItemType.KNOWLEDGE_BASE);
		entity.setUserId(userId);
		entity.setCreatedAt(LocalDateTime.now());
		entity.setUpdatedAt(LocalDateTime.now());
		entity.setIsDeleted(false);
		repository.save(entity);
		return KnowledgeItemView.fromEntity(entity);
	}

	@Transactional
	public KnowledgeItemView uploadFile(String knowledgeBaseId, MultipartFile file, Long userId) throws IOException {
		KnowledgeItemEntity base = repository.findById(knowledgeBaseId)
			.filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
			.orElseThrow(() -> new IllegalArgumentException("知识库不存在或已删除"));
		if (!Objects.equals(base.getUserId(), userId)) {
			throw new IllegalArgumentException("无权操作该知识库");
		}
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("文件不能为空");
		}
		String docId = UUID.randomUUID().toString();
		String originalFilename = file.getOriginalFilename();
		String fileNameForDisk = docId; // 按要求以文档ID命名
		Path savePath = Path.of("uploads", "knowledge", knowledgeBaseId, fileNameForDisk);
		Files.createDirectories(savePath.getParent());
		Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

		KnowledgeItemEntity entity = new KnowledgeItemEntity();
		entity.setId(docId);
		entity.setName(StringUtils.hasText(originalFilename) ? originalFilename : docId);
		entity.setOriginalFilename(originalFilename);
		entity.setMimeType(file.getContentType());
		entity.setFileSize(file.getSize());
		entity.setStoragePath(savePath.toString());
		entity.setKnowledgeBaseId(knowledgeBaseId);
		entity.setType(KnowledgeItemType.KNOWLEDGE_FILE);
		entity.setUserId(userId);
		entity.setCreatedAt(LocalDateTime.now());
		entity.setUpdatedAt(LocalDateTime.now());
		entity.setIsDeleted(false);
		repository.save(entity);

		// 将文件内容切分后写入向量库，附带知识库/文档元数据
		try {
			String content = Files.readString(savePath, StandardCharsets.UTF_8);
			TokenTextSplitter splitter = new TokenTextSplitter();
			List<Document> docs = splitter.apply(List.of(new Document(content,
					Map.of("kbId", knowledgeBaseId, "docId", docId, "filename", entity.getName(), "path",
							entity.getStoragePath()))));
			vectorStore.add(docs);
			log.info("已将文档 {} 写入向量库，分片 {} 条", docId, docs.size());
		}
		catch (Exception e) {
			log.warn("将文档写入向量库失败，不影响上传主流程: {}", e.getMessage());
		}

		return KnowledgeItemView.fromEntity(entity);
	}

	@Transactional
	public void softDelete(String itemId, Long userId) {
		KnowledgeItemEntity entity = repository.findByIdAndUserId(itemId, userId)
			.orElseThrow(() -> new IllegalArgumentException("知识条目不存在或无权限"));
		entity.setIsDeleted(true);
		repository.save(entity);
		if (entity.getType() == KnowledgeItemType.KNOWLEDGE_BASE) {
			repository.softDeleteFilesByBase(entity.getId(), userId);
		}
	}

	public String chat(String knowledgeBaseId, String question) {
		return hybridRagService.answerWithKnowledge(knowledgeBaseId, question);
	}

}
