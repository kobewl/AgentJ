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

/**
 * 代码生成类型枚举
 */
public enum CodeGenTypeEnum {

	/**
	 * HTML 单文件生成
	 */
	HTML("HTML", "HTML 单文件"),

	/**
	 * 多文件分离生成（预留，后续扩展）
	 */
	MULTI_FILE("MULTI_FILE", "多文件分离"),

	/**
	 * Vue 项目生成（预留，后续扩展）
	 */
	VUE_PROJECT("VUE_PROJECT", "Vue 项目");

	private final String code;
	private final String description;

	CodeGenTypeEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public static CodeGenTypeEnum fromCode(String code) {
		for (CodeGenTypeEnum type : values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}
		return HTML;
	}
}
