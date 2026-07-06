-- =====================================================
-- 反诈骗学习平台 数据库初始化脚本
-- 版本: 1.0
-- 日期: 2026-03-19
-- =====================================================

-- 1. 创建数据库
DROP DATABASE IF EXISTS `anti_fraud_platform`;
CREATE DATABASE `anti_fraud_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `anti_fraud_platform`;

-- =====================================================
-- 2. 用户管理模块
-- =====================================================

-- 用户表
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码(加密存储)',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `student_no` VARCHAR(30) COMMENT '学号',
    `role` ENUM('student', 'admin') NOT NULL DEFAULT 'student' COMMENT '角色:学生/管理员',
    `grade` VARCHAR(20) COMMENT '年级(如:大一、大二)',
    `major` VARCHAR(100) COMMENT '专业',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用1正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` DATETIME NULL COMMENT '最后登录时间(成就连续学习统计)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    INDEX `idx_role` (`role`),
    INDEX `idx_grade_major` (`grade`, `major`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户积分表
CREATE TABLE `user_score` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_score` INT NOT NULL DEFAULT 0 COMMENT '总积分',
    `current_level` INT NOT NULL DEFAULT 1 COMMENT '当前等级',
    `weekly_score` INT NOT NULL DEFAULT 0 COMMENT '本周积分',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_score_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分表';

-- 用户画像表(上下文感知用)
CREATE TABLE `user_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `grade` VARCHAR(20) COMMENT '年级',
    `major` VARCHAR(100) COMMENT '专业',
    `knowledge_level` INT NOT NULL DEFAULT 0 COMMENT '知识水平(0-100)',
    `weak_points` JSON COMMENT '弱点标签数组,如:["刷单诈骗","杀猪盘"]',
    `interest_tags` JSON COMMENT '兴趣标签数组',
    `lifecycle_stage` ENUM('newbie', 'growing', 'mature') NOT NULL DEFAULT 'newbie' COMMENT '生命周期阶段',
    `browse_count` INT NOT NULL DEFAULT 0 COMMENT '浏览记录数',
    `register_days` INT NOT NULL DEFAULT 0 COMMENT '注册天数',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像表';

-- 成就表
CREATE TABLE `achievement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL COMMENT '成就代码',
    `name` VARCHAR(50) NOT NULL COMMENT '成就名称',
    `description` VARCHAR(255) COMMENT '成就描述',
    `icon` VARCHAR(255) COMMENT '成就图标',
    `score_reward` INT NOT NULL DEFAULT 0 COMMENT '积分奖励',
    `condition_type` VARCHAR(50) COMMENT '达成条件类型',
    `condition_value` INT COMMENT '达成条件值',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';

-- 用户成就记录表
CREATE TABLE `user_achievement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `achievement_id` BIGINT NOT NULL COMMENT '成就ID',
    `achieved_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '达成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_id`),
    CONSTRAINT `fk_ua_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ua_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `achievement` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就记录表';

-- =====================================================
-- 3. 资讯发布模块
-- =====================================================

-- 资讯分类表
CREATE TABLE `news_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资讯分类表';

-- 资讯表
CREATE TABLE `news` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容(富文本)',
    `summary` VARCHAR(500) COMMENT '摘要',
    `cover_image` VARCHAR(255) COMMENT '封面图',
    `category_id` BIGINT COMMENT '分类ID',
    `author_id` BIGINT COMMENT '作者ID(管理员)',
    `news_type` ENUM('news', 'warning', 'policy') NOT NULL DEFAULT 'news' COMMENT '类型:新闻/预警/政策',
    `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶:0否1是',
    `is_mandatory` TINYINT NOT NULL DEFAULT 0 COMMENT '是否全校必读:0否1是',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0草稿1已发布',
    `publish_time` DATETIME COMMENT '发布时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_author` (`author_id`),
    KEY `idx_publish_time` (`publish_time`),
    KEY `idx_status_type` (`status`, `news_type`),
    CONSTRAINT `fk_news_category` FOREIGN KEY (`category_id`) REFERENCES `news_category` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_news_author` FOREIGN KEY (`author_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资讯表';

-- 资讯点赞表
CREATE TABLE `news_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `news_id` BIGINT NOT NULL COMMENT '资讯ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_news_user` (`news_id`, `user_id`),
    CONSTRAINT `fk_nl_news` FOREIGN KEY (`news_id`) REFERENCES `news` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_nl_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资讯点赞表';

-- 资讯浏览记录表
CREATE TABLE `news_browse_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `news_id` BIGINT NOT NULL COMMENT '资讯ID',
    `browse_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    `stay_duration` INT NOT NULL DEFAULT 0 COMMENT '停留时长(秒)',
    PRIMARY KEY (`id`),
    KEY `idx_user_browse` (`user_id`, `browse_time`),
    CONSTRAINT `fk_nbl_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_nbl_news` FOREIGN KEY (`news_id`) REFERENCES `news` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资讯浏览记录表';

-- =====================================================
-- 4. 案例展示模块
-- =====================================================

-- 案例标签表
CREATE TABLE `case_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `category` VARCHAR(50) COMMENT '标签分类',
    `description` VARCHAR(255) COMMENT '标签描述',
    `color` VARCHAR(20) COMMENT '标签颜色',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案例标签表';

-- 诈骗案例表
CREATE TABLE `fraud_case` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '案例标题',
    `case_type` VARCHAR(50) NOT NULL COMMENT '案例类型',
    `content` TEXT NOT NULL COMMENT '案例详情',
    `scripts` JSON COMMENT '诈骗剧本结构(JSON决策树)',
    `target_grades` JSON COMMENT '目标年级数组,如:["大一","大二"]或["all"]',
    `target_majors` JSON COMMENT '目标专业数组',
    `difficulty_level` INT NOT NULL DEFAULT 1 COMMENT '难度等级(1-5)',
    `risk_score` DECIMAL(3,1) NOT NULL DEFAULT 0 COMMENT '风险评分',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `like_rate` DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '点赞率',
    `wilson_score` DECIMAL(6,4) NOT NULL DEFAULT 0 COMMENT '威尔逊置信度得分',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用1启用',
    `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选:0否1是',
    `publish_time` DATETIME COMMENT '发布时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`case_type`),
    KEY `idx_difficulty` (`difficulty_level`),
    KEY `idx_wilson` (`wilson_score`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诈骗案例表';

-- 案例-标签关联表
CREATE TABLE `case_tag_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `case_id` BIGINT NOT NULL COMMENT '案例ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_case_tag` (`case_id`, `tag_id`),
    CONSTRAINT `fk_ctr_case` FOREIGN KEY (`case_id`) REFERENCES `fraud_case` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ctr_tag` FOREIGN KEY (`tag_id`) REFERENCES `case_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案例标签关联表';

-- 案例点赞表
CREATE TABLE `case_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `case_id` BIGINT NOT NULL COMMENT '案例ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_case_user` (`case_id`, `user_id`),
    CONSTRAINT `fk_cl_case` FOREIGN KEY (`case_id`) REFERENCES `fraud_case` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cl_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案例点赞表';

-- 案例浏览记录表
CREATE TABLE `case_browse_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `case_id` BIGINT NOT NULL COMMENT '案例ID',
    `browse_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    `stay_duration` INT NOT NULL DEFAULT 0 COMMENT '停留时长(秒)',
    PRIMARY KEY (`id`),
    KEY `idx_user_browse` (`user_id`, `browse_time`),
    CONSTRAINT `fk_cbl_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cbl_case` FOREIGN KEY (`case_id`) REFERENCES `fraud_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案例浏览记录表';

-- =====================================================
-- 5. 游戏化学习模块
-- =====================================================

-- 闯关关卡表
CREATE TABLE `challenge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL COMMENT '关卡名称',
    `description` VARCHAR(255) COMMENT '关卡描述',
    `level_order` INT NOT NULL COMMENT '关卡顺序',
    `difficulty` INT NOT NULL DEFAULT 1 COMMENT '难度(1-5)',
    `type` ENUM('quiz', 'scenario', 'agent_scenario') NOT NULL DEFAULT 'quiz' COMMENT '类型:答题/情景模拟/Agent模拟挑战',
    `passing_score` INT NOT NULL DEFAULT 60 COMMENT '及格分数',
    `score_reward` INT NOT NULL DEFAULT 0 COMMENT '通关奖励积分',
    `content` JSON NULL COMMENT '题目JSON(答题类型)',
    `scripts` JSON COMMENT '情景剧本JSON(FSM状态机)',
    `agent_config` JSON COMMENT 'Agent情景模拟配置JSON',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`level_order`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闯关关卡表';

-- 用户闯关记录表
CREATE TABLE `user_challenge_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `challenge_id` BIGINT NOT NULL COMMENT '关卡ID',
    `attempts` INT NOT NULL DEFAULT 1 COMMENT '尝试次数',
    `score` INT COMMENT '得分',
    `passed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否通关:0否1是',
    `answer_detail` JSON COMMENT '答题详情',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_challenge` (`user_id`, `challenge_id`),
    CONSTRAINT `fk_ucr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ucr_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户闯关记录表';

-- 情景模拟进度表
CREATE TABLE `scenario_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `challenge_id` BIGINT NOT NULL COMMENT '情景模拟ID',
    `current_node` VARCHAR(50) COMMENT '当前节点ID',
    `decision_history` JSON COMMENT '决策历史',
    `status` ENUM('in_progress', 'completed', 'failed') NOT NULL DEFAULT 'in_progress' COMMENT '状态',
    `start_time` DATETIME COMMENT '开始时间',
    `final_score` INT DEFAULT 0 COMMENT '最终得分',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_scenario` (`user_id`, `challenge_id`),
    CONSTRAINT `fk_sp_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sp_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情景模拟进度表';

-- Agent模拟挑战会话表
CREATE TABLE `agent_challenge_session` (
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

-- Agent模拟挑战每日奖励表
CREATE TABLE `agent_challenge_daily_reward` (
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

-- 排行榜表
CREATE TABLE `leaderboard` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `period_type` ENUM('daily', 'weekly', 'all') NOT NULL COMMENT '周期类型',
    `score` INT NOT NULL DEFAULT 0 COMMENT '周期内积分',
    `rank` INT COMMENT '排名',
    `update_date` DATE COMMENT '更新日期',
    PRIMARY KEY (`id`),
    KEY `idx_period_rank` (`period_type`, `update_date`, `rank`),
    CONSTRAINT `fk_lb_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜表';

-- =====================================================
-- 6. 社区互动模块
-- =====================================================

-- 论坛帖子表
CREATE TABLE `forum_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) COMMENT '帖子标题',
    `content` TEXT NOT NULL COMMENT '帖子内容',
    `post_type` ENUM('experience', 'question', 'discussion') NOT NULL DEFAULT 'experience' COMMENT '类型',
    `tag_ids` JSON COMMENT '标签ID数组',
    `image_urls` JSON COMMENT '帖子图片URL数组',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选:0否1是',
    `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶:0否1是',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_type_time` (`post_type`, `create_time`),
    KEY `idx_like_count` (`like_count`),
    CONSTRAINT `fk_fp_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';

-- 帖子点赞表
CREATE TABLE `post_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    CONSTRAINT `fk_pl_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pl_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';

-- 评论表
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_parent` (`post_id`, `parent_id`),
    CONSTRAINT `fk_c_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_c_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 评论点赞表
CREATE TABLE `comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    CONSTRAINT `fk_clike_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_clike_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- =====================================================
-- 7. 智能客服与问答模块
-- =====================================================

-- 问答会话记录表
CREATE TABLE `qa_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(50) NOT NULL COMMENT '会话ID',
    `question` TEXT NOT NULL COMMENT '用户问题',
    `answer` TEXT COMMENT 'AI回答',
    `model` VARCHAR(50) NOT NULL DEFAULT 'deepseek' COMMENT '使用的模型',
    `tokens_used` INT COMMENT '消耗token数',
    `feedback` TINYINT COMMENT '用户反馈:1满意-1不满意',
    `answer_type` VARCHAR(32) NOT NULL DEFAULT 'qa' COMMENT '回答类型:qa/latest_report',
    `risk_level` VARCHAR(16) NOT NULL DEFAULT 'low' COMMENT '风险等级:low/medium/high',
    `sources_json` JSON COMMENT '检索来源JSON数组',
    `fallback` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否降级回答',
    `retrieved_at` DATETIME COMMENT '检索时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_session` (`user_id`, `session_id`),
    KEY `idx_answer_type_time` (`answer_type`, `create_time`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_qa_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答会话记录表';

-- =====================================================
-- 8. 推荐算法相关表
-- =====================================================

-- 关联规则库表(SPM算法用)
CREATE TABLE `association_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `trigger_tag` VARCHAR(50) NOT NULL COMMENT '触发标签',
    `predicted_tags` JSON NOT NULL COMMENT '预测标签数组',
    `confidence` DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '置信度',
    `description` VARCHAR(255) COMMENT '规则描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_trigger` (`trigger_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关联规则库表';

-- 用户-标签行为矩阵表
CREATE TABLE `user_behavior_matrix` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `behavior_score` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '行为得分',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`),
    KEY `idx_tag_score` (`tag_id`, `behavior_score`),
    CONSTRAINT `fk_ubm_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ubm_tag` FOREIGN KEY (`tag_id`) REFERENCES `case_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-标签行为矩阵表';

-- 用户相似度表(协同过滤用)
CREATE TABLE `user_similarity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id_a` BIGINT NOT NULL COMMENT '用户A ID',
    `user_id_b` BIGINT NOT NULL COMMENT '用户B ID',
    `similarity_score` DECIMAL(5,4) COMMENT '皮尔逊相关系数',
    `common_tags` JSON COMMENT '共同标签',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_pair` (`user_id_a`, `user_id_b`),
    KEY `idx_similarity` (`similarity_score`),
    CONSTRAINT `fk_us_user_a` FOREIGN KEY (`user_id_a`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_us_user_b` FOREIGN KEY (`user_id_b`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户相似度表';

-- 推荐记录日志表
CREATE TABLE `recommendation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `item_type` ENUM('news', 'case', 'challenge') NOT NULL COMMENT '推荐类型',
    `item_id` BIGINT NOT NULL COMMENT '推荐内容ID',
    `recommend_reason` JSON COMMENT '推荐原因',
    `score` DECIMAL(6,4) COMMENT '推荐得分',
    `lifecycle_stage` VARCHAR(20) COMMENT '用户生命周期阶段',
    `clicked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否点击:0否1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    CONSTRAINT `fk_rl_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐记录日志表';

-- =====================================================
-- 9. 数据统计模块
-- =====================================================

-- 每日统计表
CREATE TABLE `daily_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `daily_active_users` INT NOT NULL DEFAULT 0 COMMENT '日活用户数',
    `new_users` INT NOT NULL DEFAULT 0 COMMENT '新增用户数',
    `total_page_views` INT NOT NULL DEFAULT 0 COMMENT '页面浏览量',
    `case_views` JSON COMMENT '各类型案例浏览量',
    `top_cases` JSON COMMENT 'TOP案例ID数组',
    `challenge_completions` INT NOT NULL DEFAULT 0 COMMENT '闯关完成数',
    `avg_test_score` DECIMAL(5,2) COMMENT '平均测试得分',
    `new_posts` INT NOT NULL DEFAULT 0 COMMENT '新增帖子数',
    `new_comments` INT NOT NULL DEFAULT 0 COMMENT '新增评论数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计表';

-- 院系统计数据表
CREATE TABLE `department_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `grade` VARCHAR(20) COMMENT '年级',
    `major` VARCHAR(100) COMMENT '专业',
    `user_count` INT NOT NULL DEFAULT 0 COMMENT '用户数',
    `avg_knowledge_level` DECIMAL(5,2) COMMENT '平均知识水平',
    `avg_test_score` DECIMAL(5,2) COMMENT '平均测试得分',
    `completion_rate` DECIMAL(5,4) COMMENT '学习完成率',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_grade_major` (`stat_date`, `grade`, `major`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院系统计数据表';

-- =====================================================
-- 10. 初始化数据
-- =====================================================

-- 插入试运行账号 (密码均为: 123456，经过BCrypt加密)
INSERT INTO `sys_user`
    (`username`, `password`, `nickname`, `role`, `status`, `student_no`, `grade`, `major`)
VALUES
    ('admin', '$2a$10$XiwC71Y3ltBnetOwm/DuIOtNJlsCkp09h8gQi0DjEmVMQ2sokalPu', '系统管理员', 'admin', 1, NULL, NULL, NULL),
    ('student001', '$2a$10$XiwC71Y3ltBnetOwm/DuIOtNJlsCkp09h8gQi0DjEmVMQ2sokalPu', '演示学生', 'student', 1, '20260001', '大一', '软件工程');

-- 初始化演示学生积分和画像
INSERT INTO `user_score` (`user_id`, `total_score`, `current_level`, `weekly_score`)
SELECT `id`, 0, 1, 0
FROM `sys_user`
WHERE `username` = 'student001';

INSERT INTO `user_profile`
    (`user_id`, `grade`, `major`, `knowledge_level`, `lifecycle_stage`, `browse_count`, `register_days`)
SELECT `id`, `grade`, `major`, 0, 'newbie', 0, 0
FROM `sys_user`
WHERE `username` = 'student001';

-- 初始化资讯分类
INSERT INTO `news_category` (`name`, `parent_id`, `sort_order`) VALUES
('金融安全', 0, 1),
('官方预警', 0, 2),
('政策法规', 0, 3),
('防骗指南', 0, 4);

-- 初始化案例标签
INSERT INTO `case_tag` (`name`, `category`, `description`, `color`) VALUES
('刷单诈骗', '网络诈骗', '刷单返利类诈骗', '#FF6B6B'),
('杀猪盘', '网络诈骗', '网络交友诱导投资赌博', '#9B59B6'),
('冒充客服', '电话诈骗', '冒充平台客服诈骗', '#3498DB'),
('网络贷款', '金融诈骗', '虚假贷款诈骗', '#2ECC71'),
('冒充公检法', '电话诈骗', '冒充公安机关诈骗', '#E74C3C'),
('游戏交易', '网络诈骗', '游戏装备交易诈骗', '#F39C12'),
('求职招聘', '线下诈骗', '虚假招工诈骗', '#1ABC9C'),
('红包返利', '网络诈骗', '红包返利诈骗', '#E91E63');

-- 初始化成就数据
INSERT INTO `achievement` (`code`, `name`, `description`, `score_reward`, `condition_type`, `condition_value`) VALUES
('FIRST_LOGIN', '初来乍到', '完成首次登录', 10, 'login_count', 1),
('FIRST_BROWSE', '初识骗局', '浏览第一个案例', 5, 'browse_count', 1),
('FIRST_CHALLENGE', '初试牛刀', '完成第一次闯关', 10, 'challenge_count', 1),
('PERFECT_SCORE', '满分答人', '闯关获得满分', 20, 'perfect_score', 1),
('SHARING_KING', '防骗大使', '发布5篇帖子', 30, 'post_count', 5),
('HELPER', '热心网友', '发布10篇帖子', 50, 'post_count', 10),
('INVINCIBLE', '防骗达人', '连续7天学习', 100, 'continuous_days', 7),
('MASTER', '防骗大师', '完成所有闯关', 200, 'challenge_complete', 1);

-- 初始化关联规则(SPM算法用)
INSERT INTO `association_rule` (`trigger_tag`, `predicted_tags`, `confidence`, `description`) VALUES
('刷单诈骗', '["网络博彩","跑分洗钱"]', 0.85, '刷单诈骗常伴随网络博彩和跑分洗钱'),
('杀猪盘', '["网络贷款","投资诈骗"]', 0.78, '杀猪盘受害者可能被诱导贷款投资'),
('网络贷款', '["刷单诈骗","注销账户"]', 0.72, '贷款诈骗常与其他类型诈骗结合'),
('求职招聘', '["刷单诈骗","押金诈骗"]', 0.80, '虚假招聘常以刷单或押金形式诈骗'),
('游戏交易', '["充值诈骗","账号交易"]', 0.75, '游戏交易诈骗主要形式');

-- =====================================================
-- 11. 创建视图(便于查询)
-- =====================================================

-- 用户完整信息视图
CREATE OR REPLACE VIEW `v_user_info` AS
SELECT 
    u.id, u.username, u.nickname, u.avatar, u.phone, u.email, u.student_no,
    u.role, u.grade, u.major, u.status, u.create_time,
    s.total_score, s.current_level, s.weekly_score,
    p.knowledge_level, p.lifecycle_stage, p.browse_count
FROM sys_user u
LEFT JOIN user_score s ON u.id = s.user_id
LEFT JOIN user_profile p ON u.id = p.user_id;

-- 案例完整信息视图
CREATE OR REPLACE VIEW `v_case_full` AS
SELECT 
    c.id, c.title, c.case_type, c.difficulty_level, c.risk_score,
    c.view_count, c.like_count, c.wilson_score, c.is_featured, c.publish_time,
    GROUP_CONCAT(t.name) as tag_names,
    GROUP_CONCAT(t.id) as tag_ids
FROM fraud_case c
LEFT JOIN case_tag_relation r ON c.id = r.case_id
LEFT JOIN case_tag t ON r.tag_id = t.id
GROUP BY c.id;

