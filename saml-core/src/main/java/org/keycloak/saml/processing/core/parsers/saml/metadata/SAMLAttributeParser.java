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

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeValueParser;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class SAMLAttributeParser extends AbstractStaxSamlMetadataParser<AttributeType> {

    private static final SAMLAttributeParser INSTANCE = new SAMLAttributeParser();

    private SAMLAttributeParser() {
        super(SAMLMetadataQNames.ATTRIBUTE);
    }

    public static SAMLAttributeParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AttributeType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        String name = StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_NAME);
        final AttributeType attribute = new AttributeType(name);

        attribute.setFriendlyName(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_FRIENDLY_NAME));
        attribute.setNameFormat(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_NAME_FORMAT));

        final String x500Encoding = StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_X500_ENCODING);
        if (x500Encoding != null) {
            attribute.getOtherAttributes().put(SAMLMetadataQNames.ATTR_X500_ENCODING.getQName(), x500Encoding);
        }

        return attribute;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AttributeType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE_VALUE:
                target.addAttributeValue(SAMLAttributeValueParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}