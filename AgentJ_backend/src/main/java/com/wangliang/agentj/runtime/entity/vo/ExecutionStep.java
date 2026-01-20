package com.wangliang.agentj.runtime.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wangliang.agentj.agent.AgentState;
import com.wangliang.agentj.agent.BaseAgent;

import java.util.List;
import java.util.UUID;

/**
 * The result of a single step execution
 */
public class ExecutionStep {

	/**
	 * Unique identifier for this execution step
	 */
	private final String stepId;

	/**
	 * Default constructor that generates a unique step ID
	 */
	public ExecutionStep() {
		this.stepId = "step-" + UUID.randomUUID().toString();
	}

	public ExecutionStep(String stepId) {
		this.stepId = stepId;
	}

	@JsonIgnore
	private Integer stepIndex;

	private String stepRequirement;

	private String agentName;

	private List<String> selectedToolKeys;

	private String modelName;

	@JsonIgnore
	private String result;

	@JsonIgnore
	private String errorMessage;

	@JsonIgnore
	private BaseAgent agent;

	private AgentState status;

	private String terminateColumns;

	/**
	 * Execution mode: autonomous (完全自主) or guided (模板引导)
	 */
	private String executionMode;

	/**
	 * Whether to enable automatic planning (trigger PlanDraftingService)
	 */
	private Boolean enableAutoPlanning;

	/**
	 * Description of what this plan is suitable for
	 */
	private String description;

	/**
	 * List of suitable use cases for this plan
	 */
	private List<String> suitableFor;

	public Integer getStepIndex() {
		return stepIndex;
	}

	public void setStepIndex(Integer stepIndex) {
		this.stepIndex = stepIndex;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@JsonIgnore
	public String getStepId() {
		return stepId;
	}

	public String getTerminateColumns() {
		return terminateColumns;
	}

	public void setTerminateColumns(String terminateColumns) {
		this.terminateColumns = terminateColumns;
	}

	@JsonIgnore
	public AgentState getStatus() {
		return status != null ? status : (agent == null ? AgentState.NOT_STARTED : AgentState.NOT_STARTED);
	}

	public void setStatus(AgentState status) {
		this.status = status;
	}

	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	public BaseAgent getAgent() {
		return this.agent;
	}

	public String getStepRequirement() {
		return stepRequirement;
	}

	public void setStepRequirement(String stepRequirement) {
		this.stepRequirement = stepRequirement;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public List<String> getSelectedToolKeys() {
		return selectedToolKeys;
	}

	public void setSelectedToolKeys(List<String> selectedToolKeys) {
		this.selectedToolKeys = selectedToolKeys;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	@JsonIgnore
	public String getStepInStr() {
		String agentState = null;
		if (status != null) {
			agentState = status.toString();
		}
		else if (agent != null) {
			agentState = AgentState.NOT_STARTED.toString();
		}
		else {
			agentState = AgentState.NOT_STARTED.toString();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(stepIndex);
		sb.append(". ");
		sb.append("[").append(agentState).append("]");
		sb.append(" ");
		sb.append(stepRequirement);

		return sb.toString();
	}

	public String getExecutionMode() {
		return executionMode;
	}

	public void setExecutionMode(String executionMode) {
		this.executionMode = executionMode;
	}

	public Boolean getEnableAutoPlanning() {
		return enableAutoPlanning;
	}

	public void setEnableAutoPlanning(Boolean enableAutoPlanning) {
		this.enableAutoPlanning = enableAutoPlanning;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getSuitableFor() {
		return suitableFor;
	}

	public void setSuitableFor(List<String> suitableFor) {
		this.suitableFor = suitableFor;
	}

}
