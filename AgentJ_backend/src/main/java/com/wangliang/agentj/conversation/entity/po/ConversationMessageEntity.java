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
package com.wangliang.agentj.conversation.entity.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 对应 conversation_messages 表的实体
 */
@Entity
@Table(name = "conversation_messages")
public class ConversationMessageEntity {

	@Id
	@Column(length = 64, nullable = false)
	private String id;

	@Column(name = "conversation_id", length = 64, nullable = false)
	private String conversationId;

	@Column(name = "user_id")
	private Long userId;

	@Column(length = 50, nullable = false)
	private String role;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String content;

	@Column(name = "model_name", length = 100)
	private String modelName;

	@Column(name = "tokens_used")
	private Integer tokensUsed;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "completion_tokens")
	private Integer completionTokens;

	@Column(name = "images", columnDefinition = "JSON")
	private String imagesJson;

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
			isDeleted = Boolean.FALSE;
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

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public Integer getTokensUsed() {
		return tokensUsed;
	}

	public void setTokensUsed(Integer tokensUsed) {
		this.tokensUsed = tokensUsed;
	}

	public Integer getInputTokens() {
		return inputTokens;
	}

	public void setInputTokens(Integer inputTokens) {
		this.inputTokens = inputTokens;
	}

	public Integer getCompletionTokens() {
		return completionTokens;
	}

	public void setCompletionTokens(Integer completionTokens) {
		this.completionTokens = completionTokens;
	}

	public String getImagesJson() {
		return imagesJson;
	}

	public void setImagesJson(String imagesJson) {
		this.imagesJson = imagesJson;
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
