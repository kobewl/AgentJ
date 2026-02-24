package com.wangliang.agentj.orchestrator;

import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.orchestrator.model.RoutingDecision;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class RuleBasedRoutingPolicy implements RoutingPolicy {

	@Override
	public RoutingDecision route(AgentTask task, AgentContext context, List<SubAgent> agents) {
		RoutingDecision decision = new RoutingDecision();
		if (agents == null || agents.isEmpty()) {
			decision.setReason("no sub agents registered");
			return decision;
		}

		SubAgent selected = pickAgent(task, agents);
		if (selected != null) {
			decision.getSelectedAgents().add(selected.name());
			decision.setReason("rule-based selection");
		}
		else {
			decision.setReason("no matching agent");
		}
		return decision;
	}

	private SubAgent pickAgent(AgentTask task, List<SubAgent> agents) {
		String message = task.getMessage() == null ? "" : task.getMessage().toLowerCase();
		String intent = task.getIntent() == null ? "" : task.getIntent().toLowerCase();

		boolean wantsRag = intent.contains("rag")
				|| intent.contains("knowledge")
				|| message.contains("知识库")
				|| message.contains("检索")
				|| message.contains("搜索")
				|| message.contains("向量");
		if (wantsRag) {
			SubAgent rag = findByName(agents, "rag");
			if (rag != null) {
				return rag;
			}
		}

		boolean wantsWorkflow = intent.contains("workflow")
				|| message.contains("工作流")
				|| message.contains("流程");
		if (wantsWorkflow) {
			SubAgent workflow = findByName(agents, "workflow");
			if (workflow != null) {
				return workflow;
			}
		}

		boolean wantsCode = intent.contains("code")
				|| intent.contains("codegen")
				|| message.contains("code")
				|| message.contains("html")
				|| message.contains("css")
				|| message.contains("ui")
				|| message.contains("页面")
				|| message.contains("前端")
				|| message.contains("生成代码");

		if (wantsCode) {
			SubAgent codegen = findByName(agents, "codegen");
			if (codegen != null) {
				return codegen;
			}
		}

		boolean wantsDb = intent.contains("db")
				|| intent.contains("database")
				|| message.contains("数据库")
				|| message.contains("sql")
				|| message.contains("查询")
				|| message.contains("表");
		if (wantsDb) {
			SubAgent db = findByName(agents, "db");
			if (db != null) {
				return db;
			}
		}

		boolean wantsBrowser = intent.contains("browser")
				|| intent.contains("web")
				|| message.contains("浏览器")
				|| message.contains("网页")
				|| message.contains("打开网站")
				|| message.contains("抓取");
		if (wantsBrowser) {
			SubAgent browser = findByName(agents, "browser");
			if (browser != null) {
				return browser;
			}
		}

		return agents.stream()
				.filter(agent -> agent.canHandle(task))
				.max(Comparator.comparingInt(SubAgent::priority))
				.orElse(null);
	}

	private SubAgent findByName(List<SubAgent> agents, String name) {
		for (SubAgent agent : agents) {
			if (agent.name().equalsIgnoreCase(name)) {
				return agent;
			}
		}
		return null;
	}
}
