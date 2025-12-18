package com.wangliang.agentj.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Workflow execution record entity
 */
@Data
@Entity
@Table(name = "workflow_execution")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "input_data", columnDefinition = "LONGTEXT")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "LONGTEXT")
    private String outputData;

    @Column(length = 50)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    public void markCompleted(String output) {
        this.outputData = output;
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.errorMessage = error;
        this.status = "FAILED";
        this.completedAt = LocalDateTime.now();
    }
}
