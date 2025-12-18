package com.wangliang.agentj.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Workflow definition entity - stores the graph structure as JSON
 */
@Data
@Entity
@Table(name = "workflow_definition")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * JSON structure containing:
     * - nodes: array of node definitions with id, type, position, data
     * - edges: array of edge definitions with id, source, target, sourceHandle, targetHandle
     * - viewport: canvas viewport state (x, y, zoom)
     */
    @Column(name = "definition_json", columnDefinition = "LONGTEXT", nullable = false)
    private String definitionJson;

    @Column(length = 50)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
