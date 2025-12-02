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

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class ConfigServiceImpl implements IConfigService {

    private final Map<String, ConfigEntity> configs = new ConcurrentHashMap<>();
    private final Map<String, ConfigEntity> defaultConfigs = new ConcurrentHashMap<>();

    public ConfigServiceImpl() {
        // 初始化默认配置
        initializeDefaultConfigs();
    }

    private void initializeDefaultConfigs() {
        // Browser Settings
        createDefaultConfig("manus.browser.headless", "false", "浏览器无头模式", ConfigInputType.CHECKBOX);
        createDefaultConfig("manus.browser.requestTimeout", "180", "浏览器请求超时时间", ConfigInputType.NUMBER);
        
        // General Settings
        createDefaultConfig("manus.general.debugDetail", "false", "调试详细信息", ConfigInputType.CHECKBOX);
        
        // Interaction Settings
        createDefaultConfig("manus.openBrowserAuto", "true", "自动打开浏览器", ConfigInputType.CHECKBOX);
        
        // Agent Settings
        createDefaultConfig("manus.maxSteps", "200", "最大步骤数", ConfigInputType.NUMBER);
        createDefaultConfig("manus.agents.forceOverrideFromYaml", "true", "强制从YAML覆盖配置", ConfigInputType.CHECKBOX);
        createDefaultConfig("manus.agent.userInputTimeout", "300", "用户输入超时时间", ConfigInputType.NUMBER);
        createDefaultConfig("manus.agent.maxMemory", "1000", "最大内存", ConfigInputType.NUMBER);
        
        // 将默认配置复制到当前配置
        for (Map.Entry<String, ConfigEntity> entry : defaultConfigs.entrySet()) {
            configs.put(entry.getKey(), entry.getValue());
        }
    }

    private void createDefaultConfig(String configPath, String defaultValue, String description, ConfigInputType inputType) {
        ConfigEntity config = new ConfigEntity();
        config.setConfigPath(configPath);
        config.setConfigValue(defaultValue);
        config.setDefaultValue(defaultValue);
        config.setDescription(description);
        config.setInputType(inputType);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        
        // 解析group和subGroup
        String[] parts = configPath.split("\\.");
        if (parts.length >= 2) {
            config.setConfigGroup(parts[0]);
            if (parts.length >= 3) {
                config.setConfigSubGroup(parts[1]);
                config.setConfigKey(parts[2]);
            } else {
                config.setConfigKey(parts[1]);
            }
        }
        
        defaultConfigs.put(configPath, config);
    }

    @Override
    public String getConfigValue(String configPath) {
        ConfigEntity config = configs.get(configPath);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public void updateConfig(String configPath, String newValue) {
        if (configPath != null && newValue != null) {
            ConfigEntity config = configs.get(configPath);
            if (config != null) {
                config.setConfigValue(newValue);
                config.setUpdateTime(LocalDateTime.now());
            }
        }
    }

    @Override
    public List<ConfigEntity> getAllConfigs() {
        return new ArrayList<>(configs.values());
    }

    @Override
    public Optional<ConfigEntity> getConfig(String configPath) {
        return Optional.ofNullable(configs.get(configPath));
    }

    @Override
    public void resetConfig(String configPath) {
        if (configPath != null && defaultConfigs.containsKey(configPath)) {
            ConfigEntity defaultConfig = defaultConfigs.get(configPath);
            ConfigEntity currentConfig = configs.get(configPath);
            if (currentConfig != null) {
                currentConfig.setConfigValue(defaultConfig.getDefaultValue());
                currentConfig.setUpdateTime(LocalDateTime.now());
            }
        }
    }

    @Override
    public List<ConfigEntity> getConfigsByGroup(String groupName) {
        List<ConfigEntity> result = new ArrayList<>();
        for (ConfigEntity config : configs.values()) {
            if (groupName.equals(config.getConfigGroup())) {
                result.add(config);
            }
        }
        return result;
    }

    @Override
    public void batchUpdateConfigs(List<ConfigEntity> configs) {
        if (configs != null) {
            for (ConfigEntity config : configs) {
                if (config != null && config.getConfigPath() != null) {
                    ConfigEntity existingConfig = this.configs.get(config.getConfigPath());
                    if (existingConfig != null) {
                        existingConfig.setConfigValue(config.getConfigValue());
                        existingConfig.setUpdateTime(LocalDateTime.now());
                    }
                }
            }
        }
    }

    @Override
    public void resetAllConfigsToDefaults() {
        configs.clear();
        for (Map.Entry<String, ConfigEntity> entry : defaultConfigs.entrySet()) {
            ConfigEntity defaultConfig = entry.getValue();
            ConfigEntity newConfig = new ConfigEntity();
            newConfig.setConfigPath(defaultConfig.getConfigPath());
            newConfig.setConfigValue(defaultConfig.getDefaultValue());
            newConfig.setDefaultValue(defaultConfig.getDefaultValue());
            newConfig.setDescription(defaultConfig.getDescription());
            newConfig.setInputType(defaultConfig.getInputType());
            newConfig.setConfigGroup(defaultConfig.getConfigGroup());
            newConfig.setConfigSubGroup(defaultConfig.getConfigSubGroup());
            newConfig.setConfigKey(defaultConfig.getConfigKey());
            newConfig.setCreateTime(LocalDateTime.now());
            newConfig.setUpdateTime(LocalDateTime.now());
            configs.put(entry.getKey(), newConfig);
        }
    }
}