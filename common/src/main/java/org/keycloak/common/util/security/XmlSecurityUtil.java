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

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.Reader;

/**
 * Utility class for secure XML parsing with XXE (XML External Entity) protection.
 *
 * This class provides factory methods for creating secure XML parsers that are
 * protected against:
 * - XML External Entity (XXE) injection attacks
 * - XML Entity Expansion (Billion Laughs) attacks
 * - DTD (Document Type Definition) processing attacks
 *
 * All parsers created by this class have the following security features enabled:
 * - Disallow DOCTYPE declarations
 * - Disable external general entities
 * - Disable external parameter entities
 * - Disable loading of external DTDs
 * - Disable XInclude processing
 * - Disable entity expansion
 *
 * @author Keycloak Security Team
 * @version 1.0
 * @since 999.0.0
 */
public class XmlSecurityUtil {

    private static final Logger logger = Logger.getLogger(XmlSecurityUtil.class);

    /**
     * Feature to disallow DOCTYPE declarations completely.
     * This is the most secure option as it prevents all DTD-based attacks.
     */
    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    /**
     * Feature to disable external general entities.
     */
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";

    /**
     * Feature to disable external parameter entities.
     */
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";

    /**
     * Feature to disable loading of external DTDs.
     */
    private static final String FEATURE_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    /**
     * Property to disable external DTD access.
     */
    private static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";

    /**
     * Property to disable external schema access.
     */
    private static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";

    /**
     * Creates a secure DocumentBuilderFactory with XXE protection enabled.
     *
     * @return A secure DocumentBuilderFactory instance
     * @throws ParserConfigurationException if a parser cannot be created
     */
    public static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            // Primary defense: Disallow DOCTYPE declarations entirely
            factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);
            logger.debug("XXE Protection: Disallowed DOCTYPE declarations");
        } catch (ParserConfigurationException e) {
            // If disallowing DOCTYPE is not supported, use defense in depth
            logger.warn("XXE Protection: DOCTYPE disallow not supported, using alternative defenses");

            // Disable external general entities
            factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);

            // Disable external parameter entities
            factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);

            // Disable loading of external DTDs
            factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);
        }

        // Additional security settings
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);

        // Disable external access for DTD and Schema
        try {
            factory.setAttribute(ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException e) {
            logger.warn("XXE Protection: External access restriction not supported: " + e.getMessage());
        }

        logger.debug("XXE Protection: Secure DocumentBuilderFactory created successfully");
        return factory;
    }

    /**
     * Creates a secure DocumentBuilder with XXE protection enabled.
     *
     * @return A secure DocumentBuilder instance
     * @throws ParserConfigurationException if a parser cannot be created
     */
    public static DocumentBuilder createSecureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Set null EntityResolver to prevent external entity resolution
        builder.setEntityResolver((publicId, systemId) -> {
            logger.debug("XXE Protection: Blocked external entity resolution - publicId: " + publicId + ", systemId: " + systemId);
            return new InputSource(new java.io.StringReader(""));
        });

        return builder;
    }

    /**
     * Creates a secure SAXParserFactory with XXE protection enabled.
     *
     * @return A secure SAXParserFactory instance
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if a SAX error occurs
     */
    public static SAXParserFactory createSecureSAXParserFactory() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try {
            // Primary defense: Disallow DOCTYPE declarations entirely
            factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);
            logger.debug("XXE Protection: Disallowed DOCTYPE declarations in SAXParser");
        } catch (ParserConfigurationException | SAXException e) {
            // If disallowing DOCTYPE is not supported, use defense in depth
            logger.warn("XXE Protection: DOCTYPE disallow not supported in SAXParser, using alternative defenses");

            factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);
        }

        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);

        logger.debug("XXE Protection: Secure SAXParserFactory created successfully");
        return factory;
    }

    /**
     * Creates a secure SAXParser with XXE protection enabled.
     *
     * @return A secure SAXParser instance
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if a SAX error occurs
     */
    public static SAXParser createSecureSAXParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = createSecureSAXParserFactory();
        return factory.newSAXParser();
    }

    /**
     * Creates a secure XMLReader with XXE protection enabled.
     *
     * @return A secure XMLReader instance
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if a SAX error occurs
     */
    public static XMLReader createSecureXMLReader() throws ParserConfigurationException, SAXException {
        SAXParser parser = createSecureSAXParser();
        XMLReader reader = parser.getXMLReader();

        // Set null EntityResolver to prevent external entity resolution
        reader.setEntityResolver((publicId, systemId) -> {
            logger.debug("XXE Protection: Blocked external entity resolution in XMLReader - publicId: " + publicId + ", systemId: " + systemId);
            return new InputSource(new java.io.StringReader(""));
        });

        return reader;
    }

    /**
     * Safely parses an XML document from an InputStream with XXE protection.
     *
     * @param inputStream The input stream containing XML data
     * @return The parsed Document
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if a SAX error occurs
     * @throws java.io.IOException if an I/O error occurs
     */
    public static Document parseXmlDocument(InputStream inputStream)
            throws ParserConfigurationException, SAXException, java.io.IOException {
        DocumentBuilder builder = createSecureDocumentBuilder();
        return builder.parse(inputStream);
    }

    /**
     * Safely parses an XML document from a Reader with XXE protection.
     *
     * @param reader The reader containing XML data
     * @return The parsed Document
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if a SAX error occurs
     * @throws java.io.IOException if an I/O error occurs
     */
    public static Document parseXmlDocument(Reader reader)
            throws ParserConfigurationException, SAXException, java.io.IOException {
        DocumentBuilder builder = createSecureDocumentBuilder();
        return builder.parse(new InputSource(reader));
    }

    /**
     * Safely parses an XML document from a String with XXE protection.
     *
     * @param xmlString The string containing XML data
     * @return The parsed Document
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if a SAX error occurs
     * @throws java.io.IOException if an I/O error occurs
     */
    public static Document parseXmlDocument(String xmlString)
            throws ParserConfigurationException, SAXException, java.io.IOException {
        return parseXmlDocument(new java.io.StringReader(xmlString));
    }
}
