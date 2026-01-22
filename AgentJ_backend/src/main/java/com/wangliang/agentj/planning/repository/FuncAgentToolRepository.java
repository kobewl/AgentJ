package com.wangliang.agentj.planning.repository;

import com.wangliang.agentj.planning.model.po.FuncAgentToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Coordinator Tool Data Access Layer
 */
@Repository
public interface FuncAgentToolRepository extends JpaRepository<FuncAgentToolEntity, Long> {

	/**
	 * Find by plan template ID
	 */
	List<FuncAgentToolEntity> findByPlanTemplateId(String planTemplateId);

	/**
	 * Find by tool name
	 */
	List<FuncAgentToolEntity> findByToolName(String toolName);

	/**
	 * Find by service group and tool name (respects unique constraint)
	 */
	Optional<FuncAgentToolEntity> findByServiceGroupAndToolName(String serviceGroup, String toolName);

	/**
	 * Delete by plan template ID
	 */
	void deleteByPlanTemplateId(String planTemplateId);

	/**
	 * Delete by tool name
	 */
	void deleteByToolName(String toolName);

}
