package com.wangliang.agentj.orchestrator.subagent;

import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.orchestrator.SubAgent;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import org.springframework.stereotype.Component;

@Component
public class LlmChatSubAgent implements SubAgent {

	private final LlmService llmService;

	public LlmChatSubAgent(LlmService llmService) {
		this.llmService = llmService;
	}

	@Override
	public String name() {
		return "chat";
	}

	@Override
	public boolean canHandle(AgentTask task) {
		return true;
	}

	@Override
	public AgentResult execute(AgentTask task, AgentContext context) {
		String response = llmService.getDefaultDynamicAgentChatClient()
				.prompt()
				.user(task.getMessage())
				.call()
				.content();
		return AgentResult.success(name(), response);
	}

	@Override
	public int priority() {
		return 1;
	}
}
