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

package com.wangliang.agentj.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.agent.ToolCallbackProvider;
import com.wangliang.agentj.config.LynxeProperties;
import com.wangliang.agentj.conversation.service.MemoryService;
import com.wangliang.agentj.cron.service.CronService;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.llm.StreamingResponseHandler;
import com.wangliang.agentj.mcp.model.vo.McpServiceEntity;
import com.wangliang.agentj.mcp.model.vo.McpTool;
import com.wangliang.agentj.mcp.service.McpService;
import com.wangliang.agentj.planning.service.PlanFinalizer;
import com.wangliang.agentj.recorder.service.PlanExecutionRecorder;
import com.wangliang.agentj.runtime.executor.ImageRecognitionExecutorPool;
import com.wangliang.agentj.runtime.executor.LevelBasedExecutorPool;
import com.wangliang.agentj.runtime.service.PlanIdDispatcher;
import com.wangliang.agentj.runtime.service.ServiceGroupIndexService;
import com.wangliang.agentj.runtime.service.TaskInterruptionManager;
import com.wangliang.agentj.subplan.service.SubplanToolService;
import com.wangliang.agentj.tools.DebugTool;
import com.wangliang.agentj.tools.FormInputTool;
import com.wangliang.agentj.tools.TerminateTool;
import com.wangliang.agentj.tools.ToolCallBiFunctionDef;
import com.wangliang.agentj.tools.browser.BrowserUseTool;
import com.wangliang.agentj.tools.browser.ChromeDriverService;
import com.wangliang.agentj.tools.code.ToolExecuteResult;
import com.wangliang.agentj.tools.convertToMarkdown.ImageOcrProcessor;
import com.wangliang.agentj.tools.convertToMarkdown.MarkdownConverterTool;
import com.wangliang.agentj.tools.convertToMarkdown.PdfOcrProcessor;
import com.wangliang.agentj.tools.cron.CronTool;
import com.wangliang.agentj.tools.database.*;
import com.wangliang.agentj.tools.dirOperator.DirectoryOperator;
import com.wangliang.agentj.tools.excelProcessor.IExcelProcessingService;
import com.wangliang.agentj.tools.filesystem.UnifiedDirectoryManager;
import com.wangliang.agentj.tools.i18n.ToolI18nService;
import com.wangliang.agentj.tools.innerStorage.SmartContentSavingService;
import com.wangliang.agentj.tools.jsxGenerator.JsxGeneratorOperator;
import com.wangliang.agentj.tools.mapreduce.FileBasedParallelExecutionTool;
import com.wangliang.agentj.tools.mapreduce.FileSplitterTool;
import com.wangliang.agentj.tools.mapreduce.ParallelExecutionService;
import com.wangliang.agentj.tools.mapreduce.ParallelExecutionTool;
import com.wangliang.agentj.tools.pptGenerator.PptGeneratorOperator;
import com.wangliang.agentj.tools.shortUrl.ShortUrlService;
import com.wangliang.agentj.tools.tableProcessor.TableProcessingService;
import com.wangliang.agentj.tools.textOperator.FileImportOperator;
import com.wangliang.agentj.tools.textOperator.TextFileService;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Service
public class PlanningFactory {

	private final ChromeDriverService chromeDriverService;

	private final PlanExecutionRecorder recorder;

	private final LynxeProperties lynxeProperties;

	private final TextFileService textFileService;

	private final SmartContentSavingService innerStorageService;

	private final UnifiedDirectoryManager unifiedDirectoryManager;

	private final DataSourceService dataSourceService;

	// private final TableProcessingService tableProcessingService; // Currently unused -
	// commented out for future use

	private final IExcelProcessingService excelProcessingService;

	private final static Logger log = LoggerFactory.getLogger(PlanningFactory.class);

	private final McpService mcpService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	@Lazy
	private LlmService llmService;

	@Autowired
	@Lazy
	private ToolCallingManager toolCallingManager;

	@Autowired
	private StreamingResponseHandler streamingResponseHandler;

	@Autowired
	@Lazy
	private CronService cronService;

	@Autowired
	private SubplanToolService subplanToolService;

	@Autowired
	@Lazy
	private TaskInterruptionManager taskInterruptionManager;

	@SuppressWarnings("unused")
	@Autowired
	private PptGeneratorOperator pptGeneratorOperator;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Value("${agent.init}")
	private Boolean agentInit = true;

	@SuppressWarnings("unused")
	@Autowired
	private JsxGeneratorOperator jsxGeneratorOperator;

	@SuppressWarnings("unused")
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private MemoryService memoryService;

	@Autowired(required = false)
	private LevelBasedExecutorPool levelBasedExecutorPool;

	@Autowired
	private ShortUrlService shortUrlService;

	@Autowired
	private ServiceGroupIndexService serviceGroupIndexService;

	@Autowired
	private ParallelExecutionService parallelExecutionService;

	@Autowired
	private ToolI18nService toolI18nService;

	public PlanningFactory(ChromeDriverService chromeDriverService, PlanExecutionRecorder recorder,
			LynxeProperties lynxeProperties, TextFileService textFileService, McpService mcpService,
			SmartContentSavingService innerStorageService, UnifiedDirectoryManager unifiedDirectoryManager,
			DataSourceService dataSourceService, TableProcessingService tableProcessingService,
			IExcelProcessingService excelProcessingService) {
		this.chromeDriverService = chromeDriverService;
		this.recorder = recorder;
		this.lynxeProperties = lynxeProperties;
		this.textFileService = textFileService;
		this.mcpService = mcpService;
		this.innerStorageService = innerStorageService;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.dataSourceService = dataSourceService;
		// this.tableProcessingService = tableProcessingService; // Currently unused
		this.excelProcessingService = excelProcessingService;
	}

	/**
	 * Create a PlanFinalizer instance
	 * @return configured PlanFinalizer instance
	 */
	public PlanFinalizer createPlanFinalizer() {
		return new PlanFinalizer(llmService, recorder, lynxeProperties, streamingResponseHandler,
				taskInterruptionManager, memoryService);
	}

	public static class ToolCallBackContext {

		private final ToolCallback toolCallback;

		private final ToolCallBiFunctionDef<?> functionInstance;

		public ToolCallBackContext(ToolCallback toolCallback, ToolCallBiFunctionDef<?> functionInstance) {
			this.toolCallback = toolCallback;
			this.functionInstance = functionInstance;
		}

		public ToolCallback getToolCallback() {
			return toolCallback;
		}

		public ToolCallBiFunctionDef<?> getFunctionInstance() {
			return functionInstance;
		}

	}

	public Map<String, ToolCallBackContext> toolCallbackMap(String planId, String rootPlanId,
			String expectedReturnInfo) {

		Map<String, ToolCallBackContext> toolCallbackMap = new HashMap<>();
		List<ToolCallBiFunctionDef<?>> toolDefinitions = new ArrayList<>();
		if (chromeDriverService == null) {
			log.error("ChromeDriverService is null, skipping BrowserUseTool registration");
			return toolCallbackMap;
		}
		if (innerStorageService == null) {
			log.error("SmartContentSavingService is null, skipping BrowserUseTool registration");
			return toolCallbackMap;
		}
		if (agentInit) {
			// Add all tool definitions
			toolDefinitions.add(BrowserUseTool.getInstance(chromeDriverService, innerStorageService, objectMapper,
					shortUrlService, textFileService, toolI18nService));
			toolDefinitions.add(DatabaseReadTool.getInstance(dataSourceService, objectMapper, unifiedDirectoryManager,
					toolI18nService));
			toolDefinitions.add(DatabaseWriteTool.getInstance(dataSourceService, objectMapper, toolI18nService));
			toolDefinitions.add(DatabaseMetadataTool.getInstance(dataSourceService, objectMapper, toolI18nService));
			toolDefinitions.add(DatabaseTableToExcelTool.getInstance(lynxeProperties, dataSourceService,
					excelProcessingService, unifiedDirectoryManager, toolI18nService));
			toolDefinitions.add(UuidGenerateTool.getInstance(objectMapper, toolI18nService));
			toolDefinitions.add(new TerminateTool(planId, expectedReturnInfo, objectMapper, shortUrlService,
					lynxeProperties, toolI18nService));
			toolDefinitions.add(new DebugTool(toolI18nService));
            toolDefinitions.add(DatabaseMetadataTool.getInstance(dataSourceService, objectMapper, toolI18nService));
            toolDefinitions.add(DatabaseTableToExcelTool.getInstance(lynxeProperties, dataSourceService,
                    excelProcessingService, unifiedDirectoryManager, toolI18nService));
			toolDefinitions.add(new FileImportOperator(textFileService, null, toolI18nService));
			toolDefinitions.add(new FileSplitterTool(textFileService, objectMapper, toolI18nService));
			toolDefinitions.add(new DirectoryOperator(unifiedDirectoryManager, objectMapper, toolI18nService));
			// toolDefinitions.add(new UploadedFileLoaderTool(unifiedDirectoryManager,
			// applicationContext));
			// toolDefinitions.add(new TableProcessorTool(tableProcessingService));
			// toolDefinitions.add(pptGeneratorOperator);
			// toolDefinitions.add(jsxGeneratorOperator);
			// toolDefinitions.add(new FileMergeTool(unifiedDirectoryManager));
			// toolDefinitions.add(new GoogleSearch());
			// toolDefinitions.add(new PythonExecute());
			toolDefinitions.add(new FormInputTool(objectMapper, toolI18nService));
			toolDefinitions.add(new ParallelExecutionTool(objectMapper, toolCallbackMap, planIdDispatcher,
					levelBasedExecutorPool, toolI18nService, serviceGroupIndexService, parallelExecutionService));
			toolDefinitions.add(new FileBasedParallelExecutionTool(objectMapper, toolCallbackMap,
					unifiedDirectoryManager, parallelExecutionService, toolI18nService));
			toolDefinitions.add(new CronTool(cronService, objectMapper, toolI18nService));
			toolDefinitions.add(new MarkdownConverterTool(unifiedDirectoryManager,
					new PdfOcrProcessor(unifiedDirectoryManager, llmService, lynxeProperties,
							new ImageRecognitionExecutorPool(lynxeProperties)),
					new ImageOcrProcessor(unifiedDirectoryManager, llmService, lynxeProperties,
							new ImageRecognitionExecutorPool(lynxeProperties)),
					excelProcessingService, objectMapper, toolI18nService));
			// toolDefinitions.add(new ExcelProcessorTool(excelProcessingService));
		}
		else {
			toolDefinitions.add(new TerminateTool(planId, expectedReturnInfo, objectMapper, shortUrlService,
					lynxeProperties, toolI18nService));
		}

		List<McpServiceEntity> functionCallbacks = mcpService.getFunctionCallbacks(planId);
		for (McpServiceEntity toolCallback : functionCallbacks) {
			String serviceGroup = toolCallback.getServiceGroup();
			ToolCallback[] tCallbacks = toolCallback.getAsyncMcpToolCallbackProvider().getToolCallbacks();
            for (ToolCallback tCallback : tCallbacks) {
                // The serviceGroup is the name of the tool
                toolDefinitions.add(new McpTool(tCallback, serviceGroup, planId, innerStorageService, objectMapper));
            }
		}
		// Create FunctionToolCallback for each tool
		for (ToolCallBiFunctionDef<?> toolDefinition : toolDefinitions) {

			try {
				toolDefinition.setCurrentPlanId(planId);
				toolDefinition.setRootPlanId(rootPlanId);

				// Use qualified key format: toolName[index]
				String serviceGroup = toolDefinition.getServiceGroup();
				String toolName = toolDefinition.getName();
				String qualifiedKey;

				if (serviceGroup != null && !serviceGroup.isEmpty()) {
					// Get or assign index for this serviceGroup using the service
					Integer index = serviceGroupIndexService.getOrAssignIndex(serviceGroup);
					if (index != null) {
						qualifiedKey = toolName + "*" + index + "*";
					}
					else {
						qualifiedKey = toolName;
					}
				}
				else {
					qualifiedKey = toolName;
				}

				// Build FunctionToolCallback with qualified name so LLM calls tools with
				// qualified names
				FunctionToolCallback<?, ToolExecuteResult> functionToolcallback = FunctionToolCallback
					.builder(qualifiedKey, toolDefinition)
					.description(toolDefinition.getDescriptionWithServiceGroup())
					.inputSchema(toolDefinition.getParameters())
					.inputType(toolDefinition.getInputType())
					.toolMetadata(ToolMetadata.builder().returnDirect(toolDefinition.isReturnDirect()).build())
					.build();

				log.info("Registering tool: {} with qualified key: {}", toolName, qualifiedKey);
				ToolCallBackContext functionToolcallbackContext = new ToolCallBackContext(functionToolcallback,
						toolDefinition);
				toolCallbackMap.put(qualifiedKey, functionToolcallbackContext);
			}
			catch (Exception e) {
				log.error("Failed to register tool: {} - {}", toolDefinition.getName(), e.getMessage(), e);
			}
		}

		// Add subplan tool registration
		if (subplanToolService != null) {
			try {
				Map<String, ToolCallBackContext> subplanToolCallbacks = subplanToolService
					.createSubplanToolCallbacks(planId, rootPlanId, expectedReturnInfo, serviceGroupIndexService);
				toolCallbackMap.putAll(subplanToolCallbacks);
				log.info("Registered {} subplan tools", subplanToolCallbacks.size());
			}
			catch (Exception e) {
				log.warn("Failed to register subplan tools: {}", e.getMessage());
			}
		}

		return toolCallbackMap;
	}

	@SuppressWarnings("deprecation")
	@Bean
	public RestClient.Builder createRestClient() {
		// Create RequestConfig and set the timeout (10 minutes for all timeouts)
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.of(10, TimeUnit.MINUTES)) // Set the connection
																	// timeout
			.setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.build();

		// Create CloseableHttpClient and apply the configuration
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

		// Use HttpComponentsClientHttpRequestFactory to wrap HttpClient
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		// Create RestClient and set the request factory
		return RestClient.builder().requestFactory(requestFactory);
	}

	/**
	 * Provides an empty ToolCallbackProvider implementation when MCP is disabled
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.mcp.client.enabled", havingValue = "false")
	public ToolCallbackProvider emptyToolCallbackProvider() {
		return () -> new HashMap<String, ToolCallBackContext>();
	}

}
