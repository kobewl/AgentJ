package com.wangliang.agentj.rag.documentloader.parsers;

import com.wangliang.agentj.rag.common.models.Document;
import java.io.InputStream;
import java.io.IOException;

/**
 * 文档解析器接口
 * 定义将不同格式文件解析为标准化文档对象的接口
 */
public interface DocumentParser {
    
    /**
     * 解析输入流为文档对象
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @return 解析后的文档对象
     * @throws IOException 解析过程中发生的异常
     */
    Document parse(InputStream inputStream, String fileName) throws IOException;
    
    /**
     * 解析文件内容为文档对象
     * @param content 文件内容
     * @param fileName 文件名
     * @return 解析后的文档对象
     * @throws IOException 解析过程中发生的异常
     */
    Document parse(String content, String fileName) throws IOException;
    
    /**
     * 检查解析器是否支持指定的文件类型
     * @param fileType 文件类型（扩展名）
     * @return 是否支持该文件类型
     */
    boolean supports(String fileType);
    
    /**
     * 获取解析器支持的文件类型列表
     * @return 支持的文件类型列表
     */
    String[] getSupportedTypes();
    
    /**
     * 获取解析器名称
     * @return 解析器名称
     */
    String getName();
    
    /**
     * 获取解析器描述
     * @return 解析器描述
     */
    String getDescription();
}