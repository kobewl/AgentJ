package com.wangliang.agentj.conversation.entity.dto;

import lombok.Data;

/**
 * 请求生成会话标题
 */
@Data
public class ConversationTitleRequest {

	private String userContent;

	private String assistantContent;

}

