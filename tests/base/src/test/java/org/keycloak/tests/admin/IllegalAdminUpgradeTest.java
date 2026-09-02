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
package org.keycloak.tests.admin;

import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@KeycloakIntegrationTest(config = IllegalAdminUpgradeTest.IllegalAdminKeycloakServerConf.class)
public class IllegalAdminUpgradeTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private static final String REALM_NAME = "default";
    private static final String MASTER_REALM_NAME = "master";


    public static void setupUsers(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        RealmModel master = session.realms().getRealmByName(MASTER_REALM_NAME);
        ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        ClientModel realmMasterAdminClient = realm.getMasterAdminClient();
        RoleModel realmManageUsers = realmAdminClient.getRole(AdminRoles.MANAGE_USERS);
        RoleModel masterManageUsers = realmMasterAdminClient.getRole(AdminRoles.MANAGE_USERS);
        RoleModel masterMasterManageUSers = master.getMasterAdminClient().getRole(AdminRoles.MANAGE_USERS);

        UserModel realmUser = session.users().addUser(realm, "userAdmin");
        realmUser.setFirstName("User");
        realmUser.setLastName("Admin");
        realmUser.setEmail("user@admin");
        realmUser.grantRole(realmManageUsers);
        realmUser.setEnabled(true);
        realmUser.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserModel masterUser = session.users().addUser(master, "userAdmin");
        masterUser.grantRole(masterManageUsers);
        masterUser.setFirstName("User");
        masterUser.setLastName("Admin");
        masterUser.setEmail("user@admin");
        masterUser.setEnabled(true);
        masterUser.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserModel masterAdmin = session.users().addUser(master, "masterAdmin");
        masterAdmin.grantRole(masterMasterManageUSers);
        masterAdmin.setFirstName("Master");
        masterAdmin.setLastName("Admin");
        masterAdmin.setEmail("master@admin");
        masterAdmin.setEnabled(true);
        masterAdmin.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserModel user = session.users().addUser(master, "user");
        user.grantRole(masterManageUsers);
        user.setFirstName("User");
        user.setLastName("Foo");
        user.setEmail("user@foo");
        user.setEnabled(true);
        user.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserModel userDef = session.users().addUser(realm, "user");
        userDef.setFirstName("User");
        userDef.setLastName("Foo");
        userDef.setEmail("user@foo");
        userDef.grantRole(realmManageUsers);
        userDef.setEnabled(true);
        userDef.credentialManager().updateCredential(UserCredentialModel.password("password"));
    }

    @Test
    public void testRestEvaluation() throws Exception {
        runOnServer.run(IllegalAdminUpgradeTest::setupUsers);

        UserRepresentation realmUser = managedRealm.admin().users().search("user").get(0);
        UserRepresentation masterUser = masterRealm.admin().users().search("user").get(0);

        ClientRepresentation realmAdminClient = managedRealm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation realmManageAuthorization = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_AUTHORIZATION).toRepresentation();
        RoleRepresentation realmViewAuthorization = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_AUTHORIZATION).toRepresentation();
        RoleRepresentation realmManageClients = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_CLIENTS).toRepresentation();
        RoleRepresentation realmViewClients = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();
        RoleRepresentation realmManageEvents = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_EVENTS).toRepresentation();
        RoleRepresentation realmViewEvents = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_EVENTS).toRepresentation();
        RoleRepresentation realmManageIdentityProviders = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation realmViewIdentityProviders = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation realmManageRealm = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        RoleRepresentation realmViewRealm = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        RoleRepresentation realmImpersonate = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.IMPERSONATION).toRepresentation();
        RoleRepresentation realmManageUsers = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
        RoleRepresentation realmViewUsers = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        RoleRepresentation realmQueryUsers = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation realmQueryClients = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation();
        RoleRepresentation realmQueryGroups = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.QUERY_GROUPS).toRepresentation();
        RoleRepresentation realmAdmin = managedRealm.admin().clients().get(realmAdminClient.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();

        ClientRepresentation masterClient = masterRealm.admin().clients().findByClientId(REALM_NAME + "-realm").get(0);
        RoleRepresentation masterManageAuthorization = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_AUTHORIZATION).toRepresentation();
        RoleRepresentation masterViewAuthorization = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_AUTHORIZATION).toRepresentation();
        RoleRepresentation masterManageClients = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_CLIENTS).toRepresentation();
        RoleRepresentation masterViewClients = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();
        RoleRepresentation masterManageEvents = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_EVENTS).toRepresentation();
        RoleRepresentation masterViewEvents = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_EVENTS).toRepresentation();
        RoleRepresentation masterManageIdentityProviders = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation masterViewIdentityProviders = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation masterManageRealm = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        RoleRepresentation masterViewRealm = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        RoleRepresentation masterImpersonate = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.IMPERSONATION).toRepresentation();
        RoleRepresentation masterManageUsers = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
        RoleRepresentation masterViewUsers = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        RoleRepresentation masterQueryUsers = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation masterQueryClients = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation();
        RoleRepresentation masterQueryGroups = masterRealm.admin().clients().get(masterClient.getId()).roles().get(AdminRoles.QUERY_GROUPS).toRepresentation();

        List<RoleRepresentation> roles = new LinkedList<>();

        {
            ClientRepresentation client = realmAdminClient;
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(REALM_NAME).username("userAdmin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                roles.clear();
                roles.add(realmManageAuthorization);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }
            
                roles.clear();
                roles.add(realmViewAuthorization);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmAdmin);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageClients);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewClients);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageEvents);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewEvents);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageIdentityProviders);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewIdentityProviders);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageRealm);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewRealm);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmImpersonate);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmViewUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryGroups);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryClients);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
        // test master user with manage_users can't assign realm's admin roles
        {
            ClientRepresentation client = realmAdminClient;
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(MASTER_REALM_NAME).username("userAdmin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                roles.clear();
                roles.add(realmManageAuthorization);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewAuthorization);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmAdmin);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageClients);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewClients);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageEvents);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewEvents);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageIdentityProviders);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewIdentityProviders);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageRealm);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewRealm);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmImpersonate);
                try {
                    realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmViewUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryGroups);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryClients);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
        // test master manageUsers only admin can do with master realm admin roles
        {
            ClientRepresentation client = masterClient;
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(MASTER_REALM_NAME).username("masterAdmin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                roles.clear();
                roles.add(masterManageAuthorization);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewAuthorization);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageClients);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewClients);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageEvents);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewEvents);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageIdentityProviders);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewIdentityProviders);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageRealm);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewRealm);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterImpersonate);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageUsers);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewUsers);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterQueryUsers);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterQueryGroups);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterQueryClients);
                try {
                    realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assertions.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()),
                            is(equalTo(Response.Status.FORBIDDEN)));
                }
            }
        }
        // test master admin can add all admin roles in realm
        {
            ClientRepresentation client = realmAdminClient;
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(MASTER_REALM_NAME).username("admin").password("admin").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                roles.clear();
                roles.add(realmManageAuthorization);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewAuthorization);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageClients);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewClients);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageEvents);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewEvents);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageIdentityProviders);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewIdentityProviders);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageRealm);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewRealm);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmImpersonate);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(realmViewUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(realmQueryUsers);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(realmQueryGroups);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmQueryClients);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(REALM_NAME).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
        // test that "admin" in master realm can assign all roles of master realm realm client admin roles
        {
            ClientRepresentation client = masterClient;
            try (Keycloak realmClient = adminClientFactory.create()
                    .realm(MASTER_REALM_NAME).username("admin").password("admin").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
                roles.clear();
                roles.add(masterManageAuthorization);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewAuthorization);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageClients);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewClients);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageEvents);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewEvents);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageIdentityProviders);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewIdentityProviders);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageRealm);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewRealm);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterImpersonate);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageUsers);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(masterViewUsers);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(masterQueryUsers);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(masterQueryGroups);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterQueryClients);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(MASTER_REALM_NAME).users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
    }

    public static class IllegalAdminKeycloakServerConf implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.features(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
            return builder;
        }
    }
}
