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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.federation.UserMapStorageFactory;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;
import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = UserFederationFilteringTest.ServerConfig.class)
public class UserFederationFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private String fedUsername;
    private String fedUserId;
    private String memProviderId;

    @BeforeEach
    public void onBeforeEach() {
        fedUsername = "fed-user-" + KeycloakModelUtils.generateId().substring(0, 8);

        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));
        memProvider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

        memProviderId = ApiUtil.getCreatedId(realm.admin().components().add(memProvider));
        realm.cleanup().add(r -> r.components().component(memProviderId).remove());

        UserRepresentation fedUser = new UserRepresentation();
        fedUser.setUsername(fedUsername);
        fedUser.setEnabled(true);
        fedUser.setFederationLink(memProviderId);
        fedUserId = ApiUtil.getCreatedId(realm.admin().users().create(fedUser));
        assertFalse(StorageId.isLocalStorage(fedUserId));
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminCannotSearchFederatedUsers() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(fedUsername, 0, 10);
        assertThat(search, empty());
        assertThat(realmAdminClient.realm(realm.getName()).users().count(fedUsername), is(0));
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminCanCountAuthorizedFederatedUsers() {
        var users = realmAdminClient.realm(realm.getName()).users();
        assertThat(users.search(fedUsername, 0, 10), empty());
        assertThat(users.count(fedUsername), is(0));

        UserRepresentation myAdmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, fedUserId, USERS_RESOURCE_TYPE, Set.of(VIEW), policy);

        List<UserRepresentation> search = users.search(fedUsername, 0, 10);
        assertThat(search, hasSize(1));
        assertThat(search.get(0).getId(), is(fedUserId));
        assertFalse(StorageId.isLocalStorage(search.get(0).getId()));
        assertThat(users.count(fedUsername), is(1));
    }

    @Test
    @DatabaseTest
    public void testFederatedUserCountRespectsViewableGroups() {
        GroupRepresentation allowedGroup = createGroup("count-visible-group");
        String search = "group-count-" + KeycloakModelUtils.generateId().substring(0, 8);

        UserRepresentation allowedUser = createFederatedUser(search + "-allowed");
        createFederatedUser(search + "-outside");

        ComponentRepresentation userStorage = realm.admin().components().component(memProviderId).toRepresentation();
        userStorage.getConfig().putSingle(UserMapStorageFactory.USE_FEDERATED_STORAGE_FOR_GROUP_MEMBERSHIPS, Boolean.TRUE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);
        realm.admin().users().get(allowedUser.getId()).joinGroup(allowedGroup.getId());

        UserRepresentation myAdmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Group-Scoped Count Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, allowedGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), policy);

        assertThat(realmAdminClient.realm(realm.getName()).users().count(search), is(1));
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminCanListFederatedGroupMembers() {
        GroupRepresentation group = createGroup("federated-group");
        String hiddenUsername = "hidden-" + KeycloakModelUtils.generateId().substring(0, 8);
        UserRepresentation hiddenUser = new UserRepresentation();
        hiddenUser.setUsername(hiddenUsername);
        hiddenUser.setEnabled(true);
        hiddenUser.setFederationLink(memProviderId);
        String hiddenUserId = ApiUtil.getCreatedId(realm.admin().users().create(hiddenUser));
        assertFalse(StorageId.isLocalStorage(hiddenUserId));

        ComponentRepresentation userStorage = realm.admin().components().component(memProviderId).toRepresentation();
        userStorage.getConfig().putSingle(UserMapStorageFactory.USE_FEDERATED_STORAGE_FOR_GROUP_MEMBERSHIPS, Boolean.TRUE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);

        realm.admin().users().get(fedUserId).joinGroup(group.getId());
        realm.admin().users().get(hiddenUserId).joinGroup(group.getId());

        assertFalse(realmAdminClient.realm(realm.getName()).groups().groups().stream()
                .anyMatch(visibleGroup -> group.getId().equals(visibleGroup.getId())));
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).toRepresentation());
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 10));

        UserRepresentation myAdmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, group.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW, VIEW_MEMBERS, MANAGE_MEMBERSHIP, MANAGE_MEMBERS), policy);
        createPermission(adminPermissionsClient, fedUserId, USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, policy, Set.of(MANAGE_GROUP_MEMBERSHIP));

        List<UserRepresentation> fullAdminMembers = realm.admin().groups().group(group.getId()).members(0, 10);
        assertThat(fullAdminMembers, hasSize(2));

        List<UserRepresentation> delegatedMembers = realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 10);
        assertThat(delegatedMembers, hasSize(2));
        assertThat(delegatedMembers.stream().map(UserRepresentation::getUsername).toList(),
                containsInAnyOrder(fedUsername, hiddenUsername));
    }

    @Test
    @DatabaseTest
    public void testDeniedFederatedGroupMembersAreFiltered() {
        GroupRepresentation group = createGroup("federated-group");
        GroupRepresentation protectedGroup = createGroup("protected-group");
        String hiddenUsername = "hidden-" + KeycloakModelUtils.generateId().substring(0, 8);
        UserRepresentation hiddenUser = new UserRepresentation();
        hiddenUser.setUsername(hiddenUsername);
        hiddenUser.setEnabled(true);
        hiddenUser.setFederationLink(memProviderId);
        String hiddenUserId = ApiUtil.getCreatedId(realm.admin().users().create(hiddenUser));
        assertFalse(StorageId.isLocalStorage(hiddenUserId));

        ComponentRepresentation userStorage = realm.admin().components().component(memProviderId).toRepresentation();
        userStorage.getConfig().putSingle(UserMapStorageFactory.USE_FEDERATED_STORAGE_FOR_GROUP_MEMBERSHIPS, Boolean.TRUE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);

        realm.admin().users().get(fedUserId).joinGroup(group.getId());
        realm.admin().users().get(hiddenUserId).joinGroup(group.getId());
        realm.admin().users().get(hiddenUserId).joinGroup(protectedGroup.getId());

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).toRepresentation());
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 10));

        UserRepresentation myAdmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, group.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW, VIEW_MEMBERS, MANAGE_MEMBERSHIP, MANAGE_MEMBERS), allowMyAdmin);
        createPermission(adminPermissionsClient, fedUserId, USERS_RESOURCE_TYPE, Set.of(VIEW), allowMyAdmin);
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(MANAGE_GROUP_MEMBERSHIP));

        List<UserRepresentation> fullAdminMembers = realm.admin().groups().group(group.getId()).members(0, 10);
        assertThat(fullAdminMembers, hasSize(2));

        List<UserRepresentation> delegatedMembers = realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 10);
        assertThat(delegatedMembers, hasSize(2));
        assertThat(delegatedMembers.stream().map(UserRepresentation::getUsername).toList(),
                containsInAnyOrder(fedUsername, hiddenUsername));

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,
                "Deny My Admin User Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, protectedGroup.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW_MEMBERS), denyMyAdmin);

        delegatedMembers = realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 10);
        assertThat(delegatedMembers, hasSize(1));
        assertThat(delegatedMembers.get(0).getUsername(), is(fedUsername));
    }

    @Test
    @DatabaseTest
    public void testFederatedGroupMemberPaginationContinuesAfterUnauthorizedBatch() {
        GroupRepresentation group = createGroup("federated-group");
        GroupRepresentation protectedGroup = createGroup("protected-group");

        for (int i = 0; i < 100; i++) {
            UserRepresentation hiddenUser = new UserRepresentation();
            hiddenUser.setUsername(String.format(Locale.ROOT, "a-hidden-%03d", i));
            hiddenUser.setEnabled(true);
            hiddenUser.setFederationLink(memProviderId);
            String hiddenUserId = ApiUtil.getCreatedId(realm.admin().users().create(hiddenUser));
            realm.admin().users().get(hiddenUserId).joinGroup(group.getId());
            realm.admin().users().get(hiddenUserId).joinGroup(protectedGroup.getId());
        }
        realm.admin().users().get(fedUserId).joinGroup(group.getId());

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).toRepresentation());
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 10));

        UserRepresentation myAdmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myAdmin.getId());
        UserPolicyRepresentation denyMyAdmin = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,
                "Deny My Admin User Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, group.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW, VIEW_MEMBERS, MANAGE_MEMBERSHIP, MANAGE_MEMBERS), allowMyAdmin);
        createPermission(adminPermissionsClient, protectedGroup.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW_MEMBERS), denyMyAdmin);
        createPermission(adminPermissionsClient, fedUserId, USERS_RESOURCE_TYPE, Set.of(VIEW), allowMyAdmin);
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(MANAGE_GROUP_MEMBERSHIP));

        List<UserRepresentation> fullAdminMembers = realm.admin().groups().group(group.getId()).members(0, 200);
        assertThat(fullAdminMembers, hasSize(101));

        ComponentRepresentation userStorage = realm.admin().components().component(memProviderId).toRepresentation();
        userStorage.getConfig().putSingle(IMPORT_ENABLED, Boolean.TRUE.toString());
        userStorage.getConfig().putSingle(UserMapStorageFactory.EXCLUDE_IMPORTED_GROUP_MEMBERS, Boolean.TRUE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);

        List<UserRepresentation> delegatedMembers = realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 10);
        assertThat(delegatedMembers, hasSize(1));
        assertThat(delegatedMembers.get(0).getUsername(), is(fedUsername));
    }

    @Test
    @DatabaseTest
    public void testFederatedGroupMemberPaginationSkipsOnlyAuthorizedLocalMembers() {
        GroupRepresentation group = createGroup("pagination-group");
        GroupRepresentation protectedGroup = createGroup("protected-group");

        ComponentRepresentation userStorage = realm.admin().components().component(memProviderId).toRepresentation();
        userStorage.getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, Boolean.FALSE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);

        for (int i = 0; i < 10; i++) {
            UserRepresentation localUser = new UserRepresentation();
            localUser.setUsername(String.format(Locale.ROOT, "local-member-%02d", i));
            localUser.setEnabled(true);
            String localUserId = ApiUtil.getCreatedId(realm.admin().users().create(localUser));
            assertTrue(StorageId.isLocalStorage(localUserId));
            realm.admin().users().get(localUserId).joinGroup(group.getId());
            realm.admin().users().get(localUserId).joinGroup(protectedGroup.getId());
        }

        userStorage.getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, Boolean.TRUE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);

        Set<String> federatedMemberIds = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            UserRepresentation member = createFederatedUser(String.format(Locale.ROOT, "federated-member-%02d", i));
            federatedMemberIds.add(member.getId());
            realm.admin().users().get(member.getId()).joinGroup(group.getId());
        }

        UserRepresentation myAdmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(realm, adminPermissionsClient,
                "Pagination Group Admin Policy", myAdmin.getId());
        UserPolicyRepresentation denyMyAdmin = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,
                "Deny Local Members Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, group.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW, VIEW_MEMBERS), allowMyAdmin);
        createPermission(adminPermissionsClient, protectedGroup.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW_MEMBERS), denyMyAdmin);
        createPermission(adminPermissionsClient, federatedMemberIds, USERS_RESOURCE_TYPE, Set.of(VIEW), allowMyAdmin);

        assertThat(realm.admin().groups().group(group.getId()).members(0, 20), hasSize(20));
        assertThat(realm.admin().groups().group(group.getId()).members(5, 5).stream()
                        .map(UserRepresentation::getUsername).toList(),
                is(List.of("local-member-05", "local-member-06", "local-member-07",
                        "local-member-08", "local-member-09")));
        List<UserRepresentation> visibleMembers = realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(0, 20);
        assertThat(visibleMembers.stream().map(UserRepresentation::getUsername).toList(),
                is(List.of("federated-member-00", "federated-member-01", "federated-member-02",
                        "federated-member-03", "federated-member-04", "federated-member-05",
                        "federated-member-06", "federated-member-07", "federated-member-08", "federated-member-09")));

        List<UserRepresentation> page = realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(5, 5);

        assertThat(page.stream().map(UserRepresentation::getUsername).toList(),
                is(List.of("federated-member-05", "federated-member-06", "federated-member-07",
                        "federated-member-08", "federated-member-09")));
    }

    @Test
    @DatabaseTest
    public void testFederatedGroupMemberPaginationDoesNotConsumeImportedUsersDuringCount() {
        GroupRepresentation group = createGroup("pagination-group");
        List<String> memberNames = List.of("page-member-a", "page-member-b", "page-member-c");
        for (String memberName : memberNames) {
            UserRepresentation member = createFederatedUser(memberName);
            realm.admin().users().get(member.getId()).joinGroup(group.getId());
        }

        ComponentRepresentation userStorage = realm.admin().components().component(memProviderId).toRepresentation();
        userStorage.getConfig().putSingle(IMPORT_ENABLED, Boolean.TRUE.toString());
        userStorage.getConfig().putSingle(UserMapStorageFactory.EXCLUDE_IMPORTED_GROUP_MEMBERS, Boolean.TRUE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);

        UserRepresentation myAdmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Pagination Group Admin Policy", myAdmin.getId());
        createPermission(adminPermissionsClient, group.getId(), GROUPS_RESOURCE_TYPE,
                Set.of(VIEW, VIEW_MEMBERS), policy);
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<UserRepresentation> page = realmAdminClient.realm(realm.getName())
                .groups().group(group.getId()).members(1, 10);

        assertThat(page.stream().map(UserRepresentation::getUsername).toList(),
                containsInAnyOrder("page-member-b", "page-member-c"));
    }

    @Test
    @DatabaseTest
    public void testFederatedDelegatedAdminCanListImportedGroupMembers() {
        GroupRepresentation group = createGroup("groupe1");
        realm.admin().users().get(fedUserId).joinGroup(group.getId());

        String delegatedAdminUsername = "admin-groupe1";
        UserRepresentation delegatedAdmin = new UserRepresentation();
        delegatedAdmin.setUsername(delegatedAdminUsername);
        delegatedAdmin.setEnabled(true);
        delegatedAdmin.setFederationLink(memProviderId);
        String externalAdminId = ApiUtil.getCreatedId(realm.admin().users().create(delegatedAdmin));
        assertFalse(StorageId.isLocalStorage(externalAdminId));

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        realm.admin().users().get(externalAdminId).resetPassword(password);

        // Seed membership while the user is a federated adapter; after import is enabled, lookups return an imported
        // local user while the membership remains in the provider's userGroups map.
        ComponentRepresentation userStorage = realm.admin().components().component(memProviderId).toRepresentation();
        userStorage.getConfig().putSingle(IMPORT_ENABLED, Boolean.TRUE.toString());
        realm.admin().components().component(memProviderId).update(userStorage);

        UserRepresentation importedAdmin = realm.admin().users().get(externalAdminId).toRepresentation();
        assertThat(importedAdmin.getFederationLink(), is(memProviderId));
        assertTrue(StorageId.isLocalStorage(importedAdmin.getId()));
        importedAdmin.setFirstName("Admin");
        importedAdmin.setLastName("Group");
        importedAdmin.setEmail(delegatedAdminUsername + "@example.com");
        importedAdmin.setEmailVerified(true);
        realm.admin().users().get(importedAdmin.getId()).update(importedAdmin);

        UserRepresentation frameworkAdmin = realm.admin().users().search("myadmin").get(0);
        realm.admin().users().get(frameworkAdmin.getId()).remove();

        String realmManagementClientId = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        List<RoleRepresentation> queryRoles = List.of(
                realm.admin().clients().get(realmManagementClientId).roles().get(AdminRoles.QUERY_USERS).toRepresentation(),
                realm.admin().clients().get(realmManagementClientId).roles().get(AdminRoles.QUERY_GROUPS).toRepresentation(),
                realm.admin().clients().get(realmManagementClientId).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation());
        realm.admin().users().get(importedAdmin.getId()).roles().clientLevel(realmManagementClientId).add(queryRoles);

        try (Keycloak federatedAdminClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBaseUrl().toString())
                .realm(realm.getName())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("myclient")
                .clientSecret("mysecret")
                .username(delegatedAdminUsername)
                .password("password")
                .build()) {
            List<GroupRepresentation> visibleGroups = federatedAdminClient.realm(realm.getName()).groups().groups();
            assertFalse(visibleGroups.stream().anyMatch(visibleGroup -> group.getId().equals(visibleGroup.getId())));
            var groupResource = federatedAdminClient.realm(realm.getName()).groups().group(group.getId());
            assertThrows(ForbiddenException.class, () -> groupResource.toRepresentation());
            assertThrows(ForbiddenException.class, () -> groupResource.members(0, 10));

            UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                    "Only Federated Group Admin Policy", importedAdmin.getId());
            createPermission(adminPermissionsClient, group.getId(), GROUPS_RESOURCE_TYPE,
                    Set.of(VIEW, VIEW_MEMBERS, MANAGE_MEMBERSHIP, MANAGE_MEMBERS), policy);
            createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, policy,
                    Set.of(VIEW, MANAGE_GROUP_MEMBERSHIP));

            List<UserRepresentation> fullAdminMembers = realm.admin().groups().group(group.getId()).members(0, 10);
            assertThat(fullAdminMembers, hasSize(1));
            assertThat(fullAdminMembers.get(0).getUsername(), is(fedUsername));
            assertThat(fullAdminMembers.get(0).getFederationLink(), is(memProviderId));
            assertTrue(StorageId.isLocalStorage(fullAdminMembers.get(0).getId()));

            List<UserRepresentation> delegatedMembers = federatedAdminClient.realm(realm.getName())
                    .groups().group(group.getId()).members(0, 10);
            assertThat(delegatedMembers, hasSize(1));
            assertThat(delegatedMembers.get(0).getUsername(), is(fedUsername));
        }
    }

    @Test
    public void testPrivilegedAdminCanSearchFederatedUsers() {
        List<UserRepresentation> search = realm.admin().users().search(fedUsername, 0, 10);
        assertThat(search, hasSize(1));
        assertThat(search.get(0).getUsername(), is(fedUsername));
        assertThat(realm.admin().users().count(fedUsername), is(1));
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminCannotSeeRoleMembersFromFederatedStorage() {
        String myAdminId = realm.admin().users().search("myadmin").get(0).getId();
        String clientUuid = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewRealm = realm.admin().clients().get(clientUuid).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        realm.admin().users().get(myAdminId).roles().clientLevel(clientUuid).add(List.of(viewRealm));

        String roleName = "test-role-" + KeycloakModelUtils.generateId().substring(0, 8);
        realm.admin().roles().create(new RoleRepresentation(roleName, null, false));
        realm.cleanup().add(r -> r.roles().deleteRole(roleName));
        RoleRepresentation role = realm.admin().roles().get(roleName).toRepresentation();

        realm.admin().users().get(fedUserId).roles().realmLevel().add(List.of(role));

        List<UserRepresentation> members = realm.admin().roles().get(roleName).getUserMembers(0, 10);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getUsername(), is(fedUsername));

        List<UserRepresentation> delegatedMembers = realmAdminClient.realm(realm.getName()).roles().get(roleName).getUserMembers(0, 10);
        assertThat(delegatedMembers, empty());
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminWithViewUsersCanSearchFederatedUsers() {
        String myAdminId = realm.admin().users().search("myadmin").get(0).getId();
        String clientUuid = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewUsers = realm.admin().clients().get(clientUuid).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(myAdminId).roles().clientLevel(clientUuid).add(List.of(viewUsers));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(fedUsername, 0, 10);
        assertThat(search, hasSize(1));
        assertThat(search.get(0).getUsername(), is(fedUsername));
    }

    private UserRepresentation createFederatedUser(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setFederationLink(memProviderId);
        user.setId(ApiUtil.getCreatedId(realm.admin().users().create(user)));
        assertFalse(StorageId.isLocalStorage(user.getId()));
        return user;
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
