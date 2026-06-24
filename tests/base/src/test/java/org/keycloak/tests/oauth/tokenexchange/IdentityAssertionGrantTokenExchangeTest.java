/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oauth.tokenexchange;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests issuing an Identity Assertion JWT (ID-JAG) via token exchange (Keycloak as the enterprise
 * IdP). The assertions mirror the MCP conformance suite's cross-app-access wire contract.
 */
@KeycloakIntegrationTest(config = IdentityAssertionGrantTokenExchangeTest.ServerConfig.class)
public class IdentityAssertionGrantTokenExchangeTest {

    private static final String MCP_CLIENT = "mcp-client";
    private static final String MCP_CLIENT_SECRET = "secret";
    // The client_id this client is known by at the Resource AS - intentionally different from
    // MCP_CLIENT to assert the dual-client_id behaviour required by the spec (section 6.1).
    private static final String DOWNSTREAM_CLIENT_ID = "mcp-client-at-resource-as";
    private static final String DISABLED_CLIENT = "mcp-client-disabled";
    private static final String PUBLIC_CLIENT = "mcp-client-public";
    private static final String OTHER_CLIENT = "other-client";

    // The Resource Authorization Server issuer (RFC 8693 "audience") and the MCP server resource
    // identifier ("resource"). Both are opaque URIs to the issuer and are echoed into the ID-JAG.
    private static final String RESOURCE_AS_ISSUER = "https://auth.chat.example";
    private static final String MCP_SERVER_RESOURCE = "https://mcp.chat.example/mcp";

    @InjectRealm(config = IdJagRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @Test
    public void testIssueIdentityAssertionGrant() {
        String idToken = login(MCP_CLIENT, MCP_CLIENT_SECRET);
        IDToken parsedIdToken = parse(idToken, IDToken.class);

        events.clear();
        AccessTokenResponse response = oauth.tokenExchangeRequest(idToken, OAuth2Constants.ID_TOKEN_TYPE)
                .client(MCP_CLIENT, MCP_CLIENT_SECRET)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .audience(RESOURCE_AS_ISSUER)
                .resource(MCP_SERVER_RESOURCE)
                .send();

        // Response shape (RFC 8693 + SEP-990)
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        assertEquals(OAuth2Constants.ID_JAG_TOKEN_TYPE, response.getIssuedTokenType());
        assertEquals("N_A", response.getTokenType());
        assertNotNull(response.getAccessToken());

        // The ID-JAG itself
        JsonWebToken idJag = parseIdJag(response.getAccessToken());
        assertEquals(oauth.getEndpoints().getIssuer(), idJag.getIssuer(), "iss must be this realm");
        assertTrue(idJag.hasAudience(RESOURCE_AS_ISSUER), "aud must be the requested Resource AS issuer");
        assertEquals(parsedIdToken.getSubject(), idJag.getSubject(), "sub must echo the subject token");
        // client_id is the downstream client_id, NOT the authenticating Keycloak clientId (section 6.1)
        assertEquals(DOWNSTREAM_CLIENT_ID, idJag.getOtherClaims().get("client_id"));
        assertEquals(MCP_SERVER_RESOURCE, idJag.getOtherClaims().get(OAuth2Constants.RESOURCE));
        assertNotNull(idJag.getId(), "jti must be present");
        assertNotNull(idJag.getIat(), "iat must be present");
        assertNotNull(idJag.getExp(), "exp must be present");
        // email is carried through for account linking when present on the subject token
        assertEquals(parsedIdToken.getEmail(), idJag.getOtherClaims().get("email"));

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.TOKEN_EXCHANGE)
                .clientId(MCP_CLIENT);
    }

    @Test
    public void testSubjectTokenTypeMustBeIdToken() {
        String idToken = login(MCP_CLIENT, MCP_CLIENT_SECRET);

        // Declaring the subject token as an access token is rejected - the ID-JAG flow requires an ID token.
        AccessTokenResponse response = oauth.tokenExchangeRequest(idToken, OAuth2Constants.ACCESS_TOKEN_TYPE)
                .client(MCP_CLIENT, MCP_CLIENT_SECRET)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .audience(RESOURCE_AS_ISSUER)
                .resource(MCP_SERVER_RESOURCE)
                .send();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
    }

    @Test
    public void testAudienceRequired() {
        String idToken = login(MCP_CLIENT, MCP_CLIENT_SECRET);

        AccessTokenResponse response = oauth.tokenExchangeRequest(idToken, OAuth2Constants.ID_TOKEN_TYPE)
                .client(MCP_CLIENT, MCP_CLIENT_SECRET)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .resource(MCP_SERVER_RESOURCE)
                .send();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
    }

    @Test
    public void testAudienceNotAllowed() {
        String idToken = login(MCP_CLIENT, MCP_CLIENT_SECRET);

        // An audience that is not in the client's allow-list is denied (policy enforcement).
        AccessTokenResponse response = oauth.tokenExchangeRequest(idToken, OAuth2Constants.ID_TOKEN_TYPE)
                .client(MCP_CLIENT, MCP_CLIENT_SECRET)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .audience("https://not-allowed.example")
                .resource(MCP_SERVER_RESOURCE)
                .send();

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.ACCESS_DENIED, response.getError());
    }

    @Test
    public void testAccessTokenAsSubjectRejected() {
        // A valid access token (typ=Bearer) presented as subject_token_type=id_token must be rejected.
        oauth.client(MCP_CLIENT, MCP_CLIENT_SECRET);
        oauth.openid(false);
        oauth.scope(null);
        String accessToken = oauth.doPasswordGrantRequest("alice", "password").getAccessToken();

        AccessTokenResponse response = oauth.tokenExchangeRequest(accessToken, OAuth2Constants.ID_TOKEN_TYPE)
                .client(MCP_CLIENT, MCP_CLIENT_SECRET)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .audience(RESOURCE_AS_ISSUER)
                .resource(MCP_SERVER_RESOURCE)
                .send();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
    }

    @Test
    public void testSubjectTokenIssuedForDifferentClientRejected() {
        // Mint an ID token that belongs to other-client...
        oauth.client(OTHER_CLIENT, MCP_CLIENT_SECRET);
        oauth.openid(true);
        oauth.scope("openid");
        String otherIdToken = oauth.doPasswordGrantRequest("alice", "password").getIdToken();
        oauth.openid(false);
        oauth.scope(null);

        // ...and confirm mcp-client cannot exchange it.
        AccessTokenResponse response = oauth.tokenExchangeRequest(otherIdToken, OAuth2Constants.ID_TOKEN_TYPE)
                .client(MCP_CLIENT, MCP_CLIENT_SECRET)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .audience(RESOURCE_AS_ISSUER)
                .resource(MCP_SERVER_RESOURCE)
                .send();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
    }

    @Test
    public void testIssuanceNotEnabledForClient() {
        String idToken = login(MCP_CLIENT, MCP_CLIENT_SECRET);

        // The requesting client does not have ID-JAG issuance enabled.
        AccessTokenResponse response = oauth.tokenExchangeRequest(idToken, OAuth2Constants.ID_TOKEN_TYPE)
                .client(DISABLED_CLIENT, MCP_CLIENT_SECRET)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .audience(RESOURCE_AS_ISSUER)
                .resource(MCP_SERVER_RESOURCE)
                .send();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, response.getError());
    }

    @Test
    public void testPublicClientRejected() {
        String idToken = login(MCP_CLIENT, MCP_CLIENT_SECRET);

        AccessTokenResponse response = oauth.tokenExchangeRequest(idToken, OAuth2Constants.ID_TOKEN_TYPE)
                .client(PUBLIC_CLIENT)
                .requestedTokenType(OAuth2Constants.ID_JAG_TOKEN_TYPE)
                .audience(RESOURCE_AS_ISSUER)
                .resource(MCP_SERVER_RESOURCE)
                .send();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
    }

    @Test
    public void testDiscoveryAdvertisesGrantProfile() {
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest();
        assertNotNull(config.getAuthorizationGrantProfilesSupported(), "authorization_grant_profiles_supported must be present");
        assertTrue(config.getAuthorizationGrantProfilesSupported().contains(OAuth2Constants.ID_JAG_GRANT_PROFILE),
                "Discovery must advertise the id-jag grant profile");
    }

    private String login(String clientId, String secret) {
        events.clear();
        oauth.client(clientId, secret);
        oauth.openid(true);
        oauth.scope("openid email");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("alice", "password");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        assertNotNull(response.getIdToken(), "Expected an ID token from the login");
        // Reset OIDC scope so it is not carried into the subsequent token exchange request.
        oauth.openid(false);
        oauth.scope(null);
        return response.getIdToken();
    }

    private JsonWebToken parseIdJag(String idJag) {
        try {
            JWSInput jws = new JWSInput(idJag);
            assertEquals(OAuth2Constants.IDENTITY_ASSERTION_JWT_HEADER_TYPE, jws.getHeader().getType(),
                    "ID-JAG must carry the oauth-id-jag+jwt header type");
            return jws.readJsonContent(JsonWebToken.class);
        } catch (JWSInputException e) {
            fail("Could not parse ID-JAG", e);
            return null;
        }
    }

    private <T extends JsonWebToken> T parse(String token, Class<T> clazz) {
        try {
            return new JWSInput(token).readJsonContent(clazz);
        } catch (JWSInputException e) {
            fail("Could not parse token", e);
            return null;
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_ASSERTION_JWT);
        }
    }

    public static class IdJagRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.eventsEnabled(true);

            realm.users(UserBuilder.create()
                    .username("alice")
                    .name("Alice", "Doe")
                    .email("alice@email.cz")
                    .emailVerified(true)
                    .password("password"));

            // Confidential client allowed to issue ID-JAGs for RESOURCE_AS_ISSUER, presenting
            // DOWNSTREAM_CLIENT_ID as its client_id at the Resource AS.
            realm.clients(ClientBuilder.create()
                    .clientId(MCP_CLIENT)
                    .secret(MCP_CLIENT_SECRET)
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .attribute(OIDCConfigAttributes.ID_JAG_ISSUANCE_ENABLED, "true")
                    .attribute(OIDCConfigAttributes.ID_JAG_ALLOWED_AUDIENCES, RESOURCE_AS_ISSUER)
                    .attribute(OIDCConfigAttributes.ID_JAG_CLIENT_ID, DOWNSTREAM_CLIENT_ID));

            // Confidential client WITHOUT ID-JAG issuance enabled.
            realm.clients(ClientBuilder.create()
                    .clientId(DISABLED_CLIENT)
                    .secret(MCP_CLIENT_SECRET)
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true));

            // Public client - issuance is configured but must still be rejected because the client
            // is public (the ID-JAG delegates the user's identity and requires client authentication).
            realm.clients(ClientBuilder.create()
                    .clientId(PUBLIC_CLIENT)
                    .publicClient(true)
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .attribute(OIDCConfigAttributes.ID_JAG_ISSUANCE_ENABLED, "true")
                    .attribute(OIDCConfigAttributes.ID_JAG_ALLOWED_AUDIENCES, RESOURCE_AS_ISSUER));

            // Another confidential client, used to mint an ID token that belongs to a different client.
            realm.clients(ClientBuilder.create()
                    .clientId(OTHER_CLIENT)
                    .secret(MCP_CLIENT_SECRET)
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true));

            return realm;
        }
    }
}
