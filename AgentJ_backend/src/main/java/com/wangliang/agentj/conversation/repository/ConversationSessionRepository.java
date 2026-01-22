package com.wangliang.agentj.conversation.repository;

import com.wangliang.agentj.conversation.entity.ConversationType;
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

	/**
	 * 按对话类型和知识库ID搜索会话
	 */
	@Query("""
			SELECT s FROM ConversationSessionEntity s
			WHERE s.userId = :userId
			  AND (:includeDeleted = true OR s.isDeleted = false)
			  AND (:conversationType IS NULL OR s.conversationType = :conversationType)
			  AND (:knowledgeBaseId IS NULL OR s.knowledgeBaseId = :knowledgeBaseId)
			  AND (
			      :keyword IS NULL
			      OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
			      OR LOWER(s.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
			  )
			ORDER BY s.lastMessageAt DESC, s.updatedAt DESC
			""")
	Page<ConversationSessionEntity> searchByType(
			@Param("userId") Long userId,
			@Param("conversationType") ConversationType conversationType,
			@Param("knowledgeBaseId") String knowledgeBaseId,
			@Param("keyword") String keyword,
			@Param("includeDeleted") boolean includeDeleted,
			Pageable pageable);

	@Modifying
	@Query("UPDATE ConversationSessionEntity s SET s.isDeleted = true WHERE s.id = :id AND s.userId = :userId")
	int softDelete(@Param("id") String id, @Param("userId") Long userId);

	@Modifying
	@Query("UPDATE ConversationSessionEntity s SET s.isDeleted = false WHERE s.id = :id AND s.userId = :userId")
	int restore(@Param("id") String id, @Param("userId") Long userId);
}
