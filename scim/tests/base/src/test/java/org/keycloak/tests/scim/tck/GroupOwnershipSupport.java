/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.tests.scim.tck;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.group.Member;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Shared steps for the SCIM group ownership tests.
 */
final class GroupOwnershipSupport {

    static final String MANAGED_BY_ANOTHER_CLIENT = "The group is managed by another SCIM client";

    private final ManagedRealm realm;

    GroupOwnershipSupport(ManagedRealm realm) {
        this.realm = realm;
    }

    Group createGroup(ScimClient client, String... memberIds) {
        Group group = client.groups().create(group(null, "owned-" + KeycloakModelUtils.generateId(), memberIds));
        cleanupGroup(group.getId());
        return group;
    }

    String createAdminGroup() {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setName("unowned-" + KeycloakModelUtils.generateId());

        try (Response response = realm.admin().groups().add(rep)) {
            assertEquals(201, response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            cleanupGroup(id);
            return id;
        }
    }

    void cleanupGroup(String id) {
        realm.cleanup().add(r -> {
            if (r.groups().groups().stream().anyMatch(g -> g.getId().equals(id))) {
                r.groups().group(id).remove();
            }
        });
    }

    String createUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("owned-" + KeycloakModelUtils.generateId());
        user.setEnabled(true);

        try (Response response = realm.admin().users().create(user)) {
            assertEquals(201, response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.users().delete(id).close());
            return id;
        }
    }

    String createAdminUser() {
        String id = createUser();
        grantManageUsers(id);
        return id;
    }

    String createServiceAccountUser() {
        ClientRepresentation client = ClientBuilder.create()
                .clientId("owned-" + KeycloakModelUtils.generateId())
                .secret("secret")
                .serviceAccountsEnabled(true)
                .enabled(true)
                .build();

        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(201, response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clients().get(id).remove());
            return realm.admin().clients().get(id).toRepresentation().getServiceAccountUserId();
        }
    }

    void grantManageUsers(String userId) {
        realm.admin().users().get(userId).roles().clientLevel(realmManagementId()).add(List.of(manageUsersRole()));
    }

    String realmManagementId() {
        return realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
    }

    RoleRepresentation manageUsersRole() {
        return realm.admin().clients().get(realmManagementId()).roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
    }

    void adminJoin(String userId, String groupId) {
        realm.admin().users().get(userId).joinGroup(groupId);
    }

    void adminLeave(String userId, String groupId) {
        realm.admin().users().get(userId).leaveGroup(groupId);
    }

    String groupName(String groupId) {
        return realm.admin().groups().group(groupId).toRepresentation().getName();
    }

    boolean groupExists(String groupId) {
        try {
            realm.admin().groups().group(groupId).toRepresentation();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    void assertMembers(String groupId, String... expected) {
        Set<String> actual = realm.admin().groups().group(groupId).members(0, 1000).stream()
                .map(UserRepresentation::getId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(expected), actual);
    }

    static void put(ScimClient client, Group group, String... memberIds) {
        client.groups().update(group(group.getId(), group.getDisplayName(), memberIds));
    }

    static Group group(String id, String displayName, String... memberIds) {
        Group group = new Group();
        group.setId(id);
        group.setDisplayName(displayName);
        group.setMembers(new ArrayList<>());

        for (String memberId : memberIds) {
            Member member = new Member();
            member.setValue(memberId);
            group.addMember(member);
        }

        return group;
    }

    static void patch(ScimClient client, String groupId, PatchOperation... operations) {
        PatchRequest request = PatchRequest.create().build();
        request.setOperations(new ArrayList<>(List.of(operations)));
        client.groups().patch(groupId, request);
    }

    static String members(String... ids) {
        return Set.of(ids).stream().map(id -> "{\"value\":\"" + id + "\"}").collect(Collectors.joining(",", "[", "]"));
    }

    static Set<String> memberIds(Group group) {
        return group.getMembers() == null ? Set.of() : group.getMembers().stream().map(Member::getValue).collect(Collectors.toSet());
    }

    static ScimClientException assertStatus(int status, Runnable request) {
        try {
            request.run();
        } catch (ScimClientException sce) {
            assertNotNull(sce.getError(), "expected a SCIM error body");
            assertEquals(status, sce.getError().getStatusInt(), () -> "detail: " + sce.getError().getDetail());
            return sce;
        }
        fail("expected HTTP " + status);
        return null;
    }

    static void assertManagedByAnotherClient(Runnable request) {
        ScimClientException sce = assertStatus(403, request);
        assertEquals(MANAGED_BY_ANOTHER_CLIENT, sce.getError().getDetail());
    }

    static void assertMutability(Runnable request) {
        ScimClientException sce = assertStatus(400, request);
        assertEquals("mutability", sce.getError().getScimType());
    }
}
