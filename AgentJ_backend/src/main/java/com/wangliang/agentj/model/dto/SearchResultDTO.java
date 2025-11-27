package com.wangliang.agentj.model.dto;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;

/**
 * 搜索结果数据传输对象
 * 用于封装搜索引擎返回的结果数据
 */
@Data
public class SearchResultDTO {
    private String title;
    private String url;
    private String snippet;
    private String content;

    public SearchResultDTO() {
    }

    public SearchResultDTO(String title, String snippet, String url) {
        this.title = title;
        this.snippet = snippet;
        this.url = url;
        this.content = "";
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getContent() {
        return content;
    }

    /**
     * 转换为JSON字符串
     * @return JSON字符串
     */
    public String toJson() {
        JSONObject json = new JSONObject();
        json.set("title", title);
        json.set("url", url);
        json.set("snippet", snippet);
        json.set("content", content != null ? content : "");
        return JSONUtil.toJsonStr(json);
    }
}