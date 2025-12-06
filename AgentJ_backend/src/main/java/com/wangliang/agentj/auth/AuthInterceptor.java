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

import com.wangliang.agentj.user.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Set;

public class AuthInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

	private static final Set<String> WHITELIST = Set.of(
			"/api/auth/login",
			"/api/auth/register",
			"/api/auth/logout",
			"/swagger-ui.html",
			"/swagger-ui",
			"/v3/api-docs",
			"/error"
	);

	private final TokenStore tokenStore;

	public AuthInterceptor(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (path == null) {
			path = request.getRequestURI();
		}
		if (isWhitelisted(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		String authHeader = request.getHeader("Authorization");
		String token = extractToken(authHeader);
		Long userId = tokenStore.validateAndGetUser(token);
		if (userId == null) {
			log.warn("Unauthorized request path={}, token={}", path, token);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		UserContextHolder.setUserId(userId);
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		UserContextHolder.clear();
	}

	private boolean isWhitelisted(String path) {
		return WHITELIST.stream().anyMatch(path::startsWith);
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
