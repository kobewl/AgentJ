package com.wangliang.agentj.planning.repository;

import com.wangliang.agentj.planning.model.po.PlanTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The data access interface for the plan template version
 */
@Repository
public interface PlanTemplateVersionRepository extends JpaRepository<PlanTemplateVersion, Long> {

	/**
	 * Find all versions of the plan template by the plan template ID, sorted by the
	 * version index
	 * @param planTemplateId the plan template ID
	 * @return the list of versions
	 */
	List<PlanTemplateVersion> findByPlanTemplateIdOrderByVersionIndexAsc(String planTemplateId);

	/**
	 * Find the maximum version index of the plan template by the plan template ID
	 * @param planTemplateId the plan template ID
	 * @return the maximum version index, or null if there is no version
	 */
	@Query("SELECT MAX(v.versionIndex) FROM PlanTemplateVersion v WHERE v.planTemplateId = :planTemplateId")
	Integer findMaxVersionIndexByPlanTemplateId(@Param("planTemplateId") String planTemplateId);

	/**
	 * Find the specific version of the plan template by the plan template ID and the
	 * version index
	 * @param planTemplateId the plan template ID
	 * @param versionIndex the version index
	 * @return the plan template version entity
	 */
	PlanTemplateVersion findByPlanTemplateIdAndVersionIndex(String planTemplateId, Integer versionIndex);

	/**
	 * Delete all versions of the plan template by the plan template ID
	 * @param planTemplateId the plan template ID
	 */
	void deleteByPlanTemplateId(String planTemplateId);

}
