package com.wangliang.agentj.knowledge.repository;

import com.wangliang.agentj.knowledge.entity.KnowledgeItemEntity;
import com.wangliang.agentj.knowledge.entity.KnowledgeItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItemEntity, String> {

	List<KnowledgeItemEntity> findByUserIdAndTypeAndIsDeletedFalse(Long userId, KnowledgeItemType type);

	List<KnowledgeItemEntity> findByKnowledgeBaseIdAndTypeAndIsDeletedFalse(String knowledgeBaseId,
			KnowledgeItemType type);

	Optional<KnowledgeItemEntity> findByIdAndUserId(String id, Long userId);

	@Modifying
	@Query("update KnowledgeItemEntity k set k.isDeleted=true where k.knowledgeBaseId=:baseId and k.userId=:userId")
	int softDeleteFilesByBase(@Param("baseId") String baseId, @Param("userId") Long userId);

}
