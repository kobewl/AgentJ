package com.wangliang.agentj.workflow.repository;

import com.wangliang.agentj.workflow.entity.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for workflow executions
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    
    /**
     * Find executions by workflow ID
     */
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(Long workflowId);
    
    /**
     * Find executions by status
     */
    List<WorkflowExecution> findByStatusOrderByStartedAtDesc(String status);
}
