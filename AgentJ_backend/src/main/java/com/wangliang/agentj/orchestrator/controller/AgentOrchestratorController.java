package com.wangliang.agentj.orchestrator.controller;

import com.wangliang.agentj.orchestrator.AgentOrchestratorService;
import com.wangliang.agentj.orchestrator.model.AgentRequest;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent Orchestrator", description = "Main agent routing and sub-agent execution")
@RestController
@RequestMapping("/api/agentj/orchestrator")
public class AgentOrchestratorController {

	private final AgentOrchestratorService orchestratorService;

	public AgentOrchestratorController(AgentOrchestratorService orchestratorService) {
		this.orchestratorService = orchestratorService;
	}

	@Operation(summary = "Execute request through main agent orchestrator")
	@PostMapping("/execute")
	public AgentResult execute(@RequestBody AgentRequest request) {
		return orchestratorService.execute(request);
	}
}
