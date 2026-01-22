package com.wangliang.agentj.runtime.executor;

import com.wangliang.agentj.runtime.entity.vo.ExecutionContext;
import com.wangliang.agentj.runtime.entity.vo.PlanExecutionResult;

import java.util.concurrent.CompletableFuture;

/**
 * Plan executor interface defining basic behaviors for plan execution
 */
public interface PlanExecutorInterface {

	/**
	 * Execute all steps of the entire plan
	 * @param context Execution context containing user request and execution process
	 * information
	 */
	public CompletableFuture<PlanExecutionResult> executeAllStepsAsync(ExecutionContext context);

}
