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

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.EncryptedAssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.parsers.ParserNamespaceSupport;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parse the saml assertion
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 12, 2010
 */
public class SAMLAssertionParser implements ParserNamespaceSupport {
    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private final String ASSERTION = JBossSAMLConstants.ASSERTION.get();

    public AssertionType fromElement(Element element) throws ConfigurationException, ProcessingException, ParsingException {
        XMLEventReader xmlEventReader = StaxParserUtil.getXMLEventReader(DocumentUtil.getNodeAsStream(element));
        return (AssertionType) parse(xmlEventReader);
    }

    /**
     * @see {@link ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
        String startElementName = StaxParserUtil.getStartElementName(startElement);
        if (startElementName.equals(JBossSAMLConstants.ENCRYPTED_ASSERTION.get())) {
            Element domElement = StaxParserUtil.getDOMElement(xmlEventReader);

            EncryptedAssertionType encryptedAssertion = new EncryptedAssertionType();
            encryptedAssertion.setEncryptedElement(domElement);
            return encryptedAssertion;
        }

        startElement = StaxParserUtil.getNextStartElement(xmlEventReader);

        // Special case: Encrypted Assertion
        StaxParserUtil.validate(startElement, ASSERTION);
        AssertionType assertion = parseBaseAttributes(startElement);

        // Peek at the next event
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;

            if (xmlEvent instanceof EndElement) {
                xmlEvent = StaxParserUtil.getNextEvent(xmlEventReader);
                EndElement endElement = (EndElement) xmlEvent;
                String endElementTag = StaxParserUtil.getEndElementName(endElement);
                if (endElementTag.equals(JBossSAMLConstants.ASSERTION.get()))
                    break;
                else
                    throw new RuntimeException(ErrorCodes.UNKNOWN_END_ELEMENT + endElementTag);
            }

            StartElement peekedElement = null;

            if (xmlEvent instanceof StartElement) {
                peekedElement = (StartElement) xmlEvent;
            } else {
                peekedElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            }
            if (peekedElement == null)
                break;

            String tag = StaxParserUtil.getStartElementName(peekedElement);

            if (tag.equals(JBossSAMLConstants.SIGNATURE.get())) {
                assertion.setSignature(StaxParserUtil.getDOMElement(xmlEventReader));
                continue;
            }

            if (JBossSAMLConstants.ISSUER.get().equalsIgnoreCase(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String issuerValue = StaxParserUtil.getElementText(xmlEventReader);
                NameIDType issuer = new NameIDType();
                issuer.setValue(issuerValue);

                assertion.setIssuer(issuer);
            } else if (JBossSAMLConstants.SUBJECT.get().equalsIgnoreCase(tag)) {
                SAMLSubjectParser subjectParser = new SAMLSubjectParser();
                assertion.setSubject((SubjectType) subjectParser.parse(xmlEventReader));
            } else if (JBossSAMLConstants.CONDITIONS.get().equalsIgnoreCase(tag)) {
                SAMLConditionsParser conditionsParser = new SAMLConditionsParser();
                ConditionsType conditions = (ConditionsType) conditionsParser.parse(xmlEventReader);

                assertion.setConditions(conditions);
            } else if (JBossSAMLConstants.ADVICE.get().equalsIgnoreCase(tag)) {
                StaxParserUtil.bypassElementBlock(xmlEventReader);
                logger.debug("SAML Advice tag is ignored");
            } else if (JBossSAMLConstants.AUTHN_STATEMENT.get().equalsIgnoreCase(tag)) {
                AuthnStatementType authnStatementType = SAMLParserUtil.parseAuthnStatement(xmlEventReader);
                assertion.addStatement(authnStatementType);
            } else if (JBossSAMLConstants.ATTRIBUTE_STATEMENT.get().equalsIgnoreCase(tag)) {
                AttributeStatementType attributeStatementType = SAMLParserUtil.parseAttributeStatement(xmlEventReader);
                assertion.addStatement(attributeStatementType);
            } else if (JBossSAMLConstants.STATEMENT.get().equalsIgnoreCase(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);

                String xsiTypeValue = StaxParserUtil.getXSITypeValue(startElement);
                throw new RuntimeException(ErrorCodes.UNKNOWN_XSI + xsiTypeValue);
            } else
                throw new RuntimeException(ErrorCodes.UNKNOWN_TAG + tag + "::location=" + peekedElement.getLocation());
        }
        return assertion;
    }

    /**
     * @see {@link ParserNamespaceSupport#supports(QName)}
     */
    public boolean supports(QName qname) {
        String nsURI = qname.getNamespaceURI();
        String localPart = qname.getLocalPart();

        return nsURI.equals(JBossSAMLURIConstants.ASSERTION_NSURI.get())
                && localPart.equals(JBossSAMLConstants.ASSERTION.get());
    }

    private AssertionType parseBaseAttributes(StartElement nextElement) throws ParsingException {
        String id = StaxParserUtil.getAttributeValue(nextElement, JBossSAMLConstants.ID.get());
        if (id == null) {
            throw logger.parserRequiredAttribute(JBossSAMLConstants.ID.get());
        }

        String version = StaxParserUtil.getAttributeValue(nextElement, JBossSAMLConstants.VERSION.get());
        if (!JBossSAMLConstants.VERSION_2_0.get().equals(version)) {
            throw logger.parserException(new RuntimeException(
                    String.format("Assertion %s required to be \"%s\"", JBossSAMLConstants.VERSION.get(), JBossSAMLConstants.VERSION_2_0.get())));
        }

        String issueInstantString = StaxParserUtil.getAttributeValue(nextElement, JBossSAMLConstants.ISSUE_INSTANT.get());
        if (issueInstantString == null) {
            throw logger.parserRequiredAttribute(JBossSAMLConstants.ISSUE_INSTANT.get());
        }
        XMLGregorianCalendar issueInstant = XMLTimeUtil.parse(issueInstantString);

        return new AssertionType(id, issueInstant);
    }

}