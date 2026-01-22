package com.wangliang.agentj.planning.exception;

/**
 * Exception thrown when parameter validation fails in plan templates This exception
 * provides detailed information about missing or incompatible parameters to help users
 * understand and fix the problem
 */
public class ParameterValidationException extends RuntimeException {

	/**
	 * Constructs a new ParameterValidationException with the specified detail message
	 * @param message the detail message explaining the parameter validation failure
	 */
	public ParameterValidationException(String message) {
		super(message);
	}

	/**
	 * Constructs a new ParameterValidationException with the specified detail message and
	 * cause
	 * @param message the detail message explaining the parameter validation failure
	 * @param cause the cause of the parameter validation failure
	 */
	public ParameterValidationException(String message, Throwable cause) {
		super(message, cause);
	}

}
