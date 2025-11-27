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
package com.wangliang.agentj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@TableName(value = "system_config")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ConfigEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * Configuration group
	 */
	private String configGroup;

	/**
	 * Configuration sub-group
	 */
	private String configSubGroup;

	/**
	 * Configuration key
	 */
	private String configKey;

	/**
	 * Configuration item full path
	 */
	private String configPath;

	/**
	 * Configuration value
	 */
	private String configValue;

	/**
	 * Default value
	 */
	private String defaultValue;

	/**
	 * Configuration description
	 */
	private String description;

	/**
	 * Input type
	 */
	private ConfigInputType inputType;

	/**
	 * Options JSON string for storing SELECT type option data
	 */
	private String optionsJson;

	/**
	 * Last update time
	 */
	private LocalDateTime updateTime;

	/**
	 * Create time
	 */
	private LocalDateTime createTime;

	protected void onCreate() {
		createTime = LocalDateTime.now();
		updateTime = LocalDateTime.now();
	}

	protected void onUpdate() {
		updateTime = LocalDateTime.now();
	}
}
