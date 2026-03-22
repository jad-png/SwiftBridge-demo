package com.swiftbridge.converter.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;

@Component
@Slf4j
public class XmlParsingService {

    private static final String PACS_008_NS_PREFIX = "urn:iso:std:iso:20022:tech:xsd:pacs.008";
    private static final String DEFAULT_PACS_008_NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02";

    public Document parseNamespaceAwareXml(String xmlContent) throws Exception {
        log.debug("Parsing XML content with namespace awareness");

        DocumentBuilderFactory factory = createSecureFactory();

        try {
            Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            document.setUserData("namespaceAwareEnabled", factory.isNamespaceAware(), null);

            log.debug("XML parsing successful");
            return document;
        } catch (Exception ex) {
            log.error("Failed to parse XML content", ex);
            throw new RuntimeException("XML parsing failed: " + ex.getMessage(), ex);
        }
    }

    private DocumentBuilderFactory createSecureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    public XPath createPacs008XPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new Pacs008NamespaceContext(DEFAULT_PACS_008_NS));
        return xpath;
    }

    public XPath createPacs008XPath(Document document) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new Pacs008NamespaceContext(resolvePacs008Namespace(document)));
        return xpath;
    }

    private String resolvePacs008Namespace(Document document) {
        if (document == null || document.getDocumentElement() == null) {
            return DEFAULT_PACS_008_NS;
        }

        String rootNamespace = document.getDocumentElement().getNamespaceURI();
        if (rootNamespace != null && rootNamespace.startsWith(PACS_008_NS_PREFIX)) {
            return rootNamespace;
        }

        return DEFAULT_PACS_008_NS;
    }

    private static final class Pacs008NamespaceContext implements NamespaceContext {

        private final String pacs008Namespace;

        private Pacs008NamespaceContext(String pacs008Namespace) {
            this.pacs008Namespace = pacs008Namespace;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException("Prefix cannot be null");
            }
            if ("doc".equals(prefix)) {
                return pacs008Namespace;
            }
            if ("xml".equals(prefix)) {
                return XMLConstants.XML_NS_URI;
            }
            return XMLConstants.NULL_NS_URI;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (namespaceURI != null && namespaceURI.startsWith(PACS_008_NS_PREFIX)) {
                return "doc";
            }
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            String prefix = getPrefix(namespaceURI);
            if (prefix == null) {
                return Collections.emptyIterator();
            }
            return Collections.singleton(prefix).iterator();
        }
    }
}
