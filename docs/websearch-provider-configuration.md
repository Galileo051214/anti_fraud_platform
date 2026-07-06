# WebSearch Provider 配置说明

生成日期：2026-07-04

## 1. 后端运行时配置

智能反诈助手的最新诈骗汇报由 Spring Boot 后端的 `WebSearchClient` 调用搜索/抓取服务。当前支持：

- `auto`：默认模式。`WEBSEARCH_MCP_ENABLED=true` 时优先使用 Firecrawl/Exa 远程 MCP；没有可用 MCP key 时回落到原 provider。
- `firecrawl-mcp`：通过 Firecrawl 远程 MCP URL 调用 `firecrawl_search`。
- `exa-mcp`：通过 Exa 远程 MCP URL 调用 `web_search_exa`。
- `firecrawl`：直接调用 Firecrawl Search HTTP API。
- `exa`：直接调用 Exa Search HTTP API。
- `bing`：沿用原 Bing Web Search 兼容接口，只返回搜索摘要。

推荐最小配置：

```bash
WEBSEARCH_PROVIDER=auto
WEBSEARCH_MCP_ENABLED=true

# 二选一即可；auto 会优先 Firecrawl MCP，其次 Exa MCP
FIRECRAWL_API_KEY=fc-YOUR-KEY
EXA_API_KEY=YOUR-EXA-KEY
```

这些变量可以写入项目根目录 `.env` 或 `backend/.env`。后端启动时会自动加载，优先级为：真实系统环境变量 > `.env` > `application-*.yml` 默认值。仓库已提供 `.env.example`，复制后填写真实 key 即可。

## 2. Firecrawl MCP

默认远程 MCP 配置：

```bash
WEBSEARCH_PROVIDER=firecrawl-mcp
FIRECRAWL_API_KEY=fc-YOUR-KEY
FIRECRAWL_MCP_URL=https://mcp.firecrawl.dev/{apiKey}/v2/mcp
FIRECRAWL_MCP_SEARCH_TOOL=firecrawl_search
```

`{apiKey}` 会由后端在请求前替换为 URL 编码后的 `FIRECRAWL_API_KEY`。

如果使用自托管或代理后的 Firecrawl 远程 MCP，改写完整 MCP URL：

```bash
FIRECRAWL_MCP_URL=https://mcp.firecrawl.dev/{apiKey}/v2/mcp
```

调用流程：

1. 后端向 `FIRECRAWL_MCP_URL` 发送 Streamable HTTP MCP `initialize`。
2. 发送 `notifications/initialized`。
3. 通过 `tools/list` 查找 `firecrawl_search`。
4. 通过 `tools/call` 调用搜索工具。

## 3. Exa MCP

默认远程 MCP 配置：

```bash
WEBSEARCH_PROVIDER=exa-mcp
EXA_API_KEY=YOUR-EXA-KEY
EXA_MCP_URL=https://mcp.exa.ai/mcp
EXA_MCP_API_KEY_HEADER=x-api-key
EXA_MCP_SEARCH_TOOL=web_search_exa
```

## 4. 通用参数

```bash
WEBSEARCH_ALLOWED_DOMAINS=mps.gov.cn,gov.cn,12321.cn,cac.gov.cn,miit.gov.cn,pbc.gov.cn,court.gov.cn
WEBSEARCH_MAX_RESULTS=5
WEBSEARCH_TIMEOUT_MS=8000
WEBSEARCH_MCP_REQUEST_TIMEOUT_MS=20000
WEBSEARCH_CACHE_TTL_SECONDS=1800
WEBSEARCH_FRESHNESS_DAYS=30
WEBSEARCH_CONTENT_MAX_CHARS=1200
```

所有 MCP 返回结果仍会经过 `WEBSEARCH_ALLOWED_DOMAINS` 白名单过滤和去重。即使 MCP 工具返回外部网页，也不会进入最终 Prompt。

## 5. 直接 HTTP Provider

需要绕过 MCP 时可以显式指定直接 HTTP provider。

Firecrawl HTTP：

```bash
WEBSEARCH_PROVIDER=firecrawl
FIRECRAWL_API_KEY=fc-YOUR-KEY
FIRECRAWL_API_URL=https://api.firecrawl.dev/v2/search
FIRECRAWL_SCRAPE_CONTENT=true
```

Exa HTTP：

```bash
WEBSEARCH_PROVIDER=exa
EXA_API_KEY=YOUR-EXA-KEY
EXA_API_URL=https://api.exa.ai/search
EXA_MAX_AGE_HOURS=720
EXA_CONTENT_VERBOSITY=compact
```

Bing 兼容接口：

```bash
WEBSEARCH_PROVIDER=bing
WEBSEARCH_API_URL=https://api.bing.microsoft.com/v7.0/search
WEBSEARCH_API_KEY=YOUR-BING-KEY
WEBSEARCH_API_KEY_HEADER=Ocp-Apim-Subscription-Key
```

## 6. 外部 MCP 客户端配置

这些配置用于 Codex、Cursor、Claude 等客户端，不是 Spring Boot 后端必需配置。

Firecrawl 远程 MCP：

```json
{
  "mcpServers": {
    "firecrawl": {
      "url": "https://mcp.firecrawl.dev/fc-YOUR-API-KEY/v2/mcp"
    }
  }
}
```

Exa 远程 MCP：

```json
{
  "mcpServers": {
    "exa": {
      "url": "https://mcp.exa.ai/mcp",
      "headers": {
        "x-api-key": "YOUR_EXA_API_KEY"
      }
    }
  }
}
```

参考：

- MCP Lifecycle：https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle
- MCP Transports：https://modelcontextprotocol.io/specification/2025-06-18/basic/transports
- MCP Tools：https://modelcontextprotocol.io/specification/2025-06-18/server/tools
- Firecrawl MCP：https://github.com/firecrawl/firecrawl-mcp-server
- Exa MCP：https://exa.ai/docs/reference/exa-mcp
