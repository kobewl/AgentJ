package com.wangliang.agentj.orchestrator;

import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentRequest;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.orchestrator.model.RoutingDecision;
import com.wangliang.agentj.user.context.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class AgentOrchestratorService {

	private static final Logger log = LoggerFactory.getLogger(AgentOrchestratorService.class);

	private final RoutingPolicy routingPolicy;
	private final TaskDecomposer taskDecomposer;
	private final List<SubAgent> subAgents;
	private final LlmService llmService;

	public AgentOrchestratorService(RoutingPolicy routingPolicy, TaskDecomposer taskDecomposer,
			List<SubAgent> subAgents, LlmService llmService) {
		this.routingPolicy = routingPolicy;
		this.taskDecomposer = taskDecomposer;
		this.subAgents = subAgents;
		this.llmService = llmService;
	}

	public AgentResult execute(AgentRequest request) {
		String message = request.getMessage();
		if (message == null || message.isBlank()) {
			return AgentResult.failure("orchestrator", "message is required");
		}

		Long userId = UserContextHolder.getUserId();
		AgentContext context = new AgentContext(request.getConversationId(), userId);

		List<AgentTask> tasks = taskDecomposer.decompose(request);
		if (tasks.isEmpty()) {
			tasks = decompose(request);
		}
		AgentResult result = AgentResult.success("orchestrator", "ok");
		result.getMetadata().put("taskCount", tasks.size());

		for (AgentTask task : tasks) {
			RoutingDecision decision = routingPolicy.route(task, context, subAgents);
			if (decision.getSelectedAgents().isEmpty()) {
				result.addChild(AgentResult.failure("orchestrator", "no agent selected: " + decision.getReason()));
				continue;
			}

			boolean parallel = Boolean.TRUE.equals(request.getParallel());
			List<AgentResult> taskResults = parallel
					? executeParallel(task, context, decision.getSelectedAgents(), decision.getReason())
					: executeSequential(task, context, decision.getSelectedAgents(), decision.getReason());
			for (AgentResult child : taskResults) {
				result.addChild(child);
			}
		}

		mergeResults(result);
		return result;
	}

	private List<AgentTask> decompose(AgentRequest request) {
		Map<String, Object> params = new HashMap<>();
		if (request.getParameters() != null) {
			params.putAll(request.getParameters());
		}
		if (request.getAppId() != null) {
			params.put("appId", request.getAppId());
		}
		if (request.getAppName() != null) {
			params.put("appName", request.getAppName());
		}
		List<AgentTask> tasks = new ArrayList<>();
		tasks.add(new AgentTask(request.getMessage(), request.getIntent(), params));
		return tasks;
	}

	private List<AgentResult> executeSequential(AgentTask task, AgentContext context,
			List<String> agentNames, String reason) {
		List<AgentResult> results = new ArrayList<>();
		for (String agentName : agentNames) {
			results.add(executeSingle(task, context, agentName, reason));
		}
		return results;
	}

	private List<AgentResult> executeParallel(AgentTask task, AgentContext context,
			List<String> agentNames, String reason) {
		List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
		for (String agentName : agentNames) {
			futures.add(CompletableFuture.supplyAsync(() -> executeSingle(task, context, agentName, reason)));
		}

		List<AgentResult> results = new ArrayList<>();
		for (CompletableFuture<AgentResult> future : futures) {
			try {
				results.add(future.get());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				results.add(AgentResult.failure("orchestrator", "parallel execution interrupted"));
			}
			catch (ExecutionException e) {
				results.add(AgentResult.failure("orchestrator", "parallel execution failed: " + e.getMessage()));
			}
		}
		return results;
	}

	private AgentResult executeSingle(AgentTask task, AgentContext context, String agentName, String reason) {
		SubAgent agent = findAgent(agentName);
		if (agent == null) {
			return AgentResult.failure("orchestrator", "agent not found: " + agentName);
		}
		try {
			AgentResult child = agent.execute(task, context);
			child.getMetadata().put("routingReason", reason);
			return child;
		}
		catch (Exception e) {
			log.error("Sub agent execution failed: {}", agentName, e);
			return AgentResult.failure(agentName, e.getMessage());
		}
	}

	private void mergeResults(AgentResult result) {
		List<String> contents = result.getChildren().stream()
				.map(AgentResult::getContent)
				.filter(Objects::nonNull)
				.filter(content -> !content.isBlank())
				.toList();
		if (contents.isEmpty()) {
			return;
		}

		Map<String, Integer> voteCounts = new HashMap<>();
		Set<String> distinct = contents.stream()
				.map(content -> content.replaceAll("\\s+", " ").trim())
				.peek(content -> voteCounts.merge(content, 1, Integer::sum))
				.collect(Collectors.toSet());
		boolean conflict = distinct.size() > 1;
		result.getMetadata().put("conflict", conflict);
		result.getMetadata().put("distinctResponses", distinct.size());
		result.getMetadata().put("voteCounts", voteCounts);

		if (contents.size() == 1) {
			result.getMetadata().put("mergedBy", "single");
			result.setContent(contents.get(0));
			result.getMetadata().put("confidence", 0.9);
			return;
		}

		String topVote = voteCounts.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(contents.get(0));
		int topCount = voteCounts.getOrDefault(topVote, 1);
		double agreementRatio = (double) topCount / (double) contents.size();
		result.getMetadata().put("agreementRatio", agreementRatio);

		String merged = llmService.getDefaultDynamicAgentChatClient()
				.prompt()
				.system("You are a senior agent orchestrator. Merge multiple sub-agent outputs into a concise, well-structured response. Highlight disagreements if any.")
				.user(String.join("\n\n---\n\n", contents))
				.call()
				.content();
		if (merged != null && !merged.isBlank()) {
			result.getMetadata().put("mergedBy", "llm");
			result.getMetadata().put("mergedLength", merged.length());
			result.getMetadata().put("mergedAt", System.currentTimeMillis());
			result.getMetadata().put("mergedFrom", result.getChildren().size());
			result.getMetadata().put("mergedConfidence", conflict ? "medium" : "high");
			result.getMetadata().put("mergedSummary", merged);
			result.getChildren().forEach(child -> child.getMetadata().put("parentMerged", true));
			result.setContent(merged);
			double confidence = conflict ? Math.max(0.5, agreementRatio) : Math.min(0.95, 0.7 + agreementRatio * 0.3);
			result.getMetadata().put("confidence", confidence);
			return;
		}

		result.setContent(topVote);
		double confidence = conflict ? Math.max(0.45, agreementRatio) : 0.8;
		result.getMetadata().put("confidence", confidence);
	}

	private SubAgent findAgent(String name) {
		if (name == null) {
			return null;
		}
		for (SubAgent agent : subAgents) {
			if (name.equalsIgnoreCase(agent.name())) {
				return agent;
			}
		}
		return null;
	}
}
