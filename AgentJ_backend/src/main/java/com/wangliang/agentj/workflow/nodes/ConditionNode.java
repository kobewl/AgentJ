package com.wangliang.agentj.workflow.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Condition Node - evaluates a condition and determines the next path
 * Supports JavaScript expressions for complex conditions
 */
@Slf4j
public class ConditionNode implements NodeAction {

    private final ConditionNodeConfig config;
    private final ScriptEngine scriptEngine;

    public ConditionNode(ConditionNodeConfig config) {
        this.config = config;
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("JavaScript");
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("Condition Node [{}] evaluating condition", config.getNodeId());
        
        String nextNode;
        
        if (config.getConditionType() == ConditionType.EXPRESSION) {
            // Evaluate JavaScript expression
            nextNode = evaluateExpression(state);
        } else {
            // Simple value comparison
            nextNode = evaluateSimpleCondition(state);
        }
        
        log.info("Condition evaluated, next node: {}", nextNode);
        
        Map<String, Object> result = new HashMap<>();
        result.put("_next_node", nextNode);
        result.put("_current_node", config.getNodeId());
        result.put("_condition_result", nextNode);
        
        return result;
    }

    private String evaluateExpression(OverAllState state) throws Exception {
        // Bind state variables to script engine
        for (Map.Entry<String, Object> entry : state.data().entrySet()) {
            scriptEngine.put(entry.getKey(), entry.getValue());
        }
        
        Object result = scriptEngine.eval(config.getExpression());
        
        if (result instanceof Boolean) {
            return (Boolean) result ? config.getTrueTarget() : config.getFalseTarget();
        } else if (result instanceof String) {
            return (String) result;
        } else {
            return config.getDefaultTarget();
        }
    }

    private String evaluateSimpleCondition(OverAllState state) {
        Object value = state.value(config.getVariable()).orElse(null);
        
        if (value == null) {
            return config.getDefaultTarget();
        }
        
        String stringValue = String.valueOf(value);
        
        // Check against condition cases
        if (config.getCases() != null) {
            for (Map.Entry<String, String> caseEntry : config.getCases().entrySet()) {
                if (caseEntry.getKey().equals(stringValue)) {
                    return caseEntry.getValue();
                }
            }
        }
        
        // Boolean check
        if ("true".equalsIgnoreCase(stringValue) || "1".equals(stringValue)) {
            return config.getTrueTarget() != null ? config.getTrueTarget() : config.getDefaultTarget();
        }
        
        return config.getFalseTarget() != null ? config.getFalseTarget() : config.getDefaultTarget();
    }

    @Data
    @Builder
    public static class ConditionNodeConfig {
        private String nodeId;
        private ConditionType conditionType;
        
        // For EXPRESSION type
        private String expression;
        
        // For SIMPLE type
        private String variable;
        private Map<String, String> cases; // value -> target node
        
        // Targets
        private String trueTarget;
        private String falseTarget;
        private String defaultTarget;
    }

    public enum ConditionType {
        SIMPLE,      // Simple value comparison
        EXPRESSION   // JavaScript expression
    }
}
