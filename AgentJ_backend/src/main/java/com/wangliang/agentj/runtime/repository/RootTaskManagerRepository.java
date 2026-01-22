package com.wangliang.agentj.runtime.repository;

import com.wangliang.agentj.runtime.entity.po.RootTaskManagerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing RootTaskManagerEntity operations
 */
@Repository
public interface RootTaskManagerRepository extends JpaRepository<RootTaskManagerEntity, Long> {

	/**
	 * Find RootTaskManagerEntity by root plan ID
	 * @param rootPlanId The root plan ID
	 * @return Optional RootTaskManagerEntity
	 */
	Optional<RootTaskManagerEntity> findByRootPlanId(String rootPlanId);

	/**
	 * Check if a task exists by root plan ID
	 * @param rootPlanId The root plan ID
	 * @return true if task exists, false otherwise
	 */
	boolean existsByRootPlanId(String rootPlanId);

	/**
	 * Delete task by root plan ID
	 * @param rootPlanId The root plan ID
	 */
	void deleteByRootPlanId(String rootPlanId);

	/**
	 * Find tasks by desired task state
	 * @param desiredTaskState The desired task state
	 * @return List of RootTaskManagerEntity
	 */
	@Query("SELECT r FROM RootTaskManagerEntity r WHERE r.desiredTaskState = :desiredTaskState")
	java.util.List<RootTaskManagerEntity> findByDesiredTaskState(
			@Param("desiredTaskState") RootTaskManagerEntity.DesiredTaskState desiredTaskState);

}
