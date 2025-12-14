package com.wangliang.agentj.rag.documentloader.loaders;

import com.wangliang.agentj.rag.common.models.Document;
import java.io.IOException;
import java.util.List;

/**
 * 文档加载器接口
 * 定义从各种数据源加载文档的标准接口
 */
public interface DocumentLoader {
    
    /**
     * 加载单个文档
     * @param source 文档源路径或标识符
     * @return 加载的文档对象
     * @throws IOException 加载过程中发生的异常
     */
    Document loadDocument(String source) throws IOException;
    
    /**
     * 批量加载文档
     * @param sources 文档源路径或标识符列表
     * @return 加载的文档对象列表
     * @throws IOException 加载过程中发生的异常
     */
    List<Document> loadDocuments(List<String> sources) throws IOException;
    
    /**
     * 检查加载器是否支持指定的文件类型
     * @param fileType 文件类型
     * @return 是否支持该文件类型
     */
    boolean supports(String fileType);
    
    /**
     * 获取加载器支持的文件类型列表
     * @return 支持的文件类型列表
     */
    List<String> getSupportedTypes();
    
    /**
     * 获取加载器名称
     * @return 加载器名称
     */
    String getName();
    
    /**
     * 获取加载器描述
     * @return 加载器描述
     */
    String getDescription();
}