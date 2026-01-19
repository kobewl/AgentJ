package com.wangliang.agentj.tools;

import com.wangliang.agentj.tools.code.ToolExecuteResult;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Abstract base class for tools providing common functionality All concrete tool
 * implementations should extend this class
 *
 * @param <I> Tool input type
 */
public abstract class AbstractBaseTool<I> implements ToolCallBiFunctionDef<I> {

	/**
	 * Current plan ID for the tool execution context
	 */
	protected String currentPlanId;

	/**
	 * Root plan ID is the global parent of the whole execution plan
	 */
	protected String rootPlanId;

	/**
	 * Whether the tool is selectable in front end UI
	 * @return
	 */
	public abstract boolean isSelectable();

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public void setCurrentPlanId(String planId) {
		this.currentPlanId = planId;
	}

	@Override
	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	/**
	 * Get the current plan ID
	 * @return the current plan ID
	 */
	public String getCurrentPlanId() {
		return this.currentPlanId;
	}

	/**
	 * Get the root plan ID
	 * @return the root plan ID
	 */
	public String getRootPlanId() {
		return this.rootPlanId;
	}

	/**
	 * Default implementation delegates to run method Subclasses can override this method
	 * if needed
	 */
	@Override
	public ToolExecuteResult apply(I input, ToolContext toolContext) {
		return run(input);
	}

	/**
	 * Abstract method that subclasses must implement to define tool-specific execution
	 * logic
	 * @param input Tool input parameters
	 * @return Tool execution result
	 */
	public abstract ToolExecuteResult run(I input);

    /**
     * Get the description information of the tool with service group appended Default
     * implementation appends serviceGroup to the description if serviceGroup is not null
     * or empty
     * @return Returns the functional description of the tool with service group appended
     * at the end
     */
    @Override
    public String getDescriptionWithServiceGroup() {
        String description = getDescription();
        String serviceGroup = getServiceGroup();
        if (serviceGroup != null && !serviceGroup.trim().isEmpty()) {
            return description + ". Service group: " + serviceGroup;
        }
        return description;
    }

}
