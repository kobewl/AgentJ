create database agentj;

use agentj;

CREATE TABLE `system_config` (
                                 `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
                                 `config_group` varchar(64) NOT NULL COMMENT '配置分组',
                                 `config_sub_group` varchar(64) DEFAULT '' COMMENT '配置子分组',
                                 `config_key` varchar(128) NOT NULL COMMENT '配置键',
                                 `config_path` varchar(256) NOT NULL COMMENT '配置项全路径',
                                 `config_value` text COMMENT '配置值',
                                 `default_value` text COMMENT '默认值',
                                 `description` varchar(512) DEFAULT '' COMMENT '配置描述',
                                 `input_type` varchar(32) NOT NULL COMMENT '输入类型（对应ConfigInputType枚举）',
                                 `options_json` text COMMENT 'SELECT类型选项数据的JSON字符串',
                                 `update_time` datetime NOT NULL COMMENT '最后更新时间',
                                 `create_time` datetime NOT NULL COMMENT '创建时间',
                                 PRIMARY KEY (`id`),
    -- 联合索引：保证配置路径唯一性，提升查询效率
                                 UNIQUE KEY `uk_config_path` (`config_path`),
    -- 普通索引：支持按分组/子分组/键快速查询
                                 KEY `idx_config_group_sub_key` (`config_group`, `config_sub_group`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';


-- 创建MCP配置表
CREATE TABLE `mcp_config` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                              `mcp_server_name` varchar(255) NOT NULL COMMENT 'MCP服务器名称（唯一）',
                              `connection_type` varchar(50) NOT NULL COMMENT '连接类型（枚举值）',
                              `connection_config` varchar(4000) NOT NULL COMMENT '连接配置信息（JSON格式或自定义字符串）',
                              `status` varchar(10) NOT NULL DEFAULT 'ENABLE' COMMENT '配置状态（ENABLE：启用，DISABLE：禁用）',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_mcp_server_name` (`mcp_server_name`) COMMENT 'MCP服务器名称唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP配置表';

CREATE TABLE `dynamic_models` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `base_url` varchar(255) NOT NULL COMMENT '模型基础请求URL',
                                  `api_key` varchar(255) NOT NULL COMMENT '模型调用密钥（存储原始值，展示时脱敏）',
                                  `headers` varchar(2048) DEFAULT NULL COMMENT '请求头信息（JSON格式字符串存储）',
                                  `model_name` varchar(255) NOT NULL COMMENT '模型名称',
                                  `model_description` varchar(1000) NOT NULL COMMENT '模型描述',
                                  `type` varchar(50) NOT NULL COMMENT '模型类型（如：openai、anthropic等）',
                                  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为默认模型（0-否，1-是）',
                                  `temperature` double DEFAULT NULL COMMENT '温度参数（控制生成随机性）',
                                  `top_p` double DEFAULT NULL COMMENT '核采样参数',
                                  `completions_path` varchar(255) DEFAULT NULL COMMENT '补全接口路径（如：/v1/chat/completions）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_dynamic_models_type` (`type`),
                                  KEY `idx_dynamic_models_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态模型配置表';