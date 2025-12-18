package com.wangliang.agentj.workflow.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * DTO for workflow definition - represents the Vue Flow graph structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDTO {

    private Long id;
    private String name;
    private String description;
    private String status;
    
    /**
     * Vue Flow nodes array
     */
    private List<NodeDTO> nodes;
    
    /**
     * Vue Flow edges array
     */
    private List<EdgeDTO> edges;
    
    /**
     * Vue Flow viewport state
     */
    private ViewportDTO viewport;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeDTO {
        private String id;
        private String type; // start, end, llm, condition, tool
        private PositionDTO position;
        private Map<String, Object> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EdgeDTO {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
        private String label;
        private String type; // default, smoothstep
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PositionDTO {
        private double x;
        private double y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ViewportDTO {
        private double x;
        private double y;
        private double zoom;
    }
}
