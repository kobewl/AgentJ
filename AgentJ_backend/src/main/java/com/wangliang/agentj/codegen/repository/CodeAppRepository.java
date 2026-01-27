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

import com.wangliang.agentj.codegen.entity.CodeAppEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeAppRepository extends JpaRepository<CodeAppEntity, Long> {

	/**
	 * 根据用户 ID 查询应用列表（未删除）
	 */
	List<CodeAppEntity> findByUserIdAndIsDeletedOrderByCreatedAtDesc(Long userId, Boolean isDeleted);

	/**
	 * 分页查询用户的应用
	 */
	Page<CodeAppEntity> findByUserIdAndIsDeletedOrderByCreatedAtDesc(Long userId, Boolean isDeleted, Pageable pageable);

	/**
	 * 根据 ID 和用户 ID 查询
	 */
	Optional<CodeAppEntity> findByIdAndUserIdAndIsDeleted(Long id, Long userId, Boolean isDeleted);

	/**
	 * 根据部署标识查询
	 */
	Optional<CodeAppEntity> findByDeployKeyAndIsDeleted(String deployKey, Boolean isDeleted);

	/**
	 * 搜索应用（按名称或描述）
	 */
	@Query("""
			SELECT a FROM CodeAppEntity a
			WHERE a.userId = :userId
			  AND a.isDeleted = false
			  AND (:keyword IS NULL
			       OR LOWER(a.appName) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(a.initPrompt) LIKE LOWER(CONCAT('%', :keyword, '%')))
			ORDER BY a.createdAt DESC
			""")
	Page<CodeAppEntity> search(@Param("userId") Long userId,
			@Param("keyword") String keyword,
			Pageable pageable);

	/**
	 * 软删除
	 */
	@Query("UPDATE CodeAppEntity a SET a.isDeleted = true WHERE a.id = :id AND a.userId = :userId")
	int softDelete(@Param("id") Long id, @Param("userId") Long userId);
}
