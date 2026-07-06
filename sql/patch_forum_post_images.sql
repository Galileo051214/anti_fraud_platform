-- Existing DB migration: add image_urls to forum_post.
-- 可重复执行：字段已存在时不会再次添加。

USE anti_fraud_platform;

DROP PROCEDURE IF EXISTS patch_forum_post_images;
DELIMITER //
CREATE PROCEDURE patch_forum_post_images()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'forum_post'
          AND COLUMN_NAME = 'image_urls'
    ) THEN
        ALTER TABLE `forum_post`
            ADD COLUMN `image_urls` JSON NULL COMMENT '帖子图片URL数组' AFTER `tag_ids`;
    END IF;
END//
DELIMITER ;

CALL patch_forum_post_images();
DROP PROCEDURE IF EXISTS patch_forum_post_images;
