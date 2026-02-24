package com.wangliang.agentj.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.orchestrator.model.AgentRequest;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class LlmTaskDecomposer implements TaskDecomposer {

	private final LlmService llmService;
	private final ObjectMapper objectMapper;

	public LlmTaskDecomposer(LlmService llmService, ObjectMapper objectMapper) {
		this.llmService = llmService;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<AgentTask> decompose(AgentRequest request) {
		if (!shouldDecompose(request)) {
			return List.of();
		}

		String payload = llmService.getDefaultDynamicAgentChatClient()
				.prompt()
				.system("You are a task decomposition engine for a multi-agent system. Return JSON only.")
				.user(buildPrompt(request))
				.call()
				.content();

		if (payload == null || payload.isBlank()) {
			return List.of();
		}

		try {
			JsonNode root = objectMapper.readTree(payload);
			JsonNode tasksNode = root.path("tasks");
			if (!tasksNode.isArray()) {
				return List.of();
			}

			List<AgentTask> tasks = new ArrayList<>();
			for (JsonNode taskNode : tasksNode) {
				String message = taskNode.path("message").asText(null);
				String intent = taskNode.path("intent").asText(null);
				Map<String, Object> params = new HashMap<>();
				JsonNode paramsNode = taskNode.path("parameters");
				if (paramsNode.isObject()) {
					Iterator<Map.Entry<String, JsonNode>> fields = paramsNode.fields();
					while (fields.hasNext()) {
						Map.Entry<String, JsonNode> field = fields.next();
						params.put(field.getKey(), objectMapper.convertValue(field.getValue(), Object.class));
					}
				}

				if (message != null && !message.isBlank()) {
					tasks.add(new AgentTask(message, intent, params));
				}
			}
			return tasks;
		}
		catch (Exception e) {
			return List.of();
		}
	}

	private boolean shouldDecompose(AgentRequest request) {
		if (request == null || request.getMessage() == null) {
			return false;
		}
		if (request.getParameters() != null && Boolean.TRUE.equals(request.getParameters().get("decompose"))) {
			return true;
		}
		String message = request.getMessage();
		if (message.length() > 120) {
			return true;
		}
		return message.contains("并且") || message.contains("同时") || message.contains("然后")
				|| message.contains("以及") || message.contains("\n");
	}

	private String buildPrompt(AgentRequest request) {
		return "Decompose the user request into 1-3 tasks with explicit intents and parameters.\n"
				+ "Return JSON only in this schema:\n"
				+ "{ \"tasks\": [ { \"message\": \"...\", \"intent\": \"codegen|chat|rag|db|browser|workflow\", \"parameters\": { } } ] }\n\n"
				+ "User request:\n"
				+ request.getMessage();
	}
}
