package com.wangliang.agentj.knowledge.model;

import com.wangliang.agentj.knowledge.entity.KnowledgeItemEntity;
import com.wangliang.agentj.knowledge.entity.KnowledgeItemType;

import java.time.LocalDateTime;

/**
 * 前端展示用的知识条目视图。
 */
public class KnowledgeItemView {

	private String id;

	private String name;

	private KnowledgeItemType type;

	private String storagePath;

	private String knowledgeBaseId;

	private String originalFilename;

	private Long fileSize;

	private String mimeType;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	public static KnowledgeItemView fromEntity(KnowledgeItemEntity entity) {
		KnowledgeItemView view = new KnowledgeItemView();
		view.setId(entity.getId());
		view.setName(entity.getName());
		view.setType(entity.getType());
		view.setStoragePath(entity.getStoragePath());
		view.setKnowledgeBaseId(entity.getKnowledgeBaseId());
		view.setOriginalFilename(entity.getOriginalFilename());
		view.setFileSize(entity.getFileSize());
		view.setMimeType(entity.getMimeType());
		view.setCreatedAt(entity.getCreatedAt());
		view.setUpdatedAt(entity.getUpdatedAt());
		return view;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public KnowledgeItemType getType() {
		return type;
	}

	public void setType(KnowledgeItemType type) {
		this.type = type;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	public String getKnowledgeBaseId() {
		return knowledgeBaseId;
	}

	public void setKnowledgeBaseId(String knowledgeBaseId) {
		this.knowledgeBaseId = knowledgeBaseId;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
