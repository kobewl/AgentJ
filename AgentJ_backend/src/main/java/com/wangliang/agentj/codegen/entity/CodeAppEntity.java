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
 * 代码生成应用实体
 */
@Entity
@Table(name = "code_app", indexes = {
		@Index(name = "idx_user_id", columnList = "user_id"),
		@Index(name = "idx_deploy_key", columnList = "deploy_key"),
		@Index(name = "idx_created_at", columnList = "created_at")
})
public class CodeAppEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 应用名称
	 */
	@Column(name = "app_name", nullable = false, length = 256)
	private String appName;

	/**
	 * 应用封面
	 */
	@Column(name = "cover", length = 512)
	private String cover;

	/**
	 * 初始化 prompt
	 */
	@Column(name = "init_prompt", columnDefinition = "TEXT")
	private String initPrompt;

	/**
	 * 生成类型：HTML
	 */
	@Column(name = "code_gen_type", nullable = false, length = 64)
	private String codeGenType = "HTML";

	/**
	 * 部署标识
	 */
	@Column(name = "deploy_key", unique = true, length = 64)
	private String deployKey;

	/**
	 * 部署时间
	 */
	@Column(name = "deployed_time")
	private LocalDateTime deployedTime;

	/**
	 * 创建用户
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

	/**
	 * 更新时间
	 */
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
		if (codeGenType == null) {
			codeGenType = "HTML";
		}
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// Getters and Setters
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

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
