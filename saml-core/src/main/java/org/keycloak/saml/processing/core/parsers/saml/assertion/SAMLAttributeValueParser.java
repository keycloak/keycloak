/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.core.parsers.saml.assertion;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.StaxParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import java.io.StringWriter;
import java.util.Objects;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 */
public class SAMLAttributeValueParser implements StaxParser {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private static final SAMLAttributeValueParser INSTANCE = new SAMLAttributeValueParser();
    private static final QName NIL = new QName(JBossSAMLURIConstants.XSI_NSURI.get(), "nil", JBossSAMLURIConstants.XSI_PREFIX.get());
    private static final QName XSI_TYPE = new QName(JBossSAMLURIConstants.XSI_NSURI.get(), "type", JBossSAMLURIConstants.XSI_PREFIX.get());

    private static final ThreadLocal<XMLEventFactory> XML_EVENT_FACTORY = ThreadLocal.withInitial(XMLEventFactory::newInstance);

    public static SAMLAttributeValueParser getInstance() {
        return INSTANCE;
    }

    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(element, SAMLAssertionQNames.ATTRIBUTE_VALUE.getQName());

        Attribute nil = element.getAttributeByName(NIL);
        if (nil != null) {
            String nilValue = StaxParserUtil.getAttributeValue(nil);
            if (nilValue != null && (nilValue.equalsIgnoreCase("true") || nilValue.equals("1"))) {
                String elementText = StaxParserUtil.getElementText(xmlEventReader);
                if (elementText == null || elementText.isEmpty()) {
                    return null;
                } else {
                    throw logger.nullValueError("nil attribute is not in SAML20 format");
                }
            } else {
                throw logger.parserRequiredAttribute(JBossSAMLURIConstants.XSI_PREFIX.get() + ":nil");
            }
        }

        Attribute type = element.getAttributeByName(XSI_TYPE);
        if (type == null) {
            if (StaxParserUtil.hasTextAhead(xmlEventReader)) {
                return StaxParserUtil.getElementText(xmlEventReader);
            }
            // Else we may have Child Element
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof StartElement) {
                element = (StartElement) xmlEvent;
                final QName qName = element.getName();
                if (Objects.equals(qName, SAMLAssertionQNames.NAMEID.getQName())) {
                    return SAMLParserUtil.parseNameIDType(xmlEventReader);
                }
            } else if (xmlEvent instanceof EndElement) {
                return "";
            }

            // when no type attribute assigned -> assume anyType
            return parseAsString(xmlEventReader);
        }

        //      RK Added an additional type check for base64Binary type as calheers is passing this type
        String typeValue = StaxParserUtil.getAttributeValue(type);
        if (typeValue.contains(":string")) {
            return StaxParserUtil.getElementText(xmlEventReader);
        } else if (typeValue.contains(":anyType")) {
            return parseAsString(xmlEventReader);
        } else if(typeValue.contains(":base64Binary")){
            return StaxParserUtil.getElementText(xmlEventReader);
        } else if(typeValue.contains(":date")){
            return XMLTimeUtil.parse(StaxParserUtil.getElementText(xmlEventReader));
        } else if(typeValue.contains(":boolean")){
            return StaxParserUtil.getElementText(xmlEventReader);
        }

        return parseAsString(xmlEventReader);
    }

    private static String parseAsString(XMLEventReader xmlEventReader) throws ParsingException {
        try {
            if (xmlEventReader.peek().isStartElement()) {
                StringWriter sw = new StringWriter();
                XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(sw);
                Deque<Map<String, String>> definedNamespaces = new LinkedList<>();
                int tagLevel = 0;
                while (xmlEventReader.hasNext() && (tagLevel > 0 || !xmlEventReader.peek().isEndElement())) {
                    XMLEvent event = (XMLEvent) xmlEventReader.next();
                    writer.add(event);
                    if (event.isStartElement()) {
                        definedNamespaces.push(addNamespaceWhenMissing(definedNamespaces, writer, event.asStartElement()));
                        tagLevel++;
                    }
                    if (event.isEndElement()) {
                        definedNamespaces.pop();
                        tagLevel--;
                    }
                }
                writer.close();
                return sw.toString();
            } else {
                return StaxParserUtil.getElementText(xmlEventReader);
            }
        } catch (Exception e) {
            throw logger.parserError(e);
        }
    }

    private static Map<String, String> addNamespaceWhenMissing(Deque<Map<String, String>> definedNamespaces, XMLEventWriter writer,
            StartElement startElement) throws XMLStreamException {

        final Map<String, String> necessaryNamespaces = new HashMap<>();
        // Namespace in tag
        if (startElement.getName().getPrefix() != null && !startElement.getName().getPrefix().isEmpty()) {
            necessaryNamespaces.put(startElement.getName().getPrefix(), startElement.getName().getNamespaceURI());
        }
        // Namespaces in attributes
        final Iterator<Attribute> attributes = startElement.getAttributes();
        while (attributes.hasNext()) {
            final Attribute attribute = attributes.next();
            if (attribute.getName().getPrefix() != null && !attribute.getName().getPrefix().isEmpty()) {
                necessaryNamespaces.put(attribute.getName().getPrefix(), attribute.getName().getNamespaceURI());
            }
        }

        // Already contained in stack
        necessaryNamespaces.entrySet().removeIf(nn -> definedNamespaces.stream().anyMatch(dn -> dn.containsKey(nn.getKey())));
        // Contained in current element
        Iterator<Namespace> namespaces = startElement.getNamespaces();
        while (namespaces.hasNext() && !necessaryNamespaces.isEmpty()) {
            necessaryNamespaces.remove(namespaces.next().getPrefix());
        }

        // Add all remaining necessaryNamespaces
        if (!necessaryNamespaces.isEmpty()) {
            XMLEventFactory xmlEventFactory = XML_EVENT_FACTORY.get();
            for (Map.Entry<String, String> entry : necessaryNamespaces.entrySet()) {
                writer.add(xmlEventFactory.createNamespace(entry.getKey(), entry.getValue()));
            }
        }
        return necessaryNamespaces;
    }
}