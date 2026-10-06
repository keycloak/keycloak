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
package org.keycloak.tests.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the {@code composite} and {@code available} query parameters of the role mapping, scope mapping and
 * composite role endpoints, which the Admin Console uses to show effective roles and to search for client roles
 * across all clients when assigning roles.
 */
@KeycloakIntegrationTest
public class RoleMappingSearchTest {

    private static final String CLIENT_A = "client-a";
    private static final String CLIENT_B = "client-b";
    private static final String OTHER_CLIENT = "other-client";

    @InjectRealm(config = RoleMappingSearchRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectAdminClient(ref = "userManager", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "user-manager")
    Keycloak userManager;

    @InjectAdminClient(ref = "userViewer", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "user-viewer")
    Keycloak userViewer;

    @InjectAdminClient(ref = "clientManager", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "client-manager")
    Keycloak clientManager;

    @InjectAdminClient(ref = "clientViewer", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "client-viewer")
    Keycloak clientViewer;

    @InjectAdminClient(ref = "clientQuerier", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "client-querier")
    Keycloak clientQuerier;

    @InjectAdminClient(ref = "realmViewer", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "realm-viewer")
    Keycloak realmViewer;

    private String userId;
    private String groupId;
    private String clientAId;
    private String clientBId;
    private String otherClientId;

    @BeforeEach
    public void lookupIds() {
        RealmResource realm = managedRealm.admin();
        userId = realm.users().search("target", 0, 1).get(0).getId();
        groupId = realm.groups().groups("target-group", 0, 1).get(0).getId();
        clientAId = AdminApiUtil.findClientByClientId(realm, CLIENT_A).toRepresentation().getId();
        clientBId = AdminApiUtil.findClientByClientId(realm, CLIENT_B).toRepresentation().getId();
        otherClientId = AdminApiUtil.findClientByClientId(realm, OTHER_CLIENT).toRepresentation().getId();
    }

    @Test
    public void availableClientRolesForUserExcludeMappedRolesAndSupportPaging() {
        RealmResource realm = managedRealm.admin();
        realm.users().get(userId).roles().clientLevel(clientAId).add(List.of(clientRole(clientAId, "a-role-1")));

        MappingsRepresentation available = realm.users().get(userId).roles().getAllAvailable(null, null, null);
        assertThat(available.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                hasItems("realm-role-1", "realm-role-2", "composite-role"));
        assertThat(roleNames(available, CLIENT_A), containsInAnyOrder("a-role-2"));
        assertThat(roleNames(available, CLIENT_B), containsInAnyOrder("b-role-1", "b-role-2"));
        assertThat(roleNames(available, OTHER_CLIENT), containsInAnyOrder("other-role"));

        // search matches the role name or the client id
        available = realm.users().get(userId).roles().getAllAvailable("b-role", null, null);
        assertThat(available.getClientMappings().keySet(), contains(CLIENT_B));
        assertThat(roleNames(available, CLIENT_B), containsInAnyOrder("b-role-1", "b-role-2"));

        available = realm.users().get(userId).roles().getAllAvailable(OTHER_CLIENT, null, null);
        assertThat(available.getClientMappings().keySet(), contains(OTHER_CLIENT));

        // paging over all client roles, regardless of the client they belong to
        List<String> firstPage = allClientRoleNames(realm.users().get(userId).roles().getAllAvailable(null, 0, 2));
        List<String> secondPage = allClientRoleNames(realm.users().get(userId).roles().getAllAvailable(null, 2, 2));
        assertThat(firstPage, hasSize(2));
        assertThat(secondPage, hasSize(2));
        assertThat(Set.copyOf(firstPage).stream().filter(secondPage::contains).collect(Collectors.toList()), empty());
    }

    @Test
    public void availableClientRolesForGroupExcludeMappedRoles() {
        RealmResource realm = managedRealm.admin();
        realm.groups().group(groupId).roles().clientLevel(clientBId).add(List.of(clientRole(clientBId, "b-role-1")));

        MappingsRepresentation available = realm.groups().group(groupId).roles().getAllAvailable(null, null, null);
        assertThat(roleNames(available, CLIENT_B), containsInAnyOrder("b-role-2"));
        assertThat(roleNames(available, CLIENT_A), containsInAnyOrder("a-role-1", "a-role-2"));
    }

    @Test
    public void manageUsersIsEnoughToSearchClientRolesForUsers() {
        // the user manager cannot list clients...
        assertThrows(ForbiddenException.class, () -> userManager.realm(managedRealm.getName()).clients().findAll());

        // ...but can still search the client roles that can be mapped to a user
        MappingsRepresentation available = userManager.realm(managedRealm.getName()).users().get(userId).roles().getAllAvailable(null, null, null);
        assertThat(roleNames(available, CLIENT_A), containsInAnyOrder("a-role-1", "a-role-2"));
        assertThat(roleNames(available, CLIENT_B), containsInAnyOrder("b-role-1", "b-role-2"));

        available = userManager.realm(managedRealm.getName()).groups().group(groupId).roles().getAllAvailable("a-role", null, null);
        assertThat(available.getClientMappings().keySet(), contains(CLIENT_A));
    }

    @Test
    public void viewOnlyAdminGetsNoAvailableRolesButCanListMappings() {
        RealmResource realm = managedRealm.admin();
        realm.users().get(userId).roles().clientLevel(clientAId).add(List.of(clientRole(clientAId, "a-role-1")));

        MappingsRepresentation available = userViewer.realm(managedRealm.getName()).users().get(userId).roles().getAllAvailable(null, null, null);
        assertThat(available.getRealmMappings(), nullValue());
        assertThat(available.getClientMappings(), nullValue());

        MappingsRepresentation mappings = userViewer.realm(managedRealm.getName()).users().get(userId).roles().getAll();
        assertThat(roleNames(mappings, CLIENT_A), containsInAnyOrder("a-role-1"));
    }

    @Test
    public void compositeReturnsEffectiveRolesForUsersIncludingGroupsAndComposites() {
        RealmResource realm = managedRealm.admin();
        realm.users().get(userId).roles().realmLevel().add(List.of(realmRole("composite-role")));
        realm.groups().group(groupId).roles().clientLevel(clientBId).add(List.of(clientRole(clientBId, "b-role-1")));
        realm.users().get(userId).joinGroup(groupId);

        MappingsRepresentation direct = realm.users().get(userId).roles().getAll();
        assertThat(direct.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                hasItem("composite-role"));
        assertThat(direct.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                not(hasItem("realm-role-1")));
        assertThat(direct.getClientMappings(), nullValue());

        MappingsRepresentation effective = realm.users().get(userId).roles().getAllComposite();
        Set<String> realmRoles = effective.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(realmRoles, hasItems("composite-role", "realm-role-1"));
        // inherited from the group
        assertThat(roleNames(effective, CLIENT_B), hasItem("b-role-1"));
        // child of composite-role
        assertThat(roleNames(effective, CLIENT_A), hasItem("a-role-2"));
    }

    @Test
    public void scopeMappingsSupportCompositeAndAvailable() {
        RealmResource realm = managedRealm.admin();
        realm.clients().get(clientAId).getScopeMappings().realmLevel().add(List.of(realmRole("composite-role")));
        realm.clients().get(clientAId).getScopeMappings().clientLevel(clientBId).add(List.of(clientRole(clientBId, "b-role-1")));

        MappingsRepresentation effective = realm.clients().get(clientAId).getScopeMappings().getAllComposite();
        assertThat(effective.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                hasItems("composite-role", "realm-role-1"));
        assertThat(roleNames(effective, CLIENT_A), hasItem("a-role-2"));
        assertThat(roleNames(effective, CLIENT_B), hasItem("b-role-1"));

        MappingsRepresentation available = realm.clients().get(clientAId).getScopeMappings().getAllAvailable(null, null, null);
        // already mapped roles and the roles of the client itself are excluded
        assertThat(available.getClientMappings(), not(hasKey(CLIENT_A)));
        assertThat(roleNames(available, CLIENT_B), containsInAnyOrder("b-role-2"));
        assertThat(roleNames(available, OTHER_CLIENT), containsInAnyOrder("other-role"));
        assertThat(available.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                hasItems("realm-role-1", "realm-role-2"));
        assertThat(available.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                not(hasItem("composite-role")));

        // scope mappings require manage-clients, view-clients is not enough to get candidates
        available = clientViewer.realm(managedRealm.getName()).clients().get(clientAId).getScopeMappings().getAllAvailable(null, null, null);
        assertThat(available.getClientMappings(), nullValue());
        available = clientManager.realm(managedRealm.getName()).clients().get(clientAId).getScopeMappings().getAllAvailable("b-role", null, null);
        assertThat(roleNames(available, CLIENT_B), containsInAnyOrder("b-role-2"));
    }

    @Test
    public void roleCompositesSupportCompositeAndAvailable() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation composite = realmRole("composite-role");
        RoleRepresentation nested = realmRole("nested-composite-role");

        Set<String> direct = realm.rolesById().getRoleComposites(nested.getId()).stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(direct, containsInAnyOrder("composite-role"));

        MappingsRepresentation effective = realm.rolesById().getCompositeRoleComposites(nested.getId());
        assertThat(effective.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                containsInAnyOrder("composite-role", "realm-role-1"));
        assertThat(roleNames(effective, CLIENT_A), containsInAnyOrder("a-role-2"));

        MappingsRepresentation available = realm.rolesById().getAvailableRoleComposites(composite.getId(), null, null, null);
        assertThat(roleNames(available, CLIENT_A), containsInAnyOrder("a-role-1"));
        assertThat(roleNames(available, CLIENT_B), containsInAnyOrder("b-role-1", "b-role-2"));
        assertThat(roleNames(available, OTHER_CLIENT), hasItem("other-role"));
        Set<String> availableRealmRoles = available.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(availableRealmRoles, hasItem("realm-role-2"));
        // the role itself and its current children are not available
        assertThat(availableRealmRoles, not(hasItems("composite-role", "realm-role-1")));

        available = realm.rolesById().getAvailableRoleComposites(composite.getId(), CLIENT_B, 0, 1);
        List<String> page = allClientRoleNames(available);
        assertThat(page, hasSize(1));
        assertThat(page.get(0), anyOf(equalTo("b-role-1"), equalTo("b-role-2")));
    }

    @Test
    public void inheritedReturnsRolesObtainedThroughGroupsAndComposites() {
        RealmResource realm = managedRealm.admin();
        // composite-role (direct) brings realm-role-1 and a-role-2, b-role-1 is both direct and inherited from the group
        realm.users().get(userId).roles().realmLevel().add(List.of(realmRole("composite-role")));
        realm.users().get(userId).roles().clientLevel(clientBId).add(List.of(clientRole(clientBId, "b-role-1")));
        realm.groups().group(groupId).roles().clientLevel(clientBId).add(List.of(clientRole(clientBId, "b-role-1")));
        realm.users().get(userId).joinGroup(groupId);

        MappingsRepresentation inherited = realm.users().get(userId).roles().getAllInherited();
        assertThat(realmRoleNames(inherited), containsInAnyOrder("realm-role-1"));
        assertThat(roleNames(inherited, CLIENT_A), containsInAnyOrder("a-role-2"));
        assertThat(roleNames(inherited, CLIENT_B), containsInAnyOrder("b-role-1"));

        // a group inherits from its parents and from its composite roles, not from its direct roles
        GroupRepresentation subGroup = new GroupRepresentation();
        subGroup.setName("sub-group");
        String subGroupId;
        try (Response response = realm.groups().group(groupId).subGroup(subGroup)) {
            subGroupId = ApiUtil.getCreatedId(response);
        }
        realm.groups().group(subGroupId).roles().realmLevel().add(List.of(realmRole("composite-role")));

        inherited = realm.groups().group(subGroupId).roles().getAllInherited();
        assertThat(realmRoleNames(inherited), containsInAnyOrder("realm-role-1"));
        assertThat(roleNames(inherited, CLIENT_A), containsInAnyOrder("a-role-2"));
        assertThat(roleNames(inherited, CLIENT_B), containsInAnyOrder("b-role-1"));

        // scope mappings inherit from the composite roles in the scope
        realm.clients().get(clientAId).getScopeMappings().realmLevel().add(List.of(realmRole("composite-role"), realmRole("realm-role-1")));
        inherited = realm.clients().get(clientAId).getScopeMappings().getAllInherited();
        assertThat(realmRoleNames(inherited), containsInAnyOrder("realm-role-1"));
        assertThat(roleNames(inherited, CLIENT_A), containsInAnyOrder("a-role-2"));
        assertThat(inherited.getClientMappings(), not(hasKey(CLIENT_B)));

        // composite roles inherit from their composite children
        inherited = realm.rolesById().getInheritedRoleComposites(realmRole("nested-composite-role").getId());
        assertThat(realmRoleNames(inherited), containsInAnyOrder("realm-role-1"));
        assertThat(roleNames(inherited, CLIENT_A), containsInAnyOrder("a-role-2"));
    }

    @Test
    public void availablePagesAreNotShortenedByRolesTheCallerCannotMap() {
        // manage-users does not allow mapping realm-management roles the admin does not hold, those roles must not
        // consume the page
        RoleMappingResource roles = userManager.realm(managedRealm.getName()).users().get(userId).roles();
        List<String> all = allClientRoleNames(roles.getAllAvailable(null, null, null));
        assertThat(all, hasItem(AdminRoles.MANAGE_USERS));
        assertThat(all, not(hasItem(AdminRoles.MANAGE_REALM)));

        List<String> paged = new ArrayList<>();
        for (int first = 0; ; first += 3) {
            MappingsRepresentation page = roles.getAllAvailable(null, first, 3);
            List<String> names = page.getClientMappings() == null ? List.of() : allClientRoleNames(page);
            if (names.isEmpty()) {
                break;
            }
            assertThat(names, anyOf(hasSize(3), hasSize(all.size() - first)));
            paged.addAll(names);
        }

        assertThat(paged, equalTo(all));
    }

    @Test
    public void viewClientsIsEnoughToListClientRoles() {
        // the scenario of an admin who only maintains the authorization settings of a client and needs to pick roles
        RealmResource realm = clientViewer.realm(managedRealm.getName());

        List<ClientMappingsRepresentation> roles = realm.clients().findRoles(null, null, null);
        assertThat(clientRoleNames(roles, CLIENT_A), containsInAnyOrder("a-role-1", "a-role-2"));
        assertThat(clientRoleNames(roles, CLIENT_B), containsInAnyOrder("b-role-1", "b-role-2"));
        assertThat(clientRoleNames(roles, OTHER_CLIENT), containsInAnyOrder("other-role"));

        // search matches the role name or the client id
        roles = realm.clients().findRoles("b-role", null, null);
        assertThat(roles.stream().map(ClientMappingsRepresentation::getClient).collect(Collectors.toList()), contains(CLIENT_B));
        roles = realm.clients().findRoles(OTHER_CLIENT, null, null);
        assertThat(roles.stream().map(ClientMappingsRepresentation::getClient).collect(Collectors.toList()), contains(OTHER_CLIENT));

        // paging over all client roles, regardless of the client they belong to
        List<String> firstPage = allClientRoleNames(realm.clients().findRoles(null, 0, 2));
        List<String> secondPage = allClientRoleNames(realm.clients().findRoles(null, 2, 2));
        assertThat(firstPage, hasSize(2));
        assertThat(secondPage, hasSize(2));
        assertThat(firstPage.stream().filter(secondPage::contains).collect(Collectors.toList()), empty());

        // ...while the role mapping endpoints stay out of reach without permissions on the target
        assertThrows(ForbiddenException.class, () -> realm.users().get(userId).roles().getAllAvailable(null, null, null));
    }

    @Test
    public void listingClientRolesFollowsViewPermissions() {
        // an admin that can map roles can also see them, even without view-clients
        List<ClientMappingsRepresentation> roles = userManager.realm(managedRealm.getName()).clients().findRoles(null, null, null);
        assertThat(clientRoleNames(roles, CLIENT_A), containsInAnyOrder("a-role-1", "a-role-2"));

        // query-clients alone is enough to call the endpoint, but no role is visible
        roles = clientQuerier.realm(managedRealm.getName()).clients().findRoles(null, null, null);
        assertThat(roles, empty());

        // without any client permission the endpoint is forbidden
        assertThrows(ForbiddenException.class, () -> realmViewer.realm(managedRealm.getName()).clients().findRoles(null, null, null));
    }

    private Set<String> realmRoleNames(MappingsRepresentation mappings) {
        return mappings.getRealmMappings() == null
                ? Set.of()
                : mappings.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private Set<String> roleNames(MappingsRepresentation mappings, String clientId) {
        Map<String, ClientMappingsRepresentation> clientMappings = mappings.getClientMappings();
        assertThat(clientMappings, hasKey(clientId));
        return clientMappings.get(clientId).getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private List<String> allClientRoleNames(MappingsRepresentation mappings) {
        return allClientRoleNames(List.copyOf(mappings.getClientMappings().values()));
    }

    private List<String> allClientRoleNames(List<ClientMappingsRepresentation> clients) {
        return clients.stream()
                .flatMap(m -> m.getMappings().stream())
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    private Set<String> clientRoleNames(List<ClientMappingsRepresentation> clients, String clientId) {
        return clients.stream()
                .filter(c -> clientId.equals(c.getClient()))
                .findFirst()
                .map(c -> c.getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()))
                .orElseThrow(() -> new AssertionError("No roles for client " + clientId));
    }

    private RoleRepresentation realmRole(String name) {
        return managedRealm.admin().roles().get(name).toRepresentation();
    }

    private RoleRepresentation clientRole(String clientUuid, String name) {
        return managedRealm.admin().clients().get(clientUuid).roles().get(name).toRepresentation();
    }

    public static class RoleMappingSearchRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.realmRoles(
                    RoleBuilder.create("realm-role-1"),
                    RoleBuilder.create("realm-role-2"),
                    RoleBuilder.create("composite-role").composite(true)
                            .realmComposite("realm-role-1").clientComposite(CLIENT_A, "a-role-2"),
                    RoleBuilder.create("nested-composite-role").composite(true)
                            .realmComposite("composite-role"));

            realm.clients(
                    ClientBuilder.create("myclient").secret("mysecret").directAccessGrantsEnabled(true),
                    ClientBuilder.create(CLIENT_A),
                    ClientBuilder.create(CLIENT_B),
                    ClientBuilder.create(OTHER_CLIENT));
            realm.clientRoles(CLIENT_A, "a-role-1", "a-role-2");
            realm.clientRoles(CLIENT_B, "b-role-1", "b-role-2");
            realm.clientRoles(OTHER_CLIENT, "other-role");

            realm.groups(GroupBuilder.create().name("target-group"));

            realm.users(
                    UserBuilder.create("target").name("Target", "User").email("target@localhost").emailVerified(true),
                    adminUser("user-manager", AdminRoles.MANAGE_USERS),
                    adminUser("user-viewer", AdminRoles.VIEW_USERS, AdminRoles.VIEW_CLIENTS),
                    adminUser("client-manager", AdminRoles.MANAGE_CLIENTS),
                    adminUser("client-viewer", AdminRoles.VIEW_CLIENTS, AdminRoles.MANAGE_AUTHORIZATION),
                    adminUser("client-querier", AdminRoles.QUERY_CLIENTS),
                    adminUser("realm-viewer", AdminRoles.VIEW_REALM));

            return realm;
        }

        private UserBuilder adminUser(String username, String... roles) {
            return UserBuilder.create(username)
                    .password("password")
                    .name(username, "Admin")
                    .email(username + "@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, roles);
        }
    }
}
