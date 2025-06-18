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

import org.keycloak.dom.saml.v2.protocol.StatusDetailType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AnyDomParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.w3c.dom.Element;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import java.util.List;

/**
 * Base Class for all Response Type parsing for SAML2
 *
 */
public class SAMLStatusParser extends AbstractStaxSamlProtocolParser<StatusType> {

    private static final SAMLStatusParser INSTANCE = new SAMLStatusParser();
    private static final AnyDomParser STATUS_DETAIL_PARSER = AnyDomParser.getInstance(SAMLProtocolQNames.STATUS_DETAIL.getQName());

    private SAMLStatusParser() {
        super(SAMLProtocolQNames.STATUS);
    }

    public static SAMLStatusParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected StatusType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new StatusType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, StatusType target, SAMLProtocolQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case STATUS_CODE:
                target.setStatusCode(SAMLStatusCodeParser.getInstance().parse(xmlEventReader));
                break;

            case STATUS_MESSAGE:
                StaxParserUtil.advance(xmlEventReader);
                target.setStatusMessage(StaxParserUtil.getElementText(xmlEventReader));
                break;

            case STATUS_DETAIL:
                List<Element> elements = STATUS_DETAIL_PARSER.parse(xmlEventReader);
                
                StatusDetailType statusDetailType = new StatusDetailType();
                for (Element e : elements) {
                    statusDetailType.addStatusDetail(e);
                }
                target.setStatusDetail(statusDetailType);
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}