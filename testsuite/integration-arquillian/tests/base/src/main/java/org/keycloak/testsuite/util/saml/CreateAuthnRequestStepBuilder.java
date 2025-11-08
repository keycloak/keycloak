/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util.saml;

import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;


public class CreateAuthnRequestStepBuilder extends SamlDocumentStepBuilder<AuthnRequestType, CreateAuthnRequestStepBuilder> {

    private final String issuer;
    private final URI authServerSamlUrl;
    private final Binding requestBinding;
    private final String assertionConsumerURL;
    private String signingPublicKeyPem;  // TODO: should not be needed
    private String signingPrivateKeyPem;
    private String signingCertificate;
    private URI protocolBinding;
    private String authorizationHeader;

    private final Document forceLoginRequestDocument;

    private Supplier<String> relayState;

    public CreateAuthnRequestStepBuilder(URI authServerSamlUrl, String issuer, String assertionConsumerURL, Binding requestBinding, SamlClientBuilder clientBuilder) {
        super(clientBuilder);
        this.issuer = issuer;
        this.authServerSamlUrl = authServerSamlUrl;
        this.requestBinding = requestBinding;
        this.assertionConsumerURL = assertionConsumerURL;

        this.forceLoginRequestDocument = null;
    }

    public CreateAuthnRequestStepBuilder(URI authServerSamlUrl, Document loginRequestDocument, Binding requestBinding, SamlClientBuilder clientBuilder) {
        super(clientBuilder);
        this.forceLoginRequestDocument = loginRequestDocument;

        this.authServerSamlUrl = authServerSamlUrl;
        this.requestBinding = requestBinding;

        this.issuer = null;
        this.assertionConsumerURL = null;
    }

    public CreateAuthnRequestStepBuilder relayState(Supplier<String> relayState) {
        this.relayState = relayState;
        return this;
    }

    public CreateAuthnRequestStepBuilder relayState(String relayState) {
        this.relayState = () -> relayState;
        return this;
    }

    public CreateAuthnRequestStepBuilder setProtocolBinding(URI protocolBinding) {
        this.protocolBinding = protocolBinding;
        return this;
    }

    public URI getProtocolBinding() {
        return protocolBinding;
    }

    public CreateAuthnRequestStepBuilder signWith(String signingPrivateKeyPem, String signingPublicKeyPem) {
        return signWith(signingPrivateKeyPem, signingPublicKeyPem, null);
    }

    public CreateAuthnRequestStepBuilder signWith(String signingPrivateKeyPem, String signingPublicKeyPem, String signingCertificate) {
        this.signingPrivateKeyPem = signingPrivateKeyPem;
        this.signingPublicKeyPem = signingPublicKeyPem;
        this.signingCertificate = signingCertificate;
        return this;
    }

    public CreateAuthnRequestStepBuilder basicAuthentication(UserRepresentation user) {
        String username = user.getUsername();
        String password = Users.getPasswordOf(user);
        String pair = username + ":" + password;
        this.authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(pair.getBytes());
        return this;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        Document doc = createLoginRequestDocument();

        String documentAsString = DocumentUtil.getDocumentAsString(doc);
        String transformed = getTransformer().transform(documentAsString);

        if (transformed == null) {
            return null;
        }

        Document samlDoc = DocumentUtil.getDocument(transformed);
        String relayState = this.relayState == null ? null : this.relayState.get();

        HttpUriRequest request = this.signingPrivateKeyPem == null
          ? requestBinding.createSamlUnsignedRequest(authServerSamlUrl, relayState, samlDoc)
          : requestBinding.createSamlSignedRequest(authServerSamlUrl, relayState, samlDoc, signingPrivateKeyPem, signingPublicKeyPem, signingCertificate);

        if (authorizationHeader != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        return request;
    }

    protected Document createLoginRequestDocument() {
        if (this.forceLoginRequestDocument != null) {
            return this.forceLoginRequestDocument;
        }

        try {
            SAML2Request samlReq = new SAML2Request();
            AuthnRequestType loginReq = samlReq.createAuthnRequestType(UUID.randomUUID().toString(), assertionConsumerURL, this.authServerSamlUrl.toString(), issuer, requestBinding.getBindingUri());
            if (protocolBinding != null) {
                loginReq.setProtocolBinding(protocolBinding);
            }
            return SAML2Request.convert(loginReq);
        } catch (ConfigurationException | ParsingException | ProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
