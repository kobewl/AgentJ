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
package com.wangliang.agentj.codegen.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.codegen.dto.AppVO;
import com.wangliang.agentj.codegen.dto.ChatMessageVO;
import com.wangliang.agentj.codegen.dto.CodeGenRequest;
import com.wangliang.agentj.codegen.dto.CreateAppRequest;
import com.wangliang.agentj.codegen.service.CodeGenService;
import com.wangliang.agentj.user.context.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成控制器
 */
@Tag(name = "代码生成管理", description = "AI 代码生成相关接口")
@RestController
@RequestMapping("/api/codegen")
public class CodeGenController {

	private final CodeGenService codeGenService;
	private final ObjectMapper objectMapper;

	public CodeGenController(CodeGenService codeGenService, ObjectMapper objectMapper) {
		this.codeGenService = codeGenService;
		this.objectMapper = objectMapper;
	}

	/**
	 * 创建应用
	 */
	@Operation(summary = "创建代码生成应用")
	@PostMapping("/app")
	public AppVO createApp(@Valid @RequestBody CreateAppRequest request) {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.createApp(
				request.getAppName(),
				request.getInitPrompt(),
				request.getCodeGenType(),
				userId);
	}

	/**
	 * 获取应用详情
	 */
	@Operation(summary = "获取应用详情")
	@GetMapping("/app/{id}")
	public AppVO getApp(@PathVariable Long id) {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.getApp(id, userId);
	}

	/**
	 * 获取用户的应用列表
	 */
	@Operation(summary = "获取用户的应用列表")
	@GetMapping("/app")
	public List<AppVO> getUserApps() {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.getUserApps(userId);
	}

	/**
	 * 更新应用
	 */
	@Operation(summary = "更新应用信息")
	@PutMapping("/app/{id}")
	public AppVO updateApp(@PathVariable Long id,
			@Valid @RequestBody com.wangliang.agentj.codegen.dto.UpdateAppRequest request) {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.updateApp(id, request.getAppName(), request.getCover(), userId);
	}

	/**
	 * 删除应用
	 */
	@Operation(summary = "删除应用")
	@DeleteMapping("/app/{id}")
	public boolean deleteApp(@PathVariable Long id) {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.deleteApp(id, userId);
	}

	/**
	 * 部署应用
	 */
	@Operation(summary = "部署应用")
	@PostMapping("/app/{id}/deploy")
	public AppVO deployApp(@PathVariable Long id) {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.deployApp(id, userId);
	}

	/**
	 * 流式生成代码 (SSE)
	 */
	@Operation(summary = "流式生成代码")
	@GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> generateCodeStream(
			@RequestParam Long appId,
			@RequestParam String message,
			@RequestParam(required = false) String elementInfo) {
		Long userId = UserContextHolder.getUserId();

		CodeGenRequest request = new CodeGenRequest();
		request.setAppId(appId);
		request.setMessage(message);

		// 解析元素信息
		if (elementInfo != null && !elementInfo.isBlank()) {
			try {
				CodeGenRequest.ElementInfo info = objectMapper.readValue(elementInfo, CodeGenRequest.ElementInfo.class);
				request.setElementInfo(info);
			} catch (JsonProcessingException e) {
				// 忽略解析错误
			}
		}

		// 获取流式响应
		Flux<String> contentFlux = codeGenService.generateCodeStream(request, userId);

		return contentFlux
				.map(chunk -> {
					try {
						// 包装为 SSE 格式，与 yu-ai-code-mother 保持一致
						Map<String, String> wrapper = Map.of("d", chunk);
						String jsonData = objectMapper.writeValueAsString(wrapper);
						return ServerSentEvent.<String>builder()
								.data(jsonData)
								.build();
					} catch (JsonProcessingException e) {
						return ServerSentEvent.<String>builder()
								.data(chunk)
								.build();
					}
				})
				.concatWith(Mono.just(
						// 发送结束事件
						ServerSentEvent.<String>builder()
								.event("done")
								.data("")
								.build()
				))
				.onErrorResume(error -> {
					// 错误时发送错误事件
					try {
						Map<String, String> errorWrapper = Map.of(
								"error", error.getMessage() != null ? error.getMessage() : "生成失败"
						);
						String errorJson = objectMapper.writeValueAsString(errorWrapper);
						return Flux.just(ServerSentEvent.<String>builder()
								.event("error")
								.data(errorJson)
								.build());
					} catch (JsonProcessingException e) {
						return Flux.just(ServerSentEvent.<String>builder()
								.event("error")
								.data("{\"error\":\"生成失败\"}")
								.build());
					}
				});
	}

	/**
	 * 获取已生成的代码（兼容旧接口）
	 */
	@Operation(summary = "获取已生成的代码")
	@GetMapping("/code/{appId}")
	public String getGeneratedCode(@PathVariable Long appId) {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.getGeneratedCode(appId, userId);
	}

	/**
	 * 获取对话历史
	 */
	@Operation(summary = "获取对话历史")
	@GetMapping("/chat/history/{appId}")
	public List<ChatMessageVO> getChatHistory(@PathVariable Long appId) {
		Long userId = UserContextHolder.getUserId();
		return codeGenService.getChatHistory(appId, userId);
	}

	/**
	 * 下载应用代码
	 */
	@Operation(summary = "下载应用代码")
	@GetMapping("/app/{id}/download")
	public void downloadAppCode(@PathVariable Long id, HttpServletResponse response) {
		Long userId = UserContextHolder.getUserId();

		try {
			// 获取应用信息验证权限
			AppVO app = codeGenService.getApp(id, userId);
			if (app == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "应用不存在");
				return;
			}

			// 获取代码文件路径
			String deployKey = app.getDeployKey();
			if (deployKey == null || deployKey.isBlank()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "应用未生成代码");
				return;
			}

			// 从 FileStorageService 获取文件路径
			String codePath = System.getProperty("agentj.codegen.output-dir", "./tmp/code_output");
			File appDir = new File(codePath, String.valueOf(id));
			File indexFile = new File(appDir, "index.html");

			if (!indexFile.exists()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "代码文件不存在");
				return;
			}

			// 设置响应头
			response.setContentType("application/zip");
			response.setHeader("Content-Disposition", "attachment; filename=\"app_" + id + ".zip\"");

			// 创建 ZIP 文件
			try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
				ZipEntry entry = new ZipEntry("index.html");
				zos.putNextEntry(entry);

				try (FileInputStream fis = new FileInputStream(indexFile)) {
					byte[] buffer = new byte[1024];
					int len;
					while ((len = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				}

				zos.closeEntry();
			}

		} catch (IOException e) {
			throw new RuntimeException("下载失败", e);
		}
	}
}
