package com.wangliang.agentj.rag.queryenhancement.rewriters;

import com.wangliang.agentj.rag.queryenhancement.models.Query;

/**
 * 查询重写器接口
 * 定义查询重写和优化的标准接口
 */
public interface QueryRewriter {
    
    /**
     * 重写查询
     * @param query 原始查询对象
     * @return 重写后的查询对象
     */
    Query rewrite(Query query);
    
    /**
     * 检查重写器是否支持指定的查询类型
     * @param query 查询对象
     * @return 是否支持该查询类型
     */
    boolean supports(Query query);
    
    /**
     * 获取重写器名称
     * @return 重写器名称
     */
    String getName();
    
    /**
     * 获取重写器描述
     * @return 重写器描述
     */
    String getDescription();
    
    /**
     * 获取重写器的优先级
     * @return 优先级，数值越高优先级越高
     */
    int getPriority();
}