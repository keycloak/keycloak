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

import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.ParserNamespaceSupport;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.net.URI;

/**
 * Parse the SAML2 AuthnRequest
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 2, 2010
 */
public class SAMLAuthNRequestParser extends SAMLRequestAbstractParser implements ParserNamespaceSupport {

    /**
     * @see {@link ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        // Get the startelement
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.AUTHN_REQUEST.get());

        AuthnRequestType authnRequest = parseBaseAttributes(startElement);

        while (xmlEventReader.hasNext()) {
            // Let us peek at the next start element
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            super.parseCommonElements(startElement, xmlEventReader, authnRequest);

            String elementName = StaxParserUtil.getStartElementName(startElement);

            if (JBossSAMLConstants.NAMEID_POLICY.get().equals(elementName)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                authnRequest.setNameIDPolicy(getNameIDPolicy(startElement));
            } else if (JBossSAMLConstants.SUBJECT.get().equals(elementName)) {
                authnRequest.setSubject(getSubject(xmlEventReader));
            } else if (JBossSAMLConstants.CONDITIONS.get().equals(elementName)) {
                authnRequest.setConditions((ConditionsType) (new SAMLConditionsParser()).parse(xmlEventReader));
            } else if (JBossSAMLConstants.REQUESTED_AUTHN_CONTEXT.get().equals(elementName)) {
                authnRequest.setRequestedAuthnContext(getRequestedAuthnContextType(xmlEventReader));
            } else if (JBossSAMLConstants.ISSUER.get().equals(elementName)) {
                continue;
            } else if (JBossSAMLConstants.SIGNATURE.get().equals(elementName)) {
                continue;
            } else
                throw new RuntimeException(ErrorCodes.UNKNOWN_START_ELEMENT + elementName + "::location="
                        + startElement.getLocation());
        }
        return authnRequest;
    }

    /**
     * @see {@link ParserNamespaceSupport#supports(QName)}
     */
    public boolean supports(QName qname) {
        return JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(qname.getNamespaceURI());
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
    private AuthnRequestType parseBaseAttributes(StartElement startElement) throws ParsingException {
        super.parseRequiredAttributes(startElement);
        AuthnRequestType authnRequest = new AuthnRequestType(id, issueInstant);
        // Let us get the attributes
        super.parseBaseAttributes(startElement, authnRequest);

        Attribute assertionConsumerServiceURL = startElement.getAttributeByName(new QName(
                JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_URL.get()));
        if (assertionConsumerServiceURL != null) {
            String uri = StaxParserUtil.getAttributeValue(assertionConsumerServiceURL);
            authnRequest.setAssertionConsumerServiceURL(URI.create(uri));
        }

        Attribute assertionConsumerServiceIndex = startElement.getAttributeByName(new QName(
                JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_INDEX.get()));
        if (assertionConsumerServiceIndex != null)
            authnRequest.setAssertionConsumerServiceIndex(Integer.parseInt(StaxParserUtil
                    .getAttributeValue(assertionConsumerServiceIndex)));

        Attribute protocolBinding = startElement.getAttributeByName(new QName(JBossSAMLConstants.PROTOCOL_BINDING.get()));
        if (protocolBinding != null)
            authnRequest.setProtocolBinding(URI.create(StaxParserUtil.getAttributeValue(protocolBinding)));

        Attribute providerName = startElement.getAttributeByName(new QName(JBossSAMLConstants.PROVIDER_NAME.get()));
        if (providerName != null)
            authnRequest.setProviderName(StaxParserUtil.getAttributeValue(providerName));

        Attribute forceAuthn = startElement.getAttributeByName(new QName(JBossSAMLConstants.FORCE_AUTHN.get()));
        if (forceAuthn != null) {
            authnRequest.setForceAuthn(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(forceAuthn)));
        }

        Attribute isPassive = startElement.getAttributeByName(new QName(JBossSAMLConstants.IS_PASSIVE.get()));
        if (isPassive != null) {
            authnRequest.setIsPassive(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(isPassive)));
        }

        Attribute attributeConsumingServiceIndex = startElement.getAttributeByName(new QName(
                JBossSAMLConstants.ATTRIBUTE_CONSUMING_SERVICE_INDEX.get()));
        if (attributeConsumingServiceIndex != null)
            authnRequest.setAttributeConsumingServiceIndex(Integer.parseInt(StaxParserUtil
                    .getAttributeValue(attributeConsumingServiceIndex)));

        return authnRequest;
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
        Attribute format = startElement.getAttributeByName(new QName(JBossSAMLConstants.FORMAT.get()));
        if (format != null)
            nameIDPolicy.setFormat(URI.create(StaxParserUtil.getAttributeValue(format)));

        Attribute allowCreate = startElement.getAttributeByName(new QName(JBossSAMLConstants.ALLOW_CREATE.get()));
        if (allowCreate != null)
            nameIDPolicy.setAllowCreate(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(allowCreate)));

        return nameIDPolicy;
    }

    private RequestedAuthnContextType getRequestedAuthnContextType(XMLEventReader xmlEventReader) throws ParsingException {
        RequestedAuthnContextType ract = new RequestedAuthnContextType();
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.REQUESTED_AUTHN_CONTEXT.get());

        Attribute comparison = startElement.getAttributeByName(new QName(JBossSAMLConstants.COMPARISON.get()));

        if (comparison != null) {
            ract.setComparison(AuthnContextComparisonType.fromValue(comparison.getValue()));
        }

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);

            if (xmlEvent instanceof EndElement) {
                EndElement nextEndElement = (EndElement) xmlEvent;
                if (StaxParserUtil.matches(nextEndElement, JBossSAMLConstants.REQUESTED_AUTHN_CONTEXT.get())) {
                    nextEndElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw new RuntimeException(ErrorCodes.UNKNOWN_END_ELEMENT
                            + StaxParserUtil.getEndElementName(nextEndElement));
            }

            String tag = null;

            if (xmlEvent instanceof StartElement) {
                StartElement peekedElement = (StartElement) xmlEvent;
                tag = StaxParserUtil.getStartElementName(peekedElement);
            }

            startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
            String elName = StaxParserUtil.getStartElementName(startElement);

            if (elName.equals(JBossSAMLConstants.AUTHN_CONTEXT_CLASS_REF.get())) {
                String value = StaxParserUtil.getElementText(xmlEventReader);
                ract.addAuthnContextClassRef(value);
            } else
                throw new RuntimeException(ErrorCodes.UNKNOWN_TAG + elName);
        }

        return ract;
    }
}