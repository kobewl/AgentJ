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

import com.wangliang.agentj.user.model.po.UserEntity;
import com.wangliang.agentj.user.model.vo.User;
import com.wangliang.agentj.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;

	private final BCryptPasswordEncoder passwordEncoder;

	private final TokenStore tokenStore;

	public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, TokenStore tokenStore) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenStore = tokenStore;
	}

	public String login(String usernameOrEmail, String rawPassword) {
		if (!StringUtils.hasText(usernameOrEmail) || !StringUtils.hasText(rawPassword)) {
			throw new IllegalArgumentException("用户名/邮箱与密码不能为空");
		}

		Optional<UserEntity> userOpt = userRepository.findByUsername(usernameOrEmail);
		if (userOpt.isEmpty()) {
			userOpt = userRepository.findByEmail(usernameOrEmail);
		}
		if (userOpt.isEmpty()) {
			throw new IllegalArgumentException("用户不存在");
		}

		UserEntity user = userOpt.get();
		if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new IllegalArgumentException("密码错误");
		}

		user.setLastLogin(LocalDateTime.now());
		userRepository.save(user);

		return tokenStore.generateToken(user.getId());
	}

	public LoginResponse loginWithUserInfo(String usernameOrEmail, String rawPassword) {
		if (!StringUtils.hasText(usernameOrEmail) || !StringUtils.hasText(rawPassword)) {
			throw new IllegalArgumentException("用户名/邮箱与密码不能为空");
		}

		Optional<UserEntity> userOpt = userRepository.findByUsername(usernameOrEmail);
		if (userOpt.isEmpty()) {
			userOpt = userRepository.findByEmail(usernameOrEmail);
		}
		if (userOpt.isEmpty()) {
			throw new IllegalArgumentException("用户不存在");
		}

		UserEntity userEntity = userOpt.get();
		if (!passwordEncoder.matches(rawPassword, userEntity.getPasswordHash())) {
			throw new IllegalArgumentException("密码错误");
		}

		userEntity.setLastLogin(LocalDateTime.now());
		userRepository.save(userEntity);

		String token = tokenStore.generateToken(userEntity.getId());
		
		// Convert UserEntity to User VO
		User user = new User();
		user.setId(userEntity.getId());
		user.setUsername(userEntity.getUsername());
		user.setEmail(userEntity.getEmail());
		user.setDisplayName(userEntity.getDisplayName());
		user.setStatus(userEntity.getStatus());
		user.setCreatedAt(userEntity.getCreatedAt());
		user.setLastLogin(userEntity.getLastLogin());
		user.setPreferences(userEntity.getPreferences());
		
		return new LoginResponse(token, user);
	}

	public String register(String username, String email, String displayName, String rawPassword) {
		if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(rawPassword)) {
			throw new IllegalArgumentException("用户名/邮箱/密码不能为空");
		}
		if (userRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("用户名已存在");
		}
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("邮箱已存在");
		}

		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setEmail(email);
		user.setDisplayName(displayName);
		user.setStatus("active");
		user.setCreatedAt(LocalDateTime.now());
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		userRepository.save(user);

		return tokenStore.generateToken(user.getId());
	}

	public void logout(String token) {
		tokenStore.invalidate(token);
	}

}
