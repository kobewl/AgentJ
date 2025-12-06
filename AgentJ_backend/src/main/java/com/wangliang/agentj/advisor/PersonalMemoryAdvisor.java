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
package com.wangliang.agentj.advisor;

import com.wangliang.agentj.user.context.UserContextHolder;
import com.wangliang.agentj.user.service.UserPersonalMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Advisor that inspects each dialog turn and lets AI decide whether to persist
 * user personal memory.
 */
@Component
public class PersonalMemoryAdvisor implements CallAdvisor, StreamAdvisor {

	private static final int MAX_CAPTURE_LENGTH = 2000;

	private static final Logger log = LoggerFactory.getLogger(PersonalMemoryAdvisor.class);

	private final UserPersonalMemoryService userPersonalMemoryService;

	public PersonalMemoryAdvisor(UserPersonalMemoryService userPersonalMemoryService) {
		this.userPersonalMemoryService = userPersonalMemoryService;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		// Place after logging so we capture final content but before other tail advisors.
		return 5;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		String userText = safeUserText(chatClientRequest);
		ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
		String assistantText = safeAssistantText(response);
		triggerAutoCapture(userText, assistantText);
		return response;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		String userText = safeUserText(chatClientRequest);
		Flux<ChatClientResponse> respFlux = streamAdvisorChain.nextStream(chatClientRequest);
		return new ChatClientMessageAggregator().aggregateChatClientResponse(respFlux, aggregated -> {
			String assistantText = safeAssistantText(aggregated);
			triggerAutoCapture(userText, assistantText);
		});
	}

	private void triggerAutoCapture(String userText, String assistantText) {
		Long userId = UserContextHolder.getUserId();
		if (userId == null) {
			log.debug("PersonalMemoryAdvisor skipped: userId is null");
			return;
		}
		log.debug("PersonalMemoryAdvisor capture start, userId={}, userTextLen={}, assistantTextLen={}", userId,
				userText != null ? userText.length() : 0, assistantText != null ? assistantText.length() : 0);
		try {
			userPersonalMemoryService.autoCaptureFromDialog(userId, userText, assistantText);
		}
		catch (Exception e) {
			log.warn("PersonalMemoryAdvisor auto capture failed for user {}", userId, e);
		}
	}

	private String safeUserText(ChatClientRequest request) {
		try {
			var userMessage = request.prompt().getUserMessage();
			if (userMessage != null && StringUtils.hasText(userMessage.getText())) {
				return truncate(userMessage.getText());
			}
		}
		catch (Exception ignored) {
			// No-op
		}
		return "";
	}

	private String safeAssistantText(ChatClientResponse response) {
		try {
			return truncate(response.chatResponse().getResult().getOutput().getText());
		}
		catch (Exception ignored) {
			return "";
		}
	}

	private String truncate(String text) {
		if (!StringUtils.hasText(text)) {
			return "";
		}
		if (text.length() <= MAX_CAPTURE_LENGTH) {
			return text;
		}
		// Keep tail part which通常包含最新信息
		return text.substring(text.length() - MAX_CAPTURE_LENGTH);
	}

}
