package com.wangliang.agentj.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 知识库与知识文件共用的条目表。
 */
@Entity
@Table(name = "knowledge_items", indexes = { @Index(name = "idx_kb_user", columnList = "knowledge_base_id,user_id"),
		@Index(name = "idx_user_type", columnList = "user_id,type") })
public class KnowledgeItemEntity {

	@Id
	@Column(length = 64, nullable = false)
	private String id;

	@Column(nullable = false, length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private KnowledgeItemType type;

	/**
	 * 对于文件存储文件相对路径；库本身可为空。
	 */
	@Column(name = "storage_path", length = 1024)
	private String storagePath;

	/**
	 * 归属的知识库 ID（文件需要，库本身可为空）。
	 */
	@Column(name = "knowledge_base_id", length = 64)
	private String knowledgeBaseId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "original_filename", length = 255)
	private String originalFilename;

	@Column(name = "mime_type", length = 128)
	private String mimeType;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (isDeleted == null) {
			isDeleted = false;
		}
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = LocalDateTime.now();
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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean deleted) {
		isDeleted = deleted;
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
