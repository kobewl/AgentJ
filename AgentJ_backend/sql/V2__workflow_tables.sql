-- Workflow Definition Tables
-- V2__workflow_tables.sql

CREATE TABLE IF NOT EXISTS workflow_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    definition_json LONGTEXT NOT NULL COMMENT 'JSON format: nodes, edges, viewport',
    status VARCHAR(50) DEFAULT 'DRAFT' COMMENT 'DRAFT, PUBLISHED, ARCHIVED',
    user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workflow_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL,
    input_data LONGTEXT COMMENT 'JSON input',
    output_data LONGTEXT COMMENT 'JSON output',
    status VARCHAR(50) DEFAULT 'RUNNING' COMMENT 'RUNNING, COMPLETED, FAILED, INTERRUPTED',
    error_message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (workflow_id) REFERENCES workflow_definition(id) ON DELETE CASCADE,
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
