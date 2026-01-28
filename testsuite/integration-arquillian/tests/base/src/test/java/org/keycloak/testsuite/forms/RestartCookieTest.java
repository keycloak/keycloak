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

package org.keycloak.testsuite.forms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.SecretKey;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenCategory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedHmacKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ParConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.util.TokenUtil;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RestartCookieTest extends AbstractTestRealmKeycloakTest {


    @Page
    protected LoginPage loginPage;


    @Rule
    public AssertEvents events = new AssertEvents(this);

    // KC_RESTART cookie from Keycloak 3.1.0
    private static final String OLD_RESTART_COOKIE_JSON = "{\n" +
            "  \"cs\": \"874a1ea8-5579-4f21-add0-903dd8e3ec1b\",\n" +
            "  \"cid\": \"test-app\",\n" +
            "  \"pty\": \"openid-connect\",\n" +
            "  \"ruri\": \"http://localhost:8081/auth/realms/master/app/auth\",\n" +
            "  \"act\": \"AUTHENTICATE\",\n" +
            "  \"notes\": {\n" +
            "    \"auth_type\": \"code\",\n" +
            "    \"scope\": \"openid\",\n" +
            "    \"iss\": \"http://localhost:8081/auth/realms/master/app/auth\",\n" +
            "    \"response_type\": \"code\",\n" +
            "    \"redirect_uri\": \"http://localhost:8081/auth/realms/master/app/auth/\",\n" +
            "    \"state\": \"6c983e5b-2dc1-411a-9ed1-0f51095949c5\",\n" +
            "    \"code_challenge_method\": \"plain\",\n" +
            "    \"nonce\": \"65639660-99b2-4cdf-bc9f-9978fdce5b03\",\n" +
            "    \"response_mode\": \"fragment\"\n" +
            "  }\n" +
            "}";

    public static final Set<String> sensitiveNotes = new HashSet<>();
    static {
        sensitiveNotes.add(OAuth2Constants.CLIENT_ASSERTION_TYPE);
        sensitiveNotes.add(OAuth2Constants.CLIENT_ASSERTION);
        sensitiveNotes.add(OAuth2Constants.CLIENT_SECRET);
        sensitiveNotes.add(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + OAuth2Constants.CLIENT_ASSERTION_TYPE);
        sensitiveNotes.add(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + OAuth2Constants.CLIENT_ASSERTION);
        sensitiveNotes.add(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + OAuth2Constants.CLIENT_SECRET);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Override
    protected void afterAbstractKeycloakTestRealmImport() {
        // create a HS256 for the compatibility tests for previous RESTART cookie formats
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(GeneratedHmacKeyProviderFactory.ID + "-256");
        rep.setParentId(testRealm().toRepresentation().getId());
        rep.setProviderId(GeneratedHmacKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.addFirst(Attributes.PRIORITY_KEY, DefaultKeyProviders.DEFAULT_PRIORITY);
        config.addFirst(Attributes.ALGORITHM_KEY, Algorithm.HS256);
        rep.setConfig(config);

        try (Response res = testRealm().components().add(rep)) {
            Assert.assertEquals(Response.Status.CREATED.getStatusCode(), res.getStatus());
        }
    }

    @Test
    public void testRestartCookie() {
        loginPage.open();
        String restartCookie = driver.manage().getCookieNamed(RestartLoginCookie.KC_RESTART).getValue();
        assertRestartCookie(restartCookie);
    }

    @Test
    public void testRestartCookieWithPar() {
        String clientId = "par-confidential-client";
        adminClient.realm("test").clients().create(ClientBuilder.create()
                .clientId("par-confidential-client")
                .secret("secret")
                .redirectUris(oauth.getRedirectUri() + "/*")
                .attribute(ParConfig.REQUIRE_PUSHED_AUTHORIZATION_REQUESTS, "true")
                .build());

        oauth.client(clientId, "secret");
        String requestUri = null;
        try {
            ParResponse pResp = oauth.doPushedAuthorizationRequest();
            assertEquals(201, pResp.getStatusCode());
            requestUri = pResp.getRequestUri();
        }
        catch (Exception e) {
            Assert.fail();
        }

        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);

        oauth.loginForm().requestUri(requestUri).state("testRestartCookieWithPar").open();
        String restartCookie = driver.manage().getCookieNamed(RestartLoginCookie.KC_RESTART).getValue();
        assertRestartCookie(restartCookie);
    }

    private void assertRestartCookie(String restartCookie) {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                {
                    try {
                        String sigAlgorithm = session.tokens().signatureAlgorithm(TokenCategory.INTERNAL);
                        String encAlgorithm = session.tokens().cekManagementAlgorithm(TokenCategory.INTERNAL);
                        SecretKey encKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, encAlgorithm).getSecretKey();
                        SecretKey signKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, sigAlgorithm).getSecretKey();

                        byte[] contentBytes = TokenUtil.jweDirectVerifyAndDecode(encKey, signKey, restartCookie);
                        String jwt = new String(contentBytes, StandardCharsets.UTF_8);
                        RestartLoginCookie restartLoginCookie = session.tokens().decode(jwt, RestartLoginCookie.class);
                        Assert.assertFalse(restartLoginCookie.getNotes().keySet().stream().anyMatch(sensitiveNotes::contains));
                    } catch (Exception e) {
                        Assert.fail();
                    }
                });
    }

    // KEYCLOAK-5440 -- migration from Keycloak 3.1.0
    @Test
    public void testRestartCookieBackwardsCompatible_Keycloak25() throws IOException {
        String oldRestartCookie = testingClient.server().fetchString((KeycloakSession session) -> {
            String cookieVal = OLD_RESTART_COOKIE_JSON.replace("\n", "").replace(" ", "");
            RealmModel realm = session.realms().getRealmByName("test");

            KeyManager.ActiveHmacKey activeKey = session.keys().getActiveHmacKey(realm);

            String encodedToken = new JWSBuilder()
                    .kid(activeKey.getKid())
                    .content(cookieVal.getBytes(StandardCharsets.UTF_8))
                    .hmac256(activeKey.getSecretKey());

            return encodedToken;
        });

        oauth.openLoginForm();

        driver.manage().deleteAllCookies();
        driver.manage().addCookie(new Cookie(RestartLoginCookie.KC_RESTART, oldRestartCookie));

        loginPage.login("foo", "bar");
        loginPage.assertCurrent();
        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                .assertEvent();
    }


    // KEYCLOAK-7158 -- migration from Keycloak 1.9.8
    @Test
    public void testRestartCookieBackwardsCompatible_Keycloak19() throws IOException {
        String oldRestartCookie = testingClient.server().fetchString((KeycloakSession session) -> {
            String cookieVal = OLD_RESTART_COOKIE_JSON.replace("\n", "").replace(" ", "");
            RealmModel realm = session.realms().getRealmByName("test");

            KeyManager.ActiveHmacKey activeKey = session.keys().getActiveHmacKey(realm);

            // There was no KID in the token in Keycloak 1.9.8
            String encodedToken = new JWSBuilder()
                    //.kid(activeKey.getKid())
                    .content(cookieVal.getBytes(StandardCharsets.UTF_8))
                    .hmac256(activeKey.getSecretKey());

            return encodedToken;
        });

        oauth.openLoginForm();

        driver.manage().deleteAllCookies();
        driver.manage().addCookie(new Cookie(RestartLoginCookie.KC_RESTART, oldRestartCookie));

        loginPage.login("foo", "bar");
        loginPage.assertCurrent();
        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                .assertEvent();
    }
}
