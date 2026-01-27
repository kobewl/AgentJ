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
package com.wangliang.agentj.codegen.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * 应用视图对象
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppVO {

	private Long id;
	private String appName;
	private String cover;
	private String initPrompt;
	private String codeGenType;
	private String deployKey;
	private LocalDateTime deployedTime;
	private Long userId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	/**
	 * 预览 URL
	 */
	private String previewUrl;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}

	public String getInitPrompt() {
		return initPrompt;
	}

	public void setInitPrompt(String initPrompt) {
		this.initPrompt = initPrompt;
	}

	public String getCodeGenType() {
		return codeGenType;
	}

	public void setCodeGenType(String codeGenType) {
		this.codeGenType = codeGenType;
	}

	public String getDeployKey() {
		return deployKey;
	}

	public void setDeployKey(String deployKey) {
		this.deployKey = deployKey;
	}

	public LocalDateTime getDeployedTime() {
		return deployedTime;
	}

	public void setDeployedTime(LocalDateTime deployedTime) {
		this.deployedTime = deployedTime;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
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

	public String getPreviewUrl() {
		return previewUrl;
	}

	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}
}
