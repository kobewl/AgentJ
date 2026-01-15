package com.wangliang.agentj.agent;

public enum AgentState {

	NOT_STARTED("not_started"), IN_PROGRESS("in_progress"), COMPLETED("completed"), BLOCKED("blocked"),
	FAILED("failed"), INTERRUPTED("interrupted");

	private final String state;

	AgentState(String state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return state;
	}

}
