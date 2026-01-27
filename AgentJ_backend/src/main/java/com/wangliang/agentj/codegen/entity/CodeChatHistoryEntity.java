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
package com.wangliang.agentj.codegen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 代码生成对话历史实体
 */
@Entity
@Table(name = "code_chat_history", indexes = {
		@Index(name = "idx_app_id", columnList = "app_id"),
		@Index(name = "idx_app_id_created_at", columnList = "app_id,created_at"),
		@Index(name = "idx_user_id", columnList = "user_id")
})
public class CodeChatHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 消息内容
	 */
	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	/**
	 * 消息类型：user/ai
	 */
	@Column(name = "message_type", nullable = false, length = 32)
	private String messageType;

	/**
	 * 应用 ID
	 */
	@Column(name = "app_id", nullable = false)
	private Long appId;

	/**
	 * 用户 ID
	 */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/**
	 * 是否删除
	 */
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	/**
	 * 创建时间
	 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (isDeleted == null) {
			isDeleted = false;
		}
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public Long getAppId() {
		return appId;
	}

	public void setAppId(Long appId) {
		this.appId = appId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
