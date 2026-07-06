# 项目优化实施与验收报告

生成日期：2026-07-04  
分支：`develop`

## 1. Subagent 执行情况

| Subagent | 类型 | 结果 |
| --- | --- | --- |
| `backend-forum-image-worker` | worker | 完成论坛帖子 `imageUrls` 后端支持、SQL 初始化字段、旧库 patch、后端测试。 |
| `backend-chat-agent-worker` | worker | 完成智能反诈助手简单 agent、websearch 客户端、来源返回、降级、缓存、配置和后端测试。 |
| `acceptance-prep-subagent` | explorer | 完成验收矩阵，覆盖后端、前端、SQL、构建和安全风险。 |
| `frontend-forum-image-worker` | worker | 完成发帖多图上传、预览、提交、列表缩略图、详情图集和管理端展示。 |
| `frontend-chat-agent-worker` | worker | 完成聊天模式切换、最新诈骗汇报参数、来源卡片、风险等级和降级状态展示。 |

## 2. 已完成能力

### 论坛图片

- `forum_post` 增加 `image_urls` JSON 字段。
- 新增 `sql/patch_forum_post_images.sql`，用于旧库增量升级。
- `CreatePostRequest`、`UpdatePostRequest`、`ForumPost`、`PostVO` 支持 `imageUrls`。
- 发帖创建、更新、列表、详情均回传图片 URL。
- 后端限制最多 9 张、URL 非空、最长 500 字符，仅允许本站 `/uploads/images/` 或配置上传域名。
- 前端发帖弹窗支持多图上传、预览、删除、上传中禁用发布。
- 论坛列表展示最多 3 张缩略图，详情页和管理端详情展示图集。

### 智能反诈助手 Agent

- 新增 `AntiFraudAgentService` / `AntiFraudAgentServiceImpl`。
- 新增 `WebSearchClient`，支持 allowed domains、来源去重、Redis 缓存、本地缓存和官方来源降级。
- `WebSearchClient` 已扩展为 `auto / bing / firecrawl / exa / firecrawl-mcp / exa-mcp` 多 provider，Firecrawl/Exa 可通过环境变量配置 API key 后按 MCP 协议或直接 HTTP 获取正文摘录。
- `ChatRequest` 支持 `answerType`、`mode`、`useWebSearch`、`fraudType`、`region`、`timeRange`。
- `ChatVO` 支持 `sources`、`retrievedAt`、`answerType`、`riskLevel`、`fallback`。
- `QAConversation` 持久化 `answerType`、`riskLevel`、`sourcesJson`、`fallback`、`retrievedAt`。
- 新增 `sql/patch_chat_agent.sql`，`sql/init.sql` 同步空库初始化字段。
- 前端聊天页支持“普通咨询 / 最新诈骗汇报”模式，汇报模式可选择诈骗类型、地区、时间范围。
- 助手消息展示来源卡片、检索时间、风险等级和降级状态。

## 3. 验收结果

| 命令 | 结果 |
| --- | --- |
| `cd backend && mvn test` | 通过，148 tests，0 failures，0 errors。 |
| `cd frontend && npm run build` | 通过，仅有 Sass legacy JS API 和 Vite chunk size 警告。 |
| `git diff --check` | 通过，无空白错误。 |
| `rg -n "localhost|123456|填自己的apikey|upload.base-url|websearch|allowed-domains|JWT_SECRET" backend/src/main/resources` | 生产 JWT 使用环境变量；dev 配置保留本地示例密码和 DeepSeek 占位 key；websearch allowed domains 已配置。 |

## 4. 与原计划的差异

- 原计划建议可新增 `qa_conversation_source` 和 `agent_tool_call` 独立审计表。当前实现采用 MVP 方式，将来源保存到 `qa_conversation.sources_json`，并未新增独立工具轨迹表。
- 前端验收目前以 `npm run build` 和手工流程为主，项目仍没有前端单元或端到端测试框架。
- `FileController` 仍主要校验 MIME、扩展名、大小和路径，尚未增加真实文件魔数校验。

## 5. 后续建议

1. 试运行前配置生产 `upload.base-url`、`WEBSEARCH_PROVIDER`、`FIRECRAWL_API_KEY` 或 `EXA_API_KEY`、`WEBSEARCH_ALLOWED_DOMAINS` 和 `JWT_SECRET`。
2. 如果需要更强审计，再把 `sources_json` 演进为 `qa_conversation_source`，并增加 `agent_tool_call`。
3. 增加前端 e2e 验收：发帖上传图片、查看详情图集、生成最新诈骗汇报、点击来源链接。
4. 增加上传文件魔数校验和孤儿图片清理策略。
