# 反诈骗学习平台 Code Wiki

> 本文档为反诈骗学习平台（anti-fraud-platform）的完整代码知识库，涵盖项目整体架构、模块职责、关键类与函数说明、依赖关系以及项目运行方式。

---

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术栈](#2-技术栈)
- [3. 项目目录结构](#3-项目目录结构)
- [4. 整体架构](#4-整体架构)
- [5. 后端模块详解](#5-后端模块详解)
  - [5.1 启动入口与配置](#51-启动入口与配置)
  - [5.2 公共基础设施（common）](#52-公共基础设施common)
  - [5.3 安全模块（security）](#53-安全模块security)
  - [5.4 配置类（config）](#54-配置类config)
  - [5.5 工具类（util）](#55-工具类util)
  - [5.6 业务模块（controller / service）](#56-业务模块controller--service)
  - [5.7 定时任务（task）](#57-定时任务task)
  - [5.8 数据访问与实体层（mapper / entity）](#58-数据访问与实体层mapper--entity)
- [6. 前端架构](#6-前端架构)
- [7. 数据库设计](#7-数据库设计)
- [8. 依赖关系](#8-依赖关系)
- [9. 缓存策略](#9-缓存策略)
- [10. 关键算法说明](#10-关键算法说明)
- [11. 项目运行方式](#11-项目运行方式)
- [12. 注意事项与已知问题](#12-注意事项与已知问题)

---

## 1. 项目概述

**反诈骗学习平台**是一个面向高校学生（以大学生为主）的反诈骗教育学习系统，采用前后端分离架构，集成了资讯学习、案例展示、知识闯关、情景模拟、社区互动、AI 智能客服、个性化推荐、数据统计等核心功能。

平台通过游戏化学习机制（积分、等级、成就、排行榜）激励学生主动学习反诈知识，并基于用户行为画像与生命周期阶段（新手 / 成长 / 成熟）提供差异化的个性化推荐。AI 模块接入 DeepSeek 大模型提供反诈智能问答，推荐模块融合了内容推荐（TF-IDF + 余弦相似度）、协同过滤（皮尔逊相关系数）、热度排序（Wilson 置信区间）与规则挖掘（SPM 序列模式）四种策略。

**角色划分：**
- **学生**：浏览资讯 / 案例、闯关答题、情景模拟、社区发帖评论、AI 问答、查看个人积分 / 成就 / 排行榜、接收个性化推荐
- **管理员**：内容维护（资讯 / 案例 / 关卡 / 帖子）、用户管理、数据看板、统计导出、推荐规则管理

---

## 2. 技术栈

### 后端
| 分类 | 技术 / 版本 |
|---|---|
| 语言 / 运行时 | Java 17 |
| 框架 | Spring Boot 3.2.3 |
| 安全 | Spring Security + JJWT 0.12.5（无状态 JWT） |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL（utf8mb4） |
| 缓存 | Spring Data Redis + Lettuce 连接池 |
| API 文档 | springdoc-openapi 2.3.0（Swagger UI） |
| AI | DeepSeek Chat API（HttpClient5 / Hutool） |
| 工具库 | Hutool 5.8.25、Apache Commons Lang3、Lombok |
| 导出 | EasyExcel 3.3.3 |
| 构建 | Maven |

### 前端
| 分类 | 技术 / 版本 |
|---|---|
| 框架 | Vue 3.4 + TypeScript 5.4 |
| 构建 | Vite 5.2 |
| 路由 | Vue Router 4.3 |
| 状态管理 | Pinia 2.1 |
| UI 组件 | Element Plus 2.7 + @element-plus/icons-vue |
| HTTP | Axios 1.6 |
| 图表 | ECharts 5.5 |
| 其他 | dayjs、lodash-es、nprogress、cropperjs（头像裁剪）、html2canvas + jspdf（导出） |
| 自动导入 | unplugin-auto-import、unplugin-vue-components |

---

## 3. 项目目录结构

```
anti_fraud_platform/
├── backend/                          # 后端 Spring Boot 工程
│   ├── pom.xml                       # Maven 依赖与构建配置
│   └── src/main/
│       ├── java/com/anti/
│       │   ├── AntiFraudPlatformApplication.java   # 启动入口
│       │   ├── common/               # 公共：Result/BusinessException/全局异常/缓存常量
│       │   ├── config/               # 配置：Security/Cors/Redis/MyBatisPlus/WebMvc...
│       │   ├── controller/           # 19 个 REST 控制器
│       │   ├── entity/               # 实体 + dto/ + vo/
│       │   ├── mapper/               # MyBatis-Plus Mapper 接口
│       │   ├── security/             # JWT 过滤器/工具/入口点/登录用户
│       │   ├── service/              # 服务接口 + impl/ 实现
│       │   ├── task/                 # 定时任务（推荐/统计）
│       │   └── util/                 # 工具类（DeepSeek/推荐算法/Redis/脱敏/Prompt）
│       └── resources/
│           ├── mapper/*.xml          # MyBatis XML 映射
│           ├── application.yml       # 主配置
│           ├── application-dev.yml   # 开发环境
│           └── application-prod.yml  # 生产环境
├── frontend/                         # 前端 Vue3 + Vite 工程
│   ├── package.json / vite.config.ts / tsconfig.json
│   └── src/
│       ├── main.ts / App.vue         # 入口
│       ├── router/index.ts           # 路由表 + 全局守卫
│       ├── stores/                   # Pinia（user/profile/score/challenge）
│       ├── api/                      # Axios 接口封装
│       ├── utils/request.ts          # Axios 实例 + 拦截器
│       ├── layout/                   # 学生端 / 管理端布局
│       ├── views/                    # 页面（home/news/case/challenge/forum/chat/...）
│       ├── components/               # 组件（icons/profile/admin）
│       ├── types/                    # TS 类型定义
│       └── styles/                   # 全局样式
├── sql/                              # 数据库脚本
│   ├── init.sql                      # 建库建表 + 初始化数据 + 视图
│   ├── seed_case.sql / seed_news.sql / seed_challenge.sql
│   └── patch_*.sql                   # 增量补丁脚本
├── docx/                             # 设计文档与图表（drawio/png/docx）
└── 技术文档.md                        # 既有技术文档
```

---

## 4. 整体架构

### 4.1 分层架构

系统采用经典的前后端分离 + 后端分层架构：

```
┌──────────────────────────────────────────────────────────┐
│  前端 Vue3 SPA（端口 5173）                                │
│  View → Store → API(request.ts) ──┐                       │
└────────────────────────────────────┼──────────────────────┘
                                     │ /api  (Vite proxy → 8888)
┌────────────────────────────────────▼──────────────────────┐
│  Controller 层（REST API，@RestController）                │
│  参数校验 / 鉴权 / 调用 Service / 包装 Result              │
├──────────────────────────────────────────────────────────┤
│  Service 层（接口 + Impl）                                 │
│  业务逻辑 / 事务 / 调度多 Mapper / 触发积分成就排行榜       │
├──────────────────────────────────────────────────────────┤
│  Mapper 层（MyBatis-Plus BaseMapper + XML）               │
│  数据持久化                                                │
├──────────────────────────────────────────────────────────┤
│  MySQL  +  Redis（缓存/会话/Token黑名单/分布式锁）         │
└──────────────────────────────────────────────────────────┘
        ▲                              ▲
        │                              │
   DeepSeek API（AI 问答）      定时任务（推荐相似度/统计聚合）
```

### 4.2 请求处理流程

1. 前端 `utils/request.ts` 创建 Axios 实例，`baseURL=/api`，请求拦截器自动注入 `Authorization: Bearer <token>`。
2. Vite 开发服务器将 `/api` 代理到后端 `http://localhost:8888`。
3. 后端 `JwtAuthenticationFilter`（继承 `OncePerRequestFilter`）在 `UsernamePasswordAuthenticationFilter` 之前执行：
   - 提取 `Authorization` 头中的 JWT；
   - 校验 Redis Token 黑名单（`token:blacklist:<jwt>`）；
   - 调用 `JwtUtils.validateToken` 校验签名与过期；
   - 构建 `LoginUser` 主体检入 `SecurityContextHolder`，授予 `ROLE_<ROLE>` 权限。
4. `SecurityFilterChain` 按规则放行（注册/登录/Swagger/静态资源）、要求 ADMIN 角色（`/api/admin/**`、用户管理）或要求已认证。
5. 进入 Controller → Service（事务）→ Mapper → MySQL；命中 Redis 缓存的直接返回。
6. 统一异常处理 `GlobalExceptionHandler` 捕获 `BusinessException` / 校验异常 / 未知异常，转换为统一 `Result` JSON。
7. 响应拦截器按 `code` 处理：200 返回数据、401 清除登录态跳转登录、403/404/500 弹出提示。

### 4.3 统一响应格式

所有接口返回 `com.anti.common.Result<T>`：

```json
{ "code": 200, "message": "操作成功", "data": { ... } }
```

`code` 语义：`200` 成功、`400` 业务/参数异常、`401` 未授权、`403` 无权限、`500` 系统异常。

### 4.4 认证授权流程

```
注册   → POST /api/user/register  → UserServiceImpl.register（事务：建用户+积分+画像）
登录   → POST /api/user/login     → 校验账号密码(BCrypt) → JwtUtils.generateToken(username, role, userId)
                                  → 更新 lastLoginTime → 检查登录成就/连续学习
                                  → 返回 { token, userId, role, ... }
登出   → POST /api/user/logout    → 将 JWT 写入 Redis 黑名单（剩余 TTL）
鉴权   → 每次请求 JwtAuthenticationFilter 校验签名 + 黑名单
未授权 → JwtAuthenticationEntryPoint 返回 401 JSON { code, message, path }
```

> 注意：`LoginUser.getUsername()` 实际返回 **userId 字符串**（便于 Controller 通过 `Authentication#getName()` 取 userId），非用户名。

---

## 5. 后端模块详解

### 5.1 启动入口与配置

**[AntiFraudPlatformApplication.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/AntiFraudPlatformApplication.java)**
- `@SpringBootApplication` + `@MapperScan("com.anti.mapper")`，标准启动类。

**配置文件：**
- [application.yml](file:///d:/Project/anti_fraud_platform/backend/src/main/resources/application.yml)：应用名、激活 `dev` profile、MyBatis-Plus 配置（下划线转驼峰、逻辑删除字段 `deleted`、分页）、默认 `server.port=8080`。
- [application-dev.yml](file:///d:/Project/anti_fraud_platform/backend/src/main/resources/application-dev.yml)：MySQL（`anti_fraud_platform` 库）、Redis（localhost:6379）、JWT 密钥与 24h 过期、DeepSeek API（key/model/temperature）、Swagger 开启、文件上传 10MB。
- [application-prod.yml](file:///d:/Project/anti_fraud_platform/backend/src/main/resources/application-prod.yml)：全部走环境变量（`${DB_USERNAME}` / `${REDIS_HOST}` / `${JWT_SECRET}` 等），关闭 Swagger，日志级别 info。

> ⚠️ 端口说明：`application.yml` 默认 `server.port=8080`，但前端 Vite 代理目标是 `8888`。运行全栈时需以 `8888` 启动后端（详见 [第 11 节](#11-项目运行方式) 与 [第 12 节](#12-注意事项与已知问题)）。

### 5.2 公共基础设施（common）

| 类 | 职责 |
|---|---|
| [Result.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/common/Result.java) | 统一响应封装 `Result<T>{code,message,data}`，静态工厂 `success()/success(data)/error(msg)/error(code,msg)/unauthorized(msg)/forbidden(msg)` |
| [BusinessException.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/common/BusinessException.java) | 业务异常，携带 `code`（默认 400），用于可预期的业务错误 |
| [GlobalExceptionHandler.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/common/GlobalExceptionHandler.java) | `@RestControllerAdvice`：处理 `BusinessException`(400)、`MethodArgumentNotValidException`/`BindException`(400，拼接字段错误)、兜底 `Exception`(500,"系统繁忙") |
| [CacheConstants.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/common/CacheConstants.java) | 缓存键前缀与 TTL 常量集中管理：`cache:hot_news:` / `cache:hot_case:` / `cache:recommend:` / `cache:leaderboard:` / `jwt:blacklist:` 等，并提供 `getXxxKey(...)` 组键方法 |

### 5.3 安全模块（security）

| 类 | 职责与关键方法 |
|---|---|
| [JwtUtils.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/security/JwtUtils.java) | `@Component`，HS-SHA 签名。<br>`generateToken(username, role, userId)` 生成含 role/userId 的 JWT；`validateToken(token)` 捕获各类 JJWT 异常返回布尔；`getUserIdFromToken`/`getRoleFromToken`/`getUsernameFromToken` 提取声明；`getExpirationFromToken` 返回剩余毫秒 |
| [JwtAuthenticationFilter.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/security/JwtAuthenticationFilter.java) | `OncePerRequestFilter`。提取 Bearer Token → 校验黑名单（`token:blacklist:`）→ 校验签名 → 构建 `LoginUser` + `ROLE_<ROLE>` 权限注入 `SecurityContextHolder`。异常被吞并记录日志，链路继续 |
| [JwtAuthenticationEntryPoint.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/security/JwtAuthenticationEntryPoint.java) | `AuthenticationEntryPoint`，未授权访问时返回 401 JSON `{code:401, message, path}` |
| [LoginUser.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/security/LoginUser.java) | `UserDetails` 实现，字段 `userId/username/role`。`getAuthorities()` 返回 `ROLE_<ROLE>`；`getUsername()`/`toString()` 返回 userId（便于 Controller 取用） |

### 5.4 配置类（config）

| 类 | 职责 |
|---|---|
| [SecurityConfig.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/SecurityConfig.java) | Spring Security 配置：禁 CSRF、开 CORS、无状态会话、配置放行路径（注册/登录/Swagger/`/uploads/**`）、`/api/admin/**` 需 ADMIN、用户管理接口需 ADMIN，注册 JWT 过滤器 |
| [RedisConfig.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/RedisConfig.java) | `@EnableScheduling @EnableAsync`；定义 `RedisTemplate<String,Object>`，Key 用 String 序列化、Value 用 Jackson JSON 序列化 |
| [MyBatisPlusConfig.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/MyBatisPlusConfig.java) | 注册分页插件 `PaginationInnerInterceptor(DbType.MYSQL)` |
| [WebMvcConfig.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/WebMvcConfig.java) | 将 `/uploads/**` 映射到本地 `upload.path` 目录（图片静态资源） |
| [CorsConfig.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/CorsConfig.java) | CORS 跨域配置 |
| [PasswordConfig.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/PasswordConfig.java) | 提供 `BCryptPasswordEncoder` Bean |
| [SwaggerConfig.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/SwaggerConfig.java) | OpenAPI 文档配置 |
| [AutoFillMetaObjectHandler.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/AutoFillMetaObjectHandler.java) | MyBatis-Plus 自动填充 `createTime`/`updateTime` |

### 5.5 工具类（util）

#### DeepSeekClient（AI 调用）
[DeepSeekClient.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/util/DeepSeekClient.java) — `@Component`，封装 DeepSeek Chat Completions API。
- `chat(systemPrompt, userMessage, history)`：组装 `messages`（system + 历史 + user）→ POST（60s 超时，Bearer 鉴权）→ 解析 `choices[0].message.content` 与 `usage` token 统计。
- 内部类 `DeepSeekResponse{success, content, errorMessage, promptTokens, completionTokens, totalTokens}`。
- 异常捕获后返回 `success=false`。

#### AntiFraudPromptTemplate（提示词工程）
[AntiFraudPromptTemplate.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/util/AntiFraudPromptTemplate.java) — 纯静态工具，提供反诈助手 "反诈卫士" 系统提示词：
- `getSystemPrompt()` 角色设定 + 10 类常见诈骗 + 回答原则与边界；
- `getSimplePrompt()`、`buildHistoryPrompt()`、`getRiskAssessmentPrompt(desc)`（风险评估）、`getPreventionGuidePrompt(type)`（防范指南）。

#### RecommendationAlgorithmUtil（推荐算法核心）
[RecommendationAlgorithmUtil.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/util/RecommendationAlgorithmUtil.java) — 纯静态工具，混合推荐算法基石（详见 [第 10 节](#10-关键算法说明)）：
- `calculateWilsonScore(positive, total)` — Wilson 置信区间下界（z=1.645）；
- `cosineSimilarity(...)` — 余弦相似度（List / Map 两种重载，用于内容推荐）；
- `pearsonCorrelation(...)` — 皮尔逊相关系数（用于协同过滤）；
- `extractKeywords` / `calculateTfIdf` — 关键词与 TF-IDF；
- `calculateHotScore(views, likes, comments)` — 热度分（0.3/0.5/0.2 加权）；
- `spmPredict(currentTag, history, rules)` — SPM 序列模式预测；
- `calculateCompositeScore(content, context, hot, w1, w2, w3)` — 综合排序分；
- `normalizeScore` / `weightedAverage`。

#### RedisCacheUtil（缓存工具）
[RedisCacheUtil.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/util/RedisCacheUtil.java) — `@Component`，封装 Redis 操作且**所有方法吞异常返回安全默认值**（保证 Redis 宕机不影响主流程）：
- 通用：`get/set/delete/hasKey/expire/increment/decrement/deleteByPattern`；
- 领域缓存：热点资讯/案例、紧急预警、推荐结果、用户兴趣、排行榜（日/周/总）、详情、用户会话、统计；
- Token 黑名单：`isTokenBlacklisted/addTokenToBlacklist`（前缀 `jwt:blacklist:`）；
- 验证码：`setCaptcha/verifyCaptcha`（不区分大小写，校验成功自动删除）；
- 分布式锁：`tryLock(lockKey, lockValue, seconds)`（SETNX）/ `releaseLock`。

#### DesensitizationUtils（脱敏）
[DesensitizationUtils.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/util/DesensitizationUtils.java) — 手机号/邮箱/学号/身份证脱敏（中间星号替换）。

### 5.6 业务模块（controller / service）

服务层统一采用 **接口（`com.anti.service.XxxService`）+ 实现（`com.anti.service.impl.XxxServiceImpl`）** 模式，多数实现继承 MyBatis-Plus `ServiceImpl<Mapper, Entity>`，使用 `@RequiredArgsConstructor` 构造器注入。两类鉴权风格并存：`@AuthenticationPrincipal LoginUser`（News/Forum/Chat/Comment/Achievement/Score/Profile/Learning）与手动 `JwtUtils.getUserIdFromToken`（Case/Challenge/Recommendation/Leaderboard/Scenario/Cache）。

#### 5.6.1 用户域

**[UserController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/UserController.java)** — `/api/user`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/register` | 注册（学生/管理员） |
| POST | `/login` | 登录，返回 JWT |
| GET | `/info` | 当前用户信息 |
| PUT | `/update` | 更新个人资料 |
| PUT | `/password` | 修改密码 |
| POST | `/logout` | 登出（JWT 加入黑名单） |
| GET | `/list` | 管理员：分页用户列表 |
| GET/PUT | `/{id}` | 管理员：用户详情/更新 |
| PUT | `/{id}/enable`、`/{id}/disable` | 管理员：启用/禁用 |

**UserServiceImpl 关键逻辑：**
- `login`：查用户 → 校验 `status` → BCrypt 比对 → 生成 JWT → 更新 `lastLoginTime` → 触发登录成就与连续学习（try/catch 包裹）→ 返回 `LoginVO`。
- `register`（`@Transactional`）：用户名/学号唯一性校验 → BCrypt 加密 → 插入用户 → `initScore`（建 `UserScore`）→ `initProfile`（建 `UserProfile`）。
- `logout`：解析 token 剩余过期时间，写入 Redis 黑名单。

#### 5.6.2 资讯域

**[NewsController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/NewsController.java)** — `/api/news`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/page` | 分页列表（categoryId/newsType/keyword） |
| GET | `/{id}` | 详情 |
| POST/PUT/DELETE | `/`、`/{id}` | 管理员：增/改/删 |
| POST | `/{id}/publish` | 发布 |
| PUT | `/{id}/top`、`/{id}/mandatory` | 置顶/必读 |
| POST | `/{id}/view`、`/{id}/browse` | 浏览量+1 / 记录浏览（停留时长） |
| POST/DELETE | `/{id}/like` | 点赞/取消 |
| GET | `/browse/history` | 浏览历史 |

**NewsServiceImpl**：列表按置顶+发布时间排序，批量填充 `likeCount`/`isLiked`；点赞幂等校验；浏览记录触发连续学习成就；写操作触发 `CacheRefreshService` 异步失效缓存。配套 [NewsCategoryController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/NewsCategoryController.java)（`/api/news/category`）管理分类。

#### 5.6.3 案例域

**[CaseController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/CaseController.java)** — `/api/case`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/page` | 分页（tagId/keyword） |
| GET | `/{id}` | 详情 |
| POST/PUT/DELETE | `/`、`/{id}` | 管理员维护 |
| POST | `/{id}/publish`、PUT `/{id}/featured` | 发布/精选 |
| POST/DELETE | `/{id}/like` | 点赞 |
| POST | `/{id}/browse` | 浏览（停留时长） |
| GET | `/browse/history`、`/hot`、`/wilson` | 历史/热点/Wilson 分 |

**FraudCaseServiceImpl**：
- 排序权重：精选 > Wilson 分 > 发布时间；
- `browseCase`：首次浏览奖励 **+2 积分**，更新日/周/总排行榜，触发 `browse_count` 成就与连续学习，按停留时长更新知识水平；
- `calculateWilsonScore`：z=1.645 实现 Wilson 区间下界，用于点赞率排序；
- 辅助：`getRiskLevel`（风险等级）、`getDifficultyName`（难度名）、`matchTargetGrade/Major`（目标人群匹配，支持 "all"）。

配套 [CaseTagController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/CaseTagController.java)（`/api/case/tag`）管理标签。

#### 5.6.4 学习闯关域

**[ChallengeController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/ChallengeController.java)** — `/api/challenge`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/list` | 用户关卡列表（含解锁/通关状态） |
| GET | `/{id}` | 关卡详情 |
| GET | `/records`、`/progress` | 闯关记录 / 总进度 |
| POST | `/submit` | 提交答题 → 评分 + 奖励 |
| POST/PUT/DELETE | `/`、`/{id}` | 管理员维护 |
| GET | `/admin/list`、`/admin/overview`、`/admin/stats/{id}` | 管理员统计 |
| PUT/DELETE | `/admin/batch/...` | 批量操作 |

**ChallengeServiceImpl**：
- **解锁规则**：`levelOrder <= maxPassedLevel + 1`（顺序解锁）；
- `submitChallenge`（事务）：仅 `type=quiz`；按 `correctIndexes` 集合比对判分；通关奖励 `scoreReward`，更新三榜，检查成就（`challenge_count`/`perfect_score`/`challenge_complete`），按 `totalScore*difficulty/20` 更新知识水平。

**[ScenarioController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/ScenarioController.java)** — `/api/scenario`（情景模拟 FSM）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/start/{challengeId}` | 开始情景 |
| GET | `/progress/{challengeId}` | 当前进度 |
| POST | `/decision` | 做决策（推进状态机） |
| POST | `/reset/{challengeId}` | 重置 |
| GET | `/ending/{challengeId}` | 结局状态 |

**ScenarioServiceImpl**：基于 `Challenge.ScenarioScript`（节点+边 FSM）。`makeDecision` 找到匹配当前节点+选择的边 → 记录 `DecisionRecord`（含 `isSafeChoice`）→ 推进 `currentNode` → 到达终点时 `finalScore = 100 * 安全选择数 / 总选择数`，≥60 通关，奖励 `scoreReward`（默认 20）。

#### 5.6.5 社区域

**[ForumController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/ForumController.java)** — `/api/forum`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/post/page` | 分页（postType/sortBy:time,like,comment/keyword） |
| GET | `/post/{postId}` | 详情（浏览量+1） |
| POST/PUT/DELETE | `/post`、`/post/{postId}` | 增/改/删（作者或管理员） |
| POST/DELETE | `/post/{postId}/like` | 点赞 |
| POST | `/post/{postId}/top`、`/featured` | 管理员置顶/精选 |
| GET | `/post/{postId}/comments` | 评论树 |
| GET | `/user/{userId}/posts` | 用户帖子 |

**ForumPostServiceImpl**：`createPost` 奖励 **+3 积分**并更新三榜；`deletePost` 级联删除（帖子点赞→评论→评论点赞→评论→帖子）；`getPostComments` 递归构建评论树，含 `isLiked`/`isAuthor` 标记。配套 [CommentController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/CommentController.java)（`/api/comment`）评论点赞。

#### 5.6.6 AI 客服与推荐域

**[ChatController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/ChatController.java)** — `/api/chat`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ask` | 提问（sessionId 可选） |
| GET | `/history/{sessionId}`、`/sessions` | 历史 / 会话列表 |
| POST | `/feedback` | 反馈（1满意/-1不满意） |
| GET | `/stats` | Token 使用统计 |
| DELETE | `/session/{sessionId}` | 删除会话 |
| POST | `/new-session` | 新建会话 |

**QAConversationServiceImpl**：`askQuestion`（事务）→ 无 sessionId 则新建 `session_{userId}_{millis}` → 加载历史 → `AntiFraudPromptTemplate.getSystemPrompt()` → `DeepSeekClient.chat` → 持久化 `QAConversation`（含 token 消耗）→ 返回 `ChatVO`。

**[RecommendationController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/RecommendationController.java)** — `/api/recommendation`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/list` | 自动策略推荐（limit/itemType） |
| GET | `/newbie`、`/growing`、`/mature` | 三种生命周期策略 |
| GET | `/interest` | 用户兴趣分析 |
| POST | `/click` | 点击反馈 |
| GET/POST/PUT/DELETE | `/rules`、`/rule/{id}` | 管理员关联规则管理 |
| POST | `/calculate-similarity` | 管理员：批量计算用户相似度 |

**RecommendationServiceImpl**：根据 `UserProfile.lifecycleStage` 分发：
- **新手（newbie）**：必读资讯优先 → 静态属性匹配案例（`targetGrades/targetMajors`）→ 热点补充 → 情景关卡优先；评分 `Wilson*0.6 + hot*0.4`。
- **成长（growing）**：内容推荐（用户行为向量 vs 案例标签向量余弦相似度）+ SPM 预测（关联规则）+ 上下文修正（弱点标签 ×1.2、低知识水平 ×1.1）+ 综合分 `0.5*content + 0.3*context + 0.2*hot`。
- **成熟（mature）**：协同过滤——同年级预过滤 → KNN（k=20）→ 偏置加权平均预测 → 标签→案例转换。
- `getUserInterestAnalysis`：兴趣标签归一化到 0-100 并缓存。
- `batchCalculateUserSimilarities`：遍历行为矩阵计算用户对余弦相似度，upsert `UserSimilarity`。

#### 5.6.7 统计与数据看板域

**[StatisticsController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/StatisticsController.java)** — `/api/statistics`（全部 `@PreAuthorize("hasRole('ADMIN')")`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/dashboard` | 看板聚合 |
| GET | `/visit/trend`、`/fraud/types`、`/fraud/top` | 访问趋势/诈骗类型分布/Top |
| GET | `/department/scores`、`/completion/rate`、`/cases/top`、`/activity/hourly` | 院系/完成率/Top 案例/小时活跃 |
| POST | `/refresh` | 手动刷新 |
| GET | `/export/daily`、`/export/department` | EasyExcel 导出 |

**StatisticsServiceImpl**：`getDashboardData` 聚合今日浏览/新增/活跃/通关/均分（含实时兜底），每个子查询 try/catch 兜底 0 或 mock 数据（避免看板崩溃）；`triggerStatisticsUpdate` 计算当日统计 upsert `DailyStatistics` 与 `DepartmentStatistics`。

**[LeaderboardController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/LeaderboardController.java)** — `/api/leaderboard`：`/daily`、`/weekly`、`/all`、`/user-rank`。**LeaderboardServiceImpl** 重度 Redis 缓存，`updateScore` 写三榜并刷新排名，`refreshRankings()` 每日 0 点重排（日榜清零）。

**[LearningController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/LearningController.java)** — `/api/learning`：无 Service 层，直接聚合 4 个 Mapper，`GET /records` 返回统一学习时间线（案例浏览+闯关+发帖），内存分页。

#### 5.6.8 积分 / 成就 / 画像域

**[ScoreController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/ScoreController.java)** — `/api/score`：`/info`、`/add`、`/deduct`、`/level/{level}`。
- **ScoreServiceImpl**：`LEVEL_THRESHOLD=100`，`calculateLevel = totalScore/100 + 1`；`addScore` 自动初始化缺失积分行、升级处理。

**[AchievementController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/AchievementController.java)** — `/api/achievement`：`/list`、`/user`、`/user/count`、`/unlock/{code}`。
- **AchievementServiceImpl**：`grantAchievementIfAbsent` 用 **原子 `insertIfAbsent`** 避免重复发奖；`checkAndUnlockAchievements(userId, conditionType, currentValue)` 匹配条件类型（`login_count`/`browse_count`/`post_count`/`challenge_count`/`perfect_score`/`challenge_complete`/`continuous_days`）；`refreshContinuousLearningStreak` 计算连续学习天数（Asia/Shanghai 时区）。

**[ProfileController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/ProfileController.java)** — `/api/profile`：`/info`、`/update`、`/browse`、`/weak-points`、`/interest-tags`、`/weak-point/{tag}`、`/interest-tag/{tag}`、`/lifecycle`。
- **ProfileServiceImpl**：阈值 `NEWBIE_BROWSE=5`、`GROWING_BROWSE=20`；`determineLifecycleStage` 综合注册天数与浏览数判定新手/成长/成熟；弱点和兴趣标签以 JSON 数组存储，幂等添加。

#### 5.6.9 文件与缓存域

- [FileController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/FileController.java) — `/api/file/upload`：图片上传，校验 `image/*`，保存到 `{upload.path}/images/{yyyyMMdd}/{UUID}.{suffix}`。
- [CacheController.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/controller/CacheController.java) — `/api/cache`：热点资讯/案例、紧急预警、缓存排行榜、手动刷新、缓存预热、`clearAllCache`、按实体失效、`/status`（管理员鉴权）。

### 5.7 定时任务（task）

| 类 | Cron | 说明 |
|---|---|---|
| [RecommendationTask.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/task/RecommendationTask.java) | `0 0 2 * * ?`（每日 02:00）<br>`0 0 3 ? * SUN`（每周日 03:00） | 批量计算用户相似度（`batchCalculateUserSimilarities`） |
| [StatisticsTask.java](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/task/StatisticsTask.java) | `0 0 1 * * ?`（每日 01:00）<br>`0 0 * * * ?`（每小时） | 触发统计聚合（`triggerStatisticsUpdate`） |
| LeaderboardServiceImpl | `0 0 0 * * ?`（每日 0 点） | `refreshRankings()` 重排三榜，日榜清零 |

### 5.8 数据访问与实体层（mapper / entity）

**Mapper**：全部继承 MyBatis-Plus `BaseMapper<T>`，部分配合 XML（[resources/mapper/](file:///d:/Project/anti_fraud_platform/backend/src/main/resources/mapper/)：User/Comment/ForumPost/UserAchievement/UserProfile/UserScore/Achievement）。复杂查询走 XML，如 `UserMapper.selectUserPage`、`UserAchievementMapper.insertIfAbsent`、`FraudCaseMapper.selectCasesByTagId`、`StatisticsQueryMapper` 等。

**实体**（`com.anti.entity`，共 20+ 个）：核心实体见 [第 7 节数据库设计](#7-数据库设计)。其中 `Challenge` 含嵌套类建模：
- `ChallengeContent`（题库）→ `Question`（题目，含 `correctIndexes`）→ `Option`（选项）；
- `ScenarioScript`（情景 FSM）→ `ScenarioNode`（节点：start/dialog/decision/result/end，含 `role`/`riskTip`）+ `ScenarioEdge`（边：from/to/`isSafeChoice`）；
- JSON 字段统一用 `JacksonTypeHandler` + `autoResultMap=true`。
- `ScenarioProgress`（进度：currentNode + `List<DecisionRecord>`）、`UserChallengeRecord`（记录：`AnswerDetail` → `QuestionAnswer`）。

DTO（`entity/dto`）为请求参数，VO（`entity/vo`）为响应视图。

---

## 6. 前端架构

### 6.1 入口与路由

- [main.ts](file:///d:/Project/anti_fraud_platform/frontend/src/main.ts)：创建 Vue 应用，挂载 Pinia、Router、Element Plus。
- [App.vue](file:///d:/Project/anti_fraud_platform/frontend/src/App.vue)：仅含 `<router-view />`。
- [router/index.ts](file:///d:/Project/anti_fraud_platform/frontend/src/router/index.ts)：
  - 公开路由：`/login`、`/register`；
  - 学生端（`/`，[layout/index.vue](file:///d:/Project/anti_fraud_platform/frontend/src/layout/index.vue)）：home/news/case/challenge(含 scenario/ranking)/forum/chat/profile/score/achievement/recommend；
  - 管理端（`/admin`，[layout/admin.vue](file:///d:/Project/anti_fraud_platform/frontend/src/layout/admin.vue)，`meta.isAdmin`）：dashboard/user/news/case/challenge/forum；
  - 404 兜底。
  - 全局守卫 `beforeEach`：NProgress 进度条、标题设置、Token 校验、`getUserInfo` 拉取、管理员权限拦截。

### 6.2 状态管理（Pinia stores）

| Store | 职责 |
|---|---|
| [user.ts](file:///d:/Project/anti_fraud_platform/frontend/src/stores/user.ts) | token/userInfo；`login/register/getUserInfo/updateUser/changePassword/logout`；`isAdmin`/`isLoggedIn` 计算属性 |
| profile.ts | 用户画像相关 |
| score.ts | 积分等级 |
| challenge.ts | 闯关状态 |

### 6.3 API 与请求封装

- [utils/request.ts](file:///d:/Project/anti_fraud_platform/frontend/src/utils/request.ts)：Axios 实例，`baseURL=/api`，30s 超时。请求拦截器注入 `Authorization`；响应拦截器按 `code` 分流（200 返回 data、401 清登录态跳转、403/404/500 提示）。导出 `get/post/put/del` 泛型助手。
- [api/](file:///d:/Project/anti_fraud_platform/frontend/src/api/)：按域拆分 `user/case/challenge/chat/forum/news/recommendation/statistics/upload`，统一类型定义（如 `UserVO`、`PageResult<T>`）。

### 6.4 视图模块

`views/` 按功能划分：home（首页）、news（资讯列表/详情）、case（案例列表/详情）、challenge（闯关列表/详情/情景模拟/排行榜）、forum（社区/详情）、chat（AI 客服）、profile（个人中心）、score（积分）、achievement（成就）、recommend（推荐）、login/register、admin（管理后台 6 页）、error/404。组件含 `components/icons`（26 个反诈主题 SVG 图标）、`components/profile`（头像上传/学习记录/编辑表单）、`components/admin/ScenarioScriptEditor`（情景剧本编辑器）。

---

## 7. 数据库设计

数据库名 `anti_fraud_platform`，字符集 utf8mb4。共 20+ 张表 + 2 个视图，按业务域分组（[init.sql](file:///d:/Project/anti_fraud_platform/sql/init.sql)）：

| 域 | 表 | 说明 |
|---|---|---|
| **用户** | `sys_user` | 用户主表（username/password/nickname/avatar/role:student,admin/grade/major/status） |
| | `user_score` | 积分等级（total_score/current_level/weekly_score） |
| | `user_profile` | 用户画像（knowledge_level/weak_points:JSON/interest_tags:JSON/lifecycle_stage/browse_count/register_days） |
| | `achievement` | 成就定义（code/condition_type/condition_value/score_reward） |
| | `user_achievement` | 用户成就记录（uk user+achievement） |
| **资讯** | `news_category` / `news` | 分类 / 资讯（news_type:news,warning,policy / is_top / is_mandatory / view_count） |
| | `news_like` / `news_browse_log` | 点赞 / 浏览记录（stay_duration） |
| **案例** | `case_tag` / `fraud_case` | 标签 / 案例（scripts:JSON 决策树 / target_grades:JSON / risk_score / wilson_score / like_rate） |
| | `case_tag_relation` / `case_like` / `case_browse_log` | 标签关联 / 点赞 / 浏览 |
| **学习** | `challenge` | 关卡（type:quiz,scenario / content:JSON 题库 / scripts:JSON FSM / level_order 顺序解锁） |
| | `user_challenge_record` | 闯关记录（attempts/score/passed/answer_detail:JSON） |
| | `scenario_progress` | 情景进度（current_node/decision_history:JSON/status:in_progress,completed,failed/final_score） |
| | `leaderboard` | 排行榜（period_type:daily,weekly,all / score / rank / update_date） |
| **社区** | `forum_post` / `post_like` | 帖子（post_type:experience,question,discussion）/ 点赞 |
| | `comment` / `comment_like` | 评论（parent_id 树形）/ 评论点赞 |
| **AI/推荐** | `qa_conversation` | 问答会话（session_id/question/answer/model/tokens_used/feedback） |
| | `association_rule` | SPM 关联规则（trigger_tag/predicted_tags:JSON/confidence） |
| | `user_behavior_matrix` | 用户-标签行为矩阵（behavior_score） |
| | `user_similarity` | 用户相似度（similarity_score 皮尔逊/common_tags:JSON） |
| | `recommendation_log` | 推荐日志（item_type/item_id/recommend_reason:JSON/score/clicked） |
| **统计** | `daily_statistics` / `department_statistics` | 每日统计 / 院系统计（case_views:JSON/top_cases:JSON） |

**视图：** `v_user_info`（用户+积分+画像）、`v_case_full`（案例+标签聚合）。

**逻辑删除：** MyBatis-Plus 全局配置 `logic-delete-field=deleted`（1 删除 / 0 正常）。**初始化数据：** 内置管理员账号（admin/123456 BCrypt）、4 个资讯分类、8 个案例标签、8 个成就、5 条关联规则。

---

## 8. 依赖关系

### 8.1 后端依赖（pom.xml）
核心：`spring-boot-starter-web` / `-security` / `-data-redis` / `-validation`、`mybatis-plus-spring-boot3-starter`、`mysql-connector-j`、`commons-pool2`、`jjwt-api/impl/jackson`、`springdoc-openapi-starter-webmvc-ui`、`lombok`、`commons-lang3`、`hutool-all`、`easyexcel`、`httpclient5`。

### 8.2 前端依赖（package.json）
核心：`vue`、`vue-router`、`pinia`、`element-plus`、`@element-plus/icons-vue`、`axios`、`echarts`、`dayjs`、`lodash-es`、`nprogress`、`cropperjs`、`html2canvas`、`jspdf`、`@vueuse/core`；开发依赖 `vite`、`vue-tsc`、`typescript`、`sass`、`eslint`、`prettier`、`unplugin-auto-import`、`unplugin-vue-components`。

### 8.3 模块间依赖（业务流转）

**游戏化副作用链**（内容交互触发，try/catch 包裹不阻塞主流程）：
```
浏览案例(+2) / 浏览资讯 / 发帖(+3) / 通关(quiz +scoreReward / scenario +20)
   │
   ├─→ ScoreService.addScore ──→ 升级处理
   ├─→ LeaderboardService.updateScore (daily/weekly/all 三榜)
   ├─→ AchievementService.checkAndUnlockAchievements (条件类型匹配)
   └─→ ProfileService.updateKnowledgeLevel (按难度与得分提升知识水平)
```

**推荐数据流**：
```
用户行为 → user_behavior_matrix(行为矩阵) ─┐
                                          ├─→ RecommendationAlgorithmUtil
association_rule(SPM规则) ────────────────┤     ├─ cosine (内容)
                                          │     ├─ pearson (协同)
user_similarity(相似度) ──────────────────┘     ├─ wilson/hot (热度)
                                                └─ composite (综合排序)
                                                      │
            ProfileService.determineLifecycleStage ──┤ (新手/成长/成熟分发)
                                                      ▼
                                          recommendation_log (推荐日志+点击反馈)
```

**缓存失效链**：写操作 → `CacheRefreshService.handleXxxEvent`（`@Lazy` 注入打破循环依赖）→ 失效对应 Redis 键。

---

## 9. 缓存策略

[RedisCacheUtil](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/util/RedisCacheUtil.java) + [CacheConstants](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/common/CacheConstants.java) 集中管理，所有方法**吞异常返回默认值**（容错优先）：

| 缓存对象 | 键前缀 | TTL |
|---|---|---|
| 热点资讯 / 热点案例 | `cache:hot_news:` / `cache:hot_case:` | 300s |
| 紧急预警 | `cache:emergency_alert:` | 60s |
| 推荐结果 | `cache:recommend:` | 3600s |
| 用户兴趣 | `cache:user_interest:` | 7200s |
| 日榜 / 用户排名 | `cache:leaderboard:` | 300s |
| 周榜 / 总榜 | 同上 | 900s / 1800s |
| 资讯/案例详情 | `cache:news:detail:` / `cache:case:detail:` | 600s |
| 用户会话 / 用户画像 | `cache:user_session:` / `cache:user_profile:` | 1800s |
| 统计 | `cache:statistics:` | 300s |
| 验证码 | `captcha:` | 300s |
| JWT 黑名单 | `jwt:blacklist:` | 86400s |

---

## 10. 关键算法说明

### 10.1 Wilson 置信区间（排序去偏）
用于按点赞率排序但样本量差异大的场景（避免 1/1=100% 压过 95/100）。`RecommendationAlgorithmUtil.calculateWilsonScore(positive, total)`，z=1.645（95% 单侧）：
```
p = positive/total
score = (p + z²/2n − z·√(p(1−p)/n + z²/4n²)) / (1 + z²/n)
```

### 10.2 混合推荐算法
按用户生命周期分发（`RecommendationServiceImpl`）：

| 阶段 | 策略 | 核心算法 |
|---|---|---|
| 新手 | 冷启动 | 必读资讯 + 静态属性匹配 + 热点补充；`Wilson*0.6 + hot*0.4` |
| 成长 | 内容+协同 | 余弦相似度（用户行为向量 vs 案例标签向量）+ SPM 序列预测 + 上下文修正；综合分 `0.5*content + 0.3*context + 0.2*hot` |
| 成熟 | 协同过滤 | 同年级预过滤 → KNN(k=20) → 偏置加权平均预测 |

- **内容相似度**：`cosineSimilarity(Map<tagId, score>)`，统一两向量 key 集合后计算；
- **协同相似度**：`pearsonCorrelation`，基于用户行为向量；
- **SPM 预测**：`spmPredict(currentTag, history, rules)`，查关联规则表预测下一标签；
- **综合分**：`calculateCompositeScore(content, context, hot, w1, w2, w3)`。

### 10.3 情景模拟 FSM
`ScenarioScript` 为有向图：用户从 `startNodeId` 出发，在 `decision` 节点选择 `ScenarioEdge`（标注 `isSafeChoice`），到达 `endNodeIds` 结束。`finalScore = 100 * 安全选择数 / 总选择数`，≥60 通关。

### 10.4 连续学习天数
`AchievementServiceImpl.refreshContinuousLearningStreak`：读取去重活动日字符串，从今日（或昨日）向前回溯连续天数，触发 `continuous_days` 成就（Asia/Shanghai 时区）。

---

## 11. 项目运行方式

### 11.1 环境要求
- JDK 17+
- Maven 3.8+
- Node.js 18+ / npm
- MySQL 8.x
- Redis 6.x+

### 11.2 数据库初始化
```bash
# 登录 MySQL 后执行
mysql -u root -p < sql/init.sql
# 可选：导入种子数据
mysql -u root -p anti_fraud_platform < sql/seed_case.sql
mysql -u root -p anti_fraud_platform < sql/seed_news.sql
mysql -u root -p anti_fraud_platform < sql/seed_challenge.sql
```
默认管理员账号：**admin / 123456**。

### 11.3 后端启动
1. 配置 [application-dev.yml](file:///d:/Project/anti_fraud_platform/backend/src/main/resources/application-dev.yml)：MySQL 账号密码、Redis 密码、DeepSeek API Key（第 54 行）。
2. **端口注意**：`application.yml` 默认 `server.port=8080`，但前端 Vite 代理目标是 `8888`。需以 8888 启动后端：
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8888"
   ```
   或将 `application.yml` 的 `server.port` 改为 `8888`（推荐，避免 Windows 8080 端口被 Docker/WSL2 占用）。
3. 启动成功后访问 Swagger：`http://localhost:8888/swagger-ui.html`。

### 11.4 前端启动
```bash
cd frontend
npm install
npm run dev
```
访问 `http://localhost:5173`，Vite 自动将 `/api` 代理到 `http://localhost:8888`。

### 11.5 生产打包
```bash
# 后端
cd backend && mvn clean package
java -jar target/anti-fraud-platform-1.0.0.jar --spring.profiles.active=prod

# 前端
cd frontend && npm run build   # 产物在 dist/，由 Nginx 托管并代理 /api → 后端
```
生产环境配置走环境变量（`DB_USERNAME`/`DB_PASSWORD`/`REDIS_HOST`/`JWT_SECRET` 等，见 [application-prod.yml](file:///d:/Project/anti_fraud_platform/backend/src/main/resources/application-prod.yml)）。

---

## 12. 注意事项与已知问题

1. **端口约束**：后端必须使用 **8888** 端口（8080/8088 被 Windows/Docker/WSL2 系统占用），前端 Vite 代理已指向 8888。`application.yml` 默认值需覆盖。

2. **Redis 必须可用**：`JwtAuthenticationFilter` 每次请求校验 Token 黑名单依赖 Redis，Redis 连接失败会导致静默 401（异常被吞，请求以未认证继续）。`RedisCacheUtil` 同样吞异常返回默认值，缓存失效会静默降级。

3. **DeepSeek API Key**：必须配置有效 Key（`application-dev.yml` 第 54 行）。`DeepSeekClient` 未在解析 JSON 前校验 HTTP 状态码，无效 Key 返回非 JSON 401 会触发误导性的 JSON 解析错误。

4. **JWT 黑名单前缀不一致**：`JwtAuthenticationFilter` 硬编码 `token:blacklist:`，而 `CacheConstants.JWT_BLACKLIST_PREFIX` 与 `RedisCacheUtil` 用 `jwt:blacklist:`。若登出走 `RedisCacheUtil.addTokenToBlacklist` 而过滤器用另一前缀校验，已登出 Token 可能仍被放行。建议统一前缀。

5. **两套鉴权风格**：部分 Controller 用 `@AuthenticationPrincipal LoginUser`，部分用 `JwtUtils.getUserIdFromToken` 手动解析，注意 `LoginUser.getUsername()` 返回的是 userId 而非用户名。

6. **容错设计**：`StatisticsServiceImpl` 大量 try/catch + mock 数据兜底，看板即使无数据也能渲染；游戏化副作用全部 try/catch 包裹，不影响主业务。

7. **幂等性**：点赞操作前置查重；`grantAchievementIfAbsent` 用原子 `insertIfAbsent` 防止重复发奖。

8. **资源映射**：上传图片通过 `/uploads/**` 静态映射（[WebMvcConfig](file:///d:/Project/anti_fraud_platform/backend/src/main/java/com/anti/config/WebMvcConfig.java)），已在 Security 中匿名放行（浏览器 `<img>` 无法携带 JWT）。

---

*本文档基于代码库现状自动梳理生成，涵盖架构、模块、关键类函数、依赖与运行方式。如代码发生变更，请同步更新对应章节。*
