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
package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class IllegalAdminUpgradeTest extends AbstractKeycloakTest {

    public static final String CLIENT_NAME = "application";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }


    public static void setupUsers(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RealmModel master = session.realms().getRealmByName("master");
        ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        ClientModel realmMasterAdminClient = realm.getMasterAdminClient();
        RoleModel realmManageUsers = realmAdminClient.getRole(AdminRoles.MANAGE_USERS);
        RoleModel masterManageUsers = realmMasterAdminClient.getRole(AdminRoles.MANAGE_USERS);
        RoleModel masterMasterManageUSers = master.getMasterAdminClient().getRole(AdminRoles.MANAGE_USERS);

        UserModel realmUser = session.users().addUser(realm, "userAdmin");
        realmUser.grantRole(realmManageUsers);
        realmUser.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, realmUser, UserCredentialModel.password("password"));

        UserModel masterUser = session.users().addUser(master, "userAdmin");
        masterUser.grantRole(masterManageUsers);
        masterUser.setEnabled(true);
        session.userCredentialManager().updateCredential(master, masterUser, UserCredentialModel.password("password"));

        UserModel masterAdmin = session.users().addUser(master, "masterAdmin");
        masterAdmin.grantRole(masterMasterManageUSers);
        masterAdmin.setEnabled(true);
        session.userCredentialManager().updateCredential(master, masterAdmin, UserCredentialModel.password("password"));

        UserModel user = session.users().addUser(master, "user");
        user.grantRole(masterManageUsers);
        user.setEnabled(true);
        session.userCredentialManager().updateCredential(master, user, UserCredentialModel.password("password"));

        user = session.users().addUser(realm, "user");
        user.grantRole(realmManageUsers);
        user.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
    }

    //@Test
    public void testConsole() throws Exception {
        testingClient.server().run(IllegalAdminUpgradeTest::setupUsers);
        Thread.sleep(10000000);
    }

    @Test
    public void testRestEvaluation() throws Exception {
        testingClient.server().run(IllegalAdminUpgradeTest::setupUsers);

        UserRepresentation realmUserAdmin = adminClient.realm(TEST).users().search("userAdmin").get(0);
        UserRepresentation masterUserAdmin = adminClient.realm("master").users().search("userAdmin").get(0);
        UserRepresentation realmUser = adminClient.realm(TEST).users().search("user").get(0);
        UserRepresentation masterUser = adminClient.realm("master").users().search("user").get(0);

        ClientRepresentation realmAdminClient = adminClient.realm(TEST).clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation realmManageAuthorization = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_AUTHORIZATION).toRepresentation();
        RoleRepresentation realmViewAuthorization = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_AUTHORIZATION).toRepresentation();
        RoleRepresentation realmManageClients = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_CLIENTS).toRepresentation();
        RoleRepresentation realmViewClients = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();
        RoleRepresentation realmManageEvents = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_EVENTS).toRepresentation();
        RoleRepresentation realmViewEvents = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_EVENTS).toRepresentation();
        RoleRepresentation realmManageIdentityProviders = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation realmViewIdentityProviders = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation realmManageRealm = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        RoleRepresentation realmViewRealm = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        RoleRepresentation realmImpersonate = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(ImpersonationConstants.IMPERSONATION_ROLE).toRepresentation();
        RoleRepresentation realmManageUsers = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
        RoleRepresentation realmViewUsers = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        RoleRepresentation realmQueryUsers = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation realmQueryClients = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation();
        RoleRepresentation realmQueryGroups = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.QUERY_GROUPS).toRepresentation();
        RoleRepresentation realmAdmin = adminClient.realm(TEST).clients().get(realmAdminClient.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();

        ClientRepresentation masterClient = adminClient.realm("master").clients().findByClientId(TEST + "-realm").get(0);
        RoleRepresentation masterManageAuthorization = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_AUTHORIZATION).toRepresentation();
        RoleRepresentation masterViewAuthorization = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_AUTHORIZATION).toRepresentation();
        RoleRepresentation masterManageClients = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_CLIENTS).toRepresentation();
        RoleRepresentation masterViewClients = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();
        RoleRepresentation masterManageEvents = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_EVENTS).toRepresentation();
        RoleRepresentation masterViewEvents = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_EVENTS).toRepresentation();
        RoleRepresentation masterManageIdentityProviders = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation masterViewIdentityProviders = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_IDENTITY_PROVIDERS).toRepresentation();
        RoleRepresentation masterManageRealm = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        RoleRepresentation masterViewRealm = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        RoleRepresentation masterImpersonate = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(ImpersonationConstants.IMPERSONATION_ROLE).toRepresentation();
        RoleRepresentation masterManageUsers = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
        RoleRepresentation masterViewUsers = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        RoleRepresentation masterQueryUsers = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation masterQueryClients = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation();
        RoleRepresentation masterQueryGroups = adminClient.realm("master").clients().get(masterClient.getId()).roles().get(AdminRoles.QUERY_GROUPS).toRepresentation();

        List<RoleRepresentation> roles = new LinkedList<>();

        {
            ClientRepresentation client = realmAdminClient;
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "userAdmin", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                roles.clear();
                roles.add(realmManageAuthorization);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }
            
                roles.clear();
                roles.add(realmViewAuthorization);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmAdmin);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageClients);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewClients);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageEvents);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewEvents);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageIdentityProviders);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewIdentityProviders);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageRealm);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewRealm);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmImpersonate);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmViewUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryGroups);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryClients);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
        // test master user with manage_users can't assign realm's admin roles
        {
            ClientRepresentation client = realmAdminClient;
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    "master", "userAdmin", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                roles.clear();
                roles.add(realmManageAuthorization);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewAuthorization);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmAdmin);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageClients);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewClients);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageEvents);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewEvents);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageIdentityProviders);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewIdentityProviders);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageRealm);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmViewRealm);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmImpersonate);
                try {
                    realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }

                roles.clear();
                roles.add(realmManageUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmViewUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryGroups);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);

                roles.clear();
                roles.add(realmQueryClients);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
        // test master manageUsers only admin can do with master realm admin roles
        {
            ClientRepresentation client = masterClient;
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    "master", "masterAdmin", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                roles.clear();
                roles.add(masterManageAuthorization);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewAuthorization);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageClients);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewClients);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageEvents);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewEvents);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageIdentityProviders);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewIdentityProviders);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageRealm);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewRealm);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterImpersonate);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterManageUsers);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterViewUsers);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterQueryUsers);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterQueryGroups);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));                    
                }
                roles.clear();
                roles.add(masterQueryClients);
                try {
                    realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertThat(Response.Status.fromStatusCode(e.getResponse().getStatus()), 
                            is(equalTo(Response.Status.FORBIDDEN)));
                }
            }
        }
        // test master admin can add all admin roles in realm
        {
            ClientRepresentation client = realmAdminClient;
            try (Keycloak realmClient = AdminClientUtil.createAdminClient()) {
                roles.clear();
                roles.add(realmManageAuthorization);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewAuthorization);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageClients);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewClients);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageEvents);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewEvents);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageIdentityProviders);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewIdentityProviders);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageRealm);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmViewRealm);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmImpersonate);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmManageUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(realmViewUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(realmQueryUsers);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(realmQueryGroups);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(realmQueryClients);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm(TEST).users().get(realmUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
        // test that "admin" in master realm can assign all roles of master realm realm client admin roles
        {
            ClientRepresentation client = masterClient;
            try (Keycloak realmClient = AdminClientUtil.createAdminClient()) {
                roles.clear();
                roles.add(masterManageAuthorization);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewAuthorization);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageClients);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewClients);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageEvents);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewEvents);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageIdentityProviders);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewIdentityProviders);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageRealm);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterViewRealm);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterImpersonate);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterManageUsers);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(masterViewUsers);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(masterQueryUsers);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                
                roles.clear();
                roles.add(masterQueryGroups);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
                
                roles.clear();
                roles.add(masterQueryClients);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).add(roles);
                realmClient.realm("master").users().get(masterUser.getId()).roles().clientLevel(client.getId()).remove(roles);
            }
        }
    }




}
