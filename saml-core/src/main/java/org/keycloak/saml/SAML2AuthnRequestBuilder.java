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

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.w3c.dom.Document;

/**
 * @author pedroigor
 */
public class SAML2AuthnRequestBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2AuthnRequestBuilder> {

    private final AuthnRequestType authnRequestType;
    protected String destination;
    protected NameIDType issuer;
    protected final List<NodeGenerator> extensions = new LinkedList<>();

    public SAML2AuthnRequestBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2AuthnRequestBuilder issuer(NameIDType issuer) {
        this.issuer = issuer;
        return this;
    }

    public SAML2AuthnRequestBuilder issuer(String issuer) {
        return issuer(SAML2NameIDBuilder.value(issuer).build());
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

    public SAML2AuthnRequestBuilder attributeConsumingServiceIndex(Integer attributeConsumingServiceIndex) {
        this.authnRequestType.setAttributeConsumingServiceIndex(attributeConsumingServiceIndex);
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

    public SAML2AuthnRequestBuilder nameIdPolicy(SAML2NameIDPolicyBuilder nameIDPolicyBuilder) {
        this.authnRequestType.setNameIDPolicy(nameIDPolicyBuilder.build());
        return this;
    }

    public SAML2AuthnRequestBuilder protocolBinding(String protocolBinding) {
        this.authnRequestType.setProtocolBinding(URI.create(protocolBinding));
        return this;
    }

    public SAML2AuthnRequestBuilder subject(String subject) {
        String sanitizedSubject = subject != null ? subject.trim() : null;
        if (sanitizedSubject != null && !sanitizedSubject.isEmpty()) {
            this.authnRequestType.setSubject(createSubject(sanitizedSubject));
        }
        return this;
    }

    private SubjectType createSubject(String value) {
        NameIDType nameId = new NameIDType();
        nameId.setValue(value);
        nameId.setFormat(this.authnRequestType.getNameIDPolicy() != null ? this.authnRequestType.getNameIDPolicy().getFormat() : null);
        SubjectType subject = new SubjectType();
        SubjectType.STSubType subType = new SubjectType.STSubType();
        subType.addBaseID(nameId);
        subject.setSubType(subType);
        return subject;
    }

    public SAML2AuthnRequestBuilder requestedAuthnContext(SAML2RequestedAuthnContextBuilder requestedAuthnContextBuilder) {
        RequestedAuthnContextType requestedAuthnContext = requestedAuthnContextBuilder.build();

        // Only emit the RequestedAuthnContext element if at least a ClassRef or a DeclRef is present
        if (!requestedAuthnContext.getAuthnContextClassRef().isEmpty() ||
            !requestedAuthnContext.getAuthnContextDeclRef().isEmpty())
            this.authnRequestType.setRequestedAuthnContext(requestedAuthnContext);

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

        res.setIssuer(issuer);
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