package com.wangliang.agentj.recorder;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AgentJSpringEnvironmentHolder implements EnvironmentAware {

	private static Environment environment;

	public void setEnvironment(Environment environment) {
		AgentJSpringEnvironmentHolder.environment = environment;
	}

	public static Environment getEnvironment() {
		return environment;
	}

}
