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

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.keycloak.dom.saml.v1.assertion.SAML11AssertionType;
import org.keycloak.dom.saml.v1.assertion.SAML11AttributeStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11AuthenticationStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11AuthorizationDecisionStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11ConditionsType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.parsers.StaxParser;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.core.parsers.util.SAML11ParserUtil;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.w3c.dom.Element;

/**
 * Parse the saml assertion
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 12, 2010
 */
public class SAML11AssertionParser implements StaxParser {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private final String ASSERTION = JBossSAMLConstants.ASSERTION.get();

    public SAML11AssertionType fromElement(Element element) throws ConfigurationException, ProcessingException,
            ParsingException {
        XMLEventReader xmlEventReader = StaxParserUtil.getXMLEventReader(DocumentUtil.getNodeAsStream(element));
        return (SAML11AssertionType) parse(xmlEventReader);
    }

    /**
     * @see {@link ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);

        startElement = StaxParserUtil.getNextStartElement(xmlEventReader);

        // Special case: Encrypted Assertion
        StaxParserUtil.validate(startElement, ASSERTION);
        SAML11AssertionType assertion = parseBaseAttributes(startElement);

        Attribute issuerAttribute = startElement.getAttributeByName(new QName(SAML11Constants.ISSUER));
        String issuer = StaxParserUtil.getAttributeValue(issuerAttribute);
        assertion.setIssuer(issuer);

        // Peek at the next event
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;

            if (xmlEvent instanceof EndElement) {
                xmlEvent = StaxParserUtil.getNextEvent(xmlEventReader);
                EndElement endElement = (EndElement) xmlEvent;
                String endElementTag = StaxParserUtil.getElementName(endElement);
                if (endElementTag.equals(JBossSAMLConstants.ASSERTION.get()))
                    break;
                else
                    throw logger.parserUnknownEndElement(endElementTag, xmlEvent.getLocation());
            }

            StartElement peekedElement = null;

            if (xmlEvent instanceof StartElement) {
                peekedElement = (StartElement) xmlEvent;
            } else {
                peekedElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            }
            if (peekedElement == null)
                break;

            String tag = StaxParserUtil.getElementName(peekedElement);

            if (tag.equals(JBossSAMLConstants.SIGNATURE.get())) {
                assertion.setSignature(StaxParserUtil.getDOMElement(xmlEventReader));
            } else if (JBossSAMLConstants.ISSUER.get().equalsIgnoreCase(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                issuer = StaxParserUtil.getElementText(xmlEventReader);

                assertion.setIssuer(issuer);
            } else if (JBossSAMLConstants.SUBJECT.get().equalsIgnoreCase(tag)) {
                SAML11SubjectParser subjectParser = new SAML11SubjectParser();
                SAML11SubjectType subject = (SAML11SubjectType) subjectParser.parse(xmlEventReader);
                SAML11SubjectStatementType subStat = new SAML11SubjectStatementType();
                subStat.setSubject(subject);
            } else if (JBossSAMLConstants.CONDITIONS.get().equalsIgnoreCase(tag)) {
                startElement = (StartElement) xmlEvent;

                SAML11ConditionsType conditions = SAML11ParserUtil.parseSAML11Conditions(xmlEventReader);
                assertion.setConditions(conditions);
            } else if (SAML11Constants.AUTHENTICATION_STATEMENT.equals(tag)) {
                startElement = (StartElement) xmlEvent;
                SAML11AuthenticationStatementType authStat = SAML11ParserUtil.parseAuthenticationStatement(xmlEventReader);
                assertion.add(authStat);
            } else if (SAML11Constants.ATTRIBUTE_STATEMENT.equalsIgnoreCase(tag)) {
                SAML11AttributeStatementType attributeStatementType = SAML11ParserUtil
                        .parseSAML11AttributeStatement(xmlEventReader);
                assertion.add(attributeStatementType);
            } else if (SAML11Constants.AUTHORIZATION_DECISION_STATEMENT.equalsIgnoreCase(tag)) {
                SAML11AuthorizationDecisionStatementType authzStat = SAML11ParserUtil
                        .parseSAML11AuthorizationDecisionStatement(xmlEventReader);
                assertion.add(authzStat);
            } else
                throw logger.parserUnknownTag(tag, peekedElement.getLocation());
        }
        return assertion;
    }


    private SAML11AssertionType parseBaseAttributes(StartElement nextElement) throws ParsingException {
        Attribute idAttribute = nextElement.getAttributeByName(new QName(SAML11Constants.ASSERTIONID));
        if (idAttribute == null)
            throw logger.parserRequiredAttribute("AssertionID");
        String id = StaxParserUtil.getAttributeValue(idAttribute);

        Attribute majVersionAttribute = nextElement.getAttributeByName(new QName(SAML11Constants.MAJOR_VERSION));
        String majVersion = StaxParserUtil.getAttributeValue(majVersionAttribute);
        StringUtil.match("1", majVersion);

        Attribute minVersionAttribute = nextElement.getAttributeByName(new QName(SAML11Constants.MINOR_VERSION));
        String minVersion = StaxParserUtil.getAttributeValue(minVersionAttribute);
        StringUtil.match("1", minVersion);

        Attribute issueInstantAttribute = nextElement.getAttributeByName(new QName(JBossSAMLConstants.ISSUE_INSTANT.get()));
        XMLGregorianCalendar issueInstant = XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(issueInstantAttribute));

        return new SAML11AssertionType(id, issueInstant);
    }
}