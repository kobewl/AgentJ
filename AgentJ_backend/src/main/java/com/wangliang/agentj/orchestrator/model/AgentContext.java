package com.wangliang.agentj.orchestrator.model;

import java.util.HashMap;
import java.util.Map;

public class AgentContext {
	private final String conversationId;
	private final Long userId;
	private final Map<String, Object> state;

	public AgentContext(String conversationId, Long userId) {
		this.conversationId = conversationId;
		this.userId = userId;
		this.state = new HashMap<>();
	}

	public String getConversationId() {
		return conversationId;
	}

	public Long getUserId() {
		return userId;
	}

	public Map<String, Object> getState() {
		return state;
	}
}
