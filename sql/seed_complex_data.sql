-- =====================================================
-- 复杂诈骗套路综合种子数据
-- 用途：基于 10 种新型复杂诈骗套路，填充：
--   - fraud_case（10条案例，含完整剧情和FSM剧本）
--   - case_tag_relation（案例-标签关联）
--   - news（10篇资讯文章）
--   - challenge（10组答题关卡 + 5个情景模拟FSM关）
-- 来源：复杂诈骗套路解析.md（10种套路）
-- 执行方式：
--   mysql -u root -p anti_fraud_platform < sql/seed_complex_data.sql
-- 注意：全部使用 title 去重，可幂等执行
-- =====================================================

USE anti_fraud_platform;

-- =====================================================
-- 第一部分：扩展案例标签
-- =====================================================
INSERT INTO `case_tag` (`name`, `category`, `description`, `color`)
SELECT * FROM (SELECT 'AI深度伪造', '网络诈骗', 'AI换脸/语音克隆/身份确认类诈骗', '#00BCD4') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `case_tag` WHERE `name` = 'AI深度伪造');

INSERT INTO `case_tag` (`name`, `category`, `description`, `color`)
SELECT * FROM (SELECT '注销校园贷', '金融诈骗', '注销校园贷/调整征信类诈骗', '#FF5722') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `case_tag` WHERE `name` = '注销校园贷');

INSERT INTO `case_tag` (`name`, `category`, `description`, `color`)
SELECT * FROM (SELECT '裸聊敲诈', '网络诈骗', '裸聊敲诈勒索类诈骗', '#E91E63') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `case_tag` WHERE `name` = '裸聊敲诈');

INSERT INTO `case_tag` (`name`, `category`, `description`, `color`)
SELECT * FROM (SELECT '虚拟货币洗钱', '金融诈骗', '虚拟货币交易/洗钱类诈骗', '#673AB7') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `case_tag` WHERE `name` = '虚拟货币洗钱');

-- =====================================================
-- 第二部分：诈骗案例（10条，含完整FMS剧本）
-- =====================================================

-- 案例1：杀猪盘·复合式情感投资诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '杀猪盘·复合式情感投资诈骗',
  '网络诈骗',
  '<h3>案例背景</h3><p>杀猪盘是当前单案损失金额最高的诈骗类型，诈骗分子通过婚恋平台（世纪佳缘、探探、Soul）或社交APP筛选目标，打造「完美人设」建立情感依赖后，诱导虚假投资或博彩。</p><h3>诈骗剧本</h3><p><strong>第一步（选猪）：</strong>诈骗团伙在婚恋平台筛选25~50岁单身/离异、有稳定收入、情感需求强烈的人群作为目标。</p><p><strong>第二步（养猪）：</strong>打造「投行精英/跨境电商老板/部队军官」人设，每日嘘寒问暖、分享「日常生活」，持续1~4个月建立情感依赖，期间进行服从性测试（发送小额红包、赠送虚拟礼物）。</p><p><strong>第三步（杀猪）：</strong>无意中透露「内部投资渠道」「数字货币套利」「博彩平台漏洞」，先让受害者小额试水并成功提现（返利500~2000元），待完全信任后诱导大额充值。</p><p><strong>第四步（毁尸）：</strong>受害者要求提现时，以「账户冻结需解冻费」「流水不足需刷流水」「税金」「保证金」等连环收费，榨干最后一分钱后平台关闭、拉黑失联。</p><h3>真实案例</h3><p>澳门2025年7月以来至少18名居民被骗，总损失超2789万澳门元。其中一名受害者被虚假投资平台「INST-SI」「TOKENPOCKET」吞噬数百万元。</p>',
  CAST('{
    "name": "杀猪盘情感投资",
    "description": "诈骗分子通过婚恋平台建立情感关系后诱导虚假投资",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_risk", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"社交平台初识","content":"你在交友软件上匹配到一位条件优越的异性，对方每日主动问候、分享生活点滴。","role":"narrator","riskTip":"警惕过于完美的陌生人，照片可全网反搜验证"},
      {"id":"d1","type":"dialog","title":"建立情感联系","content":"对方自称投行精英，每日嘘寒问暖，偶尔发来小额红包，与你讨论未来规划。","role":"victim","riskTip":"诈骗分子会进行1-4个月的情感投资才收网"},
      {"id":"d2","type":"dialog","title":"透露投资渠道","content":"对方无意中透露自己正在用「内部渠道」做数字货币套利，截图展示高额收益，邀请你小额试水。","role":"narrator","riskTip":"任何声称稳赚不赔的投资都是诈骗"},
      {"id":"d_trial","type":"dialog","title":"小额试水成功","content":"你投入500元试水，很快收到100元返利并成功提现，开始相信这个渠道。","role":"victim","riskTip":"先给甜头是杀猪盘的经典手法"},
      {"id":"d_big","type":"dialog","title":"大额投入","content":"对方怂恿你加大投入，称现在行情好、机会难得。你将积蓄8万元全部投入。","role":"victim"},
      {"id":"d_charge","type":"dialog","title":"连环收费","content":"你尝试提现时平台显示「账户冻结」，客服要求缴纳解冻费2万元、保证金3万元。","role":"victim","riskTip":"提现受阻+连环收费=100%诈骗"},
      {"id":"e_safe","type":"end","title":"及时脱身","content":"你产生怀疑，拒绝继续缴费，向警方报案并保留所有聊天和转账记录。","role":"victim"},
      {"id":"e_risk","type":"end","title":"继续缴费","content":"你相信了「解冻」说辞继续缴费，又被要求刷流水，最终损失15万元。","role":"victim"},
      {"id":"e_loss","type":"end","title":"平台关闭","content":"你多次缴费后平台突然无法访问，对方失联，你意识到被骗，损失惨重。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d1","condition":"继续聊天","label":"与对方保持联系","isSafeChoice":true},
      {"from":"d1","to":"d2","condition":"关系升温","label":"信任对方并接受红包","isSafeChoice":true},
      {"from":"d2","to":"d_trial","condition":"产生兴趣","label":"小额试水投资","isSafeChoice":false},
      {"from":"d2","to":"e_safe","condition":"拒绝投资","label":"警惕并拒绝投资","isSafeChoice":true},
      {"from":"d_trial","to":"d_big","condition":"尝到甜头","label":"追加投入","isSafeChoice":false},
      {"from":"d_trial","to":"e_safe","condition":"见好就收","label":"提现后不再投入","isSafeChoice":true},
      {"from":"d_big","to":"d_charge","condition":"提现受阻","label":"联系客服处理","isSafeChoice":false},
      {"from":"d_big","to":"e_safe","condition":"产生怀疑","label":"立刻停止并向警方求助","isSafeChoice":true},
      {"from":"d_charge","to":"e_risk","condition":"继续缴费","label":"缴纳解冻费和保证金","isSafeChoice":false},
      {"from":"d_charge","to":"e_safe","condition":"及时止损","label":"拒绝缴费并报警","isSafeChoice":true},
      {"from":"e_risk","to":"e_loss","condition":"平台关闭","label":"平台关闭且无法联系","isSafeChoice":false}
    ]
  }' AS JSON),
  CAST('["大三","大四"]' AS JSON),
  CAST('["all"]' AS JSON),
  5, 9.2,
  580, 125, 0.2155, 0.0850,
  1, 1, NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '杀猪盘·复合式情感投资诈骗');

-- 案例2：AI深度伪造诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  'AI深度伪造诈骗（换脸/语音克隆/身份确认）',
  '网络诈骗',
  '<h3>案例背景</h3><p>AI深度伪造诈骗是技术门槛最高、迷惑性最强的新型骗局，涵盖AI换脸视频、AI语音克隆、以及2026年新型「身份确认」语音取证诈骗三个主要方向。</p><h3>一、AI换脸视频诈骗</h3><p>诈骗分子通过社交平台获取受害者亲友的照片或视频，利用Deepfake技术（DeepFaceLab/DeepLiveCam等开源工具）生成实时换脸视频，伪装成亲友进行视频通话。只需10~30张目标人物照片即可生成可实时驱动的换脸模型。</p><p><strong>真实案例：</strong>浙江王女士2025年收到「女儿」视频电话，画面中女儿满脸伤痕哭诉车祸需15万手术费，转账后核实发现女儿在校安然无恙。</p><h3>二、AI语音克隆诈骗</h3><p>通过骚扰电话或社交平台语音消息采集目标声音样本（30秒即可），使用ElevenLabs或讯飞开放平台克隆声音，模仿领导声音要求财务转账、模仿家人声音求救。</p><p><strong>真实案例：</strong>某公司财务收到「CEO」语音消息要求加急转账85万「合同保证金」，声音语气完全一致，未经电话核实直接转账。</p><h3>三、2026新型「身份确认」语音取证诈骗</h3><p>不再索要密码/验证码，只问「请问是XX本人吗」全程录音。获取录音后利用银行/运营商线上业务的语音核验漏洞，开通代扣业务、申请小额贷款、开通免密支付。</p><p><strong>防范要点：</strong>陌生来电绝不随意回答「是」，挂断后通过官方客服回拨核实。</p>',
  CAST('{
    "name": "AI深度伪造识别",
    "description": "诈骗分子利用AI换脸/语音克隆技术冒充亲友或领导",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_risk", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"收到AI视频来电","content":"你突然收到「亲友」的视频电话，画面中对方满脸焦急，称遭遇急事急需用钱。","role":"narrator","riskTip":"AI换脸可通过让对方做特定动作（挥手/转头）来识别"},
      {"id":"d_verify","type":"dialog","title":"视频画面可疑","content":"你注意到对方虽然脸和声音很熟悉，但表情有些不自然，眨眼频率异常。","role":"victim","riskTip":"AI换脸在快速转头、挥手时会出现画面扭曲或延迟"},
      {"id":"d_trust","type":"dialog","title":"完全信任画面","content":"画面和声音都跟亲友一模一样，你完全相信了，准备立即转账。","role":"victim"},
      {"id":"d_call","type":"dialog","title":"电话核实","content":"你挂断视频后拨打亲友的常用号码进行核实。","role":"victim"},
      {"id":"d_voice","type":"dialog","title":"收到语音消息","content":"你收到「领导」发来的语音消息，语气焦急要求加急转账，声音完全一致。","role":"narrator","riskTip":"语音克隆仅需30秒样本，涉及转账必须多渠道确认"},
      {"id":"e_safe","type":"end","title":"识破骗局","content":"经电话核实，亲友安然无恙，你确认是AI诈骗并报警。","role":"victim"},
      {"id":"e_risk","type":"end","title":"语音诈骗上当","content":"你相信了语音消息直接转账，后与领导当面核实才发现被骗。","role":"victim"},
      {"id":"e_loss","type":"end","title":"视频诈骗上当","content":"你向视频中的「亲友」转账后，联系对方才发现被骗，损失已无法挽回。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_verify","condition":"产生怀疑","label":"留意到画面异常","isSafeChoice":true},
      {"from":"s1","to":"d_trust","condition":"完全信任","label":"准备立即转账","isSafeChoice":false},
      {"from":"d_verify","to":"d_call","condition":"决定核实","label":"挂断后电话确认","isSafeChoice":true},
      {"from":"d_verify","to":"d_trust","condition":"打消疑虑","label":"对方解释后相信了","isSafeChoice":false},
      {"from":"d_trust","to":"e_loss","condition":"直接转账","label":"按照要求转账","isSafeChoice":false},
      {"from":"d_trust","to":"d_call","condition":"临时起意核实","label":"转账前先核实","isSafeChoice":true},
      {"from":"d_call","to":"e_safe","condition":"确认安全","label":"亲友未联系过你","isSafeChoice":true},
      {"from":"d_call","to":"d_voice","condition":"语音消息","label":"刚挂断又收到语音","isSafeChoice":false},
      {"from":"d_voice","to":"e_risk","condition":"相信语音","label":"按领导要求转账","isSafeChoice":false},
      {"from":"d_voice","to":"d_call","condition":"再次核实","label":"打领导办公电话核实","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大一","大二","大三","大四"]' AS JSON),
  CAST('["计算机","软件工程","信息安全"]' AS JSON),
  4, 8.5,
  340, 78, 0.2294, 0.0720,
  1, 1, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = 'AI深度伪造诈骗（换脸/语音克隆/身份确认）');

-- 案例3：刷单返利诈骗（含新型变种）
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '刷单返利诈骗（含线下现金/数字人民币变种）',
  '网络诈骗',
  '<h3>案例背景</h3><p>刷单返利类诈骗是发案量最高的诈骗类型。诈骗分子以「点赞赚钱」「日结高薪」为诱饵，前期小额返利骗取信任，后期要求大额垫资后失联。</p><h3>传统刷单流程</h3><p><strong>第一步：</strong>在短视频/社交平台发布「点赞赚钱」「日结高薪」广告。</p><p><strong>第二步：</strong>引导关注抖音/快手账号，每单返2~5元，前10单正常返现建立信任。</p><p><strong>第三步：</strong>诱导下载专属APP做「联单任务」，需连续完成3~5单才能提现。</p><p><strong>第四步：</strong>第2单起金额暴涨（100→500→3000→10000），完成后以「操作失误需补单」「账户冻结需解冻」为由要求继续转账。</p><h3>2025新变种：线下现金交付</h3><p>不再线上转账，而是要求受害人提取现金放置于指定地点（如小区垃圾箱、公园长椅下），诈骗团伙用遥控车转移赃款，目的是规避银行风控系统的大额转账监测。</p><p><strong>真实案例：</strong>北京李先生被刷单诈骗13万元，现金被遥控车取走。</p><h3>2025新变种：数字人民币钱包</h3><p>诱导用户开通数字人民币钱包并绑定银行卡，以「充值返利」名义要求将资金充值到虚假平台，利用数字人民币的匿名性和便捷性实施诈骗。</p>',
  CAST('{
    "name": "刷单返利诈骗",
    "description": "以点赞返利为诱饵，诱导垫资刷单实施诈骗",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"看到兼职广告","content":"你在短视频平台看到「点赞赚钱、日结高薪、月入过万」的兼职广告。","role":"narrator","riskTip":"凡是以点赞/刷单为名的兼职都是违法行为"},
      {"id":"d_contact","type":"dialog","title":"联系派单员","content":"你添加了对方微信，对方发来工作流程：关注指定账号，每单返5元。","role":"victim"},
      {"id":"d_small","type":"dialog","title":"小额返利","content":"你完成10单任务，每单都收到了返利，共赚了50元。对方推荐下载专属APP做「高级任务」。","role":"narrator","riskTip":"先给甜头是刷单诈骗的标准手法"},
      {"id":"d_advanced","type":"dialog","title":"下载专属APP","content":"你下载了「任务助手」APP，开始做联单任务，每单金额逐步加大：100元→500元→3000元。","role":"victim","riskTip":"非应用商店下载的APP极有可能是诈骗工具"},
      {"id":"d_big","type":"dialog","title":"大额联单","content":"系统提示你接到一个「三连单」，需连续完成3000+10000+30000元的任务才能提现。","role":"narrator"},
      {"id":"d_freeze","type":"dialog","title":"账户冻结","content":"你完成三连单后系统显示「操作失误，账户冻结」，客服要求缴纳解冻费2万元。","role":"victim","riskTip":"操作失误、账户冻结、补单都是诈骗的连环套"},
      {"id":"e_safe","type":"end","title":"及时退出","content":"你意识到这是诈骗，停止所有操作，向公安机关报案。","role":"victim"},
      {"id":"e_warn","type":"end","title":"拒绝垫资","content":"你只做了免费任务赚了小额返利，拒绝垫资刷单。","role":"victim"},
      {"id":"e_loss","type":"end","title":"上当受骗","content":"你缴纳了解冻费后又被告知需刷流水，最终损失5.5万元。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_contact","condition":"感兴趣","label":"添加对方微信了解","isSafeChoice":false},
      {"from":"s1","to":"e_warn","condition":"忽略","label":"直接划走不看","isSafeChoice":true},
      {"from":"d_contact","to":"d_small","condition":"照做","label":"完成小额任务赚返利","isSafeChoice":false},
      {"from":"d_contact","to":"e_warn","condition":"拒绝","label":"怀疑是诈骗不参与","isSafeChoice":true},
      {"from":"d_small","to":"d_advanced","condition":"信任","label":"下载APP做高级任务","isSafeChoice":false},
      {"from":"d_small","to":"e_warn","condition":"见好就收","label":"赚到小额返利后退出","isSafeChoice":true},
      {"from":"d_advanced","to":"d_big","condition":"继续","label":"做联单任务","isSafeChoice":false},
      {"from":"d_advanced","to":"e_safe","condition":"警惕","label":"发现是陷阱立即退出","isSafeChoice":true},
      {"from":"d_big","to":"d_freeze","condition":"照做","label":"完成大额联单","isSafeChoice":false},
      {"from":"d_big","to":"e_safe","condition":"犹豫","label":"拒绝继续投入","isSafeChoice":true},
      {"from":"d_freeze","to":"e_loss","condition":"缴费","label":"缴纳解冻费","isSafeChoice":false},
      {"from":"d_freeze","to":"e_safe","condition":"报警","label":"意识到诈骗立刻报警","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大一","大二","大三"]' AS JSON),
  CAST('["all"]' AS JSON),
  2, 3.5,
  720, 168, 0.2333, 0.0880,
  1, 0, NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '刷单返利诈骗（含线下现金/数字人民币变种）');

-- 案例4：冒充客服·屏幕共享诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '冒充客服·屏幕共享诈骗',
  '电话诈骗',
  '<h3>案例背景</h3><p>冒充客服系列是发案增速最快的诈骗类型。诈骗分子非法获取用户网购订单信息后，准确说出订单细节建立信任，随后诱导下载会议APP开启屏幕共享窃取资金。</p><h3>诈骗流程</h3><p><strong>第一步：</strong>非法获取用户网购订单信息（商品名称、价格、收货地址、订单号）。</p><p><strong>第二步：</strong>冒充平台/快递客服来电，准确说出订单信息建立信任。常用话术：①商品质量问题退款X倍赔偿；②快递丢失双倍赔付；③误开通会员/代理商需取消否则每月扣费。</p><p><strong>第三步：</strong>诱导下载「会议APP」（腾讯会议/Zoom/云会议等），开启屏幕共享功能。</p><p><strong>第四步：</strong>通过屏幕共享窃取银行卡号、密码、短信验证码，实时转走资金。</p><h3>技术细节</h3><p>安卓/iOS系统自带的屏幕录制+远程协助权限被利用。骗子通过会议软件的「共享屏幕」「远程控制」功能实时查看受害者手机上的短信验证码。受害者往往以为只是在配合「客服指导操作」，实则全程被监控。</p><p><strong>真实案例：</strong>重庆王某接到「电商客服」电话，被诱导下载「云会议」APP开启屏幕共享，银行卡内17.6万元被分批转走。</p>',
  CAST('{
    "name": "冒充客服屏幕共享",
    "description": "诈骗分子冒充客服诱导开启屏幕共享窃取资金",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_risk", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"接到客服电话","content":"你接到自称电商客服的电话，对方准确说出你最近购买的订单信息（商品名称、价格、地址）。","role":"narrator","riskTip":"个人信息已大量泄露，能说出订单信息不等于真客服"},
      {"id":"d_story","type":"dialog","title":"退款话术","content":"客服称你购买的商品有质量问题，可三倍退款，需配合操作。","role":"victim"},
      {"id":"d_share","type":"dialog","title":"诱导共享屏幕","content":"客服引导你下载「云会议」APP，并输入会议号加入会议，要求开启屏幕共享以便「指导操作」。","role":"narrator","riskTip":"任何要求共享屏幕的客服都是诈骗！"},
      {"id":"d_bank","type":"dialog","title":"操作银行卡","content":"在共享屏幕状态下，你按客服指引打开手机银行，输入卡号、密码、验证码。","role":"victim","riskTip":"共享屏幕时输入的密码和验证码会被实时窃取"},
      {"id":"d_sms","type":"dialog","title":"验证码被窃","content":"银行发来短信验证码，你正在念给客服听，手机收到了扣款短信。","role":"victim"},
      {"id":"e_safe","type":"end","title":"识破挂断","content":"你听到共享屏幕要求时产生警惕，挂断电话后通过官方APP联系客服核实。","role":"victim"},
      {"id":"e_risk","type":"end","title":"部分损失","content":"你只共享了屏幕但银行卡余额较少，损失了余额中的1.2万元。","role":"victim"},
      {"id":"e_loss","type":"end","title":"资金被洗劫","content":"骗子通过屏幕共享看到你的卡号和验证码，将卡内17.6万元分批转走。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_story","condition":"接听","label":"继续听对方说","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"挂断","label":"直接挂断并官方核实","isSafeChoice":true},
      {"from":"d_story","to":"d_share","condition":"相信","label":"相信退款说辞","isSafeChoice":false},
      {"from":"d_story","to":"e_safe","condition":"怀疑","label":"表示自己会联系官方处理","isSafeChoice":true},
      {"from":"d_share","to":"d_bank","condition":"共享","label":"开启屏幕共享","isSafeChoice":false},
      {"from":"d_share","to":"e_safe","condition":"拒绝共享","label":"听到共享屏幕要求后挂断","isSafeChoice":true},
      {"from":"d_bank","to":"d_sms","condition":"输入信息","label":"在手机银行输入密码","isSafeChoice":false},
      {"from":"d_bank","to":"e_risk","condition":"中途警觉","label":"输入部分信息后察觉异常","isSafeChoice":true},
      {"from":"d_sms","to":"e_loss","condition":"验证码泄露","label":"告知对方验证码","isSafeChoice":false},
      {"from":"d_sms","to":"e_risk","condition":"及时止损","label":"看到扣款短信立即挂断","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大一","大二","大三","大四"]' AS JSON),
  CAST('["all"]' AS JSON),
  3, 6.8,
  490, 105, 0.2143, 0.0810,
  1, 1, NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '冒充客服·屏幕共享诈骗');

-- 案例5：冒充公检法·FaceTime诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '冒充公检法·FaceTime诈骗',
  '电话诈骗',
  '<h3>案例背景</h3><p>冒充公检法诈骗是威慑力最强的诈骗类型。诈骗分子利用人们对执法机关的敬畏心理，通过权威身份+恐慌话术+技术控制三重手段实施诈骗。</p><h3>完整话术流程</h3><p><strong>第一步：</strong>「你好，我是XX市公安局民警，你名下的一张银行卡涉嫌洗钱案」——权威身份+突然袭击制造恐慌。</p><p><strong>第二步：</strong>准确说出受害者的姓名、身份证号、家庭住址（从黑产购买）——信息准确增加可信度。</p><p><strong>第三步：</strong>发送伪造的「逮捕令」「通缉令」（带红章、编号、受害者照片）——视觉冲击力强化恐惧。</p><p><strong>第四步：</strong>「案件涉密，不准告诉任何人，否则会连累家人」——隔离受害者，阻止向亲友求助。</p><p><strong>第五步：</strong>诱导下载「安全防护」APP（实为远程控制/木马软件）——技术控制。</p><p><strong>第六步：</strong>要求将资金转入「安全账户」进行「资金清查」——最终目的。</p><h3>2025新变种：FaceTime+伪造通缉令</h3><p>通过苹果FaceTime视频通话冒充公检法，视频画面中骗子穿着假警服、背景是伪造的「公安局」办公室，使用改号软件将来电显示修改为当地公安局的真实号码。</p><p><strong>真实案例：</strong>北京张女士接到FaceTime来电，对方显示为「北京市公安局」，视频中看到穿警服的「民警」，最终被骗3万元。</p>',
  CAST('{
    "name": "冒充公检法FaceTime",
    "description": "诈骗分子冒充公安机关通过FaceTime实施诈骗",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"接到FaceTime来电","content":"你的iPhone收到FaceTime视频来电，显示为「北京市公安局」的号码。","role":"narrator","riskTip":"公检法机关不会通过FaceTime办案，这是诈骗"},
      {"id":"d_video","type":"dialog","title":"视频中的民警","content":"你接通后看到对方穿着警服，背景是公安标志，对方自称民警并报出你的姓名、身份证号、家庭住址。","role":"victim","riskTip":"个人信息可从黑产购买，说出这些信息不代表真警察"},
      {"id":"d_warrant","type":"dialog","title":"收到逮捕令","content":"对方称你名下银行卡涉嫌洗钱案，通过微信发来一张带红章的「逮捕令」，上面有你的照片和身份证号。","role":"narrator","riskTip":"真警察不会在网上发送逮捕令，更不会要求转账"},
      {"id":"d_secret","type":"dialog","title":"要求保密","content":"对方强调案件涉密，不准告诉任何人包括家人，否则会连累家人并立即逮捕。","role":"victim","riskTip":"隔离你与外界联系是诈骗的关键手段"},
      {"id":"d_app","type":"dialog","title":"下载安全APP","content":"对方引导你下载「国家安全防护」APP（实为远程控制软件），要求你开启权限配合资金清查。","role":"narrator","riskTip":"任何所谓的「安全防护」APP要求远程控制都是木马"},
      {"id":"e_safe","type":"end","title":"挂断核实","content":"你挂断后拨打110核实，警方确认是诈骗。","role":"victim"},
      {"id":"e_warn","type":"end","title":"半信半疑","content":"你感到恐惧但保持了一定理智，先联系了学校保卫处咨询后被劝阻。","role":"victim"},
      {"id":"e_loss","type":"end","title":"上当受骗","content":"你被恐慌支配，将存款转入对方提供的「安全账户」，损失8.5万元。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_video","condition":"接通","label":"接通FaceTime视频","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"挂断","label":"直接挂断不接","isSafeChoice":true},
      {"from":"d_video","to":"d_warrant","condition":"恐慌","label":"相信对方民警身份","isSafeChoice":false},
      {"from":"d_video","to":"e_safe","condition":"质疑","label":"质疑对方并要求去派出所当面核实","isSafeChoice":true},
      {"from":"d_warrant","to":"d_secret","condition":"恐惧","label":"看到逮捕令后感到恐惧","isSafeChoice":false},
      {"from":"d_warrant","to":"e_warn","condition":"半信半疑","label":"先联系学校保卫处咨询","isSafeChoice":true},
      {"from":"d_secret","to":"d_app","condition":"完全相信","label":"按要求绝对保密并配合","isSafeChoice":false},
      {"from":"d_secret","to":"e_warn","condition":"犹豫","label":"偷偷告诉老师或家人","isSafeChoice":true},
      {"from":"d_app","to":"e_loss","condition":"转账","label":"将资金转入安全账户","isSafeChoice":false},
      {"from":"d_app","to":"e_warn","condition":"最后醒悟","label":"在输入密码前醒悟并报警","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大二","大三","大四"]' AS JSON),
  CAST('["all"]' AS JSON),
  4, 7.5,
  380, 82, 0.2158, 0.0740,
  1, 1, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '冒充公检法·FaceTime诈骗');

-- 案例6：虚假网络贷款诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '虚假网络贷款诈骗',
  '金融诈骗',
  '<h3>案例背景</h3><p>虚假网络贷款诈骗利用人们急需资金周转的心理，以「无抵押、低利息、秒放款」为诱饵，通过仿冒APP和连环收费实施诈骗。</p><h3>诈骗流程</h3><p><strong>第一步：</strong>在搜索引擎/短视频投放「无抵押、低利息、秒放款」广告。</p><p><strong>第二步：</strong>诱导下载仿冒银行/网贷APP，界面与正规平台几乎一致。</p><p><strong>第三步：</strong>填写资料提交申请后，系统显示「审核通过，额度XX万」。</p><p><strong>第四步：</strong>点击提现→显示「银行卡号错误，资金被冻结」。</p><p><strong>第五步：</strong>客服要求缴纳「解冻费」（通常为贷款金额的20%~30%）。</p><p><strong>第六步：</strong>缴纳后→「还需刷流水验证还款能力」「保险费」「保证金」等连环收费。</p><p><strong>关键点：</strong>诈骗APP的后台可以人工控制「错误提示」的内容和时机。所谓「银行卡号错误」实际上是骗子在后台手动修改受害者填写的卡号。</p><p><strong>真实案例：</strong>个体商户张先生因急需资金周转在网上申请贷款，对方以「账户异常需解冻」为由诱使其多次转账「验证资金」共计12.3万元。</p>',
  CAST('{
    "name": "虚假网络贷款",
    "description": "以无抵押贷款为诱饵，通过仿冒APP和连环收费实施诈骗",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"看到贷款广告","content":"你在搜索引擎看到「无抵押、低利息、秒放款、最高20万」的贷款广告。","role":"narrator","riskTip":"正规贷款不会在放款前收取任何费用"},
      {"id":"d_install","type":"dialog","title":"下载贷款APP","content":"你点击广告后下载了「 XX 金融」APP，界面看起来很正规，有工商信息展示。","role":"victim","riskTip":"仿冒APP可以完全复制正规平台的界面设计"},
      {"id":"d_approve","type":"dialog","title":"审核通过","content":"你填写了姓名、身份证、银行卡信息后，系统显示「审核通过，额度10万元」。","role":"narrator"},
      {"id":"d_error","type":"dialog","title":"提现失败","content":"你点击提现时系统提示「银行卡号错误，资金已被冻结」，需联系客服处理。","role":"victim","riskTip":"所谓卡号错误是骗子在后台手动修改的"},
      {"id":"d_charge1","type":"dialog","title":"收取解冻费","content":"客服称需缴纳贷款金额30%的解冻费（3万元）才能解冻，解冻后与贷款一并返还。","role":"narrator","riskTip":"放款前收费=100%诈骗"},
      {"id":"d_charge2","type":"dialog","title":"连环收费","content":"你缴纳解冻费后，对方又以「刷流水验证还款能力」「保险费」「保证金」为由继续收费。","role":"victim"},
      {"id":"e_safe","type":"end","title":"及时识别","content":"你听到放款前需缴费时立刻识别为诈骗并举报。","role":"victim"},
      {"id":"e_warn","type":"end","title":"中途醒悟","content":"你缴纳了解冻费后对继续收费产生怀疑，咨询银行后确认被骗并报警。","role":"victim"},
      {"id":"e_loss","type":"end","title":"连环被骗","content":"你多次缴费共计12.3万元后仍无法放款，最终被拉黑。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_install","condition":"点击广告","label":"下载贷款APP","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"忽略","label":"直接关闭广告","isSafeChoice":true},
      {"from":"d_install","to":"d_approve","condition":"填写申请","label":"填写个人信息申请贷款","isSafeChoice":false},
      {"from":"d_install","to":"e_safe","condition":"谨慎","label":"先核实平台资质再操作","isSafeChoice":true},
      {"from":"d_approve","to":"d_error","condition":"提现","label":"点击提现按钮","isSafeChoice":false},
      {"from":"d_error","to":"d_charge1","condition":"联系客服","label":"联系客服解冻","isSafeChoice":false},
      {"from":"d_error","to":"e_warn","condition":"产生怀疑","label":"搜索该平台是否有诈骗举报","isSafeChoice":true},
      {"from":"d_charge1","to":"d_charge2","condition":"缴费","label":"缴纳解冻费3万元","isSafeChoice":false},
      {"from":"d_charge1","to":"e_warn","condition":"警惕","label":"拒绝缴费并咨询银行","isSafeChoice":true},
      {"from":"d_charge2","to":"e_loss","condition":"继续缴费","label":"继续缴纳各种费用","isSafeChoice":false},
      {"from":"d_charge2","to":"e_warn","condition":"醒悟","label":"意识到被骗并报警","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大二","大三","大四"]' AS JSON),
  CAST('["金融学","经济学","会计"]' AS JSON),
  3, 6.2,
  560, 118, 0.2107, 0.0790,
  1, 0, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '虚假网络贷款诈骗');

-- 案例7：注销校园贷/调整征信诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '注销校园贷/调整征信诈骗',
  '金融诈骗',
  '<h3>案例背景</h3><p>注销校园贷诈骗专门针对大学生和刚毕业群体，利用「影响征信」制造恐慌，诱导从网贷平台借款后转入骗子账户。</p><h3>诈骗流程</h3><p><strong>第一步：</strong>冒充京东金融/支付宝/银监会工作人员来电。</p><p><strong>第二步：</strong>话术：「你在学生时期注册过校园贷/京东白条，现在国家禁止校园贷，需要注销/关闭，否则影响个人征信」。</p><p><strong>第三步：</strong>诱导查询征信，受害者看到征信报告上的注册记录（实为正常的大学生信用卡/花呗记录），产生恐慌。</p><p><strong>第四步：</strong>诱导从各个网贷平台（借呗、微粒贷、美团借钱等）借款。</p><p><strong>第五步：</strong>要求将借款转入「银监会清算账户」进行「清零操作」。</p><p><strong>第六步：</strong>承诺借款还清后可撤销贷款记录，实际款项已进入骗子账户。</p><p><strong>核心逻辑：</strong>利用受害者大学期间可能开通过的小额信贷服务，制造「不注销就会影响征信」的恐慌。</p><p><strong>真实案例：</strong>刚毕业的小刘接到「银监会工作人员」电话，在恐慌之下从多个网贷平台借款18万元转入「清算账户」。</p>',
  CAST('{
    "name": "注销校园贷诈骗",
    "description": "冒充银监会以注销校园贷影响征信为由诱导借款转账",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"接到注销电话","content":"你接到自称银监会工作人员的来电，称你大学期间注册过校园贷需要注销，否则影响征信。","role":"narrator","riskTip":"个人征信由央行统一管理，任何人无权修改"},
      {"id":"d_query","type":"dialog","title":"诱导查征信","content":"对方引导你查询个人征信报告，你看到上面确实有大学期间的信贷记录（花呗/信用卡），开始紧张。","role":"victim","riskTip":"正常的大学生信用卡和花呗记录不等于校园贷"},
      {"id":"d_borrow","type":"dialog","title":"诱导网贷","content":"对方称你的借款额度需要全部清零才能注销，引导你从借呗、微粒贷等平台借款。","role":"narrator","riskTip":"任何要求你网贷后转账的都是诈骗！"},
      {"id":"d_transfer","type":"dialog","title":"转入清算账户","content":"你按指示将借来的8万元转入对方提供的「银监会清算账户」，对方承诺核实后会自动归还。","role":"victim","riskTip":"银监会没有所谓的清算账户，公家转账不会用个人账户"},
      {"id":"d_more","type":"dialog","title":"继续借款","content":"对方称还有关联平台未清零，引导你从更多平台借款继续转账。","role":"narrator"},
      {"id":"e_safe","type":"end","title":"挂断核实","content":"你产生怀疑，挂断后拨打110或银行官方电话核实，确认是诈骗。","role":"victim"},
      {"id":"e_warn","type":"end","title":"中途醒悟","content":"你在转账第一笔后感到不对劲，咨询身边同学/老师后报警。","role":"victim"},
      {"id":"e_loss","type":"end","title":"惨重损失","content":"你从多个平台借款累计18万元全部转入骗子账户，损失惨重。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_query","condition":"相信","label":"相信对方身份并配合","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"质疑","label":"直接挂断并拨打官方电话核实","isSafeChoice":true},
      {"from":"d_query","to":"d_borrow","condition":"紧张","label":"看到征信记录后信以为真","isSafeChoice":false},
      {"from":"d_query","to":"e_warn","condition":"理性","label":"先咨询银行客服征信事宜","isSafeChoice":true},
      {"from":"d_borrow","to":"d_transfer","condition":"借款","label":"从网贷平台借款","isSafeChoice":false},
      {"from":"d_borrow","to":"e_warn","condition":"犹豫","label":"对借款产生怀疑","isSafeChoice":true},
      {"from":"d_transfer","to":"d_more","condition":"相信","label":"相信转账后会自动归还","isSafeChoice":false},
      {"from":"d_transfer","to":"e_warn","condition":"怀疑","label":"转账后发现不对劲","isSafeChoice":true},
      {"from":"d_more","to":"e_loss","condition":"继续","label":"从更多平台借款继续转账","isSafeChoice":false},
      {"from":"d_more","to":"e_warn","condition":"醒悟","label":"意识到被骗立即报警","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大一","大二","大三","大四"]' AS JSON),
  CAST('["all"]' AS JSON),
  3, 6.5,
  450, 96, 0.2133, 0.0760,
  1, 0, NOW() - INTERVAL 7 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '注销校园贷/调整征信诈骗');

-- 案例8：裸聊敲诈诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '裸聊敲诈诈骗',
  '网络诈骗',
  '<h3>案例背景</h3><p>裸聊敲诈诈骗利用受害者恐惧心理实施勒索。诈骗分子在社交平台以「美女」身份搭讪，诱导下载木马APP窃取通讯录，录制不雅视频后以群发为要挟敲诈钱财。</p><h3>诈骗流程</h3><p><strong>第一步：</strong>在社交软件（陌陌/探探/Soul）添加附近的人或匹配用户。</p><p><strong>第二步：</strong>诱导下载指定「聊天APP」（实为木马程序，安装后窃取通讯录）。</p><p><strong>第三步：</strong>诱导视频裸聊，期间录制不雅视频。</p><p><strong>第四步：</strong>向受害者展示通讯录截图，以「向亲友群发视频」为要挟。</p><p><strong>第五步：</strong>索要「封口费」，金额从几百元起步逐步升级到数万元。</p><h3>技术细节</h3><p>所谓「美女」多为提前录制的色情视频在摄像头前播放+实时语音互动。木马APP伪装成人脸识别/直播软件，安装时获取通讯录权限。敲诈金额采用「阶梯式」：第一次要888→第二次1888→第三次要求贷款转账。</p><p><strong>真实案例：</strong>公司职员吴先生深夜被「美女」搭讪，短暂视频裸聊后被发来不雅视频及通讯录截图，被迫转账3.5万元「封口费」。</p>',
  CAST('{
    "name": "裸聊敲诈",
    "description": "以裸聊为名录制不雅视频，窃取通讯录后进行敲诈勒索",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"社交软件被搭讪","content":"深夜你在社交软件上收到一位「美女」的好友请求，头像性感，主动发起聊天。","role":"narrator","riskTip":"深夜主动搭讪的陌生美女需要高度警惕"},
      {"id":"d_talk","type":"dialog","title":"暧昧聊天","content":"对方言语亲昵，很快提出想和你视频聊天，并发送了一个APP下载链接，称是私密聊天软件。","role":"victim","riskTip":"非应用商店的聊天APP极可能含有木马"},
      {"id":"d_install","type":"dialog","title":"安装木马APP","content":"你下载安装了对方推荐的APP，并允许了通讯录权限（APP伪装成人脸识别功能）。","role":"narrator","riskTip":"任何要求读取通讯录的陌生APP都是危险的"},
      {"id":"d_video","type":"dialog","title":"视频裸聊","content":"视频接通后对方开始脱衣，引导你也脱衣，过程中对方一直在说话但画面有些不自然。","role":"victim","riskTip":"对面可能是提前录制的视频在播放"},
      {"id":"d_extortion","type":"dialog","title":"敲诈开始","content":"视频突然挂断，对方发来你的不雅视频录像和你的通讯录截图，要求转账888元封口费。","role":"narrator","riskTip":"一旦转账后续会有连环敲诈，金额会越来越大"},
      {"id":"d_escalate","type":"dialog","title":"连环敲诈","content":"你转了888元后，对方又以「删除视频需要技术费」「全部删完要保证金」为由继续索要1888元、8888元。","role":"victim"},
      {"id":"e_safe","type":"end","title":"拒绝转账","content":"你拒绝安装APP，拉黑对方，避免了损失。","role":"victim"},
      {"id":"e_warn","type":"end","title":"报警止损","content":"你在被敲诈后选择报警，不再理会对方，避免了更多损失。","role":"victim"},
      {"id":"e_loss","type":"end","title":"被反复敲诈","content":"你因恐惧不断转账，最终被敲诈3.5万元。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_talk","condition":"接受搭讪","label":"通过好友请求并聊天","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"拒绝","label":"忽略好友请求","isSafeChoice":true},
      {"from":"d_talk","to":"d_install","condition":"下载APP","label":"下载对方推荐的APP","isSafeChoice":false},
      {"from":"d_talk","to":"e_safe","condition":"警惕","label":"拒绝下载陌生APP","isSafeChoice":true},
      {"from":"d_install","to":"d_video","condition":"继续","label":"同意视频裸聊","isSafeChoice":false},
      {"from":"d_install","to":"e_safe","condition":"犹豫","label":"安装后感到不安立即删除","isSafeChoice":true},
      {"from":"d_video","to":"d_extortion","condition":"被录视频","label":"对方发来录像和通讯录","isSafeChoice":false},
      {"from":"d_extortion","to":"d_escalate","condition":"转账","label":"转账888元封口费","isSafeChoice":false},
      {"from":"d_extortion","to":"e_warn","condition":"报警","label":"拉黑对方并报警","isSafeChoice":true},
      {"from":"d_escalate","to":"e_loss","condition":"继续转账","label":"继续转账满足对方要求","isSafeChoice":false},
      {"from":"d_escalate","to":"e_warn","condition":"止损","label":"意识到这是无底洞决定报警","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大一","大二","大三","大四"]' AS JSON),
  CAST('["all"]' AS JSON),
  2, 4.8,
  290, 52, 0.1793, 0.0560,
  1, 0, NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '裸聊敲诈诈骗');

-- 案例9：虚假购物/游戏交易诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '虚假购物/游戏交易诈骗',
  '网络诈骗',
  '<h3>案例背景</h3><p>虚假购物和游戏交易诈骗是学生群体最容易遇到的诈骗类型。诈骗分子以低价商品、账号装备交易为诱饵，诱导脱离官方平台私下交易实施诈骗。</p><h3>常见类型</h3><p><strong>一、游戏账号交易：</strong>在游戏内喊话低价出售高等级账号→诱导脱离官方平台（微信/QQ私聊）→发送仿冒交易平台链接→以「解冻费」「保证金」连环收费。</p><p><strong>二、游戏装备/皮肤：</strong>伪装卖家在闲鱼/转转发布低价游戏道具→诱导点击钓鱼链接付款→收款后不发货拉黑。</p><p><strong>三、演唱会门票：</strong>在微博/小红书发布转让信息→诱导扫码付款→伪造购票截图发送给受害者→受害者到现场才发现票是假的。</p><p><strong>真实案例：</strong>中学生小赵欲购买「限量皮肤」，在非官方平台交易时，卖家称其「账户被冻结需充值解冻」，使用母亲手机多次转账共计2.1万元。</p>',
  CAST('{
    "name": "游戏交易诈骗",
    "description": "以低价游戏装备/账号为诱饵诱导私下交易实施诈骗",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"看到低价装备","content":"你在游戏里看到有人喊话「低价出售限量皮肤，只要市场价的三折」。","role":"narrator","riskTip":"过低的价格往往是诈骗的诱饵"},
      {"id":"d_contact","type":"dialog","title":"联系卖家","content":"你添加了卖家QQ，对方发来装备截图和交易记录，看起来很靠谱，要求私下交易不走官方平台。","role":"victim","riskTip":"脱离官方担保平台的私下交易风险极高"},
      {"id":"d_link","type":"dialog","title":"点击假链接","content":"对方发来一个交易平台链接，页面看起来和官方平台一模一样，要求你注册并付款。","role":"narrator","riskTip":"仿冒平台域名往往和官网只有一两个字母的差异"},
      {"id":"d_freeze","type":"dialog","title":"账户冻结","content":"你付款后平台显示「账户冻结，需充值解冻」，客服称需要缴纳等额保证金才能解冻。","role":"victim","riskTip":"解冻费/保证金是游戏交易诈骗的经典套路"},
      {"id":"d_more","type":"dialog","title":"连环收费","content":"你缴纳保证金后，平台又要求「激活费」「提现手续费」，金额越来越大。","role":"narrator"},
      {"id":"e_safe","type":"end","title":"拒绝私下交易","content":"你坚持走官方交易平台，不私下转账。","role":"victim"},
      {"id":"e_warn","type":"end","title":"中途识破","content":"你付款一次后对方继续要钱，你意识到被骗并停止付款、举报对方。","role":"victim"},
      {"id":"e_loss","type":"end","title":"多次被骗","content":"你为了拿回之前的钱不断满足对方要求，最终损失2.1万元。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_contact","condition":"感兴趣","label":"添加卖家联系方式","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"忽略","label":"不理会低价广告","isSafeChoice":true},
      {"from":"d_contact","to":"d_link","condition":"接受","label":"同意私下交易","isSafeChoice":false},
      {"from":"d_contact","to":"e_safe","condition":"坚持走平台","label":"要求走官方交易平台","isSafeChoice":true},
      {"from":"d_link","to":"d_freeze","condition":"付款","label":"在假平台付款","isSafeChoice":false},
      {"from":"d_link","to":"e_warn","condition":"核对网址","label":"发现域名异常立即退出","isSafeChoice":true},
      {"from":"d_freeze","to":"d_more","condition":"急于解冻","label":"缴纳保证金解冻账号","isSafeChoice":false},
      {"from":"d_freeze","to":"e_warn","condition":"醒悟","label":"拒绝缴费并举报","isSafeChoice":true},
      {"from":"d_more","to":"e_loss","condition":"继续缴费","label":"为了拿回之前的钱继续缴费","isSafeChoice":false},
      {"from":"d_more","to":"e_warn","condition":"止损","label":"意识到无底洞立即报警","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大一","大二","大三"]' AS JSON),
  CAST('["计算机","软件工程"]' AS JSON),
  2, 3.2,
  310, 55, 0.1774, 0.0540,
  1, 0, NOW() - INTERVAL 9 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '虚假购物/游戏交易诈骗');

-- 案例10：虚拟货币洗钱诈骗
INSERT INTO fraud_case
  (title, case_type, content, scripts, target_grades, target_majors, difficulty_level, risk_score,
   view_count, like_count, like_rate, wilson_score, status, is_featured, publish_time)
SELECT
  '虚拟货币洗钱诈骗',
  '金融诈骗',
  '<h3>案例背景</h3><p>虚拟货币洗钱诈骗是最新型的诈骗手法。诈骗分子冒充交易所客服或公检法，谎称账户存在洗钱风险，诱导受害者购买USDT等稳定币转入指定的「安全钱包地址」实施诈骗。</p><h3>诈骗流程</h3><p><strong>第一步：</strong>冒充交易所客服/公检法来电，谎称账户存在洗钱风险/涉嫌洗钱。</p><p><strong>第二步：</strong>制造恐慌——「你的账户被黑客利用进行洗钱，所有用户都会被调查」。</p><p><strong>第三步：</strong>诱导购买USDT（泰达币）等稳定币。</p><p><strong>第四步：</strong>要求将USDT转入指定的「安全钱包地址」进行「资金验资」。</p><p><strong>第五步：</strong>承诺验资后原路退回，实际转入诈骗地址后立刻转走。</p><p><strong>特点：</strong>利用虚拟货币的匿名性和不可逆性，一旦转账无法追回。区块链技术被犯罪分子利用，USDT的链上转账不可逆、难以追踪资金去向。</p><p><strong>真实案例：</strong>投资者郑先生接到「某交易所客服」电话，称其账户涉及洗钱需配合调查，按指引购买价值25万元的USDT转入「安全钱包」，后发现被骗。</p>',
  CAST('{
    "name": "虚拟货币洗钱诈骗",
    "description": "冒充交易所客服以洗钱风险为由诱导购买虚拟货币转入骗子钱包",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"接到交易所客服电话","content":"你接到某加密货币交易所的客服来电，称你的账户涉嫌参与洗钱活动，需要配合调查。","role":"narrator","riskTip":"正规交易所不会电话要求转账或转入安全钱包"},
      {"id":"d_detail","type":"dialog","title":"准确说出信息","content":"客服准确说出你的账户注册时间、交易记录和绑定的银行卡号，让你开始相信对方身份。","role":"victim","riskTip":"个人信息泄露不等于对方身份真实"},
      {"id":"d_panic","type":"dialog","title":"制造恐慌","content":"客服称由于你的账户被利用，你的银行卡和名下所有资产都将被冻结，除非配合清查。","role":"narrator","riskTip":"制造恐慌让你失去判断力是诈骗的惯用手段"},
      {"id":"d_buy","type":"dialog","title":"诱导购买USDT","content":"客服引导你在交易所购买价值25万元的USDT，并称这是「资金验资」的必需步骤。","role":"victim","riskTip":"任何要求购买虚拟货币转入陌生地址的都是诈骗"},
      {"id":"d_wallet","type":"dialog","title":"转入安全钱包","content":"客服发来一个「官方安全钱包地址」，要求你将USDT转入进行验资，承诺验资后原路退回。","role":"narrator","riskTip":"区块链转账不可逆，一旦转出无法追回"},
      {"id":"e_safe","type":"end","title":"识破骗局","content":"你听到涉及转账时产生警惕，挂断后自行联系交易所官方客服核实。","role":"victim"},
      {"id":"e_warn","type":"end","title":"中途醒悟","content":"你购买了少量USDT后对转入陌生地址感到不安，停止操作并报警。","role":"victim"},
      {"id":"e_loss","type":"end","title":"资金损失","content":"你将全部USDT转入骗子提供的地址，后发现无法联系对方，损失25万元。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_detail","condition":"接听","label":"继续听对方说","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"挂断","label":"直接挂断并通过官方APP核实","isSafeChoice":true},
      {"from":"d_detail","to":"d_panic","condition":"相信","label":"相信对方是交易所工作人员","isSafeChoice":false},
      {"from":"d_detail","to":"e_safe","condition":"质疑","label":"自行拨打交易所官方热线","isSafeChoice":true},
      {"from":"d_panic","to":"d_buy","condition":"恐慌","label":"信以为真并配合操作","isSafeChoice":false},
      {"from":"d_panic","to":"e_warn","condition":"冷静","label":"先咨询懂币圈的朋友","isSafeChoice":true},
      {"from":"d_buy","to":"d_wallet","condition":"购买USDT","label":"购买价值25万元的USDT","isSafeChoice":false},
      {"from":"d_buy","to":"e_warn","condition":"犹豫","label":"对购买大额USDT产生怀疑","isSafeChoice":true},
      {"from":"d_wallet","to":"e_loss","condition":"转账","label":"将USDT转入安全钱包地址","isSafeChoice":false},
      {"from":"d_wallet","to":"e_warn","condition":"最后警觉","label":"转账前再次核实发现是诈骗","isSafeChoice":true}
    ]
  }' AS JSON),
  CAST('["大三","大四"]' AS JSON),
  CAST('["金融学","经济学","信息安全","计算机"]' AS JSON),
  5, 9.0,
  180, 42, 0.2333, 0.0620,
  1, 1, NOW() - INTERVAL 10 DAY
WHERE NOT EXISTS (SELECT 1 FROM fraud_case WHERE title = '虚拟货币洗钱诈骗');


-- =====================================================
-- 第三部分：案例-标签关联
-- =====================================================

-- 案例1：杀猪盘 → 杀猪盘、AI深度伪造
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '杀猪盘·复合式情感投资诈骗'
  AND t.name IN ('杀猪盘', '红包返利')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例2：AI深度伪造 → AI深度伪造、冒充公检法
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = 'AI深度伪造诈骗（换脸/语音克隆/身份确认）'
  AND t.name IN ('AI深度伪造', '冒充公检法')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例3：刷单返利 → 刷单诈骗、红包返利
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '刷单返利诈骗（含线下现金/数字人民币变种）'
  AND t.name IN ('刷单诈骗', '红包返利')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例4：冒充客服 → 冒充客服、刷单诈骗
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '冒充客服·屏幕共享诈骗'
  AND t.name IN ('冒充客服', '网络贷款')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例5：冒充公检法 → 冒充公检法、杀猪盘
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '冒充公检法·FaceTime诈骗'
  AND t.name IN ('冒充公检法', 'AI深度伪造')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例6：虚假贷款 → 网络贷款、刷单诈骗
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '虚假网络贷款诈骗'
  AND t.name IN ('网络贷款', '注销校园贷')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例7：注销校园贷 → 注销校园贷、冒充客服
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '注销校园贷/调整征信诈骗'
  AND t.name IN ('注销校园贷', '网络贷款')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例8：裸聊敲诈 → 裸聊敲诈、杀猪盘
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '裸聊敲诈诈骗'
  AND t.name IN ('裸聊敲诈', '游戏交易')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例9：游戏交易 → 游戏交易、网络贷款
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '虚假购物/游戏交易诈骗'
  AND t.name IN ('游戏交易', '红包返利')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );

-- 案例10：虚拟货币 → 虚拟货币洗钱、杀猪盘
INSERT INTO case_tag_relation (case_id, tag_id)
SELECT c.id, t.id
FROM fraud_case c, case_tag t
WHERE c.title = '虚拟货币洗钱诈骗'
  AND t.name IN ('虚拟货币洗钱', '网络贷款')
  AND NOT EXISTS (
    SELECT 1 FROM case_tag_relation r
    WHERE r.case_id = c.id AND r.tag_id = t.id
  );


-- =====================================================
-- 第四部分：资讯文章（10篇）
-- =====================================================

-- 资讯1：杀猪盘预警
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '【预警】杀猪盘复合式情感投资诈骗高发',
  '<p><strong>案情通报：</strong>近期杀猪盘诈骗案件持续高发，2025年7月以来仅澳门地区至少18名居民被骗，总损失超2789万澳门元。</p><p><strong>手法揭秘：</strong>诈骗分子通过婚恋平台筛选目标，打造「完美人设」建立情感依赖后，以「内部投资渠道」「数字货币套利」为诱饵诱导大额充值，最终以「账户冻结需解冻费」等连环收费榨干受害者。</p><p><strong>防范要点：</strong>①网络交友涉及金钱往来务必警惕；②任何声称稳赚不赔的投资都是诈骗；③不要下载非官方应用商店的投资APP；④发现被骗立即拨打96110。</p>',
  '近期杀猪盘诈骗高发，虚假投资平台「INST-SI」「TOKENPOCKET」为典型诈骗工具，请师生提高警惕。',
  NULL, 2, 1, 'warning', 1, 1, 2100, 1, NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '【预警】杀猪盘复合式情感投资诈骗高发');

-- 资讯2：AI深度伪造预警
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '【预警】AI换脸/语音克隆诈骗来袭，视频通话也可能是假的',
  '<p><strong>技术揭秘：</strong>诈骗分子利用Deepfake技术，仅需10~30张目标人物照片即可生成可实时驱动的换脸模型。通过社交平台获取受害者亲友照片后，伪装成亲友进行视频通话实施诈骗。</p><p><strong>典型案例：</strong>浙江王女士收到「女儿」视频电话，画面中女儿满脸伤痕哭诉车祸需15万手术费，转账后核实发现女儿在校安然无恙。某公司财务收到「CEO」语音消息要求加急转账85万，声音语气完全一致。</p><p><strong>2026新型变种：</strong>「身份确认」语音取证诈骗——只问「请问是XX本人吗」，全程录音后利用银行语音核验漏洞开通代扣业务。</p><p><strong>防范要点：</strong>①视频通话要求转账时，让对方做特定动作（挥手/转头）验证是否AI换脸；②挂断后通过常用号码回拨核实；③陌生来电绝不随意回答「是」。</p>',
  'AI深度伪造技术被用于诈骗，视频通话和语音消息均可能被伪造，涉及转账务必多渠道确认。',
  NULL, 2, 1, 'warning', 0, 1, 1850, 1, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '【预警】AI换脸/语音克隆诈骗来袭，视频通话也可能是假的');

-- 资讯3：刷单返利新变种
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '刷单返利诈骗升级：线下现金交付+数字人民币新手法',
  '<p><strong>发案数据：</strong>刷单返利类诈骗是目前发案量最高的诈骗类型。</p><p><strong>传统手法：</strong>在短视频平台发布「点赞赚钱」广告，前10单小额返利骗取信任，诱导下载专属APP做联单任务后以「操作失误需补单」为由连环收费。</p><p><strong>2025新变种一（线下现金交付）：</strong>不再线上转账，要求受害人提取现金放置于指定地点（如小区垃圾箱、公园长椅下），诈骗团伙用遥控车转移赃款，规避银行风控监测。</p><p><strong>2025新变种二（数字人民币钱包）：</strong>诱导用户开通数字人民币钱包并绑定银行卡，以「充值返利」名义将资金充值到虚假平台。</p><p><strong>防范要点：</strong>①只要是刷单就是诈骗；②拒绝垫资刷单；③不下载非官方APP；④发现被骗立即报警。</p>',
  '刷单诈骗出现线下现金交付和数字人民币新手法，任何要求垫资的兼职都是诈骗。',
  NULL, 4, 1, 'news', 0, 0, 1680, 1, NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '刷单返利诈骗升级：线下现金交付+数字人民币新手法');

-- 资讯4：屏幕共享风险
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '「屏幕共享」成诈骗新工具，17.6万元瞬间被转走',
  '<p><strong>案情通报：</strong>重庆王某接到「电商客服」电话，称其购买的商品有质量问题可三倍退款，被诱导下载「云会议」APP开启屏幕共享，银行卡内17.6万元被分批转走。</p><p><strong>手法揭秘：</strong>诈骗分子非法获取用户网购订单信息后，准确说出订单细节建立信任。随后以「指导退款操作」为由诱导下载会议APP（腾讯会议/Zoom/云会议等），并开启屏幕共享功能。</p><p><strong>技术原理：</strong>开启屏幕共享后，诈骗分子可实时查看受害者手机上的短信验证码、银行卡号、密码等敏感信息，无需受害者主动提供即可转走资金。</p><p><strong>防范要点：</strong>①任何要求共享屏幕的客服都是诈骗；②退款理赔应通过官方APP内渠道处理；③切勿在共享屏幕状态下操作银行APP；④挂断后自行拨打官方客服电话核实。</p>',
  '屏幕共享功能被诈骗分子利用，开启后银行卡信息和验证码一览无余，切勿与陌生人共享屏幕。',
  NULL, 4, 1, 'news', 0, 0, 1520, 1, NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '「屏幕共享」成诈骗新工具，17.6万元瞬间被转走');

-- 资讯5：FaceTime冒充公检法
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '【紧急预警】FaceTime冒充公检法诈骗，显示公安局号码也可能是假的',
  '<p><strong>新型手法：</strong>诈骗分子通过苹果FaceTime视频通话冒充公检法，视频画面中穿着假警服、背景是伪造的「公安局」办公室，同时使用改号软件将来电显示修改为当地公安局的真实号码。</p><p><strong>典型案例：</strong>北京张女士接到FaceTime来电，对方显示为「北京市公安局」，视频中看到穿警服的「民警」出示伪造的逮捕令，最终被骗3万元。</p><p><strong>完整话术流程：</strong>①「民警」告知涉嫌洗钱案→②准确说出受害者身份信息→③发送伪造逮捕令→④要求绝对保密不准告诉任何人→⑤诱导下载「安全防护」APP（实为远程控制软件）→⑥要求转入「安全账户」进行资金清查。</p><p><strong>警方提示：</strong>公检法机关不会通过FaceTime、微信视频等网络方式办案，更不会设立「安全账户」。接到此类电话请立即挂断并拨打110核实。</p>',
  '诈骗分子通过FaceTime视频冒充公检法，改号伪装公安局号码，请勿相信。公检法不会网络办案。',
  NULL, 2, 1, 'warning', 1, 1, 2350, 1, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '【紧急预警】FaceTime冒充公检法诈骗，显示公安局号码也可能是假的');

-- 资讯6：虚假贷款平台识别
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '如何识别虚假网络贷款平台？记住这五个警示信号',
  '<p><strong>现状：</strong>虚假网络贷款诈骗利用人们急需资金周转的心理，以「无抵押、低利息、秒放款」为诱饵，通过仿冒APP和连环收费实施诈骗。</p><p><strong>诈骗流程：</strong>①投放「无抵押、秒放款」广告→②诱导下载仿冒APP→③显示「审核通过，额度XX万」→④提现时显示「银行卡号错误，资金被冻结」→⑤要求缴纳解冻费、保证金、保险费等连环收费。</p><p><strong>技术内幕：</strong>诈骗APP的后台可人工控制「错误提示」的内容和时机。所谓「银行卡号错误」实际上是骗子在后台手动修改受害者填写的卡号。</p><p><strong>五个警示信号：</strong></p><p>①任何放款前要求缴费的贷款都是诈骗；②非应用商店下载的贷款APP极可能是仿冒的；③号称「不查征信、秒批」的多为骗局；④客服通过个人微信/QQ联系而非官方渠道；⑤要求缴纳解冻费/保证金/刷流水的都是诈骗。</p>',
  '虚假贷款平台以「无抵押秒放款」为饵，放款前收费是典型诈骗信号，记住五个警示信号远离骗局。',
  NULL, 1, 1, 'news', 0, 0, 1180, 1, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '如何识别虚假网络贷款平台？记住这五个警示信号');

-- 资讯7：注销校园贷防范
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '【预警】注销校园贷诈骗专盯大学生，毕业季需特别警惕',
  '<p><strong>案情通报：</strong>刚毕业的小刘接到「银监会工作人员」电话，称其大学期间注册过校园贷需注销否则影响征信，在恐慌之下从多个网贷平台借款18万元转入「清算账户」，后发现被骗。</p><p><strong>诈骗三部曲：</strong></p><p><strong>第一步：</strong>冒充银监会/京东金融/支付宝工作人员来电，称你在学生时期注册过校园贷/京东白条，现在国家禁止校园贷，需要注销否则影响个人征信。</p><p><strong>第二步：</strong>诱导查询征信，你看到征信报告上有大学期间的信贷记录（花呗/信用卡），产生恐慌。</p><p><strong>第三步：</strong>要求从借呗、微粒贷、美团借钱等平台借款，转入「银监会清算账户」进行「清零操作」，承诺借款还清后可撤销贷款记录。</p><p><strong>核心逻辑：</strong>利用大学期间开通过的小额信贷服务，制造「不注销就会影响征信」的恐慌。</p><p><strong>重要提醒：</strong>①个人征信由央行统一管理，任何人无权修改；②银监会没有所谓清算账户；③任何要求网贷后转账的都是诈骗；④接到此类电话直接挂断并拨打96110咨询。</p>',
  '毕业季到来，注销校园贷诈骗进入高发期。接到「影响征信」电话要求网贷转账的都是诈骗。',
  NULL, 2, 1, 'warning', 0, 1, 1980, 1, NOW() - INTERVAL 7 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '【预警】注销校园贷诈骗专盯大学生，毕业季需特别警惕');

-- 资讯8：裸聊敲诈警示
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '【警示】裸聊敲诈手法揭秘：你的通讯录可能已被窃取',
  '<p><strong>案情通报：</strong>公司职员吴先生深夜被社交软件上「美女」搭讪，短暂视频裸聊后被发来不雅视频及通讯录截图，被迫转账3.5万元「封口费」。</p><p><strong>手法全揭秘：</strong></p><p>①诈骗分子在陌陌/探探/Soul等社交平台以美女身份添加好友；②诱导下载指定「聊天APP」（实为木马程序，安装后窃取通讯录）；③诱导视频裸聊并全程录制；④向受害者展示通讯录截图，以「向亲友群发视频」为要挟勒索钱财。</p><p><strong>技术细节：</strong>所谓「美女」多为提前录制的色情视频在摄像头前播放+实时语音互动。木马APP伪装成人脸识别/直播软件。敲诈金额采用阶梯式递增。</p><p><strong>防范要点：</strong>①不安装非官方应用商店的APP；②不给陌生APP授权通讯录权限；③遇到裸聊敲诈不要转账，立刻报警；④不要因羞耻感而妥协，报警是最佳选择。</p>',
  '裸聊敲诈利用木马APP窃取通讯录后勒索，遇到此类威胁不要转账，立即报警。',
  NULL, 4, 1, 'news', 0, 0, 920, 1, NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '【警示】裸聊敲诈手法揭秘：你的通讯录可能已被窃取');

-- 资讯9：游戏交易安全
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '游戏账号装备交易诈骗手法汇总，学生群体需特别留意',
  '<p><strong>现状：</strong>虚假购物和游戏交易诈骗是学生群体最容易遇到的诈骗类型。</p><p><strong>常见手法一（游戏账号交易）：</strong>在游戏内喊话低价出售高等级账号→诱导脱离官方平台（微信/QQ私聊）→发送仿冒交易平台链接→以「解冻费」「保证金」连环收费。</p><p><strong>常见手法二（游戏装备/皮肤）：</strong>伪装卖家在闲鱼/转转发布低价游戏道具→诱导点击钓鱼链接付款→收款后不发货拉黑。</p><p><strong>常见手法三（演唱会门票）：</strong>在微博/小红书发布转让信息→诱导扫码付款→伪造购票截图→到现场才发现票是假的。</p><p><strong>真实案例：</strong>中学生小赵欲购买「限量皮肤」，在非官方平台交易时被要求多次转账共计2.1万元。</p><p><strong>防范要点：</strong>①坚持走官方交易平台担保交易；②拒绝站外私下付款；③不点击陌生链接，核对网址域名；④「解冻费」「保证金」都是诈骗话术。</p>',
  '游戏交易诈骗频发，坚持走官方平台担保交易，拒绝站外私下付款是防骗关键。',
  NULL, 4, 1, 'news', 0, 0, 1340, 1, NOW() - INTERVAL 9 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '游戏账号装备交易诈骗手法汇总，学生群体需特别留意');

-- 资讯10：虚拟货币安全
INSERT INTO news
  (title, content, summary, cover_image, category_id, author_id, news_type, is_top, is_mandatory, view_count, status, publish_time)
SELECT
  '【政策】关于防范以「虚拟货币验资」为名实施诈骗的风险提示',
  '<p><strong>最新骗局：</strong>虚拟货币洗钱诈骗是最新型的诈骗手法。诈骗分子冒充交易所客服或公检法，谎称账户存在洗钱风险，诱导受害者购买USDT等稳定币转入指定的「安全钱包地址」。</p><p><strong>典型案例：</strong>投资者郑先生接到「某交易所客服」电话，称其账户涉及洗钱需配合调查，按指引购买价值25万元的USDT转入「安全钱包」后无法追回。</p><p><strong>诈骗流程：</strong>①冒充交易所客服谎称账户存在洗钱风险→②制造恐慌「不配合将冻结所有资产」→③诱导购买USDT等稳定币→④要求转入指定「安全钱包地址」进行「验资」→⑤承诺验资后退回，实际立即转走。</p><p><strong>监管提示：</strong>中国人民银行等十部门明确防范虚拟货币交易炒作风险。任何要求购买虚拟货币转入陌生地址进行「验资」「清查」的都是诈骗。区块链转账具有不可逆性，一旦转出无法追回。</p><p><strong>防范要点：</strong>①正规交易所不会电话要求转账或转入安全钱包；②切勿向陌生地址转账虚拟货币；③发现被骗立即报警并联系交易所冻结账户。</p>',
  '虚拟货币「验资」诈骗兴起，利用USDT不可逆特性实施诈骗。勿向陌生地址转账虚拟货币。',
  NULL, 3, 1, 'policy', 0, 0, 760, 1, NOW() - INTERVAL 10 DAY
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title = '【政策】关于防范以「虚拟货币验资」为名实施诈骗的风险提示');

-- =====================================================
-- 第五部分：知识闯关题库（10组×5题=50道）
-- =====================================================

-- 答题1：杀猪盘识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：杀猪盘·情感投资诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：杀猪盘·情感投资诈骗识别',
  '测试你对杀猪盘诈骗话术和防范要点的掌握程度。',
  6, 3, 'quiz', 40, 20,
  CAST('{
    "questions": [
      {
        "id": "zp_q1",
        "questionType": "single",
        "text": "杀猪盘诈骗中，「养猪」阶段的主要目的是什么？",
        "options": [
          {"label":"A","text":"获取受害者的银行卡密码"},
          {"label":"B","text":"通过长期情感交流建立信任和依赖"},
          {"label":"C","text":"直接要求受害者转账"},
          {"label":"D","text":"窃取受害者的社交账号"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "zp_q2",
        "questionType": "single",
        "text": "杀猪盘诈骗中，诈骗分子通常在哪个阶段让受害者「小额试水并成功提现」？",
        "options": [
          {"label":"A","text":"选猪阶段——筛选目标时"},
          {"label":"B","text":"养猪阶段——建立情感联系时"},
          {"label":"C","text":"杀猪阶段——诱导投资时"},
          {"label":"D","text":"毁尸阶段——实施连环收费时"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "zp_q3",
        "questionType": "multiple",
        "text": "以下哪些行为属于杀猪盘的高危信号？（多选）",
        "options": [
          {"label":"A","text":"对方以「内部渠道」「稳赚不赔」诱导投资"},
          {"label":"B","text":"对方拒绝视频验证或线下见面"},
          {"label":"C","text":"对方主动提出带你赚钱，先让你小额获利"},
          {"label":"D","text":"对方分享日常生活的照片和经历"}
        ],
        "correctIndexes": [0, 1, 2],
        "score": 20
      },
      {
        "id": "zp_q4",
        "questionType": "single",
        "text": "遇到杀猪盘诈骗后，以下做法正确的是？",
        "options": [
          {"label":"A","text":"继续与对方沟通争取要回之前的钱"},
          {"label":"B","text":"立即停止转账，保留聊天和转账记录并报警"},
          {"label":"C","text":"自行联系所谓的投资平台客服协商"},
          {"label":"D","text":"因为羞耻而隐瞒不报"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "zp_q5",
        "questionType": "single",
        "text": "杀猪盘诈骗的洗钱路径通常是？",
        "options": [
          {"label":"A","text":"通过银行柜台直接取现"},
          {"label":"B","text":"诈骗赃款→中间账户→购买商品转卖→多级账户→虚拟货币洗白"},
          {"label":"C","text":"直接转入诈骗分子个人账户"},
          {"label":"D","text":"通过支付宝转给亲友"}
        ],
        "correctIndexes": [1],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：杀猪盘·情感投资诈骗识别');

-- 答题2：AI深度伪造识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：AI深度伪造诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：AI深度伪造诈骗识别',
  '测试你对AI换脸、语音克隆等新型诈骗的识别能力。',
  7, 4, 'quiz', 40, 25,
  CAST('{
    "questions": [
      {
        "id": "ai_q1",
        "questionType": "single",
        "text": "诈骗分子进行AI换脸诈骗至少需要多少张目标人物照片？",
        "options": [
          {"label":"A","text":"1~5张"},
          {"label":"B","text":"10~30张"},
          {"label":"C","text":"100张以上"},
          {"label":"D","text":"需要完整视频"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "ai_q2",
        "questionType": "single",
        "text": "接到亲友视频电话要求转账时，以下哪种方法可验证对方是否为AI换脸？",
        "options": [
          {"label":"A","text":"听声音判断是否熟悉"},
          {"label":"B","text":"让对方做快速挥手或转头的动作"},
          {"label":"C","text":"看对方的面容是否清晰"},
          {"label":"D","text":"询问对方一些生活问题"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "ai_q3",
        "questionType": "single",
        "text": "2026年新型「身份确认」语音取证诈骗的核心套路是什么？",
        "options": [
          {"label":"A","text":"索要银行卡号和密码"},
          {"label":"B","text":"诱导下载木马APP"},
          {"label":"C","text":"只问「请问是XX本人吗」全程录音，利用录音开通代扣业务"},
          {"label":"D","text":"要求转账到安全账户"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "ai_q4",
        "questionType": "multiple",
        "text": "以下哪些属于AI深度伪造诈骗的防范要点？（多选）",
        "options": [
          {"label":"A","text":"视频通话时要求对方做特定动作验证"},
          {"label":"B","text":"挂断后通过常用号码回拨核实"},
          {"label":"C","text":"陌生来电绝不随意回答「是」"},
          {"label":"D","text":"只要是视频通话看到人脸就完全信任"}
        ],
        "correctIndexes": [0, 1, 2],
        "score": 20
      },
      {
        "id": "ai_q5",
        "questionType": "single",
        "text": "AI语音克隆诈骗需要多长的声音样本即可克隆？",
        "options": [
          {"label":"A","text":"至少10分钟"},
          {"label":"B","text":"需要1小时以上的录音"},
          {"label":"C","text":"约30秒即可"},
          {"label":"D","text":"需要3分钟以上的清晰对话"}
        ],
        "correctIndexes": [2],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：AI深度伪造诈骗识别');

-- 答题3：刷单返利识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：刷单返利诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：刷单返利诈骗识别',
  '测试你对刷单返利类诈骗的识别和防范能力。',
  8, 2, 'quiz', 40, 15,
  CAST('{
    "questions": [
      {
        "id": "sd_q1",
        "questionType": "single",
        "text": "刷单返利诈骗中，前几单正常返现的目的是？",
        "options": [
          {"label":"A","text":"展示电商平台的正常运营"},
          {"label":"B","text":"骗取信任，让受害者放松警惕加大投入"},
          {"label":"C","text":"测试受害者的操作速度"},
          {"label":"D","text":"为平台增加真实销量"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "sd_q2",
        "questionType": "single",
        "text": "2025年刷单新变种中，诈骗分子要求线下现金交付的目的是？",
        "options": [
          {"label":"A","text":"方便给受害者送礼物"},
          {"label":"B","text":"规避银行风控系统的大额转账监测"},
          {"label":"C","text":"让受害者当面签合同"},
          {"label":"D","text":"提高诈骗效率"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "sd_q3",
        "questionType": "multiple",
        "text": "以下哪些是刷单返利诈骗的典型特征？（多选）",
        "options": [
          {"label":"A","text":"承诺「日结高薪、月入过万、足不出户」"},
          {"label":"B","text":"要求垫资做联单任务"},
          {"label":"C","text":"以「操作失误需补单」为由要求继续转账"},
          {"label":"D","text":"要求下载非官方应用商店的APP"}
        ],
        "correctIndexes": [0, 1, 2, 3],
        "score": 20
      },
      {
        "id": "sd_q4",
        "questionType": "single",
        "text": "遇到「点赞赚钱、日结高薪」的兼职广告，正确的做法是？",
        "options": [
          {"label":"A","text":"尝试做几单赚点零花钱"},
          {"label":"B","text":"识别为诈骗，不参与并可以向平台举报"},
          {"label":"C","text":"推荐给朋友一起赚钱"},
          {"label":"D","text":"先垫付小额资金试试"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "sd_q5",
        "questionType": "single",
        "text": "刷单返利诈骗中，「联单任务」通常意味着什么？",
        "options": [
          {"label":"A","text":"连续完成多单才能一并结算提现"},
          {"label":"B","text":"组队完成可以拿到更多返利"},
          {"label":"C","text":"联合其他平台一起推广"},
          {"label":"D","text":"需要邀请好友一起参与"}
        ],
        "correctIndexes": [0],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：刷单返利诈骗识别');

-- 答题4：冒充客服识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：冒充客服屏幕共享诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：冒充客服屏幕共享诈骗识别',
  '测试你对冒充客服类诈骗话术和屏幕共享风险的识别能力。',
  9, 3, 'quiz', 40, 20,
  CAST('{
    "questions": [
      {
        "id": "kf_q1",
        "questionType": "single",
        "text": "冒充客服诈骗中，诈骗分子能准确说出你的订单信息，这说明什么？",
        "options": [
          {"label":"A","text":"对方是真正的平台客服"},
          {"label":"B","text":"你的个人信息已被非法获取，不代表对方身份真实"},
          {"label":"C","text":"该平台数据安全做得好"},
          {"label":"D","text":"这是正常客服回访流程"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "kf_q2",
        "questionType": "single",
        "text": "以下哪种行为是冒充客服诈骗的核心高危动作？",
        "options": [
          {"label":"A","text":"要求你下载会议APP并开启屏幕共享"},
          {"label":"B","text":"要求你在官方APP内查看订单"},
          {"label":"C","text":"建议你拨打官方客服电话核实"},
          {"label":"D","text":"发送订单截图给你确认"}
        ],
        "correctIndexes": [0],
        "score": 10
      },
      {
        "id": "kf_q3",
        "questionType": "single",
        "text": "开启屏幕共享后，以下哪项信息最容易被诈骗分子窃取？",
        "options": [
          {"label":"A","text":"手机型号和操作系统版本"},
          {"label":"B","text":"手机上的短信验证码和银行卡密码"},
          {"label":"C","text":"已安装APP列表"},
          {"label":"D","text":"WiFi网络名称"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "kf_q4",
        "questionType": "multiple",
        "text": "接到自称客服的电话要求退款理赔时，正确的做法包括？（多选）",
        "options": [
          {"label":"A","text":"挂断电话，通过官方APP内的客服渠道核实"},
          {"label":"B","text":"拒绝任何要求共享屏幕的操作"},
          {"label":"C","text":"不向对方提供短信验证码"},
          {"label":"D","text":"按对方指引下载会议APP配合操作"}
        ],
        "correctIndexes": [0, 1, 2],
        "score": 20
      },
      {
        "id": "kf_q5",
        "questionType": "single",
        "text": "冒充客服常用的三种话术中，不包括以下哪项？",
        "options": [
          {"label":"A","text":"商品质量问题可X倍退款"},
          {"label":"B","text":"快递丢失可双倍赔付"},
          {"label":"C","text":"恭喜你中奖了需要先缴纳手续费"},
          {"label":"D","text":"误开通会员/代理商需取消否则每月扣费"}
        ],
        "correctIndexes": [2],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：冒充客服屏幕共享诈骗识别');

-- 答题5：冒充公检法识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：冒充公检法FaceTime诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：冒充公检法FaceTime诈骗识别',
  '测试你对冒充公检法诈骗的心理操控手段和防范要点的理解。',
  10, 4, 'quiz', 40, 25,
  CAST('{
    "questions": [
      {
        "id": "gjf_q1",
        "questionType": "single",
        "text": "冒充公检法诈骗中，要求受害者「绝对保密不准告诉任何人」的目的是？",
        "options": [
          {"label":"A","text":"案件确实需要保密"},
          {"label":"B","text":"保护受害者的人身安全"},
          {"label":"C","text":"隔离受害者阻止其向亲友求助，方便继续操控"},
          {"label":"D","text":"避免信息泄露影响办案"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "gjf_q2",
        "questionType": "single",
        "text": "公检法机关不会通过以下哪种方式办案？",
        "options": [
          {"label":"A","text":"电话传唤当事人"},
          {"label":"B","text":"上门出示证件和文书"},
          {"label":"C","text":"通过FaceTime或微信视频通话发送逮捕令"},
          {"label":"D","text":"在派出所内进行讯问"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "gjf_q3",
        "questionType": "single",
        "text": "对方通过FaceTime来电，显示为「XX市公安局」，以下做法正确的是？",
        "options": [
          {"label":"A","text":"接通视频配合调查"},
          {"label":"B","text":"直接挂断，自行拨打110或到派出所核实"},
          {"label":"C","text":"按对方要求将资金转入安全账户"},
          {"label":"D","text":"保存对方号码后回拨"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "gjf_q4",
        "questionType": "multiple",
        "text": "冒充公检法诈骗使用哪些手段制造恐慌？（多选）",
        "options": [
          {"label":"A","text":"突然告知你涉嫌洗钱等严重犯罪"},
          {"label":"B","text":"准确说出你的姓名、身份证号等个人信息"},
          {"label":"C","text":"发送伪造的逮捕令或通缉令"},
          {"label":"D","text":"以「连累家人」施压阻止你求助"}
        ],
        "correctIndexes": [0, 1, 2, 3],
        "score": 20
      },
      {
        "id": "gjf_q5",
        "questionType": "single",
        "text": "所谓「安全账户」是什么？",
        "options": [
          {"label":"A","text":"公安机关设立的专门资金监管账户"},
          {"label":"B","text":"银行提供的额外安全保障服务"},
          {"label":"C","text":"纯粹是诈骗分子虚构的概念，公检法没有安全账户"},
          {"label":"D","text":"国家反诈中心指定的保护账户"}
        ],
        "correctIndexes": [2],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：冒充公检法FaceTime诈骗识别');

-- 答题6：虚假贷款识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：虚假网络贷款诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：虚假网络贷款诈骗识别',
  '测试你对虚假网络贷款诈骗的识别能力。',
  11, 3, 'quiz', 40, 20,
  CAST('{
    "questions": [
      {
        "id": "dk_q1",
        "questionType": "single",
        "text": "虚假贷款诈骗中，显示「银行卡号错误」的真实原因是什么？",
        "options": [
          {"label":"A","text":"受害者确实填错了银行卡号"},
          {"label":"B","text":"银行系统故障导致"},
          {"label":"C","text":"诈骗分子在后台手动修改受害者填写的正确卡号"},
          {"label":"D","text":"网络传输错误"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "dk_q2",
        "questionType": "single",
        "text": "以下哪条是识别虚假贷款诈骗的黄金法则？",
        "options": [
          {"label":"A","text":"贷款额度越高越可靠"},
          {"label":"B","text":"任何放款前要求缴费的都是诈骗"},
          {"label":"C","text":"利息越低越安全"},
          {"label":"D","text":"APP界面越正规越可信"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "dk_q3",
        "questionType": "single",
        "text": "虚假贷款诈骗中，诈骗分子通常要求缴纳以下哪种费用？",
        "options": [
          {"label":"A","text":"贷款利息"},
          {"label":"B","text":"账户管理费"},
          {"label":"C","text":"解冻费、保证金、刷流水"},
          {"label":"D","text":"年费"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "dk_q4",
        "questionType": "multiple",
        "text": "以下哪些渠道下载的贷款APP更可能是仿冒诈骗平台？（多选）",
        "options": [
          {"label":"A","text":"通过短信链接下载"},
          {"label":"B","text":"通过官方应用商店（App Store/华为应用市场）下载"},
          {"label":"C","text":"通过搜索引擎广告中的链接下载"},
          {"label":"D","text":"通过社交软件对方发送的链接下载"}
        ],
        "correctIndexes": [0, 2, 3],
        "score": 20
      },
      {
        "id": "dk_q5",
        "questionType": "single",
        "text": "对于急需资金周转的学生，正确的借款渠道是？",
        "options": [
          {"label":"A","text":"在网上搜索「无抵押秒放款」广告"},
          {"label":"B","text":"优先考虑正规银行或学校资助渠道"},
          {"label":"C","text":"找网络贷款中介帮忙"},
          {"label":"D","text":"使用不明贷款APP"}
        ],
        "correctIndexes": [1],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：虚假网络贷款诈骗识别');

-- 答题7：注销校园贷识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：注销校园贷/调整征信诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：注销校园贷/调整征信诈骗识别',
  '测试你对注销校园贷诈骗话术和征信保护知识的掌握程度。',
  12, 3, 'quiz', 40, 20,
  CAST('{
    "questions": [
      {
        "id": "xy_q1",
        "questionType": "single",
        "text": "注销校园贷诈骗中，骗子诱导你查询征信报告看到大学期间信贷记录后，正确的认知是？",
        "options": [
          {"label":"A","text":"说明真的注册过校园贷需要立刻注销"},
          {"label":"B","text":"那可能是正常的信用卡或花呗记录，不等于校园贷"},
          {"label":"C","text":"征信报告有错误需要联系骗子处理"},
          {"label":"D","text":"必须按对方指引才能消除记录"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "xy_q2",
        "questionType": "single",
        "text": "以下关于个人征信的说法，正确的是？",
        "options": [
          {"label":"A","text":"征信记录可以由个人申请修改"},
          {"label":"B","text":"征信由央行统一管理，任何人都无权随意修改"},
          {"label":"C","text":"网贷平台可以帮忙消除不良征信"},
          {"label":"D","text":"只要付费就能修复征信"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "xy_q3",
        "questionType": "single",
        "text": "「银监会清算账户」是什么？",
        "options": [
          {"label":"A","text":"银监会设立的合法资金清理通道"},
          {"label":"B","text":"银行间结算的专用账户"},
          {"label":"C","text":"纯属诈骗分子虚构的概念，不存在这样的账户"},
          {"label":"D","text":"银保监会监管的专项资金账户"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "xy_q4",
        "questionType": "multiple",
        "text": "接到「注销校园贷否则影响征信」的电话，以下做法正确的是？（多选）",
        "options": [
          {"label":"A","text":"直接挂断电话"},
          {"label":"B","text":"自行拨打银行官方客服或96110咨询"},
          {"label":"C","text":"告诉家人或老师征询意见"},
          {"label":"D","text":"按对方要求从网贷平台借款并转账"}
        ],
        "correctIndexes": [0, 1, 2],
        "score": 20
      },
      {
        "id": "xy_q5",
        "questionType": "single",
        "text": "注销校园贷诈骗的核心是利用了受害者的哪种心理？",
        "options": [
          {"label":"A","text":"贪图小便宜"},
          {"label":"B","text":"对征信问题的恐慌和对权威的信任"},
          {"label":"C","text":"好奇心和冒险精神"},
          {"label":"D","text":"社交需求"}
        ],
        "correctIndexes": [1],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：注销校园贷/调整征信诈骗识别');

-- 答题8：裸聊敲诈识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：裸聊敲诈诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：裸聊敲诈诈骗识别',
  '测试你对裸聊敲诈手法和应对策略的了解。',
  13, 2, 'quiz', 40, 15,
  CAST('{
    "questions": [
      {
        "id": "ll_q1",
        "questionType": "single",
        "text": "裸聊敲诈中，对方发送的所谓「聊天APP」实际上是？",
        "options": [
          {"label":"A","text":"正规的加密聊天软件"},
          {"label":"B","text":"木马程序，安装后会窃取通讯录等隐私信息"},
          {"label":"C","text":"普通的视频通话软件"},
          {"label":"D","text":"直播平台"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "ll_q2",
        "questionType": "single",
        "text": "被裸聊敲诈后，以下做法正确的是？",
        "options": [
          {"label":"A","text":"感到羞耻选择沉默并转账封口费"},
          {"label":"B","text":"立即停止联系、不要转账、保留证据并报警"},
          {"label":"C","text":"与对方协商分期付款"},
          {"label":"D","text":"按对方要求继续转账让其删除视频"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "ll_q3",
        "questionType": "single",
        "text": "裸聊敲诈中诈骗分子冒充的「美女」实际上是？",
        "options": [
          {"label":"A","text":"真人美女主播"},
          {"label":"B","text":"提前录制的色情视频在摄像头前播放配合语音互动"},
          {"label":"C","text":"AI生成的虚拟人物"},
          {"label":"D","text":"诈骗团伙中的女性成员"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "ll_q4",
        "questionType": "multiple",
        "text": "如何防范裸聊敲诈诈骗？（多选）",
        "options": [
          {"label":"A","text":"不安装非官方应用商店的APP"},
          {"label":"B","text":"不给陌生APP授权通讯录权限"},
          {"label":"C","text":"拒绝与陌生人在网上进行裸聊"},
          {"label":"D","text":"遇到敲诈直接报警不转账"}
        ],
        "correctIndexes": [0, 1, 2, 3],
        "score": 20
      },
      {
        "id": "ll_q5",
        "questionType": "single",
        "text": "裸聊敲诈的敲诈金额通常采用什么模式？",
        "options": [
          {"label":"A","text":"一次性要求大额转账"},
          {"label":"B","text":"阶梯式递增：从小额开始逐步加码"},
          {"label":"C","text":"固定金额不变"},
          {"label":"D","text":"按受害者收入比例计算"}
        ],
        "correctIndexes": [1],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：裸聊敲诈诈骗识别');

-- 答题9：游戏交易识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：虚假购物/游戏交易诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：虚假购物/游戏交易诈骗识别',
  '测试你对游戏交易和虚假购物类诈骗的识别能力。',
  14, 1, 'quiz', 40, 10,
  CAST('{
    "questions": [
      {
        "id": "game_q1",
        "questionType": "single",
        "text": "游戏交易中，下列哪种交易方式最安全？",
        "options": [
          {"label":"A","text":"通过微信/QQ直接转账给卖家"},
          {"label":"B","text":"走游戏官方交易平台进行担保交易"},
          {"label":"C","text":"点击卖家发送的链接付款"},
          {"label":"D","text":"扫描卖家发来的收款码付款"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "game_q2",
        "questionType": "single",
        "text": "对方要求缴纳「解冻费」才能完成游戏交易时，这意味着什么？",
        "options": [
          {"label":"A","text":"正常交易流程，缴纳后即可完成"},
          {"label":"B","text":"平台规定的安全保证金"},
          {"label":"C","text":"100%是诈骗，立即停止交易"},
          {"label":"D","text":"需要联系平台客服确认"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "game_q3",
        "questionType": "single",
        "text": "游戏交易诈骗中，卖家要求私下交易最可能的目的是？",
        "options": [
          {"label":"A","text":"省去平台手续费"},
          {"label":"B","text":"交易速度更快"},
          {"label":"C","text":"脱离平台监管以便实施诈骗"},
          {"label":"D","text":"保护买卖双方隐私"}
        ],
        "correctIndexes": [2],
        "score": 10
      },
      {
        "id": "game_q4",
        "questionType": "multiple",
        "text": "以下哪些属于游戏交易/虚假购物诈骗的典型特征？（多选）",
        "options": [
          {"label":"A","text":"价格明显低于市场价"},
          {"label":"B","text":"要求脱离官方平台私下交易"},
          {"label":"C","text":"发送仿冒交易平台链接"},
          {"label":"D","text":"以「账户冻结需解冻」为由要求继续充值"}
        ],
        "correctIndexes": [0, 1, 2, 3],
        "score": 20
      },
      {
        "id": "game_q5",
        "questionType": "single",
        "text": "在闲鱼/转转上购买商品时，卖家要求加微信私下交易，正确的做法是？",
        "options": [
          {"label":"A","text":"加微信方便沟通"},
          {"label":"B","text":"拒绝私下交易，坚持在平台内完成交易"},
          {"label":"C","text":"加微信后用微信转账更快捷"},
          {"label":"D","text":"加微信后让卖家发更详细的商品视频"}
        ],
        "correctIndexes": [1],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：虚假购物/游戏交易诈骗识别');

-- 答题10：虚拟货币洗钱识别
UPDATE challenge
SET passing_score = 40, status = 1
WHERE title = '答题：虚拟货币洗钱诈骗识别';

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '答题：虚拟货币洗钱诈骗识别',
  '测试你对虚拟货币相关诈骗的识别和防范能力。',
  15, 5, 'quiz', 40, 30,
  CAST('{
    "questions": [
      {
        "id": "coin_q1",
        "questionType": "single",
        "text": "虚拟货币洗钱诈骗中，诈骗分子要求你将USDT转入「安全钱包」的目的是？",
        "options": [
          {"label":"A","text":"进行资金验资以证明账户安全"},
          {"label":"B","text":"将你的USDT骗到手，利用区块链的不可逆性转走"},
          {"label":"C","text":"配合交易所升级系统"},
          {"label":"D","text":"按照监管部门要求进行合规检查"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "coin_q2",
        "questionType": "single",
        "text": "正规加密货币交易所的客服不会做以下哪件事？",
        "options": [
          {"label":"A","text":"通过官方APP内消息联系你"},
          {"label":"B","text":"电话要求你将资产转入指定钱包地址"},
          {"label":"C","text":"发送邮件通知系统维护"},
          {"label":"D","text":"在官网发布公告"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "coin_q3",
        "questionType": "single",
        "text": "USDT（泰达币）的区块链转账有什么特点？",
        "options": [
          {"label":"A","text":"转账后可以申请撤回"},
          {"label":"B","text":"转账具有不可逆性，一旦转出无法追回"},
          {"label":"C","text":"需要3个工作日才能到账"},
          {"label":"D","text":"单笔转账有金额上限"}
        ],
        "correctIndexes": [1],
        "score": 10
      },
      {
        "id": "coin_q4",
        "questionType": "multiple",
        "text": "以下哪些属于虚拟货币诈骗的防范要点？（多选）",
        "options": [
          {"label":"A","text":"正规交易所不会电话要求你转账"},
          {"label":"B","text":"不要向陌生地址转账虚拟货币"},
          {"label":"C","text":"购买虚拟货币转入指定地址「验资」都是诈骗"},
          {"label":"D","text":"接到来电显示为交易所的电话要完全信任"}
        ],
        "correctIndexes": [0, 1, 2],
        "score": 20
      },
      {
        "id": "coin_q5",
        "questionType": "single",
        "text": "对方声称你的加密货币账户涉及洗钱并要求配合调查时，正确的做法是？",
        "options": [
          {"label":"A","text":"按对方指引购买USDT转入安全钱包"},
          {"label":"B","text":"挂断电话，自行联系交易所官方客服或拨打96110咨询"},
          {"label":"C","text":"积极配合以证明自己的清白"},
          {"label":"D","text":"将账户密码告知对方便于核查"}
        ],
        "correctIndexes": [1],
        "score": 10
      }
    ]
  }' AS JSON),
  NULL,
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '答题：虚拟货币洗钱诈骗识别');

-- =====================================================
-- 第六部分：情景模拟FSM（5个完整FSM剧本）
-- =====================================================

-- 情景模拟1：杀猪盘·情感投资陷阱
INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '情景模拟：杀猪盘情感投资陷阱',
  '你在交友软件上认识了一位条件优越的异性，对方逐步引导你进行虚假投资。测试你能否识破杀猪盘的层层套路。',
  16, 4, 'scenario', 60, 35,
  CAST('{"questions":[]}' AS JSON),
  CAST('{
    "name": "杀猪盘情感投资陷阱",
    "description": "诈骗分子通过婚恋平台建立情感关系后诱导虚假投资",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"社交平台上认识TA","content":"你在交友软件上匹配到一位条件优越的异性，对方自称金融行业精英，每天主动问候、分享精致生活照片。","role":"narrator","riskTip":"警惕过于完美的陌生人，照片可全网反搜验证"},
      {"id":"d_warm","type":"dialog","title":"甜蜜互动","content":"对方对你关怀备至，每天早晚问候、分享工作日常。偶尔发来小额红包，说「请你喝杯奶茶」。你感觉遇到了对的人。","role":"victim","riskTip":"诈骗分子会进行1-4个月的情感投资才收网"},
      {"id":"d_invest","type":"dialog","title":"无意中透露投资机会","content":"对方无意中提到自己正在利用平台漏洞做数字货币套利，截图给你看收益，「这才是我真正的收入来源」。","role":"narrator","riskTip":"任何声称稳赚不赔的投资都是诈骗"},
      {"id":"d_try","type":"dialog","title":"小额试水","content":"对方邀请你试试，「拿500块玩玩，亏了算我的」。你投入500元，很快收到50元收益并成功提现。","role":"victim","riskTip":"先给甜头是杀猪盘的经典手法"},
      {"id":"d_big","type":"dialog","title":"加码投入","content":"对方说「现在行情特别好，错过了会后悔一辈子」。你被说服，将积蓄8万元全部投入。","role":"victim"},
      {"id":"d_trap","type":"dialog","title":"提现受阻","content":"你想提现时平台显示「账户异常，资金冻结」。客服要求缴纳解冻费2万元、保证金3万元。","role":"victim","riskTip":"提现受阻+连环收费=100%诈骗"},
      {"id":"e_safe","type":"end","title":"智者不入爱河","content":"当对方提及投资时你立刻警觉并拒绝，拉黑对方并举报账号。","role":"victim"},
      {"id":"e_warn","type":"end","title":"中途醒悟","content":"你投入了5000元后感到不安，搜索发现同类骗局，及时报警止损。","role":"victim"},
      {"id":"e_loss","type":"end","title":"情财两空","content":"你缴纳了解冻费和保证金，但客服仍然要求继续缴费。最终对方失联、平台关闭，你损失13万元。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_warm","condition":"回应对方","label":"接受对方的关心和红包","isSafeChoice":true},
      {"from":"s1","to":"e_safe","condition":"保持距离","label":"对过于完美的人设保持警惕","isSafeChoice":true},
      {"from":"d_warm","to":"d_invest","condition":"关系升温","label":"信任对方并接受邀约","isSafeChoice":false},
      {"from":"d_warm","to":"e_safe","condition":"拒绝投资话题","label":"明确拒绝讨论金钱话题","isSafeChoice":true},
      {"from":"d_invest","to":"d_try","condition":"感兴趣","label":"小额试水投资","isSafeChoice":false},
      {"from":"d_invest","to":"e_safe","condition":"警惕","label":"拒绝投资并终止联系","isSafeChoice":true},
      {"from":"d_try","to":"d_big","condition":"尝到甜头","label":"加码投入更多资金","isSafeChoice":false},
      {"from":"d_try","to":"e_warn","condition":"见好就收","label":"提现后不再投入","isSafeChoice":true},
      {"from":"d_big","to":"d_trap","condition":"提现","label":"尝试提现时遇到问题","isSafeChoice":false},
      {"from":"d_big","to":"e_warn","condition":"产生怀疑","label":"搜索类似骗局案例后报警","isSafeChoice":true},
      {"from":"d_trap","to":"e_loss","condition":"继续缴费","label":"缴纳解冻费和保证金","isSafeChoice":false},
      {"from":"d_trap","to":"e_warn","condition":"及时止损","label":"拒绝缴费并立刻报警","isSafeChoice":true}
    ]
  }' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '情景模拟：杀猪盘情感投资陷阱');

-- 情景模拟2：AI深度伪造·紧急求助
INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '情景模拟：AI换脸诈骗·亲友紧急求助',
  '你突然收到亲友的视频来电，画面中对方满脸伤痕哭诉遭遇车祸急需手术费。测试你能否识破AI深度伪造骗局。',
  17, 5, 'scenario', 60, 40,
  CAST('{"questions":[]}' AS JSON),
  CAST('{
    "name": "AI换脸诈骗紧急求助",
    "description": "诈骗分子利用AI换脸技术伪装亲友进行诈骗",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"突然接到视频电话","content":"你正在上课/上班，手机突然震动，屏幕上显示是你妈妈/爸爸/挚友的微信视频来电。","role":"narrator"},
      {"id":"d_video","type":"dialog","title":"看到求救画面","content":"接通视频后，对方满脸伤痕、声音颤抖地哭诉：「我出车祸了，急需15万手术费，快转给我！」背景隐约能听到医院广播。","role":"victim","riskTip":"AI换脸在快速转头、挥手时会出现画面扭曲或延迟"},
      {"id":"d_doubt","type":"dialog","title":"发现异常","content":"你注意到对方虽然脸和声音非常像，但说话时表情有些不自然，眨眼频率异常，而且从来没有主动挂断视频的迹象。","role":"victim","riskTip":"让对方做特定动作（挥手/转头）验证是否AI换脸"},
      {"id":"d_panic","type":"dialog","title":"被恐慌支配","content":"你看到亲人痛苦的样子非常焦急，对方不停催促：「快点转过来，医生说要立刻手术！」","role":"victim"},
      {"id":"d_verify","type":"dialog","title":"电话核实","content":"你决定先挂断视频，拨打亲人常用的手机号码进行核实。","role":"victim"},
      {"id":"d_voice","type":"dialog","title":"又收到语音消息","content":"刚挂断视频，你又收到「领导」的语音消息：「给我转85万合同保证金，等会补手续，加急处理！」声音和领导一模一样。","role":"narrator","riskTip":"语音克隆仅需30秒样本"},
      {"id":"e_safe","type":"end","title":"识破AI骗局","content":"你通过电话联系到亲人确认对方安然无恙，确认是AI诈骗并报警。","role":"victim"},
      {"id":"e_warn","type":"end","title":"语音骗局醒悟","content":"你转发语音消息给领导本人核实，领导说从没发过，你意识到是语音克隆诈骗。","role":"victim"},
      {"id":"e_loss","type":"end","title":"上当受骗","content":"你在恐慌中向对方提供的账户转账15万元，后联系亲人才发现被骗。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_video","condition":"接通视频","label":"接通亲友的视频来电","isSafeChoice":true},
      {"from":"d_video","to":"d_doubt","condition":"发现异常","label":"留意到画面表情不自然","isSafeChoice":true},
      {"from":"d_video","to":"d_panic","condition":"完全相信","label":"看到画面后完全相信","isSafeChoice":false},
      {"from":"d_doubt","to":"d_verify","condition":"决定核实","label":"挂断视频后打电话核实","isSafeChoice":true},
      {"from":"d_doubt","to":"d_panic","condition":"打消疑虑","label":"对方解释后打消疑虑","isSafeChoice":false},
      {"from":"d_panic","to":"e_loss","condition":"直接转账","label":"立即转账15万元","isSafeChoice":false},
      {"from":"d_panic","to":"d_verify","condition":"临时起意","label":"转账前先打个电话核实","isSafeChoice":true},
      {"from":"d_verify","to":"e_safe","condition":"确认安全","label":"亲人在学校/单位安然无恙","isSafeChoice":true},
      {"from":"d_verify","to":"d_voice","condition":"又收语音","label":"挂断后又收到领导语音消息","isSafeChoice":false},
      {"from":"d_voice","to":"e_warn","condition":"再次核实","label":"打领导办公电话核实","isSafeChoice":true},
      {"from":"d_voice","to":"e_loss","condition":"相信语音","label":"按领导要求转账85万","isSafeChoice":false}
    ]
  }' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '情景模拟：AI换脸诈骗·亲友紧急求助');

-- 情景模拟3：冒充客服·屏幕共享陷阱
INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '情景模拟：冒充客服退款·屏幕共享陷阱',
  '你接到电商客服电话，称商品有质量问题可三倍退款。对方准确说出你的订单信息，要求下载会议APP「指导退款」。测试你能否识破屏幕共享骗局。',
  18, 3, 'scenario', 60, 30,
  CAST('{"questions":[]}' AS JSON),
  CAST('{
    "name": "冒充客服屏幕共享陷阱",
    "description": "诈骗分子冒充客服诱导开启屏幕共享窃取资金",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"接到客服电话","content":"你接到自称XX电商平台客服的电话，对方准确说出你最近购买的订单信息：商品名称、价格、收货地址。","role":"narrator","riskTip":"个人信息已大量泄露，能说出订单信息不等于真客服"},
      {"id":"d_refund","type":"dialog","title":"退款话术","content":"客服说：「您购买的商品被检出质量问题，平台决定三倍退款，需要您配合操作。」态度诚恳专业。","role":"victim"},
      {"id":"d_app","type":"dialog","title":"诱导下载会议APP","content":"客服说退款需通过「云会议」APP指导完成，发送了下载链接和会议号。要求你加入会议并开启屏幕共享。","role":"narrator","riskTip":"任何要求共享屏幕的客服都是诈骗！"},
      {"id":"d_share","type":"dialog","title":"开启屏幕共享","content":"你下载了APP并开启了屏幕共享。对方说：「请在屏幕上操作，我来指导您完成退款。」","role":"victim","riskTip":"共享屏幕时输入的密码和验证码会被实时窃取"},
      {"id":"d_bank","type":"dialog","title":"操作手机银行","content":"在共享屏幕状态下，对方引导你打开手机银行App，称「需要验证您的银行卡状态才能退款」。","role":"victim"},
      {"id":"d_sms","type":"dialog","title":"验证码泄露","content":"你的手机收到一条验证码短信，对方说「请把验证码念给我确认」，你正准备念出验证码。","role":"victim"},
      {"id":"e_safe","type":"end","title":"立刻挂断","content":"你听到共享屏幕要求时果断挂断，并通过官方APP客服核实，确认是诈骗。","role":"victim"},
      {"id":"e_warn","type":"end","title":"中途警觉","content":"你在输入银行卡号时感到不对劲，关闭屏幕共享并拨打了银行客服冻结账户。","role":"victim"},
      {"id":"e_loss","type":"end","title":"卡内资金被洗劫","content":"你念出了验证码，随后收到银行扣款短信，卡内17.6万元被分批转走。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_refund","condition":"听下去","label":"继续听对方介绍","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"挂断","label":"直接挂断并拨打官方客服核实","isSafeChoice":true},
      {"from":"d_refund","to":"d_app","condition":"相信","label":"相信退款说辞","isSafeChoice":false},
      {"from":"d_refund","to":"e_safe","condition":"怀疑","label":"表示自己会去官方APP处理","isSafeChoice":true},
      {"from":"d_app","to":"d_share","condition":"下载APP","label":"下载会议APP并开启屏幕共享","isSafeChoice":false},
      {"from":"d_app","to":"e_safe","condition":"拒绝共享","label":"听到共享屏幕要求立即挂断","isSafeChoice":true},
      {"from":"d_share","to":"d_bank","condition":"配合操作","label":"打开手机银行配合操作","isSafeChoice":false},
      {"from":"d_share","to":"e_warn","condition":"警觉","label":"关闭共享并联系银行","isSafeChoice":true},
      {"from":"d_bank","to":"d_sms","condition":"继续","label":"等待验证码","isSafeChoice":false},
      {"from":"d_bank","to":"e_warn","condition":"犹豫","label":"输入信息时产生怀疑","isSafeChoice":true},
      {"from":"d_sms","to":"e_loss","condition":"念出验证码","label":"将验证码告知对方","isSafeChoice":false},
      {"from":"d_sms","to":"e_warn","condition":"突然醒悟","label":"看到扣款短信立即挂断","isSafeChoice":true}
    ]
  }' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '情景模拟：冒充客服退款·屏幕共享陷阱');

-- 情景模拟4：FaceTime冒充公检法
INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '情景模拟：FaceTime冒充公检法恐吓',
  '你收到FaceTime来电，显示为「XX市公安局」，对方穿着警服称你涉嫌洗钱。测试你在恐惧压力下能否保持理智。',
  19, 5, 'scenario', 60, 40,
  CAST('{"questions":[]}' AS JSON),
  CAST('{
    "name": "FaceTime冒充公检法",
    "description": "诈骗分子通过FaceTime冒充公安机关实施诈骗",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"接到FaceTime来电","content":"你的iPhone突然响起FaceTime视频来电，屏幕上显示「北京市公安局」的号码。","role":"narrator","riskTip":"公检法机关不会通过FaceTime办案，这是诈骗"},
      {"id":"d_answer","type":"dialog","title":"接通视频","content":"你犹豫了一下接通了，画面中对方穿着警服，背景有警徽标志。对方自称李警官，报出你的姓名、身份证号、家庭住址说：「你名下银行卡涉嫌洗钱案」。","role":"victim"},
      {"id":"d_warrant","type":"dialog","title":"收到逮捕令","content":"对方通过微信发来一张「刑事逮捕令」，上面有你的照片、红章和编号。你感到震惊和恐惧。","role":"narrator","riskTip":"真警察不会在网上发送逮捕令"},
      {"id":"d_secret","type":"dialog","title":"要求保密","content":"对方语气严厉：「此案涉密，不准告诉任何人，否则立即逮捕你！也不要挂断电话！」","role":"victim","riskTip":"隔离你与外界联系是诈骗的关键手段"},
      {"id":"d_app","type":"dialog","title":"下载安全APP","content":"对方引导你下载「国家安全防护」APP并安装，声称这是「警方用来清查资金的官方软件」。","role":"narrator","riskTip":"所谓的「安全防护」APP实为远程控制木马"},
      {"id":"d_account","type":"dialog","title":"要求转入安全账户","content":"对方说：「清查资金需要你将名下存款转入国家安全账户，核实无误后48小时内退还。」","role":"victim"},
      {"id":"e_safe","type":"end","title":"挂断并报警","content":"你听到要转账时产生怀疑，挂断电话后拨打110核实，警方确认是诈骗。","role":"victim"},
      {"id":"e_warn","type":"end","title":"向老师求助","content":"你的恐惧中保持了一线理智，偷偷给辅导员发了消息，老师及时赶到劝阻。","role":"victim"},
      {"id":"e_loss","type":"end","title":"被骗转账","content":"你在恐慌中将自己8.5万元存款全部转入所谓的「安全账户」，后才发现被骗。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_answer","condition":"接通视频","label":"接通FaceTime视频","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"挂断","label":"直接挂断并举报","isSafeChoice":true},
      {"from":"d_answer","to":"d_warrant","condition":"相信对方","label":"相信对方民警身份","isSafeChoice":false},
      {"from":"d_answer","to":"e_safe","condition":"质疑","label":"要求去派出所当面核实","isSafeChoice":true},
      {"from":"d_warrant","to":"d_secret","condition":"恐惧","label":"看到逮捕令后感到恐惧","isSafeChoice":false},
      {"from":"d_warrant","to":"e_warn","condition":"半信半疑","label":"偷偷联系老师或家人","isSafeChoice":true},
      {"from":"d_secret","to":"d_app","condition":"听话配合","label":"按要求安装安全APP","isSafeChoice":false},
      {"from":"d_secret","to":"e_warn","condition":"告诉他人","label":"觉得不对劲告诉身边人","isSafeChoice":true},
      {"from":"d_app","to":"d_account","condition":"安装完成","label":"安装后按指示操作","isSafeChoice":false},
      {"from":"d_app","to":"e_warn","condition":"最后怀疑","label":"安装APP后感到不安咨询同学","isSafeChoice":true},
      {"from":"d_account","to":"e_loss","condition":"转账","label":"将存款转入安全账户","isSafeChoice":false},
      {"from":"d_account","to":"e_safe","condition":"醒悟","label":"意识到要转账后挂断报警","isSafeChoice":true}
    ]
  }' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '情景模拟：FaceTime冒充公检法恐吓');

-- 情景模拟5：注销校园贷
INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, status)
SELECT
  '情景模拟：注销校园贷·征信恐吓',
  '你接到「银监会」电话称大学期间的校园贷需注销否则影响征信，并准确说出你的学校和信贷记录。测试你能否识破征信类诈骗。',
  20, 3, 'scenario', 60, 30,
  CAST('{"questions":[]}' AS JSON),
  CAST('{
    "name": "注销校园贷征信恐吓",
    "description": "诈骗分子冒充银监会以注销校园贷影响征信为由诱导借款",
    "startNodeId": "s1",
    "endNodeIds": ["e_safe", "e_warn", "e_loss"],
    "nodes": [
      {"id":"s1","type":"start","title":"接到银监会来电","content":"你接到自称银监会工作人员的电话，准确说出你的姓名、学校、毕业年份和大学期间使用的信贷服务。称现在国家禁止校园贷，需立即注销否则影响征信。","role":"narrator","riskTip":"个人征信由央行统一管理，任何人无权修改"},
      {"id":"d_query","type":"dialog","title":"引导查征信","content":"对方引导你查询个人征信报告，你看到上面确实有大学期间开通的花呗、信用卡记录。","role":"victim","riskTip":"正常的大学生信用卡和花呗记录不等于校园贷"},
      {"id":"d_borrow","type":"dialog","title":"从网贷平台借款","content":"对方称「你的信贷额度需要全部清零才能注销，这些额度是校园贷的授信」，引导你从借呗、微粒贷等平台借款。","role":"victim","riskTip":"任何要求你网贷后转账的都是诈骗"},
      {"id":"d_transfer","type":"dialog","title":"转给清算账户","content":"你从借呗借款5万元，按对方指示转入「银监会清算账户」。对方称核实后会原路退回。","role":"victim"},
      {"id":"d_more","type":"dialog","title":"要求继续借款","content":"对方说：「根据系统检测，你名下的京东金条、美团借钱也有关联额度需要清零。」","role":"narrator","riskTip":"一旦你开始转账，对方会以各种理由要求继续"},
      {"id":"e_safe","type":"end","title":"挂断咨询","content":"你听到影响征信感到紧张，但决定先拨打银行客服或96110咨询，发现是诈骗。","role":"victim"},
      {"id":"e_warn","type":"end","title":"中途醒悟","content":"你转账第一笔后感到不对劲，告诉室友后一起去保卫处咨询，及时报警。","role":"victim"},
      {"id":"e_loss","type":"end","title":"损失惨重","content":"你从多个平台借款累计18万元全部转入骗子账户，催收电话不断打来时你才意识到被骗。","role":"victim"}
    ],
    "edges": [
      {"from":"s1","to":"d_query","condition":"相信","label":"相信对方是银监会工作人员","isSafeChoice":false},
      {"from":"s1","to":"e_safe","condition":"质疑","label":"先拨打银行官方电话咨询","isSafeChoice":true},
      {"from":"d_query","to":"d_borrow","condition":"恐慌","label":"看到记录后信以为真","isSafeChoice":false},
      {"from":"d_query","to":"e_warn","condition":"理性","label":"咨询银行客服关于征信的问题","isSafeChoice":true},
      {"from":"d_borrow","to":"d_transfer","condition":"借款","label":"从网贷平台借款","isSafeChoice":false},
      {"from":"d_borrow","to":"e_warn","condition":"犹豫","label":"对借款行为产生怀疑","isSafeChoice":true},
      {"from":"d_transfer","to":"d_more","condition":"相信","label":"相信转账后会自动退还","isSafeChoice":false},
      {"from":"d_transfer","to":"e_warn","condition":"怀疑","label":"感觉不对劲告诉身边人","isSafeChoice":true},
      {"from":"d_more","to":"e_loss","condition":"继续借款","label":"从更多平台借款继续转账","isSafeChoice":false},
      {"from":"d_more","to":"e_warn","condition":"醒悟","label":"意识到被骗立即报警","isSafeChoice":true}
    ]
  }' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = '情景模拟：注销校园贷·征信恐吓');

-- =====================================================
-- 第七部分：Agent模拟挑战（10种复杂诈骗类型）
-- =====================================================

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：杀猪盘情感投资',
  'AI扮演一位条件优越的异性，通过情感铺垫诱导你参与虚假投资。测试你能否识破情感投资型杀猪盘。',
  201, 5, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"杀猪盘情感投资","scenarioBrief":"对方通过婚恋/社交平台建立情感关系，随后透露高收益投资渠道并诱导你入金。","persona":"投行精英/温柔异性","riskPoints":["打造完美人设","长期情感铺垫","声称内部投资渠道","小额返利诱导大额投入","提现受阻需缴费"],"safeActions":["拒绝网友推荐的投资","不相信稳赚不赔项目","通过官方渠道核实对方身份","涉及金钱立即拉黑并举报"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：杀猪盘情感投资');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：AI换脸亲友求助',
  'AI扮演你熟悉的亲友，通过AI换脸视频哭诉遭遇急事需用钱。测试你能否识破AI深度伪造诈骗。',
  202, 5, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"AI深度伪造","scenarioBrief":"对方利用AI换脸或语音克隆技术冒充你的亲友/领导，以紧急事件为由要求转账。","persona":"视频中的亲友/领导","riskPoints":["视频画面可伪造","声音可克隆","制造紧急恐慌情绪","要求立即转账"],"safeActions":["让对方做挥手/转头等动作测试","挂断后通过常用号码回拨核实","多渠道确认对方身份","绝不按视频要求转账"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：AI换脸亲友求助');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：刷单返利陷阱',
  'AI扮演兼职派单员，以点赞赚钱为饵诱导你下载APP做联单任务。测试你能否拒绝垫资刷单。',
  203, 3, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"刷单返利","scenarioBrief":"对方以轻松兼职、高额返利为诱饵，诱导小额返利后要求垫付大额资金。","persona":"兼职派单员/任务导师","riskPoints":["轻松高薪","先小额返利建立信任","要求垫资做任务","以连单/冻结为由继续转账"],"safeActions":["拒绝垫资刷单","停止转账","保存聊天和转账证据","向110或学校保卫处求助"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：刷单返利陷阱');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：冒充客服屏幕共享',
  'AI扮演电商平台客服，准确说出你的订单信息并诱导开启屏幕共享。测试你能否识破屏幕共享陷阱。',
  204, 4, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"冒充客服屏幕共享","scenarioBrief":"对方准确说出订单信息获取信任，以退款为由诱导下载会议APP并开启屏幕共享。","persona":"电商/快递平台客服","riskPoints":["准确说出订单信息","要求离开官方平台沟通","索要验证码","诱导屏幕共享或转账验证"],"safeActions":["回到官方App核验","拒绝验证码和屏幕共享","不向陌生账户转账","联系平台官方客服"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：冒充客服屏幕共享');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：冒充公检法FaceTime',
  'AI扮演公安民警通过FaceTime来电，以涉嫌洗钱为由要求你配合资金清查。测试你能否在恐惧下保持理智。',
  205, 5, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"冒充公检法","scenarioBrief":"对方通过FaceTime或电话冒充公安民警，以涉案、保密、资金核查为由要求配合转账。","persona":"办案民警/检察机关工作人员","riskPoints":["涉案恐吓","要求绝对保密","发送假文书","要求转入安全账户"],"safeActions":["挂断并拨打110核实","拒绝安全账户转账","告知家人老师","保存证据报警"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：冒充公检法FaceTime');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：虚假网络贷款',
  'AI扮演贷款平台经理，以无抵押秒放款为饵诱导你缴纳解冻费。测试你能否识别虚假贷款平台。',
  206, 4, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"虚假网络贷款","scenarioBrief":"对方以无抵押、低利息、秒放款为诱饵，放款前要求缴纳解冻费、保证金或刷流水。","persona":"贷款平台经理","riskPoints":["低息免审快速放款","放款前收费","刷流水验证还款能力","银行卡填错需解冻"],"safeActions":["拒绝放款前收费","通过正规金融机构申请","不提供验证码和银行卡密码","向银保监/公安渠道核验"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：虚假网络贷款');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：注销校园贷征信恐吓',
  'AI扮演银监会工作人员，以注销校园贷、修复征信为由诱导你贷款转账。测试你能否识破征信类诈骗。',
  207, 4, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"注销校园贷/征信","scenarioBrief":"对方声称你的校园贷或征信存在异常，需要配合注销账户或清空额度。","persona":"金融平台风控客服","riskPoints":["征信异常恐吓","要求下载会议App共享屏幕","诱导贷款提现转账","要求提供银行卡和验证码"],"safeActions":["通过官方金融机构核验","拒绝屏幕共享和远程控制","不贷款转账给个人账户","及时报警并联系银行"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：注销校园贷征信恐吓');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：裸聊敲诈勒索',
  'AI扮演社交平台上的陌生人，诱导你下载APP并视频裸聊，随后用录像和通讯录截图威胁敲诈。测试你如何应对。',
  208, 3, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"裸聊敲诈","scenarioBrief":"对方以暧昧聊天为诱饵，诱导下载木马APP窃取通讯录，录制不雅视频后敲诈勒索。","persona":"社交平台陌生网友","riskPoints":["主动搭讪诱惑","诱导下载非官方APP","要求视频裸聊","窃取通讯录威胁群发"],"safeActions":["不安装陌生APP","拒绝裸聊","不转账保留证据","立即报警处理"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：裸聊敲诈勒索');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：虚假游戏交易',
  'AI扮演游戏买家/平台客服，以低价装备/账号交易为饵诱导你去假平台缴纳保证金。测试你能否识别游戏交易诈骗。',
  209, 2, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"虚假游戏交易","scenarioBrief":"对方联系你买卖账号或装备，诱导进入假交易平台并要求缴纳保证金或解冻费。","persona":"游戏买家/平台客服","riskPoints":["脱离官方平台交易","假平台冻结账户","要求保证金/解冻费","诱导继续充值"],"safeActions":["只走官方交易渠道","拒绝保证金和解冻费","保留聊天截图","向平台官方客服举报"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：虚假游戏交易');

INSERT INTO challenge
  (title, description, level_order, difficulty, type, passing_score, score_reward, content, scripts, agent_config, status)
SELECT
  'Agent模拟：虚拟货币洗钱',
  'AI扮演交易所客服或公检法人员，以账户洗钱风险为由诱导你购买USDT转入安全钱包。测试你能否识破虚拟货币诈骗。',
  210, 5, 'agent_scenario', 75, 100,
  NULL, NULL,
  CAST('{"fraudType":"虚拟货币洗钱","scenarioBrief":"对方以账户涉嫌洗钱需配合调查为由，诱导购买USDT等稳定币转入指定的安全钱包地址。","persona":"交易所客服/办案人员","riskPoints":["谎称账户洗钱风险","制造恐慌情绪","诱导购买虚拟货币","要求转入陌生钱包地址"],"safeActions":["正规交易所不会电话要求转账","不向陌生地址转账虚拟货币","联系官方客服核实","发现被骗立即报警"]}' AS JSON),
  1
WHERE NOT EXISTS (SELECT 1 FROM challenge WHERE title = 'Agent模拟：虚拟货币洗钱');
