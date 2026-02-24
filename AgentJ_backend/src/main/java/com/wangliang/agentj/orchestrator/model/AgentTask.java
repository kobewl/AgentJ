package com.wangliang.agentj.orchestrator.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AgentTask {
	private final String id;
	private final String message;
	private final String intent;
	private final Map<String, Object> parameters;

	public AgentTask(String message, String intent, Map<String, Object> parameters) {
		this.id = UUID.randomUUID().toString();
		this.message = message;
		this.intent = intent;
		this.parameters = parameters == null ? new HashMap<>() : new HashMap<>(parameters);
	}

	public String getId() {
		return id;
	}

	public String getMessage() {
		return message;
	}

	public String getIntent() {
		return intent;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}
}
