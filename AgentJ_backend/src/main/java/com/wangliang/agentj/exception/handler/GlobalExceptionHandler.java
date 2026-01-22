package com.wangliang.agentj.exception.handler;

import com.wangliang.agentj.exception.PlanException;
import com.wangliang.agentj.planning.exception.PlanTemplateConfigException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wangliang
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Handle plan exceptions
	 */
	@SuppressWarnings("rawtypes")
	@ExceptionHandler(PlanException.class)
	public ResponseEntity handlePlanException(PlanException ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("error", ex.getMessage());
		return ResponseEntity.internalServerError().body(response);
	}

	/**
	 * Handle PlanTemplateConfigException - return JSON format with errorCode
	 */
	@ExceptionHandler(PlanTemplateConfigException.class)
	public ResponseEntity<Map<String, Object>> handlePlanTemplateConfigException(PlanTemplateConfigException ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("error", ex.getMessage());
		response.put("errorCode", ex.getErrorCode());
		return ResponseEntity.badRequest().body(response);
	}

	/**
	 * Handle all uncaught exceptions
	 */
	@SuppressWarnings("rawtypes")
	@ExceptionHandler(Exception.class)
	public ResponseEntity handleGlobalException(Exception ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("error", ex.getMessage());
		return ResponseEntity.internalServerError().body(response);
	}

}
