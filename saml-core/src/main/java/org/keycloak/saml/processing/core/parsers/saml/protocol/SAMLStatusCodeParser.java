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

import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Base Class for all Response Type parsing for SAML2
 *
 */
public class SAMLStatusCodeParser extends AbstractStaxSamlProtocolParser<StatusCodeType> {

    private static final SAMLStatusCodeParser INSTANCE = new SAMLStatusCodeParser();

    private SAMLStatusCodeParser() {
        super(SAMLProtocolQNames.STATUS_CODE);
    }

    public static SAMLStatusCodeParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected StatusCodeType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        final StatusCodeType res = new StatusCodeType();
        res.setValue(StaxParserUtil.getUriAttributeValue(element, SAMLProtocolQNames.ATTR_VALUE));
        return res;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, StatusCodeType target, SAMLProtocolQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case STATUS_CODE:
                target.setStatusCode(SAMLStatusCodeParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}