-- Add final_score column to scenario_progress table

USE anti_fraud_platform;

ALTER TABLE scenario_progress ADD COLUMN final_score INT DEFAULT 0 COMMENT 'final score' AFTER start_time;
