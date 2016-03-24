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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


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
        Assert.assertEquals(1, topGroup.getRealmRoles().size());
        Assert.assertTrue(topGroup.getRealmRoles().contains("topRole"));
        Assert.assertEquals(1, topGroup.getSubGroups().size());

        level2Group = topGroup.getSubGroups().get(0);
        Assert.assertEquals("level2", level2Group.getName());
        Assert.assertEquals(1, level2Group.getRealmRoles().size());
        Assert.assertTrue(level2Group.getRealmRoles().contains("level2Role"));
        Assert.assertEquals(1, level2Group.getSubGroups().size());

        level3Group = level2Group.getSubGroups().get(0);
        Assert.assertEquals("level3", level3Group.getName());
        Assert.assertEquals(1, level3Group.getRealmRoles().size());
        Assert.assertTrue(level3Group.getRealmRoles().contains("level3Role"));

        try {
            GroupRepresentation notFound = realm.getGroupByPath("/notFound");
            Assert.fail();
        } catch (NotFoundException e) {

        }
        try {
            GroupRepresentation notFound = realm.getGroupByPath("/top/notFound");
            Assert.fail();
        } catch (NotFoundException e) {

        }

        UserRepresentation user = realm.users().search("direct-login", -1, -1).get(0);
        realm.users().get(user.getId()).joinGroup(level3Group.getId());
        List<GroupRepresentation> membership = realm.users().get(user.getId()).groups();
        Assert.assertEquals(1, membership.size());
        Assert.assertEquals("level3", membership.get(0).getName());

        AccessToken token = login("direct-login", "resource-owner", "secret", user.getId());
        Assert.assertTrue(token.getRealmAccess().getRoles().contains("topRole"));
        Assert.assertTrue(token.getRealmAccess().getRoles().contains("level2Role"));
        Assert.assertTrue(token.getRealmAccess().getRoles().contains("level3Role"));

        realm.addDefaultGroup(level3Group.getId());

        List<GroupRepresentation> defaultGroups = realm.getDefaultGroups();
        Assert.assertEquals(1, defaultGroups.size());
        Assert.assertEquals(defaultGroups.get(0).getId(), level3Group.getId());

        UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername("groupUser");
        newUser.setEmail("group@group.com");
        response = realm.users().create(newUser);
        response.close();
        newUser =  realm.users().search("groupUser", -1, -1).get(0);
        membership = realm.users().get(newUser.getId()).groups();
        Assert.assertEquals(1, membership.size());
        Assert.assertEquals("level3", membership.get(0).getName());

        realm.removeDefaultGroup(level3Group.getId());
        defaultGroups = realm.getDefaultGroups();
        Assert.assertEquals(0, defaultGroups.size());
    }
}
