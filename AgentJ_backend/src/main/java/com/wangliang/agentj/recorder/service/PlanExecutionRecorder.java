package com.wangliang.agentj.recorder.service;

import com.wangliang.agentj.model.dto.ActToolParam;
import com.wangliang.agentj.model.dto.ThinkActRecordParams;
import com.wangliang.agentj.runtime.entity.vo.ExecutionStep;

import java.util.List;

/**
 * Plan execution recorder interface that defines methods for recording and retrieving
 * plan execution details.
 */
public interface PlanExecutionRecorder {

	// ========== 第一类：生命周期记录 ==========
	/**
	 * Record the start of plan execution.
	 * information
	 * @return Plan execution record ID
	 */
	public Long recordPlanExecutionStart(String currentPlanId, String title, String userRequset,
			List<ExecutionStep> executionSteps, String parentPlanId, String rootPlanId, String toolcallId);

	/**
	 * Interface 3: Record plan completion. This method handles plan completion recording
	 * logic without exposing internal record objects.
	 * @param currentPlanId Current plan ID
	 * plans)
	 * @param summary The summary of the plan execution
	 */
	void recordPlanCompletion(String currentPlanId, String summary);

	// ========== 第二类：步骤执行记录 ==========
	/**
	 * Record the start of step execution.
	 * @param step Execution step
	 */
	void recordStepStart(ExecutionStep step, String currentPlanId);

	/**
	 * Record the end of step execution.
	 * @param step Execution step
	 */
	void recordStepEnd(ExecutionStep step, String currentPlanId);

	// ========== 第三类：详细执行过程 ==========
	/**
	 * Record complete agent execution at the end. This method handles all agent execution
	 * record management logic without exposing internal record objects.
	 */
	void recordCompleteAgentExecution(com.wangliang.agentj.runtime.entity.vo.ExecutionStep step);

	/**
	 * Interface 1: Record thinking and action execution process. This method handles
	 * ThinkActRecord creation and thinking process without exposing internal record
	 * objects.
	 * @param params Encapsulated parameters for plan execution
	 * @return ThinkActRecord ID for subsequent action recording
	 */
	Long recordThinkingAndAction(com.wangliang.agentj.runtime.entity.vo.ExecutionStep step, ThinkActRecordParams params);

	/**
	 * Interface 2: Record action execution result. This method updates the ThinkActRecord
	 * with action results without exposing internal record objects.
	 */
	void recordActionResult(List<ActToolParam> actToolParamList);

}
