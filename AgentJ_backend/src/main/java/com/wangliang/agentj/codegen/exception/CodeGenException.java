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
 * 代码生成业务异常
 */
public class CodeGenException extends RuntimeException {

	private final int code;

	public CodeGenException(CodeGenErrorCode errorCode) {
		super(errorCode.getMessage());
		this.code = errorCode.getCode();
	}

	public CodeGenException(CodeGenErrorCode errorCode, String message) {
		super(message);
		this.code = errorCode.getCode();
	}

	public CodeGenException(int code, String message) {
		super(message);
		this.code = code;
	}

	public CodeGenException(CodeGenErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.code = errorCode.getCode();
	}

	public int getCode() {
		return code;
	}
}
