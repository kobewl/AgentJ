package com.wangliang.agentj.runtime.executor.factory;


import com.wangliang.agentj.runtime.entity.vo.PlanInterface;
import com.wangliang.agentj.runtime.executor.PlanExecutorInterface;

/**
 * Interface for plan executor factory that creates executors for different plan types
 */
public interface IPlanExecutorFactory {

	/**
	 * Create executor for the given plan
	 */
	PlanExecutorInterface createExecutor(PlanInterface plan);

	/**
	 * Get all supported plan types
	 */
	String[] getSupportedPlanTypes();

	/**
	 * Check if a plan type is supported
	 */
	boolean isPlanTypeSupported(String planType);

}
