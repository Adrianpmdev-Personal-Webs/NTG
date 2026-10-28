package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.news.api.dto.response.NewsResponses.NewsItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads an RSS 2.0 feed into news items. The parser accepts no DOCTYPE and loads nothing external, so a hostile feed
 * cannot make it read files or call other hosts. Only http and https links are kept.
 *
 * @author adrianpmdev
 */
final class NewsFeedParser {

    private static final Pattern ID_PATTERN = Pattern.compile("news=(\\d+)");
    private static final Pattern CONTINUE_LINK = Pattern.compile("(?is)<a\\b[^>]*>\\s*continue reading.*?</a>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private NewsFeedParser() {
    }

    /**
     * The newest items of one category, in feed order.
     *
     * @param xml      the feed
     * @param category RSS category to keep (ignoring case), or null to keep every item
     * @param limit    most items returned
     * @return items, newest first
     * @throws IOException when the feed is not valid XML
     */
    static List<NewsItem> parse(byte[] xml, String category, int limit) throws IOException {
        NodeList nodes = read(xml).getElementsByTagName("item");
        List<NewsItem> items = new ArrayList<>();

        for (int index = 0; index < nodes.getLength() && items.size() < limit; index++) {
            Element item = (Element) nodes.item(index);

            if (category != null && !hasCategory(item, category)) {
                continue;
            }

            NewsItem news = toItem(item);

            if (news != null) {
                items.add(news);
            }
        }

        return List.copyOf(items);
    }

    private static Document read(byte[] xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());

            return builder.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException("The news feed is not valid RSS", exception);
        }
    }

    private static boolean hasCategory(Element item, String category) {
        NodeList categories = item.getElementsByTagName("category");

        for (int index = 0; index < categories.getLength(); index++) {
            if (category.equalsIgnoreCase(categories.item(index).getTextContent().trim())) {
                return true;
            }
        }

        return false;
    }

    private static NewsItem toItem(Element item) {
        String title = clean(childText(item, "title"));
        String url = webUrl(childText(item, "link"));
        String publishedAt = instant(childText(item, "pubDate"));
        Long id = id(childText(item, "guid"), url);

        if (title == null || url == null || publishedAt == null || id == null) {
            return null;
        }

        return new NewsItem(id, title, summary(childText(item, "description")), publishedAt, image(item), url);
    }

    private static String childText(Element item, String name) {
        for (Node child = item.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNamespaceURI() == null
                    && name.equals(child.getLocalName())) {
                return child.getTextContent();
            }
        }

        return null;
    }

    private static Long id(String guid, String url) {
        for (String source : new String[]{guid, url}) {
            Matcher matcher = source == null ? null : ID_PATTERN.matcher(source);

            if (matcher != null && matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }

        return null;
    }

    private static String instant(String pubDate) {
        if (pubDate == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(pubDate.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toString();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String image(Element item) {
        NodeList contents = item.getElementsByTagNameNS("*", "content");

        for (int index = 0; index < contents.getLength(); index++) {
            Element content = (Element) contents.item(index);
            String medium = content.getAttribute("medium");
            String type = content.getAttribute("type");

            if ("image".equals(medium) || type.startsWith("image/")) {
                return webUrl(content.getAttribute("url"));
            }
        }

        return null;
    }

    private static String summary(String description) {
        if (description == null) {
            return null;
        }

        String text = CONTINUE_LINK.matcher(description).replaceAll(" ");

        return clean(TAG.matcher(text).replaceAll(" "));
    }

    private static String clean(String text) {
        if (text == null) {
            return null;
        }

        String decoded = decodeEntities(text);
        String collapsed = SPACES.matcher(decoded).replaceAll(" ").trim();

        return collapsed.isEmpty() ? null : collapsed;
    }

    private static String decodeEntities(String text) {
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuilder decoded = new StringBuilder();

        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(2), matcher.group(1).isEmpty() ? 10 : 16);
            String character = Character.isValidCodePoint(codePoint) ? Character.toString(codePoint) : "";
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(character));
        }

        matcher.appendTail(decoded);

        return decoded.toString()
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    private static String webUrl(String url) {
        if (url == null) {
            return null;
        }

        String trimmed = url.trim();

        return trimmed.startsWith("https://") || trimmed.startsWith("http://") ? trimmed : null;
    }
}
