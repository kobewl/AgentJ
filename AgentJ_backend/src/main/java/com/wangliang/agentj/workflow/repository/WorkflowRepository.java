package com.wangliang.agentj.workflow.repository;

import com.wangliang.agentj.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for workflow definitions
 */
@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowDefinition, Long> {
    
    /**
     * Find all workflows by user ID
     */
    List<WorkflowDefinition> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    /**
     * Find all workflows by status
     */
    List<WorkflowDefinition> findByStatusOrderByUpdatedAtDesc(String status);
    
    /**
     * Find workflows by user ID and status
     */
    List<WorkflowDefinition> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);
    
    /**
     * Find all workflows ordered by update time
     */
    List<WorkflowDefinition> findAllByOrderByUpdatedAtDesc();
}
