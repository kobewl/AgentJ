package com.wangliang.agentj.workflow.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * End node - final node of the workflow
 * Collects and formats the final output
 */
@Slf4j
public class EndNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("Workflow ending with state: {}", state.data());
        
        Map<String, Object> result = new HashMap<>();
        result.put("_workflow_status", "COMPLETED");
        result.put("_current_node", "end");
        
        // Collect output from state
        Object output = state.value("output").orElse(state.value("result").orElse("Workflow completed"));
        result.put("final_output", output);
        
        return result;
    }
}
