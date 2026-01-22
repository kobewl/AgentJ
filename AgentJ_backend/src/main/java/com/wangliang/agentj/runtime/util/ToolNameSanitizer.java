package com.wangliang.agentj.runtime.util;

import java.util.regex.Pattern;

/**
 * Utility for sanitizing tool names before sending them to LLM providers. Some
 * providers (e.g., OpenAI-compatible APIs) only accept letters, numbers,
 * underscores and hyphens in {@code function.name}. This class enforces that
 * restriction and provides a consistent fallback.
 */
public final class ToolNameSanitizer {

	private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

	private ToolNameSanitizer() {
	}

	/**
	 * Check whether a tool name already satisfies the provider requirement.
	 * @param name original tool name
	 * @return true if the name only contains letters, digits, underscores or hyphens
	 */
	public static boolean isValid(String name) {
		return name != null && ALLOWED_PATTERN.matcher(name).matches();
	}

	/**
	 * Sanitize tool name to contain only letters, digits, underscores or hyphens.
	 * <ul>
	 * <li>Replace every invalid character with an underscore</li>
	 * <li>Collapse repeated separators and trim leading/trailing separators</li>
	 * <li>Ensure non-empty and cap length to 64 characters</li>
	 * </ul>
	 * @param rawName original tool name (may be null/blank)
	 * @return sanitized name safe to send to LLM tool registration
	 */
	public static String sanitize(String rawName) {
		if (rawName == null || rawName.trim().isEmpty()) {
			return "tool";
		}

		String sanitized = rawName.trim().replaceAll("[^A-Za-z0-9_-]", "_");
		sanitized = sanitized.replaceAll("_+", "_").replaceAll("-+", "-");
		sanitized = sanitized.replaceAll("^[_-]+", "").replaceAll("[_-]+$", "");

		if (sanitized.isEmpty()) {
			sanitized = "tool";
		}

		if (sanitized.length() > 64) {
			sanitized = sanitized.substring(0, 64);
		}
		return sanitized;
	}

}
