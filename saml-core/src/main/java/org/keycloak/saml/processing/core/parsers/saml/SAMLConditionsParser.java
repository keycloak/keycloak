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
package org.keycloak.saml.processing.core.parsers.saml;

import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.ParserNamespaceSupport;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.OneTimeUseType;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.net.URI;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 14, 2010
 */
public class SAMLConditionsParser implements ParserNamespaceSupport {

    /**
     * @see {@link ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        // We are entering this method with <conditions> as the next start element
        // and we have to exit after seeing the </conditions> end tag

        StartElement conditionsElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(conditionsElement, JBossSAMLConstants.CONDITIONS.get());

        ConditionsType conditions = new ConditionsType();

        String assertionNS = JBossSAMLURIConstants.ASSERTION_NSURI.get();

        QName notBeforeQName = new QName("", JBossSAMLConstants.NOT_BEFORE.get());
        QName notBeforeQNameWithNS = new QName(assertionNS, JBossSAMLConstants.NOT_BEFORE.get());

        QName notAfterQName = new QName("", JBossSAMLConstants.NOT_ON_OR_AFTER.get());
        QName notAfterQNameWithNS = new QName(assertionNS, JBossSAMLConstants.NOT_ON_OR_AFTER.get());

        Attribute notBeforeAttribute = conditionsElement.getAttributeByName(notBeforeQName);
        if (notBeforeAttribute == null)
            notBeforeAttribute = conditionsElement.getAttributeByName(notBeforeQNameWithNS);

        Attribute notAfterAttribute = conditionsElement.getAttributeByName(notAfterQName);
        if (notAfterAttribute == null)
            notAfterAttribute = conditionsElement.getAttributeByName(notAfterQNameWithNS);

        if (notBeforeAttribute != null) {
            String notBeforeValue = StaxParserUtil.getAttributeValue(notBeforeAttribute);
            conditions.setNotBefore(XMLTimeUtil.parse(notBeforeValue));
        }

        if (notAfterAttribute != null) {
            String notAfterValue = StaxParserUtil.getAttributeValue(notAfterAttribute);
            conditions.setNotOnOrAfter(XMLTimeUtil.parse(notAfterValue));
        }

        // Let us find additional elements

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);

            if (xmlEvent instanceof EndElement) {
                EndElement nextEndElement = (EndElement) xmlEvent;
                if (StaxParserUtil.matches(nextEndElement, JBossSAMLConstants.CONDITIONS.get())) {
                    nextEndElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw new RuntimeException(ErrorCodes.UNKNOWN_END_ELEMENT
                            + StaxParserUtil.getEndElementName(nextEndElement));
            }

            String tag = null;

            if (xmlEvent instanceof StartElement) {
                StartElement peekedElement = (StartElement) xmlEvent;
                tag = StaxParserUtil.getStartElementName(peekedElement);
            }

            if (JBossSAMLConstants.AUDIENCE_RESTRICTION.get().equals(tag)) {
                AudienceRestrictionType audienceRestriction = getAudienceRestriction(xmlEventReader);
                conditions.addCondition(audienceRestriction);
            } else if (JBossSAMLConstants.ONE_TIME_USE.get().equals(tag)) {
                // just parses the onetimeuse tag. until now PL has no support for onetimeuse conditions.
                StaxParserUtil.getNextStartElement(xmlEventReader);
                OneTimeUseType oneTimeUseCondition = new OneTimeUseType();
                conditions.addCondition(oneTimeUseCondition);

                // Get the end tag
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                StaxParserUtil.matches(endElement, JBossSAMLConstants.ONE_TIME_USE.get());
            } else
                throw new RuntimeException(ErrorCodes.UNKNOWN_TAG + tag + "::location=" + xmlEvent.getLocation());
        }
        return conditions;
    }

    /**
     * @see {@link ParserNamespaceSupport#supports(QName)}
     */
    public boolean supports(QName qname) {
        String nsURI = qname.getNamespaceURI();
        String localPart = qname.getLocalPart();

        return nsURI.equals(JBossSAMLURIConstants.ASSERTION_NSURI.get())
                && localPart.equals(JBossSAMLConstants.CONDITIONS.get());
    }

    /**
     * Parse the <audiencerestriction/> element
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    private AudienceRestrictionType getAudienceRestriction(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement audienceRestElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.matches(audienceRestElement, JBossSAMLConstants.AUDIENCE_RESTRICTION.get());

        AudienceRestrictionType audience = new AudienceRestrictionType();

        while (xmlEventReader.hasNext()) {
            StartElement audienceElement = StaxParserUtil.getNextStartElement(xmlEventReader);
            if (!StaxParserUtil.matches(audienceElement, JBossSAMLConstants.AUDIENCE.get()))
                break;

            if (!StaxParserUtil.hasTextAhead(xmlEventReader))
                throw new ParsingException(ErrorCodes.EXPECTED_TAG + "audienceValue");

            String audienceValue = StaxParserUtil.getElementText(xmlEventReader);
            audience.addAudience(URI.create(audienceValue));

            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) xmlEvent;
                if (StaxParserUtil.matches(endElement, JBossSAMLConstants.AUDIENCE_RESTRICTION.get())) {
                    StaxParserUtil.getNextEvent(xmlEventReader); // Just get the end element
                    break;
                } else
                    throw new RuntimeException(ErrorCodes.UNKNOWN_END_ELEMENT + StaxParserUtil.getEndElementName(endElement));
            }
        }
        return audience;
    }
}