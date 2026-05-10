-- 闯关关卡种子数据脚本
-- 用途：为前端 `知识闯关` 页面准备可用关卡数据（quiz + scenario）
-- 注意：
-- 1) 建议在空库或已知不会重复插入的情况下执行；本脚本对每条记录使用 title 去重。
-- 2) MySQL JSON 字段使用 CAST(... AS JSON)

USE anti_fraud_platform;

-- =========================
-- Quiz 关卡：第 1 关
-- =========================
UPDATE challenge
SET passing_score = 40,
    status = 1
WHERE title = '初识反诈：钓鱼短信';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '初识反诈：钓鱼短信',
  '通过题目理解钓鱼短信特征与正确应对方式。',
  1, 1, 'quiz', 40, 10,
  CAST('{
    "questions": [
      {
        "id": "q1_1",
        "questionType": "single",
        "text": "钓鱼短信最常见的诱导方式是什么？",
        "options": [
          { "label": "A", "text": "引导你点击来历不明的链接" },
          { "label": "B", "text": "引导你点击银行/官方App的安全链接" },
          { "label": "C", "text": "引导你只看公告不操作" },
          { "label": "D", "text": "引导你在官方App内完成查询" }
        ],
        "correctIndexes": [0],
        "score": 10
      },
      {
        "id": "q1_2",
        "questionType": "single",
        "text": "收到可疑短信后，最安全的做法是？",
        "options": [
          { "label": "A", "text": "直接回复对方询问" },
          { "label": "B", "text": "立即转发给同学求证" },
          { "label": "C", "text": "不要点击链接，拨打官方客服电话核实" },
          { "label": "D", "text": "先截图再按短信提示操作" }
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "q1_3",
        "questionType": "multiple",
        "text": "哪些行为可能增加被骗风险？",
        "options": [
          { "label": "A", "text": "轻信陌生人要求你立即转账/操作" },
          { "label": "B", "text": "先核实短信真伪" },
          { "label": "C", "text": "按对方指引完成转账或提供验证码" },
          { "label": "D", "text": "在官方App内查看交易明细确认" }
        ],
        "correctIndexes": [0, 2],
        "score": 20
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '初识反诈：钓鱼短信');

-- =========================
-- Quiz 关卡：第 2 关
-- =========================
UPDATE challenge
SET passing_score = 40,
    status = 1
WHERE title = '识别冒充客服';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '识别冒充客服',
  '识别“客服”冒充诈骗套路，避免上当。',
  2, 2, 'quiz', 40, 15,
  CAST('{
    "questions": [
      {
        "id": "q2_1",
        "questionType": "single",
        "text": "冒充客服诈骗通常会让你做什么？",
        "options": [
          { "label": "A", "text": "提供验证码或引导你在站外转账" },
          { "label": "B", "text": "在官方App内查看订单并联系客服核实" },
          { "label": "C", "text": "拒绝提供任何个人敏感信息" },
          { "label": "D", "text": "通过平台内工单处理问题" }
        ],
        "correctIndexes": [0],
        "score": 15
      },
      {
        "id": "q2_2",
        "questionType": "multiple",
        "text": "下列哪些线索更符合“冒充客服”的风险特征？",
        "options": [
          { "label": "A", "text": "要求你立即操作并声称“限时处理”" },
          { "label": "B", "text": "提供正规工单入口并允许你自行查看" },
          { "label": "C", "text": "要求你在对方引导下转账/付款" },
          { "label": "D", "text": "鼓励你先核实再做决定" }
        ],
        "correctIndexes": [0, 2],
        "score": 25
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '识别冒充客服');

-- =========================
-- Quiz 关卡：第 3 关
-- =========================
UPDATE challenge
SET passing_score = 40,
    status = 1
WHERE title = '识别冒充公检法';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '识别冒充公检法',
  '学习“冒充公检法”常见话术与自救方式。',
  3, 3, 'quiz', 40, 20,
  CAST('{
    "questions": [
      {
        "id": "q3_1",
        "questionType": "single",
        "text": "“公检法”冒充诈骗中，常见的诱导行为是？",
        "options": [
          { "label": "A", "text": "让你立即交“罚款/保证金”并进行转账" },
          { "label": "B", "text": "要求你通过官方渠道自行核实案件信息" },
          { "label": "C", "text": "仅提供线索不要求转账" },
          { "label": "D", "text": "建议你挂断后联系当地正规机构" }
        ],
        "correctIndexes": [0],
        "score": 20
      },
      {
        "id": "q3_2",
        "questionType": "single",
        "text": "当你怀疑自己遇到“冒充公检法”诈骗时，最正确的做法是？",
        "options": [
          { "label": "A", "text": "立刻按对方要求转账以免“追究”" },
          { "label": "B", "text": "挂断电话，使用官方公开联系方式核实" },
          { "label": "C", "text": "与对方继续沟通获取更多指令" },
          { "label": "D", "text": "把对方提供的链接转发给朋友一起处理" }
        ],
        "correctIndexes": [1],
        "score": 20
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '识别冒充公检法');

-- =========================
-- Scenario 关卡：第 4 关
-- =========================
INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '情景模拟：冒充客服索要转账',
  '你接到“客服”诱导转账。选择更安全的处理方式。',
  4, 2, 'scenario', 60, 25,
  CAST('{"questions":[]}' AS JSON),
  CAST('{
    "name": "冒充客服索要转账",
    "description": "你正在处理订单，突然收到“客服”消息要求你转账完成验证。",
    "startNodeId": "start",
    "endNodeIds": ["end_safe", "end_risk"],
    "nodes": [
      {
        "id": "start",
        "type": "start",
        "title": "通话开始",
        "content": "你正在处理订单，突然收到“客服”消息：请您立即按提示完成验证并转账！",
        "role": "narrator",
        "riskTip": "正规客服不会要求你在站外转账或提供验证码。"
      },
      {
        "id": "dialog_safe",
        "type": "dialog",
        "title": "谨慎核实",
        "content": "你拒绝了对方的要求，改为到官方App核实订单并拨打客服电话。",
        "role": "victim"
      },
      {
        "id": "dialog_risk",
        "type": "dialog",
        "title": "被带节奏",
        "content": "你按对方指引点击链接并准备转账。",
        "role": "victim",
        "riskTip": "诈骗往往通过“紧急验证/限时处理”制造压力。"
      },
      {
        "id": "end_safe",
        "type": "end",
        "title": "成功自救",
        "content": "核实后发现是诈骗，及时报警并更换密码。",
        "role": "victim"
      },
      {
        "id": "end_risk",
        "type": "end",
        "title": "遭遇损失",
        "content": "转账后才发现已被骗，损失已发生。",
        "role": "victim"
      }
    ],
    "edges": [
      {
        "from": "start",
        "to": "dialog_safe",
        "condition": "选择核实身份",
        "label": "先去官方渠道核实再处理",
        "isSafeChoice": true
      },
      {
        "from": "start",
        "to": "dialog_risk",
        "condition": "被诱导操作",
        "label": "点击链接并按提示转账",
        "isSafeChoice": false
      },
      {
        "from": "dialog_safe",
        "to": "end_safe",
        "condition": "结束",
        "label": "完成自查并停止交互",
        "isSafeChoice": true
      },
      {
        "from": "dialog_risk",
        "to": "end_risk",
        "condition": "结束",
        "label": "继续转账并造成风险",
        "isSafeChoice": false
      }
    ]
  }' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '情景模拟：冒充客服索要转账');

-- =========================
-- Scenario 关卡：第 5 关
-- =========================
INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '情景模拟：冒充公检法催缴罚款',
  '你收到“公检法”威胁催缴消息，选择正确的核实流程。',
  5, 3, 'scenario', 60, 30,
  CAST('{"questions":[]}' AS JSON),
  CAST('{
    "name": "冒充公检法催缴罚款",
    "description": "对方声称案件需要紧急处理并要求转账。",
    "startNodeId": "s0",
    "endNodeIds": ["e_safe", "e_risk"],
    "nodes": [
      {
        "id": "s0",
        "type": "start",
        "title": "威胁催缴",
        "content": "电话里对方称：你涉及案件，需要马上缴纳罚款，否则将面临更严厉后果。",
        "role": "narrator",
        "riskTip": "冒充公检法常用“紧急/威胁/扣押”话术。"
      },
      {
        "id": "d_safe",
        "type": "dialog",
        "title": "挂断核实",
        "content": "你挂断电话，通过官方公开渠道核实信息，并联系正规机构。",
        "role": "victim"
      },
      {
        "id": "d_risk",
        "type": "dialog",
        "title": "仓促转账",
        "content": "你按对方要求把钱转到指定账户以“消除风险”。",
        "role": "victim",
        "riskTip": "诈骗会不断施压，让你在恐慌中操作。"
      },
      {
        "id": "e_safe",
        "type": "end",
        "title": "避免损失",
        "content": "核实后发现是诈骗，你及时报警并保护账户安全。",
        "role": "victim"
      },
      {
        "id": "e_risk",
        "type": "end",
        "title": "不幸中招",
        "content": "转账后发现被骗，损失无法挽回。",
        "role": "victim"
      }
    ],
    "edges": [
      {
        "from": "s0",
        "to": "d_safe",
        "condition": "选择冷静核实",
        "label": "挂断后走官方公开渠道核实",
        "isSafeChoice": true
      },
      {
        "from": "s0",
        "to": "d_risk",
        "condition": "被威胁带跑",
        "label": "按要求立即转账",
        "isSafeChoice": false
      },
      {
        "from": "d_safe",
        "to": "e_safe",
        "condition": "结束",
        "label": "完成核实并停止对话",
        "isSafeChoice": true
      },
      {
        "from": "d_risk",
        "to": "e_risk",
        "condition": "结束",
        "label": "继续转账导致损失",
        "isSafeChoice": false
      }
    ]
  }' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '情景模拟：冒充公检法催缴罚款');

