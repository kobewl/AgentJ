/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wangliang.agentj.recorder.service;

import com.wangliang.agentj.runtime.entity.vo.ExecutionStep;

import java.util.List;

/**
 * Plan execution recorder interface that defines methods for recording and retrieving
 * plan execution details.
 */
public interface PlanExecutionRecorder {

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

	/**
	 * Parameter class for recordThinkingAndAction method Based on ThinkActRecordEntity
	 * structure
	 */
	public static class ThinkActRecordParams {

		private final String thinkActId;

		private final String stepId;

		private final String thinkInput;

		private final String thinkOutput;

		private final String errorMessage;

		private final Integer inputCharCount;

		private final Integer outputCharCount;

		private final List<ActToolParam> actToolInfoList;

		public ThinkActRecordParams(String thinkActId, String stepId, String thinkInput, String thinkOutput,
				String errorMessage, List<ActToolParam> actToolInfoList) {
			this(thinkActId, stepId, thinkInput, thinkOutput, errorMessage, null, null, actToolInfoList);
		}

		public ThinkActRecordParams(String thinkActId, String stepId, String thinkInput, String thinkOutput,
				String errorMessage, Integer inputCharCount, Integer outputCharCount,
				List<ActToolParam> actToolInfoList) {

			this.thinkActId = thinkActId;
			this.stepId = stepId;
			this.thinkInput = thinkInput;
			this.thinkOutput = thinkOutput;
			this.errorMessage = errorMessage;
			this.inputCharCount = inputCharCount;
			this.outputCharCount = outputCharCount;
			this.actToolInfoList = actToolInfoList;
		}

		public String getThinkActId() {
			return thinkActId;
		}

		public String getStepId() {
			return stepId;
		}

		public String getThinkInput() {
			return thinkInput;
		}

		public String getThinkOutput() {
			return thinkOutput;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public List<ActToolParam> getActToolInfoList() {
			return actToolInfoList;
		}

		public Integer getInputCharCount() {
			return inputCharCount;
		}

		public Integer getOutputCharCount() {
			return outputCharCount;
		}

	}

	/**
	 * Parameter class for action tool information Based on ActToolInfoEntity structure
	 * but without JPA annotations
	 */
	public static class ActToolParam {

		private final String name;

		private final String parameters;

		private String result;

		private final String toolCallId;

		public ActToolParam(String name, String parameters, String toolCallId) {
			this.name = name;
			this.parameters = parameters;
			this.toolCallId = toolCallId;
			this.result = null; // Result is set after tool execution
		}

		public ActToolParam(String name, String parameters, String result, String toolCallId) {
			this.name = name;
			this.parameters = parameters;
			this.result = result;
			this.toolCallId = toolCallId;
		}

		public void setResult(String result) {
			this.result = result;
		}

		// Getters
		public String getName() {
			return name;
		}

		public String getParameters() {
			return parameters;
		}

		public String getResult() {
			return result;
		}

		public String getToolCallId() {
			return toolCallId;
		}

		@Override
		public String toString() {
			return "ActToolParam{" + "name='" + name + '\'' + ", parameters='" + parameters + '\'' + ", result='"
					+ result + '\'' + ", toolCallId='" + toolCallId + '\'' + '}';
		}

	}

}
