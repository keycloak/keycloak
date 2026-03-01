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
package org.keycloak.tests.admin.finegrainedadminv1;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@KeycloakIntegrationTest(config = AbstractFineGrainedAdminTest.FineGrainedAdminServerConf.class)
public class FineGrainedAdminRestTest extends AbstractFineGrainedAdminTest {

    @Test
    public void testRestEvaluation() throws Exception {
        String groupId = ApiUtil.getCreatedId(managedRealm.admin().groups().add(GroupConfigBuilder.create().name("restricted-group").build()));

        runOnServer.run(FineGrainedAdminRestTest::setupPolices);
        runOnServer.run(FineGrainedAdminRestTest::setupUsers);

        UserRepresentation user1 = managedRealm.admin().users().search("user1").get(0);
        UserRepresentation anotherAdmin = managedRealm.admin().users().search("anotherAdmin").get(0);
        UserRepresentation groupMember = managedRealm.admin().users().search("groupMember").get(0);
        RoleRepresentation realmRole = managedRealm.admin().roles().get("realm-role").toRepresentation();
        List<RoleRepresentation> realmRoleSet = new LinkedList<>();
        realmRoleSet.add(realmRole);
        RoleRepresentation realmRole2 = managedRealm.admin().roles().get("realm-role2").toRepresentation();
        List<RoleRepresentation> realmRole2Set = new LinkedList<>();
        realmRole2Set.add(realmRole2);
        ClientRepresentation client = managedRealm.admin().clients().findByClientId(CLIENT_NAME).get(0);
        ClientScopeRepresentation scope = managedRealm.admin().clientScopes().findAll().get(0);
        RoleRepresentation clientRole = managedRealm.admin().clients().get(client.getId()).roles().get("client-role").toRepresentation();
        List<RoleRepresentation> clientRoleSet = new LinkedList<>();
        clientRoleSet.add(clientRole);

        // test configure client
        {
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("clientConfigurer").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                client.setAdminUrl("http://nowhere");
                realmClient.realm(REALM_NAME).clients().get(client.getId()).update(client);
                client.setFullScopeAllowed(true);
                try {
                    realmClient.realm(REALM_NAME).clients().get(client.getId()).update(client);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assertions.assertEquals(403, e.getResponse().getStatus());

                }
                client.setFullScopeAllowed(false);
                realmClient.realm(REALM_NAME).clients().get(client.getId()).update(client);

                try {
                    realmClient.realm(REALM_NAME).clients().get(client.getId()).addDefaultClientScope(scope.getId());
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assertions.assertEquals(403, e.getResponse().getStatus());

                }

                try {
                    realmClient.realm(REALM_NAME).clients().get(client.getId()).getScopeMappings().realmLevel().add(realmRoleSet);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assertions.assertEquals(403, e.getResponse().getStatus());

                }
            }
        }

        // test illegal impersonation
        {
            Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("nomap-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build();
            try {
                realmClient.realm(REALM_NAME).users().get(user1.getId()).impersonate();
                realmClient.close(); // just in case of cookie settings
                realmClient = adminClientFactory.create()
                        .realm(REALM_NAME).username("nomap-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build();
                try {
                    realmClient.realm(REALM_NAME).users().get(anotherAdmin.getId()).impersonate();
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assertions.assertEquals(403, e.getResponse().getStatus());
                }
            } finally {
                realmClient.close();
            }
        }


        {
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("authorized").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                List<RoleRepresentation> roles = managedRealm.admin().users().get(user1.getId()).roles().realmLevel().listAll();
                Assertions.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("realm-role");
                }));
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().remove(realmRoleSet);
                roles = managedRealm.admin().users().get(user1.getId()).roles().realmLevel().listAll();
                Assertions.assertTrue(roles.stream().noneMatch((r) -> {
                    return r.getName().equals("realm-role");
                }));

                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
                roles = managedRealm.admin().users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assertions.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).remove(clientRoleSet);
                roles = managedRealm.admin().users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assertions.assertTrue(roles.stream().noneMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
            }
        }

        {
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("authorizedComposite").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                List<RoleRepresentation> roles = managedRealm.admin().users().get(user1.getId()).roles().realmLevel().listAll();
                Assertions.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("realm-role");
                }));
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().remove(realmRoleSet);
                roles = managedRealm.admin().users().get(user1.getId()).roles().realmLevel().listAll();
                Assertions.assertTrue(roles.stream().noneMatch((r) -> {
                    return r.getName().equals("realm-role");
                }));

                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
                roles = managedRealm.admin().users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assertions.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).remove(clientRoleSet);
                roles = managedRealm.admin().users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assertions.assertTrue(roles.stream().noneMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
            }
        }
        {
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("unauthorized").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                Assertions.fail("should fail with forbidden exception");
            } catch (ClientErrorException e) {
                Assertions.assertEquals(403, e.getResponse().getStatus());
            }
        }
        {
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("unauthorizedMapper").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                Assertions.fail("should fail with forbidden exception");
            } catch (ClientErrorException e) {
                Assertions.assertEquals(403, e.getResponse().getStatus());
            }
        }

        {
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("groupManager").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                realmClient.realm(REALM_NAME).users().get(groupMember.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
                List<RoleRepresentation> roles = realmClient.realm(REALM_NAME).users().get(groupMember.getId()).roles().clientLevel(client.getId()).listAll();
                Assertions.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
                realmClient.realm(REALM_NAME).users().get(groupMember.getId()).roles().clientLevel(client.getId()).remove(clientRoleSet);

                roles = realmClient.realm(REALM_NAME).users().get(groupMember.getId()).roles().realmLevel().listAvailable();
                Assertions.assertEquals(1, roles.size());
                realmClient.realm(REALM_NAME).users().get(groupMember.getId()).roles().realmLevel().add(realmRoleSet);
                realmClient.realm(REALM_NAME).users().get(groupMember.getId()).roles().realmLevel().remove(realmRoleSet);
                try {
                    realmClient.realm(REALM_NAME).users().get(groupMember.getId()).roles().realmLevel().add(realmRole2Set);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assertions.assertEquals(403, e.getResponse().getStatus());
                }
                try {
                    realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assertions.assertEquals(403, e.getResponse().getStatus());
                }
            }
        }


        // test client.mapRoles
        {
            try (Keycloak realmClient = adminClientFactory
                    .create().realm(REALM_NAME).username("clientMapper").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                List<RoleRepresentation> roles = realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assertions.assertTrue(roles.isEmpty());
                realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
                roles = realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assertions.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
                roles = realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().listAvailable();
                Assertions.assertTrue(roles.isEmpty());
                try {
                    realmClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assertions.assertEquals(403, e.getResponse().getStatus());
                }
            }
        }

        // KEYCLOAK-5878

        {
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("groupViewer").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                // Should only return the list of users that belong to "top" group
                List<UserRepresentation> queryUsers = realmClient.realm(REALM_NAME).users().list();
                Assertions.assertEquals(queryUsers.size(), 1);
                MatcherAssert.assertThat("groupmember", Matchers.equalTo(queryUsers.get(0).getUsername()));
                for (UserRepresentation user : queryUsers) {
                    System.out.println(user.getUsername());
                }
            }
        }

        // KEYCLOAK-11261 : user creation via fine grain admin

        {
            try (Keycloak realmClient = adminClientFactory.create().realm(REALM_NAME)
                    .username("noMapperGroupManager").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                // Should only return the list of users that belong to "top" group
                List<UserRepresentation> queryUsers = realmClient.realm(REALM_NAME).users().list();
                Assertions.assertEquals(1, queryUsers.size());

                UserRepresentation newGroupMemberWithoutGroup = UserConfigBuilder.create()
                        .username("new-group-member").email("new-group-member@keycloak.org").name("New", "Member").build();

                Response response1 = realmClient.realm(REALM_NAME).users().create(newGroupMemberWithoutGroup);

                Assertions.assertEquals(403, response1.getStatus());

                UserRepresentation newEmptyGroupList = UserConfigBuilder.create()
                        .username("new-group-member").email("new-group-member@keycloak.org").name("New", "Member").build();
                newEmptyGroupList.setGroups(Collections.emptyList());

                Response response2 = realmClient.realm(REALM_NAME).users().create(newEmptyGroupList);
                Assertions.assertEquals(403, response2.getStatus());

                UserRepresentation newGroupMemberWithNonExistentGroup = UserConfigBuilder.create()
                        .username("new-group-member").email("new-group-member@keycloak.org").name("New", "Member").groups("wrong-group").build();

                Response response3 = realmClient.realm(REALM_NAME).users().create(newGroupMemberWithNonExistentGroup);
                Assertions.assertEquals(403, response3.getStatus());

                UserRepresentation newGroupMemberOfNotManagedGroup = UserConfigBuilder.create()
                        .username("new-group-member").email("new-group-member@keycloak.org").name("New", "Member").groups("restricted-group").build();

                Response response4 = realmClient.realm(REALM_NAME).users().create(newGroupMemberOfNotManagedGroup);
                Assertions.assertEquals(403, response4.getStatus());

                UserRepresentation newGroupMember = UserConfigBuilder.create()
                        .username("new-group-member").email("new-group-member@keycloak.org").name("New", "Member").groups("top").build();
                AdminApiUtil.createUserWithAdminClient(realmClient.realm(REALM_NAME), newGroupMember);

                // Should only return the list of users that belong to "top" group + the new one
                queryUsers = realmClient.realm(REALM_NAME).users().list();
                Assertions.assertEquals(2, queryUsers.size());
            }
        }
        managedRealm.cleanup().add(r -> r.groups().group(groupId).remove());
    }
}
