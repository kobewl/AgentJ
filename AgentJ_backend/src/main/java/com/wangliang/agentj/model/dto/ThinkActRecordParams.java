package com.wangliang.agentj.model.dto;

import java.util.List;

/**
 * Parameter class for recording thinking and action during plan execution.
 * Based on ThinkActRecordEntity structure.
 * <p>
 * This class encapsulates all the parameters needed to record a single think-act cycle,
 * including the input/output character counts, error messages, and the list of tools
 * that were called during the action phase.
 */
public class ThinkActRecordParams {

	private final String thinkActId;

	private final String stepId;

	private final String thinkInput;

	private final String thinkOutput;

	private final String errorMessage;

	private final Integer inputCharCount;

	private final Integer outputCharCount;

	private final List<ActToolParam> actToolInfoList;

	/**
	 * Creates a new ThinkActRecordParams without character counts.
	 * Character counts will be calculated internally if needed.
	 * @param thinkActId the unique identifier for this think-act cycle
	 * @param stepId the execution step ID this think-act belongs to
	 * @param thinkInput the input provided to the thinking process
	 * @param thinkOutput the output produced by the thinking process
	 * @param errorMessage any error message that occurred during execution
	 * @param actToolInfoList the list of tool calls made during the action phase
	 */
	public ThinkActRecordParams(String thinkActId, String stepId, String thinkInput, String thinkOutput,
			String errorMessage, List<ActToolParam> actToolInfoList) {
		this(thinkActId, stepId, thinkInput, thinkOutput, errorMessage, null, null, actToolInfoList);
	}

	/**
	 * Creates a new ThinkActRecordParams with all parameters including character counts.
	 * @param thinkActId the unique identifier for this think-act cycle
	 * @param stepId the execution step ID this think-act belongs to
	 * @param thinkInput the input provided to the thinking process
	 * @param thinkOutput the output produced by the thinking process
	 * @param errorMessage any error message that occurred during execution
	 * @param inputCharCount the number of characters in the input
	 * @param outputCharCount the number of characters in the output
	 * @param actToolInfoList the list of tool calls made during the action phase
	 */
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

	// Getters

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

	public Integer getInputCharCount() {
		return inputCharCount;
	}

	public Integer getOutputCharCount() {
		return outputCharCount;
	}

	public List<ActToolParam> getActToolInfoList() {
		return actToolInfoList;
	}

}
