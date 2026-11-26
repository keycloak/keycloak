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

package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class ClientResourceTypeFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @BeforeEach
    public void onBeforeEach() {
        for (int i = 0; i < 50; i++) {
            ClientRepresentation client = new ClientRepresentation();

            client.setClientId("client-" + i);
            client.setName(client.getClientId());
            client.setEnabled(true);
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            client.setPublicClient(false);
            client.setSecret("secret");
            client.setAttributes(Map.of("saml.artifact.binding.identifier", "value"));

            realm.admin().clients().create(client).close();
        }
    }

    @Test
    @DatabaseTest
    public void testViewAllClientsUsingUserPolicy() {
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertFalse(search.isEmpty());
        assertEquals(59, search.size());
    }

    @Test
    @DatabaseTest
    public void testViewSpecificClientsUsingUserPolicy() {
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).clients().findAll("client-", true, true, null, null);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    @DatabaseTest
    public void testViewClientsByAttributeUsingUserPolicy() {
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).clients().query("saml.artifact.binding.identifier:\"value\"");
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    @DatabaseTest
    public void testDeniedResourcesPrecedenceOverGrantedResources() {
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll("client-", true, true, null, null);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> notAllowedClients = search.stream()
                .filter((g) -> Set.of("client-0", "client-15", "client-30", "client-45").contains(g.getClientId()))
                .map(ClientRepresentation::getId)
                .collect(Collectors.toSet());
        assertFalse(notAllowedClients.isEmpty());
        createPermission(adminPermissionsClient, notAllowedClients, CLIENTS_RESOURCE_TYPE, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).clients().findAll("client-", true, true, null, null);
        assertFalse(search.isEmpty());
        assertTrue(search.stream().map(ClientRepresentation::getId).noneMatch(notAllowedClients::contains));
    }

    @Test
    public void testSearchByClientId() {
        String expectedClientId = "client-0";
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findByClientId(expectedClientId);
        assertTrue(search.isEmpty());

        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, expectedClientId, CLIENTS_RESOURCE_TYPE, Set.of(VIEW), allowPolicy);
        search = realmAdminClient.realm(realm.getName()).clients().findByClientId(expectedClientId);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
        assertEquals(search.get(0).getClientId(), expectedClientId);
    }

    @Test
    public void testClientSessionStatsRespectsClientVisibility() {
        UserRepresentation admin = realm.admin().users().search("myadmin").get(0);
        String realmManagementId = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewRealm = realm.admin().clients().get(realmManagementId).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        realm.admin().users().get(admin.getId()).roles().clientLevel(realmManagementId).add(List.of(viewRealm));

        UserRepresentation user = createUser("session-stats-user", "password");
        try (Keycloak visibleSession = createSessionClient("visible-client", user);
             Keycloak hiddenSession = createSessionClient("hidden-client", user);
             Keycloak hiddenOfflineSession = createSessionClient("hidden-offline-client", user, OAuth2Constants.OFFLINE_ACCESS)) {
            visibleSession.tokenManager().getAccessToken();
            hiddenSession.tokenManager().getAccessToken();
            hiddenOfflineSession.tokenManager().getAccessToken();

            String visibleClientId = realm.admin().clients().findByClientId("visible-client").get(0).getId();
            String hiddenClientId = realm.admin().clients().findByClientId("hidden-client").get(0).getId();
            String hiddenOfflineClientId = realm.admin().clients().findByClientId("hidden-offline-client").get(0).getId();
            assertThat(realm.admin().clients().get(hiddenOfflineClientId).getApplicationSessionCount().get("count"), is(0));

            List<Map<String, String>> allStats = realm.admin().getClientSessionStats();
            Map<String, String> visibleStats = Map.of("id", visibleClientId, "clientId", "visible-client",
                    "active", "1", "offline", "0");
            Map<String, String> hiddenStats = Map.of("id", hiddenClientId, "clientId", "hidden-client",
                    "active", "1", "offline", "0");
            Map<String, String> hiddenOfflineStats = Map.of("id", hiddenOfflineClientId, "clientId", "hidden-offline-client",
                    "active", "0", "offline", "1");
            assertThat(allStats, containsInAnyOrder(visibleStats, hiddenStats, hiddenOfflineStats));

            UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Visible client policy", admin.getId());
            ScopePermissionRepresentation permission = createPermission(adminPermissionsClient, Set.of(visibleClientId), CLIENTS_RESOURCE_TYPE, Set.of(MANAGE), policy);
            ClientRepresentation managedClient = new ClientRepresentation();
            managedClient.setName("Managed client");
            realmAdminClient.realm(realm.getName()).clients().get(visibleClientId).update(managedClient);
            assertThat(realm.admin().clients().get(visibleClientId).toRepresentation().getName(), is("Managed client"));
            assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).clients().get(visibleClientId).toRepresentation());
            assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).clients().get(visibleClientId).getApplicationSessionCount());
            assertThat(realmAdminClient.realm(realm.getName()).getClientSessionStats(), empty());

            permission.setScopes(Set.of(VIEW));
            getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).update(permission);
            assertThat(realmAdminClient.realm(realm.getName()).clients().get(visibleClientId).toRepresentation().getClientId(), is("visible-client"));
            assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).clients().get(hiddenClientId).toRepresentation());
            assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).clients().get(hiddenOfflineClientId).toRepresentation());
            assertThat(realmAdminClient.realm(realm.getName()).getClientSessionStats(), contains(visibleStats));
        }
    }

    private Keycloak createSessionClient(String clientId, UserRepresentation user, String... scopes) {
        try (Response response = realm.admin().clients().create(ClientBuilder.create(clientId)
                .secret("secret")
                .directAccessGrantsEnabled(true)
                .build())) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        return KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBaseUrl().toString())
                .realm(realm.getName())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(clientId)
                .clientSecret("secret")
                .username(user.getUsername())
                .password("password")
                .scope(scopes.length == 0 ? null : String.join(" ", scopes))
                .build();
    }
}
