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
package com.wangliang.agentj.runtime.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wangliang.agentj.config.LynxeProperties;
import com.wangliang.agentj.conversation.entity.vo.Memory;
import com.wangliang.agentj.conversation.service.MemoryService;
import com.wangliang.agentj.event.LynxeListener;
import com.wangliang.agentj.event.PlanExceptionClearedEvent;
import com.wangliang.agentj.event.PlanExceptionEvent;
import com.wangliang.agentj.exception.PlanException;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.llm.StreamingResponseHandler;
import com.wangliang.agentj.planning.service.IPlanParameterMappingService;
import com.wangliang.agentj.planning.model.vo.PlanTemplateConfigVO;
import com.wangliang.agentj.planning.service.PlanTemplateConfigService;
import com.wangliang.agentj.planning.service.PlanTemplateService;
import com.wangliang.agentj.recorder.entity.vo.ActToolInfo;
import com.wangliang.agentj.recorder.entity.vo.AgentExecutionRecord;
import com.wangliang.agentj.recorder.entity.vo.PlanExecutionRecord;
import com.wangliang.agentj.recorder.entity.vo.ThinkActRecord;
import com.wangliang.agentj.recorder.service.NewRepoPlanExecutionRecorder;
import com.wangliang.agentj.recorder.service.PlanHierarchyReaderService;
import com.wangliang.agentj.runtime.entity.po.RootTaskManagerEntity;
import com.wangliang.agentj.runtime.entity.vo.*;
import com.wangliang.agentj.runtime.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/executor")
public class LynxeController implements LynxeListener<PlanExceptionEvent> {

	private static final Logger logger = LoggerFactory.getLogger(LynxeController.class);

	private final ObjectMapper objectMapper;

	private final Cache<String, Throwable> exceptionCache;

	@Autowired
	@Lazy
	private PlanningCoordinator planningCoordinator;

	@Autowired
	private PlanHierarchyReaderService planHierarchyReaderService;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private UserInputService userInputService;

	@Autowired
	private MemoryService memoryService;

	@Autowired
	private NewRepoPlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private PlanTemplateConfigService planTemplateConfigService;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private IPlanParameterMappingService parameterMappingService;

	@Autowired
	private RootTaskManagerService rootTaskManagerService;

	@Autowired
	private TaskInterruptionManager taskInterruptionManager;

	@Autowired
	@Lazy
	private LynxeProperties lynxeProperties;

	@Autowired
	@Lazy
	private LlmService llmService;

	@Autowired
	@Lazy
	private StreamingResponseHandler streamingResponseHandler;

	@Autowired
	@Lazy
	private com.wangliang.agentj.user.service.UserService userService;

	@Autowired
	@Lazy
	private com.wangliang.agentj.user.service.UserPersonalMemoryService userPersonalMemoryService;

	public LynxeController(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		// Register JavaTimeModule to handle LocalDateTime serialization/deserialization
		this.objectMapper.registerModule(new JavaTimeModule());
		// Ensure pretty printing is disabled by default for compact JSON
		// this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
		// 10minutes timeout for plan exception
		this.exceptionCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
	}

	/**
	 * Get request source from request map, default to HTTP_REQUEST if not provided
	 * @param request Request map
	 * @return RequestSource enum
	 */
	private RequestSource getRequestSource(Map<String, Object> request) {
		// Check for requestSource field (enum-based approach)
		Object requestSourceObj = request.get("requestSource");
		if (requestSourceObj != null) {
			if (requestSourceObj instanceof String) {
				return RequestSource.fromString((String) requestSourceObj);
			}
			else if (requestSourceObj instanceof RequestSource) {
				return (RequestSource) requestSourceObj;
			}
		}

		// By default, it is an HTTP request
		return RequestSource.HTTP_REQUEST;
	}

	/**
	 * Execute plan by tool name synchronously (GET method)
	 * @param toolName Tool name
	 * @return Execution result directly
	 */
	@GetMapping("/executeByToolNameSync/{toolName}")
	public ResponseEntity<Map<String, Object>> executeByToolNameGetSync(@PathVariable("toolName") String toolName,
			@RequestParam(required = false, name = "allParams") Map<String, String> allParams,
			@RequestParam(required = false, name = "serviceGroup") String serviceGroup) {
		if (toolName == null || toolName.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool name cannot be empty"));
		}

		// Get plan template ID from coordinator tool
		String planTemplateId = getPlanTemplateIdFromTool(toolName, serviceGroup);
		if (planTemplateId == null) {
			planTemplateId = ensureRuntimePlanTemplate(toolName, serviceGroup);
			if (planTemplateId == null) {
				return ResponseEntity.badRequest().body(Map.of("error", "Tool not found with name: " + toolName));
			}
		}
		if (planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "No plan template ID associated with tool: " + toolName));
		}

		// Execute synchronously and return result directly
		RequestSource requestSource = RequestSource.HTTP_REQUEST; // GET requests default
																	// to HTTP_REQUEST

		// Extract conversationId from query params if present
		String conversationId = allParams != null ? allParams.get("conversationId") : null;
		conversationId = validateOrGenerateConversationId(conversationId, requestSource);

		logger.info("Execute tool '{}' synchronously with plan template ID '{}', parameters: {}, conversationId: {}",
				toolName, planTemplateId, allParams, conversationId);
		return executePlanSync(planTemplateId, null, null, requestSource, null, conversationId);
	}

	/**
	 * Execute plan by tool name asynchronously
	 * @param request Request containing tool name and parameters
	 * @return Task ID and status
	 */
	@PostMapping("/executeByToolNameAsync")
	public ResponseEntity<Map<String, Object>> executeByToolNameAsync(@RequestBody Map<String, Object> request) {
		String toolName = (String) request.get("toolName");
		if (toolName == null || toolName.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool name cannot be empty"));
		}
		RequestSource requestSource = getRequestSource(request);

		// Log request source
		logger.info("📡 [{}] Received query request from: {}", requestSource, requestSource.name());

		// Extract serviceGroup from request (optional)
		String serviceGroup = (String) request.get("serviceGroup");

		// Get plan template ID from coordinator tool
		String planTemplateId = getPlanTemplateIdFromTool(toolName, serviceGroup);
		if (planTemplateId == null) {
			planTemplateId = ensureRuntimePlanTemplate(toolName, serviceGroup);
			if (planTemplateId == null) {
				return ResponseEntity.badRequest().body(Map.of("error", "Tool not found with name: " + toolName));
			}
		}
		if (planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "No plan template ID associated with tool: " + toolName));
		}

		try {
			// Validate or generate conversationId for VUE_DIALOG and VUE_SIDEBAR requests
			// Both should use the same conversation memory
			String conversationId = validateOrGenerateConversationId((String) request.get("conversationId"),
					requestSource);

			// Handle uploaded files if present
			@SuppressWarnings("unchecked")
			List<String> uploadedFiles = (List<String>) request.get("uploadedFiles");

			String uploadKey = (String) request.get("uploadKey");

			// Debug logging for uploaded files
			logger.info("🔍 [DEBUG] Request keys: {}", request.keySet());
			logger.info("🔍 [DEBUG] uploadedFiles from request: {}", uploadedFiles);
			logger.info("🔍 [DEBUG] uploadedFiles is null: {}", uploadedFiles == null);
			if (uploadedFiles != null) {
				logger.info("🔍 [DEBUG] uploadedFiles size: {}", uploadedFiles.size());
				logger.info("🔍 [DEBUG] uploadedFiles names: {}", uploadedFiles);
			}

			// Get replacement parameters for <<>> replacement
			@SuppressWarnings("unchecked")
			Map<String, Object> replacementParams = (Map<String, Object>) request.get("replacementParams");

			// Execute the plan template using the new unified method
			PlanExecutionWrapper wrapper = executePlanTemplate(planTemplateId, uploadedFiles, conversationId,
					replacementParams, requestSource, uploadKey);

			// Create or update task manager entity for database-driven interruption
			if (wrapper.getRootPlanId() != null) {
				rootTaskManagerService.createOrUpdateTask(wrapper.getRootPlanId(),
						RootTaskManagerEntity.DesiredTaskState.START);
			}

			// Start the async execution (fire and forget)
			wrapper.getResult().whenComplete((result, throwable) -> {
				if (throwable != null) {
					logger.error("Async plan execution failed for planId: {}", wrapper.getRootPlanId(), throwable);
					// Complete task with failure state
					rootTaskManagerService.completeTask(wrapper.getRootPlanId(),
							"Execution failed: " + throwable.getMessage(), false);
				}
				else {
					logger.info("Async plan execution completed for planId: {}", wrapper.getRootPlanId());
					// Complete task with success state
					rootTaskManagerService.completeTask(wrapper.getRootPlanId(),
							result != null ? result.getFinalResult() : "Execution completed", true);
				}
			});

			// Return task ID and initial status
			Map<String, Object> response = new HashMap<>();
			response.put("planId", wrapper.getRootPlanId());
			response.put("status", "processing");
			response.put("message", "Task submitted, processing");
			response.put("conversationId", conversationId);
			response.put("toolName", toolName);
			response.put("planTemplateId", planTemplateId);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Failed to start plan execution for tool: {} with planTemplateId: {}", toolName,
					planTemplateId, e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to start plan execution: " + e.getMessage());
			errorResponse.put("toolName", toolName);
			errorResponse.put("planTemplateId", planTemplateId);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute plan by tool name synchronously (POST method)
	 * @param request Request containing tool name
	 * @return Execution result directly
	 */
	@PostMapping("/executeByToolNameSync")
	public ResponseEntity<Map<String, Object>> executeByToolNameSync(@RequestBody Map<String, Object> request) {
		String toolName = (String) request.get("toolName");
		if (toolName == null || toolName.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool name cannot be empty"));
		}

		RequestSource requestSource = getRequestSource(request);

		// Log request source
		logger.info("📡 [{}] Received query request from: {}", requestSource, requestSource.name());

		// Extract serviceGroup from request (optional)
		String serviceGroup = (String) request.get("serviceGroup");

		// Get plan template ID from coordinator tool
		String planTemplateId = getPlanTemplateIdFromTool(toolName, serviceGroup);
		if (planTemplateId == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool not found with name: " + toolName));
		}
		if (planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "No plan template ID associated with tool: " + toolName));
		}

		// Handle uploaded files if present
		@SuppressWarnings("unchecked")
		List<String> uploadedFiles = (List<String>) request.get("uploadedFiles");

		String uploadKey = (String) request.get("uploadKey");

		// Get replacement parameters for <<>> replacement
		@SuppressWarnings("unchecked")
		Map<String, Object> replacementParams = (Map<String, Object>) request.get("replacementParams");

		// Validate or generate conversation ID for VUE_DIALOG and VUE_SIDEBAR requests
		String conversationId = validateOrGenerateConversationId((String) request.get("conversationId"), requestSource);

		logger.info(
				"Executing tool '{}' synchronously with plan template ID '{}', uploadedFiles: {}, replacementParams: {}, uploadKey: {}, conversationId: {}",
				toolName, planTemplateId, uploadedFiles != null ? uploadedFiles.size() : "null",
				replacementParams != null ? replacementParams.size() : "null", uploadKey, conversationId);

		return executePlanSync(planTemplateId, uploadedFiles, replacementParams, requestSource, uploadKey,
				conversationId);
	}

	/**
	 * Get execution record overview (without detailed ThinkActRecord information) Note:
	 * This method returns basic execution information and does not include detailed
	 * ThinkActRecord steps for each agent execution.
	 * @param planId Plan ID
	 * @return JSON representation of execution record overview
	 */
	@GetMapping("/details/{planId}")
	public synchronized ResponseEntity<?> getExecutionDetails(@PathVariable("planId") String planId) {
		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body("Plan ID cannot be null or empty");
		}
		PlanExecutionRecord planRecord = buildPlanExecutionView(planId);
		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		try {
			// Use Jackson ObjectMapper to convert object to JSON string
			String jsonResponse = objectMapper.writeValueAsString(planRecord);
			return ResponseEntity.ok(jsonResponse);
		}
		catch (JsonProcessingException e) {
			logger.error("Error serializing PlanExecutionRecord to JSON for planId: {}", planId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error processing request: " + e.getMessage());
		}
	}

	/**
	 * Delete execution record for specified plan ID
	 * @param planId Plan ID
	 * @return Result of delete operation
	 */
	@DeleteMapping("/details/{planId}")
	public ResponseEntity<Map<String, String>> removeExecutionDetails(@PathVariable("planId") String planId) {
		PlanExecutionRecord planRecord = planHierarchyReaderService.readPlanTreeByRootId(planId);
		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		// Note: We don't need to remove execution records since they are already stored
		// in the database
		// The database serves as the persistent storage for all execution records
		return ResponseEntity.ok(Map.of("message", "Execution record found (no deletion needed)", "planId", planId));
	}

	/**
	 * Submits user input for a plan that is waiting.
	 * @param planId The ID of the plan.
	 * @param formData The user-submitted form data, expected as Map<String, String>.
	 * @return ResponseEntity indicating success or failure.
	 */
	@PostMapping("/submit-input/{planId}")
	public ResponseEntity<Map<String, Object>> submitUserInput(@PathVariable("planId") String planId,
			@RequestBody Map<String, String> formData) { // Changed formData to
		// Map<String, String>
		try {
			logger.info("Received user input for plan {}: {}", planId, formData);

			// Submit user input to the provided planId
			// Since getExecutionDetails now sets the correct planId in waitState, this
			// should work correctly
			boolean success = userInputService.submitUserInputs(planId, formData);
			if (success) {
				logger.info("Successfully submitted user input to plan {}", planId);
				return ResponseEntity.ok(Map.of("message", "Input submitted successfully", "planId", planId));
			}

			// No waiting plan found
			logger.warn("No waiting plan found for user input submission. Plan {} is not waiting for input.", planId);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "No plan is currently waiting for user input.", "planId", planId));
		}
		catch (IllegalArgumentException e) {
			logger.error("Error submitting user input for plan {}: {}", planId, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", e.getMessage(), "planId", planId));
		}
		catch (Exception e) {
			logger.error("Unexpected error submitting user input for plan {}: {}", planId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "An unexpected error occurred.", "planId", planId));
		}
	}

	/**
	 * Execute plan synchronously and build response with parameter replacement support
	 * @param planTemplateId The plan template ID to execute
	 * @param uploadedFiles List of uploaded file names (can be null)
	 * @param replacementParams Parameters for <<>> replacement (can be null)
	 * @param requestSource Request source (HTTP_REQUEST, VUE_SIDEBAR, or VUE_DIALOG)
	 * @param uploadKey Optional uploadKey provided by frontend (can be null)
	 * @param conversationId Conversation ID for the execution (validated/generated)
	 * @return ResponseEntity with execution result
	 */
	private ResponseEntity<Map<String, Object>> executePlanSync(String planTemplateId, List<String> uploadedFiles,
			Map<String, Object> replacementParams, RequestSource requestSource, String uploadKey,
			String conversationId) {
		PlanExecutionWrapper wrapper = null;
		try {
			// Execute the plan template using the new unified method
			wrapper = executePlanTemplate(planTemplateId, uploadedFiles, conversationId, replacementParams,
					requestSource, uploadKey);

			// Create or update task manager entity for database-driven interruption
			if (wrapper.getRootPlanId() != null) {
				rootTaskManagerService.createOrUpdateTask(wrapper.getRootPlanId(),
						RootTaskManagerEntity.DesiredTaskState.START);
			}

			PlanExecutionResult planExecutionResult = wrapper.getResult().get();

			// Complete task with success state and execution result
			if (planExecutionResult != null) {
				rootTaskManagerService.completeTask(wrapper.getRootPlanId(), planExecutionResult.getFinalResult(),
						true);
			}

			// Return success with execution result
			Map<String, Object> response = new HashMap<>();
			response.put("status", "completed");
			response.put("result", planExecutionResult != null ? planExecutionResult.getFinalResult() : "No result");
			response.put("conversationId", conversationId);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Failed to execute plan template synchronously: {}", planTemplateId, e);

			// Complete task with failure state
			if (wrapper != null && wrapper.getRootPlanId() != null) {
				rootTaskManagerService.completeTask(wrapper.getRootPlanId(), "Execution failed: " + e.getMessage(),
						false);
			}

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Execution failed: " + e.getMessage());
			errorResponse.put("status", "failed");
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute a plan template by its ID with parameter replacement support
	 *
	 * key method
	 * @param planTemplateId The ID of the plan template to execute
	 * @param uploadedFiles List of uploaded file names (can be null)
	 * @param conversationId Conversation ID for the execution (can be null)
	 * @param replacementParams Parameters for <<>> replacement (can be null)
	 * @param requestSource Request source (HTTP_REQUEST, VUE_SIDEBAR, or VUE_DIALOG)
	 * @param uploadKey Optional uploadKey provided by frontend (can be null)
	 * @return PlanExecutionWrapper containing both PlanExecutionResult and rootPlanId
	 */
	private PlanExecutionWrapper executePlanTemplate(String planTemplateId, List<String> uploadedFiles,
			String conversationId, Map<String, Object> replacementParams, RequestSource requestSource,
			String uploadKey) {
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			logger.error("Plan template ID is null or empty");
			throw new IllegalArgumentException("Plan template ID cannot be null or empty");
		}
		String planJson = null;
		try {

			String currentPlanId;
			String rootPlanId;
			currentPlanId = planIdDispatcher.generatePlanId();
			rootPlanId = currentPlanId;
			logger.info("🆕 Generated new planId: {}", currentPlanId);

			// Get the latest plan version JSON string
			planJson = planTemplateService.getLatestPlanVersion(planTemplateId);
			if (planJson == null) {
				throw new RuntimeException("Plan template not found: " + planTemplateId);
			}

			// Prepare parameters for replacement
			Map<String, Object> parametersForReplacement = new HashMap<>();
			if (replacementParams != null) {
				parametersForReplacement.putAll(replacementParams);
			}
			// Add the generated planId to parameters
			parametersForReplacement.put("planId", rootPlanId);

			// Replace parameter placeholders (<< >>) with actual input parameters
			if (!parametersForReplacement.isEmpty()) {
				try {
					logger.info("Replacing parameter placeholders in plan template with input parameters: {}",
							parametersForReplacement.keySet());
					planJson = parameterMappingService.replaceParametersInJson(planJson, parametersForReplacement);
					logger.debug("Parameter replacement completed successfully");
				}
				catch (Exception e) {
					String errorMsg = "Failed to replace parameters in plan template: " + e.getMessage();
					logger.error(errorMsg, e);
					CompletableFuture<PlanExecutionResult> failedFuture = new CompletableFuture<>();
					failedFuture.completeExceptionally(new RuntimeException(errorMsg, e));
					return new PlanExecutionWrapper(failedFuture, null);
				}
			}
			else {
				logger.debug("No parameter replacement needed - replacementParams: {}",
						replacementParams != null ? replacementParams.size() : 0);
			}

			// Parse the plan JSON to create PlanInterface
			PlanInterface plan = objectMapper.readValue(planJson, PlanInterface.class);

			// Handle uploaded files if present
			if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
				logger.info("Uploaded files will be handled by the execution context for plan template: {}",
						uploadedFiles.size());

				// Attach uploaded files to each step's stepRequirement
				if (plan.getAllSteps() != null) {
					for (ExecutionStep step : plan.getAllSteps()) {
						if (step.getStepRequirement() != null) {
							String fileInfo = String.join(", ", uploadedFiles);
							String originalRequirement = step.getStepRequirement();
							step.setStepRequirement(originalRequirement + "\n \n  [Uploaded files: " + fileInfo + "]");
							logger.info("Attached uploaded files to step requirement: {}", step.getStepRequirement());
						}
					}
				}
			}

			// Log uploadKey if provided
			if (uploadKey != null) {
				logger.info("Executing plan with upload key: {}", uploadKey);
			}

			// Create Memory with step requirements as the name
			if (conversationId != null && !conversationId.trim().isEmpty()) {
				String memoryName = buildMemoryNameFromPlan(plan);
				Memory memory = new Memory(conversationId, memoryName);
				memoryService.saveMemory(memory);
				logger.info("Created/updated memory with name: {}", memoryName);
			}

			// Execute using the PlanningCoordinator (root plan has depth = 0)
			CompletableFuture<PlanExecutionResult> future = planningCoordinator.executeByPlan(plan, rootPlanId, null,
					currentPlanId, null, requestSource, uploadKey, 0, conversationId);

			// Return the wrapper containing both the future and rootPlanId
			return new PlanExecutionWrapper(future, rootPlanId);

		}
		catch (Exception e) {
			logger.error("Failed to execute plan template: {}", planTemplateId, e);
			logger.error("Failed to execute plan json : {}", planJson);
			CompletableFuture<PlanExecutionResult> failedFuture = new CompletableFuture<>();
			failedFuture.completeExceptionally(new RuntimeException("Plan execution failed: " + e.getMessage(), e));
			return new PlanExecutionWrapper(failedFuture, null);
		}
	}

	/**
	 * Get detailed agent execution record by stepId (includes ThinkActRecord details)
	 * @param stepId The step ID to query
	 * @return Detailed agent execution record with ThinkActRecord details
	 */
	@GetMapping("/agent-execution/{stepId}")
	public ResponseEntity<AgentExecutionRecord> getAgentExecutionDetail(@PathVariable("stepId") String stepId) {
		try {
			logger.info("Fetching agent execution detail for stepId: {}", stepId);

			AgentExecutionRecord detail = planExecutionRecorder.getAgentExecutionDetail(stepId);
			if (detail == null) {
				logger.warn("Agent execution detail not found for stepId: {}", stepId);
				return ResponseEntity.notFound().build();
			}

			logger.info("Successfully retrieved agent execution detail for stepId: {}", stepId);
			return ResponseEntity.ok(detail);
		}
		catch (Exception e) {
			logger.error("Error fetching agent execution detail for stepId: {}", stepId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Extract the last tool call result from the plan execution record. This method
	 * traverses through the execution hierarchy: PlanExecutionRecord ->
	 * AgentExecutionRecord -> ThinkActRecord -> ActToolInfo to get the result from the
	 * last tool call.
	 * @param planRecord The plan execution record
	 * @return The last tool call result, or null if not found
	 */
	private String extractLastToolCallResult(PlanExecutionRecord planRecord) {
		if (planRecord == null || !planRecord.isCompleted()) {
			return null;
		}

		// Get the agent execution sequence
		List<AgentExecutionRecord> agentExecutionSequence = planRecord.getAgentExecutionSequence();
		if (agentExecutionSequence == null || agentExecutionSequence.isEmpty()) {
			return null;
		}

		// Get the last agent execution record
		AgentExecutionRecord lastAgentRecord = agentExecutionSequence.get(agentExecutionSequence.size() - 1);
		if (lastAgentRecord == null) {
			return null;
		}

		// Get stepId from the last agent execution record
		String stepId = lastAgentRecord.getStepId();
		if (stepId == null || stepId.trim().isEmpty()) {
			logger.warn("StepId is null or empty in the last agent execution record");
			return null;
		}

		// Use stepId to get the real AgentExecutionRecord with actual thinkActSteps
		// The thinkActSteps in agentExecutionSequence is dummy data
		AgentExecutionRecord realAgentRecord = planExecutionRecorder.getAgentExecutionDetail(stepId);
		if (realAgentRecord == null) {
			logger.warn("Failed to get real agent execution detail for stepId: {}", stepId);
			return null;
		}

		// Get the think-act steps from the real agent execution record
		List<ThinkActRecord> thinkActSteps = realAgentRecord.getThinkActSteps();
		if (thinkActSteps == null || thinkActSteps.isEmpty()) {
			return null;
		}

		// Get the last think-act record
		ThinkActRecord lastThinkActRecord = thinkActSteps.get(thinkActSteps.size() - 1);
		if (lastThinkActRecord == null) {
			return null;
		}

		// Get the act tool info list from the last think-act record
		List<ActToolInfo> actToolInfoList = lastThinkActRecord.getActToolInfoList();
		if (actToolInfoList == null || actToolInfoList.isEmpty()) {
			return null;
		}

		// Get the last act tool info
		ActToolInfo lastActToolInfo = actToolInfoList.get(actToolInfoList.size() - 1);
		if (lastActToolInfo == null) {
			return null;
		}

		// Get the result from the last tool call
		String result = lastActToolInfo.getResult();
		if (result == null) {
			return null;
		}

		// If the result is a JSON string, parse and re-serialize it to avoid double
		// escaping
		// This happens when TerminateTool returns a JSON string that gets stored as a
		// string field
		try {
			// Try to parse as JSON using JsonNode to preserve field order
			JsonNode jsonNode = objectMapper.readTree(result);
			// Re-serialize without escaping, preserving field order
			return objectMapper.writeValueAsString(jsonNode);
		}
		catch (Exception e) {
			// If it's not valid JSON, return as-is
			return result;
		}
	}

	/**
	 * Get plan template ID from coordinator tool by tool name Only returns plan template
	 * ID if HTTP service is enabled for the tool
	 * @param toolName The tool name to look up
	 * @param serviceGroup Optional service group to disambiguate tools with same name
	 * @return Plan template ID if found and HTTP service is enabled, null otherwise
	 */
	private String getPlanTemplateIdFromTool(String toolName, String serviceGroup) {
		return planTemplateConfigService.getPlanTemplateIdFromToolName(toolName, serviceGroup);
	}

	@Override
	public void onEvent(PlanExceptionEvent event) {
		String planId = event.getPlanId();
		Throwable throwable = event.getThrowable();
		if (planId != null && throwable != null) {
			this.exceptionCache.put(planId, throwable);
		}
	}

	@EventListener
	public void onPlanExceptionCleared(PlanExceptionClearedEvent event) {
		String planId = event.getPlanId();
		if (planId != null) {
			logger.info("Clearing exception cache for planId: {}", planId);
			this.exceptionCache.invalidate(planId);
		}
	}

	/**
	 * Stop a running task by plan ID
	 * @param planId The plan ID to stop
	 * @return Response indicating success or failure
	 */
	@PostMapping("/stopTask/{planId}")
	public ResponseEntity<Map<String, Object>> stopTask(@PathVariable("planId") String planId) {
		try {
			logger.info("Received stop task request for planId: {}", planId);

			// Check if task is currently running using database state
			boolean isTaskRunning = taskInterruptionManager.isTaskRunning(planId);
			boolean taskExists = rootTaskManagerService.taskExists(planId);

			if (!isTaskRunning && !taskExists) {
				logger.warn("No active task found for planId: {}", planId);
				return ResponseEntity.badRequest()
					.body(Map.of("error", "No active task found for the given plan ID", "planId", planId));
			}

			// Mark task for stop in database (database-driven interruption)
			boolean taskMarkedForStop = taskInterruptionManager.stopTask(planId);

			// Note: taskInterruptionManager.stopTask() already sets state to STOP and
			// end_time
			// We just update the result message here
			if (taskMarkedForStop) {
				rootTaskManagerService.updateTaskResult(planId, "Task manually stopped by user");
			}

			logger.info("Successfully marked task for stop for planId: {}", planId);
			return ResponseEntity
				.ok(Map.of("status", "stopped", "planId", planId, "message", "Task stop request processed successfully",
						"taskMarkedForStop", taskMarkedForStop, "wasRunning", isTaskRunning));

		}
		catch (Exception e) {
			logger.error("Failed to stop task for planId: {}", planId, e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to stop task: " + e.getMessage(), "planId", planId));
		}
	}

	/**
	 * Get task status by plan ID
	 * @param planId The plan ID to check
	 * @return Task status information
	 */
	@GetMapping("/taskStatus/{planId}")
	public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable("planId") String planId) {
		try {
			logger.info("Getting task status for planId: {}", planId);

			return ResponseEntity.ok(buildTaskStatus(planId));

		}
		catch (Exception e) {
			logger.error("Failed to get task status for planId: {}", planId, e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to get task status: " + e.getMessage(), "planId", planId));
		}
	}

	/**
	 * Build execution view with wait-state and structure result enrichment.
	 * @param planId The plan ID
	 * @return The enriched plan record or null if not found
	 */
	private PlanExecutionRecord buildPlanExecutionView(String planId) {
		Throwable throwable = this.exceptionCache.getIfPresent(planId);
		if (throwable != null) {
			logger.error("Exception found in exception cache for planId: {}", planId, throwable);
			logger.error("Invalidating exception cache for planId: {}", planId);
			this.exceptionCache.invalidate(planId);
			throw new PlanException(throwable);
		}

		PlanExecutionRecord planRecord = planHierarchyReaderService.readPlanTreeByRootId(planId);
		if (planRecord == null) {
			return null;
		}

		// Check for user input wait state and merge it into the plan record
		String rootPlanId = planRecord.getRootPlanId() != null ? planRecord.getRootPlanId() : planId;
		UserInputWaitState waitState = userInputService.getWaitState(rootPlanId);
		if (waitState != null && waitState.isWaiting()) {
			waitState.setPlanId(rootPlanId);
			planRecord.setUserInputWaitState(waitState);
			logger.info(
					"Root plan {} is waiting for user input. Set waitState planId to rootPlanId for proper submission.",
					rootPlanId);
		}
		else {
			planRecord.setUserInputWaitState(null);
		}

		// Set rootPlanId if it's null, using currentPlanId as default
		if (planRecord.getRootPlanId() == null) {
			planRecord.setRootPlanId(planRecord.getCurrentPlanId());
			logger.info("Set rootPlanId to currentPlanId for plan: {}", planId);
		}

		// Enrich agentExecutionSequence with detailed think/act steps from repository
		if (planRecord.getAgentExecutionSequence() != null && !planRecord.getAgentExecutionSequence().isEmpty()) {
			List<AgentExecutionRecord> enrichedAgents = new java.util.ArrayList<>();
			for (AgentExecutionRecord agent : planRecord.getAgentExecutionSequence()) {
				if (agent == null || !StringUtils.hasText(agent.getStepId())) {
					enrichedAgents.add(agent);
					continue;
				}
				AgentExecutionRecord detailed = planExecutionRecorder.getAgentExecutionDetail(agent.getStepId());
				enrichedAgents.add(detailed != null ? detailed : agent);
			}
			planRecord.setAgentExecutionSequence(enrichedAgents);

			// Surface latest partial result even when not finished
			String latest = extractLatestResult(enrichedAgents);
			if (StringUtils.hasText(latest)) {
				planRecord.setStructureResult(latest);
			}
		}
		return planRecord;
	}

	/**
	 * Build task status payload reused by status API and SSE stream.
	 * @param planId Plan ID
	 * @return status map
	 */
	private Map<String, Object> buildTaskStatus(String planId) {
		boolean isTaskRunning = taskInterruptionManager.isTaskRunning(planId);
		Optional<RootTaskManagerEntity> taskEntity = rootTaskManagerService.getTaskByRootPlanId(planId);

		Map<String, Object> response = new HashMap<>();
		response.put("planId", planId);
		response.put("isRunning", isTaskRunning);

		if (taskEntity.isPresent()) {
			RootTaskManagerEntity task = taskEntity.get();
			response.put("desiredState", task.getDesiredTaskState());
			response.put("startTime", task.getStartTime());
			response.put("endTime", task.getEndTime());
			response.put("lastUpdated", task.getLastUpdated());
		response.put("taskResult", task.getTaskResult());
		response.put("exists", true);
	}
	else {
			response.put("exists", false);
			response.put("desiredState", null);
			response.put("startTime", null);
			response.put("endTime", null);
			response.put("lastUpdated", null);
			response.put("taskResult", null);
		}
		return response;
	}

	/**
	 * Create a minimal runtime plan template and coordinator tool when a toolName is not
	 * pre-registered. This removes the need for users to manage plan templates manually.
	 * @param toolName tool name provided by user
	 * @param serviceGroup optional service group
	 * @return planTemplateId or null if creation fails
	 */
	private String ensureRuntimePlanTemplate(String toolName, String serviceGroup) {
		try {
			String planTemplateId = "runtime-" + toolName;

			PlanTemplateConfigVO config = new PlanTemplateConfigVO();
			config.setPlanTemplateId(planTemplateId);
			config.setTitle(toolName);
			config.setPlanType("dynamic_agent");
			config.setServiceGroup(serviceGroup != null && !serviceGroup.isEmpty() ? serviceGroup : "ungrouped");
			config.setDirectResponse(false);

			PlanTemplateConfigVO.StepConfig step = new PlanTemplateConfigVO.StepConfig();
			step.setAgentName("ConfigurableDynaAgent");
			// Use DEFAULT_AGENT tag so it routes to ConfigurableDynaAgent executor
			String lower = toolName.toLowerCase();
			if (lower.startsWith("ai_database_read_agent")) {
				step.setStepRequirement(
						"[DEFAULT_AGENT] 你是数据库助手。请使用工具 database_read_use / database_metadata_use 完成任务: <<input>>\n"
								+ "要求：优先使用 datasourceName（如用户提供）。需要查表时优先 database_read_use(action=get_table_name) 或查询 information_schema。\n"
								+ "完成后调用 terminate 返回结果。");
			}
			else if (lower.startsWith("ai_database_write_agent")) {
				step.setStepRequirement(
						"[DEFAULT_AGENT] 你是数据库助手。请使用工具 database_write_use / database_read_use / database_metadata_use 完成任务: <<input>>\n"
								+ "要求：写入前先用 database_read_use 做必要验证；执行写入使用 database_write_use(action=execute_write_sql)。\n"
								+ "完成后调用 terminate 返回结果。");
			}
			else if (lower.startsWith("ai_database_meta_agent")) {
				step.setStepRequirement(
						"[DEFAULT_AGENT] 你是数据库助手。请使用工具 database_metadata_use / database_read_use 完成任务: <<input>>\n"
								+ "要求：优先使用 datasourceName（如用户提供）。\n"
								+ "完成后调用 terminate 返回结果。");
			}
			else {
				step.setStepRequirement("[DEFAULT_AGENT] 使用可用工具完成任务: <<input>>");
			}
			step.setModelName("");
			step.setTerminateColumns("");

			// 默认开放最小工具集，避免引用不存在的工具
			List<String> defaultTools = new java.util.ArrayList<>(List.of("terminate"));
			// AI 数据库页面使用独立协调器 toolName，真正要开放的是 database_* 工具
			if (lower.startsWith("ai_database_read_agent")) {
				defaultTools.add(0, "database_metadata_use");
				defaultTools.add(0, "database_read_use");
			}
			else if (lower.startsWith("ai_database_write_agent")) {
				defaultTools.add(0, "database_metadata_use");
				defaultTools.add(0, "database_read_use");
				defaultTools.add(0, "database_write_use");
			}
			else if (lower.startsWith("ai_database_meta_agent")) {
				defaultTools.add(0, "database_read_use");
				defaultTools.add(0, "database_metadata_use");
			}
			if (lower.contains("browser")) {
				defaultTools.add(0, "browser_use");
			}
			step.setSelectedToolKeys(defaultTools);
			config.setSteps(List.of(step));

			PlanTemplateConfigVO.ToolConfigVO toolConfig = new PlanTemplateConfigVO.ToolConfigVO();
			// toolConfig.setToolName(toolName);
			toolConfig.setToolDescription(toolName);
			toolConfig.setEnableInternalToolcall(true);
			toolConfig.setEnableHttpService(true);
			toolConfig.setEnableInConversation(false);
			toolConfig.setPublishStatus("PUBLISHED");

			PlanTemplateConfigVO.InputSchemaParam inputParam = new PlanTemplateConfigVO.InputSchemaParam();
			inputParam.setName("input");
			inputParam.setDescription("任务描述");
			inputParam.setType("string");
			inputParam.setRequired(true);
			toolConfig.setInputSchema(List.of(inputParam));

			config.setToolConfig(toolConfig);

			planTemplateConfigService.createOrUpdateCoordinatorToolFromPlanTemplateConfig(config);
			logger.info("Created runtime plan template {} for tool {}", planTemplateId, toolName);
			return planTemplateId;
		}
		catch (Exception e) {
			logger.error("Failed to create runtime plan template for tool {}: {}", toolName, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Extract latest result from agent executions (prefers action result, then think
	 * output, then agent result).
	 */
	private String extractLatestResult(List<AgentExecutionRecord> agents) {
		if (agents == null || agents.isEmpty()) {
			return null;
		}
		String latest = null;
		for (AgentExecutionRecord agent : agents) {
			if (agent == null) {
				continue;
			}
			List<ThinkActRecord> steps = agent.getThinkActSteps();
			if (steps != null && !steps.isEmpty()) {
				for (int i = steps.size() - 1; i >= 0; i--) {
					ThinkActRecord step = steps.get(i);
					if (step == null) {
						continue;
					}
					if (StringUtils.hasText(step.getActionResult())) {
						return step.getActionResult();
					}
					if (StringUtils.hasText(step.getThinkOutput())) {
						return step.getThinkOutput();
					}
				}
			}
			if (StringUtils.hasText(agent.getResult())) {
				latest = agent.getResult();
			}
		}
		return latest;
	}

	/**
	 * Stream task status and execution details so the frontend can render live progress.
	 * @param request Body containing planId
	 * @return SSE emitter with periodic updates
	 */
	@PostMapping(value = "/taskStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamTask(@RequestBody Map<String, Object> request) {
		String planId = request != null ? (String) request.get("planId") : null;
		if (!StringUtils.hasText(planId)) {
			SseEmitter errorEmitter = new SseEmitter(5000L);
			try {
				Map<String, Object> error = Map.of("type", "error", "message", "planId is required");
				errorEmitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(error)));
				errorEmitter.complete();
			}
			catch (Exception e) {
				errorEmitter.completeWithError(e);
			}
			return errorEmitter;
		}

		SseEmitter emitter = new SseEmitter(300000L);
		emitter.onTimeout(() -> {
			logger.warn("taskStream timeout for planId {}", planId);
			emitter.complete();
		});
		emitter.onError((ex) -> logger.error("taskStream error for planId {}", planId, ex));

		CompletableFuture.runAsync(() -> {
			try {
				boolean finished = false;
				boolean hasProgress = false;
				int maxTicks = 300; // Align with 5 minute emitter timeout
				for (int i = 0; i < maxTicks && !finished; i++) {
					Map<String, Object> status = buildTaskStatus(planId);
					PlanExecutionRecord detail = buildPlanExecutionView(planId);

					boolean completed = detail != null && detail.isCompleted();
					boolean running = Boolean.TRUE.equals(status.get("isRunning"));
					boolean exists = Boolean.TRUE.equals(status.get("exists"));
					boolean stopRequested = status.get("desiredState") != null
							&& "STOP".equals(status.get("desiredState").toString());
					hasProgress = hasProgress || detail != null || exists || running;

					Map<String, Object> payload = new HashMap<>();
					payload.put("type", completed ? "done" : "update");
					payload.put("planId", planId);
					payload.put("status", status);
					payload.put("detail", detail);
					payload.put("completed", completed);
					payload.put("running", running);
					emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload)));

					finished = completed || (hasProgress && !running && (!exists || stopRequested));
					if (!finished) {
						Thread.sleep(1000L);
					}
				}
				emitter.complete();
			}
			catch (PlanException e) {
				logger.error("Plan exception while streaming task {}", planId, e);
				try {
					Map<String, Object> error = new HashMap<>();
					error.put("type", "error");
					error.put("planId", planId);
					error.put("message", e.getMessage());
					emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(error)));
				}
				catch (Exception ignored) {
					// ignore send errors
				}
				emitter.completeWithError(e);
			}
			catch (Exception e) {
				logger.error("Failed to stream task updates for planId: {}", planId, e);
				try {
					Map<String, Object> error = new HashMap<>();
					error.put("type", "error");
					error.put("planId", planId);
					error.put("message", e.getMessage());
					emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(error)));
				}
				catch (Exception ignored) {
					// ignore send errors
				}
				emitter.completeWithError(e);
			}
		});
		return emitter;
	}

	/**
	 * Validate or generate conversation ID. Generates for VUE_DIALOG and VUE_SIDEBAR
	 * requests. Internal calls (HTTP_REQUEST, subplans, cron tasks) should not generate
	 * conversationId. If enableConversationMemory is false, always generate a new
	 * conversationId.
	 * @param conversationId The conversation ID to validate (can be null)
	 * @param requestSource The request source to determine if conversationId should be
	 * generated
	 * @return Valid conversation ID (existing or newly generated for Vue requests)
	 */
	private String validateOrGenerateConversationId(String conversationId, RequestSource requestSource) {
		// If conversation memory is disabled, always generate a new conversationId
		if (lynxeProperties != null && !lynxeProperties.getEnableConversationMemory()) {
			if (requestSource == RequestSource.VUE_DIALOG || requestSource == RequestSource.VUE_SIDEBAR) {
				conversationId = memoryService.generateConversationId();
				logger.info("Conversation memory disabled, generated new conversation ID for {} request: {}",
						requestSource, conversationId);
				return conversationId;
			}
			// For HTTP_REQUEST and internal calls, do not generate conversationId (return
			// null)
			return null;
		}

		if (!StringUtils.hasText(conversationId)) {
			// Generate conversation ID for VUE_DIALOG and VUE_SIDEBAR requests
			// Both should use the same conversation memory
			if (requestSource == RequestSource.VUE_DIALOG || requestSource == RequestSource.VUE_SIDEBAR) {
				conversationId = memoryService.generateConversationId();
				logger.info("Generated new conversation ID for {} request: {}", requestSource, conversationId);
			}
			// For HTTP_REQUEST and internal calls, do not generate conversationId (return
			// null)
		}
		else {
			logger.debug("Using provided conversation ID: {} (source: {})", conversationId, requestSource);
		}
		return conversationId;
	}

	/**
	 * Build memory name from plan's step requirements Extracts step requirements and
	 * joins them with newlines
	 * @param plan The plan interface
	 * @return Formatted memory name from step requirements
	 */
	private String buildMemoryNameFromPlan(PlanInterface plan) {
		if (plan == null) {
			return "Untitled Conversation";
		}

		// Otherwise, build from step requirements
		List<ExecutionStep> steps = plan.getAllSteps();
		if (steps == null || steps.isEmpty()) {
			return "Empty Plan";
		}

		StringBuilder memoryName = new StringBuilder();
		for (int i = 0; i < steps.size(); i++) {
			ExecutionStep step = steps.get(i);
			if (step.getStepRequirement() != null && !step.getStepRequirement().trim().isEmpty()) {
				if (memoryName.length() > 0) {
					memoryName.append("\n");
				}
				// Clean up the step requirement (remove uploaded files info if present)
				String requirement = step.getStepRequirement();
				int uploadedFilesIndex = requirement.indexOf("[Uploaded files:");
				if (uploadedFilesIndex > 0) {
					requirement = requirement.substring(0, uploadedFilesIndex).trim();
				}
				memoryName.append(requirement);
			}
		}

		String result = memoryName.toString();
		if (result.isEmpty()) {
			return "Plan Execution";
		}

		// Limit length to avoid excessively long names
		if (result.length() > 30) {
			return result.substring(0, 30) + "...";
		}

		return result;
	}

	/**
	 * Build a brief system prompt from user's personal memories to prime the LLM.
	 * @param userId The user ID
	 * @return System prompt content
	 */
	private String buildUserProfile(Long userId) {
		try {
			var memories = userPersonalMemoryService.listByUser(userId);
			if (memories == null || memories.isEmpty()) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			sb.append("以下是该用户的长期记忆，请在回答中参考：\n");
			for (var mem : memories) {
				if (!StringUtils.hasText(mem.getMemoryKey())) {
					continue;
				}
				String content = extractContent(mem.getContentJson());
				if (!StringUtils.hasText(content)) {
					continue;
				}
				sb.append("- ").append(mem.getMemoryKey()).append(": ").append(content);
				if (StringUtils.hasText(mem.getTitle())) {
					sb.append(" (").append(mem.getTitle()).append(")");
				}
				sb.append("\n");
			}
			return sb.toString();
		}
		catch (Exception e) {
			logger.warn("Failed to build user profile for userId {}", userId, e);
			return null;
		}
	}

	/**
	 * Extract "content" from stored JSON; fallback to original if parsing fails.
	 */
	private String extractContent(String contentJson) {
		if (!StringUtils.hasText(contentJson)) {
			return "";
		}
		try {
			var node = objectMapper.readTree(contentJson);
			var contentNode = node.get("content");
			if (contentNode != null && contentNode.isTextual()) {
				return contentNode.asText();
			}
			// fallback
			return contentJson;
		}
		catch (Exception e) {
			return contentJson;
		}
	}

	/**
	 * Simple chat endpoint for standard LLM chat without plan execution with SSE
	 * streaming
	 * @param request Request containing input message, conversationId (optional),
	 * uploadedFiles (optional), uploadKey (optional)
	 * @return SSE stream with incremental text chunks
	 */
	@PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter chat(@RequestBody Map<String, Object> request) {
		String input = (String) request.get("input");
		if (input == null || input.trim().isEmpty()) {
			SseEmitter errorEmitter = new SseEmitter(5000L);
			try {
				Map<String, Object> errorData = new HashMap<>();
				errorData.put("type", "error");
				errorData.put("message", "Input message cannot be empty");
				errorEmitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorData)));
				errorEmitter.complete();
			}
			catch (Exception e) {
				errorEmitter.completeWithError(e);
			}
			return errorEmitter;
		}

		// Optional userId from frontend to support personal memory capture
		Long userId = userService.resolveUserId(request.get("userId"));
		if (userId == null) {
			userId = userService.currentUserId();
		}
		if (userId == null) {
			// Fallback to default user (ID 1) to avoid missing personal memory capture
			userId = 1L;
			logger.warn("userId not provided in chat request, fallback to default userId: {}", userId);
		}

		RequestSource requestSource = getRequestSource(request);
		logger.info("📡 [{}] Received chat streaming request", requestSource.name());

		// Create SSE emitter with 5 minute timeout
		SseEmitter emitter = new SseEmitter(300000L);
		StringBuilder accumulatedText = new StringBuilder();
		final SseEmitter finalEmitter = emitter;
		final StringBuilder finalAccumulatedText = accumulatedText;
		final Long resolvedUserId = userId;
		final RequestSource resolvedRequestSource = requestSource;
		final String resolvedInput = input;

		// Register timeout and error handlers before starting async task to avoid race
		// condition
		emitter.onTimeout(() -> {
			logger.warn("SSE emitter timeout");
			finalEmitter.complete();
		});

		emitter.onError((ex) -> {
			logger.error("SSE emitter error", ex);
			finalEmitter.completeWithError(ex);
		});

		// Execute asynchronously
		CompletableFuture.runAsync(() -> {
			// Ensure userId is bound to this async thread for the whole request lifecycle
			com.wangliang.agentj.user.context.UserContextHolder.setUserId(resolvedUserId);
			try {
				// Validate or generate conversationId
				String conversationId = validateOrGenerateConversationId((String) request.get("conversationId"),
						resolvedRequestSource);

				// Send initial event with conversationId
					Map<String, Object> startData = new HashMap<>();
					startData.put("type", "start");
					startData.put("conversationId", conversationId);
					finalEmitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(startData)));

				// Build message list with conversation history
				List<Message> messages = new java.util.ArrayList<>();

				// Prepend user personal memories as system prompt
				if (resolvedUserId != null && userPersonalMemoryService != null) {
					String profile = buildUserProfile(resolvedUserId);
					if (StringUtils.hasText(profile)) {
						messages.add(new SystemMessage(profile));
					}
				}

				// Retrieve conversation history if conversationId exists and conversation
				// memory is enabled
				if (lynxeProperties != null && lynxeProperties.getEnableConversationMemory() && memoryService != null
						&& conversationId != null && !conversationId.trim().isEmpty()) {
					try {
						org.springframework.ai.chat.memory.ChatMemory conversationMemory = llmService
							.getConversationMemoryWithLimit(lynxeProperties.getMaxMemory(), conversationId);
						List<Message> conversationHistory = conversationMemory.get(conversationId);
						if (conversationHistory != null && !conversationHistory.isEmpty()) {
							logger.debug("Adding {} conversation history messages for conversationId: {}",
									conversationHistory.size(), conversationId);
							messages.addAll(conversationHistory);
						}
					}
					catch (Exception e) {
						logger.warn(
								"Failed to retrieve conversation history for conversationId: {}. Continuing without it.",
								conversationId, e);
					}
				}

				// Add user message
				UserMessage userMessage = new UserMessage(resolvedInput);
				messages.add(userMessage);

				// Save user message to conversation memory
				if (lynxeProperties != null && lynxeProperties.getEnableConversationMemory() && conversationId != null
						&& !conversationId.trim().isEmpty()) {
					try {
						llmService.addToConversationMemoryWithLimit(lynxeProperties.getMaxMemory(), conversationId,
								userMessage, resolvedUserId);
						logger.debug("Saved user message to conversation memory for conversationId: {}",
								conversationId);
					}
					catch (Exception e) {
						logger.warn("Failed to save user message to conversation memory for conversationId: {}",
								conversationId, e);
					}
				}

				// Call LLM with simple chat (no tools, no plan execution)
				ChatClient chatClient = llmService.getDiaChatClient();
				Prompt prompt = new Prompt(messages);

				// Calculate input character count
				int inputCharCount = messages.stream().mapToInt(message -> {
					String text = message.getText();
					return (text != null && !text.trim().isEmpty()) ? text.length() : 0;
				}).sum();
				logger.info("Chat input character count: {}", inputCharCount);

				// Prepare effectively-final references for lambdas
				final String finalConversationId = conversationId;

				// Process streaming response and send chunks as they arrive
				ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt(prompt);
				Flux<ChatResponse> responseFlux = requestSpec.stream().chatResponse();

				// Subscribe to flux and send chunks via SSE
				responseFlux.doOnNext(chatResponse -> {
					try {
						if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
							String text = chatResponse.getResult().getOutput().getText();
							if (text != null && !text.isEmpty()) {
								finalAccumulatedText.append(text);

								// Send chunk event
								Map<String, Object> chunkData = new HashMap<>();
								chunkData.put("type", "chunk");
								chunkData.put("content", text);
								finalEmitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(chunkData)));
							}
						}
					}
					catch (Exception e) {
						logger.error("Error sending SSE chunk", e);
					}
				}).doOnComplete(() -> {
					try {
						String finalText = finalAccumulatedText.toString();
						if (finalText == null || finalText.trim().isEmpty()) {
							finalText = "No response generated";
						}

						// Save assistant response to conversation memory
						if (lynxeProperties != null && lynxeProperties.getEnableConversationMemory()
								&& finalConversationId != null && !finalConversationId.trim().isEmpty()) {
							try {
								AssistantMessage assistantMessage = new AssistantMessage(finalText);
								llmService.addToConversationMemoryWithLimit(lynxeProperties.getMaxMemory(),
										finalConversationId, assistantMessage, resolvedUserId);
								logger.debug("Saved assistant response to conversation memory for conversationId: {}",
										finalConversationId);
							}
							catch (Exception e) {
								logger.warn(
										"Failed to save assistant response to conversation memory for conversationId: {}",
										finalConversationId, e);
							}
						}

						// Send completion event
						Map<String, Object> doneData = new HashMap<>();
						doneData.put("type", "done");
						finalEmitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(doneData)));
						finalEmitter.complete();

						logger.info("Chat streaming completed for conversationId: {}, response length: {}",
								finalConversationId, finalText.length());
					}
					catch (Exception e) {
						logger.error("Error completing SSE stream", e);
						finalEmitter.completeWithError(e);
					}
				}).doOnError(error -> {
					logger.error("Error in chat streaming", error);
					try {
						Map<String, Object> errorData = new HashMap<>();
						errorData.put("type", "error");
						errorData.put("message",
								error.getMessage() != null ? error.getMessage() : "Streaming error occurred");
						finalEmitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorData)));
						finalEmitter.completeWithError(error);
					}
					catch (Exception e) {
						finalEmitter.completeWithError(e);
					}
				}).doFinally(signalType -> {
					// Prevent ThreadLocal leakage
					com.wangliang.agentj.user.context.UserContextHolder.clear();
				}).subscribe();

			}
			catch (Exception e) {
				logger.error("Failed to process chat streaming request", e);
				com.wangliang.agentj.user.context.UserContextHolder.clear();
				try {
					Map<String, Object> errorData = new HashMap<>();
					errorData.put("type", "error");
					errorData.put("message", "Failed to process chat request: " + e.getMessage());
					emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorData)));
					emitter.completeWithError(e);
				}
				catch (Exception ex) {
					emitter.completeWithError(ex);
				}
			}
		});

		return emitter;
	}

}
