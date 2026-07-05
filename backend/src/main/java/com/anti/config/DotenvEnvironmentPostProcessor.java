package com.anti.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads local .env files before Spring binds application properties.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "localDotenv";
    private static final int ORDER_BEFORE_CONFIG_DATA = Ordered.HIGHEST_PRECEDENCE + 9;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (isDisabled()) {
            return;
        }

        Map<String, Object> properties = loadDotenvFiles(resolveDotenvFiles());
        if (properties.isEmpty()) {
            return;
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.replace(PROPERTY_SOURCE_NAME, new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
            return;
        }

        MapPropertySource dotenvSource = new MapPropertySource(PROPERTY_SOURCE_NAME, properties);
        if (propertySources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, dotenvSource);
        } else if (propertySources.contains(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
            propertySources.addAfter(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, dotenvSource);
        } else {
            propertySources.addFirst(dotenvSource);
        }
    }

    @Override
    public int getOrder() {
        return ORDER_BEFORE_CONFIG_DATA;
    }

    static Map<String, Object> loadDotenvFiles(List<Path> files) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Path file : files) {
            if (file == null || !Files.isRegularFile(file) || !Files.isReadable(file)) {
                continue;
            }
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    parseLine(line, lineNumber).forEach(properties::put);
                }
            } catch (IOException ignored) {
                // Ignore unreadable local env files; explicit environment variables still work.
            }
        }
        return properties;
    }

    static Map<String, Object> parseLine(String rawLine, int lineNumber) {
        String line = rawLine == null ? "" : rawLine;
        if (lineNumber == 1 && line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return Map.of();
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }

        int equalsIndex = line.indexOf('=');
        if (equalsIndex <= 0) {
            return Map.of();
        }

        String key = line.substring(0, equalsIndex).trim();
        if (!isValidKey(key)) {
            return Map.of();
        }

        String value = line.substring(equalsIndex + 1).trim();
        return Map.of(key, parseValue(value));
    }

    private List<Path> resolveDotenvFiles() {
        Set<Path> files = new LinkedHashSet<>();
        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        String dirName = userDir.getFileName() == null ? "" : userDir.getFileName().toString();

        if ("backend".equalsIgnoreCase(dirName) && userDir.getParent() != null) {
            files.add(userDir.getParent().resolve(".env").normalize());
            files.add(userDir.resolve(".env").normalize());
        } else {
            files.add(userDir.resolve(".env").normalize());
            files.add(userDir.resolve("backend").resolve(".env").normalize());
        }

        String explicitPath = firstText(System.getProperty("dotenv.path"), System.getenv("DOTENV_PATH"));
        if (explicitPath != null) {
            files.add(Paths.get(explicitPath).toAbsolutePath().normalize());
        }
        return new ArrayList<>(files);
    }

    private boolean isDisabled() {
        String enabled = firstText(System.getProperty("dotenv.enabled"), System.getenv("DOTENV_ENABLED"));
        return enabled != null && "false".equalsIgnoreCase(enabled.trim());
    }

    private static String parseValue(String value) {
        if (value.isEmpty()) {
            return "";
        }
        if (isWrapped(value, '"')) {
            return unescapeDoubleQuoted(value.substring(1, value.length() - 1));
        }
        if (isWrapped(value, '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return stripInlineComment(value).trim();
    }

    private static boolean isWrapped(String value, char quote) {
        return value.length() >= 2 && value.charAt(0) == quote && value.charAt(value.length() - 1) == quote;
    }

    private static String stripInlineComment(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '#' && (i == 0 || Character.isWhitespace(value.charAt(i - 1)))) {
                return value.substring(0, i);
            }
        }
        return value;
    }

    private static String unescapeDoubleQuoted(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                result.append(switch (ch) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> ch;
                });
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                result.append(ch);
            }
        }
        if (escaping) {
            result.append('\\');
        }
        return result.toString();
    }

    private static boolean isValidKey(String key) {
        if (key.isEmpty()) {
            return false;
        }
        char first = key.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }
        for (int i = 1; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '.' && ch != '-') {
                return false;
            }
        }
        return true;
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }
}
