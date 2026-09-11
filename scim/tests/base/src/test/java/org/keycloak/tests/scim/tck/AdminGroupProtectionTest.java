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
import java.util.Optional;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.scim.resource.Scim.getCoreSchema;
import static org.keycloak.tests.scim.tck.AdminGroupProtectionRealmConfig.ADMIN_CHILD_GROUP;
import static org.keycloak.tests.scim.tck.AdminGroupProtectionRealmConfig.ADMIN_GROUP;
import static org.keycloak.tests.scim.tck.AdminGroupProtectionRealmConfig.ADMIN_PARENT_GROUP;
import static org.keycloak.tests.scim.tck.AdminGroupProtectionRealmConfig.ADMIN_USER;
import static org.keycloak.tests.scim.tck.AdminGroupProtectionRealmConfig.ADMIN_VIA_COMPOSITE_GROUP;
import static org.keycloak.tests.scim.tck.AdminGroupProtectionRealmConfig.REGULAR_GROUP;
import static org.keycloak.tests.scim.tck.AdminGroupProtectionRealmConfig.REGULAR_USER;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class AdminGroupProtectionTest {

    @InjectRealm(config = AdminGroupProtectionRealmConfig.class)
    ManagedRealm realm;

    @InjectScimClient
    ScimClient client;

    @Test
    public void testAdminGroupGetByIdReturnsMinimalRepresentation() {
        String groupId = getGroupId(ADMIN_GROUP);
        Group result = client.groups().get(groupId);

        assertNotNull(result);
        assertEquals(ADMIN_GROUP, result.getDisplayName());
        assertTrue(result.hasSchema(getCoreSchema(result.getClass())));
        assertNull(result.getMeta().getCreated());
        assertNull(result.getMembers());
    }

    @Test
    public void testAdminGroupInFilteredSearchWithMinimalRepresentation() {
        String filter = ResourceFilter.filter().eq("displayName", ADMIN_GROUP).build();
        ListResponse<Group> response = client.groups().getAll(filter);
        assertEquals(1, response.getTotalResults());

        Group group = response.getResources().get(0);
        assertEquals(ADMIN_GROUP, group.getDisplayName());
    }

    @Test
    public void testAdminGroupCannotBeUpdated() {
        String groupId = getGroupId(ADMIN_GROUP);

        Group group = new Group();
        group.setId(groupId);
        group.setDisplayName("updated-name");

        try {
            client.groups().update(group);
            fail("Should not be able to update admin group");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testAdminGroupCannotBeDeleted() {
        String groupId = getGroupId(ADMIN_GROUP);

        try {
            client.groups().delete(groupId);
            fail("Should not be able to delete admin group");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testAdminGroupCannotBePatched() {
        String groupId = getGroupId(ADMIN_GROUP);

        try {
            client.groups().patch(groupId, PatchRequest.create()
                    .replace("displayName", "patched-name")
                    .build());
            fail("Should not be able to patch admin group");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testChildGroupUnderAdminParentReturnsMinimalRepresentation() {
        String childGroupId = getGroupId(ADMIN_CHILD_GROUP);
        Group result = client.groups().get(childGroupId);

        assertNotNull(result);
        assertEquals(ADMIN_CHILD_GROUP, result.getDisplayName());
        assertNull(result.getMeta().getCreated());
        assertNull(result.getMembers());
    }

    @Test
    public void testChildGroupUnderAdminParentCannotBeDeleted() {
        String childGroupId = getGroupId(ADMIN_CHILD_GROUP);

        try {
            client.groups().delete(childGroupId);
            fail("Should not be able to delete child group under admin parent");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testAdminParentGroupReturnsMinimalRepresentation() {
        String parentGroupId = getGroupId(ADMIN_PARENT_GROUP);
        Group result = client.groups().get(parentGroupId);

        assertNotNull(result);
        assertEquals(ADMIN_PARENT_GROUP, result.getDisplayName());
        assertNull(result.getMeta().getCreated());
        assertNull(result.getMembers());
    }

    @Test
    public void testAdminViaCompositeRoleReturnsMinimalRepresentation() {
        String groupId = getGroupId(ADMIN_VIA_COMPOSITE_GROUP);
        Group result = client.groups().get(groupId);

        assertNotNull(result);
        assertEquals(ADMIN_VIA_COMPOSITE_GROUP, result.getDisplayName());
        assertNull(result.getMeta().getCreated());
        assertNull(result.getMembers());
    }

    @Test
    public void testAdminViaCompositeRoleCannotBeDeleted() {
        String groupId = getGroupId(ADMIN_VIA_COMPOSITE_GROUP);

        try {
            client.groups().delete(groupId);
            fail("Should not be able to delete group with composite admin role");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testRegularGroupHasFullRepresentation() {
        String regularGroupId = getGroupId(REGULAR_GROUP);
        Group result = client.groups().get(regularGroupId);

        assertNotNull(result);
        assertEquals(REGULAR_GROUP, result.getDisplayName());
        assertNotNull(result.getMeta().getCreated());
    }

    @Test
    public void testCannotAddUserToAdminGroupViaUserPatch() {
        String userId = getUserId(REGULAR_USER);
        String adminGroupId = getGroupId(ADMIN_GROUP);

        try {
            client.users().patch(userId, PatchRequest.create()
                    .add("groups", adminGroupId)
                    .build());
            fail("Should not be able to add user to admin group via user PATCH");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testCannotRemoveUserFromAdminGroupViaUserPatch() {
        String userId = getUserId(REGULAR_USER);
        String adminGroupId = getGroupId(ADMIN_GROUP);

        realm.admin().users().get(userId).joinGroup(adminGroupId);

        try {
            client.users().patch(userId, PatchRequest.create()
                    .remove("groups[value eq \"" + adminGroupId + "\"]")
                    .build());
            fail("Should not be able to remove user from admin group via user PATCH");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        } finally {
            realm.admin().users().get(userId).leaveGroup(adminGroupId);
        }
    }

    @Test
    public void testCannotAddMemberToAdminGroupViaGroupPatch() {
        String userId = getUserId(REGULAR_USER);
        String adminGroupId = getGroupId(ADMIN_GROUP);

        try {
            client.groups().patch(adminGroupId, PatchRequest.create()
                    .add("members", userId)
                    .build());
            fail("Should not be able to add member to admin group via group PATCH");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testCannotAddAdminUserToRegularGroupViaGroupPatch() {
        String adminUserId = getUserId(ADMIN_USER);
        String regularGroupId = getGroupId(REGULAR_GROUP);

        try {
            client.groups().patch(regularGroupId, PatchRequest.create()
                    .add("members", adminUserId)
                    .build());
            fail("Should not be able to add admin user to regular group via group PATCH");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        }
    }

    @Test
    public void testCannotRemoveAdminUserFromRegularGroupViaGroupPatch() {
        String adminUserId = getUserId(ADMIN_USER);
        String regularGroupId = getGroupId(REGULAR_GROUP);

        realm.admin().users().get(adminUserId).joinGroup(regularGroupId);

        try {
            client.groups().patch(regularGroupId, PatchRequest.create()
                    .remove("members[value eq \"" + adminUserId + "\"]")
                    .build());
            fail("Should not be able to remove admin user from regular group via group PATCH");
        } catch (ScimClientException sce) {
            assertEquals(403, sce.getError().getStatusInt());
        } finally {
            realm.admin().users().get(adminUserId).leaveGroup(regularGroupId);
        }
    }

    @Test
    public void testServiceAccountNotExposedInGroupMembers() {
        ClientRepresentation confidentialClient = ClientBuilder.create()
                .clientId("sa-test-client")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .enabled(true)
                .build();
        String clientDbId;
        try (Response response = realm.admin().clients().create(confidentialClient)) {
            clientDbId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientDbId).remove());

        String regularGroupId = getGroupId(REGULAR_GROUP);
        UserRepresentation serviceAccount = realm.admin().clients().get(clientDbId).getServiceAccountUser();
        realm.admin().users().get(serviceAccount.getId()).joinGroup(regularGroupId);

        Group group = client.groups().get(regularGroupId, List.of("members"));
        List<org.keycloak.scim.resource.group.Member> members = group.getMembers();
        if (members != null) {
            boolean exposed = members.stream().anyMatch(m -> serviceAccount.getId().equals(m.getValue()));
            assertFalse(exposed, "Service account must not appear in SCIM group members");
        }
    }

    @Test
    public void testCannotAddServiceAccountToGroupViaGroupPatch() {
        ClientRepresentation confidentialClient = ClientBuilder.create()
                .clientId("sa-patch-test-client")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .enabled(true)
                .build();
        String clientDbId;
        try (Response response = realm.admin().clients().create(confidentialClient)) {
            clientDbId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientDbId).remove());

        String regularGroupId = getGroupId(REGULAR_GROUP);
        UserRepresentation serviceAccount = realm.admin().clients().get(clientDbId).getServiceAccountUser();

        try {
            client.groups().patch(regularGroupId, PatchRequest.create()
                    .add("members", serviceAccount.getId())
                    .build());
            fail("Should not be able to add a service account to a group via SCIM PATCH");
        } catch (ScimClientException sce) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), sce.getError().getStatusInt());
        }
    }

    @Test
    public void testCannotRemoveServiceAccountFromGroupViaGroupPatch() {
        ClientRepresentation confidentialClient = ClientBuilder.create()
                .clientId("sa-remove-test-client")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .enabled(true)
                .build();
        String clientDbId;
        try (Response response = realm.admin().clients().create(confidentialClient)) {
            clientDbId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientDbId).remove());

        String regularGroupId = getGroupId(REGULAR_GROUP);
        UserRepresentation serviceAccount = realm.admin().clients().get(clientDbId).getServiceAccountUser();

        // Add service account to group via Admin API (bypassing SCIM protection)
        realm.admin().users().get(serviceAccount.getId()).joinGroup(regularGroupId);

        try {
            client.groups().patch(regularGroupId, PatchRequest.create()
                    .remove("members[value eq \"" + serviceAccount.getId() + "\"]")
                    .build());
            fail("Should not be able to remove a service account from a group via SCIM PATCH");
        } catch (ScimClientException sce) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), sce.getError().getStatusInt());
        }

        // Verify service account is still in the group (operation was rejected)
        UserResource userResource = realm.admin().users().get(serviceAccount.getId());
        List<String> groups = Optional.ofNullable(userResource.groups()).orElse(List.of()).stream().map(GroupRepresentation::getId).toList();
        assertTrue(groups.contains(regularGroupId), "Service account should still be in group after failed PATCH");
    }

    private String getUserId(String username) {
        List<UserRepresentation> users = realm.admin().users().searchByUsername(username, true);
        assertEquals(1, users.size());
        return users.get(0).getId();
    }

    private String getGroupId(String name) {
        List<GroupRepresentation> groups = realm.admin().groups().groups(name, true, -1, -1, true);
        assertEquals(1, groups.size(), "Expected exactly one group with name: " + name);
        GroupRepresentation group = groups.get(0);
        if (group.getName().equals(name)) {
            return group.getId();
        }
        // search returns parent when matching a child — look through subgroups
        return group.getSubGroups().stream()
                .filter(sub -> name.equals(sub.getName()))
                .findFirst()
                .map(GroupRepresentation::getId)
                .orElseThrow(() -> new AssertionError("Group not found: " + name));
    }
}
