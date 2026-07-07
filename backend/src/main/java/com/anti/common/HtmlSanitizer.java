package com.anti.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlSanitizer {

    private static final Safelist SAFELIST;

    private static final Safelist COMMENT_SAFELIST;

    private static final Set<String> ALLOWED_CSS_PROPERTIES = new HashSet<>();

    private static final Pattern CSS_PROPERTY_PATTERN = Pattern.compile("\\s*([\\w-]+)\\s*:\\s*([^;]+);?");

    static {
        SAFELIST = Safelist.relaxed()
                .addTags("h1", "h2", "h3", "h4", "h5", "h6")
                .addTags("pre", "code")
                .addTags("ul", "ol", "li")
                .addTags("p", "br", "hr")
                .addTags("strong", "em", "b", "i", "u", "s", "span")
                .addTags("blockquote", "cite")
                .addTags("div", "section")
                .addAttributes(":all", "class", "style")
                .addAttributes("a", "href", "title", "target", "rel")
                .addProtocols("a", "href", "ftp", "http", "https", "mailto")
                .preserveRelativeLinks(true);

        COMMENT_SAFELIST = Safelist.none()
                .addTags("b", "i", "u", "em", "strong", "span", "s")
                .addTags("a")
                .addAttributes(":all", "style")
                .addAttributes("a", "href", "title", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto")
                .preserveRelativeLinks(true);

        ALLOWED_CSS_PROPERTIES.add("color");
        ALLOWED_CSS_PROPERTIES.add("background");
        ALLOWED_CSS_PROPERTIES.add("background-color");
        ALLOWED_CSS_PROPERTIES.add("font-weight");
        ALLOWED_CSS_PROPERTIES.add("font-style");
        ALLOWED_CSS_PROPERTIES.add("text-decoration");
        ALLOWED_CSS_PROPERTIES.add("text-align");
        ALLOWED_CSS_PROPERTIES.add("font-size");
    }

    /**
     * 净化帖子正文内容，保留常用富文本标签
     */
    public static String sanitizePostContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String cleaned = Jsoup.clean(content, SAFELIST);
        cleaned = sanitizeCss(cleaned);
        return cleaned.strip();
    }

    /**
     * 净化评论内容
     * 保留安全的内联标签（b, i, u, em, strong, span, s, a）及颜色样式
     */
    public static String sanitizeCommentContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String cleaned = Jsoup.clean(content, COMMENT_SAFELIST);
        cleaned = sanitizeCss(cleaned);
        return cleaned.strip();
    }

    /**
     * 净化纯文本（剥离所有标签）
     */
    public static String sanitizePlainText(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String cleaned = Jsoup.clean(content, Safelist.none());
        return cleaned.strip();
    }

    /**
     * 仅保留安全的 CSS 颜色相关属性
     */
    private static String sanitizeCss(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        Document doc = Jsoup.parse(html);
        for (Element el : doc.select("[style]")) {
            String style = el.attr("style");
            if (style != null && !style.isBlank()) {
                el.attr("style", filterCssProperties(style));
            }
        }
        String result = doc.body().html();
        if (result == null) {
            return html;
        }
        return result;
    }

    /**
     * 过滤 CSS，只保留颜色相关的白名单属性
     */
    private static String filterCssProperties(String css) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = CSS_PROPERTY_PATTERN.matcher(css);
        while (matcher.find()) {
            String property = matcher.group(1).trim().toLowerCase();
            String value = matcher.group(2).trim().toLowerCase();
            if (ALLOWED_CSS_PROPERTIES.contains(property) && isSafeCssValue(value)) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(property).append(": ").append(matcher.group(2).trim());
            }
        }
        return sb.toString();
    }

    /**
     * 检查 CSS 值是否安全（禁止 url()、expression() 等危险内容）
     */
    private static boolean isSafeCssValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase();
        if (lower.contains("url(") || lower.contains("expression(")
                || lower.contains("javascript:") || lower.contains("eval(")
                || lower.contains("import(") || lower.contains("progid:")) {
            return false;
        }
        return true;
    }
}
