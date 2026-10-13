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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeValueParser;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class SAMLRequestedAttributeParser extends AbstractStaxSamlMetadataParser<RequestedAttributeType> {

    private static final SAMLRequestedAttributeParser INSTANCE = new SAMLRequestedAttributeParser();

    private SAMLRequestedAttributeParser() {
        super(SAMLMetadataQNames.REQUESTED_ATTRIBUTE);
    }

    public static SAMLRequestedAttributeParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected RequestedAttributeType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        RequestedAttributeType attributeType = new RequestedAttributeType(StaxParserUtil.getRequiredAttributeValue(element, SAMLAssertionQNames.ATTR_NAME));

        attributeType.setFriendlyName(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_FRIENDLY_NAME));
        attributeType.setIsRequired(StaxParserUtil.getBooleanAttributeValue(element, SAMLMetadataQNames.ATTR_IS_REQUIRED));
        attributeType.setNameFormat(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_NAME_FORMAT));

        String encoding = StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_X500_ENCODING);
        if (encoding != null && !encoding.isEmpty()) {
            attributeType.getOtherAttributes().put(SAMLMetadataQNames.ATTR_X500_ENCODING.getQName(), encoding);
        }

        return attributeType;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, RequestedAttributeType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE_VALUE:
                target.addAttributeValue(SAMLAttributeValueParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}