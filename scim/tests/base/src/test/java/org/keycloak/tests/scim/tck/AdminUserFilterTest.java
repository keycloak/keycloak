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

import static org.keycloak.tests.scim.tck.AdminUserFilterRealmConfig.ADMIN_IMPERSONATION;
import static org.keycloak.tests.scim.tck.AdminUserFilterRealmConfig.ADMIN_MANAGE_USERS;
import static org.keycloak.tests.scim.tck.AdminUserFilterRealmConfig.ADMIN_QUERY_USERS;
import static org.keycloak.tests.scim.tck.AdminUserFilterRealmConfig.ADMIN_VIEW_EVENTS;
import static org.keycloak.tests.scim.tck.AdminUserFilterRealmConfig.ADMIN_VIEW_REALM_REVOKABLE;
import static org.keycloak.tests.scim.tck.AdminUserFilterRealmConfig.ADMIN_VIEW_USERS;
import static org.keycloak.tests.scim.tck.AdminUserFilterRealmConfig.REGULAR_USER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class AdminUserFilterTest {

    @InjectRealm(config = AdminUserFilterRealmConfig.class)
    ManagedRealm realm;

    @InjectScimClient
    ScimClient client;

    @Test
    public void testAdminUserNotInListing() {
        String adminUserId = getUserId(ADMIN_MANAGE_USERS);
        String regularUserId = getUserId(REGULAR_USER);

        ListResponse<User> response = client.users().getAll();
        List<String> userIds = response.getResources().stream().map(User::getId).toList();
        assertThat(userIds, hasItem(regularUserId));
        assertThat(userIds, not(hasItem(adminUserId)));
    }

    @Test
    public void testAdminUserNotInFilteredSearch() {
        ListResponse<User> response = client.users().search(
                ResourceFilter.filter().eq("userName", ADMIN_VIEW_USERS).build());
        assertEquals(0, response.getTotalResults());
    }

    @Test
    public void testAdminUserNotFoundById() {
        String adminUserId = getUserId(ADMIN_MANAGE_USERS);
        User result = client.users().get(adminUserId);
        assertNull(result);
    }

    @Test
    public void testAdminUserCannotBeUpdated() {
        String adminUserId = getUserId(ADMIN_MANAGE_USERS);

        User user = new User();
        user.setId(adminUserId);
        user.setUserName("updated-name");

        try {
            client.users().update(adminUserId, user);
        } catch (ScimClientException sce) {
            assertEquals(404, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testAdminUserCannotBeDeleted() {
        String adminUserId = getUserId(ADMIN_MANAGE_USERS);

        try {
            client.users().delete(adminUserId);
        } catch (ScimClientException sce) {
            assertEquals(404, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testAdminUserCannotBePatched() {
        String adminUserId = getUserId(ADMIN_MANAGE_USERS);

        try {
            client.users().patch(adminUserId, PatchRequest.create()
                    .replace("active", "false")
                    .build());
        } catch (ScimClientException sce) {
            assertEquals(404, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testRegularUserStillVisible() {
        String regularUserId = getUserId(REGULAR_USER);
        User result = client.users().get(regularUserId);
        assertNotNull(result);
        assertEquals(REGULAR_USER, result.getUserName());
    }

    @Test
    public void testUserBecomesVisibleWhenAdminRoleRevoked() {
        String userId = getUserId(ADMIN_VIEW_REALM_REVOKABLE);

        assertNull(client.users().get(userId));

        revokeAdminRole(userId, AdminRoles.VIEW_REALM);

        assertNotNull(client.users().get(userId));
    }

    @Test
    public void testCountExcludesAdminUsers() {
        String adminUserId = getUserId(ADMIN_MANAGE_USERS);
        String regularUserId = getUserId(REGULAR_USER);

        ListResponse<User> response = client.users().getAll();
        List<String> userIds = response.getResources().stream().map(User::getId).toList();
        assertThat(userIds, not(hasItem(adminUserId)));
        assertThat(userIds, hasItem(regularUserId));
    }

    @Test
    public void testAnyRealmManagementRoleMakesUserAdmin() {
        assertNull(client.users().get(getUserId(ADMIN_QUERY_USERS)));
        assertNull(client.users().get(getUserId(ADMIN_VIEW_EVENTS)));
        assertNull(client.users().get(getUserId(ADMIN_IMPERSONATION)));
    }

    private String getUserId(String username) {
        List<UserRepresentation> users = realm.admin().users().search(username);
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
