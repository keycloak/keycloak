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

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusDetailType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.net.URI;

/**
 * Base Class for all Response Type parsing for SAML2
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 2, 2010
 */
public abstract class SAMLStatusResponseTypeParser {

    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Parse the attributes that are common to all SAML Response Types
     *
     * @param startElement
     * @param response
     *
     * @throws org.keycloak.saml.common.exceptions.ParsingException
     */
    protected StatusResponseType parseBaseAttributes(StartElement startElement) throws ParsingException {
        Attribute idAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.ID.get()));
        if (idAttr == null)
            throw logger.parserRequiredAttribute("ID");
        String id = StaxParserUtil.getAttributeValue(idAttr);

        Attribute version = startElement.getAttributeByName(new QName(JBossSAMLConstants.VERSION.get()));
        if (version == null)
            throw logger.parserRequiredAttribute("Version");

        StringUtil.match(JBossSAMLConstants.VERSION_2_0.get(), StaxParserUtil.getAttributeValue(version));

        Attribute issueInstant = startElement.getAttributeByName(new QName(JBossSAMLConstants.ISSUE_INSTANT.get()));
        if (issueInstant == null)
            throw logger.parserRequiredAttribute("IssueInstant");
        XMLGregorianCalendar issueInstantVal = XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(issueInstant));

        StatusResponseType response = new StatusResponseType(id, issueInstantVal);

        Attribute destination = startElement.getAttributeByName(new QName(JBossSAMLConstants.DESTINATION.get()));
        if (destination != null)
            response.setDestination(StaxParserUtil.getAttributeValue(destination));

        Attribute consent = startElement.getAttributeByName(new QName(JBossSAMLConstants.CONSENT.get()));
        if (consent != null)
            response.setConsent(StaxParserUtil.getAttributeValue(consent));

        Attribute inResponseTo = startElement.getAttributeByName(new QName(JBossSAMLConstants.IN_RESPONSE_TO.get()));
        if (inResponseTo != null)
            response.setInResponseTo(StaxParserUtil.getAttributeValue(inResponseTo));
        return response;
    }

    /**
     * Parse the status element
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    protected StatusType parseStatus(XMLEventReader xmlEventReader) throws ParsingException {
        // Get the Start Element
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        String STATUS = JBossSAMLConstants.STATUS.get();
        StaxParserUtil.validate(startElement, STATUS);

        StatusType status = new StatusType();

        while (xmlEventReader.hasNext()) {
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);

            if (startElement == null)
                break;

            QName startElementName = startElement.getName();
            String elementTag = startElementName.getLocalPart();

            StatusCodeType statusCode = new StatusCodeType();

            if (JBossSAMLConstants.STATUS_CODE.get().equals(elementTag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                if (startElement == null)
                    break;
                Attribute valueAttr = startElement.getAttributeByName(new QName("Value"));
                if (valueAttr != null) {
                    statusCode.setValue(URI.create(StaxParserUtil.getAttributeValue(valueAttr)));
                }
                status.setStatusCode(statusCode);

                // Peek at the next start element to see if it is status code
                startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
                if (startElement == null) {
                    // Go to Status code end element.
                    EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                    if (endElement != null) {
                        StaxParserUtil.validate(endElement, JBossSAMLConstants.STATUS_CODE.get());
                    }
                    continue;
                }
                elementTag = startElement.getName().getLocalPart();
                if (JBossSAMLConstants.STATUS_CODE.get().equals(elementTag)) {
                    StatusCodeType subStatusCodeType = new StatusCodeType();
                    startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                    Attribute subValueAttr = startElement.getAttributeByName(new QName("Value"));
                    if (subValueAttr != null) {
                        subStatusCodeType.setValue(URI.create(StaxParserUtil.getAttributeValue(subValueAttr)));
                    }
                    statusCode.setStatusCode(subStatusCodeType);

                    // Go to Status code end element.
                    EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                    StaxParserUtil.validate(endElement, JBossSAMLConstants.STATUS_CODE.get());
                    continue;
                }
            }
            if (JBossSAMLConstants.STATUS_MESSAGE.get().equals(elementTag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                if (startElement == null)
                    break;
                status.setStatusMessage(StaxParserUtil.getElementText(xmlEventReader));
            }

            if (JBossSAMLConstants.STATUS_DETAIL.get().equals(elementTag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                if (startElement == null)
                    break;
                Element domElement = StaxParserUtil.getDOMElement(xmlEventReader);
                StatusDetailType statusDetailType = new StatusDetailType();
                statusDetailType.addStatusDetail(domElement);
                status.setStatusDetail(statusDetailType);
            }

            // Get the next end element
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                if (StaxParserUtil.matches(endElement, STATUS))
                    break;
                else
                    throw logger.parserUnknownEndElement(StaxParserUtil.getEndElementName(endElement));
            } else
                break;
        }
        return status;
    }
}