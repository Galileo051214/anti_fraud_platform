package com.anti.util;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anti.entity.vo.SourceVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 面向最新诈骗汇报的受限域名搜索客户端。
 */
@Slf4j
@Component
public class WebSearchClient {

    private static final String CACHE_PREFIX = "chat:agent:websearch:";
    private static final int DEFAULT_TIMEOUT_MS = 8000;
    private static final int DEFAULT_CONTENT_MAX_CHARS = 1200;
    private static final String PROVIDER_AUTO = "auto";
    private static final String PROVIDER_BING = "bing";
    private static final String PROVIDER_FIRECRAWL = "firecrawl";
    private static final String PROVIDER_EXA = "exa";
    private static final String PROVIDER_FIRECRAWL_MCP = "firecrawl-mcp";
    private static final String PROVIDER_EXA_MCP = "exa-mcp";
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^\\]]{1,160})]\\((https?://[^\\s)]+)\\)");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s)\\]}>\"]+");
    private static final int MAX_SEARCH_QUERY_VARIANTS = 3;
    private static final List<String> SPECIFIC_FRAUD_TERMS = List.of(
            "刷单", "返利", "杀猪盘", "公检法", "客服", "贷款", "网贷", "投资", "理财",
            "征信", "校园贷", "冒充", "虚拟绑架", "裸聊", "游戏", "招聘", "租号", "跑分",
            "洗钱", "验证码", "屏幕共享", "共享屏幕"
    );
    private static final List<String> RECENT_TIME_TERMS = List.of(
            "近期", "最近", "当前", "最新", "新型", "高发", "趋势", "通报", "预警"
    );

    @Value("${websearch.enabled:true}")
    private boolean enabled;

    @Value("${websearch.provider:auto}")
    private String provider;

    @Value("${websearch.api-url:https://api.bing.microsoft.com/v7.0/search}")
    private String apiUrl;

    @Value("${websearch.api-key:}")
    private String apiKey;

    @Value("${websearch.api-key-header:Ocp-Apim-Subscription-Key}")
    private String apiKeyHeader;

    @Value("${websearch.allowed-domains:mps.gov.cn,gov.cn,12321.cn,cac.gov.cn,miit.gov.cn,pbc.gov.cn,court.gov.cn}")
    private String allowedDomainsConfig;

    @Value("${websearch.max-results:5}")
    private int maxResults;

    @Value("${websearch.cache-ttl-seconds:1800}")
    private long cacheTtlSeconds;

    @Value("${websearch.timeout-ms:8000}")
    private int timeoutMs;

    @Value("${websearch.freshness-days:30}")
    private int freshnessDays;

    @Value("${websearch.content-max-chars:1200}")
    private int contentMaxChars;

    @Value("${websearch.firecrawl.api-url:https://api.firecrawl.dev/v2/search}")
    private String firecrawlApiUrl;

    @Value("${websearch.firecrawl.api-key:}")
    private String firecrawlApiKey;

    @Value("${websearch.firecrawl.scrape-content:true}")
    private boolean firecrawlScrapeContent;

    @Value("${websearch.exa.api-url:https://api.exa.ai/search}")
    private String exaApiUrl;

    @Value("${websearch.exa.api-key:}")
    private String exaApiKey;

    @Value("${websearch.exa.max-age-hours:720}")
    private int exaMaxAgeHours;

    @Value("${websearch.exa.content-verbosity:compact}")
    private String exaContentVerbosity;

    @Value("${websearch.mcp.enabled:true}")
    private boolean mcpEnabled;

    @Value("${websearch.mcp.protocol-version:2025-06-18}")
    private String mcpProtocolVersion;

    @Value("${websearch.mcp.request-timeout-ms:20000}")
    private int mcpRequestTimeoutMs;

    @Value("${websearch.mcp.firecrawl.url:https://mcp.firecrawl.dev/{apiKey}/v2/mcp}")
    private String firecrawlMcpUrl;

    @Value("${websearch.mcp.firecrawl.search-tool:firecrawl_search}")
    private String firecrawlMcpSearchTool;

    @Value("${websearch.mcp.exa.url:https://mcp.exa.ai/mcp}")
    private String exaMcpUrl;

    @Value("${websearch.mcp.exa.api-key-header:x-api-key}")
    private String exaMcpApiKeyHeader;

    @Value("${websearch.mcp.exa.search-tool:web_search_exa}")
    private String exaMcpSearchTool;

    private final RedisCacheUtil redisCacheUtil;
    private final ConcurrentMap<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();

    public WebSearchClient(RedisCacheUtil redisCacheUtil) {
        this.redisCacheUtil = redisCacheUtil;
    }

    public SearchResult searchLatestFraud(String question) {
        SearchPlan searchPlan = buildSearchPlan(question);
        String selectedProvider = resolveProvider();
        String cacheKey = buildCacheKey(searchPlan, selectedProvider);

        SearchResult cached = getCached(cacheKey);
        if (cached != null) {
            cached.setFromCache(true);
            return cached;
        }

        if (!enabled) {
            return fallbackResult(cacheKey, "WEBSEARCH_DISABLED", selectedProvider);
        }
        if (!hasProviderApiKey(selectedProvider)) {
            return fallbackResult(cacheKey, keyMissingReason(selectedProvider), selectedProvider);
        }

        try {
            List<SourceVO> sources = requestSearch(searchPlan, selectedProvider);
            if (sources.isEmpty()) {
                return fallbackResult(cacheKey, "NO_ALLOWED_SOURCES", selectedProvider);
            }

            SearchResult result = new SearchResult();
            result.setSources(sources);
            result.setRetrievedAt(LocalDateTime.now());
            result.setFallback(false);
            result.setFallbackReason(null);
            result.setProvider(selectedProvider);
            cache(cacheKey, result);
            return copyOf(result);
        } catch (Exception e) {
            log.warn("Web search failed, provider={}, error={}, message={}",
                    selectedProvider,
                    e.getClass().getSimpleName(),
                    safeErrorMessage(e));
            log.debug("Web search failure detail", e);
            return fallbackResult(cacheKey, "WEBSEARCH_UNAVAILABLE", selectedProvider);
        }
    }

    List<SourceVO> filterAndDeduplicate(List<SourceVO> rawSources) {
        if (rawSources == null || rawSources.isEmpty()) {
            return Collections.emptyList();
        }

        int limit = Math.max(1, Math.min(maxResults, 10));
        Set<String> seenUrls = new LinkedHashSet<>();
        Set<String> seenTitles = new LinkedHashSet<>();
        List<SourceVO> filtered = new ArrayList<>();

        for (SourceVO source : rawSources) {
            if (source == null || !hasText(source.getUrl())) {
                continue;
            }
            String domain = extractDomain(source.getUrl());
            if (!isAllowedDomain(domain)) {
                continue;
            }

            String normalizedUrl = normalizeUrl(source.getUrl());
            String titleKey = normalizeTitle(source.getTitle()) + "|" + domain;
            if (!seenUrls.add(normalizedUrl) || !seenTitles.add(titleKey)) {
                continue;
            }

            String snippet = cleanContent(firstText(source.getSnippet(), source.getContent()));
            String content = cleanContent(source.getContent());
            if (!hasText(content)) {
                content = snippet;
            }

            SourceVO cleaned = new SourceVO();
            cleaned.setTitle(abbreviate(source.getTitle(), 120));
            cleaned.setUrl(source.getUrl());
            cleaned.setDomain(domain);
            cleaned.setSnippet(abbreviate(snippet, 500));
            cleaned.setContent(abbreviate(content, resolveContentMaxChars()));
            cleaned.setPublishedAt(source.getPublishedAt());
            filtered.add(cleaned);

            if (filtered.size() >= limit) {
                break;
            }
        }
        return filtered;
    }

    List<SourceVO> parseFirecrawlSources(String responseBody) {
        if (!hasText(responseBody)) {
            return Collections.emptyList();
        }

        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        Object dataObj = responseJson.get("data");
        List<SourceVO> rawSources = new ArrayList<>();

        if (dataObj instanceof JSONArray dataArray) {
            addFirecrawlSources(dataArray, rawSources);
        } else if (dataObj instanceof JSONObject data) {
            addFirecrawlSources(data.getJSONArray("web"), rawSources);
            addFirecrawlSources(data.getJSONArray("news"), rawSources);
        }
        return filterAndDeduplicate(rawSources);
    }

    List<SourceVO> parseExaSources(String responseBody) {
        if (!hasText(responseBody)) {
            return Collections.emptyList();
        }

        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        JSONArray values = responseJson.getJSONArray("results");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        List<SourceVO> rawSources = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            JSONObject item = values.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String highlights = joinTextArray(item.getJSONArray("highlights"));
            String summary = item.getStr("summary");
            String text = item.getStr("text");

            SourceVO source = new SourceVO();
            source.setTitle(item.getStr("title"));
            source.setUrl(item.getStr("url"));
            source.setSnippet(firstText(summary, highlights, text));
            source.setContent(firstText(text, highlights, summary));
            source.setPublishedAt(item.getStr("publishedDate"));
            rawSources.add(source);
        }
        return filterAndDeduplicate(rawSources);
    }

    private List<SourceVO> requestSearch(SearchPlan searchPlan, String selectedProvider) {
        List<List<SourceVO>> sourceGroups = new ArrayList<>();
        Exception lastError = null;

        for (String searchTerms : searchPlan.getSearchQueries()) {
            try {
                List<SourceVO> sources = requestSingleSearch(searchTerms, selectedProvider, searchPlan.getFreshnessDays());
                if (sources != null && !sources.isEmpty()) {
                    sourceGroups.add(sources);
                }
            } catch (Exception e) {
                lastError = e;
                log.debug("Web search query variant failed, provider={}, query={}, error={}",
                        selectedProvider,
                        searchTerms,
                        e.getClass().getSimpleName());
            }
        }

        List<SourceVO> merged = interleaveSources(sourceGroups);
        if (merged.isEmpty() && lastError != null) {
            throw new IllegalStateException("All web search query variants failed", lastError);
        }
        return filterAndDeduplicate(merged);
    }

    private List<SourceVO> requestSingleSearch(String searchTerms, String selectedProvider, int effectiveFreshnessDays) {
        if (PROVIDER_FIRECRAWL_MCP.equals(selectedProvider) || PROVIDER_EXA_MCP.equals(selectedProvider)) {
            return requestMcpSearch(searchTerms, selectedProvider, effectiveFreshnessDays);
        }
        if (PROVIDER_FIRECRAWL.equals(selectedProvider)) {
            return requestFirecrawlSearch(searchTerms);
        }
        if (PROVIDER_EXA.equals(selectedProvider)) {
            return requestExaSearch(searchTerms, effectiveFreshnessDays);
        }
        return requestBingSearch(searchTerms, effectiveFreshnessDays);
    }

    private List<SourceVO> requestMcpSearch(String searchTerms, String selectedProvider, int effectiveFreshnessDays) {
        McpSearchConfig config = mcpSearchConfig(selectedProvider);
        String endpointUrl = buildMcpEndpointUrl(selectedProvider);
        Map<String, String> headers = buildMcpHeaders(selectedProvider);

        try (McpHttpClient client = new McpHttpClient(
                endpointUrl,
                headers,
                mcpProtocolVersion,
                resolveMcpRequestTimeoutMs()
        )) {
            client.initialize();
            List<McpStdioClient.McpTool> tools = client.listTools();
            McpStdioClient.McpTool tool = resolveMcpTool(config.getSearchTool(), config.getFallbackToolNames(), tools);
            JSONObject arguments = buildMcpSearchArguments(searchTerms, selectedProvider, tool, effectiveFreshnessDays);
            McpStdioClient.McpToolResult toolResult = client.callTool(tool.getName(), arguments);
            if (toolResult.isError()) {
                throw new IllegalStateException("MCP tool result error: " + summarizeMcpToolResult(toolResult));
            }
            List<SourceVO> sources = parseMcpSources(toolResult);
            return enrichMcpSources(client, selectedProvider, sources, tools);
        }
    }

    private McpSearchConfig mcpSearchConfig(String selectedProvider) {
        McpSearchConfig config = new McpSearchConfig();
        if (PROVIDER_FIRECRAWL_MCP.equals(selectedProvider)) {
            config.setSearchTool(firstText(firecrawlMcpSearchTool, "firecrawl_search"));
            config.setFallbackToolNames(List.of("firecrawl_search", "search"));
            return config;
        }

        config.setSearchTool(firstText(exaMcpSearchTool, "web_search_exa"));
        config.setFallbackToolNames(List.of("web_search_exa", "web_search_advanced_exa", "search"));
        return config;
    }

    private String buildMcpEndpointUrl(String selectedProvider) {
        if (PROVIDER_FIRECRAWL_MCP.equals(selectedProvider)) {
            String url = firstText(firecrawlMcpUrl, "https://mcp.firecrawl.dev/{apiKey}/v2/mcp");
            String encodedApiKey = URLEncoder.encode(firecrawlApiKey, StandardCharsets.UTF_8);
            if (url.contains("{apiKey}")) {
                return url.replace("{apiKey}", encodedApiKey);
            }
            if (url.contains("{FIRECRAWL_API_KEY}")) {
                return url.replace("{FIRECRAWL_API_KEY}", encodedApiKey);
            }
            return url;
        }
        return firstText(exaMcpUrl, "https://mcp.exa.ai/mcp");
    }

    private Map<String, String> buildMcpHeaders(String selectedProvider) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (PROVIDER_EXA_MCP.equals(selectedProvider) && hasText(exaApiKey) && hasText(exaMcpApiKeyHeader)) {
            headers.put(exaMcpApiKeyHeader, exaApiKey);
        }
        return headers;
    }

    private McpStdioClient.McpTool resolveMcpTool(String configuredToolName,
                                                  List<String> fallbackToolNames,
                                                  List<McpStdioClient.McpTool> tools) {
        List<McpStdioClient.McpTool> safeTools = tools == null ? List.of() : tools;
        for (McpStdioClient.McpTool tool : safeTools) {
            if (tool != null && Objects.equals(configuredToolName, tool.getName())) {
                return tool;
            }
        }
        for (String fallbackToolName : fallbackToolNames == null ? List.<String>of() : fallbackToolNames) {
            for (McpStdioClient.McpTool tool : safeTools) {
                if (tool != null && Objects.equals(fallbackToolName, tool.getName())) {
                    return tool;
                }
            }
        }

        throw new IllegalStateException("MCP search tool not found: configured="
                + configuredToolName
                + ", available="
                + availableToolNames(safeTools));
    }

    JSONObject buildMcpToolArguments(String normalizedQuery, String selectedProvider, McpStdioClient.McpTool tool) {
        String searchTerms = buildSearchTerms(normalizedQuery);
        return buildMcpSearchArguments(searchTerms, selectedProvider, tool, resolveEffectiveFreshnessDays(normalizedQuery));
    }

    private JSONObject buildMcpSearchArguments(String searchTerms,
                                               String selectedProvider,
                                               McpStdioClient.McpTool tool,
                                               int effectiveFreshnessDays) {
        JSONObject arguments = new JSONObject();
        JSONObject properties = schemaProperties(tool);

        String query = buildMcpSearchQuery(searchTerms, properties);
        setIfSchemaAllows(arguments, properties, "query", query);
        setIfSchemaAllows(arguments, properties, "q", query);

        int resultLimit = Math.max(1, Math.min(maxResults * 2, 10));
        setIfSchemaAllows(arguments, properties, "limit", resultLimit);
        setIfSchemaAllows(arguments, properties, "numResults", resultLimit);
        setIfSchemaAllows(arguments, properties, "maxResults", resultLimit);
        setIfSchemaAllows(arguments, properties, "count", resultLimit);

        JSONArray domains = toJsonArray(allowedDomains());
        if (!domains.isEmpty()) {
            setIfSchemaAllows(arguments, properties, "includeDomains", domains);
            setIfSchemaAllows(arguments, properties, "domains", domains);
            setIfSchemaAllows(arguments, properties, "allowedDomains", domains);
        }

        if (PROVIDER_FIRECRAWL_MCP.equals(selectedProvider)) {
            setIfSchemaAllows(arguments, properties, "sources", jsonArrayOf(sourceType("web")));
            setIfSchemaAllows(arguments, properties, "lang", "zh");
            setIfSchemaAllows(arguments, properties, "country", "cn");
            if (schemaAllows(properties, "scrapeOptions")) {
                JSONObject scrapeOptions = new JSONObject();
                scrapeOptions.set("formats", jsonArrayOf("markdown"));
                scrapeOptions.set("onlyMainContent", true);
                arguments.set("scrapeOptions", scrapeOptions);
            }
        }

        if (effectiveFreshnessDays > 0) {
            String startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(effectiveFreshnessDays).toString();
            setIfSchemaAllows(arguments, properties, "startPublishedDate", startDate);
            setIfSchemaAllows(arguments, properties, "startCrawlDate", startDate);
        }

        if (arguments.isEmpty()) {
            arguments.set("query", query);
        }
        return arguments;
    }

    private JSONObject sourceType(String type) {
        JSONObject source = new JSONObject();
        source.set("type", type);
        return source;
    }

    private JSONObject schemaProperties(McpStdioClient.McpTool tool) {
        if (tool == null || tool.getInputSchema() == null) {
            return new JSONObject();
        }
        JSONObject properties = tool.getInputSchema().getJSONObject("properties");
        return properties == null ? new JSONObject() : properties;
    }

    private String availableToolNames(List<McpStdioClient.McpTool> tools) {
        if (tools == null || tools.isEmpty()) {
            return "[]";
        }
        List<String> names = new ArrayList<>();
        for (McpStdioClient.McpTool tool : tools) {
            if (tool != null && hasText(tool.getName())) {
                names.add(tool.getName());
            }
        }
        return names.toString();
    }

    private String summarizeMcpToolResult(McpStdioClient.McpToolResult toolResult) {
        if (toolResult == null) {
            return "empty result";
        }
        String text = joinText(toolResult.getTextContents());
        if (hasText(text)) {
            return abbreviate(cleanContent(text), 500);
        }
        JSONObject rawResult = toolResult.getRawResult();
        return rawResult == null ? "isError=true" : abbreviate(rawResult.toString(), 500);
    }

    private String safeErrorMessage(Exception e) {
        if (e == null || !hasText(e.getMessage())) {
            return "";
        }
        return abbreviate(cleanContent(e.getMessage()), 500);
    }

    private List<SourceVO> enrichMcpSources(McpHttpClient client,
                                            String selectedProvider,
                                            List<SourceVO> sources,
                                            List<McpStdioClient.McpTool> tools) {
        if (client == null || sources == null || sources.isEmpty()) {
            return sources == null ? Collections.emptyList() : sources;
        }

        McpStdioClient.McpTool contentTool = resolveOptionalMcpTool(contentToolNames(selectedProvider), tools);
        if (contentTool == null) {
            return sources;
        }

        int enriched = 0;
        int enrichLimit = Math.max(1, Math.min(maxResults, sources.size()));
        for (SourceVO source : sources) {
            if (enriched >= enrichLimit) {
                break;
            }
            if (!shouldEnrichContent(source)) {
                continue;
            }
            try {
                JSONObject arguments = buildMcpContentArguments(source, selectedProvider, contentTool);
                if (arguments.isEmpty()) {
                    continue;
                }
                McpStdioClient.McpToolResult toolResult = client.callTool(contentTool.getName(), arguments);
                if (toolResult.isError()) {
                    log.debug("MCP content enrichment returned tool error, tool={}, source={}",
                            contentTool.getName(),
                            source.getUrl());
                    continue;
                }
                String content = extractBestMcpContent(toolResult);
                if (hasText(content) && content.length() > safeLength(source.getContent())) {
                    source.setContent(abbreviate(cleanContent(content), resolveContentMaxChars()));
                    if (!hasText(source.getSnippet()) || source.getSnippet().length() < 80) {
                        source.setSnippet(abbreviate(cleanContent(content), 500));
                    }
                    enriched++;
                }
            } catch (Exception e) {
                log.debug("MCP content enrichment failed, tool={}, source={}, error={}",
                        contentTool.getName(),
                        source.getUrl(),
                        e.getClass().getSimpleName());
            }
        }
        return sources;
    }

    private List<String> contentToolNames(String selectedProvider) {
        if (PROVIDER_FIRECRAWL_MCP.equals(selectedProvider)) {
            return List.of("firecrawl_scrape", "scrape", "firecrawl_scrape_url");
        }
        if (PROVIDER_EXA_MCP.equals(selectedProvider)) {
            return List.of("get_contents_exa", "contents_exa", "crawling", "crawl");
        }
        return List.of();
    }

    private McpStdioClient.McpTool resolveOptionalMcpTool(List<String> names, List<McpStdioClient.McpTool> tools) {
        if (names == null || names.isEmpty() || tools == null || tools.isEmpty()) {
            return null;
        }
        for (String name : names) {
            for (McpStdioClient.McpTool tool : tools) {
                if (tool != null && Objects.equals(name, tool.getName())) {
                    return tool;
                }
            }
        }
        return null;
    }

    JSONObject buildMcpContentArguments(SourceVO source, String selectedProvider, McpStdioClient.McpTool tool) {
        JSONObject arguments = new JSONObject();
        if (source == null || !hasText(source.getUrl())) {
            return arguments;
        }

        JSONObject properties = schemaProperties(tool);
        String url = source.getUrl();
        if (properties.isEmpty()) {
            arguments.set("url", url);
        } else {
            setIfSchemaAllows(arguments, properties, "url", url);
            setIfSchemaAllows(arguments, properties, "sourceURL", url);
            setIfSchemaAllows(arguments, properties, "sourceUrl", url);
            setIfSchemaAllows(arguments, properties, "urls", jsonArrayOf(url));
        }

        if (PROVIDER_FIRECRAWL_MCP.equals(selectedProvider)) {
            setIfSchemaAllows(arguments, properties, "formats", jsonArrayOf("markdown"));
            setIfSchemaAllows(arguments, properties, "onlyMainContent", true);
            if (schemaAllows(properties, "scrapeOptions")) {
                JSONObject scrapeOptions = new JSONObject();
                scrapeOptions.set("formats", jsonArrayOf("markdown"));
                scrapeOptions.set("onlyMainContent", true);
                arguments.set("scrapeOptions", scrapeOptions);
            }
        }

        return arguments;
    }

    private boolean shouldEnrichContent(SourceVO source) {
        if (source == null || !hasText(source.getUrl())) {
            return false;
        }
        String content = cleanContent(source.getContent());
        if (!hasText(content)) {
            return true;
        }
        return content.length() < Math.min(900, resolveContentMaxChars() / 2)
                || looksLikeNavigationContent(content);
    }

    private boolean looksLikeNavigationContent(String content) {
        if (!hasText(content)) {
            return true;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String term : List.of("首页", "导航", "登录", "注册", "版权", "备案", "当前位置", "search", "menu")) {
            if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                hits++;
            }
        }
        return hits >= 3 && content.length() < 1500;
    }

    private String extractBestMcpContent(McpStdioClient.McpToolResult toolResult) {
        if (toolResult == null) {
            return "";
        }
        List<String> candidates = new ArrayList<>();
        collectTextCandidates(toolResult.getStructuredContent(), candidates);
        collectTextCandidates(toolResult.getRawResult(), candidates);
        for (String text : toolResult.getTextContents() == null ? List.<String>of() : toolResult.getTextContents()) {
            if (!hasText(text)) {
                continue;
            }
            try {
                collectTextCandidates(JSONUtil.parse(text), candidates);
            } catch (Exception ignored) {
                candidates.add(text);
            }
        }
        return longestCleanText(candidates);
    }

    private void collectTextCandidates(Object value, List<String> candidates) {
        if (value == null) {
            return;
        }
        if (value instanceof JSONArray array) {
            for (int i = 0; i < array.size(); i++) {
                collectTextCandidates(array.get(i), candidates);
            }
            return;
        }
        if (!(value instanceof JSONObject object)) {
            if (value instanceof String text && hasText(text)) {
                candidates.add(text);
            }
            return;
        }

        for (String key : List.of("markdown", "text", "content", "contents", "summary", "description", "html", "rawHtml")) {
            Object candidate = object.get(key);
            if (candidate instanceof String text && hasText(text)) {
                candidates.add(text);
            }
        }
        for (String key : object.keySet()) {
            Object child = object.get(key);
            if (child instanceof JSONObject || child instanceof JSONArray) {
                collectTextCandidates(child, candidates);
            }
        }
    }

    private String longestCleanText(List<String> candidates) {
        String best = "";
        for (String candidate : candidates == null ? List.<String>of() : candidates) {
            String cleaned = cleanContent(candidate);
            if (cleaned.length() > best.length()) {
                best = cleaned;
            }
        }
        return best;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private void setIfSchemaAllows(JSONObject arguments, JSONObject properties, String key, Object value) {
        if (schemaAllows(properties, key)) {
            arguments.set(key, value);
        }
    }

    private boolean schemaAllows(JSONObject properties, String key) {
        return properties == null || properties.isEmpty() || properties.containsKey(key);
    }

    private String buildMcpSearchQuery(String searchTerms, JSONObject properties) {
        if (properties != null
                && (properties.containsKey("includeDomains")
                || properties.containsKey("domains")
                || properties.containsKey("allowedDomains"))) {
            return searchTerms;
        }
        return buildBingSearchQuery(searchTerms);
    }

    List<SourceVO> parseMcpSources(McpStdioClient.McpToolResult toolResult) {
        if (toolResult == null) {
            return Collections.emptyList();
        }

        List<SourceVO> rawSources = new ArrayList<>();
        addGenericSources(toolResult.getStructuredContent(), rawSources);
        addGenericSources(toolResult.getRawResult(), rawSources);

        for (String text : toolResult.getTextContents() == null ? List.<String>of() : toolResult.getTextContents()) {
            if (!hasText(text)) {
                continue;
            }
            boolean parsedJson = false;
            try {
                Object parsed = JSONUtil.parse(text);
                addGenericSources(parsed, rawSources);
                parsedJson = true;
            } catch (Exception ignored) {
                // Text MCP results are common; parse them heuristically below.
            }
            if (!parsedJson) {
                rawSources.addAll(parseTextSources(text));
            }
        }
        return filterAndDeduplicate(rawSources);
    }

    private void addGenericSources(Object value, List<SourceVO> rawSources) {
        if (value == null) {
            return;
        }
        if (value instanceof JSONArray array) {
            for (int i = 0; i < array.size(); i++) {
                addGenericSources(array.get(i), rawSources);
            }
            return;
        }
        if (!(value instanceof JSONObject object)) {
            return;
        }

        SourceVO source = sourceFromJson(object);
        if (source != null) {
            rawSources.add(source);
        }
        for (String key : object.keySet()) {
            Object child = object.get(key);
            if (child instanceof JSONObject || child instanceof JSONArray) {
                addGenericSources(child, rawSources);
            }
        }
    }

    private SourceVO sourceFromJson(JSONObject object) {
        String url = jsonText(object, "url", "link", "sourceURL", "sourceUrl", "source_url");
        if (!hasText(url)) {
            JSONObject metadata = object.getJSONObject("metadata");
            url = jsonText(metadata, "url", "sourceURL", "sourceUrl", "source_url");
        }
        if (!hasText(url)) {
            return null;
        }

        JSONObject metadata = object.getJSONObject("metadata");
        SourceVO source = new SourceVO();
        source.setTitle(firstText(
                jsonText(object, "title", "name", "headline"),
                jsonText(metadata, "title", "name", "headline")
        ));
        source.setUrl(url);
        source.setSnippet(firstText(
                jsonText(object, "description", "snippet", "summary", "excerpt"),
                jsonText(metadata, "description", "snippet", "summary")
        ));
        source.setContent(firstText(
                jsonText(object, "markdown", "text", "content", "summary", "description", "snippet"),
                jsonText(metadata, "markdown", "text", "content", "summary", "description")
        ));
        source.setPublishedAt(firstText(
                jsonText(object, "publishedDate", "publishedAt", "datePublished", "date", "published_date"),
                jsonText(metadata, "publishedDate", "publishedAt", "datePublished", "date", "published_date")
        ));
        return source;
    }

    private String jsonText(JSONObject object, String... keys) {
        if (object == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            Object value = object.get(key);
            if (value instanceof String text && hasText(text)) {
                return text;
            }
            if (value instanceof Number || value instanceof Boolean) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private List<SourceVO> parseTextSources(String text) {
        List<SourceVO> rawSources = new ArrayList<>();
        Matcher markdownMatcher = MARKDOWN_LINK_PATTERN.matcher(text);
        while (markdownMatcher.find()) {
            SourceVO source = new SourceVO();
            source.setTitle(markdownMatcher.group(1));
            source.setUrl(markdownMatcher.group(2));
            source.setSnippet(snippetAround(text, markdownMatcher.start()));
            source.setContent(snippetAround(text, markdownMatcher.start()));
            rawSources.add(source);
        }

        Matcher urlMatcher = URL_PATTERN.matcher(text);
        while (urlMatcher.find()) {
            String url = trimTrailingUrlPunctuation(urlMatcher.group());
            boolean alreadyAdded = false;
            for (SourceVO source : rawSources) {
                if (Objects.equals(normalizeUrl(source.getUrl()), normalizeUrl(url))) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (alreadyAdded) {
                continue;
            }
            SourceVO source = new SourceVO();
            source.setTitle(titleNear(text, urlMatcher.start()));
            source.setUrl(url);
            source.setSnippet(snippetAround(text, urlMatcher.start()));
            source.setContent(snippetAround(text, urlMatcher.start()));
            rawSources.add(source);
        }
        return rawSources;
    }

    private String trimTrailingUrlPunctuation(String url) {
        String trimmed = url == null ? "" : url.trim();
        while (trimmed.endsWith(".") || trimmed.endsWith(",") || trimmed.endsWith(";") || trimmed.endsWith("。")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String titleNear(String text, int index) {
        int start = Math.max(0, index - 160);
        String prefix = text.substring(start, index).replaceAll("\\s+", " ").trim();
        if (!hasText(prefix)) {
            return "MCP 来源";
        }
        int separator = Math.max(prefix.lastIndexOf("标题："), prefix.lastIndexOf("Title:"));
        if (separator >= 0) {
            prefix = prefix.substring(separator).replaceFirst("^(标题：|Title:)\\s*", "");
        }
        return abbreviate(prefix, 120);
    }

    private String snippetAround(String text, int index) {
        int start = Math.max(0, index - 240);
        int end = Math.min(text.length(), index + 480);
        return cleanContent(text.substring(start, end));
    }

    private List<SourceVO> requestBingSearch(String searchTerms, int effectiveFreshnessDays) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("q", buildBingSearchQuery(searchTerms));
        params.put("count", Math.max(1, Math.min(maxResults * 2, 10)));
        params.put("mkt", "zh-CN");
        String bingFreshness = resolveBingFreshness(effectiveFreshnessDays);
        if (hasText(bingFreshness)) {
            params.put("freshness", bingFreshness);
        }
        params.put("textDecorations", false);
        params.put("textFormat", "Raw");

        HttpResponse response = HttpUtil.createGet(apiUrl)
                .form(params)
                .header(apiKeyHeader, apiKey)
                .timeout(resolveTimeoutMs())
                .execute();

        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new IllegalStateException("search status " + response.getStatus());
        }

        JSONObject responseJson = JSONUtil.parseObj(response.body());
        JSONObject webPages = responseJson.getJSONObject("webPages");
        JSONArray values = webPages == null ? null : webPages.getJSONArray("value");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        List<SourceVO> rawSources = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            JSONObject item = values.getJSONObject(i);
            SourceVO source = new SourceVO();
            source.setTitle(item.getStr("name"));
            source.setUrl(item.getStr("url"));
            source.setSnippet(item.getStr("snippet"));
            source.setPublishedAt(item.getStr("datePublished"));
            rawSources.add(source);
        }
        return filterAndDeduplicate(rawSources);
    }

    private List<SourceVO> requestFirecrawlSearch(String searchTerms) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("query", searchTerms);
        requestBody.set("limit", Math.max(1, Math.min(maxResults * 2, 10)));
        requestBody.set("sources", jsonArrayOf("web"));

        List<String> domains = allowedDomains();
        if (!domains.isEmpty()) {
            requestBody.set("includeDomains", toJsonArray(domains));
        }

        if (firecrawlScrapeContent) {
            JSONObject format = new JSONObject();
            format.set("type", "markdown");
            JSONObject scrapeOptions = new JSONObject();
            scrapeOptions.set("formats", jsonArrayOf(format));
            requestBody.set("scrapeOptions", scrapeOptions);
        }

        HttpResponse response = HttpUtil.createPost(firecrawlApiUrl)
                .header("Authorization", "Bearer " + firecrawlApiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(resolveTimeoutMs())
                .execute();

        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new IllegalStateException("firecrawl search status " + response.getStatus());
        }
        return parseFirecrawlSources(response.body());
    }

    private List<SourceVO> requestExaSearch(String searchTerms, int effectiveFreshnessDays) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("query", searchTerms);
        requestBody.set("numResults", Math.max(1, Math.min(maxResults * 2, 10)));

        List<String> domains = allowedDomains();
        if (!domains.isEmpty()) {
            requestBody.set("includeDomains", toJsonArray(domains));
        }
        if (effectiveFreshnessDays > 0) {
            requestBody.set("startPublishedDate", OffsetDateTime.now(ZoneOffset.UTC)
                    .minusDays(effectiveFreshnessDays)
                    .toString());
        }

        JSONObject text = new JSONObject();
        text.set("verbosity", normalizeExaContentVerbosity());
        text.set("includeSections", jsonArrayOf("body"));

        JSONObject highlights = new JSONObject();
        highlights.set("query", searchTerms);
        highlights.set("maxCharacters", Math.min(resolveContentMaxChars(), 2000));

        JSONObject contents = new JSONObject();
        contents.set("text", text);
        contents.set("highlights", highlights);
        contents.set("maxAgeHours", resolveExaMaxAgeHours(effectiveFreshnessDays));
        requestBody.set("contents", contents);

        HttpResponse response = HttpUtil.createPost(exaApiUrl)
                .header("x-api-key", exaApiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(resolveTimeoutMs())
                .execute();

        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new IllegalStateException("exa search status " + response.getStatus());
        }
        return parseExaSources(response.body());
    }

    private void addFirecrawlSources(JSONArray values, List<SourceVO> rawSources) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            JSONObject item = values.getJSONObject(i);
            if (item == null) {
                continue;
            }

            JSONObject metadata = item.getJSONObject("metadata");
            String description = firstText(
                    item.getStr("description"),
                    item.getStr("snippet"),
                    metadataStr(metadata, "description")
            );

            SourceVO source = new SourceVO();
            source.setTitle(firstText(item.getStr("title"), metadataStr(metadata, "title")));
            source.setUrl(firstText(
                    item.getStr("url"),
                    item.getStr("sourceURL"),
                    metadataStr(metadata, "sourceURL"),
                    metadataStr(metadata, "url")
            ));
            source.setSnippet(description);
            source.setContent(firstText(
                    item.getStr("markdown"),
                    item.getStr("summary"),
                    item.getStr("html"),
                    item.getStr("rawHtml"),
                    description
            ));
            source.setPublishedAt(firstText(
                    item.getStr("date"),
                    item.getStr("publishedDate"),
                    metadataStr(metadata, "publishedTime"),
                    metadataStr(metadata, "date")
            ));
            rawSources.add(source);
        }
    }

    private String buildBingSearchQuery(String searchTerms) {
        List<String> domains = allowedDomains();
        if (domains.isEmpty()) {
            return searchTerms;
        }
        StringBuilder query = new StringBuilder(searchTerms).append(" (");
        for (int i = 0; i < domains.size(); i++) {
            if (i > 0) {
                query.append(" OR ");
            }
            query.append("site:").append(domains.get(i));
        }
        return query.append(")").toString();
    }

    String buildSearchTerms(String normalizedQuery) {
        List<String> queries = buildSearchQueries(normalizedQuery, LocalDate.now());
        return queries.isEmpty() ? normalizeQuery(normalizedQuery) : queries.get(0);
    }

    List<String> buildSearchQueries(String normalizedQuery, LocalDate today) {
        String baseQuery = normalizeQuery(normalizedQuery);
        LocalDate safeToday = today == null ? LocalDate.now() : today;
        String monthLabel = safeToday.getYear() + "年" + safeToday.getMonthValue() + "月";
        String dayLabel = monthLabel + safeToday.getDayOfMonth() + "日";
        String timeLabel = wantsSameDaySearch(baseQuery) ? dayLabel : monthLabel;

        LinkedHashSet<String> queries = new LinkedHashSet<>();
        queries.add(cleanSearchTerms(baseQuery + " " + timeLabel + " 最新 通报 预警 诈骗 反诈"));

        if (isGenericRecentFraudQuery(baseQuery)) {
            queries.add(cleanSearchTerms(timeLabel + " 近期 高发 电信网络诈骗 类型 反诈中心 公安 通报"));
            queries.add(cleanSearchTerms(timeLabel + " 新型 诈骗手法 预警 刷单返利 冒充客服 虚假投资 虚假贷款"));
        } else {
            queries.add(cleanSearchTerms(baseQuery + " " + timeLabel + " 反诈中心 公安 预警 案例"));
        }

        return queries.stream()
                .filter(this::hasText)
                .limit(MAX_SEARCH_QUERY_VARIANTS)
                .toList();
    }

    private SearchPlan buildSearchPlan(String question) {
        String normalizedQuery = normalizeQuery(question);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        SearchPlan plan = new SearchPlan();
        plan.setNormalizedQuery(normalizedQuery);
        plan.setSearchQueries(buildSearchQueries(normalizedQuery, today));
        plan.setFreshnessDays(resolveEffectiveFreshnessDays(normalizedQuery));
        plan.setCacheBucket(today + "-h" + now.getHour());
        return plan;
    }

    int resolveEffectiveFreshnessDays(String normalizedQuery) {
        if (freshnessDays <= 0) {
            return 0;
        }

        int configuredDays = Math.max(1, freshnessDays);
        String normalized = normalizeQuery(normalizedQuery);
        if (containsAny(normalized, List.of("今天", "今日"))) {
            return Math.min(configuredDays, 2);
        }
        if (containsAny(normalized, List.of("昨天", "昨日", "近两天", "这两天"))) {
            return Math.min(configuredDays, 3);
        }
        if (containsAny(normalized, List.of("本周", "这周", "近一周", "近7天", "近七天"))) {
            return Math.min(configuredDays, 7);
        }
        if (containsAny(normalized, RECENT_TIME_TERMS)) {
            return Math.min(configuredDays, 14);
        }
        if (containsAny(normalized, List.of("本月", "这个月", "近一月", "近30天", "近三十天"))) {
            return Math.min(configuredDays, 30);
        }
        return configuredDays;
    }

    private boolean wantsSameDaySearch(String normalizedQuery) {
        return containsAny(normalizedQuery, List.of("今天", "今日"));
    }

    private boolean isGenericRecentFraudQuery(String normalizedQuery) {
        String normalized = normalizeQuery(normalizedQuery);
        return containsAny(normalized, RECENT_TIME_TERMS)
                && !containsAny(normalized, SPECIFIC_FRAUD_TERMS);
    }

    private boolean containsAny(String text, List<String> terms) {
        if (!hasText(text)) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String term : terms == null ? List.<String>of() : terms) {
            if (hasText(term) && normalized.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String cleanSearchTerms(String value) {
        return abbreviate(value == null ? "" : value.replaceAll("\\s+", " ").trim(), 220);
    }

    private List<SourceVO> interleaveSources(List<List<SourceVO>> sourceGroups) {
        if (sourceGroups == null || sourceGroups.isEmpty()) {
            return Collections.emptyList();
        }

        int maxSize = 0;
        for (List<SourceVO> group : sourceGroups) {
            maxSize = Math.max(maxSize, group == null ? 0 : group.size());
        }

        List<SourceVO> merged = new ArrayList<>();
        for (int index = 0; index < maxSize; index++) {
            for (List<SourceVO> group : sourceGroups) {
                if (group != null && index < group.size()) {
                    merged.add(group.get(index));
                }
            }
        }
        return merged;
    }

    private String resolveBingFreshness(int effectiveFreshnessDays) {
        if (effectiveFreshnessDays <= 0) {
            return "";
        }
        if (effectiveFreshnessDays <= 2) {
            return "Day";
        }
        if (effectiveFreshnessDays <= 7) {
            return "Week";
        }
        return "Month";
    }

    private int resolveExaMaxAgeHours(int effectiveFreshnessDays) {
        int configuredHours = Math.max(0, exaMaxAgeHours);
        if (effectiveFreshnessDays <= 0) {
            return configuredHours;
        }
        int effectiveHours = effectiveFreshnessDays * 24;
        return configuredHours == 0 ? effectiveHours : Math.min(configuredHours, effectiveHours);
    }

    private SearchResult getCached(String cacheKey) {
        SearchResult redisResult = getRedisCache(cacheKey);
        if (redisResult != null) {
            return redisResult;
        }
        return getLocalCache(cacheKey, false);
    }

    private SearchResult getRedisCache(String cacheKey) {
        if (redisCacheUtil == null) {
            return null;
        }
        try {
            Object cached = redisCacheUtil.get(cacheKey);
            if (cached == null) {
                return null;
            }
            if (cached instanceof SearchResult result) {
                return copyOf(result);
            }
            return JSONUtil.toBean(JSONUtil.toJsonStr(cached), SearchResult.class);
        } catch (Exception e) {
            log.debug("Read websearch Redis cache failed, key={}", cacheKey);
            return null;
        }
    }

    private SearchResult getLocalCache(String cacheKey, boolean allowExpired) {
        LocalCacheEntry entry = localCache.get(cacheKey);
        if (entry == null || entry.getResult() == null) {
            return null;
        }
        if (!allowExpired && entry.getExpireAtEpochMillis() < System.currentTimeMillis()) {
            localCache.remove(cacheKey);
            return null;
        }
        return copyOf(entry.getResult());
    }

    private void cache(String cacheKey, SearchResult result) {
        long ttl = Math.max(60, cacheTtlSeconds);
        localCache.put(cacheKey, new LocalCacheEntry(copyOf(result), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttl)));
        if (redisCacheUtil != null) {
            redisCacheUtil.set(cacheKey, copyOf(result), ttl, TimeUnit.SECONDS);
        }
    }

    private SearchResult fallbackResult(String cacheKey, String reason, String selectedProvider) {
        SearchResult stale = getLocalCache(cacheKey, true);
        if (stale != null) {
            stale.setFallback(true);
            stale.setFromCache(true);
            stale.setFallbackReason(reason);
            stale.setProvider(selectedProvider);
            return stale;
        }

        SearchResult result = new SearchResult();
        result.setSources(fallbackSources());
        result.setRetrievedAt(LocalDateTime.now());
        result.setFallback(true);
        result.setFallbackReason(reason);
        result.setProvider(selectedProvider);
        return result;
    }

    private List<SourceVO> fallbackSources() {
        List<SourceVO> sources = new ArrayList<>();
        sources.add(source("公安部", "https://www.mps.gov.cn/", "mps.gov.cn", "公安机关官方信息发布与反诈提示核验入口。"));
        sources.add(source("12321网络不良与垃圾信息举报受理中心", "https://www.12321.cn/", "12321.cn", "诈骗电话、短信、网站和App线索举报渠道。"));
        sources.add(source("中国政府网", "https://www.gov.cn/", "gov.cn", "国务院和政府部门政策、通报信息核验入口。"));
        return filterAndDeduplicate(sources);
    }

    private SourceVO source(String title, String url, String domain, String snippet) {
        SourceVO source = new SourceVO();
        source.setTitle(title);
        source.setUrl(url);
        source.setDomain(domain);
        source.setSnippet(snippet);
        return source;
    }

    private SearchResult copyOf(SearchResult result) {
        SearchResult copy = new SearchResult();
        copy.setSources(result.getSources() == null ? new ArrayList<>() : new ArrayList<>(result.getSources()));
        copy.setRetrievedAt(result.getRetrievedAt());
        copy.setFallback(result.getFallback());
        copy.setFromCache(result.getFromCache());
        copy.setFallbackReason(result.getFallbackReason());
        copy.setProvider(result.getProvider());
        return copy;
    }

    private String buildCacheKey(SearchPlan searchPlan, String selectedProvider) {
        return CACHE_PREFIX + Integer.toHexString(Objects.hash(
                searchPlan == null ? "" : searchPlan.getNormalizedQuery(),
                searchPlan == null ? List.of() : searchPlan.getSearchQueries(),
                searchPlan == null ? 0 : searchPlan.getFreshnessDays(),
                searchPlan == null ? "" : searchPlan.getCacheBucket(),
                allowedDomainsConfig,
                selectedProvider,
                maxResults,
                resolveContentMaxChars()
        ));
    }

    private String normalizeQuery(String question) {
        String normalized = question == null ? "" : question.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "最新诈骗通报" : abbreviate(normalized, 160);
    }

    private String resolveProvider() {
        String normalizedProvider = normalizeProvider(provider);
        if (!PROVIDER_AUTO.equals(normalizedProvider)) {
            return normalizedProvider;
        }
        if (mcpEnabled && hasText(firecrawlApiKey)) {
            return PROVIDER_FIRECRAWL_MCP;
        }
        if (mcpEnabled && hasText(exaApiKey)) {
            return PROVIDER_EXA_MCP;
        }
        if (hasText(firecrawlApiKey)) {
            return PROVIDER_FIRECRAWL;
        }
        if (hasText(exaApiKey)) {
            return PROVIDER_EXA;
        }
        return PROVIDER_BING;
    }

    private String normalizeProvider(String value) {
        if (!hasText(value)) {
            return PROVIDER_AUTO;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case PROVIDER_FIRECRAWL_MCP, "mcp-firecrawl", "mcp:firecrawl" -> PROVIDER_FIRECRAWL_MCP;
            case PROVIDER_EXA_MCP, "mcp-exa", "mcp:exa" -> PROVIDER_EXA_MCP;
            case PROVIDER_FIRECRAWL -> PROVIDER_FIRECRAWL;
            case PROVIDER_EXA -> PROVIDER_EXA;
            case PROVIDER_BING, "bing-web-search", "bing-compatible" -> PROVIDER_BING;
            case PROVIDER_AUTO -> PROVIDER_AUTO;
            default -> PROVIDER_BING;
        };
    }

    private boolean hasProviderApiKey(String selectedProvider) {
        if (PROVIDER_FIRECRAWL.equals(selectedProvider) || PROVIDER_FIRECRAWL_MCP.equals(selectedProvider)) {
            return hasText(firecrawlApiKey);
        }
        if (PROVIDER_EXA.equals(selectedProvider) || PROVIDER_EXA_MCP.equals(selectedProvider)) {
            return hasText(exaApiKey);
        }
        return hasText(apiKey);
    }

    private String keyMissingReason(String selectedProvider) {
        if (PROVIDER_FIRECRAWL.equals(selectedProvider) || PROVIDER_FIRECRAWL_MCP.equals(selectedProvider)) {
            return "FIRECRAWL_API_KEY_MISSING";
        }
        if (PROVIDER_EXA.equals(selectedProvider) || PROVIDER_EXA_MCP.equals(selectedProvider)) {
            return "EXA_API_KEY_MISSING";
        }
        return "WEBSEARCH_API_KEY_MISSING";
    }

    private List<String> allowedDomains() {
        if (!hasText(allowedDomainsConfig)) {
            return Collections.emptyList();
        }
        String[] parts = allowedDomainsConfig.split(",");
        List<String> domains = new ArrayList<>();
        for (String part : parts) {
            String domain = part == null ? "" : part.trim().toLowerCase(Locale.ROOT);
            if (!domain.isEmpty()) {
                domains.add(domain);
            }
        }
        return domains;
    }

    private boolean isAllowedDomain(String domain) {
        if (!hasText(domain)) {
            return false;
        }
        String normalizedDomain = domain.toLowerCase(Locale.ROOT);
        for (String allowedDomain : allowedDomains()) {
            if (normalizedDomain.equals(allowedDomain) || normalizedDomain.endsWith("." + allowedDomain)) {
                return true;
            }
        }
        return false;
    }

    private String extractDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return "";
            }
            return host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeUrl(String url) {
        String normalized = url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private int resolveTimeoutMs() {
        return Math.max(1000, Math.min(timeoutMs <= 0 ? DEFAULT_TIMEOUT_MS : timeoutMs, 30000));
    }

    private int resolveMcpRequestTimeoutMs() {
        return Math.max(3000, Math.min(mcpRequestTimeoutMs <= 0 ? 20000 : mcpRequestTimeoutMs, 60000));
    }

    private int resolveContentMaxChars() {
        return Math.max(300, Math.min(contentMaxChars <= 0 ? DEFAULT_CONTENT_MAX_CHARS : contentMaxChars, 4000));
    }

    private String normalizeExaContentVerbosity() {
        if (!hasText(exaContentVerbosity)) {
            return "compact";
        }
        String normalized = exaContentVerbosity.trim().toLowerCase(Locale.ROOT);
        if ("standard".equals(normalized) || "full".equals(normalized)) {
            return normalized;
        }
        return "compact";
    }

    private JSONArray toJsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        if (values == null) {
            return array;
        }
        for (String value : values) {
            if (hasText(value)) {
                array.add(value.trim());
            }
        }
        return array;
    }

    private JSONArray jsonArrayOf(Object... values) {
        JSONArray array = new JSONArray();
        if (values == null) {
            return array;
        }
        for (Object value : values) {
            if (value != null) {
                array.add(value);
            }
        }
        return array;
    }

    private String joinTextArray(JSONArray values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            String value = values.getStr(i);
            if (!hasText(value)) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append("\n");
            }
            joined.append(value.trim());
        }
        return joined.toString();
    }

    private String joinText(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append("\n");
            }
            joined.append(value.trim());
        }
        return joined.toString();
    }

    private String metadataStr(JSONObject metadata, String key) {
        return metadata == null ? "" : metadata.getStr(key);
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String cleanContent(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    @Data
    private static class LocalCacheEntry {
        private final SearchResult result;
        private final long expireAtEpochMillis;
    }

    @Data
    private static class McpSearchConfig {
        private String searchTool;
        private List<String> fallbackToolNames = new ArrayList<>();
    }

    @Data
    private static class SearchPlan {
        private String normalizedQuery;
        private List<String> searchQueries = new ArrayList<>();
        private int freshnessDays;
        private String cacheBucket;
    }

    @Data
    public static class SearchResult implements Serializable {
        private List<SourceVO> sources = new ArrayList<>();
        private LocalDateTime retrievedAt;
        private Boolean fallback = false;
        private Boolean fromCache = false;
        private String fallbackReason;
        private String provider;

        private static final long serialVersionUID = 1L;
    }
}
