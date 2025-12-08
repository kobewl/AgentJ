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
package com.wangliang.agentj.conversation.entity.vo;

import java.util.List;

/**
 * 通用分页响应结构
 */
public class PagedResult<T> {

	private List<T> items;

	private long total;

	private int page;

	private int size;

	private int totalPages;

	public PagedResult() {
	}

	public PagedResult(List<T> items, long total, int page, int size) {
		this.items = items;
		this.total = total;
		this.page = page;
		this.size = size;
		if (size > 0) {
			this.totalPages = (int) Math.ceil((double) total / size);
		} else {
			this.totalPages = 1;
		}
	}

	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}
}
