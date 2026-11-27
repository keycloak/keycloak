/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.forms;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectDependency;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.saml.SamlClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class SSOLogoutTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String SAML_ACS_PATH = "/mixed-protocol-saml/acs";
    private static final String SAML_LOGOUT_PATH = "/mixed-protocol-saml/slo";

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = UserConfig.class)
    ManagedUser managedUser;

    @InjectOAuthClient
    OAuthClient oidcClient;

    @InjectClient(config = SamlClientConfig.class)
    ManagedClient samlClient;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    void logoutMixedProtocolSessionOidcInitiated() throws Exception {
        AccessTokenResponse tokenResponse = loginOidcClient();

        String samlEndpoint = getSamlEndpoint();
        String acsUrl = getAssertionConsumerUrl();

        loginSamlClient(samlEndpoint, acsUrl);
        assertSharedSessionContainsBothClients();

        samlClient.updateWithCleanup(client -> client.attribute(
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                samlEndpoint));

        oidcClient.logoutForm()
                .idTokenHint(tokenResponse.getIdToken())
                .postLogoutRedirectUri(oidcClient.getRedirectUri())
                .open();

        if (driver.getCurrentUrl().contains("logout-confirm")) {
            driver.driver().findElement(By.id("kc-logout")).click();
        }

        new WebDriverWait(driver.driver(), TIMEOUT)
                .withMessage(() -> "Expected OIDC logout redirect at "
                        + oidcClient.getRedirectUri()
                        + " but was "
                        + driver.getCurrentUrl())
                .until(webDriver -> webDriver.getCurrentUrl() != null
                        && webDriver.getCurrentUrl().startsWith(
                        oidcClient.getRedirectUri()));

        // OIDC initiated the logout, so it owns the final redirect.
        assertTrue(driver.getCurrentUrl().startsWith(oidcClient.getRedirectUri()),
                () -> "Expected OIDC logout redirect but was: " + driver.getCurrentUrl());
    }

    @Test
    void logoutMixedProtocolSessionSamlInitiated() throws ConfigurationException, ProcessingException, ParsingException, IOException {
        loginOidcClient();

        String samlEndpoint = getSamlEndpoint();
        String acsUrl = getAssertionConsumerUrl();
        String logoutResponseUrl = getLogoutResponseUrl();

        loginSamlClient(samlEndpoint, acsUrl);
        assertSharedSessionContainsBothClients();

        samlClient.updateWithCleanup(client -> client
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, logoutResponseUrl)
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, logoutResponseUrl));

        NameIDType nameId = new NameIDType();
        nameId.setValue(managedUser.getUsername());
        String logoutRequestUri = new BaseSAML2BindingBuilder()
                .redirectBinding(new SAML2LogoutRequestBuilder()
                        .destination(samlEndpoint)
                        .issuer(samlClient.getClientId())
                        .nameId(nameId)
                        .buildDocument())
                .requestURI(samlEndpoint)
                .toString();

        driver.open(logoutRequestUri);

        new WebDriverWait(driver.driver(), TIMEOUT)
                .withMessage(() -> "Expected SAML logout response at "
                        + logoutResponseUrl
                        + " but was "
                        + driver.getCurrentUrl())
                .until(webDriver -> webDriver.getCurrentUrl() != null
                        && webDriver.getCurrentUrl()
                        .startsWith(logoutResponseUrl));

        /*
         * SAML initiated the logout, so it owns the final destination.
         * Do not assert SAMLResponse in the URL: POST binding sends it in
         * the request body.
         */
        assertTrue(
                driver.getCurrentUrl().startsWith(logoutResponseUrl),
                () -> "Expected SAML logout destination but was: "
                        + driver.getCurrentUrl());
    }

    private AccessTokenResponse loginOidcClient() {
        oidcClient.openLoginForm();

        loginPage.assertCurrent();
        loginPage.fillLogin(managedUser.getUsername(), managedUser.getPassword());
        loginPage.submit();

        driver.waiting().waitForOAuthCallback();

        String code = oidcClient.parseLoginResponse().getCode();
        return oidcClient.doAccessTokenRequest(code);
    }

    private void loginSamlClient(String samlEndpoint, String acsUrl) throws ConfigurationException, ParsingException, ProcessingException, IOException {
        AuthnRequestType loginRequest =
                SamlClient.createLoginRequestDocument(
                        samlClient.getClientId(),
                        acsUrl,
                        URI.create(samlEndpoint));

        loginRequest.setProtocolBinding(SamlClient.Binding.POST.getBindingUri());

        String encodedRequest = new BaseSAML2BindingBuilder()
                .postBinding(SAML2Request.convert(loginRequest))
                .encoded();

        driver.open("about:blank");

        ((JavascriptExecutor) driver.driver()).executeScript("""
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = arguments[0];

                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = arguments[1];
                input.value = arguments[2];

                form.appendChild(input);
                document.body.appendChild(form);
                form.submit();
                """,
                samlEndpoint,
                GeneralConstants.SAML_REQUEST_KEY,
                encodedRequest);

        new WebDriverWait(driver.driver(), TIMEOUT)
                .withMessage(() -> "Expected SAML response at "
                        + acsUrl
                        + " but was "
                        + driver.getCurrentUrl())
                .until(webDriver -> webDriver.getCurrentUrl() != null
                        && webDriver.getCurrentUrl().startsWith(acsUrl));
    }

    private void assertSharedSessionContainsBothClients() {
        Map<String, String> clients = managedUser.admin()
                .getUserSessions()
                .get(0)
                .getClients();
        assertTrue(clients.containsValue(oidcClient.getClientId()), "session should include the OIDC client");
        assertTrue(clients.containsValue(samlClient.getClientId()), "session should include the SAML client");
    }

    private String getSamlEndpoint() {
        return RealmsResource.protocolUrl(
                        UriBuilder.fromUri(keycloakUrls.getBase()))
                .build(managedRealm.getName(), SamlProtocol.LOGIN_PROTOCOL)
                .toString();
    }

    private String getAssertionConsumerUrl() {
        return keycloakUrls.getBase() + SAML_ACS_PATH;
    }

    private String getLogoutResponseUrl() {
        return keycloakUrls.getBase() + SAML_LOGOUT_PATH;
    }

    public static class SamlClientConfig implements ClientConfig {
        @InjectDependency
        ManagedRealm realm;

        @InjectDependency
        KeycloakUrls keycloakUrls;

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            String assertionConsumerUrl = keycloakUrls.getBase() + SAML_ACS_PATH;
            String logoutResponseUrl = keycloakUrls.getBase() + SAML_LOGOUT_PATH;
            String samlClientId = RealmsResource.realmBaseUrl(
                            UriBuilder.fromUri(keycloakUrls.getBase()))
                    .build(realm.getName())
                    .toString();

            return client.clientId(samlClientId)
                    .enabled(true)
                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                    .redirectUris(assertionConsumerUrl)
                    .frontchannelLogout(true)
                    .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, logoutResponseUrl)
                    .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, logoutResponseUrl)
                    .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, assertionConsumerUrl)
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
                    .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, SamlProtocol.ATTRIBUTE_FALSE_VALUE);
        }
    }

    public static class UserConfig implements org.keycloak.testframework.realm.UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user")
                    .password("Password123")
                    .name("test", "user")
                    .email("test-user@email.org")
                    .emailVerified(true);
        }
    }
}
