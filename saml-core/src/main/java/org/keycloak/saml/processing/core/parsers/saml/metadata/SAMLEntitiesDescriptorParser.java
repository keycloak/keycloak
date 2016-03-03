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
package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.ParserNamespaceSupport;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parse the SAML Entities Descriptor
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 31, 2011
 */
public class SAMLEntitiesDescriptorParser extends AbstractDescriptorParser implements ParserNamespaceSupport {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private final String EDT = JBossSAMLConstants.ENTITIES_DESCRIPTOR.get();

    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {

        xmlEventReader = filterWhiteSpaceCharacters(xmlEventReader);

        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, EDT);

        EntitiesDescriptorType entitiesDescriptorType = new EntitiesDescriptorType();

        // Parse the attributes
        Attribute validUntil = startElement.getAttributeByName(new QName(JBossSAMLConstants.VALID_UNTIL.get()));
        if (validUntil != null) {
            String validUntilValue = StaxParserUtil.getAttributeValue(validUntil);
            entitiesDescriptorType.setValidUntil(XMLTimeUtil.parse(validUntilValue));
        }

        Attribute id = startElement.getAttributeByName(new QName(JBossSAMLConstants.ID.get()));
        if (id != null) {
            entitiesDescriptorType.setID(StaxParserUtil.getAttributeValue(id));
        }

        Attribute name = startElement.getAttributeByName(new QName(JBossSAMLConstants.NAME.get()));
        if (name != null) {
            entitiesDescriptorType.setName(StaxParserUtil.getAttributeValue(name));
        }

        Attribute cacheDuration = startElement.getAttributeByName(new QName(JBossSAMLConstants.CACHE_DURATION.get()));
        if (cacheDuration != null) {
            entitiesDescriptorType
                    .setCacheDuration(XMLTimeUtil.parseAsDuration(StaxParserUtil.getAttributeValue(cacheDuration)));
        }

        // Get the Child Elements
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                StaxParserUtil.validate((EndElement) xmlEvent, EDT);
                StaxParserUtil.getNextEndElement(xmlEventReader);
                break;
            }
            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.ENTITY_DESCRIPTOR.get().equals(localPart)) {
                SAMLEntityDescriptorParser entityParser = new SAMLEntityDescriptorParser();
                entitiesDescriptorType.addEntityDescriptor(entityParser.parse(xmlEventReader));
            } else if (JBossSAMLConstants.EXTENSIONS.get().equalsIgnoreCase(localPart)) {
                entitiesDescriptorType.setExtensions(parseExtensions(xmlEventReader));
            } else if (JBossSAMLConstants.ENTITIES_DESCRIPTOR.get().equalsIgnoreCase(localPart)) {
                SAMLEntitiesDescriptorParser parser = new SAMLEntitiesDescriptorParser();
                entitiesDescriptorType.addEntityDescriptor(parser.parse(xmlEventReader));
            } else if (localPart.equals(JBossSAMLConstants.SIGNATURE.get())) {
                entitiesDescriptorType.setSignature(StaxParserUtil.getDOMElement(xmlEventReader));
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
        }
        return entitiesDescriptorType;
    }

    public boolean supports(QName qname) {
        String nsURI = qname.getNamespaceURI();
        String localPart = qname.getLocalPart();

        return nsURI.equals(JBossSAMLURIConstants.ASSERTION_NSURI.get()) && localPart.equals(EDT);
    }

    private ExtensionsType parseExtensions(XMLEventReader xmlEventReader) throws ParsingException {
        ExtensionsType extensions = new ExtensionsType();
        Element extElement = StaxParserUtil.getDOMElement(xmlEventReader);
        extensions.setElement(extElement);
        return extensions;
    }
}