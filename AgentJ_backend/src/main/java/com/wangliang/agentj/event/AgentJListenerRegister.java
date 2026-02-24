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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

/**
 * @author wangliang
 * @time 2025/7/15
 * @desc agentj event listener registration
 */
@Component
public class AgentJListenerRegister implements BeanPostProcessor {

	@Autowired
	@Lazy
	private AgentJEventPublisher agentjEventPublisher;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AgentJListener) {
			ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(AgentJListener.class);
			ResolvableType eventType = resolvableType.getGeneric(0);
			Class<?> eventClass = eventType.resolve();
			Class<? extends AgentJEvent> agentjEventClass;
			try {
				agentjEventClass = (Class<? extends AgentJEvent>) eventClass;
			}
			catch (Exception e) {
				throw new IllegalArgumentException("The listener can only listen to AgentJEvent type");
			}
			agentjEventPublisher.registerListener(agentjEventClass, (AgentJListener) bean);
		}
		return bean;
	}

}
