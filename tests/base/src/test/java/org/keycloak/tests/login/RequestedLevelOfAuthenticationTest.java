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

import org.keycloak.cookie.CookieType;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class RequestedLevelOfAuthenticationTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient(config = AcrLoaMapClientConfig.class)
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void mappedAcrValueResolvesToConfiguredLoa() {
        openLoginFormWithAcrClaim(false, "gold");
        assertEquals("2", fetchRequestedLoaNote());
    }

    @Test
    public void mappingTakesPrecedenceOverNumericAcrValue() {
        openLoginFormWithAcrClaim(false, "5");
        assertEquals("3", fetchRequestedLoaNote());
    }

    @Test
    public void unmappedNumericAcrValueResolvesToItsNumericValue() {
        openLoginFormWithAcrClaim(false, "7");
        assertEquals("7", fetchRequestedLoaNote());
    }

    @Test
    public void lowestRequestedLevelWins() {
        openLoginFormWithAcrClaim(false, "gold", "7");
        assertEquals("2", fetchRequestedLoaNote());
    }

    @Test
    public void unknownNonEssentialAcrValueFallsBackToMinimumLoa() {
        openLoginFormWithAcrClaim(false, "iron");
        assertEquals(String.valueOf(Constants.MINIMUM_LOA), fetchRequestedLoaNote());
    }

    @Test
    public void unknownEssentialAcrValueShowsErrorPage() {
        openLoginFormWithAcrClaim(true, "uranium");
        assertEquals("Invalid parameter: claims", errorPage.getError());
    }

    private void openLoginFormWithAcrClaim(boolean essential, String... acrValues) {
        for (Cookie cookie : driver.driver().manage().getCookies()) {
            if (cookie.getName().startsWith(CookieType.AUTH_SESSION_ID.getName())) {
                driver.driver().manage().deleteCookie(cookie);
            }
        }
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        if (essential || acrValues.length > 1) {
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

    public static class AcrLoaMapClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .secret("test-secret")
                    .attribute(Constants.ACR_LOA_MAP, "{\"silver\":1,\"gold\":2,\"5\":3}");
        }
    }
}
