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
package com.wangliang.agentj.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.mcp.service.TolerantWebFluxSseClientTransport;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 阿里云百炼 DashScope MCP 连接测试
 * 用于诊断SSE连接和事件格式
 * 
 * 运行测试前请确保：
 * 1. API_KEY 是有效的
 * 2. 网络可以访问阿里云
 */
public class DashScopeMcpConnectionTest {

    // 请替换为你的实际API Key
    private static final String API_KEY = "sk-7ffe50084259436a9ac7b6689b51fd3c";
    private static final String BASE_URL = "https://dashscope.aliyuncs.com";
    private static final String SSE_ENDPOINT = "/api/v1/mcps/WebSearch/sse";

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE=new ParameterizedTypeReference<>(){};

    /**
     * 测试1: 直接测试SSE连接，查看服务器返回的事件
     */
    @Test
    public void testSseConnectionRawEvents() throws InterruptedException {
        System.out.println("========== 测试 DashScope SSE 连接 ==========");
        System.out.println("URL: " + BASE_URL + SSE_ENDPOINT);

        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + API_KEY)
                .defaultHeader("Accept", "text/event-stream")
                .defaultHeader("Content-Type", "application/json")
                .build();

        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("\n开始SSE连接...");

        Flux<ServerSentEvent<String>> eventStream = webClient.get()
                .uri(SSE_ENDPOINT)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header("MCP-Protocol-Version", "2024-11-05")
                .retrieve()
                .bodyToFlux(SSE_TYPE);

        eventStream
                .timeout(Duration.ofSeconds(30))
                .doOnNext(event -> {
                    System.out.println("\n>>> 收到SSE事件 <<<");
                    System.out.println("  Event Type: " + event.event());
                    System.out.println("  Event ID: " + event.id());
                    System.out.println("  Event Data: " + event.data());
                    System.out.println("  Event Comment: " + event.comment());
                })
                .doOnError(error -> {
                    System.err.println("\n>>> SSE连接错误 <<<");
                    System.err.println("  Error: " + error.getMessage());
                    error.printStackTrace();
                    latch.countDown();
                })
                .doOnComplete(() -> {
                    System.out.println("\n>>> SSE连接完成 <<<");
                    latch.countDown();
                })
                .subscribe();

        // 等待30秒观察事件
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("\n>>> 30秒超时，未收到完成信号 <<<");
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE01=new ParameterizedTypeReference<>(){};
    private static final String MCP_VERSION = "2024-11-05";

    @Test
    public void testSseConnectionAndCallTool() throws InterruptedException {
        URI sseUri = URI.create("https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse");

        WebClient client = WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + API_KEY)
                .build();

        CountDownLatch done = new CountDownLatch(1);

        AtomicReference<URI> messageUriRef = new AtomicReference<>();
        AtomicBoolean initSent = new AtomicBoolean(false);
        AtomicBoolean initializedSent = new AtomicBoolean(false);
        AtomicBoolean toolSent = new AtomicBoolean(false);

        Flux<ServerSentEvent<String>> eventStream = client.get()
                .uri(sseUri) // ✅ 直接使用完整 SSE URL
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header("MCP-Protocol-Version", MCP_VERSION)
                .retrieve()
                .bodyToFlux(SSE_TYPE)
                // SSE 是长连接，别用 30s 这种很短的 timeout
                .timeout(Duration.ofSeconds(120));

        eventStream
                .doOnNext(event -> {
                    System.out.println("\n>>> SSE <<<");
                    System.out.println("event=" + event.event());
                    System.out.println("id=" + event.id());
                    System.out.println("data=" + event.data());

                    // 1) endpoint：拿到 message 相对路径，resolve 成完整 URL
                    if ("endpoint".equals(event.event())) {
                        String msgPath = event.data() == null ? "" : event.data().trim();
                        if (msgPath.isEmpty())
                            return;

                        // ✅ 将 /api/v1/... 相对路径拼成 https://dashscope.aliyuncs.com/api/v1/...
                        URI msgUri = sseUri.resolve(msgPath);
                        messageUriRef.set(msgUri);

                        // 2) initialize（只发一次）
                        if (initSent.compareAndSet(false, true)) {
                            postJsonRpc(client, msgUri, Map.of(
                                    "jsonrpc", "2.0",
                                    "id", 1,
                                    "method", "initialize",
                                    "params", Map.of(
                                            "protocolVersion", MCP_VERSION,
                                            "capabilities", Map.of(),
                                            "clientInfo", Map.of(
                                                    "name", "dashscope-mcp-java-test",
                                                    "version", "0.1.0"))));
                        }
                        return;
                    }

                    // 3) 后续 SSE data 通常是 JSON-RPC 响应
                    String data = event.data();
                    if (data == null || data.isBlank())
                        return;

                    try {
                        JsonNode node = MAPPER.readTree(data);

                        // initialize response (id=1)
                        if (node.has("id") && node.get("id").asInt() == 1 && node.has("result")) {
                            URI msgUri = messageUriRef.get();
                            if (msgUri == null)
                                return;

                            // 4) notifications/initialized（只发一次）
                            if (initializedSent.compareAndSet(false, true)) {
                                postJsonRpc(client, msgUri, Map.of(
                                        "jsonrpc", "2.0",
                                        "method", "notifications/initialized",
                                        "params", Map.of()));
                            }

                            // 5) tools/call：调用你唯一工具 bailian_web_search（只发一次）
                            if (toolSent.compareAndSet(false, true)) {
                                postJsonRpc(client, msgUri, Map.of(
                                        "jsonrpc", "2.0",
                                        "id", 3,
                                        "method", "tools/call",
                                        "params", Map.of(
                                                "name", "bailian_web_search",
                                                "arguments", Map.of(
                                                        "query", "杭州今天的天气怎么样？",
                                                        "count", 3))));
                            }
                            return;
                        }

                        // tools/call response (id=3)
                        if (node.has("id") && node.get("id").asInt() == 3) {
                            System.out.println("\n>>> TOOL RESULT (id=3) <<<");
                            System.out.println(node.toPrettyString());
                            done.countDown();
                        }
                    } catch (Exception ignore) {
                        // keepalive/非 JSON 数据直接忽略
                    }
                })
                .doOnError(err -> {
                    System.err.println("\n>>> SSE ERROR <<<");
                    err.printStackTrace();
                    done.countDown();
                })
                .subscribe();

        boolean ok = done.await(120, TimeUnit.SECONDS);
        if (!ok) {
            System.out.println("\n>>> 超时：未收到 tools/call(id=3) 的结果 <<<");
        }
    }

    private static void postJsonRpc(WebClient client, URI msgUri, Object payload) {
        client.post()
                .uri(msgUri)
                .header("MCP-Protocol-Version", "2024-11-05")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(resp -> System.out.println("POST OK -> " + msgUri))
                .doOnError(e -> {
                    System.err.println("POST FAIL -> " + msgUri);
                    e.printStackTrace();
                })
                .subscribe();
    }}

    /**
     * 测试2: 测试原始HTTP响应（不使用SSE解析）
     */
    // @Test
    // public void testRawHttpResponse() {
    // System.out.println("========== 测试原始HTTP响应 ==========");
    // System.out.println("URL: " + DashScopeMcpConnectionTest.BASE_URL +
    // DashScopeMcpConnectionTest.SSE_ENDPOINT);
    //
    // WebClient webClient = WebClient.builder()
    // .baseUrl(A.BASE_URL)
    // .defaultHeader("Authorization", "Bearer " + API_KEY)
    // .build();
    //
    // try {
    // String response = webClient.get()
    // .uri(DashScopeMcpConnectionTest.SSE_ENDPOINT)
    // .accept(MediaType.TEXT_EVENT_STREAM)
    // .header("MCP-Protocol-Version", "2024-11-05")
    // .retrieve()
    // .bodyToMono(String.class)
    // .timeout(Duration.ofSeconds(15))
    // .block();
    //
    // System.out.println("\n>>> 原始响应内容 <<<");
    // System.out.println(response);
    // } catch (Exception e) {
    // System.err.println("\n>>> 请求错误 <<<");
    // System.err.println("Error: " + e.getMessage());
    // e.printStackTrace();
    // }
    // }
    //
    // /**
    // * 测试3: 测试发送初始化请求到SSE端点
    // */
    // @Test
    // public void testInitializeRequest() {
    // System.out.println("========== 测试发送初始化请求 ==========");
    // System.out.println("URL: " + DashScopeMcpConnectionTest.BASE_URL +
    // DashScopeMcpConnectionTest.SSE_ENDPOINT);
    //
    // WebClient webClient = WebClient.builder()
    // .baseUrl(DashScopeMcpConnectionTest.BASE_URL)
    // .defaultHeader("Authorization", "Bearer " + "API_KEY")
    // .defaultHeader("Content-Type", "application/json")
    // .build();
    //
    // // MCP 初始化请求
    // String initializeRequest = """
    // {
    // "jsonrpc": "2.0",
    // "id": 1,
    // "method": "initialize",
    // "params": {
    // "protocolVersion": "2024-11-05",
    // "capabilities": {},
    // "clientInfo": {
    // "name": "AgentJ",
    // "version": "1.0.0"
    // }
    // }
    // }
    // """;
    //
    // try {
    // System.out.println("\n发送初始化请求...");
    // System.out.println("Request Body: " + initializeRequest);
    //
    // String response = webClient.post()
    // .uri(SSE_ENDPOINT)
    // .contentType(MediaType.APPLICATION_JSON)
    // .header("MCP-Protocol-Version", "2024-11-05")
    // .bodyValue(initializeRequest)
    // .retrieve()
    // .bodyToMono(String.class)
    // .timeout(Duration.ofSeconds(15))
    // .block();
    //
    // System.out.println("\n>>> 初始化响应 <<<");
    // System.out.println(response);
    // } catch (Exception e) {
    // System.err.println("\n>>> 初始化请求错误 <<<");
    // System.err.println("Error: " + e.getMessage());
    // e.printStackTrace();
    // }
    // }
    //
    // /**
    // * 测试4: 测试列出工具请求
    // */
    // @Test
    // public void testListToolsRequest() {
    // System.out.println("========== 测试列出工具请求 ==========");
    // System.out.println("URL: " + BASE_URL + SSE_ENDPOINT);
    //
    // WebClient webClient = WebClient.builder()
    // .baseUrl(BASE_URL)
    // .defaultHeader("Authorization", "Bearer " + API_KEY)
    // .defaultHeader("Content-Type", "application/json")
    // .build();
    //
    // // MCP 列出工具请求
    // String listToolsRequest = """
    // {
    // "jsonrpc": "2.0",
    // "id": 2,
    // "method": "tools/list",
    // "params": {}
    // }
    // """;
    //
    // try {
    // System.out.println("\n发送列出工具请求...");
    // System.out.println("Request Body: " + listToolsRequest);
    //
    // String response = webClient.post()
    // .uri(SSE_ENDPOINT)
    // .contentType(MediaType.APPLICATION_JSON)
    // .header("MCP-Protocol-Version", "2024-11-05")
    // .bodyValue(listToolsRequest)
    // .retrieve()
    // .bodyToMono(String.class)
    // .timeout(Duration.ofSeconds(15))
    // .block();
    //
    // System.out.println("\n>>> 列出工具响应 <<<");
    // System.out.println(response);
    // } catch (Exception e) {
    // System.err.println("\n>>> 列出工具请求错误 <<<");
    // System.err.println("Error: " + e.getMessage());
    // e.printStackTrace();
    // }
    // }
    //
    // /**
    // * 测试5: 综合SSE流 + POST消息测试
    // */
    // @Test
    // public void testSseWithPostMessage() throws InterruptedException {
    // System.out.println("========== 综合SSE + POST测试 ==========");
    //
    // WebClient webClient = WebClient.builder()
    // .baseUrl(BASE_URL)
    // .defaultHeader("Authorization", "Bearer " + API_KEY)
    // .build();
    //
    // CountDownLatch latch = new CountDownLatch(1);
    //
    // // 1. 首先建立SSE连接
    // System.out.println("1. 建立SSE连接...");
    //
    // Flux<ServerSentEvent<String>> eventStream = webClient.get()
    // .uri(SSE_ENDPOINT)
    // .accept(MediaType.TEXT_EVENT_STREAM)
    // .header("MCP-Protocol-Version", "2024-11-05")
    // .retrieve()
    // .bodyToFlux(SSE_TYPE);
    //
    // // 订阅事件流
    // eventStream
    // .timeout(Duration.ofSeconds(20))
    // .doOnNext(event -> {
    // System.out.println("\n>>> SSE事件 <<<");
    // System.out.println(" Type: " + event.event());
    // System.out.println(" Data: " + event.data());
    //
    // // 如果收到endpoint事件，发送初始化请求
    // if ("endpoint".equals(event.event())) {
    // String endpoint = event.data();
    // System.out.println("\n收到endpoint: " + endpoint + ", 发送初始化请求...");
    // sendInitializeToEndpoint(webClient, endpoint);
    // }
    // })
    // .doOnError(error -> {
    // System.err.println("SSE Error: " + error.getMessage());
    // latch.countDown();
    // })
    // .doOnComplete(() -> {
    // System.out.println("SSE Complete");
    // latch.countDown();
    // })
    // .subscribe();
    //
    // // 2. 等待几秒后，尝试直接向SSE端点POST初始化请求
    // Thread.sleep(3000);
    // System.out.println("\n2. 尝试直接向SSE端点POST初始化请求...");
    // sendInitializeToEndpoint(webClient, SSE_ENDPOINT);
    //
    // latch.await(25, TimeUnit.SECONDS);
    // }
    //
    // private void sendInitializeToEndpoint(WebClient webClient, String endpoint) {
    // String initRequest = """
    // {
    // "jsonrpc": "2.0",
    // "id": 1,
    // "method": "initialize",
    // "params": {
    // "protocolVersion": "2024-11-05",
    // "capabilities": {},
    // "clientInfo": {
    // "name": "AgentJ",
    // "version": "1.0.0"
    // }
    // }
    // }
    // """;
    //
    // try {
    // String response = webClient.post()
    // .uri(endpoint)
    // .contentType(MediaType.APPLICATION_JSON)
    // .header("MCP-Protocol-Version", "2024-11-05")
    // .bodyValue(initRequest)
    // .retrieve()
    // .bodyToMono(String.class)
    // .timeout(Duration.ofSeconds(10))
    // .block();
    //
    // System.out.println("Initialize Response: " + response);
    // } catch (Exception e) {
    // System.err.println("Initialize Error: " + e.getMessage());
    // }
    // }
    //
    // /**
    // * 测试6: 使用 TolerantWebFluxSseClientTransport 进行完整的 MCP 连接测试
    // * 这是最接近实际使用场景的测试
    // */
    // @Test
    // public void testWithTolerantTransport() throws Exception {
    // System.out.println("========== 使用 TolerantWebFluxSseClientTransport 测试
    // ==========");
    //
    // ObjectMapper objectMapper = new ObjectMapper();
    // JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
    //
    // WebClient.Builder webClientBuilder = WebClient.builder()
    // .baseUrl(BASE_URL)
    // .defaultHeader("Authorization", "Bearer " + API_KEY)
    // .defaultHeader("Accept", "text/event-stream")
    // .defaultHeader("Content-Type", "application/json");
    //
    // // 去掉开头的斜杠
    // String endpoint = SSE_ENDPOINT.startsWith("/") ? SSE_ENDPOINT.substring(1) :
    // SSE_ENDPOINT;
    //
    // TolerantWebFluxSseClientTransport transport = new
    // TolerantWebFluxSseClientTransport(
    // webClientBuilder, jsonMapper, endpoint);
    //
    // System.out.println("Transport created, building MCP client...");
    //
    // McpAsyncClient mcpClient = McpClient.async(transport)
    // .requestTimeout(Duration.ofSeconds(30))
    // .clientInfo(new McpSchema.Implementation("AgentJ-Test", "1.0.0"))
    // .build();
    //
    // System.out.println("MCP client built, attempting to initialize...");
    //
    // try {
    // McpSchema.InitializeResult result = mcpClient.initialize()
    // .timeout(Duration.ofSeconds(30))
    // .doOnSubscribe(s -> System.out.println("Initialize subscribed"))
    // .doOnSuccess(r -> System.out.println("Initialize succeeded: " + r))
    // .doOnError(e -> System.err.println("Initialize failed: " + e.getMessage()))
    // .block();
    //
    // System.out.println("\n========== 初始化成功! ==========");
    // System.out.println("Server Info: " + result.serverInfo());
    // System.out.println("Protocol Version: " + result.protocolVersion());
    // System.out.println("Capabilities: " + result.capabilities());
    //
    // // 尝试列出工具
    // System.out.println("\n尝试列出工具...");
    // McpSchema.ListToolsResult toolsResult = mcpClient.listTools()
    // .timeout(Duration.ofSeconds(15))
    // .block();
    //
    // if (toolsResult != null && toolsResult.tools() != null) {
    // System.out.println("Available tools:");
    // for (McpSchema.Tool tool : toolsResult.tools()) {
    // System.out.println(" - " + tool.name() + ": " + tool.description());
    // }
    // }
    //
    // } catch (Exception e) {
    // System.err.println("\n========== 连接失败 ==========");
    // System.err.println("Error: " + e.getMessage());
    // e.printStackTrace();
    // } finally {
    // System.out.println("\nClosing transport...");
    // transport.closeGracefully().block();
    // }
    // }
    //
    // /**
    // * 测试7: 测试工具调用 (需要先成功连接)
    // */
    // @Test
    // public void testToolCall() throws Exception {
    // System.out.println("========== 测试工具调用 ==========");
    //
    // ObjectMapper objectMapper = new ObjectMapper();
    // JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
    //
    // WebClient.Builder webClientBuilder = WebClient.builder()
    // .baseUrl(BASE_URL)
    // .defaultHeader("Authorization", "Bearer " + API_KEY)
    // .defaultHeader("Accept", "text/event-stream")
    // .defaultHeader("Content-Type", "application/json");
    //
    // String endpoint = SSE_ENDPOINT.startsWith("/") ? SSE_ENDPOINT.substring(1) :
    // SSE_ENDPOINT;
    //
    // TolerantWebFluxSseClientTransport transport = new
    // TolerantWebFluxSseClientTransport(
    // webClientBuilder, jsonMapper, endpoint);
    //
    // McpAsyncClient mcpClient = McpClient.async(transport)
    // .requestTimeout(Duration.ofSeconds(60))
    // .clientInfo(new McpSchema.Implementation("AgentJ-Test", "1.0.0"))
    // .build();
    //
    // try {
    // // 初始化
    // System.out.println("Initializing...");
    // mcpClient.initialize()
    // .timeout(Duration.ofSeconds(30))
    // .block();
    // System.out.println("Initialized successfully");
    //
    // // 调用 bailian_web_search 工具
    // System.out.println("\n调用 bailian_web_search 工具...");
    //
    // // 构建参数
    // java.util.Map<String, Object> arguments = new java.util.HashMap<>();
    // arguments.put("query", "今天天气怎么样");
    // arguments.put("count", 5);
    //
    // McpSchema.CallToolResult callResult = mcpClient.callTool(
    // new McpSchema.CallToolRequest("bailian_web_search", arguments))
    // .timeout(Duration.ofSeconds(30))
    // .block();
    //
    // System.out.println("\n========== 工具调用结果 ==========");
    // if (callResult != null && callResult.content() != null) {
    // for (McpSchema.Content content : callResult.content()) {
    // System.out.println("Content type: " + content.getClass().getSimpleName());
    // if (content instanceof McpSchema.TextContent textContent) {
    // System.out.println("Text: " + textContent.text());
    // }
    // }
    // }
    //
    // } catch (Exception e) {
    // System.err.println("\n========== 测试失败 ==========");
    // System.err.println("Error: " + e.getMessage());
    // e.printStackTrace();
    // } finally {
    // transport.closeGracefully().block();
    // }
    // }

    

    

    
            
            
            
            
            
            
            
            

    
    
    