package com.wangliang.agentj.rag;

import com.wangliang.agentj.config.RagProperties;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.tools.filesystem.UnifiedDirectoryManager;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于 Spring AI 官方范式的混合 RAG 流程：
 * - 查询预处理由外部调用方负责（可串联重写/关键词/意图识别）
 * - Retriever 负责向量检索（Qdrant）
 * - 检索结果按阈值/TopK 过滤，构造上下文
 * - ChatClient 生成回答并做简单后检查
 */
@Service
public class HybridRagService {

    private static final Logger log = LoggerFactory.getLogger(HybridRagService.class);

    private final VectorStore vectorStore;
    private final LlmService llmService;
    private final RagProperties ragProperties;
    private ChatClient chatClient;

    public HybridRagService(VectorStore vectorStore, LlmService llmService, RagProperties ragProperties) {
        this.vectorStore = vectorStore;
        this.llmService = llmService;
        this.ragProperties = ragProperties;
        // lazy init to avoid startup failure if model not ready
        try {
            this.chatClient = llmService.getDefaultDynamicAgentChatClient();
        } catch (Exception ignored) {
        }
    }

    public String answer(String userQuery) {
        return answerWithKnowledge(null, userQuery);
    }

    /**
     * 指定知识库过滤的对话，metadata 使用 kbId 过滤。
     */
    public String answerWithKnowledge(String knowledgeBaseId, String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return "请输入有效的问题。";
        }

        // 使用较低的相似度阈值进行搜索，然后在代码中按 kbId 过滤
        // 因为 Qdrant 的 FilterExpressionBuilder 可能不完全兼容
        double effectiveThreshold = Math.max(0.0, ragProperties.getSimilarityThreshold() - 0.3);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(userQuery)
                .topK(ragProperties.getTopK() * 3) // 获取更多结果以便过滤
                .similarityThreshold(effectiveThreshold)
                .build();
        
        List<?> retrieved = vectorStore.similaritySearch(searchRequest);
        log.info("向量检索完成: query='{}', 原始检索数={}, effectiveThreshold={}", 
                userQuery, retrieved.size(), effectiveThreshold);
        
        // 在代码中按 kbId 过滤
        List<Object> filtered;
        if (knowledgeBaseId != null && !knowledgeBaseId.trim().isEmpty()) {
            final String targetKbId = knowledgeBaseId.trim();
            filtered = retrieved.stream()
                    .filter(doc -> {
                        Map<String, Object> metadata = extractMetadata(doc);
                        Object kbId = metadata.get("kbId");
                        boolean matches = targetKbId.equals(String.valueOf(kbId));
                        if (!matches && kbId != null) {
                            log.debug("跳过文档，kbId不匹配: expected={}, actual={}", targetKbId, kbId);
                        }
                        return matches;
                    })
                    .filter(doc -> {
                        Double score = extractScore(doc);
                        return score == null || score >= ragProperties.getSimilarityThreshold();
                    })
                    .limit(ragProperties.getTopK())
                    .collect(Collectors.toList());
            log.info("按kbId过滤后: knowledgeBaseId={}, 过滤后数量={}, similarityThreshold={}", 
                    knowledgeBaseId, filtered.size(), ragProperties.getSimilarityThreshold());
        } else {
            filtered = retrieved.stream()
                    .filter(doc -> {
                        Double score = extractScore(doc);
                        return score == null || score >= ragProperties.getSimilarityThreshold();
                    })
                    .limit(ragProperties.getTopK())
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(filtered)) {
            log.warn("未检索到足够的上下文，将提示模型说明信息不足. knowledgeBaseId={}", knowledgeBaseId);
        }

        // 生成前构造可控长度的上下文
        String context = buildContext(filtered, ragProperties.getMaxContextChars());

        String prompt = """
                你是一个严谨的知识助手，请基于检索到的上下文回答用户问题。
                - 如果上下文不足以回答，请明确说明“未在知识库找到足够信息”。
                - 回答需简洁、用中文。
                
                用户问题：{question}
                检索到的上下文：
                {context}
                """;

        String answer = chatClient().prompt()
                .user(u -> u.text(prompt)
                        .param("question", userQuery.trim())
                        .param("context", context.isBlank() ? "（无可用上下文）" : context))
                .call()
                .content();

        if (answer == null || answer.trim().isEmpty()) {
            return "未生成有效回答，请稍后重试。";
        }

        return answer.trim();
    }

    private String buildContext(List<?> documents, int maxChars) {
        if (CollectionUtils.isEmpty(documents) || maxChars <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object doc : documents) {
            String content = extractContent(doc);
            if (content.isBlank()) {
                continue;
            }
            String filename = null;
            Map<String, Object> metadata = extractMetadata(doc);
            Object name = metadata.get("filename");
            if (name != null) {
                filename = name.toString();
            }
            int remaining = maxChars - builder.length();
            if (remaining <= 0) {
                break;
            }
            if (filename != null) {
                builder.append("【").append(filename).append("】\n");
            }
            builder.append(content, 0, Math.min(content.length(), remaining));
            builder.append("\n\n");
        }
        return builder.toString().trim();
    }

    private String extractContent(Object doc) {
        // Try getContent() method (Spring AI Document)
        try {
            var m = doc.getClass().getMethod("getContent");
            String content = Objects.toString(m.invoke(doc), "");
            if (!content.isBlank()) {
                return content;
            }
        }
        catch (Exception ignored) {
        }
        // Try content() method (record style)
        try {
            var m = doc.getClass().getMethod("content");
            String content = Objects.toString(m.invoke(doc), "");
            if (!content.isBlank()) {
                return content;
            }
        }
        catch (Exception ignored) {
        }
        // Try getText() method
        try {
            var m = doc.getClass().getMethod("getText");
            String content = Objects.toString(m.invoke(doc), "");
            if (!content.isBlank()) {
                return content;
            }
        }
        catch (Exception ignored) {
        }
        // Try text() method
        try {
            var m = doc.getClass().getMethod("text");
            String content = Objects.toString(m.invoke(doc), "");
            if (!content.isBlank()) {
                return content;
            }
        }
        catch (Exception ignored) {
        }
        // Log the document class for debugging
        log.warn("无法从文档提取内容, 文档类型={}, 文档={}", doc.getClass().getName(), doc);
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMetadata(Object doc) {
        try {
            var m = doc.getClass().getMethod("getMetadata");
            Object meta = m.invoke(doc);
            if (meta instanceof Map) {
                return (Map<String, Object>) meta;
            }
        }
        catch (Exception ignored) {
        }
        try {
            var m = doc.getClass().getMethod("metadata");
            Object meta = m.invoke(doc);
            if (meta instanceof Map) {
                return (Map<String, Object>) meta;
            }
        }
        catch (Exception ignored) {
        }
        return Map.of();
    }

    private Double extractScore(Object doc) {
        try {
            var m = doc.getClass().getMethod("getScore");
            Object score = m.invoke(doc);
            if (score instanceof Number num) {
                return num.doubleValue();
            }
        }
        catch (Exception ignored) {
        }
        try {
            var m = doc.getClass().getMethod("score");
            Object score = m.invoke(doc);
            if (score instanceof Number num) {
                return num.doubleValue();
            }
        }
        catch (Exception ignored) {
        }
        return null;
    }

    private ChatClient chatClient() {
        if (this.chatClient == null) {
            this.chatClient = llmService.getDefaultDynamicAgentChatClient();
        }
        return this.chatClient;
    }
}
