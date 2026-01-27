/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wangliang.agentj.codegen.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 代码生成请求
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeGenRequest {

	/**
	 * 应用 ID
	 */
	private Long appId;

	/**
	 * 用户消息/需求描述
	 */
	@NotBlank(message = "消息内容不能为空")
	@Size(max = 2000, message = "消息内容最长2000字符")
	private String message;

	/**
	 * 选中的元素信息（用于可视化编辑修改）
	 */
	private ElementInfo elementInfo;

	public Long getAppId() {
		return appId;
	}

	public void setAppId(Long appId) {
		this.appId = appId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ElementInfo getElementInfo() {
		return elementInfo;
	}

	public void setElementInfo(ElementInfo elementInfo) {
		this.elementInfo = elementInfo;
	}

	/**
	 * 元素信息
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ElementInfo {

		/**
		 * 标签名
		 */
		private String tagName;

		/**
		 * ID
		 */
		private String id;

		/**
		 * 类名
		 */
		private String className;

		/**
		 * 文本内容
		 */
		private String textContent;

		/**
		 * CSS 选择器
		 */
		private String selector;

		/**
		 * 页面路径
		 */
		private String pagePath;

		/**
		 * 元素位置信息
		 */
		private Rect rect;

		public String getTagName() {
			return tagName;
		}

		public void setTagName(String tagName) {
			this.tagName = tagName;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public String getTextContent() {
			return textContent;
		}

		public void setTextContent(String textContent) {
			this.textContent = textContent;
		}

		public String getSelector() {
			return selector;
		}

		public void setSelector(String selector) {
			this.selector = selector;
		}

		public String getPagePath() {
			return pagePath;
		}

		public void setPagePath(String pagePath) {
			this.pagePath = pagePath;
		}

		public Rect getRect() {
			return rect;
		}

		public void setRect(Rect rect) {
			this.rect = rect;
		}
	}

	/**
	 * 位置信息
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Rect {

		private Double top;
		private Double left;
		private Double width;
		private Double height;

		public Double getTop() {
			return top;
		}

		public void setTop(Double top) {
			this.top = top;
		}

		public Double getLeft() {
			return left;
		}

		public void setLeft(Double left) {
			this.left = left;
		}

		public Double getWidth() {
			return width;
		}

		public void setWidth(Double width) {
			this.width = width;
		}

		public Double getHeight() {
			return height;
		}

		public void setHeight(Double height) {
			this.height = height;
		}
	}
}
