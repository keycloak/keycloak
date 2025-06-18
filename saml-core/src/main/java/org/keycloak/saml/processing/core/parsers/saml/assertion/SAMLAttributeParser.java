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

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class SAMLAttributeParser extends AbstractStaxSamlAssertionParser<AttributeType> {

    private static final SAMLAttributeParser INSTANCE = new SAMLAttributeParser();

    private static final Set<QName> DEFAULT_KNOWN_ATTRIBUTE_NAMES = new HashSet<>(Arrays.asList(
            SAMLAssertionQNames.ATTR_NAME.getQName(),
            SAMLAssertionQNames.ATTR_FRIENDLY_NAME.getQName(),
            SAMLAssertionQNames.ATTR_NAME_FORMAT.getQName()
    ));

    private SAMLAttributeParser() {
        super(SAMLAssertionQNames.ATTRIBUTE);
    }

    public static SAMLAttributeParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AttributeType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        String name = StaxParserUtil.getRequiredAttributeValue(element, SAMLAssertionQNames.ATTR_NAME);
        final AttributeType attribute = new AttributeType(name);

        attribute.setFriendlyName(StaxParserUtil.getAttributeValue(element, SAMLAssertionQNames.ATTR_FRIENDLY_NAME));
        attribute.setNameFormat(StaxParserUtil.getAttributeValue(element, SAMLAssertionQNames.ATTR_NAME_FORMAT));

        // add non standard elements like SAMLAssertionQNames.ATTR_X500_ENCODING to other attributes
        attribute.getOtherAttributes().putAll(collectUnknownAttributesFrom(element));

        return attribute;
    }

    /**
     * Returns a {@link Map} with the found non-standard attribute values for the given {@link StartElement}.
     * An attribute is considered as non-standard, if it is not contained in DEFAULT_KNOWN_LOCAL_ATTRIBUTE_NAMES.
     *
     * @return Map
     */
    private static Map<QName, String> collectUnknownAttributesFrom(StartElement element) {

        Map<QName, String> otherAttributes = new HashMap<>();

        Iterator<?> attributes = element.getAttributes();
        while (attributes.hasNext()) {
            Attribute currentAttribute = (Attribute) attributes.next();
            QName attributeQName = currentAttribute.getName();
            if (attributeQName == null || DEFAULT_KNOWN_ATTRIBUTE_NAMES.contains(attributeQName)) {
                continue;
            }
            String attributeValue = currentAttribute.getValue();
            otherAttributes.put(attributeQName, attributeValue);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Adding attribute %s with value %s", attributeQName, attributeValue));
            }
        }

        return otherAttributes;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AttributeType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE_VALUE:
                target.addAttributeValue(SAMLAttributeValueParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}