package com.wangliang.agentj.orchestrator.subagent;

import com.wangliang.agentj.orchestrator.SubAgent;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.workflow.dto.WorkflowExecuteRequest;
import com.wangliang.agentj.workflow.service.WorkflowExecutionServiceV2;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WorkflowSubAgent implements SubAgent {

	private final WorkflowExecutionServiceV2 workflowExecutionService;

	public WorkflowSubAgent(WorkflowExecutionServiceV2 workflowExecutionService) {
		this.workflowExecutionService = workflowExecutionService;
	}

	@Override
	public String name() {
		return "workflow";
	}

	@Override
	public boolean canHandle(AgentTask task) {
		String intent = task.getIntent() == null ? "" : task.getIntent().toLowerCase();
		String message = task.getMessage() == null ? "" : task.getMessage().toLowerCase();
		return intent.contains("workflow") || message.contains("工作流") || message.contains("流程");
	}

	@Override
	public AgentResult execute(AgentTask task, AgentContext context) {
		Long workflowId = getLong(task, "workflowId");
		if (workflowId == null) {
			return AgentResult.failure(name(), "workflowId is required");
		}

		WorkflowExecuteRequest request = new WorkflowExecuteRequest();
		request.setInputs(task.getParameters());
		try {
			Map<String, Object> result = workflowExecutionService.execute(workflowId, request);
			AgentResult agentResult = AgentResult.success(name(), result == null ? "" : result.toString());
			agentResult.getMetadata().put("workflowId", workflowId);
			agentResult.getMetadata().put("outputs", result);
			return agentResult;
		}
		catch (Exception e) {
			return AgentResult.failure(name(), e.getMessage());
		}
	}

	@Override
	public int priority() {
		return 6;
	}

	private Long getLong(AgentTask task, String key) {
		Object value = task.getParameters().get(key);
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		if (value instanceof String) {
			try {
				return Long.parseLong((String) value);
			}
			catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
