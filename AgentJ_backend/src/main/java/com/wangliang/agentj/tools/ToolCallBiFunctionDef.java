package com.wangliang.agentj.tools;

import com.wangliang.agentj.tools.code.ToolExecuteResult;
import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

/**
 * Interface for tool definitions, providing unified tool definition methods
 *
 * @param <I> Tool input type
 */
public interface ToolCallBiFunctionDef<I> extends BiFunction<I, ToolContext, ToolExecuteResult> {

    /**
     * Get the name of the tool group
     * @return Returns the unique identifier name of the tool
     */
    String getServiceGroup();

    /**
     * Get the name of the tool
     * @return Returns the unique identifier name of the tool
     */
    String getName();

    /**
     * Get the description information of the tool
     * @return Returns the functional description of the tool
     */
    String getDescription();

    /**
     * Get the description information of the tool with service group appended
     * @return Returns the functional description of the tool with service group appended
     * at the end
     */
    String getDescriptionWithServiceGroup();

    /**
     * Get the parameter definition schema of the tool
     * @return Returns JSON format parameter definition schema
     */
    String getParameters();

    /**
     * Get the input type of the tool
     * @return Returns the input parameter type Class that the tool accepts
     */
    Class<I> getInputType();

    /**
     * Determine whether the tool returns results directly
     * @return Returns true if the tool returns results directly, otherwise false
     */
    boolean isReturnDirect();

    /**
     * Determine whether the tool is selectable
     * @return Returns true if the tool is selectable, otherwise false
     */
    public boolean isSelectable();

    /**
     * Set the associated Agent instance
     * @param planId The plan ID to associate
     */
    public void setCurrentPlanId(String planId);

    /**
     * root plan id is the global parent of the whole execution plan id .
     * @param rootPlanId
     */
    public void setRootPlanId(String rootPlanId);

    /**
     * Get the current status string of the tool
     * @return Returns a string describing the current status of the tool
     */
    String getCurrentToolStateString();

    /**
     * Clean up all related resources for the specified planId
     * @param planId Plan ID
     */
    void cleanup(String planId);

}
