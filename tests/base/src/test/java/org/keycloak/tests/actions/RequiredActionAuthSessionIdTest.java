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
package org.keycloak.tests.actions;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.keycloak.cookie.CookieType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.StickySessionEncoderProvider;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;

/**
 * The {@code auth_session_id} query parameter of a login actions request must hold the same signed and encoded value
 * as the {@code AUTH_SESSION_ID} cookie, as {@code SessionCodeChecks} decodes it and compares it with the cookie.
 */
@KeycloakIntegrationTest
public class RequiredActionAuthSessionIdTest {

    private static final String PASSWORD = "password";

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectUser(config = UpdatePasswordUserConfig.class)
    ManagedUser user;

    @InjectHttpClient(followRedirects = false)
    HttpClient httpClient;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    public static class UpdatePasswordUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("required-action-auth-session-id")
                    .name("Required", "Action")
                    .password(PASSWORD)
                    .email("required-action-auth-session-id@localhost")
                    .emailVerified(true)
                    .requiredActions(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        }
    }

    /**
     * A login request that carries {@code auth_session_id} is redirected to the required action with that parameter
     * kept. The redirect must sign the id like the cookie, otherwise the required action request cannot match the
     * parameter to an authentication session.
     */
    @Test
    void redirectToRequiredActionKeepsSignedAuthSessionId() throws Exception {
        oauth.openLoginForm();
        String loginActionUrl = driver.driver().findElement(By.id("kc-form-login")).getAttribute("action");
        Cookie authSessionCookie = driver.cookies().get(CookieType.AUTH_SESSION_ID);
        Assertions.assertNotNull(authSessionCookie, "The login form must set the AUTH_SESSION_ID cookie");
        String cookieHeader = CookieType.AUTH_SESSION_ID.getName() + "=" + authSessionCookie.getValue();
        // The parameter holds the signed id without the sticky session route that the cookie may carry
        String signedAuthSessionId = withoutRoute(authSessionCookie.getValue());
        String rootId = decodeAuthSessionId(signedAuthSessionId);
        Assertions.assertNotNull(rootId, "The AUTH_SESSION_ID cookie must hold a signed auth session id");

        HttpPost login = new HttpPost(loginActionUrl + "&" + LoginActionsService.AUTH_SESSION_ID + "="
                + URLEncoder.encode(signedAuthSessionId, StandardCharsets.UTF_8));
        login.setHeader("Cookie", cookieHeader);
        login.setEntity(new UrlEncodedFormEntity(List.of(
                new BasicNameValuePair("username", user.getUsername()),
                new BasicNameValuePair("password", PASSWORD))));
        String location;
        HttpResponse loginResponse = httpClient.execute(login);
        try {
            Assertions.assertEquals(302, loginResponse.getStatusLine().getStatusCode(), "The login must redirect to the required action");
            location = loginResponse.getFirstHeader("Location").getValue();
        } finally {
            EntityUtils.consumeQuietly(loginResponse.getEntity());
        }
        Assertions.assertTrue(location.contains("/login-actions/" + LoginActionsService.REQUIRED_ACTION), "Unexpected redirect: " + location);

        String authSessionIdParam = URLEncodedUtils.parse(URI.create(location), StandardCharsets.UTF_8).stream()
                .filter(param -> LoginActionsService.AUTH_SESSION_ID.equals(param.getName()))
                .map(NameValuePair::getValue)
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(authSessionIdParam, "The redirect must keep the auth_session_id parameter: " + location);
        Assertions.assertEquals(rootId, decodeAuthSessionId(authSessionIdParam),
                "The auth_session_id parameter must be signed and encoded like the AUTH_SESSION_ID cookie");

        HttpGet requiredAction = new HttpGet(location);
        requiredAction.setHeader("Cookie", cookieHeader);
        HttpResponse requiredActionResponse = httpClient.execute(requiredAction);
        try {
            Assertions.assertEquals(200, requiredActionResponse.getStatusLine().getStatusCode());
            Assertions.assertTrue(EntityUtils.toString(requiredActionResponse.getEntity()).contains("kc-passwd-update-form"),
                    "The update password form must be shown");
        } finally {
            EntityUtils.consumeQuietly(requiredActionResponse.getEntity());
        }
    }

    /**
     * Removes the sticky session route from an {@code AUTH_SESSION_ID} cookie value, as the server does when it reads the cookie.
     */
    private String withoutRoute(String cookieValue) {
        return runOnServer.fetch(session -> session.getProvider(StickySessionEncoderProvider.class)
                .decodeSessionIdAndRoute(cookieValue).sessionId(), String.class);
    }

    /**
     * Decodes and verifies a signed auth session id as {@code SessionCodeChecks} does, {@code null} if it is not valid.
     */
    private String decodeAuthSessionId(String encodedAuthSessionId) {
        final String realmName = realm.getName();
        String decoded = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realmModel);
            return String.valueOf(new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(encodedAuthSessionId));
        }, String.class);
        return "null".equals(decoded) ? null : decoded;
    }
}
