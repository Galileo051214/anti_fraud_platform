# 数据库初始化与试运行种子数据

本目录按“空库初始化”和“已有库补丁”分开使用。`init.sql` 会删除并重建 `anti_fraud_platform` 数据库，只能用于本地空库、演示库或确认可重建的试运行库。

## 空库初始化

```bash
mysql -u root -p < sql/init.sql
mysql -u root -p anti_fraud_platform < sql/seed_news.sql
mysql -u root -p anti_fraud_platform < sql/seed_case.sql
mysql -u root -p anti_fraud_platform < sql/seed_challenge.sql
```

`init.sql` 负责建库、建表、视图和基础字典数据，包括管理员账号、资讯分类、案例标签、成就和推荐关联规则。三个 `seed_*.sql` 脚本负责导入校内试运行演示内容，按标题或唯一关系去重，可重复执行。

默认管理员账号：

- 用户名：`admin`
- 密码：`123456`

默认学生账号：

- 用户名：`student001`
- 密码：`123456`
- 学号：`20260001`

## 已有库补丁

已有库不要执行 `init.sql`。按实际缺失项执行补丁脚本：

```bash
mysql -u root -p anti_fraud_platform < sql/patch_user_last_login.sql
mysql -u root -p anti_fraud_platform < sql/patch_challenge_content.sql
mysql -u root -p anti_fraud_platform < sql/patch_scenario_progress.sql
mysql -u root -p anti_fraud_platform < sql/patch_agent_challenge.sql
```

补丁脚本目标：

- `patch_user_last_login.sql`：给 `sys_user` 增加 `last_login_time`，用于连续学习成就统计。
- `patch_challenge_content.sql`：允许 `challenge.content` 为空，支持情景模拟关卡只维护 `scripts`。
- `patch_scenario_progress.sql`：给 `scenario_progress` 增加 `final_score`，用于情景模拟最终得分。
- `patch_agent_challenge.sql`：增加 `agent_scenario` 关卡类型、`challenge.agent_config` 字段、Agent挑战会话表和每日奖励表。

`patch_user_last_login.sql`、`patch_scenario_progress.sql` 和 `patch_agent_challenge.sql` 已做字段/表存在性检查，可重复执行。`patch_challenge_content.sql` 是幂等的列定义调整，可重复执行。

## 验收检查

初始化或补丁执行后，至少确认：

```sql
SELECT username, role, status FROM sys_user WHERE username = 'admin';
SELECT username, role, status FROM sys_user WHERE username = 'student001';
SELECT s.total_score, s.current_level FROM user_score s JOIN sys_user u ON s.user_id = u.id WHERE u.username = 'student001';
SELECT p.lifecycle_stage, p.knowledge_level FROM user_profile p JOIN sys_user u ON p.user_id = u.id WHERE u.username = 'student001';
SELECT COUNT(*) AS news_count FROM news;
SELECT COUNT(*) AS case_count FROM fraud_case;
SELECT COUNT(*) AS challenge_count FROM challenge;
SELECT COUNT(*) AS achievement_count FROM achievement;
```

校内试运行推荐顺序：先用空库流程准备演示数据，再启动后端和前端，使用管理员账号维护内容，使用学生账号完成“登录 -> 浏览资讯/案例 -> 闯关/情景模拟 -> 获得积分/成就 -> 查看推荐/个人中心”的闭环验收。
