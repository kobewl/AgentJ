package com.wangliang.agentj.subplan.templates;

import java.util.HashMap;
import java.util.Map;

/**
 * Predefined plan templates for subplan tools
 *
 * Contains all the plan templates that will be automatically created when the application
 * starts
 */
public class SubplanPlanTemplates {

	/**
	 * Get all predefined plan templates
	 * @return Map of template ID to template content
	 */
	public static Map<String, String> getAllPlanTemplates() {
		Map<String, String> templates = new HashMap<>();

		// Content extraction templates
		// templates.put("extract_relevant_content_template",
		// getExtractRelevantContentTemplate());
		// templates.put("extract_relevant_content_template",
		// getExtractRelevantContentTemplateWithDynamicAgent());

		return templates;
	}

}
