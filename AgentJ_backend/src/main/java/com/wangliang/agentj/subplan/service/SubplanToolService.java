package com.wangliang.agentj.subplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.planning.PlanningFactory;
import com.wangliang.agentj.planning.model.po.FuncAgentToolEntity;
import com.wangliang.agentj.planning.model.vo.PlanTemplateConfigVO;
import com.wangliang.agentj.planning.repository.FuncAgentToolRepository;
import com.wangliang.agentj.planning.service.IPlanParameterMappingService;
import com.wangliang.agentj.planning.service.PlanTemplateConfigService;
import com.wangliang.agentj.planning.service.PlanTemplateService;
import com.wangliang.agentj.runtime.service.PlanIdDispatcher;
import com.wangliang.agentj.runtime.service.PlanningCoordinator;
import com.wangliang.agentj.runtime.service.ServiceGroupIndexService;
import com.wangliang.agentj.runtime.util.ToolNameSanitizer;
import com.wangliang.agentj.subplan.model.vo.SubplanToolWrapper;
import com.wangliang.agentj.tools.code.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for managing subplan tools
 *
 * Integrates with the existing PlanningFactory tool registry system
 */
@Service
@Transactional
public class SubplanToolService {

	private static final Logger logger = LoggerFactory.getLogger(SubplanToolService.class);

	@Autowired
	private FuncAgentToolRepository funcAgentToolRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private PlanTemplateConfigService planTemplateConfigService;

	@Autowired
	@Lazy
	private PlanningCoordinator planningCoordinator;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private IPlanParameterMappingService parameterMappingService;

	public List<FuncAgentToolEntity> getAllSubplanTools() {
		logger.debug("Fetching all coordinator tools from database");
		return funcAgentToolRepository.findAll();
	}

	public Optional<FuncAgentToolEntity> getSubplanToolByTemplate(String planTemplateId) {
		logger.debug("Fetching coordinator tool for template: {}", planTemplateId);
		List<FuncAgentToolEntity> tools = funcAgentToolRepository.findByPlanTemplateId(planTemplateId);
		return tools.isEmpty() ? Optional.empty() : Optional.of(tools.get(0));
	}

	public Map<String, PlanningFactory.ToolCallBackContext> createSubplanToolCallbacks(String planId, String rootPlanId,
                                                                                       String expectedReturnInfo, ServiceGroupIndexService serviceGroupIndexService) {

		logger.info("Creating subplan tool callbacks for planId: {}, rootPlanId: {}", planId, rootPlanId);

		Map<String, PlanningFactory.ToolCallBackContext> toolCallbackMap = new HashMap<>();

		try {
			// Get all coordinator tools from database, filter by enableInternalToolcall =
			// true
			List<FuncAgentToolEntity> coordinatorTools = funcAgentToolRepository.findAll()
				.stream()
				.filter(tool -> tool.getEnableInternalToolcall() != null && tool.getEnableInternalToolcall())
				.collect(java.util.stream.Collectors.toList());

			if (coordinatorTools.isEmpty()) {
				logger.info("No coordinator tools with enableInternalToolcall=true found in database");
				return toolCallbackMap;
			}

			logger.info("Found {} coordinator tools to register", coordinatorTools.size());

			for (FuncAgentToolEntity coordinatorTool : coordinatorTools) {

				// Get plan template config VO for this coordinator tool
				String planTemplateId = coordinatorTool.getPlanTemplateId();
				Optional<PlanTemplateConfigVO> planTemplateOpt = planTemplateConfigService
					.getPlanTemplate(planTemplateId);
				if (planTemplateOpt.isEmpty()) {
					logger.info("PlanTemplate not found for planTemplateId: {}, skipping tool registration",
							planTemplateId);
					continue;
				}
				PlanTemplateConfigVO planTemplateConfig = planTemplateOpt.get();

				// Get coordinator tool config VO
				Optional<PlanTemplateConfigVO> coordinatorToolOpt = planTemplateConfigService
					.getCoordinatorToolByPlanTemplateId(planTemplateId);
				if (coordinatorToolOpt.isEmpty()) {
					logger.info("Coordinator tool config not found for planTemplateId: {}, skipping tool registration",
							planTemplateId);
					continue;
				}
				PlanTemplateConfigVO coordinatorToolConfig = coordinatorToolOpt.get();

				String toolName = planTemplateConfig.getTitle() != null ? planTemplateConfig.getTitle()
						: planTemplateId;
				try {
					// Create a SubplanToolWrapper that extends AbstractBaseTool
					SubplanToolWrapper toolWrapper = new SubplanToolWrapper(coordinatorToolConfig, planTemplateConfig,
							planId, rootPlanId, planTemplateService, planningCoordinator, planIdDispatcher,
							objectMapper, parameterMappingService);

					// Get tool name from wrapper (uses PlanTemplateConfigVO title)
					String rawToolName = toolWrapper.getName();
					toolName = ToolNameSanitizer.sanitize(rawToolName);
					if (!toolName.equals(rawToolName)) {
						logger.warn("Coordinator tool name sanitized from '{}' to '{}' to satisfy provider constraints",
								rawToolName, toolName);
					}

					// Get description from coordinator tool config
					String description = "";
					if (coordinatorToolConfig.getToolConfig() != null) {
						description = coordinatorToolConfig.getToolConfig().getToolDescription();
					}

					// Use qualified key format: toolName__index (consistent with
					// PlanningFactory)
					String serviceGroup = coordinatorTool.getServiceGroup();
					String qualifiedKey;

					if (serviceGroup != null && !serviceGroup.isEmpty()) {
						// Get or assign index for this serviceGroup using the service
						Integer index = serviceGroupIndexService.getOrAssignIndex(serviceGroup);
						if (index != null) {
							qualifiedKey = toolName + "__" + index;
						}
						else {
							qualifiedKey = toolName;
						}
					}
					else {
						qualifiedKey = toolName;
					}

					// Create FunctionToolCallback with qualified name so LLM calls tools
					// with qualified names
					FunctionToolCallback<Map<String, Object>, ToolExecuteResult> functionToolCallback = FunctionToolCallback
						.builder(qualifiedKey, toolWrapper)
						.description(description)
						.inputSchema(toolWrapper.getParameters())
						.inputType(Map.class) // Map input type for coordinator tools
						.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
						.build();

					// Create ToolCallBackContext
					PlanningFactory.ToolCallBackContext context = new PlanningFactory.ToolCallBackContext(
							functionToolCallback, toolWrapper);

					toolCallbackMap.put(qualifiedKey, context);

					logger.info("Successfully registered coordinator tool: {} with qualified key: {} -> {}", toolName,
							qualifiedKey, planTemplateId);

				}
				catch (Exception e) {
					logger.error("Failed to register coordinator tool for planTemplateId: {}", planTemplateId, e);
				}
			}

		}
		catch (Exception e) {
			logger.error("Error creating coordinator tool callbacks", e);
		}

		logger.info("Created {} coordinator tool callbacks", toolCallbackMap.size());
		return toolCallbackMap;
	}

}
