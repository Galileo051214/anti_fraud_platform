-- =====================================================
-- 诈骗案例种子数据
-- 用途：补齐 fraud_case（10条）及 case_tag_relation 关联
-- 执行方式：
-- 1) 先确保已执行 sql/init.sql
-- 2) 再执行本脚本
-- =====================================================

USE anti_fraud_platform;

-- -----------------------------------------------------
-- 案例1：入门反诈：钓鱼短信识别
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '入门反诈：钓鱼短信识别',
  '网络诈骗',
  '<p>本案例讲解钓鱼短信的常见话术与识别要点，并给出安全处置流程。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "开始学习钓鱼短信识别", "role": "narrator" },
      { "id": "end_safe", "type": "end", "content": "识别成功并及时处置", "role": "victim" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大一","大二"]' AS JSON),
  CAST('["计算机","软件工程"]' AS JSON),
  1, 1.5,
  320, 48, 0.1500, 0.0600,
  1, 1, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '入门反诈：钓鱼短信识别');

-- -----------------------------------------------------
-- 案例2：骗取验证码：防护要点
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '骗取验证码：防护要点',
  '网络诈骗',
  '<p>学习验证码诈骗如何实施，以及在不同场景下如何快速止损与验证来源。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "验证码诈骗防护学习开始", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大二","大三"]' AS JSON),
  CAST('["信息安全","计算机"]' AS JSON),
  2, 2.8,
  260, 36, 0.1385, 0.0520,
  1, 0, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '骗取验证码：防护要点');

-- -----------------------------------------------------
-- 案例3：冒充客服要转账
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '冒充客服要转账',
  '电话诈骗',
  '<p>识别“客服”话术中的关键风险点：引导站外转账、索要验证码、制造紧急压力。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入冒充客服场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大一","大二","大三"]' AS JSON),
  CAST('["法学","工商管理"]' AS JSON),
  2, 4.2,
  410, 92, 0.2244, 0.0710,
  1, 1, NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '冒充客服要转账');

-- -----------------------------------------------------
-- 案例4：冒充公检法催缴
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '冒充公检法催缴',
  '电话诈骗',
  '<p>理解“公检法”冒充的典型威胁逻辑，并掌握挂断核实、官方渠道确认的流程。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入公检法催缴场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大二","大四"]' AS JSON),
  CAST('["会计","金融学"]' AS JSON),
  3, 5.8,
  290, 55, 0.1897, 0.0595,
  1, 0, NOW() - INTERVAL 7 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '冒充公检法催缴');

-- -----------------------------------------------------
-- 案例5：网络贷款分期陷阱
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '网络贷款分期陷阱',
  '金融诈骗',
  '<p>学习“低门槛贷款”如何诱导提供个人信息与转账操作，识别信息冒用风险。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入网络贷款场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["all"]' AS JSON),
  CAST('["all"]' AS JSON),
  3, 6.6,
  530, 120, 0.2264, 0.0780,
  1, 1, NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '网络贷款分期陷阱');

-- -----------------------------------------------------
-- 案例6：杀猪盘引导投资
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '杀猪盘引导投资',
  '网络诈骗',
  '<p>分析“高回报、低风险”叙事结构与诱导步骤，学会从话术识别资金风险。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入杀猪盘场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大三","大四"]' AS JSON),
  CAST('["金融学","经济学"]' AS JSON),
  4, 7.9,
  480, 98, 0.2042, 0.0735,
  1, 0, NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '杀猪盘引导投资');

-- -----------------------------------------------------
-- 案例7：游戏交易诈骗
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '游戏交易诈骗',
  '网络诈骗',
  '<p>了解“代充/代买/担保交易”常见套路，以及如何验证对方身份与支付链路。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入游戏交易场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大一","大二","大三"]' AS JSON),
  CAST('["计算机","软件工程"]' AS JSON),
  2, 3.9,
  240, 34, 0.1417, 0.0505,
  1, 0, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '游戏交易诈骗');

-- -----------------------------------------------------
-- 案例8：求职招聘押金诈骗
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '求职招聘押金诈骗',
  '线下诈骗',
  '<p>识别“签约先付押金”“保证名额”等话术，掌握正规流程核验要点。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入求职招聘场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大三","大四"]' AS JSON),
  CAST('["all"]' AS JSON),
  3, 4.6,
  360, 60, 0.1667, 0.0578,
  1, 0, NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '求职招聘押金诈骗');

-- -----------------------------------------------------
-- 案例9：刷单返利与诱导下单
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '刷单返利与诱导下单',
  '网络诈骗',
  '<p>学习刷单返利类诈骗如何制造“收益演示”并诱导资金投入，学会拒绝与上报。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入刷单场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大一","大二","大三"]' AS JSON),
  CAST('["all"]' AS JSON),
  1, 2.2,
  610, 140, 0.2295, 0.0810,
  1, 0, NOW() - INTERVAL 9 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '刷单返利与诱导下单');

-- -----------------------------------------------------
-- 案例10：注销账户/跑分引流
-- -----------------------------------------------------
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '注销账户/跑分引流',
  '网络诈骗',
  '<p>分析“注销账户”“刷流水消除风险”等说法背后的引流逻辑，明确安全边界与核验路径。</p>',
  CAST('{
    "nodes": [
      { "id": "start", "type": "start", "content": "进入注销账户引流场景", "role": "narrator" }
    ],
    "edges": []
  }' AS JSON),
  CAST('["大二","大三","大四"]' AS JSON),
  CAST('["信息安全","金融学"]' AS JSON),
  4, 8.3,
  520, 110, 0.2115, 0.0825,
  1, 1, NOW() - INTERVAL 10 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '注销账户/跑分引流');

-- -----------------------------------------------------
-- 标签关联（每条2个标签）
-- 通过 title / tag.name 映射，避免依赖自增 id。
-- -----------------------------------------------------

-- 案例1
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '入门反诈：钓鱼短信识别'
  AND t.name IN ('刷单诈骗', '红包返利')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例2
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '骗取验证码：防护要点'
  AND t.name IN ('冒充客服', '网络贷款')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例3
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '冒充客服要转账'
  AND t.name IN ('冒充客服', '红包返利')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例4
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '冒充公检法催缴'
  AND t.name IN ('冒充公检法', '杀猪盘')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例5
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '网络贷款分期陷阱'
  AND t.name IN ('网络贷款', '刷单诈骗')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例6
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '杀猪盘引导投资'
  AND t.name IN ('杀猪盘', '游戏交易')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例7
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '游戏交易诈骗'
  AND t.name IN ('游戏交易', '网络贷款')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例8
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '求职招聘押金诈骗'
  AND t.name IN ('求职招聘', '红包返利')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例9
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '刷单返利与诱导下单'
  AND t.name IN ('刷单诈骗', '红包返利')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例10
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '注销账户/跑分引流'
  AND t.name IN ('杀猪盘', '网络贷款')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

