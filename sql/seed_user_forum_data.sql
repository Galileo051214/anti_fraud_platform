-- =====================================================
-- 用户数据与社区帖子种子数据
-- 用途：补充系统用户和社区帖子数据
-- 执行方式：
--   mysql -u root -p anti_fraud_platform < sql/seed_user_forum_data.sql
-- 注意：使用 username 去重，可幂等执行
-- =====================================================

USE anti_fraud_platform;

-- =====================================================
-- 第一部分：系统用户数据
-- =====================================================

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'admin', '$2a$10$Axp6.nbFIy3A.CGQb/GzIu.O3NGhxLrFib5lkd98EstnGOcw8Rnku', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', '13800138000', 'admin@anti-fraud.edu.cn', 'admin001', 'admin', '2023级', '软件工程', 1, '2026-06-01 09:00:00', '2026-07-01 10:00:00', '2026-07-07 15:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'admin');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'jianglelin', '$2a$10$W3uYSeQRSsxstjtexKS6IuuykG3mEoVdJMNLl0itVnvF8ZOVOhMp6', '江乐霖', 'https://api.dicebear.com/7.x/avataaars/svg?seed=jianglelin', '13900139001', 'jianglelin@anti-fraud.edu.cn', '23201317', 'student', '2023级', '软件工程', 1, '2026-06-10 14:00:00', '2026-07-05 09:30:00', '2026-07-07 14:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'jianglelin');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'wujunliang', '$2a$10$8pbh27YAYkhj.qIPucTBQu7WPNOb1AC/RFYGbdE138JS4w8SHCyvi', '吴俊椋', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wujunliang', '13900139002', 'wujunliang@anti-fraud.edu.cn', '23201330', 'student', '2023级', '软件工程', 1, '2026-06-10 14:30:00', '2026-07-04 16:00:00', '2026-07-07 13:45:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'wujunliang');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'zhuzhao', '$2a$10$exOSAi2HYlPcRuO7QEQiQ.0gEPaiYbR5DYO4y8JjswQ.mGgrBXkOm', '朱子浩', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhuzhao', '13900139003', 'zhuzhao@anti-fraud.edu.cn', '23201337', 'student', '2023级', '软件工程', 1, '2026-06-10 15:00:00', '2026-07-03 11:00:00', '2026-07-07 12:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'zhuzhao');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'zhangwei', '$2a$10$SSSQi.IApLbSCUrkIRXYReUoT0bOs6yNrqThFwO1CmCCEseQqH2lW', '张伟', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangwei', '13800138001', 'zhangwei@anti-fraud.edu.cn', '23201301', 'student', '2023级', '计算机科学与技术', 1, '2026-06-15 09:00:00', '2026-07-02 14:00:00', '2026-07-07 11:15:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'zhangwei');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'lili', '$2a$10$W3uYSeQRSsxstjtexKS6IuuykG3mEoVdJMNLl0itVnvF8ZOVOhMp6', '李丽', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lili', '13800138002', 'lili@anti-fraud.edu.cn', '23201302', 'student', '2023级', '信息管理', 1, '2026-06-15 10:00:00', '2026-07-01 15:30:00', '2026-07-06 16:45:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'lili');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'wangqiang', '$2a$10$8pbh27YAYkhj.qIPucTBQu7WPNOb1AC/RFYGbdE138JS4w8SHCyvi', '王强', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangqiang', '13800138003', 'wangqiang@anti-fraud.edu.cn', '23201303', 'student', '2023级', '网络工程', 1, '2026-06-16 09:30:00', '2026-06-30 10:00:00', '2026-07-05 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'wangqiang');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'chenyue', '$2a$10$exOSAi2HYlPcRuO7QEQiQ.0gEPaiYbR5DYO4y8JjswQ.mGgrBXkOm', '陈悦', 'https://api.dicebear.com/7.x/avataaars/svg?seed=chenyue', '13800138004', 'chenyue@anti-fraud.edu.cn', '23201304', 'student', '2023级', '软件工程', 1, '2026-06-16 11:00:00', '2026-06-29 14:30:00', '2026-07-04 17:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'chenyue');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'liuming', '$2a$10$Axp6.nbFIy3A.CGQb/GzIu.O3NGhxLrFib5lkd98EstnGOcw8Rnku', '刘明', 'https://api.dicebear.com/7.x/avataaars/svg?seed=liuming', '13800138005', 'liuming@anti-fraud.edu.cn', '23201305', 'student', '2023级', '计算机科学与技术', 1, '2026-06-17 08:00:00', '2026-06-28 16:00:00', '2026-07-03 10:15:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'liuming');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'zhaoxia', '$2a$10$SSSQi.IApLbSCUrkIRXYReUoT0bOs6yNrqThFwO1CmCCEseQqH2lW', '赵霞', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhaoxia', '13800138006', 'zhaoxia@anti-fraud.edu.cn', '23201306', 'student', '2023级', '软件工程', 1, '2026-06-17 14:00:00', '2026-06-27 09:00:00', '2026-07-02 15:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'zhaoxia');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'sunpeng', '$2a$10$W3uYSeQRSsxstjtexKS6IuuykG3mEoVdJMNLl0itVnvF8ZOVOhMp6', '孙鹏', 'https://api.dicebear.com/7.x/avataaars/svg?seed=sunpeng', '13800138007', 'sunpeng@anti-fraud.edu.cn', '23201307', 'student', '2023级', '网络工程', 1, '2026-06-18 10:00:00', '2026-06-26 11:30:00', '2026-07-01 12:45:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'sunpeng');

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `student_no`, `role`, `grade`, `major`, `status`, `create_time`, `update_time`, `last_login_time`)
SELECT 'zhouyan', '$2a$10$8pbh27YAYkhj.qIPucTBQu7WPNOb1AC/RFYGbdE138JS4w8SHCyvi', '周燕', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhouyan', '13800138008', 'zhouyan@anti-fraud.edu.cn', '23201308', 'student', '2023级', '信息管理', 1, '2026-06-18 15:00:00', '2026-06-25 14:00:00', '2026-06-30 09:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'zhouyan');

-- =====================================================
-- 第二部分：社区帖子数据
-- user_id 通过子查询从 sys_user 表根据 username 获取
-- =====================================================

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'jianglelin'), '亲身经历杀猪盘诈骗，分享我的反诈心得', '<p>上周接到一个自称证券公司客服的电话，对方能准确说出我的姓名和学校信息。一开始说要推荐股票，后来逐渐引导我下载一个「内部交易APP」。</p><p><strong>关键转折点：</strong>当对方要求我先充值小额资金时，我突然想起反诈课上学的知识——任何要求转账的都是诈骗！</p><p><strong>防范要点：</strong></p><ul><li>正规证券公司不会主动打电话推荐股票</li><li>任何要求转账到陌生账户的都是诈骗</li><li>遇到可疑情况立即挂断并拨打96110</li></ul>', 'experience', CAST('[1,2]' AS JSON), CAST('[]' AS JSON), 356, 42, 18, 1, 1, 1, '2026-07-01 10:30:00', '2026-07-05 16:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '亲身经历杀猪盘诈骗，分享我的反诈心得');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'wujunliang'), '大家帮我看看这是不是诈骗？', '<p>今天收到一条短信，说我名下的银行卡涉嫌洗钱，要求我添加微信「王警官」配合调查。短信里还附了一个链接，说可以查看案件详情。</p><p>我有点害怕，这到底是不是真的？有没有同学遇到过类似情况？</p><p>附：短信截图已上传，链接我没敢点。</p>', 'question', CAST('[3]' AS JSON), CAST('[]' AS JSON), 289, 23, 32, 0, 0, 1, '2026-07-03 14:00:00', '2026-07-03 14:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '大家帮我看看这是不是诈骗？');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'zhuzhao'), '关于AI换脸诈骗的深度分析', '<p>最近AI换脸诈骗越来越猖獗，我搜集了一些案例和技术原理，跟大家分享。</p><p><strong>技术原理：</strong></p><p>AI换脸技术通过深度学习算法，将目标人物的面部特征替换到视频中，达到以假乱真的效果。诈骗分子通常会：</p><ol><li>获取目标人物的照片或视频素材</li><li>使用换脸软件生成虚假视频</li><li>冒充熟人进行诈骗</li></ol><p><strong>防范建议：</strong>涉及资金转账时，务必通过多种方式验证对方身份，比如打电话、视频通话等。</p>', 'discussion', CAST('[4]' AS JSON), CAST('[]' AS JSON), 421, 56, 24, 1, 0, 1, '2026-07-02 09:30:00', '2026-07-04 11:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '关于AI换脸诈骗的深度分析');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'zhangwei'), '校园贷诈骗套路总结', '<p>作为一名大三学生，我整理了校园贷诈骗的常见套路，希望能帮助学弟学妹们提高警惕。</p><p><strong>套路一：注销校园贷</strong></p><p>诈骗分子冒充贷款平台客服，声称你上学时注册的贷款账号需要注销，否则会影响征信。然后引导你转账「清零」账户。</p><p><strong>套路二：低息贷款陷阱</strong></p><p>以「低利息、秒到账」为诱饵，要求你先交「保证金」「手续费」，最后卷款跑路。</p><p><strong>套路三：培训贷</strong></p><p>声称提供免费培训，实际诱导你办理贷款支付培训费。</p>', 'experience', CAST('[5]' AS JSON), CAST('[]' AS JSON), 512, 78, 35, 1, 0, 1, '2026-07-04 16:00:00', '2026-07-06 13:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '校园贷诈骗套路总结');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'lili'), '反诈小技巧分享：如何识别虚假APP', '<p>给大家分享几个识别虚假APP的小技巧：</p><ol><li><strong>看下载渠道：</strong>只从官方应用商店下载，不要点击短信或网页链接</li><li><strong>看开发者信息：</strong>正规APP会有详细的开发者信息和公司名称</li><li><strong>看权限要求：</strong>不必要的权限要求（如读取短信、通讯录）要警惕</li><li><strong>看界面质量：</strong>界面粗糙、有明显错别字的大概率是诈骗APP</li></ol><p>记住一个原则：凡是要求转账的APP，一律卸载！</p>', 'discussion', CAST('[1,2]' AS JSON), CAST('[]' AS JSON), 278, 34, 15, 0, 0, 1, '2026-07-05 11:00:00', '2026-07-05 11:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '反诈小技巧分享：如何识别虚假APP');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'wangqiang'), '紧急求助！接到自称公检法的电话', '<p>刚刚接到一个电话，对方自称是北京市公安局的，说我涉嫌诈骗，要我配合调查。还说要冻结我的账户，让我把钱转到「安全账户」。</p><p>我现在很慌，不知道该怎么办？有没有懂的同学帮忙分析一下？</p>', 'question', CAST('[3]' AS JSON), CAST('[]' AS JSON), 367, 45, 52, 0, 0, 1, '2026-07-06 10:00:00', '2026-07-06 10:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '紧急求助！接到自称公检法的电话');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'chenyue'), '分享一次成功识破诈骗的经历', '<p>昨天晚上接到一个电话，说是快递丢失了要给我理赔。对方报出了我的收件地址和电话，听起来很真实。</p><p>但是！当对方要求我提供银行卡号和验证码时，我立刻警觉了——正规快递理赔只会原路退回，怎么会要验证码呢？</p><p>我直接挂断电话，然后去菜鸟裹裹上查询，发现根本没有丢件。果然是诈骗！</p>', 'experience', CAST('[2]' AS JSON), CAST('[]' AS JSON), 234, 38, 12, 0, 0, 1, '2026-07-04 09:00:00', '2026-07-04 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '分享一次成功识破诈骗的经历');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'liuming'), '谈谈对新型诈骗的看法', '<p>最近研究了一些新型诈骗手段，感觉诈骗分子的套路越来越深了。以前可能只是电话诈骗，现在结合了AI、社交工程等多种手段。</p><p><strong>我的几点看法：</strong></p><ul><li>反诈教育要与时俱进，及时更新案例</li><li>个人信息保护很重要，不要轻易泄露</li><li>遇到涉及资金的事情，一定要冷静思考</li></ul><p>希望咱们平台能多分享一些最新的诈骗案例，帮助大家提高防范意识。</p>', 'discussion', CAST('[1,4]' AS JSON), CAST('[]' AS JSON), 189, 26, 19, 0, 0, 1, '2026-07-03 15:30:00', '2026-07-05 14:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '谈谈对新型诈骗的看法');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'zhaoxia'), '兼职刷单诈骗太坑了！', '<p>室友差点被骗了！她在微信群看到一个刷单兼职，说是「日入200元」，先让她刷单小额返现，然后逐渐加大金额。</p><p>当她投入5000元后，对方说任务没完成，要再投10000元才能提现。幸好被我们及时发现制止了！</p><p><strong>提醒大家：</strong>所有刷单兼职都是诈骗！不要相信任何「低投入高回报」的兼职！</p>', 'experience', CAST('[2]' AS JSON), CAST('[]' AS JSON), 456, 67, 28, 1, 0, 1, '2026-07-02 14:00:00', '2026-07-06 10:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '兼职刷单诈骗太坑了！');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'sunpeng'), '如何保护个人信息不被泄露？', '<p>最近总接到骚扰电话和诈骗短信，怀疑个人信息被泄露了。想问问大家平时都是怎么保护个人信息的？</p><p>我先说几个：</p><ol><li>不随便填问卷调查</li><li>快递单信息要涂黑</li><li>不在公共WiFi下进行支付</li></ol><p>欢迎补充！</p>', 'discussion', CAST('[1,3]' AS JSON), CAST('[]' AS JSON), 312, 41, 43, 0, 0, 1, '2026-07-05 16:00:00', '2026-07-05 16:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '如何保护个人信息不被泄露？');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'zhouyan'), '反诈APP真的有用吗？', '<p>最近下载了国家反诈中心APP，想问问大家觉得这个APP真的有用吗？</p><p>我用了一段时间，感觉拦截骚扰电话还不错，但是诈骗预警功能好像没怎么触发过。</p><p>大家有什么使用心得吗？</p>', 'discussion', CAST('[1]' AS JSON), CAST('[]' AS JSON), 267, 32, 27, 0, 0, 1, '2026-07-06 14:00:00', '2026-07-06 14:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '反诈APP真的有用吗？');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'jianglelin'), '总结一下最近的高发诈骗类型', '<p>根据最近的新闻和案例，总结一下当前高发的诈骗类型：</p><ol><li><strong>杀猪盘诈骗：</strong>通过网恋建立信任，诱导投资</li><li><strong>冒充客服退款诈骗：</strong>谎称订单异常，要求转账</li><li><strong>AI换脸诈骗：</strong>冒充熟人进行视频诈骗</li><li><strong>公检法诈骗：</strong>冒充警察恐吓转账</li><li><strong>注销校园贷诈骗：</strong>以征信为由诱导转账</li></ol><p>大家一定要提高警惕，遇到可疑情况及时报警！</p>', 'discussion', CAST('[1,2,3,4,5]' AS JSON), CAST('[]' AS JSON), 567, 89, 45, 1, 1, 1, '2026-07-07 09:00:00', '2026-07-07 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '总结一下最近的高发诈骗类型');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'wujunliang'), '收到一条奇怪的验证码短信', '<p>今天收到一条验证码短信，说是我在某平台注册了账号。但我根本没注册过啊！</p><p>短信内容：「您的验证码是123456，有效期5分钟」</p><p>这种情况应该怎么办？是不是有人用我的手机号注册？</p>', 'question', CAST('[3]' AS JSON), CAST('[]' AS JSON), 198, 21, 31, 0, 0, 1, '2026-07-06 17:00:00', '2026-07-06 17:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '收到一条奇怪的验证码短信');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'zhuzhao'), '反诈知识闯关体验分享', '<p>今天体验了平台的知识闯关功能，感觉非常好！题目设计很贴近实际，答题过程中还能学到很多反诈知识。</p><p>建议大家都去试试，既能学习又能获得积分，一举两得！</p><p>希望平台能增加更多关卡，特别是针对AI诈骗的题目。</p>', 'discussion', CAST('[1]' AS JSON), CAST('[]' AS JSON), 245, 39, 16, 0, 0, 1, '2026-07-07 10:30:00', '2026-07-07 10:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '反诈知识闯关体验分享');

INSERT INTO `forum_post` (`user_id`, `title`, `content`, `post_type`, `tag_ids`, `image_urls`, `view_count`, `like_count`, `comment_count`, `is_featured`, `is_top`, `status`, `create_time`, `update_time`)
SELECT (SELECT id FROM sys_user WHERE username = 'zhangwei'), '警惕！冒充老师的诈骗电话', '<p>上周接到一个电话，对方自称是我辅导员，说学校要收什么费用。幸好我先打电话给辅导员核实了一下，才发现是诈骗！</p><p>提醒大家：接到自称老师的电话，一定要先通过官方渠道核实身份！</p>', 'experience', CAST('[3]' AS JSON), CAST('[]' AS JSON), 312, 47, 23, 0, 0, 1, '2026-07-04 11:00:00', '2026-07-04 11:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `title` = '警惕！冒充老师的诈骗电话');
