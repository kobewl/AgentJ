package com.wangliang.agentj.conversation.entity.dto;

/**
 * 请求生成会话标题
 */
public class ConversationTitleRequest {

	private String userContent;

	private String assistantContent;

	public String getUserContent() {
		return userContent;
	}

	public void setUserContent(String userContent) {
		this.userContent = userContent;
	}

	public String getAssistantContent() {
		return assistantContent;
	}

	public void setAssistantContent(String assistantContent) {
		this.assistantContent = assistantContent;
	}

}

