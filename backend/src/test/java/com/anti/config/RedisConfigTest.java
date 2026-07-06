package com.anti.config;

import com.anti.util.WebSearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    void jsonSerializerSupportsLocalDateTimeValues() {
        GenericJackson2JsonRedisSerializer serializer = RedisConfig.jsonSerializer();
        WebSearchClient.SearchResult result = new WebSearchClient.SearchResult();
        result.setRetrievedAt(LocalDateTime.of(2026, 7, 5, 15, 50));
        result.setProvider("firecrawl-mcp");

        byte[] bytes = serializer.serialize(result);
        Object restored = serializer.deserialize(bytes);

        assertThat(restored).isInstanceOf(WebSearchClient.SearchResult.class);
        WebSearchClient.SearchResult restoredResult = (WebSearchClient.SearchResult) restored;
        assertThat(restoredResult.getRetrievedAt()).isEqualTo(LocalDateTime.of(2026, 7, 5, 15, 50));
        assertThat(restoredResult.getProvider()).isEqualTo("firecrawl-mcp");
    }
}
