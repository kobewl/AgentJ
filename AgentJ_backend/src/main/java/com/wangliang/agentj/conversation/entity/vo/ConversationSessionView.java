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
package com.wangliang.agentj.conversation.entity.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wangliang.agentj.conversation.entity.po.ConversationSessionEntity;

import java.time.LocalDateTime;

/**
 * 会话视图对象
 */
public class ConversationSessionView {

	private String id;

	@JsonProperty("user_id")
	private Long userId;

	private String title;

	@JsonProperty("model_name")
	private String modelName;

	private String summary;

	@JsonProperty("last_message_at")
	private LocalDateTime lastMessageAt;

	@JsonProperty("is_deleted")
	private Boolean isDeleted;

	@JsonProperty("created_at")
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;

	public static ConversationSessionView fromEntity(ConversationSessionEntity entity) {
		ConversationSessionView view = new ConversationSessionView();
		view.setId(entity.getId());
		view.setUserId(entity.getUserId());
		view.setTitle(entity.getTitle());
		view.setModelName(entity.getModelName());
		view.setSummary(entity.getSummary());
		view.setLastMessageAt(entity.getLastMessageAt());
		view.setIsDeleted(entity.getIsDeleted());
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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public LocalDateTime getLastMessageAt() {
		return lastMessageAt;
	}

	public void setLastMessageAt(LocalDateTime lastMessageAt) {
		this.lastMessageAt = lastMessageAt;
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
