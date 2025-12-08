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

import com.wangliang.agentj.conversation.entity.po.ConversationMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, String> {

	Page<ConversationMessageEntity> findByConversationIdAndIsDeletedOrderByCreatedAtAsc(String conversationId,
			boolean isDeleted, Pageable pageable);

	Page<ConversationMessageEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId, Pageable pageable);

	@Modifying
	@Query("UPDATE ConversationMessageEntity m SET m.isDeleted = true WHERE m.conversationId = :conversationId")
	int softDeleteByConversationId(@Param("conversationId") String conversationId);

	@Modifying
	@Query("UPDATE ConversationMessageEntity m SET m.isDeleted = false WHERE m.conversationId = :conversationId")
	int restoreByConversationId(@Param("conversationId") String conversationId);

	@Modifying
	@Query("UPDATE ConversationMessageEntity m SET m.isDeleted = true WHERE m.id = :id AND m.conversationId = :conversationId")
	int softDelete(@Param("id") String id, @Param("conversationId") String conversationId);

	@Modifying
	@Query("UPDATE ConversationMessageEntity m SET m.isDeleted = false WHERE m.id = :id AND m.conversationId = :conversationId")
	int restore(@Param("id") String id, @Param("conversationId") String conversationId);

	@Query(value = """
			SELECT m.* FROM conversation_messages m
			INNER JOIN conversation_sessions s ON m.conversation_id = s.id
			WHERE s.user_id = :userId
			  AND (:conversationId IS NULL OR m.conversation_id = :conversationId)
			  AND (:includeDeleted = true OR m.is_deleted = 0)
			  AND (MATCH(m.content) AGAINST (:keyword IN BOOLEAN MODE))
			ORDER BY m.created_at DESC
			""", countQuery = """
			SELECT COUNT(1) FROM conversation_messages m
			INNER JOIN conversation_sessions s ON m.conversation_id = s.id
			WHERE s.user_id = :userId
			  AND (:conversationId IS NULL OR m.conversation_id = :conversationId)
			  AND (:includeDeleted = true OR m.is_deleted = 0)
			  AND (MATCH(m.content) AGAINST (:keyword IN BOOLEAN MODE))
			""", nativeQuery = true)
	Page<ConversationMessageEntity> searchByContent(@Param("userId") Long userId,
			@Param("conversationId") String conversationId, @Param("keyword") String keyword,
			@Param("includeDeleted") boolean includeDeleted, Pageable pageable);
}
