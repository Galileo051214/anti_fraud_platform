-- Add Agent模拟挑战 schema support.
USE anti_fraud_platform;

ALTER TABLE `challenge`
    MODIFY COLUMN `type` ENUM('quiz', 'scenario', 'agent_scenario') NOT NULL DEFAULT 'quiz' COMMENT '类型:答题/情景模拟/Agent模拟挑战';

DROP PROCEDURE IF EXISTS patch_challenge_agent_config;
DELIMITER //
CREATE PROCEDURE patch_challenge_agent_config()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'challenge'
          AND COLUMN_NAME = 'agent_config'
    ) THEN
        ALTER TABLE `challenge`
            ADD COLUMN `agent_config` JSON COMMENT 'Agent情景模拟配置JSON' AFTER `scripts`;
    END IF;
END //
DELIMITER ;
CALL patch_challenge_agent_config();
DROP PROCEDURE IF EXISTS patch_challenge_agent_config;

CREATE TABLE IF NOT EXISTS `agent_challenge_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `challenge_id` BIGINT NOT NULL COMMENT 'Agent挑战关卡ID',
    `status` ENUM('in_progress', 'completed', 'failed') NOT NULL DEFAULT 'in_progress' COMMENT '状态',
    `current_round` INT NOT NULL DEFAULT 1 COMMENT '当前轮次',
    `messages` JSON COMMENT '脱敏后的对话摘要',
    `scoring_report` JSON COMMENT '评分报告',
    `summary` VARCHAR(1000) COMMENT '会话摘要',
    `final_score` INT DEFAULT 0 COMMENT '最终得分',
    `passed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否通过',
    `reward_granted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否发放每日奖励',
    `reward_date` DATE COMMENT '奖励日期',
    `start_time` DATETIME COMMENT '开始时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_session_id` (`session_id`),
    KEY `idx_agent_session_user` (`user_id`, `challenge_id`, `status`),
    CONSTRAINT `fk_acs_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_acs_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent模拟挑战会话表';

CREATE TABLE IF NOT EXISTS `agent_challenge_daily_reward` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `reward_date` DATE NOT NULL COMMENT '奖励自然日(Asia/Shanghai)',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `challenge_id` BIGINT NOT NULL COMMENT 'Agent挑战关卡ID',
    `score` INT NOT NULL COMMENT '达标得分',
    `reward_score` INT NOT NULL DEFAULT 100 COMMENT '奖励积分',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_daily_reward` (`user_id`, `reward_date`),
    KEY `idx_agent_reward_session` (`session_id`),
    CONSTRAINT `fk_acdr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_acdr_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent模拟挑战每日奖励表';

UPDATE `challenge`
SET `passing_score` = 75
WHERE `type` = 'agent_scenario';
