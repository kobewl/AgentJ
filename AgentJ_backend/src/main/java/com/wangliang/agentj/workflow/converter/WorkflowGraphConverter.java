package com.wangliang.agentj.workflow.converter;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.workflow.dto.WorkflowDTO;
import com.wangliang.agentj.workflow.nodes.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

/**
 * Converts WorkflowDTO (frontend graph definition) to Spring AI Alibaba StateGraph
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowGraphConverter {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final Map<String, ToolCallback> toolRegistry;

    /**
     * Convert a WorkflowDTO to a StateGraph
     */
    public StateGraph convert(WorkflowDTO workflowDTO) throws Exception {
        log.info("Converting workflow '{}' to StateGraph", workflowDTO.getName());

        // Create key strategy factory for state management
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory(workflowDTO);

        // Build the graph
        StateGraph graph = new StateGraph(keyStrategyFactory);

        // Add nodes
        for (WorkflowDTO.NodeDTO node : workflowDTO.getNodes()) {
            addNodeToGraph(graph, node);
        }

        // Add edges
        addEdgesToGraph(graph, workflowDTO.getNodes(), workflowDTO.getEdges());

        log.info("StateGraph created with {} nodes and {} edges",
                workflowDTO.getNodes().size(), workflowDTO.getEdges().size());

        return graph;
    }

    private KeyStrategyFactory createKeyStrategyFactory(WorkflowDTO workflowDTO) {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            // Default keys
            strategies.put("query", new ReplaceStrategy());
            strategies.put("input", new ReplaceStrategy());
            strategies.put("output", new ReplaceStrategy());
            strategies.put("result", new ReplaceStrategy());
            strategies.put("llm_output", new ReplaceStrategy());
            strategies.put("tool_output", new ReplaceStrategy());
            strategies.put("systemPrompt", new ReplaceStrategy()); // Runtime system prompt
            strategies.put("_current_node", new ReplaceStrategy());
            strategies.put("_next_node", new ReplaceStrategy());
            strategies.put("_workflow_status", new ReplaceStrategy());
            strategies.put("_condition_result", new ReplaceStrategy());

            // Add keys from node data
            for (WorkflowDTO.NodeDTO node : workflowDTO.getNodes()) {
                if (node.getData() != null) {
                    Object outputKey = node.getData().get("outputKey");
                    if (outputKey != null) {
                        strategies.put(outputKey.toString(), new ReplaceStrategy());
                    }
                }
            }

            return strategies;
        };
    }

    private void addNodeToGraph(StateGraph graph, WorkflowDTO.NodeDTO node) throws Exception {
        String nodeId = node.getId();
        String nodeType = node.getType();
        Map<String, Object> data = node.getData() != null ? node.getData() : new HashMap<>();

        log.debug("Adding node: id={}, type={}", nodeId, nodeType);

        switch (nodeType.toLowerCase()) {
            case "start" -> graph.addNode(nodeId, node_async(new StartNode()));

            case "end" -> graph.addNode(nodeId, node_async(new EndNode()));

            case "llm" -> {
                LLMNode.LLMNodeConfig config = LLMNode.LLMNodeConfig.builder()
                        .nodeId(nodeId)
                        .promptTemplate(getStringValue(data, "promptTemplate", "{{input}}"))
                        .systemPrompt(getStringValue(data, "systemPrompt", null))
                        .outputKey(getStringValue(data, "outputKey", "llm_output"))
                        .build();
                graph.addNode(nodeId, node_async(new LLMNode(chatClientBuilder, config)));
            }

            case "condition" -> {
                ConditionNode.ConditionNodeConfig config = ConditionNode.ConditionNodeConfig.builder()
                        .nodeId(nodeId)
                        .conditionType(ConditionNode.ConditionType.EXPRESSION)
                        .expression(getStringValue(data, "expression", "true"))
                        .trueTarget(getStringValue(data, "trueTarget", null))
                        .falseTarget(getStringValue(data, "falseTarget", null))
                        .defaultTarget(getStringValue(data, "defaultTarget", "end"))
                        .build();
                graph.addNode(nodeId, node_async(new ConditionNode(config)));
            }

            case "tool" -> {
                @SuppressWarnings("unchecked")
                Map<String, String> paramMapping = data.get("parameterMapping") != null
                        ? (Map<String, String>) data.get("parameterMapping")
                        : new HashMap<>();

                ToolNode.ToolNodeConfig config = ToolNode.ToolNodeConfig.builder()
                        .nodeId(nodeId)
                        .toolName(getStringValue(data, "toolName", ""))
                        .parameterMapping(paramMapping)
                        .outputKey(getStringValue(data, "outputKey", "tool_output"))
                        .build();
                graph.addNode(nodeId, node_async(new ToolNode(config, toolRegistry)));
            }

            default -> log.warn("Unknown node type: {}, skipping", nodeType);
        }
    }

    private void addEdgesToGraph(StateGraph graph, List<WorkflowDTO.NodeDTO> nodes,
                                  List<WorkflowDTO.EdgeDTO> edges) throws Exception {
        // Build a map of node types for quick lookup
        Map<String, String> nodeTypes = new HashMap<>();
        String startNodeId = null;
        String endNodeId = null;
        
        for (WorkflowDTO.NodeDTO node : nodes) {
            nodeTypes.put(node.getId(), node.getType());
            if ("start".equalsIgnoreCase(node.getType())) {
                startNodeId = node.getId();
            }
            if ("end".equalsIgnoreCase(node.getType())) {
                endNodeId = node.getId();
            }
        }

        // Group edges by source
        Map<String, List<WorkflowDTO.EdgeDTO>> edgesBySource = new HashMap<>();
        for (WorkflowDTO.EdgeDTO edge : edges) {
            edgesBySource.computeIfAbsent(edge.getSource(), k -> new java.util.ArrayList<>()).add(edge);
        }
        
        // Track if we've added an entry edge
        boolean hasEntryEdge = false;
        boolean hasEndEdge = false;
        
        // Process edges
        for (WorkflowDTO.EdgeDTO edge : edges) {
            String sourceId = edge.getSource();
            String targetId = edge.getTarget();
            String sourceType = nodeTypes.get(sourceId);
            String targetType = nodeTypes.get(targetId);

            // Handle start node -> first real node
            if ("start".equalsIgnoreCase(sourceType)) {
                graph.addEdge(StateGraph.START, targetId);
                hasEntryEdge = true;
                log.debug("Added entry edge: START -> {}", targetId);
                continue;
            }

            // Handle node -> end node
            if ("end".equalsIgnoreCase(targetType)) {
                graph.addEdge(sourceId, StateGraph.END);
                hasEndEdge = true;
                log.debug("Added end edge: {} -> END", sourceId);
                continue;
            }

            // Handle condition nodes - need conditional edges
            if ("condition".equalsIgnoreCase(sourceType)) {
                List<WorkflowDTO.EdgeDTO> conditionEdges = edgesBySource.get(sourceId);
                if (conditionEdges != null && conditionEdges.size() > 1) {
                    // Multiple outgoing edges - create conditional routing
                    Map<String, String> routeMap = new HashMap<>();
                    for (WorkflowDTO.EdgeDTO condEdge : conditionEdges) {
                        String label = condEdge.getLabel() != null ? condEdge.getLabel() : condEdge.getTarget();
                        String condTarget = condEdge.getTarget();
                        // Check if target is end node
                        if ("end".equalsIgnoreCase(nodeTypes.get(condTarget))) {
                            routeMap.put(label, StateGraph.END);
                        } else {
                            routeMap.put(label, condTarget);
                        }
                    }

                    graph.addConditionalEdges(
                            sourceId,
                            edge_async(state -> state.value("_condition_result").orElse("default").toString()),
                            routeMap
                    );
                    continue; // Skip regular edge processing for this edge
                }
            }

            // Regular edge
            graph.addEdge(sourceId, targetId);
            log.debug("Added edge: {} -> {}", sourceId, targetId);
        }
        
        // If no edges defined at all, try to create a simple linear flow
        if (edges.isEmpty() || !hasEntryEdge) {
            log.warn("No entry edge found, attempting to auto-create flow");
            
            // Find the first non-start, non-end node to use as entry target
            String firstNodeId = null;
            for (WorkflowDTO.NodeDTO node : nodes) {
                String type = node.getType();
                if (!"start".equalsIgnoreCase(type) && !"end".equalsIgnoreCase(type)) {
                    firstNodeId = node.getId();
                    break;
                }
            }
            
            if (firstNodeId != null) {
                graph.addEdge(StateGraph.START, firstNodeId);
                log.info("Auto-created entry edge: START -> {}", firstNodeId);
                
                // Also connect to END if no end edge
                if (!hasEndEdge) {
                    graph.addEdge(firstNodeId, StateGraph.END);
                    log.info("Auto-created end edge: {} -> END", firstNodeId);
                }
            } else if (endNodeId != null) {
                // No intermediate nodes, just connect start to end
                graph.addEdge(StateGraph.START, StateGraph.END);
                log.info("Auto-created direct edge: START -> END");
            }
        }
    }

    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
