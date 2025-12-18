package com.wangliang.agentj.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.workflow.dto.WorkflowDTO;
import com.wangliang.agentj.workflow.entity.WorkflowDefinition;
import com.wangliang.agentj.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for workflow CRUD operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all workflows
     */
    public List<WorkflowDTO> getAllWorkflows() {
        return workflowRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get workflow by ID
     */
    public WorkflowDTO getWorkflow(Long id) {
        WorkflowDefinition entity = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        return toDTO(entity);
    }

    /**
     * Create new workflow
     */
    @Transactional
    public WorkflowDTO createWorkflow(WorkflowDTO dto) {
        WorkflowDefinition entity = toEntity(dto);
        entity = workflowRepository.save(entity);
        log.info("Created workflow: id={}, name={}", entity.getId(), entity.getName());
        return toDTO(entity);
    }

    /**
     * Update existing workflow
     */
    @Transactional
    public WorkflowDTO updateWorkflow(Long id, WorkflowDTO dto) {
        WorkflowDefinition entity = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : entity.getStatus());
        entity.setDefinitionJson(toJson(dto));

        entity = workflowRepository.save(entity);
        log.info("Updated workflow: id={}, name={}", entity.getId(), entity.getName());
        return toDTO(entity);
    }

    /**
     * Delete workflow
     */
    @Transactional
    public void deleteWorkflow(Long id) {
        if (!workflowRepository.existsById(id)) {
            throw new IllegalArgumentException("Workflow not found: " + id);
        }
        workflowRepository.deleteById(id);
        log.info("Deleted workflow: id={}", id);
    }

    /**
     * Publish workflow (change status to PUBLISHED)
     */
    @Transactional
    public WorkflowDTO publishWorkflow(Long id) {
        WorkflowDefinition entity = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        entity.setStatus("PUBLISHED");
        entity = workflowRepository.save(entity);
        log.info("Published workflow: id={}", id);
        return toDTO(entity);
    }

    // ============ Conversion Methods ============

    private WorkflowDTO toDTO(WorkflowDefinition entity) {
        try {
            WorkflowDTO dto = objectMapper.readValue(entity.getDefinitionJson(), WorkflowDTO.class);
            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setDescription(entity.getDescription());
            dto.setStatus(entity.getStatus());
            return dto;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse workflow definition JSON", e);
            // Return basic DTO without nodes/edges
            return WorkflowDTO.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .status(entity.getStatus())
                    .build();
        }
    }

    private WorkflowDefinition toEntity(WorkflowDTO dto) {
        return WorkflowDefinition.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .definitionJson(toJson(dto))
                .build();
    }

    private String toJson(WorkflowDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize workflow DTO", e);
            return "{}";
        }
    }
}
