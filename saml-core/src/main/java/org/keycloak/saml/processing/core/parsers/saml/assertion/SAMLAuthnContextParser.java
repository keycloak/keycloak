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
package org.keycloak.saml.processing.core.parsers.saml.assertion;

import java.net.URI;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.assertion.AuthnContextClassRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextDeclRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextDeclType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import org.w3c.dom.Element;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class SAMLAuthnContextParser extends AbstractStaxSamlAssertionParser<AuthnContextType> {

    private static final SAMLAuthnContextParser INSTANCE = new SAMLAuthnContextParser();

    private SAMLAuthnContextParser() {
        super(SAMLAssertionQNames.AUTHN_CONTEXT);
    }

    public static SAMLAuthnContextParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AuthnContextType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new AuthnContextType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AuthnContextType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        String text;
        AuthnContextType.AuthnContextTypeSequence authnContextSequence;

        switch (element) {
            case AUTHN_CONTEXT_DECL:
                Element dom = StaxParserUtil.getDOMElement(xmlEventReader);
                AuthnContextDeclType authnContextDecl = new AuthnContextDeclType(dom);
                authnContextSequence = target.getSequence() != null ? target.getSequence() : new AuthnContextType.AuthnContextTypeSequence();
                authnContextSequence.setAuthnContextDecl(authnContextDecl);
                target.setSequence(authnContextSequence);
                break;

            case AUTHN_CONTEXT_DECL_REF:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                AuthnContextDeclRefType authnContextDeclRef = new AuthnContextDeclRefType(URI.create(text));
                target.addURIType(authnContextDeclRef);
                break;

            case AUTHN_CONTEXT_CLASS_REF:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                AuthnContextClassRefType authnContextClassRef = new AuthnContextClassRefType(URI.create(text));

                authnContextSequence = target.getSequence() != null ? target.getSequence() : new AuthnContextType.AuthnContextTypeSequence();
                authnContextSequence.setClassRef(authnContextClassRef);

                target.setSequence(authnContextSequence);
                break;

            case AUTHENTICATING_AUTHORITY:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.addAuthenticatingAuthority(URI.create(text));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}