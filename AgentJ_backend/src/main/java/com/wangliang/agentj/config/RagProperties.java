package com.wangliang.agentj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 运行时可调参数。
 */
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

	/**
	 * 检索返回的候选文档数。
	 */
	private int topK = 5;

	/**
	 * 相似度阈值，小于该值的文档会被丢弃。
	 * 针对 Qdrant 向量库，默认值设置较低以确保匹配文档不被过滤。
	 */
	private double similarityThreshold = 0.35;

	/**
	 * 生成阶段可用的上下文最大字符数，避免提示过长。
	 */
	private int maxContextChars = 2000;

	public int getTopK() {
		return topK;
	}

	public void setTopK(int topK) {
		this.topK = topK;
	}

	public double getSimilarityThreshold() {
		return similarityThreshold;
	}

	public void setSimilarityThreshold(double similarityThreshold) {
		this.similarityThreshold = similarityThreshold;
	}

	public int getMaxContextChars() {
		return maxContextChars;
	}

	public void setMaxContextChars(int maxContextChars) {
		this.maxContextChars = maxContextChars;
	}

}
