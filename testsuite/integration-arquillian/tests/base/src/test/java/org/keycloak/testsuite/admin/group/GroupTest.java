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

package org.keycloak.testsuite.admin.group;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.URLAssert;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class GroupTest extends AbstractGroupTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = loadTestRealm(testRealms);

        testRealmRep.setEventsEnabled(true);

        List<UserRepresentation> users = testRealmRep.getUsers();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("direct-login");
        user.setEmail("direct-login@localhost");
        user.setEnabled(true);
        List<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");
        credentials.add(credential);
        user.setCredentials(credentials);
        users.add(user);


        List<ClientRepresentation> clients = testRealmRep.getClients();

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("resource-owner");
        client.setDirectAccessGrantsEnabled(true);
        client.setSecret("secret");
        clients.add(client);
    }

    /**
     * KEYCLOAK-2716
     * @throws Exception
     */
    @Test
    public void testClientRemoveWithClientRoleGroupMapping() throws Exception {
        RealmResource realm = adminClient.realms().realm("test");
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("foo");
        client.setRootUrl("http://foo");
        client.setProtocol("openid-connect");
        Response response = realm.clients().create(client);
        response.close();
        client = realm.clients().findByClientId("foo").get(0);
        RoleRepresentation role = new RoleRepresentation();
        role.setName("foo-role");
        realm.clients().get(client.getId()).roles().create(role);
        role = realm.clients().get(client.getId()).roles().get("foo-role").toRepresentation();
        GroupRepresentation group = new GroupRepresentation();
        group.setName("2716");
        realm.groups().add(group).close();
        group = realm.getGroupByPath("/2716");
        List<RoleRepresentation> list = new LinkedList<>();
        list.add(role);
        realm.groups().group(group.getId()).roles().clientLevel(client.getId()).add(list);
        realm.clients().get(client.getId()).remove();

    }

    @Test
    public void createAndTestGroups() throws Exception {
        RealmResource realm = adminClient.realms().realm("test");
        {
            RoleRepresentation groupRole = new RoleRepresentation();
            groupRole.setName("topRole");
            realm.roles().create(groupRole);
        }
        RoleRepresentation topRole = realm.roles().get("topRole").toRepresentation();
        {
            RoleRepresentation groupRole = new RoleRepresentation();
            groupRole.setName("level2Role");
            realm.roles().create(groupRole);
        }
        RoleRepresentation level2Role = realm.roles().get("level2Role").toRepresentation();
        {
            RoleRepresentation groupRole = new RoleRepresentation();
            groupRole.setName("level3Role");
            realm.roles().create(groupRole);
        }
        RoleRepresentation level3Role = realm.roles().get("level3Role").toRepresentation();


        GroupRepresentation topGroup = new GroupRepresentation();
        topGroup.setName("top");
        Response response = realm.groups().add(topGroup);
        response.close();
        topGroup = realm.getGroupByPath("/top");
        Assert.assertNotNull(topGroup);
        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(topRole);
        realm.groups().group(topGroup.getId()).roles().realmLevel().add(roles);

        GroupRepresentation level2Group = new GroupRepresentation();
        level2Group.setName("level2");
        response = realm.groups().group(topGroup.getId()).subGroup(level2Group);
        response.close();

        URI location = response.getLocation();
        final String level2Id = ApiUtil.getCreatedId(response);
        final GroupRepresentation level2GroupById = realm.groups().group(level2Id).toRepresentation();
        assertEquals(level2Id, level2GroupById.getId());
        assertEquals(level2Group.getName(), level2GroupById.getName());

        URLAssert.assertGetURL(location, adminClient.tokenManager().getAccessTokenString(), new URLAssert.AssertJSONResponseHandler() {
            @Override
            protected void assertResponseBody(String body) throws IOException {
                GroupRepresentation level2 = JsonSerialization.readValue(body, GroupRepresentation.class);
                assertEquals(level2Id, level2.getId());
            }
        });

        level2Group = realm.getGroupByPath("/top/level2");
        Assert.assertNotNull(level2Group);
        roles.clear();
        roles.add(level2Role);
        realm.groups().group(level2Group.getId()).roles().realmLevel().add(roles);

        GroupRepresentation level3Group = new GroupRepresentation();
        level3Group.setName("level3");
        response = realm.groups().group(level2Group.getId()).subGroup(level3Group);
        response.close();
        level3Group = realm.getGroupByPath("/top/level2/level3");
        Assert.assertNotNull(level3Group);
        roles.clear();
        roles.add(level3Role);
        realm.groups().group(level3Group.getId()).roles().realmLevel().add(roles);

        topGroup = realm.getGroupByPath("/top");
        assertEquals(1, topGroup.getRealmRoles().size());
        assertTrue(topGroup.getRealmRoles().contains("topRole"));
        assertEquals(1, topGroup.getSubGroups().size());

        level2Group = topGroup.getSubGroups().get(0);
        assertEquals("level2", level2Group.getName());
        assertEquals(1, level2Group.getRealmRoles().size());
        assertTrue(level2Group.getRealmRoles().contains("level2Role"));
        assertEquals(1, level2Group.getSubGroups().size());

        level3Group = level2Group.getSubGroups().get(0);
        assertEquals("level3", level3Group.getName());
        assertEquals(1, level3Group.getRealmRoles().size());
        assertTrue(level3Group.getRealmRoles().contains("level3Role"));

        UserRepresentation user = realm.users().search("direct-login", -1, -1).get(0);
        realm.users().get(user.getId()).joinGroup(level3Group.getId());
        List<GroupRepresentation> membership = realm.users().get(user.getId()).groups();
        assertEquals(1, membership.size());
        assertEquals("level3", membership.get(0).getName());

        AccessToken token = login("direct-login", "resource-owner", "secret", user.getId());
        assertTrue(token.getRealmAccess().getRoles().contains("topRole"));
        assertTrue(token.getRealmAccess().getRoles().contains("level2Role"));
        assertTrue(token.getRealmAccess().getRoles().contains("level3Role"));

        realm.addDefaultGroup(level3Group.getId());

        List<GroupRepresentation> defaultGroups = realm.getDefaultGroups();
        assertEquals(1, defaultGroups.size());
        assertEquals(defaultGroups.get(0).getId(), level3Group.getId());

        UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername("groupUser");
        newUser.setEmail("group@group.com");
        response = realm.users().create(newUser);
        response.close();
        newUser =  realm.users().search("groupUser", -1, -1).get(0);
        membership = realm.users().get(newUser.getId()).groups();
        assertEquals(1, membership.size());
        assertEquals("level3", membership.get(0).getName());

        realm.removeDefaultGroup(level3Group.getId());
        defaultGroups = realm.getDefaultGroups();
        assertEquals(0, defaultGroups.size());

        realm.groups().group(topGroup.getId()).remove();

        try {
            realm.getGroupByPath("/top/level2/level3");
            Assert.fail("Group should not have been found");
        }
        catch (NotFoundException e) {}

        try {
            realm.getGroupByPath("/top/level2");
            Assert.fail("Group should not have been found");
        }
        catch (NotFoundException e) {}

        try {
            realm.getGroupByPath("/top");
            Assert.fail("Group should not have been found");
        }
        catch (NotFoundException e) {}

        Assert.assertNull(login("direct-login", "resource-owner", "secret", user.getId()).getRealmAccess());
    }

    @Test
    public void updateGroup() {
        RealmResource realm = adminClient.realms().realm("test");

        GroupRepresentation group = new GroupRepresentation();
        group.setName("group");

        Map<String, List<String>> attrs = new HashMap<>();
        attrs.put("attr1", Collections.singletonList("attrval1"));
        attrs.put("attr2", Collections.singletonList("attrval2"));
        group.setAttributes(attrs);

        Response response = realm.groups().add(group);
        response.close();
        group = realm.getGroupByPath("/group");
        Assert.assertNotNull(group);
        assertEquals("group", group.getName());
        assertEquals(2, group.getAttributes().size());
        assertEquals(1, group.getAttributes().get("attr1").size());
        assertEquals("attrval1", group.getAttributes().get("attr1").get(0));
        assertEquals(1, group.getAttributes().get("attr2").size());
        assertEquals("attrval2", group.getAttributes().get("attr2").get(0));

        group.setName("group-new");

        group.getAttributes().remove("attr1");
        group.getAttributes().get("attr2").add("attrval2-2");
        group.getAttributes().put("attr3", Collections.singletonList("attrval2"));

        realm.groups().group(group.getId()).update(group);

        group = realm.getGroupByPath("/group-new");

        assertEquals("group-new", group.getName());
        assertEquals(2, group.getAttributes().size());
        assertEquals(2, group.getAttributes().get("attr2").size());
        assertEquals(1, group.getAttributes().get("attr3").size());
    }

    @Test
    public void groupMembership() {
        RealmResource realm = adminClient.realms().realm("test");

        GroupRepresentation group = new GroupRepresentation();
        group.setName("group");
        Response response = realm.groups().add(group);
        String groupId = ApiUtil.getCreatedId(response);
        response.close();

        response = realm.users().create(UserBuilder.create().username("user-a").build());
        String userAId = ApiUtil.getCreatedId(response);
        response.close();

        response = realm.users().create(UserBuilder.create().username("user-b").build());
        String userBId = ApiUtil.getCreatedId(response);
        response.close();

        realm.users().get(userAId).joinGroup(groupId);

        List<UserRepresentation> members = realm.groups().group(groupId).members(0, 10);
        org.keycloak.testsuite.Assert.assertNames(members, "user-a");

        realm.users().get(userBId).joinGroup(groupId);

        members = realm.groups().group(groupId).members(0, 10);
        org.keycloak.testsuite.Assert.assertNames(members, "user-a", "user-b");

        realm.users().get(userAId).leaveGroup(groupId);

        members = realm.groups().group(groupId).members(0, 10);
        org.keycloak.testsuite.Assert.assertNames(members, "user-b");
    }

    @Test
    // KEYCLOAK-2700
    public void deleteRealmWithDefaultGroups() throws IOException {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("foo");

        GroupRepresentation group = new GroupRepresentation();
        group.setName("default1");
        group.setPath("/default1");

        rep.setGroups(Collections.singletonList(group));
        rep.setDefaultGroups(Collections.singletonList("/default1"));

        adminClient.realms().create(rep);

        adminClient.realm(rep.getRealm()).remove();
    }
}
