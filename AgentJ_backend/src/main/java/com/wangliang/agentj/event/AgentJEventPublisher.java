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
package com.wangliang.agentj.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentJEventPublisher {

	private static final Logger logger = LoggerFactory.getLogger(AgentJEventPublisher.class);

	// Listeners cannot be dynamically registered, no need for thread safety
	private Map<Class<? extends AgentJEvent>, List<AgentJListener<? super AgentJEvent>>> listeners = new HashMap<>();

	public void publish(AgentJEvent event) {
		Class<? extends AgentJEvent> eventClass = event.getClass();
		for (Map.Entry<Class<? extends AgentJEvent>, List<AgentJListener<? super AgentJEvent>>> entry : listeners
			.entrySet()) {
			// Parent classes can also be notified here
			if (entry.getKey().isAssignableFrom(eventClass)) {
				for (AgentJListener<? super AgentJEvent> listener : entry.getValue()) {
					try {
						listener.onEvent(event);
					}
					catch (Exception e) {
						logger.error("Error occurred while processing event: {}", e.getMessage(), e);
					}
				}
			}
		}
	}

	void registerListener(Class<? extends AgentJEvent> eventClass, AgentJListener<? super AgentJEvent> listener) {
		List<AgentJListener<? super AgentJEvent>> agentjListeners = listeners.get(eventClass);
		if (agentjListeners == null) {
			List<AgentJListener<? super AgentJEvent>> list = new ArrayList<>();
			list.add(listener);
			listeners.put(eventClass, list);
		}
		else {
			agentjListeners.add(listener);
		}
	}

}
