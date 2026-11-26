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
package org.keycloak.tests.oauth;

import java.security.KeyPair;
import java.util.List;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.util.DPoPGenerator;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests access-token consumers over HTTP, independently of the browser authorization flow. */
@KeycloakIntegrationTest
public class DPoPTokenBindingTest {

    @InjectRealm(config = TestRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    KeyPair key;

    @BeforeEach
    public void createKey() {
        key = KeyUtils.generateRsaKeyPair(2048);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void resourceRequestsRequireProofRegardlessOfTokenType(boolean bearerType) throws Exception {
        String original = getTokens(true).getToken();
        String token = bearerType ? runOnServer.fetchString(session -> {
            AccessToken decoded = session.tokens().decode(original, AccessToken.class);
            decoded.type(TokenUtil.TOKEN_TYPE_BEARER);
            return session.tokens().encode(decoded);
        }) : original;

        for (String url : resourceUrls()) {
            assertEquals(401, http.doGet(url).auth(token).header("Accept", "application/json").asStatus(), url);
            assertEquals(401, resourceRequest(url, token, null).asStatus(), url);
            assertEquals(401, resourceRequest(url, token, proof(key, "GET", url, null)).asStatus(), url);
            assertEquals(401, resourceRequest(url, token, proof(key, "GET", url, "another-token")).asStatus(), url);
            assertEquals(401, resourceRequest(url, token, proof(KeyUtils.generateRsaKeyPair(2048), "GET", url, token)).asStatus(), url);
            assertEquals(401, resourceRequest(url, token, proof(key, "POST", url, token)).asStatus(), url);
            assertEquals(401, resourceRequest(url, token, proof(key, "GET", url + "/other", token)).asStatus(), url);
            String proof = proof(key, "GET", url, token);
            try (SimpleHttpResponse response = resourceRequest(url, token, proof).asResponse()) {
                assertEquals(200, response.getStatus(), url + ": " + response.asString());
            }
            assertEquals(401, resourceRequest(url, token, proof).asStatus(), url);
        }
    }

    @Test
    public void bindingSurvivesClientConfigurationChanges() throws Exception {
        String boundToken = getTokens(true).getToken();
        realm.updateClientWithCleanup("bound-client", c -> c.attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "false"));
        String bearerToken = getTokens(false).getToken();
        for (String url : resourceUrls()) {
            assertEquals(401, http.doGet(url).auth(boundToken).header("Accept", "application/json").asStatus(), url);
            assertEquals(200, resourceRequest(url, boundToken, proof(key, "GET", url, boundToken)).asStatus(), url);
            assertEquals(200, http.doGet(url).auth(bearerToken).header("Accept", "application/json").asStatus(), url);
            assertEquals(401, resourceRequest(url, bearerToken, proof(key, "GET", url, bearerToken)).asStatus(), url);
        }

        ClientResource client = AdminApiUtil.findClientByClientId(realm.admin(), "bound-client");
        ClientRepresentation representation = client.toRepresentation();
        representation.getAttributes().put(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "true");
        client.update(representation);
        for (String url : resourceUrls()) {
            assertEquals(401, http.doGet(url).auth(bearerToken).header("Accept", "application/json").asStatus(), url);
        }
    }

    @Test
    public void introspectionDoesNotRequirePossessionOfTheKey() throws Exception {
        String token = getTokens(true).getToken();
        assertTrue(http.doPost(tokenEndpoint() + "/introspect").authBasic("bound-client", "secret")
                .param("token", token).asJson().get("active").asBoolean());
    }

    @Test
    public void tokenExchangeUsesTheValidatedTokenEndpointProof() throws Exception {
        String token = getTokens(true).getToken();
        assertEquals(400, exchange(token, "bound-client", null).asStatus());
        assertEquals(400, exchange(token, "bound-client", proof(KeyUtils.generateRsaKeyPair(2048), "POST", tokenEndpoint(), null)).asStatus());
        assertEquals(400, exchange(token, "other-client", proof(key, "POST", tokenEndpoint(), null)).asStatus());

        // Unlike a resource request, the token endpoint proof does not need an ath claim.
        String proof = proof(key, "POST", tokenEndpoint(), null);
        try (SimpleHttpResponse response = exchange(token, "bound-client", proof).asResponse()) {
            assertEquals(200, response.getStatus(), response.asString());
            AccessTokenResponse exchanged = response.asJson(AccessTokenResponse.class);
            assertEquals(TokenUtil.TOKEN_TYPE_DPOP, exchanged.getTokenType());
            String url = realm.getBaseUrl() + "/account";
            assertEquals(401, http.doGet(url).auth(exchanged.getToken()).header("Accept", "application/json").asStatus());
            assertEquals(200, resourceRequest(url, exchanged.getToken(), proof(key, "GET", url, exchanged.getToken())).asStatus());
        }
        assertEquals(400, exchange(token, "bound-client", proof).asStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void registrationPolicyCanReverifyAuthenticationInTheSameRequest(boolean dpop) throws Exception {
        realm.updateClientWithCleanup("bound-client", c -> c.attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, Boolean.toString(dpop)));
        String token = getTokens(dpop).getToken();
        String url = realm.getBaseUrl() + "/clients-registrations/openid-connect";
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("authenticated-registration");
        client.setRedirectUris(List.of("https://app.example/callback"));

        try (SimpleHttpResponse response = registrationRequest(url, client, token, dpop).asResponse()) {
            assertEquals(201, response.getStatus(), response.asString());
            String clientId = response.asJson(OIDCClientRepresentation.class).getClientId();
            realm.cleanup().add(r -> AdminApiUtil.findClientByClientId(r, clientId).remove());
        }

        ClientUpdaterContextCondition.Configuration condition = new ClientUpdaterContextCondition.Configuration();
        condition.setUpdateClientSource(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER));
        realm.updateClientProfile(List.of(ClientProfileBuilder.create().name("reject-authenticated-dcr")
                .executor(RejectRequestExecutorFactory.PROVIDER_ID, null).build()));
        realm.updateClientPolicy(List.of(ClientPolicyBuilder.create().name("authenticated-dcr")
                .condition(ClientUpdaterContextConditionFactory.PROVIDER_ID, condition)
                .profile("reject-authenticated-dcr").build()));
        try (SimpleHttpResponse response = registrationRequest(url, client, token, dpop).asResponse()) {
            assertEquals(403, response.getStatus(), response.asString());
            assertEquals("invalid_request", response.asJson().get("error_description").asText());
        } finally {
            ClientPoliciesRepresentation policies = new ClientPoliciesRepresentation();
            policies.setPolicies(List.of());
            realm.admin().clientPoliciesPoliciesResource().updatePolicies(policies);
        }
    }

    private SimpleHttpRequest registrationRequest(String url, OIDCClientRepresentation client, String token, boolean dpop) {
        SimpleHttpRequest request = http.doPost(url).json(client).header("Authorization", (dpop ? "DPoP " : "Bearer ") + token);
        return dpop ? request.header("DPoP", proof(key, "POST", url, token)) : request;
    }

    private SimpleHttpRequest exchange(String token, String clientId, String proof) {
        SimpleHttpRequest request = http.doPost(tokenEndpoint()).authBasic(clientId, "secret")
                .param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                .param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
                .param("subject_token", token);
        return proof == null ? request : request.header("DPoP", proof);
    }

    private SimpleHttpRequest resourceRequest(String url, String token, String proof) {
        SimpleHttpRequest request = http.doGet(url).header("Authorization", "dpop " + token).header("Accept", "application/json");
        return proof == null ? request : request.header("DPoP", proof);
    }

    private List<String> resourceUrls() {
        return List.of(realm.getBaseUrl() + "/account", realm.getBaseUrl().replace("/realms/", "/admin/realms/"),
                realm.getBaseUrl() + "/clients-registrations/default/bound-client", realm.getBaseUrl() + "/protocol/openid-connect/userinfo");
    }

    private String tokenEndpoint() {
        return realm.getBaseUrl() + "/protocol/openid-connect/token";
    }

    private AccessTokenResponse getTokens(boolean dpop) throws Exception {
        SimpleHttpRequest request = http.doPost(tokenEndpoint()).authBasic("bound-client", "secret")
                .param("grant_type", "password").param("username", "admin").param("password", "password").param("scope", "openid");
        if (dpop) {
            request.header("DPoP", proof(key, "POST", tokenEndpoint(), null));
        }
        try (SimpleHttpResponse response = request.asResponse()) {
            assertEquals(200, response.getStatus(), response.asString());
            return response.asJson(AccessTokenResponse.class);
        }
    }

    private String proof(KeyPair signingKey, String method, String url, String token) {
        return DPoPGenerator.generateRsaSignedDPoPProof(signingKey, method, url, token);
    }

    public static class TestRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create("bound-client").secret("secret").directAccessGrantsEnabled(true).fullScopeEnabled(true)
                            .defaultClientScopes("basic", "roles", "profile", "email", Constants.KEYCLOAK_ADMIN_API_SCOPE)
                            .attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "true")
                            .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, "true")
                            .protocolMappers(ProtocolMapperBuilder.create().name("bound-client-audience")
                                    .protocol("openid-connect").protocolMapper("oidc-audience-mapper")
                                    .config("included.client.audience", "bound-client").config("access.token.claim", "true").build()),
                            ClientBuilder.create("other-client").secret("secret")
                                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, "true"))
                    .users(UserBuilder.create("admin").password("password").name("Test", "Admin").email("admin@example.com")
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                            .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE));
        }
    }
}
