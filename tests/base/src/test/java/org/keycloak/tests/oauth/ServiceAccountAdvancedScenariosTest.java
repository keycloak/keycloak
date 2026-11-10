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
package org.keycloak.tests.oauth;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Advanced (security-related) scenarios for service accounts.
 *
 * <p>This test class focuses on edge-cases and security properties of the
 * client-credentials grant and token introspection, for example verifying
 * that tokens issued to a client that has been deleted and re-created with
 * the same {@code client_id} are no longer considered active.
 */
@KeycloakIntegrationTest
public class ServiceAccountAdvancedScenariosTest {

    @InjectRealm(config = ServiceAccountRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectClient(ref = ServiceAccountClientConfig.CLIENT_ID, config = ServiceAccountClientConfig.class)
    ManagedClient serviceAccountClient;

    @InjectClient(ref = PairwiseServiceAccountClientConfig.CLIENT_ID, config = PairwiseServiceAccountClientConfig.class)
    ManagedClient pairwiseServiceAccountClient;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verify that an access token obtained via the client-credentials grant
     * becomes inactive at the introspection endpoint after the issuing client
     * is deleted and a new client is created with the same {@code client_id}.
     *
     * <ol>
     *   <li>Obtain an access token via the client-credentials grant.</li>
     *   <li>Confirm the token is active via introspection.</li>
     *   <li>Delete the client, then re-create it with the same {@code client_id}.</li>
     *   <li>Confirm the original token is no longer active via introspection.</li>
     * </ol>
     */
    @Test
    public void tokenInvalidAfterClientRecreated() throws IOException {
        assertTokenInvalidAfterClientRecreated(
                serviceAccountClient,
                ClientConfigBuilder.create()
                        .clientId(serviceAccountClient.getClientId())
                        .secret(serviceAccountClient.getSecret())
                        .serviceAccountsEnabled(true)
        );
    }

    /**
     * Same scenario as {@link #tokenInvalidAfterClientRecreated()} but for a
     * client that uses the {@code SHA256} pairwise subject mapper.  The pairwise
     * mapper replaces the {@code sub} claim with a client-specific, hashed
     * identifier, so this test also verifies that tokens carrying a pairwise
     * subject are correctly invalidated after client recreation.
     */
    @Test
    public void pairwiseTokenInvalidAfterClientRecreated() throws IOException {
        assertTokenInvalidAfterClientRecreated(
                pairwiseServiceAccountClient,
                ClientConfigBuilder.create()
                        .clientId(pairwiseServiceAccountClient.getClientId())
                        .secret(pairwiseServiceAccountClient.getSecret())
                        .serviceAccountsEnabled(true)
                        .redirectUris(PairwiseServiceAccountClientConfig.REDIRECT_URI)
                        .protocolMappers(List.of(SHA256PairwiseSubMapper.createPairwiseMapper(null, null)))
        );
    }

    // -------------------------------------------------------------------------
    // Shared assertion helper
    // -------------------------------------------------------------------------

    /**
     * Runs the four-step "token-invalid-after-client-recreated" scenario for
     * any service-account client.
     *
     * @param managedClient          the managed client whose credentials are used for the grant and introspection
     * @param recreatedClientBuilder a {@link ClientConfigBuilder} describing the replacement client
     *                               (no audience mapper needed — introspection only needs to
     *                               return {@code active: false} for the old token)
     */
    private void assertTokenInvalidAfterClientRecreated(ManagedClient managedClient,
            ClientConfigBuilder recreatedClientBuilder) throws IOException {

        String clientId = managedClient.getClientId();
        String clientSecret = managedClient.getSecret();

        // Step 1 – obtain access token via client-credentials grant
        AccessTokenResponse tokenResponse = oauth.client(clientId, clientSecret)
                .doClientCredentialsGrantAccessTokenRequest();
        assertTrue(tokenResponse.isSuccess(),
                "client-credentials grant should succeed; error=" + tokenResponse.getError()
                        + " description=" + tokenResponse.getErrorDescription());
        String accessToken = tokenResponse.getAccessToken();
        assertNotNull(accessToken, "access token must not be null");

        // Step 2 – introspect: token must be active
        IntrospectionResponse firstIntrospection = oauth.doIntrospectionAccessTokenRequest(accessToken);
        assertTrue(firstIntrospection.isSuccess(), "introspection request should succeed");
        TokenMetadataRepresentation firstMetadata = firstIntrospection.asTokenMetadata();
        assertTrue(firstMetadata.isActive(),
                "token should be active immediately after issuance");

        // Step 3 – delete the client and re-create it with the same client_id
        managedClient.admin().remove();

        try (Response resp = realm.admin().clients().create(recreatedClientBuilder.build())) {
            assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus(),
                    "re-creating client should return HTTP 201");
            String newClientUuid = ApiUtil.getCreatedId(resp);
            // Register cleanup so the freshly created client is removed after the test
            realm.cleanup().add(r -> r.clients().get(newClientUuid).remove());
        }

        // Step 4 – introspect: the original token must now be inactive because
        // the client that issued it no longer exists (a new one took its place)
        IntrospectionResponse secondIntrospection = oauth.doIntrospectionAccessTokenRequest(accessToken);
        assertTrue(secondIntrospection.isSuccess(), "introspection request should succeed");
        TokenMetadataRepresentation secondMetadata = secondIntrospection.asTokenMetadata();
        assertFalse(secondMetadata.isActive(),
                "token issued by the deleted client should no longer be active after client recreation");
    }

    // -------------------------------------------------------------------------
    // Realm / client configuration
    // -------------------------------------------------------------------------

    /**
     * Realm configuration: a plain default realm; both service-account clients
     * are registered via their respective {@code @InjectOAuthClient} entries.
     */
    public static class ServiceAccountRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm;
        }
    }

    /**
     * Client configuration: a confidential service-account client with an
     * audience mapper that adds the client's own {@code client_id} to the
     * access-token {@code aud} claim so that the introspection endpoint accepts
     * the token.
     */
    public static class ServiceAccountClientConfig implements ClientConfig {

        private static final String CLIENT_ID = "service-account-client";
        private static final String CLIENT_SECRET = "service-account-secret";

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
            audienceMapper.setName("audience-" + CLIENT_ID);
            audienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

            Map<String, String> config = new HashMap<>();
            config.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, CLIENT_ID);
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            audienceMapper.setConfig(config);

            return client
                    .clientId(CLIENT_ID)
                    .secret(CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .protocolMappers(Collections.singletonList(audienceMapper));
        }
    }

    /**
     * Client configuration for the pairwise variant: a confidential
     * service-account client with both an audience mapper and a
     * {@link SHA256PairwiseSubMapper} so the access-token {@code sub} claim
     * carries a pairwise (hashed) subject identifier.
     *
     * <p>Because {@link InjectClient} registers the
     * client with exactly the {@code clientId} set here (no suffix is appended),
     * the audience mapper value can be hardcoded in this config class.
     *
     * <p>A redirect URI is required by the pairwise-sub mapper validator even
     * though the client-credentials grant never performs a redirect.
     */
    public static class PairwiseServiceAccountClientConfig implements ClientConfig {

        private static final String CLIENT_ID = "pairwise-service-account-client";
        private static final String CLIENT_SECRET = "pairwise-service-account-secret";
        private static final String REDIRECT_URI = "https://localhost/pairwise-callback";

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
            audienceMapper.setName("audience-" + CLIENT_ID);
            audienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

            Map<String, String> config = new HashMap<>();
            config.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, CLIENT_ID);
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            audienceMapper.setConfig(config);

            // null sectorIdentifierUri → pairwise validator falls back to checking the
            // redirect URI set below (single host satisfies the constraint).
            ProtocolMapperRepresentation pairwiseMapper =
                    SHA256PairwiseSubMapper.createPairwiseMapper(null, null);

            return client
                    .clientId(CLIENT_ID)
                    .secret(CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .redirectUris(REDIRECT_URI)
                    .protocolMappers(List.of(audienceMapper, pairwiseMapper));
        }
    }
}
