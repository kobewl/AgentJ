package com.wangliang.agentj.orchestrator.subagent;

import com.wangliang.agentj.codegen.dto.AppVO;
import com.wangliang.agentj.codegen.dto.CodeGenRequest;
import com.wangliang.agentj.codegen.service.CodeGenService;
import com.wangliang.agentj.orchestrator.SubAgent;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.user.context.UserContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CodeGenSubAgent implements SubAgent {

	private final CodeGenService codeGenService;

	public CodeGenSubAgent(CodeGenService codeGenService) {
		this.codeGenService = codeGenService;
	}

	@Override
	public String name() {
		return "codegen";
	}

	@Override
	public boolean canHandle(AgentTask task) {
		String message = task.getMessage() == null ? "" : task.getMessage().toLowerCase();
		String intent = task.getIntent() == null ? "" : task.getIntent().toLowerCase();
		return intent.contains("code") || intent.contains("codegen")
				|| message.contains("html") || message.contains("css") || message.contains("页面")
				|| message.contains("生成代码");
	}

	@Override
	public AgentResult execute(AgentTask task, AgentContext context) {
		Long userId = context.getUserId();
		if (userId == null) {
			userId = UserContextHolder.getUserId();
		}
		if (userId == null) {
			userId = 0L;
		}

		Long appId = getLong(task, "appId");
		if (appId == null) {
			String appName = getString(task, "appName");
			if (appName == null || appName.isBlank()) {
				appName = "agentj-codegen-" + System.currentTimeMillis();
			}
			AppVO app = codeGenService.createApp(appName, task.getMessage(), "HTML", userId);
			appId = app.getId();
		}

		CodeGenRequest request = new CodeGenRequest();
		request.setAppId(appId);
		request.setMessage(task.getMessage());

		StringBuilder builder = new StringBuilder();
		Flux<String> stream = codeGenService.generateCodeStream(request, userId);
		stream.doOnNext(builder::append).blockLast();

		AgentResult result = AgentResult.success(name(), builder.toString());
		result.getMetadata().put("appId", appId);
		try {
			AppVO app = codeGenService.getApp(appId, userId);
			result.getMetadata().put("deployKey", app.getDeployKey());
			result.getMetadata().put("previewUrl", app.getPreviewUrl());
		}
		catch (Exception ignored) {
			// ignore metadata fetch failures
		}
		return result;
	}

	@Override
	public int priority() {
		return 10;
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

	private String getString(AgentTask task, String key) {
		Object value = task.getParameters().get(key);
		return value == null ? null : value.toString();
	}
}
