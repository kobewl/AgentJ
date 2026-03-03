package com.wangliang.agentj.codegen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

/**
 * 文件存储服务
 * 负责管理生成的代码文件存储
 */
@Service
public class FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

	@Value("${agentj.codegen.output-dir:./tmp/code_output}")
	private String outputDir;

	@Value("${agentj.codegen.deploy-dir:./tmp/code_deploy}")
	private String deployDir;

	@PostConstruct
	public void init() {
		try {
			Path outputPath = Paths.get(outputDir);
			Path deployPath = Paths.get(deployDir);

			if (!Files.exists(outputPath)) {
				Files.createDirectories(outputPath);
			}
			if (!Files.exists(deployPath)) {
				Files.createDirectories(deployPath);
			}

			log.info("FileStorageService initialized. outputDir={}, deployDir={}",
					outputPath.toAbsolutePath(), deployPath.toAbsolutePath());
		} catch (IOException e) {
			log.error("Failed to initialize directories", e);
			throw new RuntimeException("Failed to initialize file storage directories", e);
		}
	}

	/**
	 * 保存代码文件
	 *
	 * @param appId     应用 ID
	 * @param deployKey 部署标识
	 * @param content   文件内容
	 * @return 文件路径
	 */
	public String saveHtmlFile(Long appId, String deployKey, String content) {
		try {
			Path appDir = Paths.get(outputDir, String.valueOf(appId));
			if (!Files.exists(appDir)) {
				Files.createDirectories(appDir);
			}

			Path filePath = appDir.resolve("index.html");
			Files.writeString(filePath, content, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			log.info("Saved HTML file for app {}: {}", appId, filePath.toAbsolutePath());
			return filePath.toString();
		} catch (IOException e) {
			log.error("Failed to save HTML file for app {}", appId, e);
			throw new RuntimeException("Failed to save HTML file", e);
		}
	}

	/**
	 * 读取代码文件
	 *
	 * @param appId 应用 ID
	 * @return 文件内容
	 */
	public String readHtmlFile(Long appId) {
		try {
			Path filePath = Paths.get(outputDir, String.valueOf(appId), "index.html");
			if (!Files.exists(filePath)) {
				return null;
			}
			return Files.readString(filePath, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to read HTML file for app {}", appId, e);
			throw new RuntimeException("Failed to read HTML file", e);
		}
	}

	/**
	 * 部署应用（复制到部署目录）
	 *
	 * @param appId     应用 ID
	 * @param deployKey 部署标识
	 * @return 部署路径
	 */
	public String deployApp(Long appId, String deployKey) {
		try {
			Path sourceDir = Paths.get(outputDir, String.valueOf(appId));
			if (!Files.exists(sourceDir)) {
				throw new RuntimeException("Source directory not found for app " + appId);
			}

			Path targetDir = Paths.get(deployDir, deployKey);
			if (Files.exists(targetDir)) {
				// 删除旧版本
				try (Stream<Path> walk = Files.walk(targetDir)) {
					walk.sorted((a, b) -> b.compareTo(a))
							.forEach(path -> {
								try {
									Files.delete(path);
								} catch (IOException e) {
									log.warn("Failed to delete file: {}", path);
								}
							});
				}
			}
			Files.createDirectories(targetDir);

			// 复制文件
			Path sourceFile = sourceDir.resolve("index.html");
			Path targetFile = targetDir.resolve("index.html");
			Files.copy(sourceFile, targetFile);

			log.info("Deployed app {} to {}", appId, targetDir.toAbsolutePath());
			return targetDir.toString();
		} catch (IOException e) {
			log.error("Failed to deploy app {}", appId, e);
			throw new RuntimeException("Failed to deploy app", e);
		}
	}

	/**
	 * 删除应用文件
	 *
	 * @param appId     应用 ID
	 * @param deployKey 部署标识（可为空）
	 */
	public void deleteAppFiles(Long appId, String deployKey) {
		try {
			// 删除输出目录中的文件
			Path appDir = Paths.get(outputDir, String.valueOf(appId));
			if (Files.exists(appDir)) {
				try (Stream<Path> walk = Files.walk(appDir)) {
					walk.sorted((a, b) -> b.compareTo(a))
							.forEach(path -> {
								try {
									Files.delete(path);
								} catch (IOException e) {
									log.warn("Failed to delete file: {}", path);
								}
							});
				}
				log.info("Deleted output files for app {}", appId);
			}

			// 删除部署目录中的文件
			if (deployKey != null && !deployKey.isBlank()) {
				Path deployPath = Paths.get(deployDir, deployKey);
				if (Files.exists(deployPath)) {
					try (Stream<Path> walk = Files.walk(deployPath)) {
						walk.sorted((a, b) -> b.compareTo(a))
								.forEach(path -> {
									try {
										Files.delete(path);
									} catch (IOException e) {
										log.warn("Failed to delete file: {}", path);
									}
								});
					}
					log.info("Deleted deploy files for deployKey {}", deployKey);
				}
			}
		} catch (IOException e) {
			log.error("Failed to delete files for app {}", appId, e);
		}
	}

	/**
	 * 获取部署目录
	 *
	 * @return 部署目录路径
	 */
	public Path getDeployDir() {
		return Paths.get(deployDir);
	}

	/**
	 * 获取输出目录
	 *
	 * @return 输出目录路径
	 */
	public Path getOutputDir() {
		return Paths.get(outputDir);
	}

	/**
	 * 检查部署的文件是否存在
	 *
	 * @param deployKey 部署标识
	 * @return 是否存在
	 */
	public boolean isDeployed(String deployKey) {
		Path deployPath = Paths.get(deployDir, deployKey, "index.html");
		return Files.exists(deployPath);
	}

	/**
	 * 读取部署的文件
	 *
	 * @param deployKey 部署标识
	 * @return 文件内容
	 */
	public String readDeployedFile(String deployKey) {
		try {
			Path filePath = Paths.get(deployDir, deployKey, "index.html");
			if (!Files.exists(filePath)) {
				return null;
			}
			return Files.readString(filePath, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to read deployed file for deployKey {}", deployKey, e);
			throw new RuntimeException("Failed to read deployed file", e);
		}
	}
}
