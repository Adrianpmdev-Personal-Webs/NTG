package com.adrianpmdev.wowforever.features.news.application.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plain text out of the snippets of HTML other sites publish: tags removed, entities decoded and spaces collapsed, so
 * nothing they write is ever shown as markup.
 *
 * @author adrianpmdev
 */
final class HtmlText {

    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern ENTITY = Pattern.compile("&(?:#(x?)([0-9a-fA-F]+)|([a-zA-Z]+));");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private static final Map<String, String> NAMED = Map.ofEntries(
            Map.entry("amp", "&"),
            Map.entry("lt", "<"),
            Map.entry("gt", ">"),
            Map.entry("quot", "\""),
            Map.entry("apos", "'"),
            Map.entry("nbsp", " "),
            Map.entry("hellip", "…"),
            Map.entry("ndash", "–"),
            Map.entry("mdash", "—"),
            Map.entry("lsquo", "‘"),
            Map.entry("rsquo", "’"),
            Map.entry("ldquo", "“"),
            Map.entry("rdquo", "”"));

    private HtmlText() {
    }

    /**
     * A snippet of HTML as plain text.
     *
     * @param html snippet, or null
     * @return its text, or null when it has none
     */
    static String plain(String html) {
        return html == null ? null : text(TAG.matcher(html).replaceAll(" "));
    }

    /**
     * A text with its entities decoded and its spaces collapsed.
     *
     * @param text text, or null
     * @return the clean text, or null when it is blank
     */
    static String text(String text) {
        if (text == null) {
            return null;
        }

        String collapsed = SPACES.matcher(decodeEntities(text)).replaceAll(" ").trim();

        return collapsed.isEmpty() ? null : collapsed;
    }

    private static String decodeEntities(String text) {
        Matcher matcher = ENTITY.matcher(text);
        StringBuilder decoded = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(decode(matcher)));
        }

        matcher.appendTail(decoded);

        return decoded.toString();
    }

    private static String decode(Matcher entity) {
        if (entity.group(3) != null) {
            return NAMED.getOrDefault(entity.group(3), entity.group());
        }

        try {
            int codePoint = Integer.parseInt(entity.group(2), entity.group(1).isEmpty() ? 10 : 16);

            return Character.isValidCodePoint(codePoint) ? Character.toString(codePoint) : "";
        } catch (NumberFormatException exception) {
            return "";
        }
    }
}
