package com.wangliang.agentj.runtime.executor.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.config.AgentJProperties;
import com.wangliang.agentj.conversation.service.MemoryService;
import com.wangliang.agentj.event.AgentJEventPublisher;
import com.wangliang.agentj.llm.ConversationMemoryLimitService;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.llm.StreamingResponseHandler;
import com.wangliang.agentj.model.repository.DynamicModelRepository;
import com.wangliang.agentj.planning.PlanningFactory;
import com.wangliang.agentj.recorder.service.PlanExecutionRecorder;
import com.wangliang.agentj.runtime.entity.vo.PlanInterface;
import com.wangliang.agentj.runtime.executor.DynamicToolPlanExecutor;
import com.wangliang.agentj.runtime.executor.LevelBasedExecutorPool;
import com.wangliang.agentj.runtime.executor.PlanExecutorInterface;
import com.wangliang.agentj.runtime.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Plan Executor Factory - Creates appropriate executor based on plan type Factory class
 * that selects the appropriate PlanExecutor implementation based on the planType from
 * PlanInterface
 */
@Component
public class PlanExecutorFactory implements IPlanExecutorFactory {

	private static final Logger log = LoggerFactory.getLogger(PlanExecutorFactory.class);

	private final LlmService llmService;

	private final PlanExecutionRecorder recorder;

	private final AgentJProperties agentjProperties;

	private final ObjectMapper objectMapper;

	private final LevelBasedExecutorPool levelBasedExecutorPool;

	private final DynamicModelRepository dynamicModelRepository;

	private final FileUploadService fileUploadService;

	private final AgentInterruptionHelper agentInterruptionHelper;

	private final PlanningFactory planningFactory;

	private final ToolCallingManager toolCallingManager;

	private final UserInputService userInputService;

	private final StreamingResponseHandler streamingResponseHandler;

	private final PlanIdDispatcher planIdDispatcher;

	private final AgentJEventPublisher agentjEventPublisher;

	private final ParallelToolExecutionService parallelToolExecutionService;

	private final MemoryService memoryService;

	private final ConversationMemoryLimitService conversationMemoryLimitService;

	private final ServiceGroupIndexService serviceGroupIndexService;

	public PlanExecutorFactory(LlmService llmService, PlanExecutionRecorder recorder, AgentJProperties agentjProperties,
			ObjectMapper objectMapper, LevelBasedExecutorPool levelBasedExecutorPool,
			DynamicModelRepository dynamicModelRepository, FileUploadService fileUploadService,
			AgentInterruptionHelper agentInterruptionHelper, PlanningFactory planningFactory,
			ToolCallingManager toolCallingManager, UserInputService userInputService,
			StreamingResponseHandler streamingResponseHandler, PlanIdDispatcher planIdDispatcher,
			AgentJEventPublisher agentjEventPublisher, ParallelToolExecutionService parallelToolExecutionService,
			MemoryService memoryService, ConversationMemoryLimitService conversationMemoryLimitService,
			ServiceGroupIndexService serviceGroupIndexService) {
		this.llmService = llmService;
		this.recorder = recorder;
		this.agentjProperties = agentjProperties;
		this.objectMapper = objectMapper;
		this.levelBasedExecutorPool = levelBasedExecutorPool;
		this.dynamicModelRepository = dynamicModelRepository;
		this.fileUploadService = fileUploadService;
		this.agentInterruptionHelper = agentInterruptionHelper;
		this.planningFactory = planningFactory;
		this.toolCallingManager = toolCallingManager;
		this.userInputService = userInputService;
		this.streamingResponseHandler = streamingResponseHandler;
		this.planIdDispatcher = planIdDispatcher;
		this.agentjEventPublisher = agentjEventPublisher;
		this.parallelToolExecutionService = parallelToolExecutionService;
		this.memoryService = memoryService;
		this.conversationMemoryLimitService = conversationMemoryLimitService;
		this.serviceGroupIndexService = serviceGroupIndexService;
	}

	/**
	 * Create a dynamic agent plan executor for DynamicToolsAgent execution
	 * @return DynamicAgentPlanExecutor instance for dynamic agent plans
	 */
	private PlanExecutorInterface createDynamicToolExecutor() {
		log.debug("Creating dynamic agent plan executor");
		return new DynamicToolPlanExecutor(null, recorder, llmService, agentjProperties, levelBasedExecutorPool,
				dynamicModelRepository, fileUploadService, agentInterruptionHelper, planningFactory, toolCallingManager,
				userInputService, streamingResponseHandler, planIdDispatcher, agentjEventPublisher, objectMapper,
				parallelToolExecutionService, memoryService, conversationMemoryLimitService, serviceGroupIndexService);
	}

	/**
	 * Get supported plan types
	 * @return Array of supported plan type strings
	 */
	public String[] getSupportedPlanTypes() {
		return new String[] { "dynamic_agent" };
	}

	/**
	 * Check if a plan type is supported
	 * @param planType The plan type to check
	 * @return true if the plan type is supported, false otherwise
	 */
	public boolean isPlanTypeSupported(String planType) {
		if (planType == null) {
			return false;
		}
		String normalizedType = planType.toLowerCase();
		return "simple".equals(normalizedType) || "direct".equals(normalizedType)
				|| "dynamic_agent".equals(normalizedType);
	}

	/**
	 * Create the appropriate executor based on plan type
	 * @param plan The execution plan containing type information
	 * @return The appropriate PlanExecutorInterface implementation
	 * @throws IllegalArgumentException if plan type is not supported
	 */
	public PlanExecutorInterface createExecutor(PlanInterface plan) {
		if (plan == null) {
			throw new IllegalArgumentException("Plan cannot be null");
		}

		String planType = plan.getPlanType();
		if (planType == null || planType.trim().isEmpty()) {
			throw new IllegalArgumentException("Plan type is null or empty");
		}

		log.info("Creating executor for plan type: {} (planId: {})", planType, plan.getCurrentPlanId());

		return switch (planType.toLowerCase()) {
			case "dynamic_agent" -> createDynamicToolExecutor();
			default -> {
				log.warn("Unknown plan type: {}, defaulting to dynamic agent executor", planType);
				yield createDynamicToolExecutor();
			}
		};
	}

}
