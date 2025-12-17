-- 知识库与知识文件共用的表
CREATE TABLE IF NOT EXISTS `knowledge_items` (
  `id`              varchar(64)  NOT NULL COMMENT '主键；知识库ID或文档ID',
  `name`            varchar(255) NOT NULL COMMENT '名称',
  `type`            varchar(32)  NOT NULL COMMENT 'KNOWLEDGE_BASE / KNOWLEDGE_FILE',
  `storage_path`    varchar(1024) DEFAULT NULL COMMENT '本地存储路径，库本身为空',
  `knowledge_base_id` varchar(64) DEFAULT NULL COMMENT '所属知识库ID，库本身为空',
  `user_id`         bigint       NOT NULL COMMENT '所属用户ID',
  `original_filename` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `mime_type`       varchar(128) DEFAULT NULL COMMENT 'MIME类型',
  `file_size`       bigint       DEFAULT NULL COMMENT '文件大小（字节）',
  `is_deleted`      tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  `created_at`      datetime     NOT NULL,
  `updated_at`      datetime     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kb_user` (`knowledge_base_id`,`user_id`),
  KEY `idx_user_type` (`user_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库与知识文件';
