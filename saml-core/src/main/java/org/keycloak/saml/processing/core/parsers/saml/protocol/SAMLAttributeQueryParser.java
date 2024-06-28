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

import org.keycloak.dom.saml.v2.protocol.AttributeQueryType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLSubjectParser;
import org.keycloak.saml.common.parsers.StaxParser;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import javax.xml.datatype.XMLGregorianCalendar;
import static org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLRequestAbstractParser.VERSION_2_0;

/**
 * Parse the {@link org.keycloak.dom.saml.v2.protocol.ArtifactResolveType}
 *
 * @since Jul 1, 2011
 */
public class SAMLAttributeQueryParser extends SAMLRequestAbstractParser<AttributeQueryType> implements StaxParser {

    private static final SAMLAttributeQueryParser INSTANCE = new SAMLAttributeQueryParser();

    private SAMLAttributeQueryParser() {
        super(SAMLProtocolQNames.ATTRIBUTE_QUERY);
    }

    public static SAMLAttributeQueryParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AttributeQueryType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        SAMLParserUtil.validateAttributeValue(element, SAMLProtocolQNames.ATTR_VERSION, VERSION_2_0);
        String id = StaxParserUtil.getRequiredAttributeValue(element, SAMLProtocolQNames.ATTR_ID);
        XMLGregorianCalendar issueInstant = XMLTimeUtil.parse(StaxParserUtil.getRequiredAttributeValue(element, SAMLProtocolQNames.ATTR_ISSUE_INSTANT));

        AttributeQueryType authnRequest = new AttributeQueryType(id, issueInstant);
        super.parseBaseAttributes(element, authnRequest);

        return authnRequest;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AttributeQueryType target, SAMLProtocolQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ISSUER:
            case SIGNATURE:
            case EXTENSIONS:
                parseCommonElements(element, elementDetail, xmlEventReader, target);
                break;

            case SUBJECT:
                target.setSubject(SAMLSubjectParser.getInstance().parse(xmlEventReader));
                break;

            case ATTRIBUTE:
                target.add(SAMLAttributeParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}