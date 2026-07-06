# 项目优化与 Subagent 执行文档

生成日期：2026-07-04  
适用仓库：`E:\2026work\4547springboot`  
当前分支：`develop`

## 1. 本轮阅读方式

本轮使用 4 个只读 subagent 并行阅读项目：

| Subagent | 阅读范围 | 关键结论 |
| --- | --- | --- |
| 后端论坛与上传 | `ForumController`、`FileController`、`ForumPostServiceImpl`、论坛 DTO/VO/实体、`ForumPostMapper.xml`、`sql/init.sql` | 已有独立图片上传接口，但帖子模型没有图片字段或附件表。 |
| 前端论坛与上传 | `frontend/src/views/forum/*`、`frontend/src/api/forum.ts`、`frontend/src/api/upload.ts`、管理端论坛页 | 前端已有 `uploadImage(file)` 封装，但发布弹窗、列表和详情都未接入图片。 |
| 智能助手 | `ChatController`、`QAConversationServiceImpl`、`DeepSeekClient`、Prompt、聊天前端 | 当前是普通 LLM 问答，不是 agent；没有 websearch、来源引用、工具调用轨迹。 |
| 架构与任务拆解 | `pom.xml`、`package.json`、SQL、测试、现有文档 | 后端测试基础较好，前端缺少测试；后续任务适合按文件所有权拆给 worker subagent。 |

## 2. 项目现状速览

项目是前后端分离的反诈骗学习平台。

后端：Spring Boot 3.2.3、Java 17、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、SpringDoc、EasyExcel、Hutool/HttpClient。主要代码位于 `backend/src/main/java/com/anti`，按 `controller/service/mapper/entity/dto/vo/config/security/task/util` 分层。

前端：Vue 3、TypeScript、Vite 5、Element Plus、Pinia、Vue Router、Axios、ECharts。主要代码位于 `frontend/src`，接口封装在 `frontend/src/api`，页面在 `frontend/src/views`。

数据：`sql/init.sql` 会重建 `anti_fraud_platform`，覆盖用户、资讯、案例、闯关、社区、QA 会话、推荐和统计模块。`sql/README.md` 说明了 init、seed、patch 脚本用途。

测试：后端已有 JUnit/Mockito 测试，覆盖上传、论坛、聊天、权限和多个 service。前端 `package.json` 只有 `dev/build/preview/lint/format`，未发现前端测试框架配置。

## 3. 优化总目标

本阶段目标是把项目从“功能演示完整”推进到“可继续由多个 subagent 分工优化”的状态。优先优化方向：

1. 论坛发帖支持附加图片，包含上传、保存、展示、管理和测试。
2. 智能反诈助手升级为简单 agent，能按需 websearch 最新诈骗手法和反诈建议，并生成带来源的汇报。
3. 补齐数据迁移、构建验收、配置安全、审计和测试基线，让后续 subagent 改动可验证。

## 4. 方向一：发帖附加图片

### 4.1 当前链路

后端当前能力：

- `POST /api/file/upload` 位于 `backend/src/main/java/com/anti/controller/FileController.java`，字段名为 `file`，要求登录。
- 上传仅允许 `image/jpeg`、`image/png`、`image/gif`、`image/webp`，扩展名限制为 `.jpg/.jpeg/.png/.gif/.webp`，最大 10MB。
- 文件保存到 `upload.path/images/yyyyMMdd/uuid.ext`，返回 `upload.base-url + /uploads/images/...`。
- `/uploads/**` 由 `WebMvcConfig` 映射为静态资源，并在 `SecurityConfig` 中匿名可读。

论坛当前能力：

- `CreatePostRequest`、`UpdatePostRequest`、`ForumPost`、`PostVO` 都没有图片字段。
- `forum_post` 表只有 `title/content/post_type/tag_ids` 和计数字段，没有 `image_urls` 或附件表。
- `ForumPostServiceImpl#createPost` 只保存标题、正文、类型、标签。
- `frontend/src/api/upload.ts` 已有 `uploadImage(file): Promise<string>`。
- `frontend/src/views/forum/index.vue` 的发布弹窗只有类型、标题、正文。
- `frontend/src/views/forum/detail.vue` 只展示纯文本正文。

### 4.2 推荐方案

采用两步式 MVP：

1. 前端先调用 `POST /api/file/upload` 上传图片，拿到 URL。
2. 创建或更新帖子时，在 JSON 请求体中提交 `imageUrls: string[]`。
3. 后端将 `imageUrls` 保存到 `forum_post.image_urls JSON`。

推荐先用 JSON 字段实现，原因是当前 `tag_ids` 已使用 JSON 和 `JacksonTypeHandler`，改动面小，适合课程项目和短期试运行。

长期如果需要图片审核、排序、删除文件治理、图片元数据、违规记录，建议演进为独立表：

```sql
forum_post_image(
  id,
  post_id,
  image_url,
  sort_order,
  create_time
)
```

### 4.3 后端改造任务

建议 worker：`backend-forum-image-worker`

文件所有权：

- `sql/init.sql`
- 新增 `sql/patch_forum_post_images.sql`
- `backend/src/main/java/com/anti/entity/ForumPost.java`
- `backend/src/main/java/com/anti/entity/dto/CreatePostRequest.java`
- `backend/src/main/java/com/anti/entity/dto/UpdatePostRequest.java`
- `backend/src/main/java/com/anti/entity/vo/PostVO.java`
- `backend/src/main/java/com/anti/service/impl/ForumPostServiceImpl.java`
- `backend/src/test/java/com/anti/service/impl/ForumPostServiceImplTest.java`
- `backend/src/test/java/com/anti/entity/dto/RequestValidationTest.java`

建议实现：

- `forum_post` 增加 `image_urls JSON NULL COMMENT '帖子图片URL数组'`。
- `ForumPost` 增加 `@TableField(typeHandler = JacksonTypeHandler.class) private List<String> imageUrls;`。
- `CreatePostRequest` 增加 `private List<String> imageUrls;`。
- `UpdatePostRequest` 增加 `private List<String> imageUrls;`，约定 `null` 表示不更新，空数组表示清空图片。
- `PostVO` 增加 `private List<String> imageUrls;`。
- `ForumPostServiceImpl#createPost/updatePost/convertToPostVO` 处理图片字段。
- 增加 `normalizeImageUrls` 私有方法：限制最多 9 张，每个 URL 最长 500，去重，拒绝空字符串，拒绝非本站上传 URL。

接口契约：

```json
POST /api/forum/post
{
  "title": "防骗经验",
  "content": "正文",
  "postType": "experience",
  "tagIds": [1, 2],
  "imageUrls": [
    "http://localhost:8080/uploads/images/20260704/example.png"
  ]
}
```

验收标准：

- 无图发帖保持兼容。
- 带图发帖后，列表和详情接口都返回 `imageUrls`。
- 更新帖子时 `imageUrls=null` 不改图片，`imageUrls=[]` 清空图片。
- 非作者不能更新图片。
- 非 `/uploads/images/` 或非配置上传域名的 URL 被拒绝。
- 旧数据 `image_urls IS NULL` 时不会报错。
- `mvn test -Dtest=ForumPostServiceImplTest,RequestValidationTest` 通过。

### 4.4 前端改造任务

建议 worker：`frontend-forum-image-worker`

文件所有权：

- `frontend/src/api/forum.ts`
- `frontend/src/api/upload.ts`
- `frontend/src/views/forum/index.vue`
- `frontend/src/views/forum/detail.vue`
- `frontend/src/views/admin/forum.vue`

建议实现：

- `PostVO`、`CreatePostRequest`、`UpdatePostRequest` 增加 `imageUrls?: string[]`。
- 发布弹窗在正文下方增加图片上传区。
- 引入 `uploadImage`，选择文件后逐个上传。
- 增加状态：`publishImages`、`uploadingImages`、`uploadErrors`。
- 上传中禁用发布按钮。
- 预览区支持缩略图、删除、失败提示。
- 列表卡片展示首图或最多 3 张缩略图。
- 详情页展示多图网格，支持点击预览，图片 `loading="lazy"`。
- 管理端帖子详情弹窗展示图片。
- 前端校验和后端保持一致：最多 9 张，单张最大 10MB，类型为图片。

验收标准：

- 发布无图帖子不回归。
- 上传非图片或超限图片会被拦截。
- 上传失败有明确提示，不会静默丢图。
- 上传中不能提交帖子。
- 发布后刷新列表、详情、管理端弹窗均可看到图片。
- 删除预览图后，请求体不再包含该 URL。
- `npm run build` 通过。

### 4.5 已知风险

- 两步上传会产生孤儿图片，例如上传成功后取消发帖。MVP 可接受，后续加清理任务或临时上传表。
- `/uploads/**` 匿名可读，论坛图片默认公开，需要产品侧确认。
- `FileController` 当前校验 MIME 和扩展名，但未校验真实文件签名，后续可补魔数校验。
- 生产环境必须配置 `upload.base-url`，避免返回 `localhost`。

## 5. 方向二：智能反诈助手升级为简单 Agent

### 5.1 当前链路

当前聊天链路：

1. 前端 `frontend/src/views/chat/index.vue#sendMessage` 追加用户消息。
2. `frontend/src/api/chat.ts#askQuestion` 请求 `POST /api/chat/ask`。
3. `ChatController#ask` 校验登录用户。
4. `QAConversationServiceImpl#askQuestion` 校验问题和 session，读取最近 5 组完整问答。
5. `AntiFraudPromptTemplate#getSystemPrompt` 提供静态系统提示词。
6. `DeepSeekClient#chat` 调用 DeepSeek。
7. `qa_conversation` 保存 `question/answer/model/tokens_used/feedback/create_time`。

当前边界：

- 不是 agent，没有意图路由、工具调用、websearch、RAG、来源引用和工具轨迹。
- `createNewSession()` 只返回 ID，不落库，空会话不会出现在会话列表。
- 历史恢复时不返回 `feedback`，且固定 `fallback=false`。
- 反馈按 `sessionId` 更新最后一条记录，不是按具体消息 ID。
- Redis 已配置，但聊天链路未用于缓存。

### 5.2 Agent MVP 目标

把聊天助手升级为轻量 agent，不引入复杂编排框架，先实现：

- 意图识别：普通问答、风险评估、最新诈骗汇报、紧急处置建议。
- Websearch 工具：只在需要“最新”或“近期趋势”时调用。
- 来源白名单：优先官方和可信来源。
- 生成汇报：基于检索结果生成摘要、典型话术、受害人群、防范动作、官方建议和来源。
- 可审计：保存工具调用、来源、耗时、降级原因。
- 可降级：搜索失败或模型失败时，返回明确降级信息，不泄露异常细节。

### 5.3 权威来源建议

websearch 不应让模型任意引用网页。建议先配置来源白名单：

- 公安部反诈与网络安全动态：`mps.gov.cn`
- 公安部新闻发布会和防诈手册相关信息：`mps.gov.cn`
- FTC Consumer Alerts：`consumer.ftc.gov/consumer-alerts`
- FTC Scams：`consumer.ftc.gov/scams`
- FBI Common Frauds and Scams：`fbi.gov/how-we-can-help-you/scams-and-safety`
- IC3：`ic3.gov`

本轮查到的可参考入口：

- [公安部反诈动态栏目](https://www.mps.gov.cn/n2255079/n4876594/n5104076/n5104077/index.html)
- [公安部发布会：通报当前电信网络诈骗犯罪的最新形势](https://www.mps.gov.cn/n2254536/n2254544/n2254552/n10491781/index.html)
- [FTC Consumer Alerts](https://consumer.ftc.gov/consumer-alerts)
- [FTC Scams](https://consumer.ftc.gov/scams)
- [FBI Common Frauds and Scams](https://www.fbi.gov/how-we-can-help-you/scams-and-safety/common-frauds-and-scams)
- [IC3](https://www.ic3.gov/)

### 5.4 后端 Agent 改造任务

建议 worker：`backend-chat-agent-worker`

文件所有权：

- `backend/src/main/java/com/anti/controller/ChatController.java`
- `backend/src/main/java/com/anti/service/QAConversationService.java`
- `backend/src/main/java/com/anti/service/impl/QAConversationServiceImpl.java`
- 新增 `backend/src/main/java/com/anti/service/AntiFraudAgentService.java`
- 新增 `backend/src/main/java/com/anti/service/impl/AntiFraudAgentServiceImpl.java`
- 新增 `backend/src/main/java/com/anti/util/WebSearchClient.java`
- `backend/src/main/java/com/anti/util/DeepSeekClient.java`
- `backend/src/main/java/com/anti/util/AntiFraudPromptTemplate.java`
- `backend/src/main/java/com/anti/entity/dto/ChatRequest.java`
- `backend/src/main/java/com/anti/entity/vo/ChatVO.java`
- 新增 `backend/src/main/java/com/anti/entity/vo/SourceVO.java`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application-prod.yml`
- `sql/init.sql`
- 新增 `sql/patch_chat_agent.sql`

建议流程：

1. `ChatRequest` 增加 `mode`、`useWebSearch`、`fraudType`、`region`、`timeRange`。
2. `AntiFraudAgentService` 根据用户输入路由到 `DIRECT_QA`、`RISK_ASSESSMENT`、`LATEST_FRAUD_REPORT`、`EMERGENCY_GUIDANCE`。
3. `LATEST_FRAUD_REPORT` 触发 `WebSearchClient`。
4. `WebSearchClient` 通过搜索 API 或可配置 provider 查询，返回 `title/url/snippet/source/publishedAt/retrievedAt`。
5. `FraudIntelService` 或 agent service 做去重、域名白名单、结果数量限制、Redis 缓存。
6. 将搜索摘要作为上下文传给 DeepSeek，Prompt 要求“只基于给定来源生成最新汇报”。
7. `ChatVO` 返回 `answerType/riskLevel/sources/retrievedAt/toolTraceId/latencyMs/fallback/feedback`。
8. 保存主问答记录、来源记录和工具调用轨迹。

建议新增表：

```sql
ALTER TABLE qa_conversation
  ADD COLUMN answer_type VARCHAR(40) NULL COMMENT '回答类型',
  ADD COLUMN risk_level VARCHAR(20) NULL COMMENT '风险等级',
  ADD COLUMN source_count INT NOT NULL DEFAULT 0 COMMENT '来源数量',
  ADD COLUMN tool_trace_id VARCHAR(64) NULL COMMENT '工具调用追踪ID',
  ADD COLUMN prompt_version VARCHAR(40) NULL COMMENT 'Prompt版本',
  ADD COLUMN status VARCHAR(30) NULL COMMENT '处理状态',
  ADD COLUMN error_code VARCHAR(50) NULL COMMENT '降级或错误码',
  ADD COLUMN latency_ms INT NULL COMMENT '处理耗时';

CREATE TABLE qa_conversation_source (
  id BIGINT NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  title VARCHAR(300) NOT NULL,
  url VARCHAR(1000) NOT NULL,
  source VARCHAR(120) NULL,
  snippet TEXT NULL,
  published_at DATETIME NULL,
  retrieved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_conversation_id (conversation_id)
);

CREATE TABLE agent_tool_call (
  id BIGINT NOT NULL AUTO_INCREMENT,
  trace_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(50) NOT NULL,
  tool_name VARCHAR(80) NOT NULL,
  request_summary VARCHAR(500) NULL,
  status VARCHAR(30) NOT NULL,
  error_code VARCHAR(50) NULL,
  latency_ms INT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_trace_id (trace_id),
  KEY idx_user_session (user_id, session_id)
);
```

配置建议：

```yaml
websearch:
  api-url: ${WEBSEARCH_API_URL:}
  api-key: ${WEBSEARCH_API_KEY:}
  max-results: ${WEBSEARCH_MAX_RESULTS:5}
  timeout-ms: ${WEBSEARCH_TIMEOUT_MS:8000}
  cache-ttl-seconds: ${WEBSEARCH_CACHE_TTL_SECONDS:21600}
  allowed-domains:
    - mps.gov.cn
    - consumer.ftc.gov
    - fbi.gov
    - ic3.gov
agent:
  search-enabled: ${AGENT_SEARCH_ENABLED:true}
  daily-search-limit-per-user: ${AGENT_DAILY_SEARCH_LIMIT_PER_USER:30}
```

验收标准：

- 普通问答不触发 websearch。
- 用户询问“最近/最新/近期诈骗手法”会触发 websearch。
- 搜索失败时返回降级回答，并记录 `error_code`。
- 回答包含检索时间和来源列表。
- 来源 URL 只来自 allowed domains。
- 历史接口能返回 `fallback/feedback/sources`。
- session 越权访问仍被拒绝。
- `mvn test -Dtest=QAConversationServiceImplTest,ChatControllerAuthTest,RequestValidationTest` 通过。

### 5.5 前端 Chat 改造任务

建议 worker：`frontend-chat-agent-worker`

文件所有权：

- `frontend/src/api/chat.ts`
- `frontend/src/views/chat/index.vue`

建议实现：

- `ChatRequest` 增加 `mode/useWebSearch/fraudType/region/timeRange`。
- `ChatVO` 增加 `answerType/riskLevel/sources/retrievedAt/toolTraceId/latencyMs/feedback`。
- 聊天顶部增加模式切换：普通咨询、最新诈骗汇报。
- 汇报模式显示诈骗类型、地区、时间范围输入。
- 消息区域显示来源卡片、检索时间、风险等级。
- 保持 `formatContent()` 先转义再渲染，来源标题、摘要和 URL 也必须安全渲染。
- 后端未配置 websearch 时，前端展示“检索不可用，已使用基础模型回答”的降级状态。

验收标准：

- 普通咨询流程不回归。
- 汇报模式能展示“检索中/分析中/生成中”或至少 loading 状态。
- 带来源回答能展示来源列表，点击外链新窗口打开。
- 空来源、搜索失败、AI 降级都有明确状态。
- `npm run build` 通过。

### 5.6 Agent 安全风险

websearch 会引入新的风险，必须在 MVP 中就设置边界：

- Prompt injection：不要把网页正文无过滤地交给模型，先摘要或只传标题、摘要、来源。
- SSRF：后端不应抓取任意用户提供 URL，只调用搜索 provider 和 allowed domains。
- 来源投毒：只允许可信域名，记录来源和检索时间。
- 隐私外传：搜索 query 前脱敏手机号、身份证、银行卡、精确地址。
- 成本攻击：每用户每日搜索次数、token、超时和最大结果数要有限制。
- XSS：前端渲染来源标题、摘要、回答内容前必须转义。

## 6. 后续 Subagent 派发顺序

推荐按依赖顺序派发，不要让多个 worker 同时编辑同一批文件。

| 顺序 | Worker | 任务 | 可并行性 |
| --- | --- | --- | --- |
| 1 | `backend-forum-image-worker` | 后端帖子图片字段、校验、SQL 和测试 | 先做，阻塞前端论坛图片 |
| 2 | `frontend-forum-image-worker` | 发布弹窗上传、列表缩略图、详情图集、管理端展示 | 依赖后端接口契约 |
| 3 | `backend-chat-agent-worker` | Agent service、websearch、来源模型、配置、审计和测试 | 可与论坛前端并行 |
| 4 | `frontend-chat-agent-worker` | 聊天模式切换、来源卡片、汇报参数、降级状态 | 依赖 `ChatVO` 契约 |
| 5 | `sql-seed-test-worker` | `seed_forum.sql`、agent 演示数据、SQL README、构建命令记录 | 依赖论坛图片和 agent 数据模型 |
| 6 | `security-review-worker` | 上传真实文件签名、生产配置、websearch allowed domains、限流和脱敏 | 可在主要功能完成后集中审查 |

## 7. 可直接复制的 Subagent Prompt

### 7.1 后端论坛图片 Worker

```text
你是 backend-forum-image-worker。仓库路径：E:\2026work\4547springboot。你不是唯一修改者，不要回退他人改动。负责文件：sql/init.sql、新增 sql/patch_forum_post_images.sql、ForumPost.java、CreatePostRequest.java、UpdatePostRequest.java、PostVO.java、ForumPostServiceImpl.java、ForumPostServiceImplTest.java、RequestValidationTest.java。目标：实现论坛帖子 imageUrls JSON 字段，支持创建、更新、列表和详情回传，校验最多 9 张、URL 非空、长度限制、只允许本站 /uploads/images/ 或配置上传域名。更新语义：null 不变，[] 清空。完成后运行相关 Maven 测试，最终列出改动文件和测试结果。
```

### 7.2 前端论坛图片 Worker

```text
你是 frontend-forum-image-worker。仓库路径：E:\2026work\4547springboot。你不是唯一修改者，不要回退他人改动。负责文件：frontend/src/api/forum.ts、frontend/src/api/upload.ts、frontend/src/views/forum/index.vue、frontend/src/views/forum/detail.vue、frontend/src/views/admin/forum.vue。目标：接入论坛发帖多图上传，支持上传、预览、删除、提交 imageUrls，列表展示缩略图，详情展示图集，管理端详情展示图片。上传中禁止发布，失败有提示，最多 9 张，单张 10MB。完成后运行 npm run build，最终列出改动文件和测试结果。
```

### 7.3 后端 Chat Agent Worker

```text
你是 backend-chat-agent-worker。仓库路径：E:\2026work\4547springboot。你不是唯一修改者，不要回退他人改动。负责 ChatController、QAConversationService/Impl、DeepSeekClient、AntiFraudPromptTemplate、ChatRequest、ChatVO、新增 SourceVO、新增 AntiFraudAgentService/Impl、新增 WebSearchClient、application-dev/prod.yml、sql/init.sql、新增 sql/patch_chat_agent.sql 和聊天相关测试。目标：把智能反诈助手升级为简单 agent，支持普通问答和最新诈骗汇报；最新汇报触发 websearch，使用 allowed domains、Redis 缓存、来源去重和降级；ChatVO 返回 sources/retrievedAt/answerType/riskLevel/fallback。完成后运行相关 Maven 测试，最终列出改动文件和测试结果。
```

### 7.4 前端 Chat Agent Worker

```text
你是 frontend-chat-agent-worker。仓库路径：E:\2026work\4547springboot。你不是唯一修改者，不要回退他人改动。负责 frontend/src/api/chat.ts 和 frontend/src/views/chat/index.vue。目标：适配后端 ChatVO sources/retrievedAt/answerType/riskLevel/fallback；聊天页增加普通咨询和最新诈骗汇报模式，汇报模式支持诈骗类型、地区、时间范围，展示来源卡片、检索时间、降级状态。保持回答和来源内容安全渲染。完成后运行 npm run build，最终列出改动文件和测试结果。
```

## 8. 全局验收清单

后端：

- `cd backend && mvn test`
- `cd backend && mvn -DskipTests compile`
- 空库执行 `sql/init.sql` 成功。
- 旧库执行新增 patch 脚本成功。

前端：

- `cd frontend && npm run build`
- 发帖、图片上传、列表、详情、管理端查看主流程可用。
- 聊天普通问答、最新诈骗汇报、来源展示、降级状态可用。

安全：

- 未登录不能上传图片。
- 上传文件不能越权写入目录。
- websearch 只访问 allowed domains 或搜索 provider。
- 搜索 query 和日志不包含未脱敏敏感信息。
- 生产配置不使用 `application-dev.yml` 示例密钥和 `localhost` 上传域名。

## 9. 需要后续决策的问题

1. 论坛图片 MVP 是否接受 `forum_post.image_urls JSON`，还是直接使用 `forum_post_image` 独立表。
2. 论坛图片是否需要删除物理文件，还是只删除帖子引用。
3. websearch provider 选择：商业搜索 API、自建爬取白名单站点，或先用手动维护的反诈情报源。
4. 最新诈骗汇报是否只面向管理员，还是普通学生也可使用。
5. 是否需要 SSE 流式输出。MVP 可以先保留同步 `/chat/ask`，降低改造复杂度。
