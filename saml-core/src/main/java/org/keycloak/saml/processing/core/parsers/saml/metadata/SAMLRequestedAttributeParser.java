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

import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeValueParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class SAMLRequestedAttributeParser extends AbstractStaxSamlParser<RequestedAttributeType> {

    private static final SAMLRequestedAttributeParser INSTANCE = new SAMLRequestedAttributeParser();
    private static final QName X500_ENCODING = new QName(JBossSAMLURIConstants.X500_NSURI.get(), JBossSAMLConstants.ENCODING.get(),
      JBossSAMLURIConstants.X500_PREFIX.get());


    private SAMLRequestedAttributeParser() {
        super(JBossSAMLConstants.REQUESTED_ATTRIBUTE);
    }

    public static SAMLRequestedAttributeParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected RequestedAttributeType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        RequestedAttributeType attributeType;

        // TODO: replace all constants with SamlMetadataQNames ones
        attributeType = new RequestedAttributeType(StaxParserUtil.getRequiredAttributeValue(element, SAMLAssertionQNames.ATTR_NAME));
        attributeType.setFriendlyName(StaxParserUtil.getAttributeValue(element, JBossSAMLConstants.FRIENDLY_NAME.get()));
        attributeType.setIsRequired(StaxParserUtil.getBooleanAttributeValue(element, JBossSAMLConstants.IS_REQUIRED.get()));
        attributeType.setNameFormat(StaxParserUtil.getAttributeValue(element, JBossSAMLConstants.NAME_FORMAT.get()));

        Attribute x500EncodingAttr = element.getAttributeByName(X500_ENCODING);
        if (x500EncodingAttr != null) {
            attributeType.getOtherAttributes().put(x500EncodingAttr.getName(), StaxParserUtil.getAttributeValue(x500EncodingAttr));
        }

        return attributeType;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, RequestedAttributeType target, JBossSAMLConstants element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE_VALUE:
                target.addAttributeValue(SAMLAttributeValueParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}