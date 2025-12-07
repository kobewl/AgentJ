package com.wangliang.agentj.user.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通用的个人记忆接口返回体，兼容前端 MemoryResponse 结构（success/message/data）。
 */
public class UserPersonalMemoryResponse {

	private boolean success;

	private String message;

	private Object data;

	@JsonProperty("response_time")
	private LocalDateTime responseTime = LocalDateTime.now();

	public UserPersonalMemoryResponse() {
	}

	private UserPersonalMemoryResponse(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	private UserPersonalMemoryResponse(boolean success, String message, Object data) {
		this(success, message);
		this.data = data;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public LocalDateTime getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(LocalDateTime responseTime) {
		this.responseTime = responseTime;
	}

	// 工厂方法
	public static UserPersonalMemoryResponse success(UserPersonalMemory data) {
		return new UserPersonalMemoryResponse(true, "ok", data);
	}

	public static UserPersonalMemoryResponse success(List<UserPersonalMemory> list) {
		return new UserPersonalMemoryResponse(true, "ok", list);
	}

	public static UserPersonalMemoryResponse error(String message) {
		return new UserPersonalMemoryResponse(false, message);
	}

	public static UserPersonalMemoryResponse notFound() {
		return new UserPersonalMemoryResponse(false, "not found");
	}

	public static UserPersonalMemoryResponse deleted() {
		return new UserPersonalMemoryResponse(true, "deleted");
	}

	public static UserPersonalMemoryResponse updated() {
		return new UserPersonalMemoryResponse(true, "updated");
	}

}
