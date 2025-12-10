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
package com.wangliang.agentj.config;

import com.wangliang.agentj.config.entity.ConfigEntity;
import com.wangliang.agentj.model.entity.DynamicModelEntity;
import com.wangliang.agentj.model.repository.DynamicModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

	@Autowired
	private IConfigService configService;

	@Autowired
	private DynamicModelRepository dynamicModelRepository;

	@GetMapping("/group/{groupName}")
	public ResponseEntity<List<ConfigEntity>> getConfigsByGroup(@PathVariable("groupName") String groupName) {
		return ResponseEntity.ok(configService.getConfigsByGroup(groupName));
	}

	@PostMapping("/batch-update")
	public ResponseEntity<Void> batchUpdateConfigs(@RequestBody List<ConfigEntity> configs) {
		configService.batchUpdateConfigs(configs);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/reset-all-defaults")
	public ResponseEntity<Void> resetAllConfigsToDefaults() {
		configService.resetAllConfigsToDefaults();
		return ResponseEntity.ok().build();
	}

	@GetMapping("/groups")
	public ResponseEntity<List<String>> listConfigGroups() {
		return ResponseEntity.ok(configService.listConfigGroups());
	}

	@GetMapping("/groups/{groupName}/sub-groups")
	public ResponseEntity<List<String>> listConfigSubGroups(@PathVariable("groupName") String groupName) {
		return ResponseEntity.ok(configService.listConfigSubGroups(groupName));
	}

	@GetMapping("/available-models")
	public ResponseEntity<Map<String, Object>> getAvailableModels() {
		List<DynamicModelEntity> models = dynamicModelRepository.findAll();

		List<Map<String, Object>> modelOptions = models.stream().map(model -> {
			Map<String, Object> option = new HashMap<>();
			option.put("value", model.getId().toString());
			option.put("label", model.getModelName() + " (" + model.getModelDescription() + ")");
			return option;
		}).collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("options", modelOptions);
		response.put("total", modelOptions.size());

		return ResponseEntity.ok(response);
	}

}
