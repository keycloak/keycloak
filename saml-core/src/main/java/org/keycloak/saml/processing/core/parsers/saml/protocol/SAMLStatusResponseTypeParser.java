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

import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import javax.xml.stream.events.StartElement;

/**
 * Base Class for all Response Type parsing for SAML2
 *
 */
public abstract class SAMLStatusResponseTypeParser<T extends StatusResponseType> extends AbstractStaxSamlProtocolParser<T> {

    protected static final String VERSION_2_0 = "2.0";

    protected SAMLStatusResponseTypeParser(SAMLProtocolQNames expectedStartElement) {
        super(expectedStartElement);
    }

    /**
     * Parse the attributes that are common to all SAML Response Types
     *
     * @param startElement
     * @param response
     *
     * @throws org.keycloak.saml.common.exceptions.ParsingException
     */
    protected void parseBaseAttributes(StartElement startElement, T response) throws ParsingException {
        response.setDestination(StaxParserUtil.getAttributeValue(startElement, SAMLProtocolQNames.ATTR_DESTINATION));
        response.setConsent(StaxParserUtil.getAttributeValue(startElement, SAMLProtocolQNames.ATTR_CONSENT));
        response.setInResponseTo(StaxParserUtil.getAttributeValue(startElement, SAMLProtocolQNames.ATTR_IN_RESPONSE_TO));
    }
}