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
package com.wangliang.agentj.codegen.exception;

/**
 * 代码生成模块错误码
 */
public enum CodeGenErrorCode {

	SUCCESS(0, "成功"),
	PARAMS_ERROR(40000, "参数错误"),
	NOT_FOUND_ERROR(40004, "资源不存在"),
	NO_AUTH_ERROR(40101, "无权限访问"),
	NOT_LOGIN_ERROR(40100, "未登录"),
	OPERATION_ERROR(40200, "操作失败"),
	AI_GENERATION_ERROR(50001, "AI 生成失败"),
	CODE_SAVE_ERROR(50002, "代码保存失败"),
	DEPLOY_ERROR(50003, "部署失败"),
	RATE_LIMIT_ERROR(42901, "请求过于频繁，请稍后再试");

	private final int code;
	private final String message;

	CodeGenErrorCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
