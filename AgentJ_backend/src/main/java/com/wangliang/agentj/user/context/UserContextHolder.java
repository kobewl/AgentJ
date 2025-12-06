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
package com.wangliang.agentj.user.context;

/**
 * Simple ThreadLocal holder for user id during chat calls.
 */
public final class UserContextHolder {

	private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

	private UserContextHolder() {
	}

	public static void setUserId(Long userId) {
		USER_ID_HOLDER.set(userId);
	}

	public static Long getUserId() {
		return USER_ID_HOLDER.get();
	}

	public static void clear() {
		USER_ID_HOLDER.remove();
	}

}
