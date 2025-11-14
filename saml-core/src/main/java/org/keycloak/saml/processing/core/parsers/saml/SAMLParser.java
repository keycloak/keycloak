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

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.common.parsers.StaxParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAuthnStatementParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLEncryptedAssertionParser;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLEntitiesDescriptorParser;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLEntityDescriptorParser;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLArtifactResolveParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLArtifactResponseParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLAttributeQueryParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLAuthNRequestParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLProtocolQNames;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLResponseParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLSloRequestParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLSloResponseParser;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;

/**
 * Parse SAML payload
 *
 * @since Oct 12, 2010
 */
public class SAMLParser extends AbstractParser {

    private static final SAML11ResponseParser SAML_11_RESPONSE_PARSER = new SAML11ResponseParser();
    private static final SAML11RequestParser SAML_11_REQUEST_PARSER = new SAML11RequestParser();

    private static final QName SAML_11_ASSERTION = new QName(SAML11Constants.ASSERTION_11_NSURI, JBossSAMLConstants.ASSERTION.get());
    private static final QName SAML_11_ENCRYPTED_ASSERTION = new QName(SAML11Constants.ASSERTION_11_NSURI, JBossSAMLConstants.ENCRYPTED_ASSERTION.get());
    private static final QName SAML_11_RESPONSE = new QName(SAML11Constants.ASSERTION_11_NSURI, JBossSAMLConstants.RESPONSE__PROTOCOL.get());
    private static final QName SAML_11_REQUEST = new QName(SAML11Constants.ASSERTION_11_NSURI, JBossSAMLConstants.REQUEST.get());

    // Since we have to support JDK 7, no lambdas are available
    private interface ParserFactory {
        StaxParser create();
    }
    private static final Map<QName, ParserFactory> PARSERS = new HashMap<>();

    static {
        PARSERS.put(SAML_11_ASSERTION, new ParserFactory() { @Override public StaxParser create() { return new SAML11AssertionParser(); }});
        PARSERS.put(SAML_11_ENCRYPTED_ASSERTION, new ParserFactory() { @Override public StaxParser create() { return new SAML11AssertionParser(); }});
        PARSERS.put(SAML_11_RESPONSE, new ParserFactory() { @Override public StaxParser create() { return SAML_11_RESPONSE_PARSER; }});
        PARSERS.put(SAML_11_REQUEST, new ParserFactory() { @Override public StaxParser create() { return SAML_11_REQUEST_PARSER; }});

        PARSERS.put(SAMLProtocolQNames.AUTHN_REQUEST.getQName(),      new ParserFactory() { @Override public StaxParser create() { return SAMLAuthNRequestParser.getInstance(); }});
        PARSERS.put(SAMLProtocolQNames.RESPONSE.getQName(),           new ParserFactory() { @Override public StaxParser create() { return SAMLResponseParser.getInstance(); }});
        PARSERS.put(SAMLProtocolQNames.LOGOUT_REQUEST.getQName(),     new ParserFactory() { @Override public StaxParser create() { return SAMLSloRequestParser.getInstance(); }});
        PARSERS.put(SAMLProtocolQNames.LOGOUT_RESPONSE.getQName(),    new ParserFactory() { @Override public StaxParser create() { return SAMLSloResponseParser.getInstance(); }});

        PARSERS.put(SAMLProtocolQNames.ARTIFACT_RESOLVE.getQName(),   new ParserFactory() { @Override public StaxParser create() { return SAMLArtifactResolveParser.getInstance(); }});
        PARSERS.put(SAMLProtocolQNames.ARTIFACT_RESPONSE.getQName(),  new ParserFactory() { @Override public StaxParser create() { return SAMLArtifactResponseParser.getInstance(); }});

        PARSERS.put(SAMLProtocolQNames.ASSERTION.getQName(),          new ParserFactory() { @Override public StaxParser create() { return SAMLAssertionParser.getInstance(); }});
        PARSERS.put(SAMLProtocolQNames.ENCRYPTED_ASSERTION.getQName(),new ParserFactory() { @Override public StaxParser create() { return SAMLEncryptedAssertionParser.getInstance(); }});

        PARSERS.put(SAMLAssertionQNames.AUTHN_STATEMENT.getQName(),   new ParserFactory() { @Override public StaxParser create() { return SAMLAuthnStatementParser.getInstance(); }});

        PARSERS.put(SAMLMetadataQNames.ENTITY_DESCRIPTOR.getQName(),  new ParserFactory() { @Override public StaxParser create() { return SAMLEntityDescriptorParser.getInstance(); }});
        PARSERS.put(SAMLMetadataQNames.ENTITIES_DESCRIPTOR.getQName(),new ParserFactory() { @Override public StaxParser create() { return SAMLEntitiesDescriptorParser.getInstance(); }});

        PARSERS.put(SAMLProtocolQNames.ATTRIBUTE_QUERY.getQName(),    new ParserFactory() { @Override public StaxParser create() { return SAMLAttributeQueryParser.getInstance(); }});
    }

    private static final SAMLParser INSTANCE = new SAMLParser();

    public static SAMLParser getInstance() {
        return INSTANCE;
    }

    protected SAMLParser() {
    }

    /**
     * @see {@link org.keycloak.saml.common.parsers.ParserNamespaceSupport#parse(XMLEventReader)}
     */
    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);

            if (xmlEvent instanceof StartElement) {
                StartElement startElement = (StartElement) xmlEvent;
                final QName name = startElement.getName();

                ParserFactory pf = PARSERS.get(name);
                if (pf == null) {
                    throw logger.parserException(new RuntimeException(ErrorCodes.UNKNOWN_START_ELEMENT + name + "::location="
                            + startElement.getLocation()));
                }

                return pf.create().parse(xmlEventReader);
            }

            StaxParserUtil.getNextEvent(xmlEventReader);
        }

        throw new RuntimeException(ErrorCodes.FAILED_PARSING + "SAML Parsing has failed");
    }
}