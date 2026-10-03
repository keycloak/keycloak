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
import java.util.Map;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the roles that the Hardcoded Role protocol mapper adds to a token for a composite role.
 */
@KeycloakIntegrationTest
public class HardcodedRoleMapperTest {

    @InjectRealm(config = HardcodedRoleRealm.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void testCompositeRoleNotExpandedByDefault() {
        AccessToken token = accessToken("default-client");

        assertThat(token.getRealmAccess().getRoles(), hasItem("composite-role"));
        assertThat(token.getRealmAccess().getRoles(), not(hasItem("inner-realm-role")));
        assertThat(token.getRealmAccess().getRoles(), not(hasItem("nested-realm-role")));
        assertThat(token.getResourceAccess(), not(hasKey("role-service")));
    }

    @Test
    public void testCompositeRealmRoleExpanded() {
        AccessToken token = accessToken("expanding-client");

        // The roles inside the composite role are added too, including the ones inside a nested composite role
        assertThat(token.getRealmAccess().getRoles(), hasItems("composite-role", "inner-realm-role", "nested-realm-role"));
        assertThat(token.getResourceAccess("role-service").getRoles(), containsInAnyOrder("inner-client-role"));
    }

    @Test
    public void testCompositeClientRoleExpanded() {
        AccessToken token = accessToken("expanding-client-role-client");

        assertThat(token.getResourceAccess("role-service").getRoles(), containsInAnyOrder("composite-client-role", "inner-client-role"));
        assertThat(token.getRealmAccess().getRoles(), hasItem("nested-realm-role"));
        assertThat(token.getRealmAccess().getRoles(), not(hasItem("composite-role")));
    }

    @Test
    public void testUnknownRoleWithExpansion() {
        AccessToken token = accessToken("unknown-role-client");

        // A name that is not a role of the realm is added as it is
        assertThat(token.getRealmAccess().getRoles(), hasItem("no-such-role"));
    }

    private AccessToken accessToken(String clientId) {
        oauth.client(clientId, "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("john", "password");
        assertEquals(200, response.getStatusCode());
        return oauth.verifyToken(response.getAccessToken());
    }

    public static class HardcodedRoleRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create().clientId("role-service").bearerOnly(true));

            realm.realmRoles(
                    RoleBuilder.create("nested-realm-role"),
                    RoleBuilder.create("inner-realm-role").realmComposite("nested-realm-role"),
                    RoleBuilder.create("composite-role")
                            .realmComposite("inner-realm-role")
                            .clientComposite("role-service", "inner-client-role"));
            realm.clientRoles("role-service",
                    RoleBuilder.create("inner-client-role"),
                    RoleBuilder.create("composite-client-role")
                            .realmComposite("nested-realm-role")
                            .clientComposite("role-service", "inner-client-role"));

            realm.clients(
                    createClient("default-client", createHardcodedRoleMapper("composite-role", null)),
                    createClient("expanding-client", createHardcodedRoleMapper("composite-role", true)),
                    createClient("expanding-client-role-client", createHardcodedRoleMapper("role-service.composite-client-role", true)),
                    createClient("unknown-role-client", createHardcodedRoleMapper("no-such-role", true)));

            // The user is deliberately not granted any of the roles, so that they can only come from the mapper
            realm.users(UserBuilder.create().username("john")
                    .name("John", "Doe")
                    .email("john@email.cz")
                    .password("password"));

            return realm;
        }

        private ClientBuilder createClient(String clientId, ProtocolMapperRepresentation mapper) {
            return ClientBuilder.create().clientId(clientId)
                    .secret("secret")
                    .directAccessGrantsEnabled(true)
                    .fullScopeEnabled(true)
                    .protocolMappers(mapper);
        }

        private ProtocolMapperRepresentation createHardcodedRoleMapper(String role, Boolean expandCompositeRoles) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("hardcoded-role");
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            mapper.setProtocolMapper(HardcodedRole.PROVIDER_ID);

            Map<String, String> config = new HashMap<>();
            config.put(HardcodedRole.ROLE_CONFIG, role);
            config.put("access.token.claim", "true");
            if (expandCompositeRoles != null) {
                config.put(HardcodedRole.EXPAND_COMPOSITE_ROLES_CONFIG, expandCompositeRoles.toString());
            }
            mapper.setConfig(config);
            return mapper;
        }
    }
}
