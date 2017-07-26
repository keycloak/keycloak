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
package org.keycloak.testsuite.util;

import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClient.DoNotFollowRedirectStep;
import org.keycloak.testsuite.util.SamlClient.ResultExtractor;
import org.keycloak.testsuite.util.SamlClient.Step;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.testsuite.util.saml.CreateAuthnRequestStepBuilder;
import org.keycloak.testsuite.util.saml.CreateLogoutRequestStepBuilder;
import org.keycloak.testsuite.util.saml.IdPInitiatedLoginBuilder;
import org.keycloak.testsuite.util.saml.LoginBuilder;
import org.keycloak.testsuite.util.saml.ModifySamlResponseStepBuilder;
import org.keycloak.testsuite.util.saml.RequiredConsentBuilder;
import org.w3c.dom.Document;

/**
 *
 * @author hmlnarik
 */
public class SamlClientBuilder {

    private final List<Step> steps = new LinkedList<>();

    public SamlClient execute(Consumer<CloseableHttpResponse> resultConsumer) {
        final SamlClient samlClient = new SamlClient();
        samlClient.executeAndTransform(r -> {
            resultConsumer.accept(r);
            return null;
        }, steps);
        return samlClient;
    }

    public <T> T executeAndTransform(ResultExtractor<T> resultTransformer) {
        return new SamlClient().executeAndTransform(resultTransformer, steps);
    }

    public List<Step> getSteps() {
        return steps;
    }

    public <T extends Step> T addStep(T step) {
        steps.add(step);
        return step;
    }

    public SamlClientBuilder doNotFollowRedirects() {
        this.steps.add(new DoNotFollowRedirectStep());
        return this;
    }

    public SamlClientBuilder clearCookies() {
        this.steps.add((client, currentURI, currentResponse, context) -> {
            context.getCookieStore().clear();
            return null;
        });
        return this;
    }

    /** Creates fresh and issues an AuthnRequest to the SAML endpoint */
    public CreateAuthnRequestStepBuilder authnRequest(URI authServerSamlUrl, String issuer, String assertionConsumerURL, Binding requestBinding) {
        return addStep(new CreateAuthnRequestStepBuilder(authServerSamlUrl, issuer, assertionConsumerURL, requestBinding, this));
    }

    /** Issues the given AuthnRequest to the SAML endpoint */
    public CreateAuthnRequestStepBuilder authnRequest(URI authServerSamlUrl, Document authnRequestDocument, Binding requestBinding) {
        return addStep(new CreateAuthnRequestStepBuilder(authServerSamlUrl, authnRequestDocument, requestBinding, this));
    }

    /** Issues the given AuthnRequest to the SAML endpoint */
    public CreateLogoutRequestStepBuilder logoutRequest(URI authServerSamlUrl, String issuer, Binding requestBinding) {
        return addStep(new CreateLogoutRequestStepBuilder(authServerSamlUrl, issuer, requestBinding, this));
    }

    /** Handles login page */
    public LoginBuilder login() {
        return addStep(new LoginBuilder(this));
    }

    /** Starts IdP-initiated flow for the given client */
    public IdPInitiatedLoginBuilder idpInitiatedLogin(URI authServerSamlUrl, String clientId) {
        return addStep(new IdPInitiatedLoginBuilder(authServerSamlUrl, clientId, this));
    }

    /** Handles "Requires consent" page */
    public RequiredConsentBuilder consentRequired() {
        return addStep(new RequiredConsentBuilder(this));
    }

    /** Returns SAML request or response as replied from server. Note that the redirects are disabled for this to work. */
    public SAMLDocumentHolder getSamlResponse(Binding responseBinding) {
        return
          doNotFollowRedirects()
          .executeAndTransform(responseBinding::extractResponse);
    }

    /** Returns SAML request or response as replied from server. Note that the redirects are disabled for this to work. */
    public ModifySamlResponseStepBuilder processSamlResponse(Binding responseBinding) {
        return
          doNotFollowRedirects()
          .addStep(new ModifySamlResponseStepBuilder(responseBinding, this));
    }

    public SamlClientBuilder navigateTo(String httpGetUri) {
        steps.add((client, currentURI, currentResponse, context) -> {
            return new HttpGet(httpGetUri);
        });
        return this;
    }

    public SamlClientBuilder navigateTo(URI httpGetUri) {
        steps.add((client, currentURI, currentResponse, context) -> {
            return new HttpGet(httpGetUri);
        });
        return this;
    }

}
