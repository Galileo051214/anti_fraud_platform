-- 已有库增量：成就「连续学习」统计用（新库请直接执行 init.sql）
ALTER TABLE `sys_user`
    ADD COLUMN `last_login_time` DATETIME NULL COMMENT '最后登录时间(成就连续学习统计)' AFTER `update_time`;
