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
package org.keycloak.saml;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Document;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;

/**
 * @author pedroigor
 */
public class SAML2AuthnRequestBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2AuthnRequestBuilder> {

    private final AuthnRequestType authnRequestType;
    protected String destination;
    protected String issuer;
    protected final List<NodeGenerator> extensions = new LinkedList<>();

    public SAML2AuthnRequestBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2AuthnRequestBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    @Override
    public SAML2AuthnRequestBuilder addExtension(NodeGenerator extension) {
        this.extensions.add(extension);
        return this;
    }

    public SAML2AuthnRequestBuilder() {
        this.authnRequestType = new AuthnRequestType(IDGenerator.create("ID_"), XMLTimeUtil.getIssueInstant());
    }

    public SAML2AuthnRequestBuilder assertionConsumerUrl(String assertionConsumerUrl) {
        this.authnRequestType.setAssertionConsumerServiceURL(URI.create(assertionConsumerUrl));
        return this;
    }

    public SAML2AuthnRequestBuilder assertionConsumerUrl(URI assertionConsumerUrl) {
        this.authnRequestType.setAssertionConsumerServiceURL(assertionConsumerUrl);
        return this;
    }

    public SAML2AuthnRequestBuilder forceAuthn(boolean forceAuthn) {
        this.authnRequestType.setForceAuthn(forceAuthn);
        return this;
    }

    public SAML2AuthnRequestBuilder isPassive(boolean isPassive) {
        this.authnRequestType.setIsPassive(isPassive);
        return this;
    }

    public SAML2AuthnRequestBuilder nameIdPolicy(SAML2NameIDPolicyBuilder nameIDPolicy) {
        this.authnRequestType.setNameIDPolicy(nameIDPolicy.build());
        return this;
    }

    public SAML2AuthnRequestBuilder protocolBinding(String protocolBinding) {
        this.authnRequestType.setProtocolBinding(URI.create(protocolBinding));
        return this;
    }

    public Document toDocument() {
        try {
            AuthnRequestType authnRequestType = createAuthnRequest();

            return new SAML2Request().convert(authnRequestType);
        } catch (Exception e) {
            throw new RuntimeException("Could not convert " + authnRequestType + " to a document.", e);
        }
    }

    public AuthnRequestType createAuthnRequest() {
        AuthnRequestType res = this.authnRequestType;
        NameIDType nameIDType = new NameIDType();
        nameIDType.setValue(this.issuer);

        res.setIssuer(nameIDType);

        res.setDestination(URI.create(this.destination));

        if (! this.extensions.isEmpty()) {
            ExtensionsType extensionsType = new ExtensionsType();
            for (NodeGenerator extension : this.extensions) {
                extensionsType.addExtension(extension);
            }
            res.setExtensions(extensionsType);
        }

        return res;
    }
}