package com.wangliang.agentj.planning.service;

import java.util.List;

/**
 * Plan template service interface providing business logic related to plan templates
 */
public interface IPlanTemplateService {

	/**
	 * Save to version history
	 * @param planTemplateId Template ID
	 * @param planJson Plan JSON
	 * @return Version save result
	 */
	PlanTemplateService.VersionSaveResult saveToVersionHistory(String planTemplateId, String planJson);

	/**
	 * Save version to history
	 * @param planTemplateId Template ID
	 * @param planJson Plan JSON
	 */
	void saveVersionToHistory(String planTemplateId, String planJson);

	/**
	 * Get plan version list
	 * @param planTemplateId Template ID
	 * @return Version list
	 */
	List<String> getPlanVersions(String planTemplateId);

	/**
	 * Get plan of specified version
	 * @param planTemplateId Template ID
	 * @param versionIndex Version index
	 * @return Plan JSON
	 */
	String getPlanVersion(String planTemplateId, int versionIndex);

	/**
	 * Get latest version plan
	 * @param planTemplateId Template ID
	 * @return Plan JSON
	 */
	String getLatestPlanVersion(String planTemplateId);

	/**
	 * Check if content is same as latest version
	 * @param planTemplateId Template ID
	 * @param planJson Plan JSON
	 * @return Whether same
	 */
	boolean isContentSameAsLatestVersion(String planTemplateId, String planJson);

	/**
	 * Check if JSON content is equivalent
	 * @param json1 JSON1
	 * @param json2 JSON2
	 * @return Whether equivalent
	 */
	boolean isJsonContentEquivalent(String json1, String json2);

}
