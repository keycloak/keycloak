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
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class GroupResourceTypeFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @BeforeEach
    public void onBeforeEach() {
        for (int i = 0; i < 50; i++) {
            GroupRepresentation group = new GroupRepresentation();

            group.setName("group-" + i);

            try (Response response = realm.admin().groups().add(group)) {
                group.setId(ApiUtil.getCreatedId(response));
            }

            GroupResource groupResource = realm.admin().groups().group(group.getId());

            for (int j = 0; j < 5; j++) {
                GroupRepresentation subGroup = new GroupRepresentation();

                subGroup.setName("subgroup-" + i + "." + j);

                groupResource.subGroup(subGroup).close();
            }
        }
    }

    @Test
    public void testViewAllGroupsUsingUserPolicy() {
        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, GROUPS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    public void testDeniedResourcesPrecedenceOverGrantedResources() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, GROUPS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> notAllowedGroups = search.stream()
                .filter((g) -> Set.of("group-0", "group-15", "group-30", "group-45").contains(g.getName()))
                .map(GroupRepresentation::getId)
                .collect(Collectors.toSet());
        assertFalse(notAllowedGroups.isEmpty());
        createPermission(client, notAllowedGroups, GROUPS_RESOURCE_TYPE, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertFalse(search.isEmpty());
        assertTrue(search.stream().map(GroupRepresentation::getId).noneMatch(notAllowedGroups::contains));
    }

    @Test
    public void testFilterSubGroups() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, GROUPS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName()).groups().groups("group-0", -1, -1);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());

        GroupRepresentation parentGroup = search.get(0);
        assertEquals(5, parentGroup.getSubGroups().size());
        assertEquals(5, parentGroup.getSubGroupCount());
        GroupRepresentation subGroup = parentGroup.getSubGroups().stream().filter(group -> group.getName().equals("subgroup-0.0")).findFirst().orElse(null);
        assertNotNull(subGroup);

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, subGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).groups().groups("subgroup-0.0", -1, -1);
        assertTrue(search.isEmpty());

        List<GroupRepresentation> subGroups = realmAdminClient.realm(realm.getName()).groups().group(parentGroup.getId()).getSubGroups(-1, -1, false);
        assertEquals(4, subGroups.size());
        assertTrue(subGroups.stream().map(GroupRepresentation::getId).noneMatch(subGroup.getId()::equals));
        search = realmAdminClient.realm(realm.getName()).groups().groups("group-0", -1, -1);
        assertFalse(search.isEmpty());
        parentGroup = search.get(0);
        assertEquals(4, parentGroup.getSubGroups().size());
        assertEquals(4, parentGroup.getSubGroupCount());

        subGroups = realmAdminClient.realm(realm.getName()).groups().group(parentGroup.getId()).getSubGroups(subGroup.getName(), true, -1, -1, true);
        assertTrue(subGroups.isEmpty());
        subGroups = realmAdminClient.realm(realm.getName()).groups().group(parentGroup.getId()).getSubGroups("subgroup-0.1", true, -1, -1, true);
        assertEquals(1, subGroups.size());

        assertEquals(5, realm.admin().groups().group(parentGroup.getId()).getSubGroups(-1, -1, false).size());
        assertEquals(5, realm.admin().groups().group(parentGroup.getId()).getSubGroups(null, false, -1, -1, false).size());
        assertEquals(5, realm.admin().groups().group(parentGroup.getId()).toRepresentation().getSubGroupCount());
    }

    @Test
    public void testGetUserGroups() {
        GroupRepresentation parentGroup = realm.admin().groups().groups("group-0", -1, -1).get(0);
        GroupRepresentation subGroup = realm.admin().groups().groups("subgroup-1.0", -1, -1).get(0);

        userAlice.admin().joinGroup(parentGroup.getId());
        userAlice.admin().joinGroup(subGroup.getId());

        List<GroupRepresentation> groups = userAlice.admin().groups();
        assertEquals(2, groups.size());

        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, userAlice.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createPermission(client, subGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW), policy);

        groups = realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).groups();
        assertEquals(1, groups.size());

        groups = userAlice.admin().groups();
        assertEquals(2, groups.size());
    }
}
