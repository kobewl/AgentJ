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

CREATE TABLE `dynamic_memories` (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `conversation_id` varchar(64) NOT NULL COMMENT '对话ID（关联会话标识）',
                                    `memory_name` varchar(255) NOT NULL COMMENT '记忆名称（对话名称）',
                                    `create_time` datetime NOT NULL COMMENT '创建时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_dynamic_memories_conversation_id` (`conversation_id`) COMMENT '对话ID唯一索引',
                                    KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引（匹配实体类注解）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话记忆主表（存储会话元数据）';

CREATE TABLE `memory_plan_mappings` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `memory_id` bigint(20) NOT NULL COMMENT '关联的记忆主表ID',
                                        `root_plan_id` varchar(64) NOT NULL COMMENT '根计划ID（对应对话轮次的计划执行记录）',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_memory_plan_mapping` (`memory_id`, `root_plan_id`) COMMENT '同一记忆下的根计划ID唯一',
                                        KEY `idx_memory_id` (`memory_id`) COMMENT '记忆ID关联索引',
                                        CONSTRAINT `fk_memory_plan_mapping_memory` FOREIGN KEY (`memory_id`) REFERENCES `dynamic_memories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记忆-根计划ID映射表（一对多关联）';

CREATE TABLE `root_task_manager` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `root_plan_id` varchar(255) NOT NULL COMMENT '根计划ID（任务层级的主标识，全局唯一）',
                                     `desired_task_state` varchar(50) NOT NULL COMMENT '用户期望的任务状态（START/STOP/PAUSE/RESUME/CANCEL/WAIT）',
                                     `task_result` text DEFAULT NULL COMMENT '任务执行结果（汇总+详情）',
                                     `start_time` datetime DEFAULT NULL COMMENT '任务执行开始时间',
                                     `end_time` datetime DEFAULT NULL COMMENT '任务执行结束时间',
                                     `last_updated` datetime DEFAULT NULL COMMENT '任务最后更新时间',
                                     `created_at` datetime NOT NULL COMMENT '任务创建时间',
                                     `created_by` varchar(100) DEFAULT NULL COMMENT '任务创建人',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_root_task_manager_root_plan_id` (`root_plan_id`) COMMENT '根计划ID唯一索引',
                                     KEY `idx_desired_task_state` (`desired_task_state`) COMMENT '任务状态索引（用于筛选不同状态的任务）',
                                     KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引（用于按时间筛选任务）',
                                     KEY `idx_last_updated` (`last_updated`) COMMENT '最后更新时间索引（用于追踪任务更新）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='根任务管理表（核心任务协调器）';

CREATE TABLE `plan_execution_record` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录主键ID',
                                         `current_plan_id` varchar(64) NOT NULL COMMENT '当前计划ID（全局唯一）',
                                         `root_plan_id` varchar(64) DEFAULT NULL COMMENT '根计划ID（子计划关联主计划）',
                                         `parent_plan_id` varchar(64) DEFAULT NULL COMMENT '父计划ID（子计划关联父计划）',
                                         `title` varchar(255) DEFAULT NULL COMMENT '计划标题',
                                         `user_request` longtext DEFAULT NULL COMMENT '用户原始请求（大文本）',
                                         `start_time` datetime DEFAULT NULL COMMENT '执行开始时间',
                                         `end_time` datetime DEFAULT NULL COMMENT '执行结束时间',
                                         `current_step_index` int(11) DEFAULT NULL COMMENT '当前执行步骤索引',
                                         `completed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否执行完成（0-否，1-是）',
                                         `summary` longtext DEFAULT NULL COMMENT '执行汇总结果（大文本）',
                                         `tool_call_id` varchar(64) DEFAULT NULL COMMENT '触发当前计划的工具调用ID（子计划用）',
                                         `model_name` varchar(100) DEFAULT NULL COMMENT '实际调用的模型名称',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_plan_execution_current_plan_id` (`current_plan_id`) COMMENT '当前计划ID唯一索引',
                                         KEY `idx_root_plan_id` (`root_plan_id`) COMMENT '根计划ID索引（查询主计划下所有子计划）',
                                         KEY `idx_parent_plan_id` (`parent_plan_id`) COMMENT '父计划ID索引（查询父计划下所有子计划）',
                                         KEY `idx_completed` (`completed`) COMMENT '执行状态索引（筛选完成/未完成计划）',
                                         KEY `idx_start_time` (`start_time`) COMMENT '开始时间索引（按执行时间筛选）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划执行记录表（核心执行轨迹）';

CREATE TABLE `plan_execution_steps` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '步骤记录ID',
                                        `plan_execution_id` bigint(20) NOT NULL COMMENT '关联计划执行记录ID',
                                        `step` longtext NOT NULL COMMENT '步骤内容（大文本）',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_plan_execution_steps_execution_id` (`plan_execution_id`) COMMENT '计划执行记录关联索引',
                                        CONSTRAINT `fk_plan_execution_steps_record` FOREIGN KEY (`plan_execution_id`) REFERENCES `plan_execution_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划执行步骤明细表';

CREATE TABLE `agent_execution_record` (
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录主键ID',
                                          `step_id` varchar(64) DEFAULT NULL COMMENT '所属步骤ID（全局唯一）',
                                          `agent_name` varchar(100) DEFAULT NULL COMMENT '智能体名称',
                                          `agent_description` longtext DEFAULT NULL COMMENT '智能体描述（大文本）',
                                          `start_time` datetime DEFAULT NULL COMMENT '执行开始时间',
                                          `end_time` datetime DEFAULT NULL COMMENT '执行结束时间',
                                          `max_steps` int(11) NOT NULL DEFAULT 0 COMMENT '最大允许执行步骤数',
                                          `current_step` int(11) NOT NULL DEFAULT 0 COMMENT '当前执行步骤数',
                                          `status` varchar(50) DEFAULT NULL COMMENT '执行状态（IDLE/RUNNING/FINISHED等）',
                                          `agent_request` longtext DEFAULT NULL COMMENT '智能体执行请求内容（大文本）',
                                          `result` longtext DEFAULT NULL COMMENT '执行结果（大文本）',
                                          `error_message` longtext DEFAULT NULL COMMENT '错误信息（大文本）',
                                          `model_name` varchar(100) DEFAULT NULL COMMENT '实际调用的模型名称',
                                          `plan_execution_id` bigint(20) DEFAULT NULL COMMENT '关联计划执行记录ID',
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_agent_execution_step_id` (`step_id`) COMMENT '步骤ID唯一索引',
                                          KEY `idx_step_id` (`step_id`) COMMENT '步骤ID索引（匹配实体类注解）',
                                          KEY `idx_agent_name` (`agent_name`) COMMENT '智能体名称索引（按智能体筛选记录）',
                                          KEY `idx_status` (`status`) COMMENT '执行状态索引（筛选不同状态的执行记录）',
                                          KEY `idx_start_time` (`start_time`) COMMENT '开始时间索引（按执行时间筛选）',
                                          KEY `idx_agent_execution_plan_id` (`plan_execution_id`) COMMENT '计划执行记录关联索引',
                                          CONSTRAINT `fk_agent_execution_plan` FOREIGN KEY (`plan_execution_id`) REFERENCES `plan_execution_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体执行记录表（Agent执行轨迹）';

CREATE TABLE `think_act_record` (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录主键ID',
                                    `think_act_id` varchar(64) DEFAULT NULL COMMENT '思考-行动唯一标识ID',
                                    `parent_execution_id` bigint(20) DEFAULT NULL COMMENT '关联的智能体执行记录ID（父级ID）',
                                    `think_input` longtext DEFAULT NULL COMMENT '思考阶段输入内容（大文本）',
                                    `think_output` longtext DEFAULT NULL COMMENT '思考阶段输出结果（大文本）',
                                    `error_message` longtext DEFAULT NULL COMMENT '执行错误信息（大文本）',
                                    `input_char_count` int(11) DEFAULT NULL COMMENT '输入字符数（发送给LLM的总字符数）',
                                    `output_char_count` int(11) DEFAULT NULL COMMENT '输出字符数（LLM响应的总字符数）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_think_act_parent_execution_id` (`parent_execution_id`) COMMENT '父级智能体执行记录ID索引',
                                    KEY `idx_think_act_id` (`think_act_id`) COMMENT '思考-行动ID索引（按标识查询）',
                                    CONSTRAINT `fk_think_act_record_agent_execution` FOREIGN KEY (`parent_execution_id`) REFERENCES `agent_execution_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体思考-行动记录表（Think-Act执行轨迹）';

CREATE TABLE `act_tool_info` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '工具调用记录主键ID',
                                 `name` varchar(100) DEFAULT NULL COMMENT '工具名称',
                                 `parameters` longtext DEFAULT NULL COMMENT '工具参数（序列化内容，大文本）',
                                 `result` longtext DEFAULT NULL COMMENT '工具执行结果（大文本）',
                                 `tool_call_id` varchar(64) DEFAULT NULL COMMENT '工具调用ID（唯一标识）',
                                 `think_act_record_id` bigint(20) DEFAULT NULL COMMENT '关联的思考-行动记录ID（外键，可为空用于独立记录）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_act_tool_think_act_id` (`think_act_record_id`) COMMENT '思考-行动记录关联索引',
                                 KEY `idx_tool_call_id` (`tool_call_id`) COMMENT '工具调用ID索引（按调用ID查询）',
                                 KEY `idx_tool_name` (`name`) COMMENT '工具名称索引（按工具筛选记录）',
                                 CONSTRAINT `fk_act_tool_info_think_act` FOREIGN KEY (`think_act_record_id`) REFERENCES `think_act_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行动工具信息表（记录工具调用详情）';

-- auto-generated definition
create table user_personal_memories
(
    id           bigint auto_increment comment '主键'
        primary key,
    user_id      bigint                                          not null comment '关联 users.id',
    memory_key   varchar(100)                                    not null comment '记忆键/类型，如 nickname/job/custom_instruction',
    title        varchar(255)                                    null comment '可读标题，便于列表展示',
    content_json json                                            not null comment '记忆内容，结构化存储',
    source       enum ('AI', 'MANUAL') default 'AI'              not null comment 'AI 自动提炼 or 用户手动输入',
    confidence   decimal(5, 2)                                   null comment 'AI 置信度，0-1或0-100都可',
    importance   int                                             null comment '重要性/权重，数值越高越重要',
    tags         json                                            null comment '标签数组，便于筛选',
    last_used_at datetime                                        null comment '最近被引用时间',
    created_at   datetime              default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at   datetime              default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_upm_user_key
        unique (user_id, memory_key),
    constraint fk_upm_user
        foreign key (user_id) references users (id)
);

create index idx_upm_user_used
    on user_personal_memories (user_id, last_used_at);


