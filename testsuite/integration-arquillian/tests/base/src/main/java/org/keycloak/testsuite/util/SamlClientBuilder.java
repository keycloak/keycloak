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

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.page.AbstractPage;
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
import org.keycloak.testsuite.util.saml.CreateArtifactMessageStepBuilder;
import org.keycloak.testsuite.util.saml.CreateAuthnRequestStepBuilder;
import org.keycloak.testsuite.util.saml.CreateLogoutRequestStepBuilder;
import org.keycloak.testsuite.util.saml.HandleArtifactStepBuilder;
import org.keycloak.testsuite.util.saml.IdPInitiatedLoginBuilder;
import org.keycloak.testsuite.util.saml.LoginBuilder;
import org.keycloak.testsuite.util.saml.UpdateProfileBuilder;
import org.keycloak.testsuite.util.saml.ModifySamlResponseStepBuilder;
import org.keycloak.testsuite.util.saml.RequiredConsentBuilder;
import java.util.function.Function;
import javax.ws.rs.core.Response.Status;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.w3c.dom.Document;
import static org.hamcrest.Matchers.notNullValue;
import static org.keycloak.testsuite.util.saml.SamlDocumentStepBuilder.saml2Object2String;

/**
 *
 * @author hmlnarik
 */
public class SamlClientBuilder {

    private final List<Step> steps = new LinkedList<>();

    /**
     * Execute the current steps without any work on the final response.
     * @return Client that executed the steps
     */
    public SamlClient execute() {
        return execute(resp -> {});
    }

    /**
     * Execute the current steps and pass the final response to the {@code resultConsumer} for processing.
     * @param resultConsumer This function is given the final response
     * @return Client that executed the steps
     */
    public SamlClient execute(Consumer<CloseableHttpResponse> resultConsumer) {
        final SamlClient samlClient = createSamlClient();
        samlClient.executeAndTransform(r -> {
            resultConsumer.accept(r);
            return null;
        }, steps);
        return samlClient;
    }
    
    protected SamlClient createSamlClient() {
        return new SamlClient();
    }

    /**
     * Execute the current steps and pass the final response to the {@code resultTransformer} for processing.
     * @param resultTransformer This function is given the final response and processes it into some value
     * @return Value returned by {@code resultTransformer}
     */
    public <T> T executeAndTransform(ResultExtractor<T> resultTransformer) {
        return createSamlClient().executeAndTransform(resultTransformer, steps);
    }

    public List<Step> getSteps() {
        return steps;
    }

    public <T extends Step> T addStepBuilder(T step) {
        steps.add(step);
        return step;
    }

    /**
     * Adds a single generic step
     * @param step
     * @return This builder
     */
    public SamlClientBuilder addStep(Step step) {
        steps.add(step);
        return this;
    }

    /**
     * Adds a single generic step
     * @return This builder
     */
    public SamlClientBuilder addStep(Runnable stepWithNoParameters) {
        addStep((client, currentURI, currentResponse, context) -> {
            stepWithNoParameters.run();
            return null;
        });
        return this;
    }

    public <T> T andThen(Function<SamlClientBuilder, T> next) {
        return next.apply(this);
    }

    public SamlClientBuilder assertResponse(Matcher<? super CloseableHttpResponse> matcher) {
        steps.add((client, currentURI, currentResponse, context) -> {
            Assert.assertThat(currentResponse, matcher);
            return null;
        });
        return this;
    }

    public SamlClientBuilder assertResponse(Consumer<? super CloseableHttpResponse> consumer) {
        steps.add((client, currentURI, currentResponse, context) -> {
            consumer.accept(currentResponse);
            return null;
        });
        return this;
    }

    /**
     * When executing the {@link HttpUriRequest} obtained from the previous step,
     * do not to follow HTTP redirects but pass the first response immediately
     * to the following step.
     * @return This builder
     */
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
        return addStepBuilder(new CreateAuthnRequestStepBuilder(authServerSamlUrl, issuer, assertionConsumerURL, requestBinding, this));
    }

    /** Issues the given AuthnRequest to the SAML endpoint */
    public CreateAuthnRequestStepBuilder authnRequest(URI authServerSamlUrl, Document authnRequestDocument, Binding requestBinding) {
        return addStepBuilder(new CreateAuthnRequestStepBuilder(authServerSamlUrl, authnRequestDocument, requestBinding, this));
    }

    /** Issues the given AuthnRequest to the SAML endpoint */
    public CreateLogoutRequestStepBuilder logoutRequest(URI authServerSamlUrl, String issuer, Binding requestBinding) {
        return addStepBuilder(new CreateLogoutRequestStepBuilder(authServerSamlUrl, issuer, requestBinding, this));
    }

    /** Issues the given SAML document to the SAML endpoint */
    public ModifySamlResponseStepBuilder submitSamlDocument(URI authServerSamlUrl, String samlDocument, Binding binding) {
        return addStepBuilder(new ModifySamlResponseStepBuilder(binding, this)
          .targetUri(authServerSamlUrl)
          .documentSupplier(() -> samlDocument)
        );
    }

    /** Issues the given SAML document to the SAML endpoint */
    public ModifySamlResponseStepBuilder submitSamlDocument(URI authServerSamlUrl, SAML2Object samlObject, Binding binding) {
        return addStepBuilder(new ModifySamlResponseStepBuilder(binding, this)
          .targetUri(authServerSamlUrl)
          .documentSupplier(() -> saml2Object2String(samlObject))
        );
    }

    /** Handles login page */
    public LoginBuilder login() {
        return addStepBuilder(new LoginBuilder(this));
    }

    /** Handles update profile page after login */
    public UpdateProfileBuilder updateProfile() {
        return addStepBuilder(new UpdateProfileBuilder(this));
    }

    /** Starts IdP-initiated flow for the given client */
    public IdPInitiatedLoginBuilder idpInitiatedLogin(URI authServerSamlUrl, String clientId) {
        return addStepBuilder(new IdPInitiatedLoginBuilder(authServerSamlUrl, clientId, this));
    }

    /** Handles "Requires consent" page */
    public RequiredConsentBuilder consentRequired() {
        return addStepBuilder(new RequiredConsentBuilder(this));
    }

    /** Returns SAML request or response as replied from server. Note that the redirects are disabled for this to work. */
    public SAMLDocumentHolder getSamlResponse(Binding responseBinding) {
        return
          doNotFollowRedirects()
          .executeAndTransform(responseBinding::extractResponse);
    }

    /** Returns RelayState from Saml response. Note that the redirects are disabled for this to work. */
    public String getSamlRelayState(Binding responseBinding) {
        return doNotFollowRedirects()
                .executeAndTransform(responseBinding::extractRelayState);
    }

    /** Provide possibility to consume RelayState from saml response. Note that the redirects are disabled for this to work. */
    public SamlClientBuilder assertSamlRelayState(Binding responseBinding, Consumer<String> relayStateConsumer) {
        if (responseBinding.equals(Binding.REDIRECT)) doNotFollowRedirects();
        return addStep((client, currentURI, currentResponse, context) -> {
            relayStateConsumer.accept(responseBinding.extractRelayState(currentResponse));
            return null;
        });
    }

    /** Returns SAML request or response as replied from server. Note that the redirects are disabled for this to work. */
    public ModifySamlResponseStepBuilder processSamlResponse(Binding responseBinding) {
        return
          doNotFollowRedirects()
          .addStepBuilder(new ModifySamlResponseStepBuilder(responseBinding, this));
    }

    public SamlClientBuilder navigateTo(String httpGetUri) {
        steps.add((client, currentURI, currentResponse, context) -> new HttpGet(httpGetUri));
        return this;
    }

    public SamlClientBuilder navigateTo(AbstractPage page) {
        return navigateTo(page.buildUri());
    }

    public SamlClientBuilder navigateTo(URI httpGetUri) {
        steps.add((client, currentURI, currentResponse, context) -> new HttpGet(httpGetUri));
        return this;
    }

    public SamlClientBuilder followOneRedirect() {
        return
          doNotFollowRedirects()
          .addStep((client, currentURI, currentResponse, context) -> {
            Assert.assertThat(currentResponse, Matchers.statusCodeIsHC(Status.FOUND));
            Assert.assertThat("Location header not found", currentResponse.getFirstHeader("Location"), notNullValue());
            return new HttpGet(currentResponse.getFirstHeader("Location").getValue());
          });
    }

    public HandleArtifactStepBuilder handleArtifact(URI authServerSamlUrl, String issuer) {
        return doNotFollowRedirects()
                .addStepBuilder(new HandleArtifactStepBuilder(authServerSamlUrl, issuer, this));
    }

    public HandleArtifactStepBuilder handleArtifact(HandleArtifactStepBuilder handleArtifactStepBuilder) {
        return doNotFollowRedirects().addStepBuilder(handleArtifactStepBuilder);
    }

    public CreateArtifactMessageStepBuilder artifactMessage(URI authServerSamlUrl, String issuer, Binding requestBinding) {
        return addStepBuilder(new CreateArtifactMessageStepBuilder(authServerSamlUrl, issuer, requestBinding,this));
    }

    public CreateArtifactMessageStepBuilder artifactMessage(CreateArtifactMessageStepBuilder camb) {
        return addStepBuilder(camb);
    }
}
