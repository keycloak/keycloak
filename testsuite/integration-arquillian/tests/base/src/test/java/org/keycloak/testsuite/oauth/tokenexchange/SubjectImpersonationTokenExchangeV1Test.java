/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth.tokenexchange;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.Profile;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for subject impersonation token exchange (including "direct naked impersonation")
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
@DisableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2, skipRestart = true)
public class SubjectImpersonationTokenExchangeV1Test extends AbstractKeycloakTest  {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Test
    @UncaughtServerErrorExpected
    @DisableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
    public void checkFeatureDisabled() {
        // Required feature should return Status code 400 - Feature doesn't work
        testingClient.server().run(TokenExchangeTestUtils::addDirectExchanger);
        Assert.assertEquals(400, checkTokenExchange().getStatus());
        testingClient.server().run(TokenExchangeTestUtils::removeDirectExchanger);
    }

    @Test
    public void checkFeatureEnabled() {
        // Test if the required feature really works.
        testingClient.server().run(TokenExchangeTestUtils::addDirectExchanger);
        Assert.assertEquals(200, checkTokenExchange().getStatus());
        testingClient.server().run(TokenExchangeTestUtils::removeDirectExchanger);
    }

    @Test
    @UncaughtServerErrorExpected
    public void testImpersonation() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");

        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // client-exchanger can impersonate from token "user" to user "impersonated-user"
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                    ));
            org.junit.Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            assertNotNull(exchangedToken.getAudience());
            Assert.assertEquals("impersonated-user", exchangedToken.getPreferredUsername());
            Assert.assertNull(exchangedToken.getRealmAccess());

            Object impersonatorRaw = exchangedToken.getOtherClaims().get("impersonator");
            assertThat(impersonatorRaw, instanceOf(Map.class));
            Map impersonatorClaim = (Map) impersonatorRaw;

            Assert.assertEquals(token.getSubject(), impersonatorClaim.get("id"));
            Assert.assertEquals("user", impersonatorClaim.get("username"));
        }

        // client-exchanger can impersonate from token "user" to user "impersonated-user" and to "target" client
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            org.junit.Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "impersonated-user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }

        try (Response response = exchangeUrl.request()
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.CLIENT_ID, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ))) {
            org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertEquals("Client is not the holder of the token",
                    response.readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }

        try (Response response = exchangeUrl.request()
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.CLIENT_ID, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.AUDIENCE, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ))) {
            org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertEquals("Client is not the holder of the token",
                    response.readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }

        try (Response response = exchangeUrl.request()
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.CLIENT_ID, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.AUDIENCE, "client-exchanger")
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ))) {
            org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertEquals("Client is not the holder of the token",
                    response.readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testIntrospectTokenAfterImpersonation() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");

        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = tokenResponse.getAccessToken();

        try (Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ))) {
            org.junit.Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            String exchangedTokenString = accessTokenResponse.getToken();
            JsonNode json = oauth.doIntrospectionAccessTokenRequest(exchangedTokenString).asJsonNode();
            assertTrue(json.get("active").asBoolean());
            assertEquals("impersonated-user", json.get("preferred_username").asText());
            assertEquals("user", json.get("act").get("sub").asText());
        }

        try (Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                .param(OAuth2Constants.AUDIENCE, "target")

                ))) {
            org.junit.Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            String exchangedTokenString = accessTokenResponse.getToken();
            JsonNode json = oauth.doIntrospectionAccessTokenRequest(exchangedTokenString).asJsonNode();
            assertTrue(json.get("active").asBoolean());
            assertEquals("impersonated-user", json.get("preferred_username").asText());
            assertEquals("user", json.get("act").get("sub").asText());
        }
    }

    @UncaughtServerErrorExpected
    @Test
    public void testImpersonationUsingPublicClient() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.client("direct-public", "secret");

        Client httpClient = AdminClientUtil.createResteasyClient();

        AuthorizationEndpointResponse authzResponse = oauth.doLogin("user", "password");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzResponse.getCode());
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public", null))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        org.junit.Assert.assertEquals(200, response.getStatus());
        AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
        response.close();

        String exchangedTokenString = accessTokenResponse.getToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        Assert.assertEquals("direct-public", exchangedToken.getIssuedFor());
        Assert.assertEquals("impersonated-user", exchangedToken.getPreferredUsername());
        Assert.assertNull(exchangedToken.getRealmAccess());

        testingClient.server().run(TokenExchangeTestUtils::setUpUserImpersonatePermissions);
    }

    @UncaughtServerErrorExpected
    @Test
    public void testImpersonationUsingTokenIssuedToUntrustedPublicClient() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);
        testingClient.server().run(TokenExchangeTestUtils::setUpUserImpersonatePermissions);

        oauth.realm(TEST);
        oauth.client("direct-public-untrusted", "secret");

        Client httpClient = AdminClientUtil.createResteasyClient();

        AuthorizationEndpointResponse authzResponse = oauth.doLogin("user", "password");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzResponse.getCode());
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public-untrusted", null))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
        oauth.client("direct-public", "secret");
        authzResponse = oauth.doLogin("user", "password");
        tokenResponse = oauth.doAccessTokenRequest(authzResponse.getCode());
        accessToken = tokenResponse.getAccessToken();

        response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public", null))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        org.junit.Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testBadImpersonator() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");

        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("bad-impersonator", "password");
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "bad-impersonator");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // test that user does not have impersonator permission
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                    ));
            org.junit.Assert.assertEquals(403, response.getStatus());
            response.close();
        }


    }

    @Test
    @UncaughtServerErrorExpected
    public void testDirectImpersonation() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);
        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        // direct-exchanger can impersonate from token "user" to user "impersonated-user"
        // see https://issues.redhat.com/browse/KEYCLOAK-5492
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                    ));
            Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("direct-exchanger", exchangedToken.getIssuedFor());
            Assert.assertNull(exchangedToken.getAudience());
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "impersonated-user");
            Assert.assertNull(exchangedToken.getRealmAccess());
        }

        // direct-legal can impersonate from token "user" to user "impersonated-user" and to "target" client
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-legal", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("direct-legal", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "impersonated-user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }

        // direct-public fails impersonation
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            Assert.assertEquals(403, response.getStatus());
            response.close();
        }

        // direct-no-secret fails impersonation
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-no-secret", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            assertTrue(response.getStatus() >= 400);
            response.close();
        }
    }


    private Response checkTokenExchange() {
        Client httpClient = AdminClientUtil.createResteasyClient();
        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");

        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-exchanger", "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        return response;
    }
}
