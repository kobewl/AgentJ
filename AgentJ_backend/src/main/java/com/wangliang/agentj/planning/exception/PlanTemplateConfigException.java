package com.wangliang.agentj.planning.exception;

/**
 * Exception thrown when plan template configuration operations fail This exception
 * provides error codes and detailed messages for plan template configuration errors
 */
public class PlanTemplateConfigException extends RuntimeException {

	private final String errorCode;

	/**
	 * Constructs a new PlanTemplateConfigException with the specified error code and
	 * detail message
	 * @param errorCode the error code (e.g., "VALIDATION_ERROR", "NOT_FOUND",
	 * "INTERNAL_ERROR")
	 * @param message the detail message explaining the plan template configuration error
	 */
	public PlanTemplateConfigException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * Constructs a new PlanTemplateConfigException with the specified error code, detail
	 * message, and cause
	 * @param errorCode the error code (e.g., "VALIDATION_ERROR", "NOT_FOUND",
	 * "INTERNAL_ERROR")
	 * @param message the detail message explaining the plan template configuration error
	 * @param cause the cause of the plan template configuration error
	 */
	public PlanTemplateConfigException(String errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * Get the error code
	 * @return the error code
	 */
	public String getErrorCode() {
		return errorCode;
	}

}
