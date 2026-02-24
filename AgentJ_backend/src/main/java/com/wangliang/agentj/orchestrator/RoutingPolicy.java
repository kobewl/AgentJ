package com.wangliang.agentj.orchestrator;

import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.orchestrator.model.RoutingDecision;

import java.util.List;

public interface RoutingPolicy {
	RoutingDecision route(AgentTask task, AgentContext context, List<SubAgent> agents);
}
