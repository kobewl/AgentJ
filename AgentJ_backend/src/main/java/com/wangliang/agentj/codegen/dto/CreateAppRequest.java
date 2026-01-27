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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建应用请求
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateAppRequest {

	/**
	 * 应用名称
	 */
	@NotBlank(message = "应用名称不能为空")
	@Size(min = 1, max = 50, message = "应用名称长度在1-50个字符")
	private String appName;

	/**
	 * 初始化 prompt
	 */
	@NotBlank(message = "初始需求不能为空")
	@Size(max = 1000, message = "初始需求最长1000字符")
	private String initPrompt;

	/**
	 * 生成类型
	 */
	private String codeGenType = "HTML";

	/**
	 * 应用封面
	 */
	@Size(max = 512, message = "封面链接最长512字符")
	private String cover;

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
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

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}
}
