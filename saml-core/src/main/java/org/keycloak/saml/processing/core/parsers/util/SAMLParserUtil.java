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

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.constants.WSTrustConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.core.saml.v2.util.SignatureUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextClassRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextDeclRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextDeclType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectLocalityType;
import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509CertificateType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509DataType;
import org.w3c.dom.Element;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Utility methods for SAML Parser
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 4, 2010
 */
public class SAMLParserUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    public static KeyInfoType parseKeyInfo(XMLEventReader xmlEventReader) throws ParsingException {
        KeyInfoType keyInfo = new KeyInfoType();
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, WSTrustConstants.XMLDSig.KEYINFO);

        XMLEvent xmlEvent = null;
        String tag = null;

        while (xmlEventReader.hasNext()) {
            xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                tag = StaxParserUtil.getEndElementName((EndElement) xmlEvent);
                if (tag.equals(WSTrustConstants.XMLDSig.KEYINFO)) {
                    xmlEvent = StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw logger.parserUnknownEndElement(tag);
            }
            startElement = (StartElement) xmlEvent;
            tag = StaxParserUtil.getStartElementName(startElement);
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
                cert.setEncodedCertificate(certValue.getBytes());
                x509.add(cert);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, WSTrustConstants.XMLDSig.X509DATA);
                keyInfo.addContent(x509);
            } else if (tag.equals(WSTrustConstants.XMLDSig.KEYVALUE)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                KeyValueType keyValue = null;

                startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
                tag = StaxParserUtil.getStartElementName(startElement);
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

    private static RSAKeyValueType parseRSAKeyValue(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, WSTrustConstants.XMLDSig.RSA_KEYVALUE);

        XMLEvent xmlEvent = null;
        String tag = null;

        RSAKeyValueType rsaKeyValue = new RSAKeyValueType();

        while (xmlEventReader.hasNext()) {
            xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                tag = StaxParserUtil.getEndElementName((EndElement) xmlEvent);
                if (tag.equals(WSTrustConstants.XMLDSig.RSA_KEYVALUE)) {
                    xmlEvent = StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw logger.parserUnknownEndElement(tag);
            }

            startElement = (StartElement) xmlEvent;
            tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(WSTrustConstants.XMLDSig.MODULUS)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String text = StaxParserUtil.getElementText(xmlEventReader);
                rsaKeyValue.setModulus(text.getBytes());
            } else if (tag.equals(WSTrustConstants.XMLDSig.EXPONENT)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String text = StaxParserUtil.getElementText(xmlEventReader);
                rsaKeyValue.setExponent(text.getBytes());
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }
        return rsaKeyValue;
    }

    private static DSAKeyValueType parseDSAKeyValue(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, WSTrustConstants.XMLDSig.DSA_KEYVALUE);

        Element dsaElement = StaxParserUtil.getDOMElement(xmlEventReader);
        return SignatureUtil.getDSAKeyValue(dsaElement);
    }

    /**
     * Parse an {@code AttributeStatementType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static AttributeStatementType parseAttributeStatement(XMLEventReader xmlEventReader) throws ParsingException {
        AttributeStatementType attributeStatementType = new AttributeStatementType();

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
                AttributeType attribute = parseAttribute(xmlEventReader);
                attributeStatementType.addAttribute(new ASTChoiceType(attribute));
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }
        return attributeStatementType;
    }

    /**
     * Parse an {@code AttributeType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static AttributeType parseAttribute(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ATTRIBUTE.get());
        AttributeType attributeType = null;

        Attribute name = startElement.getAttributeByName(new QName(JBossSAMLConstants.NAME.get()));
        if (name == null)
            throw logger.parserRequiredAttribute("Name");
        attributeType = new AttributeType(StaxParserUtil.getAttributeValue(name));

        parseAttributeType(xmlEventReader, startElement, JBossSAMLConstants.ATTRIBUTE.get(), attributeType);

        return attributeType;
    }

    /**
     * Parse an {@code AttributeType}
     *
     * @param xmlEventReader
     *
     * @throws ParsingException
     */
    public static void parseAttributeType(XMLEventReader xmlEventReader, StartElement startElement, String rootTag,
            AttributeType attributeType) throws ParsingException {
        // Look for X500 Encoding
        QName x500EncodingName = new QName(JBossSAMLURIConstants.X500_NSURI.get(), JBossSAMLConstants.ENCODING.get(),
                JBossSAMLURIConstants.X500_PREFIX.get());
        Attribute x500EncodingAttr = startElement.getAttributeByName(x500EncodingName);

        if (x500EncodingAttr != null) {
            attributeType.getOtherAttributes().put(x500EncodingAttr.getName(),
                    StaxParserUtil.getAttributeValue(x500EncodingAttr));
        }

        Attribute friendlyName = startElement.getAttributeByName(new QName(JBossSAMLConstants.FRIENDLY_NAME.get()));
        if (friendlyName != null)
            attributeType.setFriendlyName(StaxParserUtil.getAttributeValue(friendlyName));

        Attribute nameFormat = startElement.getAttributeByName(new QName(JBossSAMLConstants.NAME_FORMAT.get()));
        if (nameFormat != null)
            attributeType.setNameFormat(StaxParserUtil.getAttributeValue(nameFormat));

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
            String tag = StaxParserUtil.getStartElementName(startElement);

            if (JBossSAMLConstants.ATTRIBUTE.get().equals(tag))
                break;

            if (JBossSAMLConstants.ATTRIBUTE_VALUE.get().equals(tag)) {
                Object attributeValue = parseAttributeValue(xmlEventReader);
                attributeType.addAttributeValue(attributeValue);
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
            if (StaxParserUtil.hasTextAhead(xmlEventReader)) {
                return StaxParserUtil.getElementText(xmlEventReader);
            }
            // Else we may have Child Element
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;
                String tag = StaxParserUtil.getStartElementName(startElement);
                if (tag.equals(JBossSAMLConstants.NAMEID.get())) {
                    return parseNameIDType(xmlEventReader);
                }
            } else if (xmlEvent instanceof EndElement) {
                return "";
            }

            throw logger.unsupportedType(StaxParserUtil.getStartElementName(startElement));
        }
        //      RK Added an additional type check for base64Binary type as calheers is passing this type
        String typeValue = StaxParserUtil.getAttributeValue(type);
        if (typeValue.contains(":string")) {
            return StaxParserUtil.getElementText(xmlEventReader);
        } else if (typeValue.contains(":anyType")) {
            // TODO: for now assume that it is a text value that can be parsed and set as the attribute value
            return StaxParserUtil.getElementText(xmlEventReader);
        } else if(typeValue.contains(":base64Binary")){
            return StaxParserUtil.getElementText(xmlEventReader);
        }


        throw logger.parserUnknownXSI(typeValue);
    }

    /**
     * Parse the AuthnStatement inside the assertion
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static AuthnStatementType parseAuthnStatement(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        String AUTHNSTATEMENT = JBossSAMLConstants.AUTHN_STATEMENT.get();
        StaxParserUtil.validate(startElement, AUTHNSTATEMENT);

        Attribute authnInstant = startElement.getAttributeByName(new QName("AuthnInstant"));
        if (authnInstant == null)
            throw logger.parserRequiredAttribute("AuthnInstant");

        XMLGregorianCalendar issueInstant = XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(authnInstant));
        AuthnStatementType authnStatementType = new AuthnStatementType(issueInstant);

        Attribute sessionIndex = startElement.getAttributeByName(new QName("SessionIndex"));
        if (sessionIndex != null)
            authnStatementType.setSessionIndex(StaxParserUtil.getAttributeValue(sessionIndex));

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;

            if (xmlEvent instanceof EndElement) {
                xmlEvent = StaxParserUtil.getNextEvent(xmlEventReader);
                EndElement endElement = (EndElement) xmlEvent;
                String endElementTag = StaxParserUtil.getEndElementName(endElement);
                if (endElementTag.equals(AUTHNSTATEMENT))
                    break;
                else
                    throw logger.parserUnknownEndElement(endElementTag);
            }
            startElement = null;

            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;
            } else {
                startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            }
            if (startElement == null)
                break;

            String tag = StaxParserUtil.getStartElementName(startElement);

            if (JBossSAMLConstants.SUBJECT_LOCALITY.get().equals(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                SubjectLocalityType subjectLocalityType = new SubjectLocalityType();
                Attribute address = startElement.getAttributeByName(new QName(JBossSAMLConstants.ADDRESS.get()));
                if (address != null) {
                    subjectLocalityType.setAddress(StaxParserUtil.getAttributeValue(address));
                }
                Attribute dns = startElement.getAttributeByName(new QName(JBossSAMLConstants.DNS_NAME.get()));
                if (dns != null) {
                    subjectLocalityType.setDNSName(StaxParserUtil.getAttributeValue(dns));
                }
                authnStatementType.setSubjectLocality(subjectLocalityType);
                StaxParserUtil.validate(StaxParserUtil.getNextEndElement(xmlEventReader),
                        JBossSAMLConstants.SUBJECT_LOCALITY.get());
            } else if (JBossSAMLConstants.AUTHN_CONTEXT.get().equals(tag)) {
                authnStatementType.setAuthnContext(parseAuthnContextType(xmlEventReader));
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());

        }

        return authnStatementType;
    }

    /**
     * Parse the AuthnContext Type inside the AuthnStatement
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static AuthnContextType parseAuthnContextType(XMLEventReader xmlEventReader) throws ParsingException {
        AuthnContextType authnContextType = new AuthnContextType();

        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.AUTHN_CONTEXT.get());

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;

            if (xmlEvent instanceof EndElement) {
                xmlEvent = StaxParserUtil.getNextEvent(xmlEventReader);
                EndElement endElement = (EndElement) xmlEvent;
                String endElementTag = StaxParserUtil.getEndElementName(endElement);
                if (endElementTag.equals(JBossSAMLConstants.AUTHN_CONTEXT.get()))
                    break;
                else
                    throw logger.parserUnknownEndElement(endElementTag);
            }
            startElement = null;

            if (xmlEvent instanceof StartElement) {
                startElement = (StartElement) xmlEvent;
            } else {
                startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            }
            if (startElement == null)
                break;

            String tag = StaxParserUtil.getStartElementName(startElement);

            if (JBossSAMLConstants.AUTHN_CONTEXT_DECLARATION.get().equals(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);

                Element dom = StaxParserUtil.getDOMElement(xmlEventReader);

                AuthnContextDeclType authnContextDecl = new AuthnContextDeclType(dom);
                AuthnContextType.AuthnContextTypeSequence authnContextSequence = authnContextType.new AuthnContextTypeSequence();
                authnContextSequence.setAuthnContextDecl(authnContextDecl);
                authnContextType.setSequence(authnContextSequence);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.AUTHN_CONTEXT_DECLARATION.get());
            } else if (JBossSAMLConstants.AUTHN_CONTEXT_DECLARATION_REF.get().equals(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String text = StaxParserUtil.getElementText(xmlEventReader);

                AuthnContextDeclRefType aAuthnContextDeclType = new AuthnContextDeclRefType(URI.create(text));
                authnContextType.addURIType(aAuthnContextDeclType);
            } else if (JBossSAMLConstants.AUTHN_CONTEXT_CLASS_REF.get().equals(tag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String text = StaxParserUtil.getElementText(xmlEventReader);

                AuthnContextClassRefType aAuthnContextClassRefType = new AuthnContextClassRefType(URI.create(text));
                AuthnContextType.AuthnContextTypeSequence authnContextSequence = authnContextType.new AuthnContextTypeSequence();
                authnContextSequence.setClassRef(aAuthnContextClassRefType);

                authnContextType.setSequence(authnContextSequence);
            } else if (JBossSAMLConstants.AUTHENTICATING_AUTHORITY.get().equals(tag)) {
              startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
              String text = StaxParserUtil.getElementText(xmlEventReader);
              authnContextType.addAuthenticatingAuthority(URI.create(text));
            } else
                throw logger.parserUnknownTag(tag, startElement.getLocation());
        }

        return authnContextType;
    }

    /**
     * Parse a {@code NameIDType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static NameIDType parseNameIDType(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement nameIDElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        NameIDType nameID = new NameIDType();

        Attribute nameQualifier = nameIDElement.getAttributeByName(new QName(JBossSAMLConstants.NAME_QUALIFIER.get()));
        if (nameQualifier != null) {
            nameID.setNameQualifier(StaxParserUtil.getAttributeValue(nameQualifier));
        }

        Attribute format = nameIDElement.getAttributeByName(new QName(JBossSAMLConstants.FORMAT.get()));
        if (format != null) {
            nameID.setFormat(URI.create(StaxParserUtil.getAttributeValue(format)));
        }

        Attribute spProvidedID = nameIDElement.getAttributeByName(new QName(JBossSAMLConstants.SP_PROVIDED_ID.get()));
        if (spProvidedID != null) {
            nameID.setSPProvidedID(StaxParserUtil.getAttributeValue(spProvidedID));
        }

        Attribute spNameQualifier = nameIDElement.getAttributeByName(new QName(JBossSAMLConstants.SP_NAME_QUALIFIER.get()));
        if (spNameQualifier != null) {
            nameID.setSPNameQualifier(StaxParserUtil.getAttributeValue(spNameQualifier));
        }

        String nameIDValue = StaxParserUtil.getElementText(xmlEventReader);
        nameID.setValue(nameIDValue);

        return nameID;
    }

    /**
     * Parse a space delimited list of strings
     *
     * @param startElement
     *
     * @return
     */
    public static List<String> parseProtocolEnumeration(StartElement startElement) {
        List<String> protocolEnum = new ArrayList<String>();
        Attribute proto = startElement.getAttributeByName(new QName(JBossSAMLConstants.PROTOCOL_SUPPORT_ENUMERATION.get()));
        String val = StaxParserUtil.getAttributeValue(proto);
        if (StringUtil.isNotNull(val)) {
            StringTokenizer st = new StringTokenizer(val);
            while (st.hasMoreTokens()) {
                protocolEnum.add(st.nextToken());
            }

        }
        return protocolEnum;
    }
}