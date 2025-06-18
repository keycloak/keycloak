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

import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Base Class for SAML Request Parsing
 *
 * @since Nov 2, 2010
 */
public abstract class SAMLRequestAbstractParser<T extends RequestAbstractType> extends AbstractStaxSamlProtocolParser<T> {

    protected static final String VERSION_2_0 = "2.0";

    protected SAMLRequestAbstractParser(SAMLProtocolQNames expectedStartElement) {
        super(expectedStartElement);
    }

    /**
     * Parse the attributes that are common to all SAML Request Types
     *
     * @param startElement
     * @param request
     *
     * @throws ParsingException
     */
    protected void parseBaseAttributes(StartElement startElement, T request) throws ParsingException {
        request.setDestination(StaxParserUtil.getUriAttributeValue(startElement, SAMLProtocolQNames.ATTR_DESTINATION));
        request.setConsent(StaxParserUtil.getAttributeValue(startElement, SAMLProtocolQNames.ATTR_CONSENT));
    }

    /**
     * If the current element is one of the common request elements (Issuer, Signature, Extensions), parses it.
     * @param element
     * @param xmlEventReader
     * @param request
     * @throws ParsingException
     */
    protected void parseCommonElements(SAMLProtocolQNames element, StartElement elementDetail, XMLEventReader xmlEventReader, RequestAbstractType request)
            throws ParsingException {
        switch (element) {
            case ISSUER:
                request.setIssuer(SAMLParserUtil.parseNameIDType(xmlEventReader));
                break;

            case SIGNATURE:
                request.setSignature(StaxParserUtil.getDOMElement(xmlEventReader));
                break;
            
            case EXTENSIONS:
                request.setExtensions(SAMLExtensionsParser.getInstance().parse(xmlEventReader));
                break;
        }
    }
}