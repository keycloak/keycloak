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
package org.keycloak.saml.processing.core.parsers.saml.protocol;

import org.keycloak.dom.saml.v2.assertion.EncryptedElementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;

import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import org.w3c.dom.Element;
import static org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLRequestAbstractParser.VERSION_2_0;

/**
 * Parse the Single Log Out requests
 *
 * @since Nov 3, 2010
 */
public class SAMLSloRequestParser extends SAMLRequestAbstractParser<LogoutRequestType> {

    private static final SAMLSloRequestParser INSTANCE = new SAMLSloRequestParser();

    private SAMLSloRequestParser() {
        super(SAMLProtocolQNames.LOGOUT_REQUEST);
    }

    public static SAMLSloRequestParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected LogoutRequestType instantiateElement(XMLEventReader xmlEventReader, StartElement startElement) throws ParsingException {
        SAMLParserUtil.validateAttributeValue(startElement, SAMLProtocolQNames.ATTR_VERSION, VERSION_2_0);
        String id = StaxParserUtil.getRequiredAttributeValue(startElement, SAMLProtocolQNames.ATTR_ID);
        XMLGregorianCalendar issueInstant = XMLTimeUtil.parse(StaxParserUtil.getRequiredAttributeValue(startElement, SAMLProtocolQNames.ATTR_ISSUE_INSTANT));

        LogoutRequestType logoutRequest = new LogoutRequestType(id, issueInstant);
        super.parseBaseAttributes(startElement, logoutRequest);

        logoutRequest.setReason(StaxParserUtil.getAttributeValue(startElement, SAMLProtocolQNames.ATTR_REASON));
        logoutRequest.setNotOnOrAfter(StaxParserUtil.getXmlTimeAttributeValue(startElement, SAMLProtocolQNames.ATTR_NOT_ON_OR_AFTER));

        return logoutRequest;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, LogoutRequestType target,
      SAMLProtocolQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ISSUER:
            case SIGNATURE:
            case EXTENSIONS:
                parseCommonElements(element, elementDetail, xmlEventReader, target);
                break;

            case NAMEID:
                NameIDType nameID = SAMLParserUtil.parseNameIDType(xmlEventReader);
                target.setNameID(nameID);
                break;

            case ENCRYPTED_ID:
                Element domElement = StaxParserUtil.getDOMElement(xmlEventReader);
                target.setEncryptedID(new EncryptedElementType(domElement));
                break;

            case SESSION_INDEX:
                StaxParserUtil.getNextStartElement(xmlEventReader);
                target.addSessionIndex(StaxParserUtil.getElementText(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}