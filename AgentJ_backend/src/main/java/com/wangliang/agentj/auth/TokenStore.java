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
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very lightweight in-memory token store. Not suitable for distributed deployments.
 */
@Component
public class TokenStore {

	private static final Logger log = LoggerFactory.getLogger(TokenStore.class);

	private static final long DEFAULT_TTL_SECONDS = 24 * 3600;

	private final Map<String, TokenEntry> tokenMap = new ConcurrentHashMap<>();

	public String generateToken(Long userId) {
		String token = UUID.randomUUID().toString();
		long expireAt = Instant.now().getEpochSecond() + DEFAULT_TTL_SECONDS;
		tokenMap.put(token, new TokenEntry(userId, expireAt));
		return token;
	}

	public Long validateAndGetUser(String token) {
		if (token == null) {
			return null;
		}
		TokenEntry entry = tokenMap.get(token);
		if (entry == null) {
			return null;
		}
		long now = Instant.now().getEpochSecond();
		if (entry.expireAt < now) {
			tokenMap.remove(token);
			log.warn("Token expired and removed");
			return null;
		}
		return entry.userId;
	}

	public void invalidate(String token) {
		if (token != null) {
			tokenMap.remove(token);
		}
	}

	private record TokenEntry(Long userId, long expireAt) {
	}

}
