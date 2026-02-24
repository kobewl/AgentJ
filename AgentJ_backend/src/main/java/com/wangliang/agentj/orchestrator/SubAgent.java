package com.wangliang.agentj.orchestrator;

import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;

public interface SubAgent {
	String name();

	boolean canHandle(AgentTask task);

	AgentResult execute(AgentTask task, AgentContext context);

	default int priority() {
		return 0;
	}
}
