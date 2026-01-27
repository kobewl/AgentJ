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
package com.wangliang.agentj.codegen.repository;

import com.wangliang.agentj.codegen.entity.CodeChatHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeChatHistoryRepository extends JpaRepository<CodeChatHistoryEntity, Long> {

	/**
	 * 根据应用 ID 查询对话历史（未删除，按时间正序）
	 */
	List<CodeChatHistoryEntity> findByAppIdAndIsDeletedOrderByCreatedAtAsc(Long appId, Boolean isDeleted);

	/**
	 * 分页查询对话历史
	 */
	Page<CodeChatHistoryEntity> findByAppIdAndIsDeletedOrderByCreatedAtAsc(Long appId, Boolean isDeleted, Pageable pageable);

	/**
	 * 根据应用 ID 删除所有历史（软删除）
	 */
	@Query("UPDATE CodeChatHistoryEntity h SET h.isDeleted = true WHERE h.appId = :appId")
	int softDeleteByAppId(@Param("appId") Long appId);

	/**
	 * 获取应用的最后一条消息
	 */
	@Query("""
			SELECT h FROM CodeChatHistoryEntity h
			WHERE h.appId = :appId
			  AND h.isDeleted = false
			ORDER BY h.createdAt DESC
			LIMIT 1
			""")
	CodeChatHistoryEntity findLastByAppId(@Param("appId") Long appId);
}
