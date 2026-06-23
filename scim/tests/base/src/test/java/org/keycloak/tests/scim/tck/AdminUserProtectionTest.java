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

package org.keycloak.tests.scim.tck;

import java.util.List;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_IMPERSONATION;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_MANAGE_CLIENTS;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_MANAGE_CLIENTS_REVOKABLE;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_MANAGE_IDENTITY_PROVIDERS;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_MANAGE_REALM;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_MANAGE_USERS;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_QUERY_USERS;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_VIA_COMPOSITE;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_VIA_GROUP;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_VIA_NESTED_GROUP;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.ADMIN_VIEW_CLIENTS;
import static org.keycloak.tests.scim.tck.AdminUserProtectionRealmConfig.REGULAR_USER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class AdminUserProtectionTest {

    @InjectRealm(config = AdminUserProtectionRealmConfig.class)
    ManagedRealm realm;

    @InjectScimClient
    ScimClient client;

    @Test
    public void testAdminUserInListingWithMinimalRepresentation() {
        String adminUserId = getUserId(ADMIN_MANAGE_REALM);

        ListResponse<User> response = client.users().getAll();
        User adminUser = response.getResources().stream()
                .filter(u -> u.getId().equals(adminUserId))
                .findFirst().orElse(null);

        assertNotNull(adminUser);
        assertEquals(ADMIN_MANAGE_REALM, adminUser.getUserName());
        assertNull(adminUser.getActive());
        assertNull(adminUser.getName());
        assertNull(adminUser.getEmails());
    }

    @Test
    public void testAdminUserInFilteredSearchWithMinimalRepresentation() {
        ListResponse<User> response = client.users().search(
                ResourceFilter.filter().eq("userName", ADMIN_VIEW_CLIENTS).build());
        assertEquals(1, response.getTotalResults());

        User adminUser = response.getResources().get(0);
        assertEquals(ADMIN_VIEW_CLIENTS, adminUser.getUserName());
        assertNull(adminUser.getName());
        assertNull(adminUser.getEmails());
    }

    @Test
    public void testAdminUserGetByIdReturnsMinimalRepresentation() {
        String adminUserId = getUserId(ADMIN_MANAGE_IDENTITY_PROVIDERS);
        User result = client.users().get(adminUserId);

        assertNotNull(result);
        assertEquals(ADMIN_MANAGE_IDENTITY_PROVIDERS, result.getUserName());
        assertNull(result.getActive());
        assertNull(result.getName());
        assertNull(result.getEmails());
    }

    @Test
    public void testAdminUserCannotBeUpdated() {
        String adminUserId = getUserId(ADMIN_MANAGE_CLIENTS);

        User user = new User();
        user.setId(adminUserId);
        user.setUserName("updated-name");

        try {
            client.users().update(adminUserId, user);
            fail("Should not be able to update admin user");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testAdminUserCannotBeDeleted() {
        String adminUserId = getUserId(ADMIN_QUERY_USERS);

        try {
            client.users().delete(adminUserId);
            fail("Should not be able to delete admin user");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testAdminUserCannotBePatched() {
        String adminUserId = getUserId(ADMIN_IMPERSONATION);

        try {
            client.users().patch(adminUserId, PatchRequest.create()
                    .replace("active", "false")
                    .build());
            fail("Should not be able to patch admin user");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testRegularUserHasFullRepresentation() {
        String regularUserId = getUserId(REGULAR_USER);
        User result = client.users().get(regularUserId);

        assertNotNull(result);
        assertEquals(REGULAR_USER, result.getUserName());
        assertNotNull(result.getActive());
    }

    @Test
    public void testMixedListingWithRequestedAttributes() {
        String adminUserId = getUserId(ADMIN_MANAGE_REALM);
        String regularUserId = getUserId(REGULAR_USER);

        ListResponse<User> response = client.users().getAll(
                List.of("userName", "active"), null);

        User adminUser = response.getResources().stream()
                .filter(u -> u.getId().equals(adminUserId))
                .findFirst().orElse(null);
        User regularUser = response.getResources().stream()
                .filter(u -> u.getId().equals(regularUserId))
                .findFirst().orElse(null);

        assertNotNull(adminUser);
        assertEquals(ADMIN_MANAGE_REALM, adminUser.getUserName());
        assertNull(adminUser.getActive());
        assertNull(adminUser.getName());

        assertNotNull(regularUser);
        assertEquals(REGULAR_USER, regularUser.getUserName());
        assertNotNull(regularUser.getActive());
        assertNull(regularUser.getName());
    }

    @Test
    public void testCountIncludesAdminUsers() {
        String adminUserId = getUserId(ADMIN_MANAGE_USERS);
        String regularUserId = getUserId(REGULAR_USER);

        ListResponse<User> response = client.users().getAll();
        List<String> userIds = response.getResources().stream().map(User::getId).toList();

        assertThat(userIds, hasItem(adminUserId));
        assertThat(userIds, hasItem(regularUserId));
    }

    @Test
    public void testAnyRealmManagementRoleReturnsMinimalRepresentation() {
        assertAdminUserMinimal(getUserId(ADMIN_MANAGE_USERS));
        assertAdminUserMinimal(getUserId(ADMIN_MANAGE_CLIENTS));
        assertAdminUserMinimal(getUserId(ADMIN_MANAGE_IDENTITY_PROVIDERS));
        assertAdminUserMinimal(getUserId(ADMIN_VIEW_CLIENTS));
        assertAdminUserMinimal(getUserId(ADMIN_QUERY_USERS));
        assertAdminUserMinimal(getUserId(ADMIN_IMPERSONATION));
    }

    @Test
    public void testAdminViaGroupMembershipReturnsMinimalRepresentation() {
        assertAdminUserMinimal(getUserId(ADMIN_VIA_GROUP));
    }

    @Test
    public void testAdminViaNestedGroupReturnsMinimalRepresentation() {
        assertAdminUserMinimal(getUserId(ADMIN_VIA_NESTED_GROUP));
    }

    @Test
    public void testAdminViaCompositeRoleReturnsMinimalRepresentation() {
        assertAdminUserMinimal(getUserId(ADMIN_VIA_COMPOSITE));
    }

    @Test
    public void testUserBecomesFullRepresentationWhenAdminRoleRevoked() {
        String userId = getUserId(ADMIN_MANAGE_CLIENTS_REVOKABLE);

        User beforeRevoke = client.users().get(userId);
        assertNotNull(beforeRevoke);
        assertNull(beforeRevoke.getName());

        revokeAdminRole(userId, AdminRoles.MANAGE_CLIENTS);

        User afterRevoke = client.users().get(userId);
        assertNotNull(afterRevoke);
    }

    private void assertAdminUserMinimal(String userId) {
        User user = client.users().get(userId);
        assertNotNull(user);
        assertNotNull(user.getUserName());
        assertNull(user.getActive());
        assertNull(user.getName());
        assertNull(user.getEmails());
    }

    private String getUserId(String username) {
        List<UserRepresentation> users = realm.admin().users().searchByUsername(username, true);
        assertEquals(1, users.size());
        return users.get(0).getId();
    }

    private void revokeAdminRole(String userId, String roleName) {
        ClientRepresentation realmMgmt = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation role = realm.admin().clients()
                .get(realmMgmt.getId()).roles().get(roleName).toRepresentation();
        realm.admin().users().get(userId).roles()
                .clientLevel(realmMgmt.getId()).remove(List.of(role));
    }
}
