package com.anti.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void parseLineSupportsCommonDotenvSyntax() {
        assertThat(DotenvEnvironmentPostProcessor.parseLine("DEEPSEEK_API_KEY=sk-test", 1))
                .containsEntry("DEEPSEEK_API_KEY", "sk-test");
        assertThat(DotenvEnvironmentPostProcessor.parseLine("export EXA_API_KEY='exa key'", 2))
                .containsEntry("EXA_API_KEY", "exa key");
        assertThat(DotenvEnvironmentPostProcessor.parseLine("WEBSEARCH_PROVIDER=auto # local default", 3))
                .containsEntry("WEBSEARCH_PROVIDER", "auto");
        assertThat(DotenvEnvironmentPostProcessor.parseLine("DEEPSEEK_MODEL=\"deepseek-chat\"", 4))
                .containsEntry("DEEPSEEK_MODEL", "deepseek-chat");
        assertThat(DotenvEnvironmentPostProcessor.parseLine("# comment", 5)).isEmpty();
    }

    @Test
    void loadDotenvFilesLetsLaterFilesOverrideEarlierFiles() throws Exception {
        Path rootEnv = tempDir.resolve(".env");
        Path backendEnv = tempDir.resolve("backend.env");
        Files.writeString(rootEnv, """
                DEEPSEEK_API_KEY=root-key
                WEBSEARCH_PROVIDER=bing
                """, StandardCharsets.UTF_8);
        Files.writeString(backendEnv, """
                DEEPSEEK_API_KEY=backend-key
                FIRECRAWL_API_KEY=fc-key
                """, StandardCharsets.UTF_8);

        Map<String, Object> properties = DotenvEnvironmentPostProcessor.loadDotenvFiles(List.of(rootEnv, backendEnv));

        assertThat(properties)
                .containsEntry("DEEPSEEK_API_KEY", "backend-key")
                .containsEntry("WEBSEARCH_PROVIDER", "bing")
                .containsEntry("FIRECRAWL_API_KEY", "fc-key");
    }
}
