package com.wangliang.agentj.rag.textsplitting;

import com.wangliang.agentj.rag.common.models.Document;

import java.util.List;

/**
 * 文本分割器接口
 * 定义文本分割的基本功能，支持多种分割策略
 */
public interface TextSplitter {

    /**
     * 分割文档内容为多个文本块
     * 
     * @param document 要分割的文档
     * @return 分割后的文本块列表
     */
    List<TextChunk> split(Document document);

    /**
     * 分割文本内容为多个文本块
     * 
     * @param text 要分割的文本内容
     * @param metadata 元数据信息
     * @return 分割后的文本块列表
     */
    List<TextChunk> split(String text, Object metadata);

    /**
     * 分割文本内容为多个文本块（带文档信息）
     * 
     * @param text 要分割的文本内容
     * @param documentId 文档ID
     * @param metadata 元数据信息
     * @return 分割后的文本块列表
     */
    List<TextChunk> split(String text, String documentId, Object metadata);

    /**
     * 获取分割器的名称
     * 
     * @return 分割器名称
     */
    String getName();

    /**
     * 获取分割器的描述
     * 
     * @return 分割器描述
     */
    String getDescription();

    /**
     * 获取分割器的配置参数
     * 
     * @return 配置参数映射
     */
    SplitterConfig getConfig();

    /**
     * 设置分割器的配置参数
     * 
     * @param config 配置参数
     */
    void setConfig(SplitterConfig config);

    /**
     * 检查分割器是否支持指定的文档类型
     * 
     * @param documentType 文档类型
     * @return 是否支持
     */
    boolean supports(String documentType);

    /**
     * 获取分割器支持的文档类型
     * 
     * @return 支持的文档类型数组
     */
    String[] getSupportedTypes();
}