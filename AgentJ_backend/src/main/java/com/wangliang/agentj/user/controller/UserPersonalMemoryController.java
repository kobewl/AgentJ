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
package com.wangliang.agentj.user.controller;

import com.wangliang.agentj.user.model.vo.UserPersonalMemory;
import com.wangliang.agentj.user.service.UserPersonalMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personal-memories")
public class UserPersonalMemoryController {

	private static final Logger log = LoggerFactory.getLogger(UserPersonalMemoryController.class);

	private final UserPersonalMemoryService memoryService;

	public UserPersonalMemoryController(UserPersonalMemoryService memoryService) {
		this.memoryService = memoryService;
	}

	@GetMapping("/{userId}")
	public ResponseEntity<List<UserPersonalMemory>> list(@PathVariable Long userId) {
		return ResponseEntity.ok(memoryService.listByUser(userId));
	}

	@GetMapping("/{userId}/{memoryKey}")
	public ResponseEntity<UserPersonalMemory> get(@PathVariable Long userId, @PathVariable String memoryKey) {
		return memoryService.getByUserAndKey(userId, memoryKey).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{userId}")
	public ResponseEntity<UserPersonalMemory> save(@PathVariable Long userId,
			@RequestBody UserPersonalMemory memory) {
		memory.setUserId(userId);
		UserPersonalMemory saved = memoryService.saveOrUpdate(memory);
		return ResponseEntity.ok(saved);
	}

	@DeleteMapping("/{userId}/{memoryKey}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable Long userId, @PathVariable String memoryKey) {
		log.info("Deleting personal memory {} for user {}", memoryKey, userId);
		memoryService.delete(userId, memoryKey);
		return ResponseEntity.ok(Map.of("deleted", true));
	}

	@PostMapping("/{userId}/mark-used/{memoryKey}")
	public ResponseEntity<Map<String, Object>> markUsed(@PathVariable Long userId, @PathVariable String memoryKey) {
		memoryService.markUsed(userId, memoryKey);
		return ResponseEntity.ok(Map.of("updated", true));
	}

}
