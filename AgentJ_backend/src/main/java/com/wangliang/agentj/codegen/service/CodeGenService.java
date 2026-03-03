package com.wangliang.agentj.codegen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.codegen.dto.AppVO;
import com.wangliang.agentj.codegen.dto.ChatMessageVO;
import com.wangliang.agentj.codegen.dto.CodeGenRequest;
import com.wangliang.agentj.codegen.entity.CodeAppEntity;
import com.wangliang.agentj.codegen.entity.CodeChatHistoryEntity;
import com.wangliang.agentj.codegen.exception.CodeGenErrorCode;
import com.wangliang.agentj.codegen.exception.CodeGenException;
import com.wangliang.agentj.codegen.repository.CodeAppRepository;
import com.wangliang.agentj.codegen.repository.CodeChatHistoryRepository;
import com.wangliang.agentj.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 代码生成核心服务
 */
@Service
public class CodeGenService {

    private static final Logger log = LoggerFactory.getLogger(CodeGenService.class);

    private final LlmService llmService;
    private final FileStorageService fileStorageService;
    private final CodeAppRepository appRepository;
    private final CodeChatHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    private String systemPrompt;

    public CodeGenService(LlmService llmService,
                          FileStorageService fileStorageService,
                          CodeAppRepository appRepository,
                          CodeChatHistoryRepository historyRepository,
                          ObjectMapper objectMapper,
                          org.springframework.core.io.ResourceLoader resourceLoader) {
        this.llmService = llmService;
        this.fileStorageService = fileStorageService;
        this.appRepository = appRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;

        // 加载系统 Prompt
        try {
            Resource promptResource = resourceLoader.getResource("classpath:prompts/codegen/html-system.txt");
            this.systemPrompt = promptResource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to load system prompt, using default", e);
            this.systemPrompt = "你是一位资深的 Web 前端开发专家，精通 HTML、CSS 和原生 JavaScript。";
        }
    }

    /**
     * 创建应用
     */
    @Transactional
    public AppVO createApp(String appName, String initPrompt, String codeGenType, Long userId) {
        CodeAppEntity entity = new CodeAppEntity();
        entity.setAppName(appName);
        entity.setInitPrompt(initPrompt);
        entity.setCodeGenType(codeGenType != null ? codeGenType : "HTML");
        entity.setUserId(userId);
        // 注意：deployKey 在第一次生成代码后才生成，而不是在创建应用时

        entity = appRepository.save(entity);

        // 保存初始消息到历史（用户消息）
        CodeChatHistoryEntity history = new CodeChatHistoryEntity();
        history.setMessage(initPrompt);
        history.setMessageType("user");
        history.setAppId(entity.getId());
        history.setUserId(userId);
        historyRepository.save(history);

        log.info("创建代码生成应用成功，appId: {}, userId: {}，等待首次代码生成", entity.getId(), userId);
        return toAppVO(entity);
    }

    /**
     * 获取应用详情
     */
    public AppVO getApp(Long appId, Long userId) {
        return appRepository.findByIdAndUserIdAndIsDeleted(appId, userId, false)
                .map(this::toAppVO)
                .orElseThrow(() -> new CodeGenException(CodeGenErrorCode.NOT_FOUND_ERROR));
    }

    /**
     * 获取用户的应用列表
     */
    public List<AppVO> getUserApps(Long userId) {
        return appRepository.findByUserIdAndIsDeletedOrderByCreatedAtDesc(userId, false)
                .stream()
                .map(this::toAppVO)
                .toList();
    }

    /**
     * 更新应用
     */
    @Transactional
    public AppVO updateApp(Long appId, String appName, String cover, Long userId) {
        CodeAppEntity entity = appRepository.findByIdAndUserIdAndIsDeleted(appId, userId, false)
                .orElseThrow(() -> new CodeGenException(CodeGenErrorCode.NOT_FOUND_ERROR));

        if (appName != null && !appName.isBlank()) {
            entity.setAppName(appName);
        }
        if (cover != null && !cover.isBlank()) {
            entity.setCover(cover);
        }

        entity = appRepository.save(entity);
        return toAppVO(entity);
    }

    /**
     * 删除应用
     */
    @Transactional
    public boolean deleteApp(Long appId, Long userId) {
        CodeAppEntity entity = appRepository.findByIdAndUserIdAndIsDeleted(appId, userId, false)
                .orElseThrow(() -> new CodeGenException(CodeGenErrorCode.NOT_FOUND_ERROR));

        entity.setIsDeleted(true);
        appRepository.save(entity);

        // 软删除历史记录
        historyRepository.softDeleteByAppId(appId);

        // 删除文件（包括输出目录和部署目录）
        fileStorageService.deleteAppFiles(appId, entity.getDeployKey());

        log.info("删除代码生成应用成功，appId: {}", appId);
        return true;
    }

    /**
     * 部署应用
     */
    @Transactional
    public AppVO deployApp(Long appId, Long userId) {
        CodeAppEntity entity = appRepository.findByIdAndUserIdAndIsDeleted(appId, userId, false)
                .orElseThrow(() -> new CodeGenException(CodeGenErrorCode.NOT_FOUND_ERROR));

        // 检查代码是否存在
        String existingCode = fileStorageService.readHtmlFile(appId);
        if (existingCode == null || existingCode.isEmpty()) {
            throw new CodeGenException(CodeGenErrorCode.DEPLOY_ERROR, "应用代码不存在，请先生成代码");
        }

        if (entity.getDeployKey() == null || entity.getDeployKey().isBlank()) {
            entity.setDeployKey(generateDeployKey());
        }

        fileStorageService.deployApp(appId, entity.getDeployKey());
        entity.setDeployedTime(LocalDateTime.now());
        entity = appRepository.save(entity);

        log.info("部署应用成功，appId: {}, deployKey: {}", appId, entity.getDeployKey());
        return toAppVO(entity);
    }

    /**
     * 获取对话历史
     */
    public List<ChatMessageVO> getChatHistory(Long appId, Long userId) {
        // 验证应用权限
        appRepository.findByIdAndUserIdAndIsDeleted(appId, userId, false)
                .orElseThrow(() -> new CodeGenException(CodeGenErrorCode.NOT_FOUND_ERROR));

        return historyRepository.findByAppIdAndIsDeletedOrderByCreatedAtAsc(appId, false)
                .stream()
                .map(this::toChatMessageVO)
                .toList();
    }

    /**
     * 流式生成代码
     * 返回 SSE 流式响应
     */
    public Flux<String> generateCodeStream(CodeGenRequest request, Long userId) {
        // 1. 参数校验
        if (request.getAppId() == null || request.getAppId() <= 0) {
            return Flux.error(new CodeGenException(CodeGenErrorCode.PARAMS_ERROR, "应用 ID 无效"));
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Flux.error(new CodeGenException(CodeGenErrorCode.PARAMS_ERROR, "消息内容不能为空"));
        }

        // 2. 查询应用并校验权限
        CodeAppEntity app = appRepository.findByIdAndUserIdAndIsDeleted(
                        request.getAppId(), userId, false)
                .orElseThrow(() -> new CodeGenException(CodeGenErrorCode.NOT_FOUND_ERROR, "应用不存在"));

        // 3. 保存用户消息
        CodeChatHistoryEntity userHistory = new CodeChatHistoryEntity();
        userHistory.setMessage(request.getMessage());
        userHistory.setMessageType("user");
        userHistory.setAppId(request.getAppId());
        userHistory.setUserId(userId);
        historyRepository.save(userHistory);

        // 4. 构建消息列表（包含历史对话上下文）
        List<Message> messages = buildMessagesWithHistory(request.getAppId(), request);

        // 5. 流式调用 AI，使用动态模型配置
        ChatClient chatClient = llmService.getDefaultDynamicAgentChatClient();
        AtomicReference<StringBuilder> contentRef = new AtomicReference<>(new StringBuilder());

        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    // 累积内容
                    contentRef.get().append(chunk);
                })
                .doOnComplete(() -> {
                    // 流结束后保存完整代码
                    String fullContent = contentRef.get().toString();
                    String htmlCode = extractHtmlCode(fullContent);

                    if (htmlCode != null && !htmlCode.isEmpty()) {
                        try {
                            // 检查是否需要生成 deployKey（首次生成代码）
                            String deployKey = app.getDeployKey();
                            if (deployKey == null || deployKey.isBlank()) {
                                deployKey = generateDeployKey();
                                app.setDeployKey(deployKey);
                                app.setDeployedTime(LocalDateTime.now());
                                appRepository.save(app);
                                log.info("首次生成代码，生成 deployKey: {}, appId: {}", deployKey, request.getAppId());
                            }

                            // 保存生成的代码文件到输出目录
                            fileStorageService.saveHtmlFile(request.getAppId(), deployKey, htmlCode);

                            // 自动部署到部署目录（用于实时预览）
                            fileStorageService.deployApp(request.getAppId(), deployKey);
                            log.info("自动部署成功，deployKey: {}, appId: {}", deployKey, request.getAppId());

                            // 保存 AI 响应到历史
                            CodeChatHistoryEntity aiHistory = new CodeChatHistoryEntity();
                            aiHistory.setMessage(htmlCode);
                            aiHistory.setMessageType("ai");
                            aiHistory.setAppId(request.getAppId());
                            aiHistory.setUserId(userId);
                            historyRepository.save(aiHistory);

                            log.info("代码生成并保存成功，appId: {}, codeLength: {}", request.getAppId(), htmlCode.length());
                        } catch (Exception e) {
                            log.error("保存生成的代码失败，appId: {}", request.getAppId(), e);
                        }
                    } else {
                        log.warn("未能从 AI 响应中提取有效的 HTML 代码，appId: {}", request.getAppId());
                    }
                })
                .doOnError(error -> {
                    log.error("AI 代码生成失败，appId: {}", request.getAppId(), error);
                })
                .onErrorResume(error -> {
                    // 错误时返回错误信息
                    if (error instanceof CodeGenException) {
                        return Flux.just("\n\n[错误: " + error.getMessage() + "]");
                    }
                    return Flux.just("\n\n[错误: 代码生成失败，请重试]");
                });
    }

    /**
     * 构建包含历史对话的消息列表
     */
    private List<Message> buildMessagesWithHistory(Long appId, CodeGenRequest request) {
        List<Message> messages = new ArrayList<>();

        // 添加系统消息
        messages.add(new SystemMessage(systemPrompt));

        // 获取历史对话（最近10轮，即20条消息）
        List<CodeChatHistoryEntity> history = historyRepository
                .findByAppIdAndIsDeletedOrderByCreatedAtAsc(appId, false);

        log.info("构建消息上下文，appId: {}, 历史记录总数: {}", appId, history.size());

        // 限制历史记录数量，避免 token 过多
        int maxHistory = 20;
        int startIndex = Math.max(0, history.size() - maxHistory);

        for (int i = startIndex; i < history.size(); i++) {
            CodeChatHistoryEntity hist = history.get(i);
            if ("user".equals(hist.getMessageType())) {
                messages.add(new UserMessage(hist.getMessage()));
                log.debug("添加用户消息到上下文，长度: {}", hist.getMessage().length());
            } else if ("ai".equals(hist.getMessageType())) {
                // 将 AI 的响应也加入上下文，以便在增量修改时 AI 能基于之前的代码进行修改
                // 而不是重新生成整个代码
                messages.add(new AssistantMessage(hist.getMessage()));
                log.debug("添加 AI 响应到上下文，代码长度: {}", hist.getMessage().length());
            }
        }

        // 构建当前用户消息（包含元素信息）
        String currentUserMessage = buildUserMessage(request);
        messages.add(new UserMessage(currentUserMessage));

        log.info("最终上下文消息数量: {} (system: 1, history: {}, current: 1)",
                messages.size(), history.size());

        return messages;
    }

    /**
     * 构建用户消息（包含元素信息）
     */
    private String buildUserMessage(CodeGenRequest request) {
        StringBuilder sb = new StringBuilder(request.getMessage());

        if (request.getElementInfo() != null) {
            sb.append("\n\n选中的元素信息：\n");
            if (request.getElementInfo().getTagName() != null) {
                sb.append("- 标签: ").append(request.getElementInfo().getTagName()).append("\n");
            }
            if (request.getElementInfo().getId() != null && !request.getElementInfo().getId().isBlank()) {
                sb.append("- ID: ").append(request.getElementInfo().getId()).append("\n");
            }
            if (request.getElementInfo().getClassName() != null && !request.getElementInfo().getClassName().isBlank()) {
                sb.append("- 类名: ").append(request.getElementInfo().getClassName()).append("\n");
            }
            if (request.getElementInfo().getTextContent() != null && !request.getElementInfo().getTextContent().isBlank()) {
                sb.append("- 文本内容: ").append(request.getElementInfo().getTextContent()).append("\n");
            }
            if (request.getElementInfo().getSelector() != null && !request.getElementInfo().getSelector().isBlank()) {
                sb.append("- CSS选择器: ").append(request.getElementInfo().getSelector()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 从 AI 响应中提取 HTML 代码
     */
    private String extractHtmlCode(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        // 查找代码块
        int codeBlockStart = response.indexOf("```html");
        if (codeBlockStart == -1) {
            codeBlockStart = response.indexOf("```");
        }

        if (codeBlockStart != -1) {
            int codeStart = response.indexOf('\n', codeBlockStart) + 1;
            int codeEnd = response.indexOf("```", codeStart);

            if (codeEnd > codeStart) {
                return response.substring(codeStart, codeEnd).trim();
            }
        }

        // 如果没有找到代码块，检查整个响应是否是 HTML
        String trimmed = response.trim();
        if (trimmed.startsWith("<!DOCTYPE html>") || trimmed.startsWith("<html")) {
            return trimmed;
        }

        // 如果响应包含 HTML 标签，尝试提取
        int htmlStart = trimmed.indexOf("<!DOCTYPE html>");
        if (htmlStart == -1) {
            htmlStart = trimmed.indexOf("<html");
        }

        if (htmlStart != -1) {
            // 找到 HTML 结束位置
            int htmlEnd = trimmed.lastIndexOf("</html>");
            if (htmlEnd > htmlStart) {
                return trimmed.substring(htmlStart, htmlEnd + 7).trim();
            }
        }

        return null;
    }

    /**
     * 获取已生成的代码
     */
    public String getGeneratedCode(Long appId, Long userId) {
        appRepository.findByIdAndUserIdAndIsDeleted(appId, userId, false)
                .orElseThrow(() -> new CodeGenException(CodeGenErrorCode.NOT_FOUND_ERROR));

        return fileStorageService.readHtmlFile(appId);
    }

    /**
     * 生成部署标识
     */
    private String generateDeployKey() {
        return "html_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 转换为 AppVO
     */
    private AppVO toAppVO(CodeAppEntity entity) {
        AppVO vo = new AppVO();
        vo.setId(entity.getId());
        vo.setAppName(entity.getAppName());
        vo.setCover(entity.getCover());
        vo.setInitPrompt(entity.getInitPrompt());
        vo.setCodeGenType(entity.getCodeGenType());
        vo.setDeployKey(entity.getDeployKey());
        vo.setDeployedTime(entity.getDeployedTime());
        vo.setUserId(entity.getUserId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getDeployKey() != null) {
            vo.setPreviewUrl("/static/html/" + entity.getDeployKey() + "/index.html");
        }

        return vo;
    }

    /**
     * 转换为 ChatMessageVO
     */
    private ChatMessageVO toChatMessageVO(CodeChatHistoryEntity entity) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(entity.getId());
        vo.setMessage(entity.getMessage());
        vo.setMessageType(entity.getMessageType());
        vo.setAppId(entity.getAppId());
        vo.setUserId(entity.getUserId());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
