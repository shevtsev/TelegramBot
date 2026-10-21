package com.example;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Разбирает RSS 2.0 ленту и достаёт из неё новости.
 */
public class FeedParser {

    /**
     * Возвращает новости ленты в том порядке, в котором они идут в XML.
     * Бросает IllegalArgumentException, если XML не удалось прочитать.
     */
    public List<FeedItem> parse(InputStream input) {
        Document document = readDocument(input);
        List<FeedItem> items = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("item");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element item = (Element) nodes.item(i);
            items.add(new FeedItem(
                    getChildText(item, "title"),
                    getChildText(item, "link"),
                    getChildText(item, "pubDate")));
        }
        return items;
    }

    /**
     * Читает XML в DOM. Внешние сущности и DTD отключены.
     */
    private Document readDocument(InputStream input) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(input);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException("Не удалось разобрать ленту", e);
        }
    }

    /**
     * Возвращает текст первого дочернего элемента с указанным именем или пустую строку.
     */
    private String getChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && tagName.equals(element.getTagName())) {
                return element.getTextContent().trim();
            }
        }
        return "";
    }
}
