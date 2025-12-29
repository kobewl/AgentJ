-- 知识库对话历史存储 - 数据库迁移脚本
-- 执行时间: 2025-12-23

-- 1. 添加对话类型字段
ALTER TABLE conversation_sessions 
ADD COLUMN conversation_type VARCHAR(32) DEFAULT 'CHAT' COMMENT '对话类型：CHAT-普通对话，KNOWLEDGE-知识库对话';

-- 2. 添加知识库ID字段
ALTER TABLE conversation_sessions 
ADD COLUMN knowledge_base_id VARCHAR(64) NULL COMMENT '关联知识库ID（知识库对话时使用）';

-- 3. 添加索引
CREATE INDEX idx_conversation_sessions_type ON conversation_sessions(conversation_type);
CREATE INDEX idx_conversation_sessions_kb ON conversation_sessions(knowledge_base_id);

-- 4. 添加外键约束（可选，根据需要启用）
-- ALTER TABLE conversation_sessions
-- ADD CONSTRAINT fk_session_knowledge_base
-- FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_items(id)
-- ON UPDATE CASCADE ON DELETE SET NULL;
