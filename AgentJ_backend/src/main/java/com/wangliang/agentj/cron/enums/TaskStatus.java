package com.wangliang.agentj.cron.enums;

/**
 * Scheduled task status enumeration
 */
public enum TaskStatus {

	/**
	 * Enabled status
	 */
	ENABLED(0, "Enabled"),

	/**
	 * Disabled status
	 */
	DISABLED(1, "Disabled");

	private final Integer code;

	private final String description;

	TaskStatus(Integer code, String description) {
		this.code = code;
		this.description = description;
	}

	public Integer getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public static TaskStatus fromCode(Integer code) {
		for (TaskStatus status : TaskStatus.values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown task status code: " + code);
	}

}
