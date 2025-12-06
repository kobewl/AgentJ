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
package com.wangliang.agentj.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
		try {
			String usernameOrEmail = body.get("username");
			if (usernameOrEmail == null) {
				usernameOrEmail = body.get("email");
			}
			String password = body.get("password");
			String token = authService.login(usernameOrEmail, password);
			return ResponseEntity.ok(Map.of("token", token));
		}
		catch (Exception e) {
			log.warn("Login failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
		try {
			String username = body.get("username");
			String email = body.get("email");
			String displayName = body.getOrDefault("displayName", username);
			String password = body.get("password");
			String token = authService.register(username, email, displayName, password);
			return ResponseEntity.ok(Map.of("token", token));
		}
		catch (Exception e) {
			log.warn("Register failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authHeader) {
		String token = extractToken(authHeader);
		authService.logout(token);
		return ResponseEntity.ok(Map.of("logout", true));
	}

	private String extractToken(String authHeader) {
		if (authHeader == null) {
			return null;
		}
		if (authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}
		return authHeader;
	}

}
