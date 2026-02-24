package com.wangliang.agentj.config;

import com.wangliang.agentj.config.entity.ConfigInputType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

@Component
@ConfigurationProperties(prefix = "agentj")
public class AgentJProperties {

	@Lazy
	@Autowired
	private IConfigService configService;

	@Autowired
	private Environment environment;

	// Browser Settings
	// Begin-------------------------------------------------------------------------------------------

	@ConfigProperty(group = "agentj", subGroup = "browser", key = "headless", path = "agentj.browser.headless",
			description = "agentj.browser.headless.description", defaultValue = "false",
			inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "agentj.browser.headless.option.true"),
					@ConfigOption(value = "false", label = "agentj.browser.headless.option.false") })
	private volatile Boolean browserHeadless;

	public Boolean getBrowserHeadless() {
		String configPath = "agentj.browser.headless";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			browserHeadless = Boolean.valueOf(value);
		}
		return browserHeadless;
	}

	public void setBrowserHeadless(Boolean browserHeadless) {
		this.browserHeadless = browserHeadless;
	}

	@ConfigProperty(group = "agentj", subGroup = "browser", key = "requestTimeout",
			path = "agentj.browser.requestTimeout", description = "agentj.browser.requestTimeout.description",
			defaultValue = "180", inputType = ConfigInputType.NUMBER)
	private volatile Integer browserRequestTimeout;

	public Integer getBrowserRequestTimeout() {
		String configPath = "agentj.browser.requestTimeout";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			browserRequestTimeout = Integer.valueOf(value);
		}
		return browserRequestTimeout;
	}

	public void setBrowserRequestTimeout(Integer browserRequestTimeout) {
		this.browserRequestTimeout = browserRequestTimeout;
	}

	@ConfigProperty(group = "agentj", subGroup = "general", key = "debugDetail", path = "agentj.general.debugDetail",
			description = "agentj.general.debugDetail.description", defaultValue = "false",
			inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "agentj.general.debugDetail.option.true"),
					@ConfigOption(value = "false", label = "agentj.general.debugDetail.option.false") })
	private volatile Boolean debugDetail;

	public Boolean getDebugDetail() {
		String configPath = "agentj.general.debugDetail";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			debugDetail = Boolean.valueOf(value);
		}
		return debugDetail;
	}

	public void setDebugDetail(Boolean debugDetail) {
		this.debugDetail = debugDetail;
	}

	// Browser Settings
	// End---------------------------------------------------------------------------------------------

	// General Settings
	// Begin---------------------------------------------------------------------------------------
	@ConfigProperty(group = "agentj", subGroup = "general", key = "openBrowser", path = "agentj.general.openBrowser",
			description = "agentj.general.openBrowser.description", defaultValue = "true",
			inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "agentj.general.openBrowser.option.true"),
					@ConfigOption(value = "false", label = "agentj.general.openBrowser.option.false") })
	@Value("${agentj.general.openBrowser:#{null}}")
	private volatile Boolean openBrowserAuto;

	public Boolean getOpenBrowserAuto() {
		// 如果配置文件/环境变量显式指定，优先使用
		if (environment.containsProperty("agentj.general.openBrowser")) {
			return openBrowserAuto;
		}

		String configPath = "agentj.general.openBrowser";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			openBrowserAuto = Boolean.valueOf(value);
		}
		// 配置中心也无值时，采用默认 true
		if (openBrowserAuto == null) {
			openBrowserAuto = true;
		}
		return openBrowserAuto;
	}

	public void setOpenBrowserAuto(Boolean openBrowserAuto) {
		this.openBrowserAuto = openBrowserAuto;
	}

	@ConfigProperty(group = "agentj", subGroup = "browser", key = "enableShortUrl",
			path = "agentj.browser.enableShortUrl", description = "agentj.browser.enableShortUrl.description",
			defaultValue = "true", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "agentj.browser.enableShortUrl.option.true"),
					@ConfigOption(value = "false", label = "agentj.browser.enableShortUrl.option.false") })
	private volatile Boolean enableShortUrl;

	public Boolean getEnableShortUrl() {
		String configPath = "agentj.browser.enableShortUrl";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			enableShortUrl = Boolean.valueOf(value);
		}
		// Default to true if not configured
		if (enableShortUrl == null) {
			enableShortUrl = true;
		}
		return enableShortUrl;
	}

	public void setEnableShortUrl(Boolean enableShortUrl) {
		this.enableShortUrl = enableShortUrl;
	}

	// General Settings
	// End-----------------------------------------------------------------------------------------

	// Agent Settings
	// Begin---------------------------------------------------------------------------------------------

	@ConfigProperty(group = "agentj", subGroup = "agent", key = "maxSteps", path = "agentj.maxSteps",
			description = "agentj.agent.maxSteps.description", defaultValue = "30", inputType = ConfigInputType.NUMBER)
	private volatile Integer maxSteps;

	public Integer getMaxSteps() {
		String configPath = "agentj.maxSteps";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			maxSteps = Integer.valueOf(value);
		}
		return maxSteps;
	}

	public void setMaxSteps(Integer maxSteps) {
		this.maxSteps = maxSteps;
	}

	@ConfigProperty(group = "agentj", subGroup = "agent", key = "userInputTimeout",
			path = "agentj.agent.userInputTimeout", description = "agentj.agent.userInputTimeout.description",
			defaultValue = "300", inputType = ConfigInputType.NUMBER)
	private volatile Integer userInputTimeout;

	public Integer getUserInputTimeout() {
		String configPath = "agentj.agent.userInputTimeout";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			userInputTimeout = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (userInputTimeout == null) {
			// Attempt to parse the default value specified in the annotation,
			// or use a hardcoded default if parsing fails or is complex to retrieve here.
			// For simplicity, directly using the intended default.
			userInputTimeout = 300;
		}
		return userInputTimeout;
	}

	public void setUserInputTimeout(Integer userInputTimeout) {
		this.userInputTimeout = userInputTimeout;
	}

	@ConfigProperty(group = "agentj", subGroup = "agent", key = "maxMemory", path = "agentj.agent.maxMemory",
			description = "agentj.agent.maxMemory.description", defaultValue = "1000",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer maxMemory;

	public Integer getMaxMemory() {
		String configPath = "agentj.agent.maxMemory";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			maxMemory = Integer.valueOf(value);
		}
		if (maxMemory == null) {
			maxMemory = 1000;
		}
		return maxMemory;
	}

	public void setMaxMemory(Integer maxMemory) {
		this.maxMemory = maxMemory;
	}

	@ConfigProperty(group = "agentj", subGroup = "general", key = "enableConversationMemory",
			path = "agentj.general.enableConversationMemory",
			description = "agentj.general.enableConversationMemory.description", defaultValue = "true",
			inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "agentj.general.enableConversationMemory.option.true"),
					@ConfigOption(value = "false", label = "agentj.general.enableConversationMemory.option.false") })
	private volatile Boolean enableConversationMemory;

	public Boolean getEnableConversationMemory() {
		String configPath = "agentj.general.enableConversationMemory";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			enableConversationMemory = Boolean.valueOf(value);
		}
		// Default to true if not configured
		if (enableConversationMemory == null) {
			enableConversationMemory = true;
		}
		return enableConversationMemory;
	}

	public void setEnableConversationMemory(Boolean enableConversationMemory) {
		this.enableConversationMemory = enableConversationMemory;
	}

	@ConfigProperty(group = "agentj", subGroup = "agent", key = "conversationMemoryMaxChars",
			path = "agentj.agent.conversationMemoryMaxChars",
			description = "agentj.agent.conversationMemoryMaxChars.description", defaultValue = "30000",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer conversationMemoryMaxChars;

	public Integer getConversationMemoryMaxChars() {
		String configPath = "agentj.agent.conversationMemoryMaxChars";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			conversationMemoryMaxChars = Integer.valueOf(value);
		}
		if (conversationMemoryMaxChars == null) {
			conversationMemoryMaxChars = 30000;
		}
		return conversationMemoryMaxChars;
	}

	public void setConversationMemoryMaxChars(Integer conversationMemoryMaxChars) {
		this.conversationMemoryMaxChars = conversationMemoryMaxChars;
	}

	@ConfigProperty(group = "agentj", subGroup = "agent", key = "parallelToolCalls",
			path = "agentj.agent.parallelToolCalls", description = "agentj.agent.parallelToolCalls.description",
			defaultValue = "false", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "agentj.agent.parallelToolCalls.option.true"),
					@ConfigOption(value = "false", label = "agentj.agent.parallelToolCalls.option.false") })
	private volatile Boolean parallelToolCalls;

	public Boolean getParallelToolCalls() {
		String configPath = "agentj.agent.parallelToolCalls";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			parallelToolCalls = Boolean.valueOf(value);
		}
		if (value == null) {
			parallelToolCalls = false;
		}
		return parallelToolCalls;
	}

	public void setParallelToolCalls(Boolean parallelToolCalls) {
		this.parallelToolCalls = parallelToolCalls;
	}

	@ConfigProperty(group = "agentj", subGroup = "agent", key = "executorPoolSize",
			path = "agentj.agent.executorPoolSize", description = "agentj.agent.executorPoolSize.description",
			defaultValue = "5", inputType = ConfigInputType.NUMBER)
	private volatile Integer executorPoolSize;

	public Integer getExecutorPoolSize() {
		String configPath = "agentj.agent.executorPoolSize";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			executorPoolSize = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (executorPoolSize == null) {
			executorPoolSize = 5;
		}
		return executorPoolSize;
	}

	public void setExecutorPoolSize(Integer executorPoolSize) {
		this.executorPoolSize = executorPoolSize;
	}

	@ConfigProperty(group = "agentj", subGroup = "agent", key = "llmReadTimeout", path = "agentj.agent.llmReadTimeout",
			description = "agentj.agent.llmReadTimeout.description", defaultValue = "120",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer llmReadTimeout;

	public Integer getLlmReadTimeout() {
		String configPath = "agentj.agent.llmReadTimeout";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			llmReadTimeout = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (llmReadTimeout == null) {
			llmReadTimeout = 120; // Default 120 seconds (2 minutes)
		}
		return llmReadTimeout;
	}

	public void setLlmReadTimeout(Integer llmReadTimeout) {
		this.llmReadTimeout = llmReadTimeout;
	}

	// Agent Settings
	// End-----------------------------------------------------------------------------------------------

	// Normal Settings
	// Begin--------------------------------------------------------------------------------------------

	// Normal Settings
	// End----------------------------------------------------------------------------------------------

	// File System Security SubGroup
	@ConfigProperty(group = "agentj", subGroup = "general", key = "externalLinkedFolder",
			path = "agentj.general.externalLinkedFolder", description = "agentj.general.externalLinkedFolder.description",
			defaultValue = "", inputType = ConfigInputType.TEXT)
	private volatile String externalLinkedFolder = "";

	public String getExternalLinkedFolder() {
		String configPath = "agentj.general.externalLinkedFolder";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			externalLinkedFolder = value;
		}
		return externalLinkedFolder;
	}

	public void setExternalLinkedFolder(String externalLinkedFolder) {
		this.externalLinkedFolder = externalLinkedFolder;
	}

	// MCP Service Loader Settings
	// Begin--------------------------------------------------------------------------------------------

	@ConfigProperty(group = "agentj", subGroup = "mcpServiceLoader", key = "connectionTimeoutSeconds",
			path = "agentj.mcpServiceLoader.connectionTimeoutSeconds",
			description = "agentj.mcpServiceLoader.connectionTimeoutSeconds.description", defaultValue = "20",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer mcpConnectionTimeoutSeconds;

	public Integer getMcpConnectionTimeoutSeconds() {
		String configPath = "agentj.mcpServiceLoader.connectionTimeoutSeconds";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			mcpConnectionTimeoutSeconds = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (mcpConnectionTimeoutSeconds == null) {
			mcpConnectionTimeoutSeconds = 3;
		}
		return mcpConnectionTimeoutSeconds;
	}

	public void setMcpConnectionTimeoutSeconds(Integer mcpConnectionTimeoutSeconds) {
		this.mcpConnectionTimeoutSeconds = mcpConnectionTimeoutSeconds;
	}

	@ConfigProperty(group = "agentj", subGroup = "mcpServiceLoader", key = "maxRetryCount",
			path = "agentj.mcpServiceLoader.maxRetryCount",
			description = "agentj.mcpServiceLoader.maxRetryCount.description", defaultValue = "3",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer mcpMaxRetryCount;

	public Integer getMcpMaxRetryCount() {
		String configPath = "agentj.mcpServiceLoader.maxRetryCount";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			mcpMaxRetryCount = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (mcpMaxRetryCount == null) {
			mcpMaxRetryCount = 1;
		}
		return mcpMaxRetryCount;
	}

	public void setMcpMaxRetryCount(Integer mcpMaxRetryCount) {
		this.mcpMaxRetryCount = mcpMaxRetryCount;
	}

	@ConfigProperty(group = "agentj", subGroup = "mcpServiceLoader", key = "maxConcurrentConnections",
			path = "agentj.mcpServiceLoader.maxConcurrentConnections",
			description = "agentj.mcpServiceLoader.maxConcurrentConnections.description", defaultValue = "10",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer mcpMaxConcurrentConnections;

	public Integer getMcpMaxConcurrentConnections() {
		String configPath = "agentj.mcpServiceLoader.maxConcurrentConnections";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			mcpMaxConcurrentConnections = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (mcpMaxConcurrentConnections == null) {
			mcpMaxConcurrentConnections = 10;
		}
		return mcpMaxConcurrentConnections;
	}

	public void setMcpMaxConcurrentConnections(Integer mcpMaxConcurrentConnections) {
		this.mcpMaxConcurrentConnections = mcpMaxConcurrentConnections;
	}

	// MCP Service Loader Settings
	// End----------------------------------------------------------------------------------------------

	// Image Recognition Settings
	// Begin--------------------------------------------------------------------------------------------

	@ConfigProperty(group = "agentj", subGroup = "imageRecognition", key = "poolSize",
			path = "agentj.imageRecognition.poolSize", description = "agentj.imageRecognition.poolSize.description",
			defaultValue = "4", inputType = ConfigInputType.NUMBER)
	private volatile Integer imageRecognitionPoolSize;

	public Integer getImageRecognitionPoolSize() {
		String configPath = "agentj.imageRecognition.poolSize";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			imageRecognitionPoolSize = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (imageRecognitionPoolSize == null) {
			imageRecognitionPoolSize = 4;
		}
		return imageRecognitionPoolSize;
	}

	public void setImageRecognitionPoolSize(Integer imageRecognitionPoolSize) {
		this.imageRecognitionPoolSize = imageRecognitionPoolSize;
	}

	@ConfigProperty(group = "agentj", subGroup = "imageRecognition", key = "modelName",
			path = "agentj.imageRecognition.modelName", description = "agentj.imageRecognition.modelName.description",
			defaultValue = "qwen-vl-ocr-latest", inputType = ConfigInputType.TEXT)
	private volatile String imageRecognitionModelName;

	public String getImageRecognitionModelName() {
		String configPath = "agentj.imageRecognition.modelName";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			imageRecognitionModelName = value;
		}
		// Ensure a default value if not configured and not set
		if (imageRecognitionModelName == null) {
			imageRecognitionModelName = "qwen-vl-ocr-latest";
		}
		return imageRecognitionModelName;
	}

	public void setImageRecognitionModelName(String imageRecognitionModelName) {
		this.imageRecognitionModelName = imageRecognitionModelName;
	}

	@ConfigProperty(group = "agentj", subGroup = "imageRecognition", key = "dpi", path = "agentj.imageRecognition.dpi",
			description = "agentj.imageRecognition.dpi.description", defaultValue = "120.0",
			inputType = ConfigInputType.NUMBER)
	private volatile Float imageRecognitionDpi;

	public Float getImageRecognitionDpi() {
		String configPath = "agentj.imageRecognition.dpi";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			imageRecognitionDpi = Float.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (imageRecognitionDpi == null) {
			imageRecognitionDpi = 120.0f;
		}
		return imageRecognitionDpi;
	}

	public void setImageRecognitionDpi(Float imageRecognitionDpi) {
		this.imageRecognitionDpi = imageRecognitionDpi;
	}

	@ConfigProperty(group = "agentj", subGroup = "imageRecognition", key = "imageType",
			path = "agentj.imageRecognition.imageType", description = "agentj.imageRecognition.imageType.description",
			defaultValue = "RGB", inputType = ConfigInputType.TEXT)
	private volatile String imageRecognitionImageType;

	public String getImageRecognitionImageType() {
		String configPath = "agentj.imageRecognition.imageType";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			imageRecognitionImageType = value;
		}
		// Ensure a default value if not configured and not set
		if (imageRecognitionImageType == null) {
			imageRecognitionImageType = "RGB";
		}
		return imageRecognitionImageType;
	}

	public void setImageRecognitionImageType(String imageRecognitionImageType) {
		this.imageRecognitionImageType = imageRecognitionImageType;
	}

	@ConfigProperty(group = "agentj", subGroup = "imageRecognition", key = "maxRetryAttempts",
			path = "agentj.imageRecognition.maxRetryAttempts",
			description = "agentj.imageRecognition.maxRetryAttempts.description", defaultValue = "3",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer imageRecognitionMaxRetryAttempts;

	public Integer getImageRecognitionMaxRetryAttempts() {
		String configPath = "agentj.imageRecognition.maxRetryAttempts";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			imageRecognitionMaxRetryAttempts = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (imageRecognitionMaxRetryAttempts == null) {
			imageRecognitionMaxRetryAttempts = 3;
		}
		return imageRecognitionMaxRetryAttempts;
	}

	public void setImageRecognitionMaxRetryAttempts(Integer imageRecognitionMaxRetryAttempts) {
		this.imageRecognitionMaxRetryAttempts = imageRecognitionMaxRetryAttempts;
	}

	// Image Recognition Settings
	// End----------------------------------------------------------------------------------------------

}
