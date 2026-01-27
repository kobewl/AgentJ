-- 代码生成功能数据库表结构
-- 创建时间: 2025-01-27

-- 代码生成应用表
CREATE TABLE IF NOT EXISTS code_app (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '应用ID',
    app_name VARCHAR(256) NOT NULL COMMENT '应用名称',
    cover VARCHAR(512) COMMENT '应用封面',
    init_prompt TEXT COMMENT '初始化 prompt',
    code_gen_type VARCHAR(64) NOT NULL DEFAULT 'HTML' COMMENT '生成类型：HTML',
    deploy_key VARCHAR(64) UNIQUE COMMENT '部署标识',
    deployed_time DATETIME COMMENT '部署时间',
    user_id BIGINT NOT NULL COMMENT '创建用户',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_deploy_key (deploy_key),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成应用表';

-- 代码生成对话历史表
CREATE TABLE IF NOT EXISTS code_chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    message TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型：user/ai',
    app_id BIGINT NOT NULL COMMENT '应用 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_app_id (app_id),
    INDEX idx_app_id_created_at (app_id, created_at),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成对话历史表';
