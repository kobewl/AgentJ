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
package com.wangliang.agentj.tools.pptGenerator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class PptInput {

	private String action;

	private String title;

	private String subtitle;

	@JsonProperty("slide_contents")
	private List<SlideContent> slideContents;

	private String path;

	@JsonProperty("template_content")
	private String templateContent;

	@JsonProperty("file_name")
	private String fileName;

	private static final ObjectMapper SLIDE_CONTENTS_MAPPER = new ObjectMapper();

	public static class SlideContent {

		private String title;

		private String content;

		@com.fasterxml.jackson.annotation.JsonProperty("image_path")
		private String imagePath;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getImagePath() {
			return imagePath;
		}

		public void setImagePath(String imagePath) {
			this.imagePath = imagePath;
		}

	}

	public PptInput() {
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public List<SlideContent> getSlideContents() {
		return slideContents;
	}

	@JsonSetter("slide_contents")
	public void setSlideContents(JsonNode slideContentsNode) {
		if (slideContentsNode == null || slideContentsNode.isNull()) {
			this.slideContents = null;
			return;
		}
		try {
			if (slideContentsNode.isTextual()) {
				String raw = slideContentsNode.asText();
				if (raw == null || raw.trim().isEmpty()) {
					this.slideContents = null;
				}
				else {
					this.slideContents = SLIDE_CONTENTS_MAPPER.readValue(raw,
							new TypeReference<List<SlideContent>>() {
							});
				}
			}
			else {
				this.slideContents = SLIDE_CONTENTS_MAPPER.convertValue(slideContentsNode,
						new TypeReference<List<SlideContent>>() {
						});
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid slide_contents format, must be JSON array or JSON string array",
					e);
		}
	}

	public void setSlideContents(List<SlideContent> slideContents) {
		this.slideContents = slideContents;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTemplateContent() {
		return templateContent;
	}

	public void setTemplateContent(String templateContent) {
		this.templateContent = templateContent;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
