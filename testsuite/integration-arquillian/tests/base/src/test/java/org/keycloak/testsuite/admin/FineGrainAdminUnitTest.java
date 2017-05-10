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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.admin.permissions.MgmtPermissions;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.util.AdminClientUtil;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
//@Ignore
public class FineGrainAdminUnitTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    public static void setupPolices(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        MgmtPermissions permissions = new MgmtPermissions(session, realm);
        RoleModel realmRole = realm.addRole("realm-role");
        RoleModel realmRole2 = realm.addRole("realm-role2");
        ClientModel client1 = realm.addClient("role-namespace");
        RoleModel client1Role = client1.addRole("client-role");

        RoleModel mapperRole = realm.addRole("mapper");
        RoleModel managerRole = realm.addRole("manager");
        RoleModel compositeRole = realm.addRole("composite-role");
        compositeRole.addCompositeRole(mapperRole);
        compositeRole.addCompositeRole(managerRole);

        // realm-role and role-namespace.client-role will have a role policy associated with their map-role permission
        {
            permissions.roles().setPermissionsEnabled(realmRole, true);
            Policy mapRolePermission = permissions.roles().mapRolePermission(realmRole);
            ResourceServer server = permissions.roles().resourceServer(realmRole);
            Policy mapperPolicy = permissions.roles().rolePolicy(server, mapperRole);
            mapRolePermission.addAssociatedPolicy(mapperPolicy);
        }

        {
            permissions.roles().setPermissionsEnabled(client1Role, true);
            Policy mapRolePermission = permissions.roles().mapRolePermission(client1Role);
            ResourceServer server = permissions.roles().resourceServer(client1Role);
            Policy mapperPolicy = permissions.roles().rolePolicy(server, mapperRole);
            mapRolePermission.addAssociatedPolicy(mapperPolicy);
        }

        // realmRole2 will have an empty map-role policy
        {
            permissions.roles().setPermissionsEnabled(realmRole2, true);
        }

        // setup Users manage policies
        {
            permissions.users().setPermissionsEnabled(true);
            ResourceServer server = permissions.users().resourceServer();
            Policy managerPolicy = permissions.roles().rolePolicy(server, managerRole);
            Policy permission = permissions.users().managePermission();
            permission.addAssociatedPolicy(managerPolicy);
            permission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        }

    }

    public static void setupUsers(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        ClientModel client = realm.getClientByClientId("role-namespace");
        RoleModel realmRole = realm.getRole("realm-role");
        RoleModel realmRole2 = realm.getRole("realm-role2");
        RoleModel clientRole = client.getRole("client-role");
        RoleModel mapperRole = realm.getRole("mapper");
        RoleModel managerRole = realm.getRole("manager");
        RoleModel compositeRole = realm.getRole("composite-role");

        UserModel authorizedUser = session.users().addUser(realm, "authorized");
        authorizedUser.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, authorizedUser, UserCredentialModel.password("password"));
        authorizedUser.grantRole(mapperRole);
        authorizedUser.grantRole(managerRole);

        UserModel authorizedComposite = session.users().addUser(realm, "authorizedComposite");
        authorizedComposite.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, authorizedComposite, UserCredentialModel.password("password"));
        authorizedComposite.grantRole(compositeRole);

        UserModel unauthorizedUser = session.users().addUser(realm, "unauthorized");
        unauthorizedUser.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, unauthorizedUser, UserCredentialModel.password("password"));

        UserModel unauthorizedMapper = session.users().addUser(realm, "unauthorizedMapper");
        unauthorizedMapper.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, unauthorizedMapper, UserCredentialModel.password("password"));
        unauthorizedMapper.grantRole(managerRole);

        UserModel user1 = session.users().addUser(realm, "user1");
        user1.setEnabled(true);
        UserModel user2 = session.users().addUser(realm, "user2");
        user2.setEnabled(true);
        UserModel user3 = session.users().addUser(realm, "user3");
        user3.setEnabled(true);
        UserModel user4 = session.users().addUser(realm, "user4");
        user4.setEnabled(true);

    }

    public static void evaluateLocally(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel realmRole = realm.getRole("realm-role");
        RoleModel realmRole2 = realm.getRole("realm-role2");
        ClientModel client = realm.getClientByClientId("role-namespace");
        RoleModel clientRole = client.getRole("client-role");

        // test authorized
        {
            UserModel user = session.users().getUserByUsername("authorized", realm);
            MgmtPermissions permissionsForAdmin = new MgmtPermissions(session, realm);
            permissionsForAdmin.setIdentity(user);
            Assert.assertTrue(permissionsForAdmin.users().canManage(user));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole2));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(clientRole));
        }
        // test composite role
        {
            UserModel user = session.users().getUserByUsername("authorizedComposite", realm);
            MgmtPermissions permissionsForAdmin = new MgmtPermissions(session, realm);
            permissionsForAdmin.setIdentity(user);
            Assert.assertTrue(permissionsForAdmin.users().canManage(user));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole2));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(clientRole));
        }

        // test unauthorized
        {
            UserModel user = session.users().getUserByUsername("unauthorized", realm);
            MgmtPermissions permissionsForAdmin = new MgmtPermissions(session, realm);
            permissionsForAdmin.setIdentity(user);
            Assert.assertFalse(permissionsForAdmin.users().canManage(user));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(clientRole));

            // will result to true because realmRole2 does not have any policies attached to this permission
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole2));
        }
        // test unauthorized mapper
        {
            UserModel user = session.users().getUserByUsername("unauthorizedMapper", realm);
            MgmtPermissions permissionsForAdmin = new MgmtPermissions(session, realm);
            permissionsForAdmin.setIdentity(user);
            Assert.assertTrue(permissionsForAdmin.users().canManage(user));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(clientRole));
            // will result to true because realmRole2 does not have any policies attached to this permission
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole2));
        }

    }


    protected boolean isImportAfterEachMethod() {
        return true;
    }


    //@Test
    public void testUI() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupPolices);
        testingClient.server().run(FineGrainAdminUnitTest::setupUsers);
        Thread.sleep(1000000000);
    }

    @Test
    public void testEvaluationLocal() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupPolices);
        testingClient.server().run(FineGrainAdminUnitTest::setupUsers);
        testingClient.server().run(FineGrainAdminUnitTest::evaluateLocally);
    }

    @Test
    public void testRestEvaluation() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupPolices);
        testingClient.server().run(FineGrainAdminUnitTest::setupUsers);

        UserRepresentation user1 = adminClient.realm(TEST).users().search("user1").get(0);
        UserRepresentation user2 = adminClient.realm(TEST).users().search("user2").get(0);
        UserRepresentation user3 = adminClient.realm(TEST).users().search("user3").get(0);
        UserRepresentation user4 = adminClient.realm(TEST).users().search("user4").get(0);
        RoleRepresentation realmRole = adminClient.realm(TEST).roles().get("realm-role").toRepresentation();
        List<RoleRepresentation> realmRoleSet = new LinkedList<>();
        realmRoleSet.add(realmRole);
        RoleRepresentation realmRole2 = adminClient.realm(TEST).roles().get("realm-role2").toRepresentation();
        List<RoleRepresentation> realmRole2Set = new LinkedList<>();
        realmRole2Set.add(realmRole);
        ClientRepresentation client = adminClient.realm(TEST).clients().findByClientId("role-namespace").get(0);
        RoleRepresentation clientRole = adminClient.realm(TEST).clients().get(client.getId()).roles().get("client-role").toRepresentation();
        List<RoleRepresentation> clientRoleSet = new LinkedList<>();
        clientRoleSet.add(clientRole);


        {
            Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "authorized", "password", Constants.ADMIN_CLI_CLIENT_ID, null);
            realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
            List<RoleRepresentation> roles = adminClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().listAll();
            Assert.assertTrue(roles.stream().anyMatch((r) -> {
                return r.getName().equals("realm-role");
            }));
            realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().remove(realmRoleSet);
            roles = adminClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().listAll();
            Assert.assertTrue(roles.stream().noneMatch((r) -> {
                return r.getName().equals("realm-role");
            }));

            realmClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
            roles = adminClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
            Assert.assertTrue(roles.stream().anyMatch((r) -> {
                return r.getName().equals("client-role");
            }));
            realmClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).remove(clientRoleSet);
            roles = adminClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
            Assert.assertTrue(roles.stream().noneMatch((r) -> {
                return r.getName().equals("client-role");
            }));
            realmClient.close();
        }

        {
            Keycloak realmClient= AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "authorizedComposite", "password", Constants.ADMIN_CLI_CLIENT_ID, null);
            realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
            List<RoleRepresentation> roles = adminClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().listAll();
            Assert.assertTrue(roles.stream().anyMatch((r) -> {
                return r.getName().equals("realm-role");
            }));
            realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().remove(realmRoleSet);
            roles = adminClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().listAll();
            Assert.assertTrue(roles.stream().noneMatch((r) -> {
                return r.getName().equals("realm-role");
            }));

            realmClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
            roles = adminClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
            Assert.assertTrue(roles.stream().anyMatch((r) -> {
                return r.getName().equals("client-role");
            }));
            realmClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).remove(clientRoleSet);
            roles = adminClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
            Assert.assertTrue(roles.stream().noneMatch((r) -> {
                return r.getName().equals("client-role");
            }));
        }
        {
            Keycloak realmClient= AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "unauthorized", "password", Constants.ADMIN_CLI_CLIENT_ID, null);
            try {
                realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                Assert.fail("should fail with forbidden exception");
            } catch (ClientErrorException e) {
                Assert.assertEquals(e.getResponse().getStatus(), 403);

            }
        }
        {
            Keycloak realmClient= AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "unauthorizedMapper", "password", Constants.ADMIN_CLI_CLIENT_ID, null);
            try {
                realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                Assert.fail("should fail with forbidden exception");
            } catch (ClientErrorException e) {
                Assert.assertEquals(e.getResponse().getStatus(), 403);

            }
        }

    }


}
