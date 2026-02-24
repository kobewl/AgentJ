package com.wangliang.agentj.llm;

import com.wangliang.agentj.model.entity.DynamicModelEntity;
import com.wangliang.agentj.model.repository.DynamicModelRepository;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workflow-specific LLM Service
 * Provides dynamic ChatClient creation with configurable model, temperature,
 * and topP
 */
@Service
public class WorkflowLlmService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowLlmService.class);

    @Autowired
    private DynamicModelRepository dynamicModelRepository;

    @Autowired
    private ObjectProvider<RestClient.Builder> restClientBuilderProvider;

    @Autowired
    private ObjectProvider<WebClient.Builder> webClientBuilderProvider;

    @Autowired
    private ObjectProvider<ObservationRegistry> observationRegistry;

    @Autowired
    private ObjectProvider<ChatModelObservationConvention> observationConvention;

    @Autowired
    private ObjectProvider<ToolExecutionEligibilityPredicate> openAiToolExecutionEligibilityPredicate;

    @Autowired(required = false)
    private WebClient webClientWithDnsCache;

    /**
     * Get available model list for frontend selection
     * 
     * @return List of model info maps containing id, name, description
     */
    public List<Map<String, Object>> getAvailableModels() {
        return dynamicModelRepository.findAll().stream()
                .map(entity -> {
                    Map<String, Object> modelInfo = new HashMap<>();
                    modelInfo.put("id", entity.getId());
                    modelInfo.put("modelName", entity.getModelName());
                    modelInfo.put("description", entity.getModelDescription());
                    modelInfo.put("isDefault", entity.getIsDefault());
                    modelInfo.put("defaultTemperature", entity.getTemperature());
                    modelInfo.put("defaultTopP", entity.getTopP());
                    return modelInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Create a ChatClient with custom configuration for workflow nodes
     * 
     * @param modelName   The model name to use (null for default)
     * @param temperature Custom temperature (null for model default)
     * @param topP        Custom topP (null for model default)
     * @return Configured ChatClient
     */
    public ChatClient createChatClient(String modelName, Double temperature, Double topP) {
        // Find the model entity
        DynamicModelEntity modelEntity = findModelEntity(modelName);
        if (modelEntity == null) {
            throw new IllegalStateException("No model configuration found for: " +
                    (modelName != null ? modelName : "default"));
        }

        log.info("Creating workflow ChatClient with model: {}, temp: {}, topP: {}",
                modelEntity.getModelName(), temperature, topP);

        // Build options with custom or default values
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelEntity.getModelName())
                .temperature(temperature != null ? temperature
                        : (modelEntity.getTemperature() != null ? modelEntity.getTemperature() : 0.7))
                .topP(topP != null ? topP : (modelEntity.getTopP() != null ? modelEntity.getTopP() : 1.0))
                .build();

        // Build the ChatClient
        return buildChatClient(modelEntity, options);
    }

    /**
     * Find model entity by name or get default
     */
    private DynamicModelEntity findModelEntity(String modelName) {
        if (modelName != null && !modelName.trim().isEmpty()) {
            DynamicModelEntity entity = dynamicModelRepository.findByModelName(modelName);
            if (entity != null) {
                return entity;
            }
            log.warn("Model '{}' not found, falling back to default", modelName);
        }

        // Get default model
        DynamicModelEntity defaultModel = dynamicModelRepository.findByIsDefaultTrue();
        if (defaultModel != null) {
            return defaultModel;
        }

        // Fallback to first available model
        List<DynamicModelEntity> allModels = dynamicModelRepository.findAll();
        return allModels.isEmpty() ? null : allModels.get(0);
    }

    /**
     * Build ChatClient from model entity and options
     */
    private ChatClient buildChatClient(DynamicModelEntity modelEntity, OpenAiChatOptions options) {
        // Set headers
        Map<String, String> headers = modelEntity.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("User-Agent", "AgentJ/4.8.0");
        options.setHttpHeaders(headers);

        // Create OpenAI API
        OpenAiApi openAiApi = createOpenAiApi(modelEntity);

        // Create chat model
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .toolExecutionEligibilityPredicate(
                        openAiToolExecutionEligibilityPredicate
                                .getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();

        observationConvention.ifAvailable(chatModel::setObservationConvention);

        // Build and return ChatClient
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(OpenAiChatOptions.fromOptions(options))
                .build();
    }

    /**
     * Create OpenAI API with enhanced WebClient
     */
    private OpenAiApi createOpenAiApi(DynamicModelEntity modelEntity) {
        Map<String, String> headers = modelEntity.getHeaders();
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        if (headers != null) {
            headers.forEach(multiValueMap::add);
        }

        // Use enhanced WebClient builder
        WebClient.Builder enhancedWebClientBuilder = createEnhancedWebClientBuilder(modelEntity);

        String completionsPath = modelEntity.getCompletionsPath();

        return new OpenAiApi(
                modelEntity.getBaseUrl(),
                new SimpleApiKey(modelEntity.getApiKey()),
                multiValueMap,
                completionsPath,
                "/v1/embeddings",
                restClientBuilderProvider.getIfAvailable(RestClient::builder),
                enhancedWebClientBuilder,
                RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
    }

    /**
     * Create enhanced WebClient builder with DNS cache or timeout configuration
     */
    private WebClient.Builder createEnhancedWebClientBuilder(DynamicModelEntity modelEntity) {
        WebClient.Builder enhancedWebClientBuilder;
        if (webClientWithDnsCache != null) {
            log.debug("Using DNS-cached WebClient for model: {}", modelEntity.getModelName());
            enhancedWebClientBuilder = webClientWithDnsCache.mutate();
        } else {
            enhancedWebClientBuilder = webClientBuilderProvider.getIfAvailable(WebClient::builder).clone()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .filter((request, next) -> next.exchange(request).timeout(Duration.ofMinutes(10)));
        }
        return enhancedWebClientBuilder;
    }
}
