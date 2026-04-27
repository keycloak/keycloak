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
import java.util.function.Supplier;

import org.keycloak.dom.saml.v2.assertion.BaseIDAbstractType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author hmlnarik
 */
public class CreateLogoutRequestStepBuilder extends SamlDocumentStepBuilder<LogoutRequestType, CreateLogoutRequestStepBuilder> {

    private final URI logoutServerSamlUrl;
    private final String issuer;
    private final Binding requestBinding;

    private Supplier<String> sessionIndex = () -> null;
    private Supplier<NameIDType> nameId = () -> null;
    private Supplier<BaseIDAbstractType> baseId = () -> null;
    private Supplier<String> relayState = () -> null;
    private String signingPublicKeyPem;  // TODO: should not be needed
    private String signingPrivateKeyPem;
    private String signingCertificate;

    private boolean skipSignature;

    public CreateLogoutRequestStepBuilder(URI logoutServerSamlUrl, String issuer, Binding requestBinding, SamlClientBuilder clientBuilder) {
        super(clientBuilder);
        this.logoutServerSamlUrl = logoutServerSamlUrl;
        this.issuer = issuer;
        this.requestBinding = requestBinding;
    }

    public CreateLogoutRequestStepBuilder(URI logoutServerSamlUrl, String issuer, Binding requestBinding, SamlClientBuilder clientBuilder, boolean skipSignature) {
        super(clientBuilder);
        this.logoutServerSamlUrl = logoutServerSamlUrl;
        this.issuer = issuer;
        this.requestBinding = requestBinding;
        this.skipSignature = skipSignature;
    }

    public String sessionIndex() {
        return sessionIndex.get();
    }

    public CreateLogoutRequestStepBuilder sessionIndex(String sessionIndex) {
        this.sessionIndex = () -> sessionIndex;
        return this;
    }

    public CreateLogoutRequestStepBuilder sessionIndex(Supplier<String> sessionIndex) {
        this.sessionIndex = sessionIndex;
        return this;
    }

    public String relayState() {
        return relayState.get();
    }

    public CreateLogoutRequestStepBuilder relayState(String relayState) {
        this.relayState = () -> relayState;
        return this;
    }

    public CreateLogoutRequestStepBuilder relayState(Supplier<String> relayState) {
        this.relayState = relayState;
        return this;
    }

    public NameIDType nameId() {
        return nameId.get();
    }

    public BaseIDAbstractType baseId() {
        return baseId.get();
    }

    public CreateLogoutRequestStepBuilder nameId(NameIDType nameId) {
        this.nameId = () -> nameId;
        return this;
    }

    public CreateLogoutRequestStepBuilder nameId(Supplier<NameIDType> nameId) {
        this.nameId = nameId;
        return this;
    }

    public CreateLogoutRequestStepBuilder baseId(Supplier<BaseIDAbstractType> baseId) {
        this.baseId = baseId;
        return this;
    }

    public CreateLogoutRequestStepBuilder signWith(String signingPrivateKeyPem, String signingPublicKeyPem) {
        return signWith(signingPrivateKeyPem, signingPublicKeyPem, null);
    }

    public CreateLogoutRequestStepBuilder signWith(String signingPrivateKeyPem, String signingPublicKeyPem, String signingCertificate) {
        this.signingPrivateKeyPem = signingPrivateKeyPem;
        this.signingPublicKeyPem = signingPublicKeyPem;
        this.signingCertificate = signingCertificate;
        return this;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        SAML2LogoutRequestBuilder builder = new SAML2LogoutRequestBuilder()
          .destination(logoutServerSamlUrl == null ? null : logoutServerSamlUrl.toString())
          .issuer(issuer)
          .sessionIndex(sessionIndex())
          .nameId(nameId());

        String documentAsString = DocumentUtil.getDocumentAsString(builder.buildDocument());
        String transformed = getTransformer().transform(documentAsString);

        if (transformed == null) {
            return null;
        }

        return this.signingPrivateKeyPem == null || skipSignature
          ? requestBinding.createSamlUnsignedRequest(logoutServerSamlUrl, relayState(), DocumentUtil.getDocument(transformed))
          : requestBinding.createSamlSignedRequest(logoutServerSamlUrl, relayState(), DocumentUtil.getDocument(transformed), signingPrivateKeyPem, signingPublicKeyPem, signingCertificate);
    }

}
