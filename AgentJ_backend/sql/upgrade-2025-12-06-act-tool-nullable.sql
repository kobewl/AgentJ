-- Allow act_tool_info to be created without a parent think_act_record when needed
ALTER TABLE act_tool_info
    MODIFY think_act_record_id BIGINT(20) NULL COMMENT '关联的思考-行动记录ID（外键，可为空用于独立记录）';
