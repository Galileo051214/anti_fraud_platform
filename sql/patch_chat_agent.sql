-- 已有库增量：智能反诈助手 agent 元数据
-- 可重复执行：字段已存在时不会再次添加。
-- 注意：主线稍后统一合并到 init.sql，本补丁暂不修改 init.sql。
USE anti_fraud_platform;

DROP PROCEDURE IF EXISTS patch_chat_agent_metadata;
DELIMITER //
CREATE PROCEDURE patch_chat_agent_metadata()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'qa_conversation'
          AND COLUMN_NAME = 'answer_type'
    ) THEN
        ALTER TABLE `qa_conversation`
            ADD COLUMN `answer_type` VARCHAR(32) NOT NULL DEFAULT 'qa' COMMENT '回答类型:qa/latest_report' AFTER `feedback`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'qa_conversation'
          AND COLUMN_NAME = 'risk_level'
    ) THEN
        ALTER TABLE `qa_conversation`
            ADD COLUMN `risk_level` VARCHAR(16) NOT NULL DEFAULT 'low' COMMENT '风险等级:low/medium/high' AFTER `answer_type`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'qa_conversation'
          AND COLUMN_NAME = 'sources_json'
    ) THEN
        ALTER TABLE `qa_conversation`
            ADD COLUMN `sources_json` JSON NULL COMMENT '检索来源JSON数组' AFTER `risk_level`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'qa_conversation'
          AND COLUMN_NAME = 'fallback'
    ) THEN
        ALTER TABLE `qa_conversation`
            ADD COLUMN `fallback` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否降级回答' AFTER `sources_json`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'qa_conversation'
          AND COLUMN_NAME = 'retrieved_at'
    ) THEN
        ALTER TABLE `qa_conversation`
            ADD COLUMN `retrieved_at` DATETIME NULL COMMENT '检索时间' AFTER `fallback`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'qa_conversation'
          AND INDEX_NAME = 'idx_answer_type_time'
    ) THEN
        ALTER TABLE `qa_conversation`
            ADD INDEX `idx_answer_type_time` (`answer_type`, `create_time`);
    END IF;
END//
DELIMITER ;

CALL patch_chat_agent_metadata();
DROP PROCEDURE IF EXISTS patch_chat_agent_metadata;
