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
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLEntitiesDescriptorParser;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLEntityDescriptorParser;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parse SAML payload
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 12, 2010
 */
public class SAMLParser extends AbstractParser {

    /**
     * @see {@link org.keycloak.saml.common.parsers.ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);

            if (xmlEvent instanceof StartElement) {
                StartElement startElement = (StartElement) xmlEvent;
                QName startElementName = startElement.getName();
                String nsURI = startElementName.getNamespaceURI();

                String localPart = startElementName.getLocalPart();

                String elementName = StaxParserUtil.getStartElementName(startElement);

                if (elementName.equalsIgnoreCase(JBossSAMLConstants.ASSERTION.get())
                        || elementName.equals(JBossSAMLConstants.ENCRYPTED_ASSERTION.get())) {
                    if (nsURI.equals(SAML11Constants.ASSERTION_11_NSURI)) {
                        SAML11AssertionParser saml11AssertionParser = new SAML11AssertionParser();
                        return saml11AssertionParser.parse(xmlEventReader);
                    }
                    SAMLAssertionParser assertionParser = new SAMLAssertionParser();
                    return assertionParser.parse(xmlEventReader);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.AUTHN_REQUEST.get().equals(startElementName.getLocalPart())) {
                    SAMLAuthNRequestParser authNRequestParser = new SAMLAuthNRequestParser();
                    return authNRequestParser.parse(xmlEventReader);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.LOGOUT_REQUEST.get().equals(startElementName.getLocalPart())) {
                    SAMLSloRequestParser sloParser = new SAMLSloRequestParser();
                    return sloParser.parse(xmlEventReader);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.LOGOUT_RESPONSE.get().equals(startElementName.getLocalPart())) {
                    SAMLSloResponseParser sloParser = new SAMLSloResponseParser();
                    return sloParser.parse(xmlEventReader);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.RESPONSE.get().equals(startElementName.getLocalPart())) {
                    SAMLResponseParser responseParser = new SAMLResponseParser();
                    return responseParser.parse(xmlEventReader);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.REQUEST_ABSTRACT.get().equals(startElementName.getLocalPart())) {
                    String xsiTypeValue = StaxParserUtil.getXSITypeValue(startElement);
                    throw new RuntimeException(ErrorCodes.UNKNOWN_XSI + xsiTypeValue);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.ARTIFACT_RESOLVE.get().equals(startElementName.getLocalPart())) {
                    SAMLArtifactResolveParser artifactResolverParser = new SAMLArtifactResolveParser();
                    return artifactResolverParser.parse(xmlEventReader);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.ARTIFACT_RESPONSE.get().equals(startElementName.getLocalPart())) {
                    SAMLArtifactResponseParser responseParser = new SAMLArtifactResponseParser();
                    return responseParser.parse(xmlEventReader);
                } else if (JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(nsURI)
                        && JBossSAMLConstants.ATTRIBUTE_QUERY.get().equals(startElementName.getLocalPart())) {
                    SAMLAttributeQueryParser responseParser = new SAMLAttributeQueryParser();
                    return responseParser.parse(xmlEventReader);
                } else if (JBossSAMLConstants.ENTITY_DESCRIPTOR.get().equals(localPart)) {
                    SAMLEntityDescriptorParser entityDescriptorParser = new SAMLEntityDescriptorParser();
                    return entityDescriptorParser.parse(xmlEventReader);
                } else if (JBossSAMLConstants.ENTITIES_DESCRIPTOR.get().equals(localPart)) {
                    SAMLEntitiesDescriptorParser entityDescriptorParser = new SAMLEntitiesDescriptorParser();
                    return entityDescriptorParser.parse(xmlEventReader);
                } else if (SAML11Constants.PROTOCOL_11_NSURI.equals(nsURI)
                        && JBossSAMLConstants.RESPONSE.get().equals(startElementName.getLocalPart())) {
                    SAML11ResponseParser responseParser = new SAML11ResponseParser();
                    return responseParser.parse(xmlEventReader);
                } else if (SAML11Constants.PROTOCOL_11_NSURI.equals(nsURI)
                        && SAML11Constants.REQUEST.equals(startElementName.getLocalPart())) {
                    SAML11RequestParser reqParser = new SAML11RequestParser();
                    return reqParser.parse(xmlEventReader);
                } else
                    throw new RuntimeException(ErrorCodes.UNKNOWN_START_ELEMENT + elementName + "::location="
                            + startElement.getLocation());
            } else {
                StaxParserUtil.getNextEvent(xmlEventReader);
            }
        }
        throw new RuntimeException(ErrorCodes.FAILED_PARSING + "SAML Parsing has failed");
    }

    /**
     * @see {@link org.keycloak.saml.common.parsers.ParserNamespaceSupport#supports(QName)}
     */
    public boolean supports(QName qname) {
        return JBossSAMLURIConstants.ASSERTION_NSURI.get().equals(qname.getNamespaceURI());
    }
}