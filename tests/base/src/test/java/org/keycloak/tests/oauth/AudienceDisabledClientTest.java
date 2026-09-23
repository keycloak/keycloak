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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a client that is disabled, or that no longer exists, is never added to the aud claim of a token.
 */
@KeycloakIntegrationTest
public class AudienceDisabledClientTest {

    @InjectRealm(config = AudienceRealm.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void testAudienceResolveIgnoresDisabledClient() {
        AccessToken token = accessToken(login());
        assertThat(audiences(token), hasItem("resolved-service"));
        assertThat(token.getResourceAccess(), hasKey("resolved-service"));

        disableClientWithCleanup("resolved-service");

        token = accessToken(login());
        assertThat(audiences(token), not(hasItem("resolved-service")));
        assertThat(token.getResourceAccess(), not(hasKey("resolved-service")));
    }

    @Test
    public void testHardcodedAudienceMapperIgnoresDisabledClient() {
        AccessToken token = accessToken(login());
        assertThat(audiences(token), hasItem("hardcoded-service"));
        assertThat(audiences(token), hasItem("custom-audience"));

        disableClientWithCleanup("hardcoded-service");

        token = accessToken(login());
        assertThat(audiences(token), not(hasItem("hardcoded-service")));
        // A custom audience is not affected
        assertThat(audiences(token), hasItem("custom-audience"));
    }

    @Test
    public void testHardcodedAudienceMapperIgnoresRemovedClient() {
        AccessToken token = accessToken(login());
        assertThat(audiences(token), hasItem("hardcoded-service"));

        removeClientWithCleanup("hardcoded-service");

        token = accessToken(login());
        assertThat(audiences(token), not(hasItem("hardcoded-service")));
        // A custom audience is not affected
        assertThat(audiences(token), hasItem("custom-audience"));
    }

    @Test
    public void testHardcodedRoleMapperIgnoresDisabledClient() {
        AccessToken token = accessToken(login());
        assertThat(audiences(token), hasItem("role-service"));
        assertThat(token.getResourceAccess(), hasKey("role-service"));

        disableClientWithCleanup("role-service");

        // A role added by a mapper does not bring the disabled client back into the token either
        token = accessToken(login());
        assertThat(audiences(token), not(hasItem("role-service")));
        assertThat(token.getResourceAccess(), not(hasKey("role-service")));
    }

    @Test
    public void testHardcodedRoleMapperIgnoresRemovedClient() {
        AccessToken token = accessToken(login());
        assertThat(audiences(token), hasItem("role-service"));
        assertThat(token.getResourceAccess(), hasKey("role-service"));

        removeClientWithCleanup("role-service");

        token = accessToken(login());
        assertThat(audiences(token), not(hasItem("role-service")));
        assertThat(token.getResourceAccess(), not(hasKey("role-service")));
    }

    @Test
    public void testRefreshIgnoresDisabledClient() {
        AccessTokenResponse response = login();
        AccessToken token = accessToken(response);
        assertThat(audiences(token), hasItem("resolved-service"));
        assertThat(audiences(token), hasItem("hardcoded-service"));

        disableClientWithCleanup("resolved-service");
        disableClientWithCleanup("hardcoded-service");

        // The refresh keeps succeeding, but the disabled clients are no longer part of the newly issued token
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        token = accessToken(response);
        assertThat(audiences(token), not(hasItem("resolved-service")));
        assertThat(token.getResourceAccess(), not(hasKey("resolved-service")));
        assertThat(audiences(token), not(hasItem("hardcoded-service")));
        assertThat(audiences(token), hasItem("custom-audience"));

        // The original audience copied into the rotated refresh token is cleaned up as well
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(List.of(refreshToken.getOriginalAudience()), not(hasItem("resolved-service")));
        assertThat(List.of(refreshToken.getOriginalAudience()), not(hasItem("hardcoded-service")));
    }

    private AccessTokenResponse login() {
        oauth.client("frontend-client", "secret");
        return oauth.doPasswordGrantRequest("john", "password");
    }

    private AccessToken accessToken(AccessTokenResponse response) {
        assertEquals(200, response.getStatusCode());
        return oauth.verifyToken(response.getAccessToken());
    }

    private List<String> audiences(AccessToken token) {
        return token.getAudience() == null ? List.of() : List.of(token.getAudience());
    }

    private void disableClientWithCleanup(String clientId) {
        ClientRepresentation client = realm.admin().clients().findByClientId(clientId).get(0);
        client.setEnabled(false);
        realm.admin().clients().get(client.getId()).update(client);

        realm.cleanup().add(r -> {
            ClientRepresentation toEnable = r.clients().findByClientId(clientId).get(0);
            toEnable.setEnabled(true);
            r.clients().get(toEnable.getId()).update(toEnable);
        });
    }

    private void removeClientWithCleanup(String clientId) {
        ClientRepresentation client = realm.admin().clients().findByClientId(clientId).get(0);
        realm.admin().clients().get(client.getId()).remove();

        realm.cleanup().add(r -> r.clients().create(client).close());
    }

    public static class AudienceRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            // Service client resolved as an audience through its client roles
            realm.addClient("resolved-service").bearerOnly(true);

            // Service client added as an audience by a hardcoded Audience protocol mapper
            realm.addClient("hardcoded-service").bearerOnly(true);

            // Service client whose role is added to the token by a Hardcoded Role protocol mapper
            realm.addClient("role-service").bearerOnly(true);

            realm.addClient("frontend-client")
                    .secret("secret")
                    .directAccessGrantsEnabled(true)
                    .fullScopeEnabled(true)
                    .protocolMappers(List.of(
                            createAudienceMapper("hardcoded-service-audience", "hardcoded-service", null),
                            createAudienceMapper("custom-audience", null, "custom-audience"),
                            createHardcodedRoleMapper("role-service-hardcoded-role", "role-service.role-service-role")));

            realm.clientRoles("resolved-service", "resolved-service-role");
            realm.clientRoles("role-service", "role-service-role");

            // The user is deliberately not granted role-service-role, so that the client can only be resolved as an
            // audience through the Hardcoded Role protocol mapper
            realm.addUser("john")
                    .name("John", "Doe")
                    .email("john@email.cz")
                    .password("password")
                    .clientRoles("resolved-service", "resolved-service-role");

            return realm;
        }

        private ProtocolMapperRepresentation createHardcodedRoleMapper(String name, String role) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            mapper.setProtocolMapper(HardcodedRole.PROVIDER_ID);
            mapper.setConfig(Map.of(HardcodedRole.ROLE_CONFIG, role, "access.token.claim", "true"));
            return mapper;
        }

        private ProtocolMapperRepresentation createAudienceMapper(String name, String clientAudience, String customAudience) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            mapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

            Map<String, String> config = new HashMap<>();
            if (clientAudience != null) {
                config.put(AudienceProtocolMapper.INCLUDED_CLIENT_AUDIENCE, clientAudience);
            }
            if (customAudience != null) {
                config.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, customAudience);
            }
            config.put("access.token.claim", "true");
            mapper.setConfig(config);
            return mapper;
        }
    }
}
