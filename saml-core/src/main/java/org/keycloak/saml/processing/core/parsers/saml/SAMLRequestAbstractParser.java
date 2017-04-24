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

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.net.URI;

/**
 * Base Class for SAML Request Parsing
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 2, 2010
 */
public abstract class SAMLRequestAbstractParser {

    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    protected String id;

    protected String version;

    protected XMLGregorianCalendar issueInstant;

    protected void parseRequiredAttributes(StartElement startElement) throws ParsingException {
        Attribute idAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.ID.get()));
        if (idAttr == null)
            throw logger.parserRequiredAttribute("ID");

        id = StaxParserUtil.getAttributeValue(idAttr);

        Attribute versionAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.VERSION.get()));
        if (versionAttr == null)
            throw logger.parserRequiredAttribute("Version");
        version = StaxParserUtil.getAttributeValue(versionAttr);

        Attribute issueInstantAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.ISSUE_INSTANT.get()));
        if (issueInstantAttr == null)
            throw logger.parserRequiredAttribute("IssueInstant");
        issueInstant = XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(issueInstantAttr));
    }

    /**
     * Parse the attributes that are common to all SAML Request Types
     *
     * @param startElement
     * @param request
     *
     * @throws ParsingException
     */
    protected void parseBaseAttributes(StartElement startElement, RequestAbstractType request) throws ParsingException {
        Attribute destinationAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.DESTINATION.get()));
        if (destinationAttr != null)
            request.setDestination(URI.create(StaxParserUtil.getAttributeValue(destinationAttr)));

        Attribute consent = startElement.getAttributeByName(new QName(JBossSAMLConstants.CONSENT.get()));
        if (consent != null)
            request.setConsent(StaxParserUtil.getAttributeValue(consent));
    }

    protected void parseCommonElements(StartElement startElement, XMLEventReader xmlEventReader, RequestAbstractType request)
            throws ParsingException {
        if (startElement == null)
            throw logger.parserNullStartElement();
        String elementName = StaxParserUtil.getStartElementName(startElement);

        if (JBossSAMLConstants.ISSUER.get().equals(elementName)) {
            startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
            NameIDType issuer = new NameIDType();
            issuer.setValue(StaxParserUtil.getElementText(xmlEventReader));
            request.setIssuer(issuer);
        } else if (JBossSAMLConstants.SIGNATURE.get().equals(elementName)) {
            request.setSignature(StaxParserUtil.getDOMElement(xmlEventReader));
        } else if (JBossSAMLConstants.EXTENSIONS.get().equals(elementName)) {
            request.setExtensions(new SAMLExtensionsParser().parse(xmlEventReader));
        }
    }

    protected SubjectType getSubject(XMLEventReader xmlEventReader) throws ParsingException {
        SAMLSubjectParser subjectParser = new SAMLSubjectParser();
        return (SubjectType) subjectParser.parse(xmlEventReader);
    }
}