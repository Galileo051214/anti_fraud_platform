package com.anti.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anti.entity.vo.SourceVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebSearchClientTest {

    @Test
    void filterKeepsAllowedDomainsAndDeduplicatesSources() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "allowedDomainsConfig", "mps.gov.cn,12321.cn");
        ReflectionTestUtils.setField(client, "maxResults", 3);
        ReflectionTestUtils.setField(client, "contentMaxChars", 1200);

        List<SourceVO> result = client.filterAndDeduplicate(List.of(
                source("公安部提醒", "https://www.mps.gov.cn/a.html#top", "提示1"),
                source("公安部提醒", "https://www.mps.gov.cn/a.html/", "重复标题和URL"),
                source("12321举报", "https://www.12321.cn/report", "举报"),
                source("不允许来源", "https://example.com/fraud", "不要保留"),
                source("子域名也允许", "https://news.mps.gov.cn/b.html", "子域名")
        ));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(SourceVO::getDomain)
                .containsExactly("mps.gov.cn", "12321.cn", "news.mps.gov.cn");
        assertThat(result).extracting(SourceVO::getTitle)
                .containsExactly("公安部提醒", "12321举报", "子域名也允许");
    }

    @Test
    void parseFirecrawlSourcesKeepsMarkdownContent() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "allowedDomainsConfig", "mps.gov.cn,12321.cn");
        ReflectionTestUtils.setField(client, "maxResults", 5);
        ReflectionTestUtils.setField(client, "contentMaxChars", 1200);

        List<SourceVO> result = client.parseFirecrawlSources("""
                {
                  "success": true,
                  "data": {
                    "web": [
                      {
                        "title": "公安部反诈预警",
                        "url": "https://www.mps.gov.cn/n2255079/a.html",
                        "description": "近期刷单返利诈骗高发。",
                        "markdown": "# 反诈预警\\n近期刷单返利诈骗高发，要求先垫付转账的均需警惕。"
                      },
                      {
                        "title": "外部来源",
                        "url": "https://example.com/fraud",
                        "description": "不应保留"
                      }
                    ]
                  }
                }
                """);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDomain()).isEqualTo("mps.gov.cn");
        assertThat(result.get(0).getSnippet()).contains("刷单返利");
        assertThat(result.get(0).getContent()).contains("先垫付转账");
    }

    @Test
    void parseExaSourcesKeepsTextAndHighlights() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "allowedDomainsConfig", "12321.cn");
        ReflectionTestUtils.setField(client, "maxResults", 5);
        ReflectionTestUtils.setField(client, "contentMaxChars", 1200);

        List<SourceVO> result = client.parseExaSources("""
                {
                  "results": [
                    {
                      "title": "12321举报提醒",
                      "url": "https://www.12321.cn/article.html",
                      "publishedDate": "2026-06-20T00:00:00.000Z",
                      "summary": "近期冒充客服诈骗增加。",
                      "text": "用户遇到冒充客服退款、索要验证码或屏幕共享时，应停止操作并通过官方渠道核验。",
                      "highlights": ["冒充客服退款", "索要验证码"]
                    }
                  ]
                }
                """);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDomain()).isEqualTo("12321.cn");
        assertThat(result.get(0).getPublishedAt()).startsWith("2026-06-20");
        assertThat(result.get(0).getSnippet()).contains("冒充客服");
        assertThat(result.get(0).getContent()).contains("屏幕共享");
    }

    @Test
    void autoProviderPrefersFirecrawlThenExaThenBing() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "provider", "auto");
        ReflectionTestUtils.setField(client, "mcpEnabled", true);
        ReflectionTestUtils.setField(client, "apiKey", "bing-key");
        ReflectionTestUtils.setField(client, "exaApiKey", "exa-key");
        ReflectionTestUtils.setField(client, "firecrawlApiKey", "fc-key");

        assertThat((String) ReflectionTestUtils.invokeMethod(client, "resolveProvider"))
                .isEqualTo("firecrawl-mcp");

        ReflectionTestUtils.setField(client, "firecrawlApiKey", "");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "resolveProvider"))
                .isEqualTo("exa-mcp");

        ReflectionTestUtils.setField(client, "exaApiKey", "");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "resolveProvider"))
                .isEqualTo("bing");
    }

    @Test
    void explicitMcpProviderAliasesResolveToMcpProviders() {
        WebSearchClient client = new WebSearchClient(null);

        ReflectionTestUtils.setField(client, "provider", "mcp-firecrawl");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "resolveProvider"))
                .isEqualTo("firecrawl-mcp");

        ReflectionTestUtils.setField(client, "provider", "exa-mcp");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "resolveProvider"))
                .isEqualTo("exa-mcp");
    }

    @Test
    void remoteMcpEndpointAndHeadersAreBuiltFromProviderConfig() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "firecrawlApiKey", "fc-test/key");
        ReflectionTestUtils.setField(client, "firecrawlMcpUrl", "https://mcp.firecrawl.dev/{apiKey}/v2/mcp");
        ReflectionTestUtils.setField(client, "exaApiKey", "exa-key");
        ReflectionTestUtils.setField(client, "exaMcpUrl", "https://mcp.exa.ai/mcp");
        ReflectionTestUtils.setField(client, "exaMcpApiKeyHeader", "x-api-key");

        assertThat((String) ReflectionTestUtils.invokeMethod(client, "buildMcpEndpointUrl", "firecrawl-mcp"))
                .isEqualTo("https://mcp.firecrawl.dev/fc-test%2Fkey/v2/mcp");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "buildMcpEndpointUrl", "exa-mcp"))
                .isEqualTo("https://mcp.exa.ai/mcp");

        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) ReflectionTestUtils.invokeMethod(client, "buildMcpHeaders", "exa-mcp");
        assertThat(headers).containsEntry("x-api-key", "exa-key");
    }

    @Test
    void buildMcpToolArgumentsUsesToolSchema() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "allowedDomainsConfig", "mps.gov.cn,12321.cn");
        ReflectionTestUtils.setField(client, "maxResults", 5);
        ReflectionTestUtils.setField(client, "freshnessDays", 30);

        McpStdioClient.McpTool tool = new McpStdioClient.McpTool();
        tool.setName("firecrawl_search");
        tool.setInputSchema(JSONUtil.parseObj("""
                {
                  "type": "object",
                  "properties": {
                    "query": {"type": "string"},
                    "limit": {"type": "number"},
                    "sources": {"type": "array"},
                    "includeDomains": {"type": "array"},
                    "scrapeOptions": {"type": "object"}
                  }
                }
                """));

        JSONObject arguments = client.buildMcpToolArguments("最新刷单诈骗", "firecrawl-mcp", tool);

        assertThat(arguments.getStr("query"))
                .contains("最新刷单诈骗", "最新", "通报", "预警", "诈骗", "反诈");
        assertThat(arguments.getInt("limit")).isEqualTo(10);
        assertThat(arguments.getJSONArray("sources").getJSONObject(0).getStr("type")).isEqualTo("web");
        assertThat(arguments.getJSONArray("includeDomains")).containsExactly("mps.gov.cn", "12321.cn");
        assertThat(arguments.getJSONObject("scrapeOptions").getJSONArray("formats")).containsExactly("markdown");
        assertThat(arguments.getJSONObject("scrapeOptions").containsKey("redactPII")).isFalse();
    }

    @Test
    void genericRecentQuestionBuildsDiverseCurrentMonthQueries() {
        WebSearchClient client = new WebSearchClient(null);
        LocalDate today = LocalDate.of(2026, 7, 5);

        List<String> queries = client.buildSearchQueries("近期高发诈骗有哪些", today);

        assertThat(queries).hasSize(3);
        assertThat(queries.get(0)).contains("近期高发诈骗有哪些", "2026年7月", "通报", "预警");
        assertThat(queries.get(1)).contains("2026年7月", "高发", "电信网络诈骗", "反诈中心");
        assertThat(queries.get(2)).contains("2026年7月", "新型", "刷单返利", "冒充客服", "虚假投资");
    }

    @Test
    void recentQuestionUsesShorterFreshnessWindow() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "freshnessDays", 30);

        assertThat(client.resolveEffectiveFreshnessDays("近期高发诈骗有哪些")).isEqualTo(14);
        assertThat(client.resolveEffectiveFreshnessDays("本周高发诈骗")).isEqualTo(7);
        assertThat(client.resolveEffectiveFreshnessDays("今天诈骗预警")).isEqualTo(2);
    }

    @Test
    void buildMcpContentArgumentsUsesScrapeSchema() {
        WebSearchClient client = new WebSearchClient(null);
        SourceVO source = source("公安部", "https://www.mps.gov.cn/a.html", "摘要");
        McpStdioClient.McpTool tool = new McpStdioClient.McpTool();
        tool.setName("firecrawl_scrape");
        tool.setInputSchema(JSONUtil.parseObj("""
                {
                  "type": "object",
                  "properties": {
                    "url": {"type": "string"},
                    "formats": {"type": "array"},
                    "onlyMainContent": {"type": "boolean"}
                  }
                }
                """));

        JSONObject arguments = client.buildMcpContentArguments(source, "firecrawl-mcp", tool);

        assertThat(arguments.getStr("url")).isEqualTo("https://www.mps.gov.cn/a.html");
        assertThat(arguments.getJSONArray("formats")).containsExactly("markdown");
        assertThat(arguments.getBool("onlyMainContent")).isTrue();
    }

    @Test
    void parseMcpSourcesReadsStructuredContentAndTextJson() {
        WebSearchClient client = new WebSearchClient(null);
        ReflectionTestUtils.setField(client, "allowedDomainsConfig", "mps.gov.cn,12321.cn");
        ReflectionTestUtils.setField(client, "maxResults", 5);
        ReflectionTestUtils.setField(client, "contentMaxChars", 1200);

        McpStdioClient.McpToolResult toolResult = new McpStdioClient.McpToolResult();
        toolResult.setStructuredContent(JSONUtil.parseObj("""
                {
                  "results": [
                    {
                      "title": "公安部 MCP 来源",
                      "url": "https://www.mps.gov.cn/mcp.html",
                      "markdown": "公安部发布涉校园诈骗预警，提醒不要共享屏幕。"
                    }
                  ]
                }
                """));
        toolResult.setTextContents(List.of("""
                {
                  "data": [
                    {
                      "title": "12321 MCP 来源",
                      "url": "https://www.12321.cn/mcp.html",
                      "summary": "举报中心提示警惕冒充客服诈骗。"
                    }
                  ]
                }
                """));

        List<SourceVO> result = client.parseMcpSources(toolResult);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SourceVO::getDomain)
                .containsExactly("mps.gov.cn", "12321.cn");
        assertThat(result.get(0).getContent()).contains("共享屏幕");
        assertThat(result.get(1).getSnippet()).contains("冒充客服");
    }

    private SourceVO source(String title, String url, String snippet) {
        SourceVO source = new SourceVO();
        source.setTitle(title);
        source.setUrl(url);
        source.setSnippet(snippet);
        return source;
    }
}
