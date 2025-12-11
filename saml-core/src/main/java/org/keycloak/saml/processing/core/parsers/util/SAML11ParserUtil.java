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
package org.keycloak.saml.processing.core.parsers.util;

import java.net.URI;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.keycloak.dom.saml.v1.assertion.SAML11ActionType;
import org.keycloak.dom.saml.v1.assertion.SAML11AttributeStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11AttributeType;
import org.keycloak.dom.saml.v1.assertion.SAML11AudienceRestrictionCondition;
import org.keycloak.dom.saml.v1.assertion.SAML11AuthenticationStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11AuthorityBindingType;
import org.keycloak.dom.saml.v1.assertion.SAML11AuthorizationDecisionStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11ConditionsType;
import org.keycloak.dom.saml.v1.assertion.SAML11DecisionType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectConfirmationType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectLocalityType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectType;
import org.keycloak.dom.saml.v1.protocol.SAML11AttributeQueryType;
import org.keycloak.dom.saml.v1.protocol.SAML11AuthenticationQueryType;
import org.keycloak.dom.saml.v1.protocol.SAML11AuthorizationDecisionQueryType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509CertificateType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509DataType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.constants.WSTrustConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.SAML11SubjectParser;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility for parsing SAML 1.1 payload
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 23, 2011
 */
public class SAML11ParserUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Parse the AuthnStatement inside the assertion
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11AuthenticationStatementType parseAuthenticationStatement(XMLEventReader xmlEventReader)
            throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);

        StaxParserUtil.validate(startElement, SAML11Constants.AUTHENTICATION_STATEMENT);

        Attribute authMethod = startElement.getAttributeByName(new QName(SAML11Constants.AUTHENTICATION_METHOD));
        if (authMethod == null)
            throw logger.parserRequiredAttribute(SAML11Constants.AUTHENTICATION_METHOD);

        Attribute authInstant = startElement.getAttributeByName(new QName(SAML11Constants.AUTHENTICATION_INSTANT));
        if (authInstant == null)
            throw logger.parserRequiredAttribute(SAML11Constants.AUTHENTICATION_INSTANT);

        SAML11AuthenticationStatementType authStat = new SAML11AuthenticationStatementType(URI.create(StaxParserUtil
                .getAttributeValue(authMethod)), XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(authInstant)));

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;

            if (xmlEvent instanceof EndElement) {
                xmlEvent = StaxParserUtil.getNextEvent(xmlEventReader);
                EndElement endElement = (EndElement) xmlEvent;
                String endElementTag = StaxParserUtil.getElementName(endElement);
                if (endElementTag.equals(SAML11Constants.AUTHENTICATION_STATEMENT))
                    break;
                else
                    throw logger.parserUnknownEndElement(endElementTag, xmlEvent.getLocation());
            }
            startElement = null;

            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;
            } else {
                startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            }
            if (startElement == null)
                break;

            String tag = StaxParserUtil.getElementName(startElement);

            if (JBossSAMLConstants.SUBJECT.get().equalsIgnoreCase(tag)) {
                SAML11SubjectParser subjectParser = new SAML11SubjectParser();
                SAML11SubjectType subject = (SAML11SubjectType) subjectParser.parse(xmlEventReader);
                SAML11SubjectStatementType subStat = new SAML11SubjectStatementType();
                subStat.setSubject(subject);

                authStat.setSubject(subject);
            } else if (JBossSAMLConstants.SUBJECT_LOCALITY.get().equals(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                SAML11SubjectLocalityType subjectLocalityType = new SAML11SubjectLocalityType();
                Attribute address = startElement.getAttributeByName(new QName(SAML11Constants.IP_ADDRESS));
                if (address != null) {
                    subjectLocalityType.setIpAddress(StaxParserUtil.getAttributeValue(address));
                }
                Attribute dns = startElement.getAttributeByName(new QName(SAML11Constants.DNS_ADDRESS));
                if (dns != null) {
                    subjectLocalityType.setDnsAddress(StaxParserUtil.getAttributeValue(dns));
                }
                authStat.setSubjectLocality(subjectLocalityType);
                StaxParserUtil.validate(StaxParserUtil.getNextEndElement(xmlEventReader),
                        JBossSAMLConstants.SUBJECT_LOCALITY.get());
            } else if (SAML11Constants.AUTHORITY_BINDING.equals(tag)) {
                Attribute authorityKindAttr = startElement.getAttributeByName(new QName(SAML11Constants.AUTHORITY_KIND));
                if (authorityKindAttr == null)
                    throw logger.parserRequiredAttribute("AuthorityKind");

                Attribute locationAttr = startElement.getAttributeByName(new QName(SAML11Constants.LOCATION));
                if (locationAttr == null)
                    throw logger.parserRequiredAttribute("Location");
                URI location = URI.create(StaxParserUtil.getAttributeValue(locationAttr));

                Attribute bindingAttr = startElement.getAttributeByName(new QName(SAML11Constants.BINDING));
                if (bindingAttr == null)
                    throw logger.parserRequiredAttribute("Binding");
                URI binding = URI.create(StaxParserUtil.getAttributeValue(bindingAttr));

                QName authorityKind = QName.valueOf(StaxParserUtil.getAttributeValue(authorityKindAttr));

                SAML11AuthorityBindingType authorityBinding = new SAML11AuthorityBindingType(authorityKind, location, binding);
                authStat.add(authorityBinding);
            } else
                throw logger.parserUnknownTag("", startElement.getLocation());

        }

        return authStat;
    }

    /**
     * Parse the {@link org.keycloak.dom.saml.v1.assertion.SAML11SubjectConfirmationType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11SubjectConfirmationType parseSAML11SubjectConfirmation(XMLEventReader xmlEventReader)
            throws ParsingException {
        SAML11SubjectConfirmationType subjectConfirmationType = new SAML11SubjectConfirmationType();

        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);

        // There may be additional things under subject confirmation
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.SUBJECT_CONFIRMATION.get());
                break;
            }

            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;

                String startTag = StaxParserUtil.getElementName(startElement);

                if (startTag.equals(SAML11Constants.CONFIRMATION_METHOD)) {
                    startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                    String method = StaxParserUtil.getElementText(xmlEventReader);
                    subjectConfirmationType.addConfirmationMethod(URI.create(method));
                } else if (startTag.equals(JBossSAMLConstants.SUBJECT_CONFIRMATION_DATA.get())) {
                    startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                    SubjectConfirmationDataType subjectConfirmationData = parseSubjectConfirmationData(xmlEventReader);
                    subjectConfirmationType.setSubjectConfirmationData(subjectConfirmationData);
                } else if (startTag.equals(JBossSAMLConstants.KEY_INFO.get())) {
                    Element keyInfo = StaxParserUtil.getDOMElement(xmlEventReader);
                    subjectConfirmationType.setKeyInfo(keyInfo);
                } else
                    throw logger.parserUnknownTag(startTag, startElement.getLocation());
            }
        }
        return subjectConfirmationType;

    }

    /**
     * Parse the {@link SubjectConfirmationDataType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SubjectConfirmationDataType parseSubjectConfirmationData(XMLEventReader xmlEventReader)
            throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.SUBJECT_CONFIRMATION_DATA.get());

        SubjectConfirmationDataType subjectConfirmationData = new SubjectConfirmationDataType();

        Attribute inResponseTo = startElement.getAttributeByName(new QName(JBossSAMLConstants.IN_RESPONSE_TO.get()));
        if (inResponseTo != null) {
            subjectConfirmationData.setInResponseTo(StaxParserUtil.getAttributeValue(inResponseTo));
        }

        Attribute notBefore = startElement.getAttributeByName(new QName(JBossSAMLConstants.NOT_BEFORE.get()));
        if (notBefore != null) {
            subjectConfirmationData.setNotBefore(XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(notBefore)));
        }

        Attribute notOnOrAfter = startElement.getAttributeByName(new QName(JBossSAMLConstants.NOT_ON_OR_AFTER.get()));
        if (notOnOrAfter != null) {
            subjectConfirmationData.setNotOnOrAfter(XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(notOnOrAfter)));
        }

        Attribute recipient = startElement.getAttributeByName(new QName(JBossSAMLConstants.RECIPIENT.get()));
        if (recipient != null) {
            subjectConfirmationData.setRecipient(StaxParserUtil.getAttributeValue(recipient));
        }

        Attribute address = startElement.getAttributeByName(new QName(JBossSAMLConstants.ADDRESS.get()));
        if (address != null) {
            subjectConfirmationData.setAddress(StaxParserUtil.getAttributeValue(address));
        }

        XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
        if (!(xmlEvent instanceof EndElement)) {
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            String tag = StaxParserUtil.getElementName(startElement);
            if (tag.equals(WSTrustConstants.XMLDSig.KEYINFO)) {
                KeyInfoType keyInfo = parseKeyInfo(xmlEventReader);
                subjectConfirmationData.setAnyType(keyInfo);
            } else if (tag.equals(WSTrustConstants.XMLEnc.ENCRYPTED_KEY)) {
                subjectConfirmationData.setAnyType(StaxParserUtil.getDOMElement(xmlEventReader));
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }

        // Get the end tag
        EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
        StaxParserUtil.matches(endElement, JBossSAMLConstants.SUBJECT_CONFIRMATION_DATA.get());
        return subjectConfirmationData;
    }

    /**
     * Parse an {@code SAML11AttributeStatementType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11AttributeStatementType parseSAML11AttributeStatement(XMLEventReader xmlEventReader)
            throws ParsingException {
        SAML11AttributeStatementType attributeStatementType = new SAML11AttributeStatementType();

        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        String ATTRIBSTATEMT = JBossSAMLConstants.ATTRIBUTE_STATEMENT.get();
        StaxParserUtil.validate(startElement, ATTRIBSTATEMT);

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.ATTRIBUTE_STATEMENT.get());
                break;
            }
            // Get the next start element
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            String tag = startElement.getName().getLocalPart();
            if (JBossSAMLConstants.ATTRIBUTE.get().equals(tag)) {
                SAML11AttributeType attribute = parseSAML11Attribute(xmlEventReader);
                attributeStatementType.add(attribute);
            } else if (JBossSAMLConstants.SUBJECT.get().equals(tag)) {
                SAML11SubjectParser parser = new SAML11SubjectParser();
                SAML11SubjectType subject = (SAML11SubjectType) parser.parse(xmlEventReader);
                attributeStatementType.setSubject(subject);
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }
        return attributeStatementType;
    }

    /**
     * Parse a {@link SAML11AttributeType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11AttributeType parseSAML11Attribute(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ATTRIBUTE.get());
        SAML11AttributeType attributeType = null;

        Attribute name = startElement.getAttributeByName(new QName(SAML11Constants.ATTRIBUTE_NAME));
        if (name == null)
            throw logger.parserRequiredAttribute("Name");
        String attribName = StaxParserUtil.getAttributeValue(name);

        Attribute namesp = startElement.getAttributeByName(new QName(SAML11Constants.ATTRIBUTE_NAMESPACE));
        if (namesp == null)
            throw logger.parserRequiredAttribute("Namespace");
        String attribNamespace = StaxParserUtil.getAttributeValue(namesp);

        attributeType = new SAML11AttributeType(attribName, URI.create(attribNamespace));

        attributeType.add(parseAttributeValue(xmlEventReader));

        parseAttributeType(xmlEventReader, startElement, JBossSAMLConstants.ATTRIBUTE.get(), attributeType);
        return attributeType;
    }

    /**
     * Parse an {@code SAML11AttributeType}
     *
     * @param xmlEventReader
     *
     * @throws ParsingException
     */
    public static void parseAttributeType(XMLEventReader xmlEventReader, StartElement startElement, String rootTag,
                                          SAML11AttributeType attributeType) throws ParsingException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                if (StaxParserUtil.matches(end, rootTag))
                    break;
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getElementName(startElement);

            if (JBossSAMLConstants.ATTRIBUTE.get().equals(tag))
                break;

            if (JBossSAMLConstants.ATTRIBUTE_VALUE.get().equals(tag)) {
                Object attributeValue = parseAttributeValue(xmlEventReader);
                attributeType.add(attributeValue);
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }
    }

    /**
     * Parse Attribute value
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static Object parseAttributeValue(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ATTRIBUTE_VALUE.get());

        Attribute type = startElement.getAttributeByName(new QName(JBossSAMLURIConstants.XSI_NSURI.get(), "type", "xsi"));
        if (type == null) {
            return StaxParserUtil.getElementText(xmlEventReader);
        }

        String typeValue = StaxParserUtil.getAttributeValue(type);
        if (typeValue.contains(":string")) {
            return StaxParserUtil.getElementText(xmlEventReader);
        }

        throw logger.parserUnknownXSI(typeValue);
    }

    public static SAML11AuthorizationDecisionStatementType parseSAML11AuthorizationDecisionStatement(
            XMLEventReader xmlEventReader) throws ParsingException {
        SAML11AuthorizationDecisionStatementType authzDecision = null;

        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, SAML11Constants.AUTHORIZATION_DECISION_STATEMENT);

        Attribute decision = startElement.getAttributeByName(new QName(SAML11Constants.DECISION));
        if (decision == null)
            throw logger.parserRequiredAttribute("Decision");
        String decisionValue = StaxParserUtil.getAttributeValue(decision);

        Attribute resource = startElement.getAttributeByName(new QName(SAML11Constants.RESOURCE));
        if (resource == null)
            throw logger.parserRequiredAttribute("Namespace");
        String resValue = StaxParserUtil.getAttributeValue(resource);

        authzDecision = new SAML11AuthorizationDecisionStatementType(URI.create(resValue),
                SAML11DecisionType.valueOf(decisionValue));

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                if (StaxParserUtil.matches(end, SAML11Constants.AUTHORIZATION_DECISION_STATEMENT))
                    break;
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getElementName(startElement);

            if (SAML11Constants.ACTION.equals(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                SAML11ActionType samlAction = new SAML11ActionType();
                Attribute namespaceAttr = startElement.getAttributeByName(new QName(SAML11Constants.NAMESPACE));
                if (namespaceAttr != null) {
                    samlAction.setNamespace(StaxParserUtil.getAttributeValue(namespaceAttr));
                }
                samlAction.setValue(StaxParserUtil.getElementText(xmlEventReader));

                authzDecision.addAction(samlAction);
            } else if (JBossSAMLConstants.SUBJECT.get().equals(tag)) {
                SAML11SubjectParser parser = new SAML11SubjectParser();
                authzDecision.setSubject((SAML11SubjectType) parser.parse(xmlEventReader));
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }
        return authzDecision;
    }

    /**
     * Parse {@link org.keycloak.dom.saml.v1.assertion.SAML11ConditionsType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11ConditionsType parseSAML11Conditions(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement;
        SAML11ConditionsType conditions = new SAML11ConditionsType();
        StartElement conditionsElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(conditionsElement, JBossSAMLConstants.CONDITIONS.get());

        String assertionNS = SAML11Constants.ASSERTION_11_NSURI;

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

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                if (StaxParserUtil.matches(end, JBossSAMLConstants.CONDITIONS.get()))
                    break;
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getElementName(startElement);

            if (SAML11Constants.AUDIENCE_RESTRICTION_CONDITION.equals(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                SAML11AudienceRestrictionCondition restrictCond = new SAML11AudienceRestrictionCondition();

                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                if (StaxParserUtil.getElementName(startElement).equals(JBossSAMLConstants.AUDIENCE.get())) {
                    restrictCond.add(URI.create(StaxParserUtil.getElementText(xmlEventReader)));
                }
                EndElement theEndElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(theEndElement, SAML11Constants.AUDIENCE_RESTRICTION_CONDITION);
                conditions.add(restrictCond);
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }
        return conditions;
    }

    public static KeyInfoType parseKeyInfo(XMLEventReader xmlEventReader) throws ParsingException {
        KeyInfoType keyInfo = new KeyInfoType();
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, WSTrustConstants.XMLDSig.KEYINFO);

        XMLEvent xmlEvent = null;
        String tag = null;

        while (xmlEventReader.hasNext()) {
            xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                tag = StaxParserUtil.getElementName((EndElement) xmlEvent);
                if (tag.equals(WSTrustConstants.XMLDSig.KEYINFO)) {
                    StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw logger.parserUnknownEndElement(tag, xmlEvent.getLocation());
            }
            startElement = (StartElement) xmlEvent;
            tag = StaxParserUtil.getElementName(startElement);
            if (tag.equals(WSTrustConstants.XMLEnc.ENCRYPTED_KEY)) {
                keyInfo.addContent(StaxParserUtil.getDOMElement(xmlEventReader));
            } else if (tag.equals(WSTrustConstants.XMLDSig.X509DATA)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                X509DataType x509 = new X509DataType();

                // Let us go for the X509 certificate
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                StaxParserUtil.validate(startElement, WSTrustConstants.XMLDSig.X509CERT);

                X509CertificateType cert = new X509CertificateType();
                String certValue = StaxParserUtil.getElementText(xmlEventReader);
                cert.setEncodedCertificate(certValue.getBytes(GeneralConstants.SAML_CHARSET));
                x509.add(cert);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, WSTrustConstants.XMLDSig.X509DATA);
                keyInfo.addContent(x509);
            } else if (tag.equals(WSTrustConstants.XMLDSig.KEYVALUE)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                KeyValueType keyValue = null;

                startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
                tag = StaxParserUtil.getElementName(startElement);
                if (tag.equals(WSTrustConstants.XMLDSig.RSA_KEYVALUE)) {
                    keyValue = parseRSAKeyValue(xmlEventReader);
                } else if (tag.equals(WSTrustConstants.XMLDSig.DSA_KEYVALUE)) {
                    keyValue = parseDSAKeyValue(xmlEventReader);
                } else
                    throw logger.parserUnknownTag(tag, startElement.getLocation());

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, WSTrustConstants.XMLDSig.KEYVALUE);

                keyInfo.addContent(keyValue);
            }
        }
        return keyInfo;
    }

    public static RSAKeyValueType parseRSAKeyValue(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, WSTrustConstants.XMLDSig.RSA_KEYVALUE);

        XMLEvent xmlEvent = null;
        String tag = null;

        RSAKeyValueType rsaKeyValue = new RSAKeyValueType();

        while (xmlEventReader.hasNext()) {
            xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                tag = StaxParserUtil.getElementName((EndElement) xmlEvent);
                if (tag.equals(WSTrustConstants.XMLDSig.RSA_KEYVALUE)) {
                    StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw logger.parserUnknownEndElement(tag, xmlEvent.getLocation());
            }

            startElement = (StartElement) xmlEvent;
            tag = StaxParserUtil.getElementName(startElement);
            if (tag.equals(WSTrustConstants.XMLDSig.MODULUS)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String text = StaxParserUtil.getElementText(xmlEventReader);
                rsaKeyValue.setModulus(text.getBytes(GeneralConstants.SAML_CHARSET));
            } else if (tag.equals(WSTrustConstants.XMLDSig.EXPONENT)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String text = StaxParserUtil.getElementText(xmlEventReader);
                rsaKeyValue.setExponent(text.getBytes(GeneralConstants.SAML_CHARSET));
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }
        return rsaKeyValue;
    }

    private static DSAKeyValueType parseDSAKeyValue(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, WSTrustConstants.XMLDSig.DSA_KEYVALUE);

        Element dsaElement = StaxParserUtil.getDOMElement(xmlEventReader);
        return getDSAKeyValue(dsaElement);
    }

    /**
     * Given a dsig:DSAKeyValue element, return {@link DSAKeyValueType}
     *
     * @param element
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ParsingException
     */
    private static DSAKeyValueType getDSAKeyValue(Element element) throws ParsingException {
        DSAKeyValueType dsa = new DSAKeyValueType();
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        for (int i = 0; i < length; i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String tag = childElement.getLocalName();

                byte[] text = childElement.getTextContent().getBytes(GeneralConstants.SAML_CHARSET);

                if (WSTrustConstants.XMLDSig.P.equals(tag)) {
                    dsa.setP(text);
                } else if (WSTrustConstants.XMLDSig.Q.equals(tag)) {
                    dsa.setQ(text);
                } else if (WSTrustConstants.XMLDSig.G.equals(tag)) {
                    dsa.setG(text);
                } else if (WSTrustConstants.XMLDSig.Y.equals(tag)) {
                    dsa.setY(text);
                } else if (WSTrustConstants.XMLDSig.SEED.equals(tag)) {
                    dsa.setSeed(text);
                } else if (WSTrustConstants.XMLDSig.PGEN_COUNTER.equals(tag)) {
                    dsa.setPgenCounter(text);
                }
            }
        }

        return dsa;
    }

    /**
     * Parse the {@link SAML11AttributeQueryType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11AttributeQueryType parseSAML11AttributeQuery(XMLEventReader xmlEventReader) throws ParsingException {
        SAML11AttributeQueryType query = new SAML11AttributeQueryType();
        StartElement startElement;
        // There may be additional things under subject confirmation
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                if (StaxParserUtil.matches(endElement, SAML11Constants.ATTRIBUTE_QUERY))
                    break;
                else
                    throw logger.parserUnknownEndElement(StaxParserUtil.getElementName(endElement), xmlEvent.getLocation());
            }

            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;

                String startTag = StaxParserUtil.getElementName(startElement);

                if (startTag.equals(JBossSAMLConstants.SUBJECT.get())) {
                    SAML11SubjectParser parser = new SAML11SubjectParser();
                    query.setSubject((SAML11SubjectType) parser.parse(xmlEventReader));
                } else
                    throw logger.parserUnknownTag(startTag, startElement.getLocation());
            }
        }
        return query;
    }

    /**
     * Parse the {@link SAML11AttributeQueryType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11AuthenticationQueryType parseSAML11AuthenticationQuery(XMLEventReader xmlEventReader)
            throws ParsingException {
        SAML11AuthenticationQueryType query = new SAML11AuthenticationQueryType();
        StartElement startElement;
        // There may be additional things under subject confirmation
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                if (StaxParserUtil.matches(endElement, SAML11Constants.AUTHENTICATION_QUERY))
                    break;
                else
                    throw logger.parserUnknownEndElement(StaxParserUtil.getElementName(endElement), xmlEvent.getLocation());
            }

            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;

                String startTag = StaxParserUtil.getElementName(startElement);

                if (startTag.equals(JBossSAMLConstants.SUBJECT.get())) {
                    SAML11SubjectParser parser = new SAML11SubjectParser();
                    query.setSubject((SAML11SubjectType) parser.parse(xmlEventReader));
                } else
                    throw logger.parserUnknownTag(startTag, startElement.getLocation());
            }
        }
        return query;
    }

    /**
     * Parse the {@link SAML11AuthorizationDecisionQueryType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static SAML11AuthorizationDecisionQueryType parseSAML11AuthorizationDecisionQueryType(XMLEventReader xmlEventReader)
            throws ParsingException {
        SAML11AuthorizationDecisionQueryType query = new SAML11AuthorizationDecisionQueryType();
        StartElement startElement;
        // There may be additional things under subject confirmation
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                if (StaxParserUtil.matches(endElement, SAML11Constants.AUTHORIZATION_DECISION_QUERY))
                    break;
                else
                    throw logger.parserUnknownEndElement(StaxParserUtil.getElementName(endElement), xmlEvent.getLocation());
            }

            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;

                String startTag = StaxParserUtil.getElementName(startElement);

                if (startTag.equals(JBossSAMLConstants.SUBJECT.get())) {
                    SAML11SubjectParser parser = new SAML11SubjectParser();
                    query.setSubject((SAML11SubjectType) parser.parse(xmlEventReader));
                } else if (startTag.equals(SAML11Constants.RESOURCE)) {
                    startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                    query.setResource(URI.create(StaxParserUtil.getElementText(xmlEventReader)));
                } else if (startTag.equals(SAML11Constants.ACTION)) {
                    startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                    SAML11ActionType action = new SAML11ActionType();
                    Attribute nsAttr = startElement.getAttributeByName(new QName(SAML11Constants.NAMESPACE));
                    if (nsAttr != null) {
                        action.setNamespace(StaxParserUtil.getAttributeValue(nsAttr));
                    }

                    action.setValue(StaxParserUtil.getElementText(xmlEventReader));
                    query.add(action);
                } else
                    throw logger.parserUnknownTag(startTag, startElement.getLocation());
            }
        }
        return query;
    }
}
