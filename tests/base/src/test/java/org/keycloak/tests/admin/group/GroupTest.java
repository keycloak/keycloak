/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.group;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class GroupTest extends AbstractGroupTest {

    @InjectRealm(config = GroupTestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    
    @Test
    @DisabledForDatabases("mssql")
    public void createMultiDeleteMultiReadMulti() {
        // create multiple groups
        List<String> groupUuuids = new ArrayList<>();
        IntStream.range(0, 100).forEach(groupIndex -> {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("Test Group " + groupIndex);
            try (Response response = managedRealm.admin().groups().add(group)) {
                boolean created = response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
                if (created) {
                    final String groupUuid = ApiUtil.getCreatedId(response);
                    groupUuuids.add(groupUuid);
                } else {
                    fail("Failed to create group: " + response.getStatusInfo().getReasonPhrase());
                }
            }
        });

        AtomicBoolean deletedAll = new AtomicBoolean(false);
        List<Exception> caughtExceptions = new CopyOnWriteArrayList<>();
        // read groups in a separate thread
        new Thread(() -> {
            while (!deletedAll.get()) {
                try {
                    // just loading briefs
                    managedRealm.admin().groups().groups(null, 0, Integer.MAX_VALUE, true);
                } catch (Exception e) {

                    caughtExceptions.add(e);
                }
            }
        }).start();

        // delete groups
        groupUuuids.forEach(groupUuid -> {
            managedRealm.admin().groups().group(groupUuid).remove();
        });
        deletedAll.set(true);

        assertThat(caughtExceptions, Matchers.empty());
    }

    // KEYCLOAK-2716 Can't delete client if its role is assigned to a group
    @Test
    public void testClientRemoveWithClientRoleGroupMapping() {
        RealmResource realm = managedRealm.admin();
        ClientRepresentation client = ClientConfigBuilder.create()
                .clientId("foo")
                .rootUrl("http://foo")
                .protocol("openid-connect")
                .build();
        Response response = managedRealm.admin().clients().create(client);
        String clientUuid = ApiUtil.getCreatedId(response);
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourcePath(AdminEventPaths.clientResourcePath(clientUuid))
                .representation(client)
                .resourceType(ResourceType.CLIENT);
        client = realm.clients().findByClientId("foo").get(0);

        RoleRepresentation role = RoleConfigBuilder.create().name("foo-role").build();
        realm.clients().get(client.getId()).roles().create(role);
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourcePath(AdminEventPaths.clientRoleResourcePath(clientUuid, "foo-role"))
                .representation(role)
                .resourceType(ResourceType.CLIENT_ROLE);
        role = realm.clients().get(clientUuid).roles().get("foo-role").toRepresentation();

        GroupRepresentation group = GroupConfigBuilder.create().name("2716").build();
        createGroup(managedRealm, group);

        List<RoleRepresentation> list = new LinkedList<>();
        list.add(role);
        realm.groups().group(group.getId()).roles().clientLevel(client.getId()).add(list);
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourcePath(AdminEventPaths.groupRolesClientRolesPath(group.getId(), clientUuid))
                .representation(list)
                .resourceType(ResourceType.CLIENT_ROLE_MAPPING);

        realm.clients().get(client.getId()).remove();
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourcePath(AdminEventPaths.clientResourcePath(clientUuid))
                .resourceType(ResourceType.CLIENT);
    }

    @Test
    // KEYCLOAK-16888 Error messages for groups with same name in the same level
    public void doNotAllowSameGroupNameAtSameLevel() {
        RealmResource realm = managedRealm.admin();

        GroupRepresentation topGroup = new GroupRepresentation();
        topGroup.setName("top");
        createGroup(managedRealm, topGroup);

        GroupRepresentation anotherTopGroup = new GroupRepresentation();
        anotherTopGroup.setName("top");
        Response response = realm.groups().add(anotherTopGroup);
        assertSameNameNotAllowed(response, "Top level group named 'top' already exists.");
        response.close();

        // allow moving the group to top level (nothing is done)
        response = realm.groups().add(topGroup);
        assertEquals(Response.Status.NO_CONTENT, response.getStatusInfo());
        response.close();

        GroupRepresentation level2Group = new GroupRepresentation();
        level2Group.setName("level2");
        response = realm.groups().group(topGroup.getId()).subGroup(level2Group);
        level2Group.setId(ApiUtil.getCreatedId(response));
        response.close();

        GroupRepresentation anotherlevel2Group = new GroupRepresentation();
        anotherlevel2Group.setName("level2");
        response = realm.groups().group(topGroup.getId()).subGroup(anotherlevel2Group);
        assertSameNameNotAllowed(response, "Sibling group named 'level2' already exists.");
        response.close();

        // allow moving the group to the same parent (nothing is done)
        response = realm.groups().group(topGroup.getId()).subGroup(level2Group);
        assertEquals(Response.Status.NO_CONTENT, response.getStatusInfo());
        response.close();
    }

    @Test
    // KEYCLOAK-11412 Unintended Groups with same names
    public void doNotAllowSameGroupNameAtSameLevelWhenUpdatingName() {
        RealmResource realm = managedRealm.admin();

        GroupRepresentation topGroup1 = GroupConfigBuilder.create().name("top1").build();
        createGroup(managedRealm, topGroup1);

        GroupRepresentation topGroup2 = GroupConfigBuilder.create().name("top2").build();
        createGroup(managedRealm, topGroup2);

        topGroup2.setName("top1");

        // conflict status 409 - same name not allowed
        ClientErrorException ex1 = Assertions.assertThrows(ClientErrorException.class, () -> realm.groups().group(topGroup2.getId()).update(topGroup2));
        assertSameNameNotAllowed(ex1.getResponse(), "Sibling group named 'top1' already exists.");


        GroupRepresentation level2Group = new GroupRepresentation();
        level2Group.setName("level2-1");
        addSubGroup(managedRealm, topGroup1, level2Group);

        GroupRepresentation anotherlevel2Group = new GroupRepresentation();
        anotherlevel2Group.setName("level2-2");
        addSubGroup(managedRealm, topGroup1, anotherlevel2Group);

        anotherlevel2Group.setName("level2-1");

        // conflict status 409 - same name not allowed
        ClientErrorException ex2 = Assertions.assertThrows(ClientErrorException.class, () -> realm.groups().group(anotherlevel2Group.getId()).update(anotherlevel2Group));
        assertSameNameNotAllowed(ex2.getResponse(), "Sibling group named 'level2-1' already exists.");

    }

    @Test
    public void allowSameGroupNameAtDifferentLevel() {
        RealmResource realm = managedRealm.admin();

        // creating "/test-group"
        GroupRepresentation topGroup = new GroupRepresentation();
        topGroup.setName("test-group");
        createGroup(managedRealm, topGroup);

        // creating "/test-group/test-group"
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("test-group");
        try (Response response = realm.groups().group(topGroup.getId()).subGroup(childGroup)) {
            assertEquals(201, response.getStatus());
        }

        assertNotNull(realm.getGroupByPath("/test-group/test-group"));
    }

    @Test
    public void doNotAllowSameGroupNameAtTopLevel() {
        // creating "/test-group"
        GroupRepresentation topGroup = new GroupRepresentation();
        topGroup.setName("test-group");
        createGroup(managedRealm, topGroup);

        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("test-group");
        try (Response response = managedRealm.admin().groups().add(group2)) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void doNotAllowSameGroupNameAtTopLevelInDatabase() {
        String realmName = managedRealm.getName();
        final String id = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            GroupModel g = realm.createGroup("test-group");
            return g.getId();
        }, String.class);
        managedRealm.cleanup().add(r -> r.groups().group(id).remove());
        // unique key should work even in top groups
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            Assertions.assertThrows(
                    ModelDuplicateException.class,
                    () -> realm.createGroup("test-group")
            );
        });
    }


    // KEYCLOAK-17581 Empty group names are not rejected by Admin API
    @Test
    public void createGroupWithEmptyNameShouldFail() {
        GroupRepresentation group = new GroupRepresentation();
        group.setName("");
        try (Response response = managedRealm.admin().groups().add(group)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        group.setName(null);
        try (Response response = managedRealm.admin().groups().add(group)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    // KEYCLOAK-17581 Empty group names are not rejected by Admin API
    @Test
    public void updatingGroupWithEmptyNameShouldFail() {
        RealmResource realm = managedRealm.admin();

        GroupRepresentation group = new GroupRepresentation();
        group.setName("groupWithName");

        Response response = realm.groups().add(group);
        String groupId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.groups().group(groupId).remove());

        group.setName("");
        Assertions.assertThrows(BadRequestException.class,
                () -> realm.groups().group(groupId).update(group),
                "Updating a group with empty name should fail"
        );

        group.setName(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> realm.groups().group(groupId).update(group),
                "Updating a group with null name should fail"
        );
    }

    @Test
    public void createAndTestGroups() throws IOException {
        RoleRepresentation topRole = createRealmRole(managedRealm, RoleConfigBuilder.create().name("topRole").build());
        RoleRepresentation level2Role = createRealmRole(managedRealm, RoleConfigBuilder.create().name("level2Role").build());
        RoleRepresentation level3Role = createRealmRole(managedRealm, RoleConfigBuilder.create().name("level3Role").build());

        // Role events tested elsewhere
        adminEvents.skipAll();
        RealmResource realm = managedRealm.admin();

        GroupRepresentation topGroup = new GroupRepresentation();
        topGroup.setName("top");
        String topGroupId = ApiUtil.getCreatedId(realm.groups().add(topGroup));
        topGroup.setId(topGroupId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupPath(topGroupId), topGroup, ResourceType.GROUP);

        List<RoleRepresentation> roles = new LinkedList<>(List.of(topRole));
        realm.groups().group(topGroup.getId()).roles().realmLevel().add(roles);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupRolesRealmRolesPath(topGroup.getId()), roles, ResourceType.REALM_ROLE_MAPPING);

        GroupRepresentation level2Group = new GroupRepresentation();
        level2Group.setName("level2");
        Response response = realm.groups().group(topGroup.getId()).subGroup(level2Group);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupSubgroupsPath(topGroup.getId()), level2Group, ResourceType.GROUP);
        URI location = response.getLocation();
        final String level2Id = ApiUtil.getCreatedId(response);

        final GroupRepresentation level2GroupById = realm.groups().group(level2Id).toRepresentation();
        assertEquals(level2Id, level2GroupById.getId());
        assertEquals(level2Group.getName(), level2GroupById.getName());

        HttpGet httpGet = new HttpGet(location);
        httpGet.setHeader("Authorization", "Bearer " + adminClient.tokenManager().getAccessTokenString());

        CloseableHttpResponse getResponse = httpClient.execute(httpGet);
        assertEquals(200, getResponse.getStatusLine().getStatusCode());
        GroupRepresentation level2 = JsonSerialization.readValue(getResponse.getEntity().getContent(), GroupRepresentation.class);
        getResponse.close();
        assertEquals(level2Id, level2.getId());

        level2Group = realm.getGroupByPath("/top/level2");
        assertNotNull(level2Group);
        roles.clear();
        roles.add(level2Role);
        realm.groups().group(level2Group.getId()).roles().realmLevel().add(roles);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupRolesRealmRolesPath(level2Group.getId()), roles, ResourceType.REALM_ROLE_MAPPING);

        GroupRepresentation level3Group = new GroupRepresentation();
        level3Group.setName("level3");
        response = realm.groups().group(level2Group.getId()).subGroup(level3Group);
        response.close();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupSubgroupsPath(level2Group.getId()), level3Group, ResourceType.GROUP);

        level3Group = realm.getGroupByPath("/top/level2/level3");
        assertNotNull(level3Group);
        roles.clear();
        roles.add(level3Role);
        realm.groups().group(level3Group.getId()).roles().realmLevel().add(roles);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupRolesRealmRolesPath(level3Group.getId()), roles, ResourceType.REALM_ROLE_MAPPING);

        topGroup = realm.getGroupByPath("/top");
        assertEquals(1, topGroup.getRealmRoles().size());
        assertTrue(topGroup.getRealmRoles().contains("topRole"));
        assertEquals(1, realm.groups().group(topGroup.getId()).getSubGroups(0, null, false).size());

        level2Group = realm.getGroupByPath("/top/level2");
        assertEquals("level2", level2Group.getName());
        assertEquals(1, level2Group.getRealmRoles().size());
        assertTrue(level2Group.getRealmRoles().contains("level2Role"));
        assertEquals(1, realm.groups().group(level2Group.getId()).getSubGroups(0, null, false).size());

        level3Group = realm.getGroupByPath("/top/level2/level3");
        assertEquals("level3", level3Group.getName());
        assertEquals(1, level3Group.getRealmRoles().size());
        assertTrue(level3Group.getRealmRoles().contains("level3Role"));

        UserRepresentation user = realm.users().search("direct-login", -1, -1).get(0);
        realm.users().get(user.getId()).joinGroup(level3Group.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userGroupPath(user.getId(), level3Group.getId()), ResourceType.GROUP_MEMBERSHIP);

        List<GroupRepresentation> membership = realm.users().get(user.getId()).groups();
        assertEquals(1, membership.size());
        assertEquals("level3", membership.get(0).getName());

        AccessToken token = login("direct-login", "resource-owner", "secret");
        assertTrue(token.getRealmAccess().getRoles().contains("topRole"));
        assertTrue(token.getRealmAccess().getRoles().contains("level2Role"));
        assertTrue(token.getRealmAccess().getRoles().contains("level3Role"));

        realm.addDefaultGroup(level3Group.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.defaultGroupPath(level3Group.getId()), ResourceType.GROUP);

        List<GroupRepresentation> defaultGroups = realm.getDefaultGroups();
        assertEquals(1, defaultGroups.size());
        assertEquals(defaultGroups.get(0).getId(), level3Group.getId());

        UserRepresentation newUser = UserConfigBuilder.create()
                .username("groupUser")
                .email("group@group.com")
                .build();
        response = realm.users().create(newUser);
        String userId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.users().get(userId).remove());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userResourcePath(userId), newUser, ResourceType.USER);

        membership = realm.users().get(userId).groups();
        assertEquals(1, membership.size());
        assertEquals("level3", membership.get(0).getName());

        realm.removeDefaultGroup(level3Group.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.defaultGroupPath(level3Group.getId()), ResourceType.GROUP);

        defaultGroups = realm.getDefaultGroups();
        assertEquals(0, defaultGroups.size());

        realm.groups().group(topGroup.getId()).remove();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.groupPath(topGroup.getId()), ResourceType.GROUP);

        Assertions.assertThrows(
                NotFoundException.class,
                () -> realm.getGroupByPath("/top/level2/level3"),
                "Group should not have been found"
        );

        Assertions.assertThrows(
                NotFoundException.class,
                () -> realm.getGroupByPath("/top/level2"),
                "Group should not have been found"
        );

        Assertions.assertThrows(
                NotFoundException.class,
                () -> realm.getGroupByPath("/top"),
                "Group should not have been found"
        );

        assertNull(login("direct-login", "resource-owner", "secret").getRealmAccess());
    }

    @Test
    public void updateGroup() {
        RealmResource realm = managedRealm.admin();
        final String groupName = "group-" + UUID.randomUUID();

        GroupRepresentation group = GroupConfigBuilder.create()
                .name(groupName)
                .attribute("attr1", "attrval1")
                .attribute("attr2", "attrval2")
                .build();
        createGroup(managedRealm, group);
        group = realm.getGroupByPath("/" + groupName);

        assertNotNull(group);
        assertThat(group.getName(), is(groupName));
        assertThat(group.getAttributes().keySet(), containsInAnyOrder("attr1", "attr2"));
        assertThat(group.getAttributes(), hasEntry(is("attr1"), contains("attrval1")));
        assertThat(group.getAttributes(), hasEntry(is("attr2"), contains("attrval2")));

        final String groupNewName = "group-" + UUID.randomUUID();
        group.setName(groupNewName);

        group.getAttributes().remove("attr1");
        group.getAttributes().get("attr2").add("attrval2-2");
        group.getAttributes().put("attr3", List.of("attrval2"));

        realm.groups().group(group.getId()).update(group);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.groupPath(group.getId()), group, ResourceType.GROUP);

        group = realm.getGroupByPath("/" + groupNewName);

        assertThat(group.getName(), is(groupNewName));
        assertThat(group.getAttributes().keySet(), containsInAnyOrder("attr2", "attr3"));
        assertThat(group.getAttributes(), hasEntry(is("attr2"), containsInAnyOrder("attrval2", "attrval2-2")));
        assertThat(group.getAttributes(), hasEntry(is("attr3"), contains("attrval2")));
    }

    @Test
    public void moveGroups() {
        RealmResource realm = managedRealm.admin();

        // Create 2 top level groups "mygroup1" and "mygroup2"
        GroupRepresentation group1 = GroupConfigBuilder.create()
                .name("mygroup1")
                .build();
        createGroup(managedRealm, group1);

        GroupRepresentation group2 = GroupConfigBuilder.create()
                .name("mygroup2")
                .build();
        createGroup(managedRealm, group2);

        // Move "mygroup2" as child of "mygroup1" . Assert it was moved
        Response response = realm.groups().group(group1.getId()).subGroup(group2);
        assertEquals(204, response.getStatus());
        response.close();

        // Assert "mygroup2" was moved
        List<GroupRepresentation> group1Children = realm.groups().group(group1.getId()).getSubGroups(0, 10, false);

        Assert.assertNames(group1Children, "mygroup2");
        assertEquals("/mygroup1/mygroup2", realm.groups().group(group2.getId()).toRepresentation().getPath());

        // Create top level group with the same name
        GroupRepresentation group3 = GroupConfigBuilder.create()
                .name("mygroup2")
                .build();
        response = managedRealm.admin().groups().add(group3);
        String group3Id = ApiUtil.getCreatedId(response);
        group3.setId(group3Id);
        // Try to move top level "mygroup2" as child of "mygroup1". It should fail as there is already a child group
        // of "mygroup1" with name "mygroup2"
        response = realm.groups().group(group1.getId()).subGroup(group3);
        assertEquals(409, response.getStatus());
        realm.groups().group(group3.getId()).remove();

        // Move "mygroup2" back under parent
        response = realm.groups().add(group2);
        assertEquals(204, response.getStatus());
        response.close();

        // Assert "mygroup2" was moved
        group1Children = realm.groups().group(group1.getId()).getSubGroups(0, 10, false);
        assertEquals(0, group1Children.size());
        assertEquals("/mygroup2", realm.groups().group(group2.getId()).toRepresentation().getPath());

        // Move "mygroup2" back under parent for cleanup
        realm.groups().add(group2);
    }

    @Test
    public void groupMembership() {
        UsersResource users = managedRealm.admin().users();

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("group");
        String groupId = createGroup(managedRealm, groupRep);
        GroupResource group = managedRealm.admin().groups().group(groupId);

        Response response = users.create(UserConfigBuilder.create().username("user-a").build());
        String userAId = ApiUtil.getCreatedId(response);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userResourcePath(userAId), ResourceType.USER);

        response = users.create(UserConfigBuilder.create().username("user-b").build());
        String userBId = ApiUtil.getCreatedId(response);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userResourcePath(userBId), ResourceType.USER);

        users.get(userAId).joinGroup(groupId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userGroupPath(userAId, groupId), groupRep, ResourceType.GROUP_MEMBERSHIP);

        List<UserRepresentation> members = group.members(0, 10);
        Assert.assertNames(members, "user-a");

        users.get(userBId).joinGroup(groupId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userGroupPath(userBId, groupId), groupRep, ResourceType.GROUP_MEMBERSHIP);

        members = group.members(0, 10);
        Assert.assertNames(members, "user-a", "user-b");

        users.get(userAId).leaveGroup(groupId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userGroupPath(userAId, groupId), groupRep, ResourceType.GROUP_MEMBERSHIP);

        members = group.members(0, 10);
        Assert.assertNames(members, "user-b");

        List<GroupRepresentation> groupsList = users.get(userAId).groups(null, null);
        Assert.assertNames(groupsList);
    }


    @Test
    //KEYCLOAK-6300 List of group members is not sorted alphabetically
    public void groupMembershipUsersOrder() {
        RealmResource realm = managedRealm.admin();

        GroupRepresentation group = new GroupRepresentation();
        group.setName("group");
        String groupId = createGroup(managedRealm, group);

        List<String> usernames = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            UserRepresentation user = UserConfigBuilder.create().username("user" + i).build();
            usernames.add(user.getUsername());

            Response create = realm.users().create(user);
            String userId = ApiUtil.getCreatedId(create);
            realm.users().get(userId).joinGroup(groupId);

        }

        List<String> memberUsernames = new ArrayList<>();
        for (UserRepresentation member : realm.groups().group(groupId).members(0, 10)) {
            memberUsernames.add(member.getUsername());
        }
        assertArrayEquals(
                usernames.toArray(), memberUsernames.toArray(),
                "Expected: " + usernames + ", was: " + memberUsernames
        );
    }

    @Test
    // KEYCLOAK-2700 Import existing realm fails due to can't delete group
    public void deleteRealmWithDefaultGroups() {
        RealmConfigBuilder realmConfigBuilder = RealmConfigBuilder.create()
                .name("foo")
                .defaultGroups("/default1");
        realmConfigBuilder.addGroup("default1").path("/default1");
        RealmRepresentation rep = realmConfigBuilder.build();

        adminClient.realms().create(rep);

        adminClient.realm(rep.getRealm()).remove();
    }

    @Test
    public void roleMappings() {
        RealmResource realm = managedRealm.admin();
        createRealmRole(managedRealm, RoleConfigBuilder.create().name("realm-role").build());
        createRealmRole(managedRealm, RoleConfigBuilder.create().name("realm-composite").build());
        createRealmRole(managedRealm, RoleConfigBuilder.create().name("realm-child").build());
        realm.roles().get("realm-composite").addComposites(List.of(realm.roles().get("realm-child").toRepresentation()));

        Response response = realm.clients().create(ClientConfigBuilder.create().clientId("myclient").build());
        String clientId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(clientId).remove());

        realm.clients().get(clientId).roles().create(RoleConfigBuilder.create().name("client-role").build());
        realm.clients().get(clientId).roles().create(RoleConfigBuilder.create().name("client-role2").build());
        realm.clients().get(clientId).roles().create(RoleConfigBuilder.create().name("client-composite").build());
        realm.clients().get(clientId).roles().create(RoleConfigBuilder.create().name("client-child").build());
        realm.clients().get(clientId).roles().get("client-composite").addComposites(List.of(realm.clients().get(clientId).roles().get("client-child").toRepresentation()));

        // Roles+clients tested elsewhere
        adminEvents.skipAll();

        GroupRepresentation group = new GroupRepresentation();
        group.setName("group");
        String groupId = createGroup(managedRealm, group);

        RoleMappingResource roles = realm.groups().group(groupId).roles();
        assertEquals(0, roles.realmLevel().listAll().size());

        // Add realm roles
        List<RoleRepresentation> l = new LinkedList<>();
        l.add(realm.roles().get("realm-role").toRepresentation());
        l.add(realm.roles().get("realm-composite").toRepresentation());
        roles.realmLevel().add(l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupRolesRealmRolesPath(group.getId()), l, ResourceType.REALM_ROLE_MAPPING);

        // Add client roles
        RoleRepresentation clientRole = realm.clients().get(clientId).roles().get("client-role").toRepresentation();
        RoleRepresentation clientComposite = realm.clients().get(clientId).roles().get("client-composite").toRepresentation();
        roles.clientLevel(clientId).add(List.of(clientRole));
        roles.clientLevel(clientId).add(List.of(clientComposite));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupRolesClientRolesPath(group.getId(), clientId), List.of(clientRole), ResourceType.CLIENT_ROLE_MAPPING);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupRolesClientRolesPath(group.getId(), clientId), List.of(clientComposite), ResourceType.CLIENT_ROLE_MAPPING);

        // List realm roles
        Assert.assertNames(roles.realmLevel().listAll(), "realm-role", "realm-composite");
        Assert.assertNames(roles.realmLevel().listAvailable(), "realm-child", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        Assert.assertNames(roles.realmLevel().listEffective(), "realm-role", "realm-composite", "realm-child");

        // List client roles
        Assert.assertNames(roles.clientLevel(clientId).listAll(), "client-role", "client-composite");
        Assert.assertNames(roles.clientLevel(clientId).listAvailable(), "client-role2", "client-child");
        Assert.assertNames(roles.clientLevel(clientId).listEffective(), "client-role", "client-composite", "client-child");

        // Get mapping representation
        MappingsRepresentation all = roles.getAll();
        Assert.assertNames(all.getRealmMappings(), "realm-role", "realm-composite");
        assertEquals(1, all.getClientMappings().size());
        Assert.assertNames(all.getClientMappings().get("myclient").getMappings(), "client-role", "client-composite");

        // Remove realm role
        RoleRepresentation realmRoleRep = realm.roles().get("realm-role").toRepresentation();
        roles.realmLevel().remove(List.of(realmRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.groupRolesRealmRolesPath(group.getId()), List.of(realmRoleRep), ResourceType.REALM_ROLE_MAPPING);
        Assert.assertNames(roles.realmLevel().listAll(), "realm-composite");

        // Remove client role
        RoleRepresentation clientRoleRep = realm.clients().get(clientId).roles().get("client-role").toRepresentation();
        roles.clientLevel(clientId).remove(List.of(clientRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.groupRolesClientRolesPath(group.getId(), clientId), List.of(clientRoleRep), ResourceType.CLIENT_ROLE_MAPPING);
        Assert.assertNames(roles.clientLevel(clientId).listAll(), "client-composite");
    }

    /**
     * Test for KEYCLOAK-10603.
     */
    @Test
    public void rolesCanBeAssignedEvenWhenTheyAreAlreadyIndirectlyAssigned() {
        RealmResource realm = managedRealm.admin();

        createRealmRole(managedRealm, RoleConfigBuilder.create().name("realm-composite").build());
        createRealmRole(managedRealm, RoleConfigBuilder.create().name("realm-child").build());
        realm.roles().get("realm-composite")
                .addComposites(List.of(realm.roles().get("realm-child").toRepresentation()));

        Response response = realm.clients().create(ClientConfigBuilder.create().clientId("myclient").build());
        String clientId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(clientId).remove());

        realm.clients().get(clientId).roles().create(RoleConfigBuilder.create().name("client-composite").build());
        realm.clients().get(clientId).roles().create(RoleConfigBuilder.create().name("client-child").build());
        realm.clients().get(clientId).roles().get("client-composite").addComposites(
                List.of(realm.clients().get(clientId).roles().get("client-child").toRepresentation())
        );

        GroupRepresentation group = new GroupRepresentation();
        group.setName("group");

        // Roles+clients tested elsewhere
        adminEvents.skipAll();

        String groupId = createGroup(managedRealm, group);

        RoleMappingResource roles = realm.groups().group(groupId).roles();
        // Make indirect assignments: assign composite roles
        roles.realmLevel()
                .add(List.of(realm.roles().get("realm-composite").toRepresentation()));
        RoleRepresentation clientComposite =
                realm.clients().get(clientId).roles().get("client-composite").toRepresentation();
        roles.clientLevel(clientId).add(List.of(clientComposite));

        // Check state before making the direct assignments
        Assert.assertNames(roles.realmLevel().listAll(), "realm-composite");
        Assert.assertNames(roles.realmLevel().listAvailable(), "realm-child", "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        Assert.assertNames(roles.realmLevel().listEffective(), "realm-composite", "realm-child");

        Assert.assertNames(roles.clientLevel(clientId).listAll(), "client-composite");
        Assert.assertNames(roles.clientLevel(clientId).listAvailable(), "client-child");
        Assert.assertNames(roles.clientLevel(clientId).listEffective(), "client-composite", "client-child");

        // Make direct assignments for roles which are already indirectly assigned
        roles.realmLevel().add(List.of(realm.roles().get("realm-child").toRepresentation()));
        RoleRepresentation clientChild =
                realm.clients().get(clientId).roles().get("client-child").toRepresentation();
        roles.clientLevel(clientId).add(List.of(clientChild));

        // List realm roles
        Assert.assertNames(roles.realmLevel().listAll(), "realm-composite", "realm-child");
        Assert.assertNames(roles.realmLevel().listAvailable(), "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        Assert.assertNames(roles.realmLevel().listEffective(), "realm-composite", "realm-child");

        // List client roles
        Assert.assertNames(roles.clientLevel(clientId).listAll(), "client-composite", "client-child");
        Assert.assertNames(roles.clientLevel(clientId).listAvailable());
        Assert.assertNames(roles.clientLevel(clientId).listEffective(), "client-composite", "client-child");

        // Get mapping representation
        MappingsRepresentation all = roles.getAll();
        Assert.assertNames(all.getRealmMappings(), "realm-composite", "realm-child");
        assertEquals(1, all.getClientMappings().size());
        Assert.assertNames(all.getClientMappings().get("myclient").getMappings(), "client-composite", "client-child");
    }

    @Test
    public void defaultMaxResults() {
        GroupsResource groups = managedRealm.admin().groups();
        Response response = groups.add(GroupConfigBuilder.create().name("test").build());
        String groupId = ApiUtil.getCreatedId(response);

        GroupResource group = groups.group(groupId);

        UsersResource users = managedRealm.admin().users();

        for (int i = 0; i < 110; i++) {
            Response resp = users.create(UserConfigBuilder.create().username("test-" + i).build());
            String userUuid = ApiUtil.getCreatedId(resp);
            users.get(userUuid).joinGroup(groupId);
            managedRealm.cleanup().add(r -> r.users().delete(userUuid).close());
        }

        assertEquals(100, group.members(null, null).size());
        assertEquals(100, group.members().size());
        assertEquals(105, group.members(0, 105).size());
        assertEquals(110, group.members(0, 1000).size());
        assertEquals(110, group.members(-1, -2).size());
    }

    @Test
    public void getGroupsWithBriefAndFullRepresentation() {
        GroupRepresentation group = GroupConfigBuilder.create()
                .name("groupWithAttribute")
                .attribute("attribute1", "attribute1", "attribute2")
                .build();
        createGroup(managedRealm, group);

        List<GroupRepresentation> groups;
        // brief representation
        groups = managedRealm.admin().groups().groups("groupWithAttribute", 0, 20);
        assertFalse(groups.isEmpty());
        assertNull(groups.get(0).getAttributes());
        // full representation
        groups = managedRealm.admin().groups().groups("groupWithAttribute", 0, 20, false);
        assertFalse(groups.isEmpty());
        assertTrue(groups.get(0).getAttributes().containsKey("attribute1"));
    }

    @Test
    public void getSubGroups() {
        GroupRepresentation parent = new GroupRepresentation();
        parent.setName("parent");
        createGroup(managedRealm, parent);

        GroupRepresentation child = GroupConfigBuilder.create()
                .name("child")
                .attribute("attribute1", "value1", "value2")
                .build();

        addSubGroup(managedRealm, parent, child);

        // Check brief and full retrieval of subgroups of parent
        GroupResource parentGroup = managedRealm.admin().groups().group(parent.getId());
        boolean briefRepresentation = true;
        assertNull(parentGroup.getSubGroups(null, null, briefRepresentation).get(0).getAttributes());

        briefRepresentation = false;
        assertThat(parentGroup.getSubGroups(null, null, briefRepresentation).get(0).getAttributes().get("attribute1"), containsInAnyOrder("value1", "value2"));
    }

    @Test
    public void removeAllGroupAttributes() {
        final GroupRepresentation groupToCreate = GroupConfigBuilder.create()
                .name("remove-all-attributes-group")
                .attribute("test-key", "test-val")
                .build();
        final GroupsResource groupsResource = managedRealm.admin().groups();
        final Response response = groupsResource.add(groupToCreate);
        final String groupId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.groups().group(groupId).remove());

        final GroupResource groupResource = groupsResource.group(groupId);
        final GroupRepresentation createdGroup = groupResource.toRepresentation();
        assertThat(createdGroup.getAttributes(), equalTo(Map.of("test-key", List.of("test-val"))));

        final GroupRepresentation groupToUpdate = GroupConfigBuilder.update(groupToCreate)
                .setAttributes(Map.of())
                .build();
        groupResource.update(groupToUpdate);

        final GroupRepresentation updatedGroup = groupResource.toRepresentation();
        assertThat(updatedGroup.getAttributes(), anEmptyMap());
    }

    @Test
    public void testBriefRepresentationOnGroupMembers() {
        RealmResource realm = managedRealm.admin();
        String groupName = "brief-grouptest-group";
        String userName = "brief-grouptest-user";

        // enable user profile unmanaged attributes
        UserProfileResource upResource = realm.users().userProfile();
        UPConfig cfg = UserProfileUtil.enableUnmanagedAttributes(upResource);
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourcePath(AdminEventPaths.userProfilePath())
                .resourceType(ResourceType.USER_PROFILE);

        try {
            String groupId = createGroup(managedRealm, GroupConfigBuilder.create().name(groupName).build());
            GroupResource group = managedRealm.admin().groups().group(groupId);

            UserRepresentation userRepresentation = UserConfigBuilder.create()
                    .username(userName)
                    .attribute("myattribute", "myvalue")
                    .build();

            UsersResource users = realm.users();
            Response response = users.create(userRepresentation);
            String userUuid = ApiUtil.getCreatedId(response);
            managedRealm.cleanup().add(r -> r.users().get(userUuid).remove());
            users.get(userUuid).joinGroup(groupId);

            UserRepresentation defaultRepresentation = group.members(null, null).get(0);
            UserRepresentation fullRepresentation = group.members(null, null, false).get(0);
            UserRepresentation briefRepresentation = group.members(null, null, true).get(0);

            assertEquals(userRepresentation.getAttributes(), fullRepresentation.getAttributes());
            assertEquals(userRepresentation.getAttributes(), defaultRepresentation.getAttributes());
            assertNull(briefRepresentation.getAttributes());
        } finally {
            cfg.setUnmanagedAttributePolicy(null);
            upResource.update(cfg);
        }
    }

    /**
     * Assert that when you create/move/update a group name, the response is not Http 409 Conflict and the message does not
     * correspond to the returned user-friendly message in such cases
     */
    private void assertSameNameNotAllowed(Response response, String expectedErrorMessage) {
        assertEquals(409, response.getStatus());
        ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
        Assertions.assertEquals(expectedErrorMessage, error.getErrorMessage());
    }

    private static class GroupTestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.eventsEnabled(true);

            realm.addUser("direct-login")
                    .name("Direct", "Login")
                    .email("direct-login@localhost")
                    .enabled(true)
                    .password("password");

            realm.addClient("resource-owner")
                    .directAccessGrantsEnabled(true)
                    .secret("secret");

            return realm;
        }
    }
}
