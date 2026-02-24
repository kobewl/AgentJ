package com.wangliang.agentj.orchestrator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentResult {
	private String status;
	private String agent;
	private String content;
	private String reason;
	private final Map<String, Object> metadata = new HashMap<>();
	private final List<AgentResult> children = new ArrayList<>();

	public static AgentResult success(String agent, String content) {
		AgentResult result = new AgentResult();
		result.status = "success";
		result.agent = agent;
		result.content = content;
		return result;
	}

	public static AgentResult failure(String agent, String reason) {
		AgentResult result = new AgentResult();
		result.status = "failed";
		result.agent = agent;
		result.reason = reason;
		return result;
	}

	public String getStatus() {
		return status;
	}

	public String getAgent() {
		return agent;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getReason() {
		return reason;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public List<AgentResult> getChildren() {
		return children;
	}

	public void addChild(AgentResult child) {
		if (child != null) {
			children.add(child);
		}
	}
}
