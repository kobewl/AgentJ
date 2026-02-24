package com.wangliang.agentj.orchestrator.model;

import java.util.ArrayList;
import java.util.List;

public class RoutingDecision {
	private final List<String> selectedAgents = new ArrayList<>();
	private String reason;

	public List<String> getSelectedAgents() {
		return selectedAgents;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
