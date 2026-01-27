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

import com.wangliang.agentj.codegen.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 静态资源控制器
 * 用于访问部署的代码生成页面
 */
@Tag(name = "静态资源", description = "代码生成静态资源访问")
@RestController
@RequestMapping("/static/html")
public class StaticResourceController {

	private static final Logger log = LoggerFactory.getLogger(StaticResourceController.class);

	private final FileStorageService fileStorageService;

	public StaticResourceController(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	/**
	 * 访问部署的 HTML 页面
	 * 支持路径如：/static/html/{deployKey}/index.html
	 */
	@Operation(summary = "访问部署的静态页面")
	@GetMapping("/{deployKey}/**")
	public ResponseEntity<Resource> serveStatic(@PathVariable String deployKey,
			HttpServletRequest request) {
		try {
			// 从请求 URI 中提取剩余路径
			String requestUri = request.getRequestURI();
			String pattern = "/static/html/" + deployKey + "/";
			String path = "";

			if (requestUri.length() > pattern.length()) {
				path = requestUri.substring(pattern.length());
			}

			// 构建文件路径
			Path filePath;
			if (path == null || path.isEmpty()) {
				filePath = fileStorageService.getDeployDir().resolve(deployKey).resolve("index.html");
			} else {
				filePath = fileStorageService.getDeployDir().resolve(deployKey).resolve(path);
			}

			log.debug("Serving static file: deployKey={}, path={}, filePath={}",
					deployKey, path, filePath.toAbsolutePath());

			// 检查文件是否存在
			if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
				log.warn("File not found or not readable: {}", filePath.toAbsolutePath());
				return ResponseEntity.notFound().build();
			}

			Resource resource = new FileSystemResource(filePath);

			// 根据文件扩展名设置 Content-Type
			String contentType = getContentType(filePath.getFileName().toString());
			MediaType mediaType = MediaType.parseMediaType(contentType);

			return ResponseEntity.ok()
					.contentType(mediaType)
					.body(resource);
		} catch (Exception e) {
			log.error("Error serving static file", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 目录访问重定向到 index.html
	 */
	@Operation(summary = "目录访问重定向")
	@GetMapping("/{deployKey}")
	public ResponseEntity<Void> redirectToIndex(@PathVariable String deployKey) {
		return ResponseEntity.status(302)
				.header("Location", "/static/html/" + deployKey + "/index.html")
				.build();
	}

	/**
	 * 根据文件扩展名获取 Content-Type
	 */
	private String getContentType(String filename) {
		if (filename.endsWith(".html")) {
			return "text/html;charset=UTF-8";
		} else if (filename.endsWith(".css")) {
			return "text/css;charset=UTF-8";
		} else if (filename.endsWith(".js")) {
			return "application/javascript;charset=UTF-8";
		} else if (filename.endsWith(".json")) {
			return "application/json;charset=UTF-8";
		} else if (filename.endsWith(".png")) {
			return "image/png";
		} else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (filename.endsWith(".gif")) {
			return "image/gif";
		} else if (filename.endsWith(".svg")) {
			return "image/svg+xml";
		} else if (filename.endsWith(".ico")) {
			return "image/x-icon";
		} else if (filename.endsWith(".woff")) {
			return "font/woff";
		} else if (filename.endsWith(".woff2")) {
			return "font/woff2";
		} else if (filename.endsWith(".ttf")) {
			return "font/ttf";
		} else if (filename.endsWith(".eot")) {
			return "application/vnd.ms-fontobject";
		}
		return "application/octet-stream";
	}
}
