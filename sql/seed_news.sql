-- =====================================================
-- 资讯（咨询）种子数据
-- 用途：向 news 表插入 10 条已发布资讯
-- 前置：已执行 sql/init.sql（含 news_category、管理员 sys_user id=1）
-- 执行：mysql -u root -p anti_fraud_platform < sql/seed_news.sql
-- =====================================================

USE anti_fraud_platform;

-- 1 官方预警
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '高校师生谨防「注销校园贷」诈骗',
  '<p>近期出现不法分子冒充网贷平台客服，以「注销校园贷账户、否则影响征信」为由诱导转账或共享屏幕。</p><p><strong>提示：</strong>个人征信由官方渠道管理，凡是要求向陌生账户转账的均为诈骗；请通过官方 App 或柜台核实。</p>',
  '警惕冒充客服以注销校园贷为名实施诈骗，勿向陌生账户转账。',
  NULL, 2, 1, 'warning', 1, 1, 1860, 1, NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '高校师生谨防「注销校园贷」诈骗');

-- 2 防骗指南
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '大学生兼职刷单骗局高发提醒',
  '<p>刷单返利类诈骗常以「足不出户、日赚百元」为诱饵，前期小额返利骗取信任，后期要求大额垫付后失联。</p><p>任何要求垫资的网络兼职均涉嫌违法或诈骗，请拒绝参与。</p>',
  '刷单兼职多为诈骗，勿垫付资金。',
  NULL, 4, 1, 'news', 0, 0, 1425, 1, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '大学生兼职刷单骗局高发提醒');

-- 3 政策法规
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '《反电信网络诈骗法》与学生权益保护要点',
  '<p>法律明确电信、金融、互联网等主体的风险防控义务，并加大对涉诈活动的惩戒力度。</p><p>师生发现涉诈线索可向学校保卫部门或公安机关反映，注意留存通话、聊天与转账记录。</p>',
  '梳理反诈法与校园场景相关的权利义务与举报途径。',
  NULL, 3, 1, 'policy', 0, 0, 980, 1, NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '《反电信网络诈骗法》与学生权益保护要点');

-- 4 金融安全
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '如何识别虚假投资理财类 App',
  '<p>虚假投资平台常通过非应用商店链接下载、承诺「稳赚不赔」、限制提现并要求缴纳保证金。</p><p>投资请选择持牌机构，核实备案与监管信息，勿轻信社交群内的「内幕消息」。</p>',
  '从下载渠道、收益承诺、提现规则等维度识别假投资平台。',
  NULL, 1, 1, 'news', 0, 0, 1120, 1, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '如何识别虚假投资理财类 App');

-- 5 官方预警
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '警惕冒充教务、辅导员收取「教材费」「活动费」',
  '<p>诈骗分子盗取或仿冒老师社交账号，在群内发布紧急缴费通知并附收款码。</p><p>缴费前请通过电话或当面与学校官方渠道二次确认，不扫来源不明的收款码。</p>',
  '涉学校收费信息务必线下或官方电话核实。',
  NULL, 2, 1, 'warning', 0, 0, 756, 1, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '警惕冒充教务、辅导员收取「教材费」「活动费」');

-- 6 金融安全
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '理性消费，远离「套路贷」与非法网贷',
  '<p>套路贷通过虚增债务、恶意垒高金额、暴力催收等手段侵害借款人。</p><p>有资金需求应优先考虑正规银行或学校资助渠道，仔细阅读合同条款，拒绝空白合同。</p>',
  '倡导理性借贷，识别套路贷特征与维权途径。',
  NULL, 1, 1, 'news', 0, 0, 640, 1, NOW() - INTERVAL 7 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '理性消费，远离「套路贷」与非法网贷');

-- 7 防骗指南
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '收到「积分即将清零」短信？先核实再点击',
  '<p>伪基站或改号短信常仿冒运营商、银行，诱导点击钓鱼链接套取账号密码。</p><p>请通过官方 App 或客服热线查询积分与活动，勿在陌生页面输入银行卡与验证码。</p>',
  '积分兑换类短信需通过官方渠道核实，慎点短链。',
  NULL, 4, 1, 'news', 0, 0, 892, 1, NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '收到「积分即将清零」短信？先核实再点击');

-- 8 政策法规
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '校园内规范网络借贷宣传的风险提示',
  '<p>各单位应加强对网贷、培训贷等营销活动的管理，防止夸大宣传与诱导过度负债。</p><p>学生参与培训或分期前，应核实机构资质与合同条款，必要时咨询学校法务或消费维权部门。</p>',
  '从校园治理与学生角度提示网贷营销风险。',
  NULL, 3, 1, 'policy', 0, 0, 415, 1, NOW() - INTERVAL 10 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '校园内规范网络借贷宣传的风险提示');

-- 9 官方预警
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '游戏账号、装备交易诈骗预警',
  '<p>常见手法包括要求私下转账、伪造支付截图、诱导下载远程软件操控设备等。</p><p>尽量使用平台官方担保交易，拒绝站外付款；不向陌生人透露二次验证信息。</p>',
  '游戏交易走官方渠道，避免私下转账。',
  NULL, 2, 1, 'warning', 0, 0, 1205, 1, NOW() - INTERVAL 11 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '游戏账号、装备交易诈骗预警');

-- 10 防骗指南（新生必读）
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '新生报到季防骗清单：十条建议请收藏',
  '<p>① 核实录取通知书与缴费渠道；② 警惕「代办入学」「内部指标」；③ 不轻信陌生「学长」推销；④ 保护个人信息与证件照片；⑤ 兼职刷单、打字员高薪多为骗局；⑥ 涉钱电话先挂断再回拨官方号码；⑦ 警惕「助学金」要先交手续费；⑧ 宿舍推销「统一订购」需向宿管确认；⑨ 网络交友涉及投资一律拒绝；⑩ 遇诈及时拨打 96110 或 110。</p>',
  '汇总报到、缴费、兼职、社交等场景下的十条防骗要点。',
  NULL, 4, 1, 'news', 1, 1, 2340, 1, NOW() - INTERVAL 12 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '新生报到季防骗清单：十条建议请收藏');
