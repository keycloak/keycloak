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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.client.admin.cli.util.ConfigUtil;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.resources.admin.permissions.ClientPermissionManagement;
import org.keycloak.services.resources.admin.permissions.GroupPermissionManagement;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.utils.tls.TLSUtils;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import static org.keycloak.testsuite.admin.ImpersonationDisabledTest.IMPERSONATION_DISABLED;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@EnableFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)
public class FineGrainAdminUnitTest extends AbstractKeycloakTest {

    public static final String CLIENT_NAME = "application";

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.AUTHORIZATION);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
        testRealmRep.setGroups(Arrays.asList(GroupBuilder.create().name("restricted-group").build()));
    }

    public static void setupDemo(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        realm.addRole("realm-role");
        ClientModel client = realm.addClient("sales-application");
        RoleModel clientAdmin = client.addRole("admin");
        client.addRole("leader-creator");
        client.addRole("viewLeads");
        GroupModel sales = realm.createGroup("sales");


        UserModel admin = session.users().addUser(realm, "salesManager");
        admin.setEnabled(true);
        admin.credentialManager().updateCredential(UserCredentialModel.password("password"));

        admin = session.users().addUser(realm, "sales-admin");
        admin.setEnabled(true);
        admin.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserModel user = session.users().addUser(realm, "salesman");
        user.setEnabled(true);
        user.joinGroup(sales);

        user = session.users().addUser(realm, "saleswoman");
        user.setEnabled(true);

    }

    public static void setupPolices(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        RoleModel realmRole = realm.addRole("realm-role");
        RoleModel realmRole2 = realm.addRole("realm-role2");
        ClientModel client1 = realm.addClient(CLIENT_NAME);
        realm.addClientScope("scope");
        client1.setFullScopeAllowed(false);
        RoleModel client1Role = client1.addRole("client-role");
        GroupModel group = realm.createGroup("top");

        RoleModel mapperRole = realm.addRole("mapper");
        RoleModel managerRole = realm.addRole("manager");
        RoleModel compositeRole = realm.addRole("composite-role");
        compositeRole.addCompositeRole(mapperRole);
        compositeRole.addCompositeRole(managerRole);

        // realm-role and application.client-role will have a role policy associated with their map-role permission
        {
            permissions.roles().setPermissionsEnabled(client1Role, true);
            Policy mapRolePermission = permissions.roles().mapRolePermission(client1Role);
            ResourceServer server = permissions.roles().resourceServer(client1Role);
            Policy mapperPolicy = permissions.roles().rolePolicy(server, mapperRole);
            mapRolePermission.addAssociatedPolicy(mapperPolicy);
        }

        {
            permissions.roles().setPermissionsEnabled(realmRole, true);
            Policy mapRolePermission = permissions.roles().mapRolePermission(realmRole);
            ResourceServer server = permissions.roles().resourceServer(realmRole);
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
            ResourceServer server = permissions.realmResourceServer();
            Policy managerPolicy = permissions.roles().rolePolicy(server, managerRole);
            Policy permission = permissions.users().managePermission();
            permission.addAssociatedPolicy(managerPolicy);
            permission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        }
        {
            permissions.groups().setPermissionsEnabled(group, true);
        }
        {
            permissions.clients().setPermissionsEnabled(client1, true);
        }
        // setup Users impersonate policy
        {
            ClientModel realmManagementClient = realm.getClientByClientId("realm-management");
            RoleModel adminRole = realmManagementClient.getRole(AdminRoles.REALM_ADMIN);
            permissions.users().setPermissionsEnabled(true);
            ResourceServer server = permissions.realmResourceServer();
            Policy adminPolicy = permissions.roles().rolePolicy(server, adminRole);
            adminPolicy.setLogic(Logic.NEGATIVE);
            Policy permission = permissions.users().userImpersonatedPermission();
            permission.addAssociatedPolicy(adminPolicy);
            permission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        }


    }

    public static void setupUsers(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        ClientModel client = realm.getClientByClientId(CLIENT_NAME);
        RoleModel realmRole = realm.getRole("realm-role");
        RoleModel realmRole2 = realm.getRole("realm-role2");
        RoleModel clientRole = client.getRole("client-role");
        RoleModel mapperRole = realm.getRole("mapper");
        RoleModel managerRole = realm.getRole("manager");
        RoleModel compositeRole = realm.getRole("composite-role");
        ClientModel realmManagementClient = realm.getClientByClientId("realm-management");
        RoleModel adminRole = realmManagementClient.getRole(AdminRoles.REALM_ADMIN);
        RoleModel queryGroupsRole = realmManagementClient.getRole(AdminRoles.QUERY_GROUPS);
        RoleModel queryUsersRole = realmManagementClient.getRole(AdminRoles.QUERY_USERS);
        RoleModel queryClientsRole = realmManagementClient.getRole(AdminRoles.QUERY_CLIENTS);

        UserModel nomapAdmin = session.users().addUser(realm, "nomap-admin");
        nomapAdmin.setEnabled(true);
        nomapAdmin.credentialManager().updateCredential(UserCredentialModel.password("password"));
        nomapAdmin.grantRole(adminRole);

        UserModel anotherAdmin = session.users().addUser(realm, "anotherAdmin");
        anotherAdmin.setEnabled(true);
        anotherAdmin.credentialManager().updateCredential(UserCredentialModel.password("password"));
        anotherAdmin.grantRole(adminRole);

        UserModel authorizedUser = session.users().addUser(realm, "authorized");
        authorizedUser.setEnabled(true);
        authorizedUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
        authorizedUser.grantRole(mapperRole);
        authorizedUser.grantRole(managerRole);

        UserModel authorizedComposite = session.users().addUser(realm, "authorizedComposite");
        authorizedComposite.setEnabled(true);
        authorizedComposite.credentialManager().updateCredential(UserCredentialModel.password("password"));
        authorizedComposite.grantRole(compositeRole);

        UserModel unauthorizedUser = session.users().addUser(realm, "unauthorized");
        unauthorizedUser.setEnabled(true);
        unauthorizedUser.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserModel unauthorizedMapper = session.users().addUser(realm, "unauthorizedMapper");
        unauthorizedMapper.setEnabled(true);
        unauthorizedMapper.credentialManager().updateCredential(UserCredentialModel.password("password"));
        unauthorizedMapper.grantRole(managerRole);

        UserModel user1 = session.users().addUser(realm, "user1");
        user1.setEnabled(true);

        // group management
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);

        GroupModel group =  KeycloakModelUtils.findGroupByPath(realm, "top");
        UserModel groupMember = session.users().addUser(realm, "groupMember");
        groupMember.joinGroup(group);
        groupMember.setEnabled(true);
        UserModel groupManager = session.users().addUser(realm, "groupManager");
        groupManager.grantRole(queryGroupsRole);
        groupManager.grantRole(queryUsersRole);
        groupManager.setEnabled(true);
        groupManager.grantRole(mapperRole);
        groupManager.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserModel groupManagerNoMapper = session.users().addUser(realm, "noMapperGroupManager");
        groupManagerNoMapper.setEnabled(true);
        groupManagerNoMapper.credentialManager().updateCredential(UserCredentialModel.password("password"));
        groupManagerNoMapper.grantRole(queryGroupsRole);
        groupManagerNoMapper.grantRole(queryUsersRole);

        UserPolicyRepresentation groupManagerRep = new UserPolicyRepresentation();
        groupManagerRep.setName("groupManagers");
        groupManagerRep.addUser("groupManager");
        groupManagerRep.addUser("noMapperGroupManager");
        ResourceServer server = permissions.realmResourceServer();
        Policy groupManagerPolicy = permissions.authz().getStoreFactory().getPolicyStore().create(server, groupManagerRep);
        permissions.groups().manageMembersPermission(group).addAssociatedPolicy(groupManagerPolicy);
        permissions.groups().manageMembershipPermission(group).addAssociatedPolicy(groupManagerPolicy);
        permissions.groups().viewPermission(group).addAssociatedPolicy(groupManagerPolicy);

        UserModel clientMapper = session.users().addUser(realm, "clientMapper");
        clientMapper.setEnabled(true);
        clientMapper.grantRole(managerRole);
        clientMapper.grantRole(queryUsersRole);
        clientMapper.credentialManager().updateCredential(UserCredentialModel.password("password"));
        Policy clientMapperPolicy = permissions.clients().mapRolesPermission(client);
        UserPolicyRepresentation userRep = new UserPolicyRepresentation();
        userRep.setName("userClientMapper");
        userRep.addUser("clientMapper");
        Policy userPolicy = permissions.authz().getStoreFactory().getPolicyStore().create(permissions.clients().resourceServer(client), userRep);
        clientMapperPolicy.addAssociatedPolicy(userPolicy);

        UserModel clientManager = session.users().addUser(realm, "clientManager");
        clientManager.setEnabled(true);
        clientManager.grantRole(queryClientsRole);
        clientManager.credentialManager().updateCredential(UserCredentialModel.password("password"));

        Policy clientManagerPolicy = permissions.clients().managePermission(client);
        userRep = new UserPolicyRepresentation();
        userRep.setName("clientManager");
        userRep.addUser("clientManager");
        userPolicy = permissions.authz().getStoreFactory().getPolicyStore().create(permissions.clients().resourceServer(client), userRep);
        clientManagerPolicy.addAssociatedPolicy(userPolicy);


        UserModel clientConfigurer = session.users().addUser(realm, "clientConfigurer");
        clientConfigurer.setEnabled(true);
        clientConfigurer.grantRole(queryClientsRole);
        clientConfigurer.credentialManager().updateCredential(UserCredentialModel.password("password"));

        Policy clientConfigurePolicy = permissions.clients().configurePermission(client);
        userRep = new UserPolicyRepresentation();
        userRep.setName("clientConfigure");
        userRep.addUser("clientConfigurer");
        userPolicy = permissions.authz().getStoreFactory().getPolicyStore().create(permissions.clients().resourceServer(client), userRep);
        clientConfigurePolicy.addAssociatedPolicy(userPolicy);


        UserModel groupViewer = session.users().addUser(realm, "groupViewer");
        groupViewer.grantRole(queryGroupsRole);
        groupViewer.grantRole(queryUsersRole);
        groupViewer.setEnabled(true);
        groupViewer.credentialManager().updateCredential(UserCredentialModel.password("password"));

        UserPolicyRepresentation groupViewMembersRep = new UserPolicyRepresentation();
        groupViewMembersRep.setName("groupMemberViewers");
        groupViewMembersRep.addUser("groupViewer");
        Policy groupViewMembersPolicy = permissions.authz().getStoreFactory().getPolicyStore().create(server, groupViewMembersRep);
        Policy groupViewMembersPermission = permissions.groups().viewMembersPermission(group);
        groupViewMembersPermission.addAssociatedPolicy(groupViewMembersPolicy);


    }

    public static void evaluateLocally(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel realmRole = realm.getRole("realm-role");
        RoleModel realmRole2 = realm.getRole("realm-role2");
        ClientModel client = realm.getClientByClientId(CLIENT_NAME);
        RoleModel clientRole = client.getRole("client-role");

        // test authorized
        {
            UserModel user = session.users().getUserByUsername(realm, "authorized");
            AdminPermissionEvaluator permissionsForAdmin = AdminPermissions.evaluator(session, realm, realm, user);
            Assert.assertTrue(permissionsForAdmin.users().canManage());
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole2));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(clientRole));
        }
        // test composite role
        {
            UserModel user = session.users().getUserByUsername(realm, "authorizedComposite");
            AdminPermissionEvaluator permissionsForAdmin = AdminPermissions.evaluator(session, realm, realm, user);
            Assert.assertTrue(permissionsForAdmin.users().canManage());
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole2));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(clientRole));
        }

        // test unauthorized
        {
            UserModel user = session.users().getUserByUsername(realm, "unauthorized");
            AdminPermissionEvaluator permissionsForAdmin = AdminPermissions.evaluator(session, realm, realm, user);
            Assert.assertFalse(permissionsForAdmin.users().canManage());
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(clientRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole2));
        }
        // test unauthorized mapper
        {
            UserModel user = session.users().getUserByUsername(realm, "unauthorizedMapper");
            AdminPermissionEvaluator permissionsForAdmin = AdminPermissions.evaluator(session, realm, realm, user);
            Assert.assertTrue(permissionsForAdmin.users().canManage());
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(clientRole));
            // will result to true because realmRole2 does not have any policies attached to this permission
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole2));
        }
        // test group management
        {
            UserModel admin = session.users().getUserByUsername(realm, "groupManager");
            AdminPermissionEvaluator permissionsForAdmin = AdminPermissions.evaluator(session, realm, realm, admin);
            UserModel user = session.users().getUserByUsername(realm, "authorized");
            Assert.assertFalse(permissionsForAdmin.users().canManage(user));
            Assert.assertFalse(permissionsForAdmin.users().canView(user));
            UserModel member = session.users().getUserByUsername(realm, "groupMember");
            Assert.assertTrue(permissionsForAdmin.users().canManage(member));
            Assert.assertTrue(permissionsForAdmin.users().canManageGroupMembership(member));
            Assert.assertTrue(permissionsForAdmin.users().canView(member));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(clientRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole2));

        }
        // test client.mapRoles
        {
            UserModel admin = session.users().getUserByUsername(realm, "clientMapper");
            AdminPermissionEvaluator permissionsForAdmin = AdminPermissions.evaluator(session, realm, realm, admin);
            UserModel user = session.users().getUserByUsername(realm, "authorized");
            Assert.assertTrue(permissionsForAdmin.users().canManage(user));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole));
            Assert.assertTrue(permissionsForAdmin.roles().canMapRole(clientRole));
            Assert.assertFalse(permissionsForAdmin.roles().canMapRole(realmRole2));

        }

    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }
    
    //@Test
    public void testDemo() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupDemo);
        Thread.sleep(1000000000);
    }


    //@Test
    public void testEvaluationLocal() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupPolices);
        testingClient.server().run(FineGrainAdminUnitTest::setupUsers);
        testingClient.server().run(FineGrainAdminUnitTest::evaluateLocally);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testRestEvaluation() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupPolices);
        testingClient.server().run(FineGrainAdminUnitTest::setupUsers);

        UserRepresentation user1 = adminClient.realm(TEST).users().search("user1").get(0);
        UserRepresentation anotherAdmin = adminClient.realm(TEST).users().search("anotherAdmin").get(0);
        UserRepresentation groupMember = adminClient.realm(TEST).users().search("groupMember").get(0);
        RoleRepresentation realmRole = adminClient.realm(TEST).roles().get("realm-role").toRepresentation();
        List<RoleRepresentation> realmRoleSet = new LinkedList<>();
        realmRoleSet.add(realmRole);
        RoleRepresentation realmRole2 = adminClient.realm(TEST).roles().get("realm-role2").toRepresentation();
        List<RoleRepresentation> realmRole2Set = new LinkedList<>();
        realmRole2Set.add(realmRole2);
        ClientRepresentation client = adminClient.realm(TEST).clients().findByClientId(CLIENT_NAME).get(0);
        ClientScopeRepresentation scope = adminClient.realm(TEST).clientScopes().findAll().get(0);
        RoleRepresentation clientRole = adminClient.realm(TEST).clients().get(client.getId()).roles().get("client-role").toRepresentation();
        List<RoleRepresentation> clientRoleSet = new LinkedList<>();
        clientRoleSet.add(clientRole);

        // test configure client
        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "clientConfigurer", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                client.setAdminUrl("http://nowhere");
                realmClient.realm(TEST).clients().get(client.getId()).update(client);
                client.setFullScopeAllowed(true);
                try {
                    realmClient.realm(TEST).clients().get(client.getId()).update(client);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());

                }
                client.setFullScopeAllowed(false);
                realmClient.realm(TEST).clients().get(client.getId()).update(client);

                try {
                    realmClient.realm(TEST).clients().get(client.getId()).addDefaultClientScope(scope.getId());
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());

                }

                try {
                    realmClient.realm(TEST).clients().get(client.getId()).getScopeMappings().realmLevel().add(realmRoleSet);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());

                }
            }
        }

        // test illegal impersonation
        if (!IMPERSONATION_DISABLED) {
            Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "nomap-admin", "password", Constants.ADMIN_CLI_CLIENT_ID, null);
            try {
                realmClient.realm(TEST).users().get(user1.getId()).impersonate();
                realmClient.close(); // just in case of cookie settings
                realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                        TEST, "nomap-admin", "password", Constants.ADMIN_CLI_CLIENT_ID, null);
                try {
                    realmClient.realm(TEST).users().get(anotherAdmin.getId()).impersonate();
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }
            } finally {
                realmClient.close();
            }
        }


        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "authorized", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
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
        }

        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "authorizedComposite", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
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
        }
        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "unauthorized", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                Assert.fail("should fail with forbidden exception");
            } catch (ClientErrorException e) {
                Assert.assertEquals(403, e.getResponse().getStatus());
            }
        }
        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "unauthorizedMapper", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                Assert.fail("should fail with forbidden exception");
            } catch (ClientErrorException e) {
                Assert.assertEquals(403, e.getResponse().getStatus());
            }
        }

        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "groupManager", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                realmClient.realm(TEST).users().get(groupMember.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
                List<RoleRepresentation> roles = realmClient.realm(TEST).users().get(groupMember.getId()).roles().clientLevel(client.getId()).listAll();
                Assert.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
                realmClient.realm(TEST).users().get(groupMember.getId()).roles().clientLevel(client.getId()).remove(clientRoleSet);

                roles = realmClient.realm(TEST).users().get(groupMember.getId()).roles().realmLevel().listAvailable();
                Assert.assertEquals(1, roles.size());
                realmClient.realm(TEST).users().get(groupMember.getId()).roles().realmLevel().add(realmRoleSet);
                realmClient.realm(TEST).users().get(groupMember.getId()).roles().realmLevel().remove(realmRoleSet);
                try {
                    realmClient.realm(TEST).users().get(groupMember.getId()).roles().realmLevel().add(realmRole2Set);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }
                try {
                    realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }
            }
        }


        // test client.mapRoles
        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "clientMapper", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                List<RoleRepresentation> roles = realmClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assert.assertTrue(roles.isEmpty());
                realmClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
                roles = realmClient.realm(TEST).users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
                Assert.assertTrue(roles.stream().anyMatch((r) -> {
                    return r.getName().equals("client-role");
                }));
                roles = realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().listAvailable();
                Assert.assertTrue(roles.isEmpty());
                try {
                    realmClient.realm(TEST).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
                    Assert.fail("should fail with forbidden exception");
                } catch (ClientErrorException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }
            }
        }

        // KEYCLOAK-5878

        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "groupViewer", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                // Should only return the list of users that belong to "top" group
                List<UserRepresentation> queryUsers = realmClient.realm(TEST).users().list();
                Assert.assertEquals(queryUsers.size(), 1);
                Assert.assertEquals("groupmember", queryUsers.get(0).getUsername());
                for (UserRepresentation user : queryUsers) {
                    System.out.println(user.getUsername());
                }
            }
        }

        // KEYCLOAK-11261 : user creation via fine grain admin

        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    TEST, "noMapperGroupManager", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
                // Should only return the list of users that belong to "top" group
                List<UserRepresentation> queryUsers = realmClient.realm(TEST).users().list();
                Assert.assertEquals(1, queryUsers.size());

                UserRepresentation newGroupMemberWithoutGroup = createUserRepresentation("new-group-member",
                        "new-group-member@keycloak.org", "New", "Member", true);
                try {
                    ApiUtil.createUserWithAdminClient(realmClient.realm(TEST), newGroupMemberWithoutGroup);
                    Assert.fail("should fail with HTTP response code 403 Forbidden");
                } catch (WebApplicationException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }

                UserRepresentation newEmptyGroupList = createUserRepresentation("new-group-member",
                        "new-group-member@keycloak.org", "New", "Member", true);
                newEmptyGroupList.setGroups(Collections.emptyList());

                try {
                    ApiUtil.createUserWithAdminClient(realmClient.realm(TEST), newEmptyGroupList);
                    Assert.fail("should fail with HTTP response code 403 Forbidden");
                } catch (WebApplicationException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }

                UserRepresentation newGroupMemberWithNonExistentGroup = createUserRepresentation("new-group-member",
                        "new-group-member@keycloak.org", "New", "Member",
                        Arrays.asList("wrong-group"),true);
                try {
                    ApiUtil.createUserWithAdminClient(realmClient.realm(TEST), newGroupMemberWithNonExistentGroup);
                    Assert.fail("should fail with HTTP response code 403 Forbidden");
                } catch (WebApplicationException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }

                UserRepresentation newGroupMemberOfNotManagedGroup = createUserRepresentation("new-group-member",
                        "new-group-member@keycloak.org", "New", "Member",
                        Arrays.asList("restricted-group"),true);
                try {
                    ApiUtil.createUserWithAdminClient(realmClient.realm(TEST), newGroupMemberOfNotManagedGroup);
                    Assert.fail("should fail with HTTP response code 403 Forbidden");
                } catch (WebApplicationException e) {
                    Assert.assertEquals(403, e.getResponse().getStatus());
                }

                UserRepresentation newGroupMember = createUserRepresentation("new-group-member",
                        "new-group-member@keycloak.org", "New", "Member",
                        Arrays.asList("top"), true);
                ApiUtil.createUserWithAdminClient(realmClient.realm(TEST), newGroupMember);

                // Should only return the list of users that belong to "top" group + the new one
                queryUsers = realmClient.realm(TEST).users().list();
                Assert.assertEquals(2, queryUsers.size());
            }
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testMasterRealm() throws Exception {
        // test that master realm can still perform operations when policies are in place
        //
        testingClient.server().run(FineGrainAdminUnitTest::setupPolices);
        testingClient.server().run(FineGrainAdminUnitTest::setupUsers);

        UserRepresentation user1 = adminClient.realm(TEST).users().search("user1").get(0);
        RoleRepresentation realmRole = adminClient.realm(TEST).roles().get("realm-role").toRepresentation();
        List<RoleRepresentation> realmRoleSet = new LinkedList<>();
        realmRoleSet.add(realmRole);
        RoleRepresentation realmRole2 = adminClient.realm(TEST).roles().get("realm-role2").toRepresentation();
        List<RoleRepresentation> realmRole2Set = new LinkedList<>();
        realmRole2Set.add(realmRole);
        ClientRepresentation client = adminClient.realm(TEST).clients().findByClientId(CLIENT_NAME).get(0);
        RoleRepresentation clientRole = adminClient.realm(TEST).clients().get(client.getId()).roles().get("client-role").toRepresentation();
        List<RoleRepresentation> clientRoleSet = new LinkedList<>();
        clientRoleSet.add(clientRole);


        {
            try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting())) {
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
        }

    }

    // KEYCLOAK-5152
    @Test
    public void testMasterRealmWithComposites() throws Exception {
        RoleRepresentation composite = new RoleRepresentation();
        composite.setName("composite");
        composite.setComposite(true);
        adminClient.realm(TEST).roles().create(composite);
        composite = adminClient.realm(TEST).roles().get("composite").toRepresentation();

        ClientRepresentation client = adminClient.realm(TEST).clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation createClient = adminClient.realm(TEST).clients().get(client.getId()).roles().get(AdminRoles.CREATE_CLIENT).toRepresentation();
        RoleRepresentation queryRealms = adminClient.realm(TEST).clients().get(client.getId()).roles().get(AdminRoles.QUERY_REALMS).toRepresentation();
        List<RoleRepresentation> composites = new LinkedList<>();
        composites.add(createClient);
        composites.add(queryRealms);
        adminClient.realm(TEST).rolesById().addComposites(composite.getId(), composites);
    }

    public static void setup5152(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        RoleModel realmAdminRole = realmAdminClient.getRole(AdminRoles.REALM_ADMIN);

        UserModel realmUser = session.users().addUser(realm, "realm-admin");
        realmUser.grantRole(realmAdminRole);
        realmUser.setEnabled(true);
        realmUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
    }

    // KEYCLOAK-5152
    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testRealmWithComposites() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setup5152);

        try (Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                TEST, "realm-admin", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
            RoleRepresentation composite = new RoleRepresentation();
            composite.setName("composite");
            composite.setComposite(true);
            realmClient.realm(TEST).roles().create(composite);
            composite = adminClient.realm(TEST).roles().get("composite").toRepresentation();

            ClientRepresentation client = adminClient.realm(TEST).clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
            RoleRepresentation viewUsers = adminClient.realm(TEST).clients().get(client.getId()).roles().get(AdminRoles.CREATE_CLIENT).toRepresentation();

            List<RoleRepresentation> composites = new LinkedList<>();
            composites.add(viewUsers);
            realmClient.realm(TEST).rolesById().addComposites(composite.getId(), composites);
        }
    }
    // testRestEvaluationMasterRealm
    // testRestEvaluationMasterAdminTestRealm

    // test role deletion that it cleans up authz objects
    public static void setupDeleteTest(KeycloakSession session )  {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel removedRole = realm.addRole("removedRole");
        ClientModel client = realm.addClient("removedClient");
        RoleModel removedClientRole = client.addRole("removedClientRole");
        GroupModel removedGroup = realm.createGroup("removedGroup");
        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.roles().setPermissionsEnabled(removedRole, true);
        management.roles().setPermissionsEnabled(removedClientRole, true);
        management.groups().setPermissionsEnabled(removedGroup, true);
        management.clients().setPermissionsEnabled(client, true);
        management.users().setPermissionsEnabled(true);
    }

    public static void invokeDelete(KeycloakSession session)  {
        RealmModel realm = session.realms().getRealmByName(TEST);
        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        List<Resource> byResourceServer = management.authz().getStoreFactory().getResourceStore().findByResourceServer(management.realmResourceServer());
        Assert.assertEquals(5, byResourceServer.size());
        RoleModel removedRole = realm.getRole("removedRole");
        realm.removeRole(removedRole);
        ClientModel client = realm.getClientByClientId("removedClient");
        RoleModel removedClientRole = client.getRole("removedClientRole");
        client.removeRole(removedClientRole);
        GroupModel group = KeycloakModelUtils.findGroupByPath(realm, "removedGroup");
        realm.removeGroup(group);
        byResourceServer = management.authz().getStoreFactory().getResourceStore().findByResourceServer(management.realmResourceServer());
        Assert.assertEquals(2, byResourceServer.size());
        realm.removeClient(client.getId());
        byResourceServer = management.authz().getStoreFactory().getResourceStore().findByResourceServer(management.realmResourceServer());
        Assert.assertEquals(1, byResourceServer.size());
        management.users().setPermissionsEnabled(false);
        Resource userResource = management.authz().getStoreFactory().getResourceStore().findByName(management.realmResourceServer(), "Users");
        Assert.assertNull(userResource);
        byResourceServer = management.authz().getStoreFactory().getResourceStore().findByResourceServer(management.realmResourceServer());
        Assert.assertEquals(0, byResourceServer.size());
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testRemoveCleanup() throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupDeleteTest);
        testingClient.server().run(FineGrainAdminUnitTest::invokeDelete);
    }

    // KEYCLOAK-5211
    @Test
    public void testCreateRealmCreateClient() throws Exception {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setName("fullScopedClient");
        rep.setClientId("fullScopedClient");
        rep.setFullScopeAllowed(true);
        rep.setSecret("618268aa-51e6-4e64-93c4-3c0bc65b8171");
        rep.setProtocol("openid-connect");
        rep.setPublicClient(false);
        rep.setEnabled(true);
        adminClient.realm("master").clients().create(rep);

        Keycloak realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                "master", "admin", "admin", "fullScopedClient", "618268aa-51e6-4e64-93c4-3c0bc65b8171");
        try {
            RealmRepresentation newRealm=new RealmRepresentation();
            newRealm.setRealm("anotherRealm");
            newRealm.setId("anotherRealm");
            newRealm.setEnabled(true);
            realmClient.realms().create(newRealm);
            
            ClientRepresentation newClient = new ClientRepresentation();

            newClient.setName("newClient");
            newClient.setClientId("newClient");
            newClient.setFullScopeAllowed(true);
            newClient.setSecret("secret");
            newClient.setProtocol("openid-connect");
            newClient.setPublicClient(false);
            newClient.setEnabled(true);
            Response response = realmClient.realm("anotherRealm").clients().create(newClient);
            Assert.assertEquals(403, response.getStatus());
            response.close();

            realmClient.close();
            //creating new client to refresh token
            realmClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
                    "master", "admin", "admin", "fullScopedClient", "618268aa-51e6-4e64-93c4-3c0bc65b8171");
            assertThat(realmClient.realms().findAll().stream().map(RealmRepresentation::getRealm).collect(Collectors.toSet()),
                    hasItem("anotherRealm"));
            response = realmClient.realm("anotherRealm").clients().create(newClient);
            Assert.assertEquals(201, response.getStatus());
            response.close();
        } finally {
            adminClient.realm("anotherRealm").remove();
            realmClient.close();
        }


    }

    // KEYCLOAK-5211
    @Test
    public void testCreateRealmCreateClientWithMaster() throws Exception {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setName("fullScopedClient");
        rep.setClientId("fullScopedClient");
        rep.setFullScopeAllowed(true);
        rep.setSecret("618268aa-51e6-4e64-93c4-3c0bc65b8171");
        rep.setProtocol("openid-connect");
        rep.setPublicClient(false);
        rep.setEnabled(true);
        adminClient.realm("master").clients().create(rep);

        RealmRepresentation newRealm=new RealmRepresentation();
        newRealm.setRealm("anotherRealm");
        newRealm.setId("anotherRealm");
        newRealm.setEnabled(true);
        adminClient.realms().create(newRealm);

        try {
            ClientRepresentation newClient = new ClientRepresentation();

            newClient.setName("newClient");
            newClient.setClientId("newClient");
            newClient.setFullScopeAllowed(true);
            newClient.setSecret("secret");
            newClient.setProtocol("openid-connect");
            newClient.setPublicClient(false);
            newClient.setEnabled(true);
            Response response = adminClient.realm("anotherRealm").clients().create(newClient);
            Assert.assertEquals(201, response.getStatus());
            response.close();            
        } finally {
            adminClient.realm("anotherRealm").remove();

        }
    }

    @Test
    @UncaughtServerErrorExpected
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testTokenExchangeDisabled() throws Exception {
        checkTokenExchange(false);
    }

    /**
     * KEYCLOAK-7406
     *
     * @throws Exception
     */
    @Test
    @UncaughtServerErrorExpected
    @AuthServerContainerExclude(AuthServer.REMOTE)
    @EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
    public void testWithTokenExchange() throws Exception {
        String exchanged = checkTokenExchange(true);
        Assert.assertNotNull(exchanged);
        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                AuthRealm.MASTER, Constants.ADMIN_CLI_CLIENT_ID, exchanged, TLSUtils.initializeTLS())) {
            Assert.assertNotNull(client.realm("master").roles().get("offline_access"));
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testUserPagination() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            session.getContext().setRealm(realm);

            GroupModel customerAGroup = session.groups().createGroup(realm, "Customer A");
            UserModel customerAManager = session.users().addUser(realm, "customer-a-manager");
            customerAManager.credentialManager().updateCredential(UserCredentialModel.password("password"));
            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            customerAManager.grantRole(realmAdminClient.getRole(AdminRoles.QUERY_USERS));
            customerAManager.setEnabled(true);
            UserModel regularAdminUser = session.users().addUser(realm, "regular-admin-user");
            regularAdminUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
            regularAdminUser.grantRole(realmAdminClient.getRole(AdminRoles.VIEW_USERS));
            regularAdminUser.setEnabled(true);

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            GroupPermissionManagement groupPermission = management.groups();

            groupPermission.setPermissionsEnabled(customerAGroup, true);

            UserPolicyRepresentation userPolicyRepresentation = new UserPolicyRepresentation();

            userPolicyRepresentation.setName("Only " + customerAManager.getUsername());
            userPolicyRepresentation.addUser(customerAManager.getId());

            Policy policy = groupPermission.viewMembersPermission(customerAGroup);

            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);

            Policy userPolicy = provider.getStoreFactory().getPolicyStore().create(management.realmResourceServer(), userPolicyRepresentation);

            policy.addAssociatedPolicy(RepresentationToModel.toModel(userPolicyRepresentation, provider, userPolicy));

            for (int i = 0; i < 20; i++) {
                UserModel userModel = session.users().addUser(realm, "a" + i);
                userModel.setFirstName("test");
            }

            for (int i = 20; i < 40; i++) {
                UserModel userModel = session.users().addUser(realm, "b" + i);
                userModel.setFirstName("test");
                userModel.joinGroup(customerAGroup);
            }
        });

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "customer-a-manager", "password", Constants.ADMIN_CLI_CLIENT_ID, TLSUtils.initializeTLS())) {

            List<UserRepresentation> result = client.realm("test").users().search(null, "test", null, null, -1, 20);

            Assert.assertEquals(20, result.size());
            Assert.assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("b"))));

            result = client.realm("test").users().search(null, "test", null, null, 20, 40);

            Assert.assertEquals(0, result.size());
        }

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID, TLSUtils.initializeTLS())) {

            List<UserRepresentation> result = client.realm("test").users().search(null, "test", null, null, -1, 20);

            Assert.assertEquals(20, result.size());
            Assert.assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("a"))));

            client.realm("test").users().search(null, null, null, null, -1, -1);

            Assert.assertEquals(20, result.size());
            Assert.assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("a"))));
        }

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "customer-a-manager", "password", Constants.ADMIN_CLI_CLIENT_ID, TLSUtils.initializeTLS())) {

            List<UserRepresentation> result = client.realm("test").users().search(null, null, null, null, -1, 20);

            Assert.assertEquals(20, result.size());
            Assert.assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("b"))));

            result = client.realm("test").users().search("test", -1, 20, false);

            Assert.assertEquals(20, result.size());
            Assert.assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("b"))));

            result = client.realm("test").users().search("a", -1, 20, false);

            Assert.assertEquals(0, result.size());
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testClientsSearch() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            session.getContext().setRealm(realm);

            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            UserModel regularAdminUser = session.users().addUser(realm, "regular-admin-user");
            regularAdminUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
            regularAdminUser.grantRole(realmAdminClient.getRole(AdminRoles.QUERY_CLIENTS));
            regularAdminUser.setEnabled(true);

            UserPolicyRepresentation userPolicyRepresentation = new UserPolicyRepresentation();

            userPolicyRepresentation.setName("Only " + regularAdminUser.getUsername());
            userPolicyRepresentation.addUser(regularAdminUser.getId());

            for (int i = 0; i < 30; i++) {
                realm.addClient("client-search-" + (i < 10 ? "0" + i : i));
            }

            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            ClientPermissionManagement clientPermission = management.clients();
            ClientModel clientModel = realm.getClientByClientId("client-search-09");

            clientPermission.setPermissionsEnabled(clientModel, true);

            Policy policy = clientPermission.viewPermission(clientModel);
            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
            Policy userPolicy = provider.getStoreFactory().getPolicyStore()
                    .create(management.realmResourceServer(), userPolicyRepresentation);

            policy.addAssociatedPolicy(RepresentationToModel.toModel(userPolicyRepresentation, provider, userPolicy));
        });

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID,
                TLSUtils.initializeTLS())) {

            List<ClientRepresentation> result = client.realm("test").clients().findAll("client-search-", true, true, 0, 5);

            Assert.assertEquals(1, result.size());
            Assert.assertEquals("client-search-09", result.get(0).getClientId());
        }

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            session.getContext().setRealm(realm);

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            ClientPermissionManagement clientPermission = management.clients();
            ClientModel clientModel = realm.getClientByClientId("client-search-10");

            clientPermission.setPermissionsEnabled(clientModel, true);

            Policy policy = clientPermission.viewPermission(clientModel);

            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(realmAdminClient);

            policy.addAssociatedPolicy(provider.getStoreFactory().getPolicyStore().findByName(resourceServer, "Only regular-admin-user"));
        });

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID,
                TLSUtils.initializeTLS())) {

            List<ClientRepresentation> result = client.realm("test").clients().findAll("client-search-", true, true, -1, -1);

            Assert.assertEquals(2, result.size());
        }

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID,
                TLSUtils.initializeTLS())) {

            List<ClientRepresentation> result = client.realm("test").clients().findAll(null, true, false, 0, 5);

            Assert.assertEquals(2, result.size());
        }

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID,
                TLSUtils.initializeTLS())) {

            List<ClientRepresentation> result = client.realm("test").clients().findAll(null, true, false, 0, 1);

            Assert.assertEquals(1, result.size());
            Assert.assertThat(result, Matchers.hasItem(Matchers.hasProperty("clientId", Matchers.is("client-search-09"))));

            result = client.realm("test").clients().findAll(null, true, false, 1, 1);
            Assert.assertThat(result, Matchers.hasItem(Matchers.hasProperty("clientId", Matchers.is("client-search-10"))));

            Assert.assertEquals(1, result.size());

            result = client.realm("test").clients().findAll(null, true, false, 2, 1);

            Assert.assertTrue(result.isEmpty());
        }
        
        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID,
                TLSUtils.initializeTLS())) {

            List<ClientRepresentation> result = client.realm("test").clients().findAll(null, true, false, -1, -1);

            Assert.assertEquals(2, result.size());
        }

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            session.getContext().setRealm(realm);

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            ClientPermissionManagement clientPermission = management.clients();
            for (int i = 11; i < 30; i++) {
                ClientModel clientModel = realm.getClientByClientId("client-search-" + i);

                clientPermission.setPermissionsEnabled(clientModel, true);

                Policy policy = clientPermission.viewPermission(clientModel);

                AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
                ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
                ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(realmAdminClient);

                policy.addAssociatedPolicy(provider.getStoreFactory().getPolicyStore()
                        .findByName(resourceServer, "Only regular-admin-user"));
            }
        });

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID,
                TLSUtils.initializeTLS())) {
            
            List<ClientRepresentation> clients = new ArrayList<>();

            List<ClientRepresentation> result = client.realm("test").clients().findAll("client-search-", true, true, 0, 10);
            clients.addAll(result);
            Assert.assertEquals(10, result.size());
            Assert.assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), Matchers.is(Arrays.asList("client-search-09",
                    "client-search-10",
                    "client-search-11",
                    "client-search-12",
                    "client-search-13",
                    "client-search-14",
                    "client-search-15",
                    "client-search-16",
                    "client-search-17",
                    "client-search-18")));

            result = client.realm("test").clients().findAll("client-search-", true, true, 10, 10);
            clients.addAll(result);
            Assert.assertEquals(10, result.size());
            Assert.assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), Matchers.is(Arrays.asList("client-search-19",
                    "client-search-20",
                    "client-search-21",
                    "client-search-22",
                    "client-search-23",
                    "client-search-24",
                    "client-search-25",
                    "client-search-26",
                    "client-search-27",
                    "client-search-28")));

            result = client.realm("test").clients().findAll("client-search-", true, true, 20, 10);
            clients.addAll(result);
            Assert.assertEquals(1, result.size());
            Assert.assertThat(result, Matchers.hasItems(
                    Matchers.hasProperty("clientId", Matchers.isOneOf("client-search-29"))));
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testClientsSearchAfterFirstPage() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            session.getContext().setRealm(realm);

            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            UserModel regularAdminUser = session.users().addUser(realm, "regular-admin-user");
            regularAdminUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
            regularAdminUser.grantRole(realmAdminClient.getRole(AdminRoles.QUERY_CLIENTS));
            regularAdminUser.setEnabled(true);

            UserPolicyRepresentation userPolicyRepresentation = new UserPolicyRepresentation();

            userPolicyRepresentation.setName("Only " + regularAdminUser.getUsername());
            userPolicyRepresentation.addUser(regularAdminUser.getId());

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            ClientPermissionManagement clientPermission = management.clients();
            
            for (int i = 15; i < 30; i++) {
                ClientModel clientModel = realm.addClient("client-search-" + (i < 10 ? "0" + i : i));
                clientPermission.setPermissionsEnabled(clientModel, true);

                Policy policy = clientPermission.viewPermission(clientModel);

                AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);

                if (i == 15) {
                    provider.getStoreFactory().getPolicyStore()
                            .create(management.realmResourceServer(), userPolicyRepresentation);
                }

                policy.addAssociatedPolicy(provider.getStoreFactory().getPolicyStore()
                        .findByName(management.realmResourceServer(), "Only regular-admin-user"));

            }
        });

        try (Keycloak client = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                "test", "regular-admin-user", "password", Constants.ADMIN_CLI_CLIENT_ID,
                TLSUtils.initializeTLS())) {

            List<ClientRepresentation> clients = new ArrayList<>();

            List<ClientRepresentation> result = client.realm("test").clients().findAll("client-search-", true, true, 0, 10);
            clients.addAll(result);
            Assert.assertEquals(10, result.size());
            Assert.assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), Matchers.is(Arrays.asList(
                    "client-search-15",
                    "client-search-16",
                    "client-search-17",
                    "client-search-18",
                    "client-search-19",
                    "client-search-20",
                    "client-search-21",
                    "client-search-22",
                    "client-search-23",
                    "client-search-24")));

            result = client.realm("test").clients().findAll("client-search-", true, true, 10, 10);
            clients.addAll(result);
            Assert.assertEquals(5, result.size());
            Assert.assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), Matchers.is(Arrays.asList(
                    "client-search-25",
                    "client-search-26",
                    "client-search-27",
                    "client-search-28",
                    "client-search-29")));

            result = client.realm("test").clients().findAll("client-search-", true, true, 20, 10);
            clients.addAll(result);
            Assert.assertTrue(result.isEmpty());
        }
    }

    private String checkTokenExchange(boolean shouldPass) throws Exception {
        testingClient.server().run(FineGrainAdminUnitTest::setupTokenExchange);
        oauth.realm("master");
        oauth.clientId("tokenexclient");
        String exchanged = null;
        String token = oauth.doGrantAccessTokenRequest("password", "admin", "admin").getAccessToken();
        Assert.assertNotNull(token);
        try {
            exchanged = oauth.doTokenExchange("master", token, "admin-cli", "tokenexclient", "password").getAccessToken();
        } catch (AssertionError e) {
            log.info("Error message is expected from oauth: " + e.getMessage());
        }
        if (shouldPass)
            Assert.assertNotNull(exchanged);
        else
            Assert.assertNull(exchanged);
        return exchanged;
    }

    private static void setupTokenExchange(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        ClientModel client = session.clients().getClientByClientId(realm, "tokenexclient");
        if (client != null) {
            return;
        }

        ClientModel tokenexclient = realm.addClient("tokenexclient");
        tokenexclient.setEnabled(true);
        tokenexclient.addRedirectUri("http://localhost:*");
        tokenexclient.setPublicClient(false);
        tokenexclient.setSecret("password");
        tokenexclient.setDirectAccessGrantsEnabled(true);

        // permission for client to client exchange to "target" client
        ClientModel adminCli = realm.getClientByClientId(ConfigUtil.DEFAULT_CLIENT);
        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.clients().setPermissionsEnabled(adminCli, true);
        ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
        clientRep.setName("to");
        clientRep.addClient(tokenexclient.getId());
        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientRep);
        management.clients().exchangeToPermission(adminCli).addAssociatedPolicy(clientPolicy);
    }
}
