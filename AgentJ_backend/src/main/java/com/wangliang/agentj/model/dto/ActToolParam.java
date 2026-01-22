package com.wangliang.agentj.model.dto;

import java.util.List;

/**
 * Parameter class for action tool information.
 * Based on ActToolInfoEntity structure but without JPA annotations.
 * <p>
 * This class encapsulates the parameters needed to record a tool call during plan execution,
 * including the tool name, parameters (as JSON), execution result, and a unique tool call ID.
 */
public class ActToolParam {

	private final String name;

	private final String parameters;

	private String result;

	private final String toolCallId;

	/**
	 * Creates a new ActToolParam without a result (result will be set after tool execution).
	 * @param name the name of the tool being called
	 * @param parameters the tool parameters as a JSON string
	 * @param toolCallId the unique identifier for this tool call
	 */
	public ActToolParam(String name, String parameters, String toolCallId) {
		this.name = name;
		this.parameters = parameters;
		this.toolCallId = toolCallId;
		this.result = null; // Result is set after tool execution
	}

	/**
	 * Creates a new ActToolParam with a result (for tools that have already been executed).
	 * @param name the name of the tool being called
	 * @param parameters the tool parameters as a JSON string
	 * @param result the execution result of the tool call
	 * @param toolCallId the unique identifier for this tool call
	 */
	public ActToolParam(String name, String parameters, String result, String toolCallId) {
		this.name = name;
		this.parameters = parameters;
		this.result = result;
		this.toolCallId = toolCallId;
	}

	/**
	 * Sets the result of the tool execution.
	 * This method is called after the tool has been executed.
	 * @param result the execution result to set
	 */
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
