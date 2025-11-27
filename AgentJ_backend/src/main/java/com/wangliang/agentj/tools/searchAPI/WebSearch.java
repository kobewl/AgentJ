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
package com.wangliang.agentj.tools.searchAPI;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.model.dto.SearchResultDTO;
import com.wangliang.agentj.tools.AbstractBaseTool;
import com.wangliang.agentj.tools.code.ToolExecuteResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;


public class WebSearch extends AbstractBaseTool<WebSearch.WebSearchInput> {

	private static final Logger log = LoggerFactory.getLogger(WebSearch.class);

	private final ObjectMapper objectMapper;
	private final OkHttpClient httpClient;
	private final ExecutorService executorService;

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final int DEFAULT_TIMEOUT = 10000; // 默认超时时间10秒
    private static final int DEFAULT_THREAD_POOL_SIZE = 5; // 默认线程池大小
    private static final int DEFAULT_MAX_RESULTS = 2; // 默认最大结果数

	private static String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "query": {
			            "type": "string",
			            "description": "(required) The search query to submit to search engine."
			        },
			        "num_results": {
			            "type": "integer",
			            "description": "(optional) The number of search results to return. Default is 10.",
			            "default": 10
			        }
			    },
			    "required": ["query"]
			}
			""";

	private static final String name = "web_search";

	private static final String description = """
			Perform a web search using free search engines (Brave Search and DuckDuckGo) and return relevant search results.
			Use this tool when you need to find information on the web, get up-to-date data, or research specific topics.
			The tool returns search results including snippets, links, and other relevant information.
			""";

	private String lastQuery = "";

	private String lastSearchResults = "";

	private Integer lastNumResults = 0;

	public WebSearch(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
				.readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
				.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
				.build();
		this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
	}

	@SuppressWarnings("unchecked")
	public ToolExecuteResult run(String toolInput) {
		log.info("WebSearch toolInput:{}", toolInput);

		// Add exception handling for JSON deserialization
		try {
			Map<String, Object> toolInputMap = objectMapper.readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});
			String query = (String) toolInputMap.get("query");
			this.lastQuery = query;

			Integer numResults = 2;
			if (toolInputMap.get("num_results") != null) {
				numResults = (Integer) toolInputMap.get("num_results");
			}
			this.lastNumResults = numResults;

			// 使用免费搜索工具进行搜索
			String searchResults = performFreeSearch(query, numResults);
			this.lastSearchResults = searchResults;
			
			log.info("WebSearch completed successfully for query: {}", query);
			return new ToolExecuteResult(searchResults);
		}
		catch (Exception e) {
			log.error("Error executing web search", e);
			return new ToolExecuteResult("Error executing web search: " + e.getMessage());
		}
	}

	/**
	 * 执行免费搜索，优先使用Brave Search，失败时使用DuckDuckGo
	 */
	private String performFreeSearch(String query, int numResults) {
		if (StrUtil.isBlank(query)) {
			return createErrorResponse("搜索关键词不能为空");
		}

		try {
			// 首先尝试使用Brave Search API（更可靠）
			List<SearchResultDTO> braveResults = searchWithBraveAPI(query, numResults);
			if (braveResults != null && !braveResults.isEmpty()) {
				return formatSearchResults(braveResults, numResults);
			}
			
			// 如果Brave API失败，回退到DuckDuckGo
			log.info("Brave Search API失败，回退到DuckDuckGo");
			List<SearchResultDTO> duckResults = searchWithDuckDuckGo(query, numResults);
			return formatSearchResults(duckResults, numResults);
			
		} catch (Exception e) {
			log.error("搜索失败: {}", query, e);
			return createErrorResponse("搜索失败: " + e.getMessage());
		}
	}
	
	/**
	 * 使用Brave Search API进行搜索（推荐）
	 */
	private List<SearchResultDTO> searchWithBraveAPI(String query, int maxResults) {
		try {
			// Brave Search API的免费端点（无需API密钥）
			String searchUrl = "https://search.brave.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + 
							  "&count=" + maxResults;
			
			Request request = new Request.Builder()
					.url(searchUrl)
					.header("User-Agent", USER_AGENT)
					.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
					.build();

			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					log.warn("Brave搜索请求失败，状态码: {}", response.code());
					return new ArrayList<>();
				}

				String html = response.body().string();
				List<SearchResultDTO> results = parseBraveSearchResults(html, maxResults);
				
				if (CollUtil.isEmpty(results)) {
					log.info("Brave Search未找到结果");
					return new ArrayList<>();
				}
				
				log.info("Brave Search成功找到{}条结果", results.size());
				return results;
			}
		} catch (Exception e) {
			log.warn("Brave Search API失败: {}", e.getMessage());
			return new ArrayList<>();
		}
	}
	
	/**
	 * 使用DuckDuckGo进行搜索（回退方案）
	 */
	private List<SearchResultDTO> searchWithDuckDuckGo(String query, int maxResults) {
		try {
			String searchUrl = "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
			
			Request request = new Request.Builder()
					.url(searchUrl)
					.header("User-Agent", USER_AGENT)
					.build();

			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					log.warn("DuckDuckGo搜索请求失败: {}, 状态码: {}", searchUrl, response.code());
					return new ArrayList<>();
				}

				String html = response.body().string();
				List<SearchResultDTO> results = parseSearchResults(html, maxResults);
				
				if (CollUtil.isEmpty(results)) {
					log.info("DuckDuckGo未找到结果");
					return new ArrayList<>();
				}
				
				log.info("DuckDuckGo成功找到{}条结果", results.size());
				return results;
			}
		} catch (IOException e) {
			log.error("DuckDuckGo搜索失败: {}", query, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 创建错误响应
	 */
	private String createErrorResponse(String errorMessage) {
		JSONObject errorJson = JSONUtil.createObj()
				.set("error", errorMessage)
				.set("results", new ArrayList<>());
		return errorJson.toString();
	}

	/**
	 * 解析Brave Search搜索结果页面
	 */
	private List<SearchResultDTO> parseBraveSearchResults(String html, int maxResults) {
		List<SearchResultDTO> results = new ArrayList<>();
		
		try {
			Document document = Jsoup.parse(html);
			
			// Brave Search的结果结构 - 基于实际观察到的结构
			Elements resultElements = document.select("div.snippet.svelte-jmfu5f, div[data-result], div.result, div.fdb");
			
			// 如果没有找到标准容器，尝试其他可能的选择器
			if (resultElements.isEmpty()) {
				resultElements = document.select("div:has(h3 a), div:has(a[href^=http])");
			}
			
			int count = 0;
			for (Element resultElement : resultElements) {
				if (count >= maxResults) {
					break;
				}
				
				try {
					// 处理搜索结果元素
					
					// 提取标题和链接 - 基于Brave Search的实际结构
					String title = "";
					String url = "";
					
					// 首先尝试找明确的链接元素
					Element linkElement = resultElement.selectFirst("a[href^=http]");
					if (linkElement != null) {
						url = linkElement.absUrl("href");
						// 链接文本可能是标题
						String linkText = linkElement.text().trim();
						if (StrUtil.isNotBlank(linkText) && !linkText.contains("http")) {
							title = linkText;
						}
					}
					
					// 提取描述
					String snippet = "";
					String elementText = resultElement.text();
					
					// 从全文中提取描述（标题之后的部分）
					if (StrUtil.isNotBlank(elementText) && StrUtil.isNotBlank(title)) {
						int titleEndIndex = elementText.indexOf(title);
						if (titleEndIndex >= 0) {
							titleEndIndex += title.length();
							String remainingText = elementText.substring(titleEndIndex).trim();
							
							// 移除URL部分
							remainingText = remainingText.replaceAll("https?://[^\\s]+", "");
							remainingText = remainingText.replaceAll("\\s+", " ").trim();
							
							// 寻找描述的开始（通常是标题后的第一个有意义的句子）
							if (remainingText.length() > 10) {
								// 找到第一个句号或分号来分割描述
								int descEndIndex = remainingText.indexOf(". ");
								if (descEndIndex > 10) {
									snippet = remainingText.substring(0, descEndIndex + 1);
								} else {
									// 如果没有句号，取前50-150个字符
									int endIndex = Math.min(150, remainingText.length());
									snippet = remainingText.substring(0, endIndex);
									// 确保不以单词中间断开
									int lastSpace = snippet.lastIndexOf(" ");
									if (lastSpace > 30) {
										snippet = snippet.substring(0, lastSpace);
									}
								}
							}
						}
					}
					
					// 清理描述
					snippet = snippet.replaceAll("\\d{4}-\\d{2}-\\d{2}", "").trim(); // 移除日期
					snippet = snippet.replaceAll("\\d{1,2}:\\d{2}:", "").trim(); // 移除时间
					snippet = snippet.replaceAll("January|February|March|April|May|June|July|August|September|October|November|December", "").trim(); // 移除月份
					snippet = snippet.replaceAll("\\s+", " ").trim(); // 规范化空白字符
					
					// 确保描述有意义
					if (snippet.length() < 10 || snippet.equals(title)) {
						snippet = ""; // 如果描述太短或与标题相同，设为空
					} else if (snippet.length() > 300) {
						snippet = snippet.substring(0, 300) + "...";
					}
					
					// 跳过无效链接和Brave的内部链接
					if (StrUtil.isBlank(title) || StrUtil.isBlank(url) || 
						url.contains("brave.com") || url.startsWith("/") || 
						url.contains("preferences") || url.contains("help")) {
						continue;
					}
					
					// 创建搜索结果对象
					SearchResultDTO searchResult = new SearchResultDTO(title, snippet, url);
					results.add(searchResult);
					count++;
					
				} catch (Exception e) {
					log.warn("解析Brave搜索结果元素失败: {}", e.getMessage());
					continue;
				}
			}
			
		} catch (Exception e) {
			log.error("解析Brave搜索结果失败: {}", e.getMessage());
		}
		
		return results;
	}

	/**
	 * 解析DuckDuckGo搜索结果页面
	 */
	private List<SearchResultDTO> parseSearchResults(String html, int maxResults) {
		List<SearchResultDTO> results = new ArrayList<>();
		
		try {
			Document document = Jsoup.parse(html);
			
			// DuckDuckGo HTML结果的选择器
			Elements resultElements = document.select("div.result");
			
			int count = 0;
			for (Element resultElement : resultElements) {
				if (count >= maxResults) {
					break;
				}
				
				try {
					// 提取标题
					Element titleElement = resultElement.selectFirst("h2.result__title a.result__a");
					if (titleElement == null) {
						continue;
					}
					
					String title = titleElement.text().trim();
					String url = titleElement.absUrl("href");
					
					// 提取描述
					Element snippetElement = resultElement.selectFirst("a.result__snippet");
					String snippet = snippetElement != null ? snippetElement.text().trim() : "";
					
					// 跳过无效结果
					if (StrUtil.isBlank(title) || StrUtil.isBlank(url)) {
						continue;
					}
					
					// 创建搜索结果对象
					SearchResultDTO searchResult = new SearchResultDTO(title, snippet, url);
					results.add(searchResult);
					count++;
					
				} catch (Exception e) {
					log.warn("解析DuckDuckGo搜索结果元素失败: {}", e.getMessage());
					continue;
				}
			}
			
		} catch (Exception e) {
			log.error("解析DuckDuckGo搜索结果失败: {}", e.getMessage());
		}
		
		return results;
	}

	/**
	 * 格式化搜索结果，提取最有用的信息
	 */
	private String formatSearchResults(List<SearchResultDTO> results, int numResults) {
		if (CollUtil.isEmpty(results)) {
			return "No relevant search results found.";
		}
		
		StringBuilder result = new StringBuilder();
		result.append("Search Results:\n");
		
		int count = 0;
		for (SearchResultDTO searchResult : results) {
			if (count >= numResults) break;
			
			result.append("Title: ").append(searchResult.getTitle()).append("\n");
			if (StrUtil.isNotBlank(searchResult.getSnippet())) {
				result.append("Snippet: ").append(searchResult.getSnippet()).append("\n");
			}
			result.append("Link: ").append(searchResult.getUrl()).append("\n");
			result.append("\n");
			count++;
		}
		
		return result.toString().trim();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<WebSearchInput> getInputType() {
		return WebSearchInput.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ToolExecuteResult run(WebSearchInput input) {
		String query = input.getQuery();
		Integer numResults = input.getNumResults() != null ? input.getNumResults() : 2;

		log.info("WebSearch input: query={}, numResults={}", query, numResults);

		this.lastQuery = query;
		this.lastNumResults = numResults;

		try {
			// 使用免费搜索工具进行搜索
			String searchResults = performFreeSearch(query, numResults);
			this.lastSearchResults = searchResults;
			
			log.info("WebSearch completed successfully for query: {}", query);
			return new ToolExecuteResult(searchResults);
		}
		catch (Exception e) {
			log.error("Error executing web search", e);
			return new ToolExecuteResult("Error executing web search: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Web Search Status:
				- Search Location: %s
				- Recent Search: %s
				- Search Results: %s
				""", new java.io.File("").getAbsolutePath(),
				lastQuery.isEmpty() ? "No search performed yet"
						: String.format("Searched for: '%s' (max results: %d)", lastQuery, lastNumResults),
				lastSearchResults.isEmpty() ? "No results found" : lastSearchResults);
	}

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	/**
	 * Internal input class for defining input parameters of Web search tool
	 */
	public static class WebSearchInput {

		private String query;

		@com.fasterxml.jackson.annotation.JsonProperty("num_results")
		private Integer numResults;

		public WebSearchInput() {
		}

		public WebSearchInput(String query, Integer numResults) {
			this.query = query;
			this.numResults = numResults;
		}

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public Integer getNumResults() {
			return numResults;
		}

		public void setNumResults(Integer numResults) {
			this.numResults = numResults;
		}

	}

	@Override
	public boolean isSelectable() {
		return true;
	}

}
