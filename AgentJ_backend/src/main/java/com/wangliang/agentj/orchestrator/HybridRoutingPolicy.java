package com.wangliang.agentj.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.orchestrator.model.RoutingDecision;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
public class HybridRoutingPolicy implements RoutingPolicy {

	private final RuleBasedRoutingPolicy ruleBasedRoutingPolicy;
	private final LlmService llmService;
	private final ObjectMapper objectMapper;

	public HybridRoutingPolicy(RuleBasedRoutingPolicy ruleBasedRoutingPolicy, LlmService llmService,
			ObjectMapper objectMapper) {
		this.ruleBasedRoutingPolicy = ruleBasedRoutingPolicy;
		this.llmService = llmService;
		this.objectMapper = objectMapper;
	}

	@Override
	public RoutingDecision route(AgentTask task, AgentContext context, List<SubAgent> agents) {
		RoutingDecision decision = ruleBasedRoutingPolicy.route(task, context, agents);
		if (!decision.getSelectedAgents().isEmpty()) {
			return decision;
		}

		RoutingDecision llmDecision = llmRoute(task, agents);
		if (!llmDecision.getSelectedAgents().isEmpty()) {
			return llmDecision;
		}

		decision.setReason("no matching agent after llm routing");
		return decision;
	}

	private RoutingDecision llmRoute(AgentTask task, List<SubAgent> agents) {
		RoutingDecision decision = new RoutingDecision();
		if (agents == null || agents.isEmpty()) {
			decision.setReason("no agents available");
			return decision;
		}

		String choices = agents.stream()
				.map(SubAgent::name)
				.collect(Collectors.joining(", "));

		String payload = llmService.getDefaultDynamicAgentChatClient()
				.prompt()
				.system("You are a routing engine for a multi-agent system. Return JSON only.")
				.user("Pick up to 2 agents from the list to handle the task.\n"
						+ "Agents: " + choices + "\n"
						+ "Return JSON: {\"agents\":[\"name1\",\"name2\"],\"reason\":\"...\"}\n"
						+ "Task: " + task.getMessage())
				.call()
				.content();

		if (payload == null || payload.isBlank()) {
			decision.setReason("llm routing returned empty");
			return decision;
		}

		try {
			JsonNode root = objectMapper.readTree(payload);
			JsonNode agentsNode = root.path("agents");
			if (agentsNode.isArray()) {
				for (JsonNode agentNode : agentsNode) {
					String name = agentNode.asText(null);
					if (name != null && !name.isBlank()) {
						decision.getSelectedAgents().add(name);
					}
				}
			}
			String reason = root.path("reason").asText(null);
			decision.setReason(reason != null ? reason : "llm routing");
		}
		catch (Exception e) {
			decision.setReason("llm routing parse error");
		}
		return decision;
	}
}
