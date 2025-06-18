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

import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.w3c.dom.Element;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Parse the SAML Entities Descriptor
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 31, 2011
 */
public class SAMLEntitiesDescriptorParser extends AbstractStaxSamlMetadataParser<EntitiesDescriptorType> {

    private static final SAMLEntitiesDescriptorParser INSTANCE = new SAMLEntitiesDescriptorParser();

    public SAMLEntitiesDescriptorParser() {
        super(SAMLMetadataQNames.ENTITIES_DESCRIPTOR);
    }

    public static SAMLEntitiesDescriptorParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected EntitiesDescriptorType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        EntitiesDescriptorType descriptor = new EntitiesDescriptorType();

        // Parse the attributes
        descriptor.setID(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_ID));
        descriptor.setValidUntil(StaxParserUtil.getXmlTimeAttributeValue(element, SAMLMetadataQNames.ATTR_VALID_UNTIL));
        descriptor.setCacheDuration(StaxParserUtil.getXmlDurationAttributeValue(element, SAMLMetadataQNames.ATTR_CACHE_DURATION));
        descriptor.setName(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_NAME));

        return descriptor;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, EntitiesDescriptorType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case SIGNATURE:
                Element sig = StaxParserUtil.getDOMElement(xmlEventReader);
                target.setSignature(sig);
                break;

            case EXTENSIONS:
                target.setExtensions(SAMLExtensionsParser.getInstance().parse(xmlEventReader));
                break;

            case ENTITY_DESCRIPTOR:
                target.addEntityDescriptor(SAMLEntityDescriptorParser.getInstance().parse(xmlEventReader));
                break;

            case ENTITIES_DESCRIPTOR:
                target.addEntityDescriptor(parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}