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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a partial client update re-validates the URI fields that are stored on the client and
 * omitted from the update payload, both for the {@code secure-client-uris} and the
 * {@code secure-client-uris-pattern} executors. A URI field that is not compliant with the policy is
 * seeded on the client before the policy is enabled, so the only way the update can be rejected is by
 * validating the effective client state.
 */
@KeycloakIntegrationTest
public class SecureClientUrisPartialUpdateTest extends AbstractClientPoliciesTest {

    private static final String INSECURE_JWKS_URL = "http://insecure.example.com/jwks";
    private static final String SECURE_JWKS_URL = "https://secure.example.com/jwks";
    private static final String INSECURE_ADMIN_URL = "http://insecure.example.com/admin";
    private static final String INSECURE_REDIRECT_URL = "http://insecure.example.com/callback";
    private static final String SECURE_REDIRECT_URL = "https://secure.example.com/callback";
    private static final String UNTRUSTED_JWKS_URL = "http://untrusted.example.com/jwks";
    private static final String TRUSTED_JWKS_URL = "https://trusted.example.com/jwks";
    private static final String SAFE_PATTERN = "^https://trusted.example.com.*";

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void testPartialUpdateRevalidatesStoredJwksUri() throws Exception {
        String clientName = generateSuffixedName("partial-update-jwks");
        String cId = createClientByAdmin(realm, clientName, OIDCLoginProtocol.LOGIN_PROTOCOL, (ClientRepresentation c) -> {
            c.setRedirectUris(Collections.singletonList(SECURE_REDIRECT_URL));
            c.getAttributes().put(OIDCConfigAttributes.JWKS_URL, INSECURE_JWKS_URL);
        });

        setupSecureClientUrisPolicy();

        // the update does not carry the attributes map, so the stored jwks URL has to be re-validated
        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientPartially(cId, (ClientRepresentation c) -> c.setDescription("updated description")));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
        Assertions.assertEquals("Invalid jwksUri", cpe.getErrorDetail());
    }

    @Test
    public void testPartialUpdateRevalidatesStoredAdminUrl() throws Exception {
        String clientName = generateSuffixedName("partial-update-adminurl");
        String cId = createClientByAdmin(realm, clientName, OIDCLoginProtocol.LOGIN_PROTOCOL, (ClientRepresentation c) -> {
            c.setRedirectUris(Collections.singletonList(SECURE_REDIRECT_URL));
            c.setAdminUrl(INSECURE_ADMIN_URL);
        });

        setupSecureClientUrisPolicy();

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientPartially(cId, (ClientRepresentation c) -> c.setDescription("updated description")));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
        Assertions.assertEquals("Invalid adminUrl", cpe.getErrorDetail());
    }

    @Test
    public void testPartialUpdateRevalidatesStoredRedirectUris() throws Exception {
        String clientName = generateSuffixedName("partial-update-redirecturis");
        String cId = createClientByAdmin(realm, clientName, OIDCLoginProtocol.LOGIN_PROTOCOL,
                (ClientRepresentation c) -> c.setRedirectUris(Collections.singletonList(INSECURE_REDIRECT_URL)));

        setupSecureClientUrisPolicy();

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientPartially(cId, (ClientRepresentation c) -> c.setDescription("updated description")));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
        Assertions.assertEquals("Invalid redirectUris", cpe.getErrorDetail());
    }

    @Test
    public void testPartialUpdateReplacingStoredJwksUriIsAccepted() throws Exception {
        String clientName = generateSuffixedName("partial-update-jwks-fix");
        String cId = createClientByAdmin(realm, clientName, OIDCLoginProtocol.LOGIN_PROTOCOL, (ClientRepresentation c) -> {
            c.setRedirectUris(Collections.singletonList(SECURE_REDIRECT_URL));
            c.getAttributes().put(OIDCConfigAttributes.JWKS_URL, INSECURE_JWKS_URL);
        });

        setupSecureClientUrisPolicy();

        // the update fixes the stored jwks URL, so it has to be accepted even if the rest of the
        // attributes map is omitted from the payload
        updateClientPartially(cId, (ClientRepresentation c) ->
                c.setAttributes(Collections.singletonMap(OIDCConfigAttributes.JWKS_URL, SECURE_JWKS_URL)));

        ClientRepresentation stored = findByClientIdByAdmin(realm, clientName);
        Assertions.assertEquals(SECURE_JWKS_URL, stored.getAttributes().get(OIDCConfigAttributes.JWKS_URL));
    }

    @Test
    public void testPatternPartialUpdateRevalidatesStoredJwksUri() throws Exception {
        String clientName = generateSuffixedName("partial-update-pattern-jwks");
        String cId = createClientByAdmin(realm, clientName, OIDCLoginProtocol.LOGIN_PROTOCOL,
                (ClientRepresentation c) -> c.getAttributes().put(OIDCConfigAttributes.JWKS_URL, UNTRUSTED_JWKS_URL));

        setupSecureClientUrisPatternPolicy();

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientPartially(cId, (ClientRepresentation c) -> c.setDescription("updated description")));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
        Assertions.assertEquals("Invalid jwksUri", cpe.getErrorDetail());
    }

    @Test
    public void testPatternPartialUpdateReplacingStoredJwksUriIsAccepted() throws Exception {
        String clientName = generateSuffixedName("partial-update-pattern-jwks-fix");
        String cId = createClientByAdmin(realm, clientName, OIDCLoginProtocol.LOGIN_PROTOCOL,
                (ClientRepresentation c) -> c.getAttributes().put(OIDCConfigAttributes.JWKS_URL, UNTRUSTED_JWKS_URL));

        setupSecureClientUrisPatternPolicy();

        updateClientPartially(cId, (ClientRepresentation c) ->
                c.setAttributes(Collections.singletonMap(OIDCConfigAttributes.JWKS_URL, TRUSTED_JWKS_URL)));

        ClientRepresentation stored = findByClientIdByAdmin(realm, clientName);
        Assertions.assertEquals(TRUSTED_JWKS_URL, stored.getAttributes().get(OIDCConfigAttributes.JWKS_URL));
    }

    /**
     * Sends an update that only carries the fields set by {@code op}, leaving the rest of the client
     * representation absent from the request, like a partial update issued by the admin REST API.
     */
    private void updateClientPartially(String cId, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientRepresentation partial = new ClientRepresentation();
        op.accept(partial);
        try {
            realm.admin().clients().get(cId).update(partial);
        } catch (BadRequestException bre) {
            try (Response resp = bre.getResponse()) {
                Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
                Map<String, String> responseJson = JsonSerialization.readValue(resp.readEntity(String.class), Map.class);
                throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR), responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
            } catch (IOException e) {
                Assertions.fail();
            }
        }
    }

    private void setupSecureClientUrisPolicy() throws Exception {
        setupPolicy(realm, SecureClientUrisExecutorFactory.PROVIDER_ID, new SecureClientUrisExecutor.Configuration(),
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }

    private void setupSecureClientUrisPatternPolicy() throws Exception {
        SecureClientUrisPatternExecutor.Configuration config = new SecureClientUrisPatternExecutor.Configuration();
        config.setAllowedPatterns(List.of(SAFE_PATTERN));
        config.setClientUriFields(List.of("jwksUri"));
        setupPolicy(realm, SecureClientUrisPatternExecutorFactory.PROVIDER_ID, config,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }
}
