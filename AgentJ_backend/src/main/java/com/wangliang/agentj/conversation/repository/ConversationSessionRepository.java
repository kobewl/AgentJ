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
package com.wangliang.agentj.conversation.repository;

import com.wangliang.agentj.conversation.entity.po.ConversationSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationSessionRepository extends JpaRepository<ConversationSessionEntity, String> {

	Optional<ConversationSessionEntity> findByIdAndUserId(String id, Long userId);

	@Query("""
			SELECT s FROM ConversationSessionEntity s
			WHERE s.userId = :userId
			  AND (:includeDeleted = true OR s.isDeleted = false)
			  AND (
			      :keyword IS NULL
			      OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
			      OR LOWER(s.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
			  )
			ORDER BY s.lastMessageAt DESC, s.updatedAt DESC
			""")
	Page<ConversationSessionEntity> search(@Param("userId") Long userId, @Param("keyword") String keyword,
			@Param("includeDeleted") boolean includeDeleted, Pageable pageable);

	@Modifying
	@Query("UPDATE ConversationSessionEntity s SET s.isDeleted = true WHERE s.id = :id AND s.userId = :userId")
	int softDelete(@Param("id") String id, @Param("userId") Long userId);

	@Modifying
	@Query("UPDATE ConversationSessionEntity s SET s.isDeleted = false WHERE s.id = :id AND s.userId = :userId")
	int restore(@Param("id") String id, @Param("userId") Long userId);
}
