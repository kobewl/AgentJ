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

CREATE TABLE `cron_task` (
                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             `cron_name` varchar(255) NOT NULL COMMENT '定时任务名称',
                             `cron_time` varchar(100) NOT NULL COMMENT 'Cron表达式（如：0 0 1 * * ?）',
                             `plan_desc` varchar(500) NOT NULL COMMENT '任务计划描述',
                             `status` int(11) NOT NULL COMMENT '任务状态（0-禁用，1-启用，可扩展其他状态）',
                             `create_time` datetime NOT NULL COMMENT '任务创建时间',
                             `last_executed_time` datetime DEFAULT NULL COMMENT '任务最后执行时间',
                             `plan_template_id` varchar(64) DEFAULT NULL COMMENT '计划模板ID（关联外部模板表）',
                             PRIMARY KEY (`id`),
                             KEY `idx_cron_task_status` (`status`),
                             KEY `idx_cron_task_create_time` (`create_time`),
                             KEY `idx_cron_task_template_id` (`plan_template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务配置表';

CREATE TABLE `datasource_config` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `name` varchar(255) NOT NULL COMMENT '数据源名称（全局唯一）',
                                     `type` varchar(50) NOT NULL COMMENT '数据源类型（如：mysql、postgresql、oracle等）',
                                     `enable` tinyint(1) NOT NULL COMMENT '是否启用（0-禁用，1-启用）',
                                     `url` varchar(1000) NOT NULL COMMENT '数据库连接URL（如：jdbc:mysql://localhost:3306/test）',
                                     `driver_class_name` varchar(255) NOT NULL COMMENT '数据库驱动类名（如：com.mysql.cj.jdbc.Driver）',
                                     `username` varchar(255) NOT NULL COMMENT '数据库用户名',
                                     `password` varchar(255) NOT NULL COMMENT '数据库密码（建议加密存储）',
                                     `created_at` datetime NOT NULL COMMENT '创建时间',
                                     `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_ds_config_name` (`name`) COMMENT '数据源名称唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- auto-generated definition
create table system_config
(
    id               bigint auto_increment comment '主键ID'
        primary key,
    config_group     varchar(100) not null comment '配置分组（如：system、model、cron）',
    config_sub_group varchar(100) not null comment '配置子分组（细分维度）',
    config_key       varchar(200) not null comment '配置项键名',
    config_path      varchar(500) not null comment '配置项全路径（全局唯一）',
    config_value     text         null comment '配置项值（支持大文本）',
    default_value    text         null comment '配置项默认值',
    description      text         null comment '配置项描述',
    input_type       varchar(50)  not null comment '输入类型（枚举：INPUT/SELECT/TEXTAREA等）',
    options_json     text         null comment 'SELECT类型的选项数据（JSON字符串）',
    update_time      datetime     not null comment '最后更新时间',
    create_time      datetime     not null comment '创建时间',
    constraint uk_config_path
        unique (config_path) comment '配置全路径唯一索引'
)
    comment '系统配置表';

create index idx_config_group_subgroup
    on system_config (config_group, config_sub_group)
    comment '分组+子分组联合索引';

create index idx_config_update_time
    on system_config (update_time)
    comment '更新时间索引';

CREATE TABLE `users` (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
                         `username` varchar(100) NOT NULL COMMENT '用户名（登录账号，全局唯一）',
                         `email` varchar(255) NOT NULL COMMENT '用户邮箱（全局唯一）',
                         `display_name` varchar(255) DEFAULT NULL COMMENT '用户展示名称（昵称）',
                         `created_at` datetime NOT NULL COMMENT '用户创建时间',
                         `last_login` datetime DEFAULT NULL COMMENT '最后登录时间',
                         `status` varchar(50) NOT NULL COMMENT '用户状态（active-活跃/inactive-未激活/locked-锁定等）',
                         `current_conversation_id` varchar(64) DEFAULT NULL COMMENT '当前会话ID',
                         `language` varchar(20) DEFAULT NULL COMMENT '用户语言设置（如：zh-CN/en-US）',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_user_username` (`username`) COMMENT '用户名唯一索引',
                         UNIQUE KEY `uk_user_email` (`email`) COMMENT '用户邮箱唯一索引',
                         KEY `idx_user_status` (`status`) COMMENT '用户状态索引',
                         KEY `idx_user_created_at` (`created_at`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

CREATE TABLE `user_preferences` (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `user_id` bigint(20) NOT NULL COMMENT '关联用户ID',
                                    `preference` varchar(255) NOT NULL COMMENT '用户偏好项（如：theme-dark、notify-on、lang-zh等）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user_preference_user_id` (`user_id`) COMMENT '用户ID关联索引',
                                    CONSTRAINT `fk_user_preference_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好配置表（一对多关联）';

CREATE TABLE `coordinator_tools` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `plan_template_id` varchar(50) NOT NULL COMMENT '计划模板ID（全局唯一）',
                                     `tool_name` varchar(3000) NOT NULL COMMENT '工具名称',
                                     `tool_description` varchar(200) NOT NULL COMMENT '工具描述',
                                     `input_schema` varchar(2048) DEFAULT NULL COMMENT '输入参数Schema（JSON格式）',
                                     `enable_internal_toolcall` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用内部工具调用（0-否，1-是）',
                                     `enable_http_service` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用HTTP服务（0-否，1-是）',
                                     `enable_in_conversation` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否在会话中启用（0-否，1-是）',
                                     `enable_mcp_service` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用MCP服务（0-否，1-是）',
                                     `service_group` varchar(100) DEFAULT NULL COMMENT '服务分组',
                                     `access_level` varchar(50) DEFAULT 'EDITABLE' COMMENT '访问级别（EDITABLE-可编辑/READ_ONLY-只读等）',
                                     `create_time` datetime NOT NULL COMMENT '创建时间',
                                     `update_time` datetime NOT NULL COMMENT '更新时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_coor_tool_plan_template_id` (`plan_template_id`) COMMENT '计划模板ID唯一索引',
    -- 精准计算：(3072 - 100*4) /4 = 643 → tool_name取前643字符，总长度=100*4 + 643*4 = 3072
                                     UNIQUE KEY `uk_coor_tool_service_group_name` (`service_group`, `tool_name`(643)) COMMENT '服务分组+工具名称（前643字符）联合唯一',
                                     KEY `idx_coor_tool_service_group` (`service_group`) COMMENT '服务分组索引',
                                     KEY `idx_coor_tool_access_level` (`access_level`) COMMENT '访问级别索引',
                                     KEY `idx_coor_tool_create_time` (`create_time`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='协调器工具/计划模板配置表';

CREATE TABLE `plan_template_version` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `plan_template_id` varchar(50) NOT NULL COMMENT '关联的计划模板ID',
                                         `version_index` int(11) NOT NULL COMMENT '版本序号（如1、2、3，同一模板递增）',
                                         `plan_json` text NOT NULL COMMENT '计划模板的JSON内容（完整版本）',
                                         `create_time` datetime NOT NULL COMMENT '版本创建时间',
                                         PRIMARY KEY (`id`),
    -- 联合唯一索引：保证同一模板的版本序号不重复
                                         UNIQUE KEY `uk_plan_template_version` (`plan_template_id`, `version_index`),
    -- 普通索引：加速按模板ID查询所有版本、按创建时间排序
                                         KEY `idx_plan_template_id_create_time` (`plan_template_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划模板版本记录表';