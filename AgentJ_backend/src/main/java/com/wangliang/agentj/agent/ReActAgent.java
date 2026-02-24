package com.wangliang.agentj.agent;

import com.wangliang.agentj.config.AgentJProperties;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.recorder.service.PlanExecutionRecorder;
import com.wangliang.agentj.runtime.entity.vo.ExecutionStep;
import com.wangliang.agentj.runtime.service.PlanIdDispatcher;
import com.wangliang.agentj.runtime.service.TaskInterruptionCheckerService;

import java.util.Map;

/**
 * Base class for ReAct (Reasoning + Acting) pattern agents. Implements an agent pattern
 * where thinking (Reasoning) and acting (Acting) are executed alternately.
 */
public abstract class ReActAgent extends BaseAgent {

	/**
	 * Constructor
	 * @param llmService LLM service instance for handling natural language interactions
	 * @param planExecutionRecorder plan execution recorder for recording execution
	 * process
	 * @param agentjProperties AgentJ configuration properties
	 */
	public ReActAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
                      AgentJProperties agentjProperties, Map<String, Object> initialAgentSetting, ExecutionStep step,
                      PlanIdDispatcher planIdDispatcher) {
		super(llmService, planExecutionRecorder, agentjProperties, initialAgentSetting, step, planIdDispatcher);
	}

	/**
	 * Execute thinking process and determine whether action needs to be taken
	 *
	 * Subclass implementation requirements: 1. Analyze current state and context 2.
	 * Perform logical reasoning to decide on next action 3. Return whether action
	 * execution is needed
	 *
	 * Example implementation: - Return true if tools need to be called - Return false if
	 * current step is completed
	 * @return true indicates action execution is needed, false indicates no action is
	 * currently needed
	 */
	protected abstract boolean think();

	/**
	 * Execute specific actions
	 *
	 * Subclass implementation requirements: 1. Execute specific operations based on
	 * think() decisions 2. Can be tool calls, state updates, or other specific behaviors
	 * 3. Return description of execution results
	 *
	 * Example implementations: - ToolCallAgent: execute selected tool calls -
	 * BrowserAgent: execute browser operations
	 * @return description of action execution results
	 */
	protected abstract AgentExecResult act();

	/**
	 * Execute a complete think-act step
	 * @return returns thinking complete message if no action is needed, otherwise returns
	 * action execution result
	 */
	@Override
	public AgentExecResult step() {
		try {
			boolean shouldAct = think();
			if (!shouldAct) {
				AgentExecResult result = new AgentExecResult("Thinking complete - no action needed",
						AgentState.IN_PROGRESS);

				return result;
			}
			return act();
		}
		catch (TaskInterruptionCheckerService.TaskInterruptedException e) {
			// Agent was interrupted, return INTERRUPTED state to stop execution
			return new AgentExecResult("Agent execution interrupted: " + e.getMessage(), AgentState.INTERRUPTED);
		}
	}

}
