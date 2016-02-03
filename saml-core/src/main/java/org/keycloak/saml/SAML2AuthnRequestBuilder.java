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

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Document;

/**
 * @author pedroigor
 */
public class SAML2AuthnRequestBuilder {

    private final AuthnRequestType authnRequestType;
    protected String destination;
    protected String issuer;

    public SAML2AuthnRequestBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2AuthnRequestBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public SAML2AuthnRequestBuilder() {
        try {
            this.authnRequestType = new AuthnRequestType(IDGenerator.create("ID_"), XMLTimeUtil.getIssueInstant());
        } catch (ConfigurationException e) {
            throw new RuntimeException("Could not create SAML AuthnRequest builder.", e);
        }
    }

    public SAML2AuthnRequestBuilder assertionConsumerUrl(String assertionConsumerUrl) {
        this.authnRequestType.setAssertionConsumerServiceURL(URI.create(assertionConsumerUrl));
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
            AuthnRequestType authnRequestType = this.authnRequestType;

            NameIDType nameIDType = new NameIDType();

            nameIDType.setValue(this.issuer);

            authnRequestType.setIssuer(nameIDType);

            authnRequestType.setDestination(URI.create(this.destination));

            return new SAML2Request().convert(authnRequestType);
        } catch (Exception e) {
            throw new RuntimeException("Could not convert " + authnRequestType + " to a document.", e);
        }
    }
}