package com.wangliang.agentj.workflow.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Start node - entry point of the workflow
 * Initializes the workflow state with input variables
 */
@Slf4j
public class StartNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("Workflow started with initial state: {}", state.data());
        
        Map<String, Object> result = new HashMap<>();
        result.put("_workflow_status", "STARTED");
        result.put("_current_node", "start");
        
        return result;
    }
}
