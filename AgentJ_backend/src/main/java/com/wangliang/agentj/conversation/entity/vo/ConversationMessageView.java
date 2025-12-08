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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.conversation.entity.po.ConversationMessageEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 消息视图对象
 */
public class ConversationMessageView {

	private String id;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("user_id")
	private Long userId;

	private String role;

	private String content;

	@JsonProperty("model_name")
	private String modelName;

	@JsonProperty("tokens_used")
	private Integer tokensUsed;

	@JsonProperty("input_tokens")
	private Integer inputTokens;

	@JsonProperty("completion_tokens")
	private Integer completionTokens;

	private List<String> images;

	@JsonProperty("is_deleted")
	private Boolean isDeleted;

	@JsonProperty("created_at")
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;

	public static ConversationMessageView fromEntity(ConversationMessageEntity entity, ObjectMapper objectMapper) {
		ConversationMessageView view = new ConversationMessageView();
		view.setId(entity.getId());
		view.setConversationId(entity.getConversationId());
		view.setUserId(entity.getUserId());
		view.setRole(entity.getRole());
		view.setContent(entity.getContent());
		view.setModelName(entity.getModelName());
		view.setTokensUsed(entity.getTokensUsed());
		view.setInputTokens(entity.getInputTokens());
		view.setCompletionTokens(entity.getCompletionTokens());
		view.setImages(parseImages(entity.getImagesJson(), objectMapper));
		view.setIsDeleted(entity.getIsDeleted());
		view.setCreatedAt(entity.getCreatedAt());
		view.setUpdatedAt(entity.getUpdatedAt());
		return view;
	}

	private static List<String> parseImages(String json, ObjectMapper objectMapper) {
		if (json == null || json.trim().isEmpty()) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {
			});
		} catch (Exception e) {
			return Collections.emptyList();
		}
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

	public List<String> getImages() {
		return images;
	}

	public void setImages(List<String> images) {
		this.images = images;
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
