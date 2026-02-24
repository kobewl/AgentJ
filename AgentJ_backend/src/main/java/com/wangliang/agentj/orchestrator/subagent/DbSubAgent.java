package com.wangliang.agentj.orchestrator.subagent;

import com.wangliang.agentj.orchestrator.SubAgent;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.tools.code.ToolExecuteResult;
import com.wangliang.agentj.tools.database.DatabaseReadTool;
import com.wangliang.agentj.tools.database.DatabaseRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DbSubAgent implements SubAgent {

	private final DatabaseReadTool databaseReadTool;

	public DbSubAgent(DatabaseReadTool databaseReadTool) {
		this.databaseReadTool = databaseReadTool;
	}

	@Override
	public String name() {
		return "db";
	}

	@Override
	public boolean canHandle(AgentTask task) {
		String message = task.getMessage() == null ? "" : task.getMessage().toLowerCase();
		String intent = task.getIntent() == null ? "" : task.getIntent().toLowerCase();
		return intent.contains("db") || intent.contains("database")
				|| message.contains("数据库") || message.contains("sql")
				|| message.contains("查询") || message.contains("表");
	}

	@Override
	public AgentResult execute(AgentTask task, AgentContext context) {
		DatabaseRequest request = new DatabaseRequest();
		request.setAction(getString(task, "action", "execute_read_sql"));
		request.setDatasourceName(getString(task, "datasourceName", null));
		request.setQuery(getString(task, "query", task.getMessage()));
		request.setText(getString(task, "text", null));
		request.setFileName(getString(task, "fileName", null));
		Object params = task.getParameters().get("parameters");
		if (params instanceof List) {
			request.setParameters((List<Object>) params);
		}

		databaseReadTool.setCurrentPlanId(task.getId());
		databaseReadTool.setRootPlanId(task.getId());
		ToolExecuteResult result = databaseReadTool.run(request);
		AgentResult agentResult = AgentResult.success(name(), result == null ? "" : result.getOutput());
		agentResult.getMetadata().put("action", request.getAction());
		agentResult.getMetadata().put("datasource", request.getDatasourceName());
		return agentResult;
	}

	@Override
	public int priority() {
		return 7;
	}

	private String getString(AgentTask task, String key, String fallback) {
		Object value = task.getParameters().get(key);
		if (value == null) {
			return fallback;
		}
		String text = value.toString();
		return text.isBlank() ? fallback : text;
	}
}
