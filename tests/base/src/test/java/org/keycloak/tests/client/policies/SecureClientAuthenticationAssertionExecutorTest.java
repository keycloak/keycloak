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
package org.keycloak.tests.client.policies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticationAssertionExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the secure-client-authentication-assertion executor. FAPI 2.0 Security Profile section 5.3.2.1
 * requires the authorization server to only accept its issuer identifier, as a string, in the aud claim of
 * client authentication assertions. The array forms must be rejected, including single-element arrays.
 */
@KeycloakIntegrationTest
public class SecureClientAuthenticationAssertionExecutorTest extends AbstractClientPoliciesTest {

    private static final String CLIENT_SECRET = "atleast-14chars-password";

    @InjectRealm(config = ClientAuthRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = ClientAuthClientConfig.class)
    OAuthClient oauth;

    @AfterEach
    void resetPolicies() {
        realm.updateWithCleanup(r -> r.resetClientProfiles().resetClientPolicies());
    }

    @Test
    void audIssuerStringAccepted() throws Exception {
        setupPolicy();

        AccessTokenResponse response = requestTokenWithAudience(TextNode.valueOf(oauth.getEndpoints().getIssuer()));
        Assertions.assertTrue(response.isSuccess());
    }

    @Test
    void audIssuerSingleElementArrayRejected() throws Exception {
        setupPolicy();

        ArrayNode aud = JsonNodeFactory.instance.arrayNode().add(oauth.getEndpoints().getIssuer());
        AccessTokenResponse response = requestTokenWithAudience(aud);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        Assertions.assertEquals("invalid audience in client assertion", response.getErrorDescription());
    }

    @Test
    void audIssuerMultipleElementArrayRejected() throws Exception {
        setupPolicy();

        ArrayNode aud = JsonNodeFactory.instance.arrayNode().add(oauth.getEndpoints().getIssuer())
                .add("https://attacker.example/realms/x");
        AccessTokenResponse response = requestTokenWithAudience(aud);

        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    void audTokenEndpointStringRejected() throws Exception {
        setupPolicy();

        AccessTokenResponse response = requestTokenWithAudience(TextNode.valueOf(oauth.getEndpoints().getToken()));

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        Assertions.assertEquals("invalid audience in client assertion", response.getErrorDescription());
    }

    @Test
    void audIssuerSingleElementArrayAcceptedWithoutProfile() {
        // without the profile the general client authentication flow is unchanged and still accepts the array form
        ArrayNode aud = JsonNodeFactory.instance.arrayNode().add(oauth.getEndpoints().getIssuer());

        AccessTokenResponse response = requestTokenWithAudience(aud);

        Assertions.assertTrue(response.isSuccess());
    }

    private void setupPolicy() throws Exception {
        setupPolicy(realm, SecureClientAuthenticationAssertionExecutorFactory.PROVIDER_ID,
                new ClientPolicyExecutorConfigurationRepresentation(), AnyClientConditionFactory.PROVIDER_ID,
                new ClientPolicyConditionConfigurationRepresentation());
    }

    private AccessTokenResponse requestTokenWithAudience(JsonNode aud) {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        String clientAssertion = createClientAssertion(aud);
        return oauth.accessTokenRequest(code).signedJwt(clientAssertion).send();
    }

    private String createClientAssertion(JsonNode aud) {
        ObjectNode payload = JsonSerialization.createObjectNode();
        payload.put("iss", oauth.getClientId());
        payload.put("sub", oauth.getClientId());
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("iat", (long) Time.currentTime());
        payload.put("exp", (long) (Time.currentTime() + 300));
        payload.set(JsonWebToken.AUD, aud);

        try {
            return new JWSBuilder().content(JsonSerialization.writeValueAsBytes(payload))
                    .hmac256(CLIENT_SECRET.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ClientAuthRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create("test-user@localhost").password("password")
                    .name("Test", "User").email("test-user@localhost").emailVerified(true));
        }
    }

    public static class ClientAuthClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .secret(CLIENT_SECRET)
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(false)
                    .authenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID)
                    .redirectUris("http://127.0.0.1:8500/callback/oauth");
        }
    }
}
