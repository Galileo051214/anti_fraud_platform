-- 修复 content 字段允许为空
USE anti_fraud_platform;

ALTER TABLE challenge MODIFY COLUMN content JSON NULL COMMENT '题目内容(JSON格式,答题类型)';
