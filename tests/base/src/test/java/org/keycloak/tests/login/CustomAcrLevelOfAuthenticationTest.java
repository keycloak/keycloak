/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.login;

import java.util.Arrays;
import java.util.Collections;

import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.cookie.CookieType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.tests.providers.oidc.CustomAcrLevelOfAuthenticationProviderFactory;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = CustomAcrLevelOfAuthenticationTest.ServerConfig.class)
public class CustomAcrLevelOfAuthenticationTest {

    private static final String STEP_UP_FLOW = "stepUpFlow";

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = MinimumAcrClientConfig.class)
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void customAcrValueAboveMinimumResolvesThroughProvider() {
        openLoginFormWithAcrClaim("level=3,method=otp");
        assertEquals("3", fetchRequestedLoaNote());
    }

    @Test
    public void customAcrValueBelowMinimumIsUpgradedToMinimum() {
        openLoginFormWithAcrClaim("level=0,method=otp");
        assertEquals("1", fetchRequestedLoaNote());
    }

    @Test
    public void tokenAcrIsRequestedAcrValueOfReachedLevel() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(STEP_UP_FLOW));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(STEP_UP_FLOW)
                .inForms(forms -> forms
                        .clear()
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                        config -> {
                                            config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                            config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                        })
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        )
                )
                .defineAsBrowserFlow()
        );

        openLoginFormWithAcrClaim("level=2,method=otp", "level=1,method=password");
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        IDToken idToken = oauth.verifyToken(tokenResponse.getIdToken(), IDToken.class);
        assertEquals("level=1,method=password", idToken.getAcr());

        oauth.logoutRequest().idTokenHint(tokenResponse.getIdToken()).send();
    }

    private void openLoginFormWithAcrClaim(String... acrValues) {
        for (Cookie cookie : driver.driver().manage().getCookies()) {
            if (cookie.getName().startsWith(CookieType.AUTH_SESSION_ID.getName())) {
                driver.driver().manage().deleteCookie(cookie);
            }
        }
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        if (acrValues.length > 1) {
            acrClaim.setValues(Arrays.asList(acrValues));
        } else {
            acrClaim.setValue(acrValues[0]);
        }
        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap(IDToken.ACR, acrClaim));
        oauth.loginForm().claims(claims).open();
    }

    private String fetchRequestedLoaNote() {
        loginPage.assertCurrent();
        String encodedAuthSessionId = driver.driver().manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
        String realmId = realm.getId();
        return runOnServer.fetchString(session -> {
            RealmModel realmModel = session.realms().getRealm(realmId);
            session.getContext().setRealm(realmModel);
            AuthenticationSessionManager authenticationSessionManager = new AuthenticationSessionManager(session);
            String authSessionId = authenticationSessionManager.decodeBase64AndValidateSignature(encodedAuthSessionId);
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realmModel, authSessionId);
            AuthenticationSessionModel authSession = rootAuthSession.getAuthenticationSessions().values().iterator().next();
            return authSession.getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION);
        });
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers")
                    .option("spi-oidc-level-of-authentication--provider", CustomAcrLevelOfAuthenticationProviderFactory.PROVIDER_ID);
        }
    }

    public static class MinimumAcrClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .secret("test-secret")
                    .attribute(Constants.ACR_LOA_MAP, "{\"level=1\":1}")
                    .attribute(Constants.MINIMUM_ACR_VALUE, "level=1");
        }
    }
}
