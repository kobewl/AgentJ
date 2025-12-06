-- Add missing plan_execution_id to agent_execution_record to align with JPA mapping
ALTER TABLE agent_execution_record
    ADD COLUMN plan_execution_id BIGINT(20) NULL COMMENT '关联计划执行记录ID' AFTER model_name;

-- Index for the new FK column
CREATE INDEX idx_agent_execution_plan_id ON agent_execution_record (plan_execution_id);

-- Foreign key to plan_execution_record
ALTER TABLE agent_execution_record
    ADD CONSTRAINT fk_agent_execution_plan FOREIGN KEY (plan_execution_id)
        REFERENCES plan_execution_record (id)
        ON DELETE CASCADE ON UPDATE CASCADE;
