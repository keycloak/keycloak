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

import org.keycloak.dom.saml.v1.assertion.SAML11AssertionType;
import org.keycloak.dom.saml.v1.protocol.SAML11ResponseType;
import org.keycloak.dom.saml.v1.protocol.SAML11StatusCodeType;
import org.keycloak.dom.saml.v1.protocol.SAML11StatusType;
import org.keycloak.dom.saml.v2.protocol.StatusDetailType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.keycloak.saml.common.parsers.StaxParser;

/**
 * Parse the SAML 11 Response
 *
 * @author Anil.Saldhana@redhat.com
 * @since 23 June 2011
 */
public class SAML11ResponseParser implements StaxParser {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private final String RESPONSE = JBossSAMLConstants.RESPONSE__PROTOCOL.get();

    /**
     * @see {@link ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        // Get the startelement
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, RESPONSE);

        Attribute idAttr = startElement.getAttributeByName(new QName(SAML11Constants.RESPONSE_ID));
        if (idAttr == null)
            throw logger.parserRequiredAttribute(SAML11Constants.RESPONSE_ID);
        String id = StaxParserUtil.getAttributeValue(idAttr);

        Attribute issueInstant = startElement.getAttributeByName(new QName(SAML11Constants.ISSUE_INSTANT));
        if (issueInstant == null)
            throw logger.parserRequiredAttribute(SAML11Constants.ISSUE_INSTANT);
        XMLGregorianCalendar issueInstantVal = XMLTimeUtil.parse(StaxParserUtil.getAttributeValue(issueInstant));

        SAML11ResponseType response = new SAML11ResponseType(id, issueInstantVal);

        while (xmlEventReader.hasNext()) {
            // Let us peek at the next start element
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String elementName = StaxParserUtil.getElementName(startElement);
            if (JBossSAMLConstants.SIGNATURE.get().equals(elementName)) {
                Element sig = StaxParserUtil.getDOMElement(xmlEventReader);
                response.setSignature(sig);
            } else if (JBossSAMLConstants.ASSERTION.get().equals(elementName)) {
                SAML11AssertionParser assertionParser = new SAML11AssertionParser();
                response.add((SAML11AssertionType) assertionParser.parse(xmlEventReader));
            } else if (JBossSAMLConstants.STATUS.get().equals(elementName)) {
                response.setStatus(parseStatus(xmlEventReader));
            } else
                throw logger.parserUnknownStartElement(elementName, startElement.getLocation());
        }

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
    protected SAML11StatusType parseStatus(XMLEventReader xmlEventReader) throws ParsingException {
        // Get the Start Element
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        String STATUS = JBossSAMLConstants.STATUS.get();
        StaxParserUtil.validate(startElement, STATUS);

        SAML11StatusType status = new SAML11StatusType();

        while (xmlEventReader.hasNext()) {
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);

            if (startElement == null)
                break;

            QName startElementName = startElement.getName();
            String elementTag = startElementName.getLocalPart();

            SAML11StatusCodeType statusCode = null;

            if (JBossSAMLConstants.STATUS_CODE.get().equals(elementTag)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                if (startElement == null)
                    break;
                Attribute valueAttr = startElement.getAttributeByName(new QName("Value"));
                if (valueAttr != null) {
                    statusCode = new SAML11StatusCodeType(new QName(StaxParserUtil.getAttributeValue(valueAttr)));
                }
                status.setStatusCode(statusCode);

                // Peek at the next start element to see if it is status code
                startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
                elementTag = startElement.getName().getLocalPart();
                if (JBossSAMLConstants.STATUS_CODE.get().equals(elementTag)) {
                    SAML11StatusCodeType subStatusCodeType = null;
                    startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                    Attribute subValueAttr = startElement.getAttributeByName(new QName("Value"));
                    if (subValueAttr != null) {
                        subStatusCodeType = new SAML11StatusCodeType(new QName(StaxParserUtil.getAttributeValue(subValueAttr)));
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
                    throw logger.parserUnknownEndElement(StaxParserUtil.getElementName(endElement), xmlEvent.getLocation());
            } else
                break;
        }
        return status;
    }

}