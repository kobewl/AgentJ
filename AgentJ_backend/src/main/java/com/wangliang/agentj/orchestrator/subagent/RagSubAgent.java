package com.wangliang.agentj.orchestrator.subagent;

import com.wangliang.agentj.orchestrator.SubAgent;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.rag.HybridRagService;
import org.springframework.stereotype.Component;

@Component
public class RagSubAgent implements SubAgent {

	private final HybridRagService ragService;

	public RagSubAgent(HybridRagService ragService) {
		this.ragService = ragService;
	}

	@Override
	public String name() {
		return "rag";
	}

	@Override
	public boolean canHandle(AgentTask task) {
		String message = task.getMessage() == null ? "" : task.getMessage().toLowerCase();
		String intent = task.getIntent() == null ? "" : task.getIntent().toLowerCase();
		return intent.contains("rag") || intent.contains("knowledge")
				|| message.contains("知识库") || message.contains("检索") || message.contains("向量");
	}

	@Override
	public AgentResult execute(AgentTask task, AgentContext context) {
		String kbId = getString(task, "kbId");
		String answer = kbId == null
				? ragService.answer(task.getMessage())
				: ragService.answerWithKnowledge(kbId, task.getMessage());
		AgentResult result = AgentResult.success(name(), answer);
		if (kbId != null) {
			result.getMetadata().put("kbId", kbId);
		}
		return result;
	}

	@Override
	public int priority() {
		return 8;
	}

	private String getString(AgentTask task, String key) {
		Object value = task.getParameters().get(key);
		return value == null ? null : value.toString();
	}
}
