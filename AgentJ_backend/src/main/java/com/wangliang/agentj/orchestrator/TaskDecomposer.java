package com.wangliang.agentj.orchestrator;

import com.wangliang.agentj.orchestrator.model.AgentRequest;
import com.wangliang.agentj.orchestrator.model.AgentTask;

import java.util.List;

public interface TaskDecomposer {
	List<AgentTask> decompose(AgentRequest request);
}
