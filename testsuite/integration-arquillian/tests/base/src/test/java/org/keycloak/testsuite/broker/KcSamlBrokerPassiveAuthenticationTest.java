/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import jakarta.ws.rs.core.UriBuilder;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.resources.RealmsResource;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * Tests forwarding of passive authentication requests (prompt=none) from OIDC clients to a backing SAML IDP.
 */
public final class KcSamlBrokerPassiveAuthenticationTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Before
    public void configureDefaults() {
        // Configure forwarding of passive authentication requests:
        configureAcceptsPromptNoneForwardingFromClient(true);
        // Disable profile update required action for the prompt=none propagation to work:
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        // Ensure the broker user is logged out from both realms to start with a clean state
        logoutFromBothRealms();
    }

    /**
     * OIDC prompt=none login should succeed when the user is already authenticated at the backing SAML IDP.
     */
    @Test
    public void testOidcPromptNoneSuccessWhenAuthenticatedAtProvider() {
        authenticateAtProvider();
        initiateLoginWithPromptNone();

        // Then the broker should start code flow without requiring user interaction
        var response = oauth.parseLoginResponse();
        assertThat("Authorization response should contain code", response.getCode(), Matchers.notNullValue());

        // Code can be exchanged for tokens
        var tokenResponse = oauth.doAccessTokenRequest(response.getCode());
        assertThat("Token response should contain access token", tokenResponse.getAccessToken(), Matchers.notNullValue());
    }

    /**
     * OIDC prompt=none login should fail when not authenticated at backing SAML IDP
     */
    @Test
    public void testOidcPromptNoneFailureWhenNotAuthenticatedAtProvider() {
        initiateLoginWithPromptNone();

        assertThatOAuthErrorIsReturned(OAuthErrorException.LOGIN_REQUIRED);
    }

    /**
     * OIDC prompt=none login should fail when IDP is configured to NOT forward passive authentication requests,
     * EVEN IF the user is already authenticated at the backing SAML IDP.
     */
    @Test
    public void testOidcPromptNoneFailureWhenProviderDoesNotAcceptPassiveAuthenticationRequests() {
        configureAcceptsPromptNoneForwardingFromClient(false);
        authenticateAtProvider();
        initiateLoginWithPromptNone();

        assertThatOAuthErrorIsReturned(OAuthErrorException.LOGIN_REQUIRED);
    }

    /**
     * OIDC prompt=none login should fail when user interaction is required,
     * EVEN IF the user is already authenticated at the backing SAML IDP.
     */
    @Test
    public void testOidcPromptNoneFailureWhenInteractionRequired() {
        authenticateAtProvider();
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
        initiateLoginWithPromptNone();

        assertThatOAuthErrorIsReturned(OAuthErrorException.INTERACTION_REQUIRED);
    }

    private void configureAcceptsPromptNoneForwardingFromClient(boolean accepts) {
        var idp = identityProviderResource.toRepresentation();
        idp.getConfig().put(IdentityProviderAuthenticator.ACCEPTS_PROMPT_NONE, Boolean.toString(accepts));
        identityProviderResource.update(idp);
    }

    private void initiateLoginWithPromptNone() {
        oauth
                .client(KcSamlBrokerConfiguration.CONSUMER_CLIENT_ID, KcSamlBrokerConfiguration.CONSUMER_CLIENT_SECRET)
                .realm(bc.consumerRealmName())
                .redirectUri(getConsumerBaseUriBuilder().path("app").build().toString())
                .loginForm()
                .prompt(OIDCLoginProtocol.PROMPT_VALUE_NONE)
                .param(AdapterConstants.KC_IDP_HINT, bc.getIDPAlias())
                .open();
    }

    private void assertThatOAuthErrorIsReturned(String error) {
        var response = oauth.parseLoginResponse();
        assertThat("OAuth response error expected", response.getError(), Matchers.equalTo(error));
    }

    /**
     * Authenticates the broker user directly in the SAML IDP to establish a valid authenticated session there.
     */
    private void authenticateAtProvider() {
        // Navigate to the provider realm's account console to establish a session
        URI providerOidcProtocolUrl = RealmsResource.protocolUrl(getProviderBaseUriBuilder()).build(bc.providerRealmName(), OIDCLoginProtocol.LOGIN_PROTOCOL);
        URI providerAccountUrl = RealmsResource.accountUrl(getProviderBaseUriBuilder()).build(bc.providerRealmName());
        driver.navigate().to(providerAccountUrl.toString());

        waitForPage(driver, "sign in to", true);
        assertThat("Driver should be on the provider realm login page",
                driver.getCurrentUrl(), Matchers.containsString(providerOidcProtocolUrl.toString()));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // Wait for redirect to account page, indicating successful authentication
        waitForPage(driver, "account", true);
        assertThat("User should be authenticated in the provider realm",
                driver.getCurrentUrl(), Matchers.containsString(providerAccountUrl.toString()));
    }

    private UriBuilder getProviderBaseUriBuilder() {
        return getBaseUri(getProviderRoot());
    }

    private UriBuilder getConsumerBaseUriBuilder() {
        return getBaseUri(getProviderRoot());
    }

    private UriBuilder getBaseUri(String root) {
        return UriBuilder.fromUri(root).path("auth");
    }

    /**
     * Logs out from both consumer and provider realms to ensure clean state.
     */
    private void logoutFromBothRealms() {
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
    }

}
