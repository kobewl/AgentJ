/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wangliang.agentj.planning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanDraftingService {

	private static final Logger log = LoggerFactory.getLogger(PlanDraftingService.class);

	private final LlmService llmService;

	private final ObjectMapper objectMapper;

	public PlanDraftingService(LlmService llmService, ObjectMapper objectMapper) {
		this.llmService = llmService;
		this.objectMapper = objectMapper;
	}

	public List<String> draftPlanSteps(String task, String context, String goal, int maxSteps) {
		if (!StringUtils.hasText(task)) {
			return List.of();
		}
		int desiredMaxSteps = Math.max(3, Math.min(maxSteps, 6));

		String systemPrompt = """
				You are a planning assistant.
				Return ONLY a JSON array of concise step strings (3-6 steps).
				No markdown, no numbering, no extra text.
				Keep the step language consistent with the task language.
				Steps must be actionable and tool-usable.
				""";

		StringBuilder userPrompt = new StringBuilder();
		userPrompt.append("Task: ").append(task).append("\n");
		if (StringUtils.hasText(context)) {
			userPrompt.append("Context: ").append(context).append("\n");
		}
		if (StringUtils.hasText(goal)) {
			userPrompt.append("Goal: ").append(goal).append("\n");
		}
		userPrompt.append("Max steps: ").append(desiredMaxSteps);

		try {
			ChatClient chatClient = llmService.getDiaChatClient();
			Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt.toString())));
			ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
			String output = response != null && response.getResult() != null && response.getResult().getOutput() != null
					? response.getResult().getOutput().getText()
					: null;
			List<String> parsed = parseSteps(output, desiredMaxSteps);
			if (!parsed.isEmpty()) {
				return parsed;
			}
		}
		catch (Exception e) {
			log.warn("Failed to draft plan steps, falling back to default: {}", e.getMessage());
		}

		return buildFallbackPlan(desiredMaxSteps);
	}

	private List<String> parseSteps(String raw, int maxSteps) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String trimmed = raw.trim();
		List<String> parsed = new ArrayList<>();

		int jsonStart = trimmed.indexOf('[');
		int jsonEnd = trimmed.lastIndexOf(']');
		if (jsonStart >= 0 && jsonEnd > jsonStart) {
			String json = trimmed.substring(jsonStart, jsonEnd + 1);
			try {
				parsed = objectMapper.readValue(json, objectMapper.getTypeFactory()
					.constructCollectionType(List.class, String.class));
			}
			catch (Exception e) {
				parsed = new ArrayList<>();
			}
		}

		if (parsed.isEmpty()) {
			String[] lines = trimmed.split("\\r?\\n");
			for (String line : lines) {
				String cleaned = line.replaceAll("^\\s*\\d+[\\.、]\\s*", "").trim();
				if (StringUtils.hasText(cleaned)) {
					parsed.add(cleaned);
				}
			}
		}

		List<String> normalized = new ArrayList<>();
		for (String step : parsed) {
			if (!StringUtils.hasText(step)) {
				continue;
			}
			String cleaned = step.trim();
			if (!normalized.contains(cleaned)) {
				normalized.add(cleaned);
			}
		}

		if (normalized.size() > maxSteps) {
			return normalized.subList(0, maxSteps);
		}
		return normalized;
	}

	private List<String> buildFallbackPlan(int maxSteps) {
		List<String> steps = new ArrayList<>();
		steps.add("Understand the task and identify required inputs.");
		steps.add("Collect or compute the necessary information.");
		steps.add("Validate findings and resolve gaps.");
		steps.add("Produce the final output in the requested format.");
		if (steps.size() > maxSteps) {
			return steps.subList(0, maxSteps);
		}
		return steps;
	}
}
