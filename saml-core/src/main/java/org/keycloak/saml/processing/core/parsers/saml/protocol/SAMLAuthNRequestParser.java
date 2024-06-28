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

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLConditionsParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLSubjectParser;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import javax.xml.datatype.XMLGregorianCalendar;
import static org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLRequestAbstractParser.VERSION_2_0;

/**
 * Parse the SAML2 AuthnRequest
 *
 * @since Nov 2, 2010
 */
public class SAMLAuthNRequestParser extends SAMLRequestAbstractParser<AuthnRequestType> {

    private static final SAMLAuthNRequestParser INSTANCE = new SAMLAuthNRequestParser();

    private SAMLAuthNRequestParser() {
        super(SAMLProtocolQNames.AUTHN_REQUEST);
    }

    public static SAMLAuthNRequestParser getInstance() {
        return INSTANCE;
    }

    /**
     * Parse the attributes at the authnrequesttype element
     *
     * @param startElement
     *
     * @return
     *
     * @throws ParsingException
     */
    @Override
    protected AuthnRequestType instantiateElement(XMLEventReader xmlEventReader, StartElement startElement) throws ParsingException {
        SAMLParserUtil.validateAttributeValue(startElement, SAMLProtocolQNames.ATTR_VERSION, VERSION_2_0);
        String id = StaxParserUtil.getRequiredAttributeValue(startElement, SAMLProtocolQNames.ATTR_ID);
        XMLGregorianCalendar issueInstant = XMLTimeUtil.parse(StaxParserUtil.getRequiredAttributeValue(startElement, SAMLProtocolQNames.ATTR_ISSUE_INSTANT));

        AuthnRequestType authnRequest = new AuthnRequestType(id, issueInstant);
        super.parseBaseAttributes(startElement, authnRequest);

        authnRequest.setAssertionConsumerServiceURL(StaxParserUtil.getUriAttributeValue(startElement, SAMLProtocolQNames.ATTR_ASSERTION_CONSUMER_SERVICE_URL));
        authnRequest.setAssertionConsumerServiceIndex(StaxParserUtil.getIntegerAttributeValue(startElement, SAMLProtocolQNames.ATTR_ASSERTION_CONSUMER_SERVICE_INDEX));
        authnRequest.setAttributeConsumingServiceIndex(StaxParserUtil.getIntegerAttributeValue(startElement, SAMLProtocolQNames.ATTR_ATTRIBUTE_CONSUMING_SERVICE_INDEX));
        authnRequest.setForceAuthn(StaxParserUtil.getBooleanAttributeValue(startElement, SAMLProtocolQNames.ATTR_FORCE_AUTHN));
        authnRequest.setIsPassive(StaxParserUtil.getBooleanAttributeValue(startElement, SAMLProtocolQNames.ATTR_IS_PASSIVE));
        authnRequest.setProtocolBinding(StaxParserUtil.getUriAttributeValue(startElement, SAMLProtocolQNames.ATTR_PROTOCOL_BINDING));
        authnRequest.setProviderName(StaxParserUtil.getAttributeValue(startElement, SAMLProtocolQNames.ATTR_PROVIDER_NAME));

        return authnRequest;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AuthnRequestType target, SAMLProtocolQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ISSUER:
            case SIGNATURE:
            case EXTENSIONS:
                parseCommonElements(element, elementDetail, xmlEventReader, target);
                break;

            case NAMEID_POLICY:
                StaxParserUtil.advance(xmlEventReader);
                target.setNameIDPolicy(getNameIDPolicy(elementDetail));
                break;

            case SUBJECT:
                target.setSubject(SAMLSubjectParser.getInstance().parse(xmlEventReader));
                break;

            case CONDITIONS:
                target.setConditions(SAMLConditionsParser.getInstance().parse(xmlEventReader));
                break;

            case REQUESTED_AUTHN_CONTEXT:
                target.setRequestedAuthnContext(SAMLRequestedAuthnContextParser.getInstance().parse(xmlEventReader));
                break;

            case SCOPING:
                StaxParserUtil.bypassElementBlock(xmlEventReader, element.getQName());
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }

    /**
     * Get the NameIDPolicy
     *
     * @param startElement
     *
     * @return
     */
    private NameIDPolicyType getNameIDPolicy(StartElement startElement) {
        NameIDPolicyType nameIDPolicy = new NameIDPolicyType();
        nameIDPolicy.setFormat(StaxParserUtil.getUriAttributeValue(startElement, SAMLProtocolQNames.ATTR_FORMAT));
        nameIDPolicy.setAllowCreate(StaxParserUtil.getBooleanAttributeValue(startElement, SAMLProtocolQNames.ATTR_ALLOW_CREATE));
        return nameIDPolicy;
    }
}