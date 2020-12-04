/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.ldap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPRoleMappingsTest extends AbstractLDAPTest {


    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }


    @Override
    protected void afterImportTestRealm() {
        // Disable pagination
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().put(LDAPConstants.PAGINATION, "false");
            appRealm.updateComponent(ctx.getLdapModel());

        });


        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            UserStorageProviderModel ldapModel = ctx.getLdapModel();

            LDAPTestUtils.addLocalUser(session, appRealm, "mary", "mary@test.com", "password-app");

            // Delete all LDAP users
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            // Add sample application
            ClientModel finance = appRealm.addClient("finance");

            // Delete all LDAP roles
            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ldapModel, LDAPGroupMapperMode.LDAP_ONLY);
            LDAPTestUtils.removeAllLDAPRoles(session, appRealm, ldapModel, "realmRolesMapper");
            LDAPTestUtils.removeAllLDAPRoles(session, appRealm, ldapModel, "financeRolesMapper");

            // Add some users for testing
            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            LDAPObject mary = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "5678");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, mary, "Password1");

            LDAPObject rob = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "robkeycloak", "Rob", "Brown", "rob@email.org", null, "8910");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, rob, "Password1");

            // Add some roles for testing
            LDAPTestUtils.createLDAPRole(session, appRealm, ldapModel, "realmRolesMapper", "realmRole1");
            LDAPTestUtils.createLDAPRole(session, appRealm, ldapModel, "realmRolesMapper", "realmRole2");
            LDAPTestUtils.createLDAPRole(session, appRealm, ldapModel, "financeRolesMapper", "financeRole1");

            // Sync LDAP roles to Keycloak DB
            LDAPTestUtils.syncRolesFromLDAP(appRealm, ldapFedProvider, ldapModel);
        });


    }



    @Test
    public void test01_ldapOnlyRoleMappings() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY);

            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");

            // 1 - Grant some roles in LDAP

            // This role should already exists as it was imported from LDAP
            RoleModel realmRole1 = appRealm.getRole("realmRole1");
            john.grantRole(realmRole1);

            // This role should already exists as it was imported from LDAP
            RoleModel realmRole2 = appRealm.getRole("realmRole2");
            mary.grantRole(realmRole2);

            // This role may already exists from previous test (was imported from LDAP), but may not
            RoleModel realmRole3 = appRealm.getRole("realmRole3");
            if (realmRole3 == null) {
                realmRole3 = appRealm.addRole("realmRole3");
            }

            john.grantRole(realmRole3);
            mary.grantRole(realmRole3);

            ClientModel accountApp = appRealm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            ClientModel financeApp = appRealm.getClientByClientId("finance");

            RoleModel financeRole1 = financeApp.getRole("financeRole1");
            john.grantRole(financeRole1);

            // 2 - Check that role mappings are not in local Keycloak DB (They are in LDAP).

            UserModel johnDb = session.userLocalStorage().getUserByUsername(appRealm, "johnkeycloak");
            Set<RoleModel> johnDbRoles = johnDb.getRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertFalse(johnDbRoles.contains(realmRole1));
            Assert.assertFalse(johnDbRoles.contains(realmRole2));
            Assert.assertFalse(johnDbRoles.contains(realmRole3));
            Assert.assertFalse(johnDbRoles.contains(financeRole1));

            // 3 - Check that role mappings are in LDAP and hence available through federation

            Set<RoleModel> johnRoles = john.getRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertTrue(johnRoles.contains(realmRole1));
            Assert.assertFalse(johnRoles.contains(realmRole2));
            Assert.assertTrue(johnRoles.contains(realmRole3));
            Assert.assertTrue(johnRoles.contains(financeRole1));

            Set<RoleModel> johnRealmRoles = john.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertEquals(2, johnRealmRoles.size());
            Assert.assertTrue(johnRealmRoles.contains(realmRole1));
            Assert.assertTrue(johnRealmRoles.contains(realmRole3));

            Set<RoleModel> johnFinanceRoles = john.getClientRoleMappingsStream(financeApp).collect(Collectors.toSet());
            Assert.assertEquals(1, johnFinanceRoles.size());
            Assert.assertTrue(johnFinanceRoles.contains(financeRole1));

            // 4 - Delete some role mappings and check they are deleted

            john.deleteRoleMapping(realmRole3);
            john.deleteRoleMapping(realmRole1);
            john.deleteRoleMapping(financeRole1);

            johnRoles = john.getRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertFalse(johnRoles.contains(realmRole1));
            Assert.assertFalse(johnRoles.contains(realmRole2));
            Assert.assertFalse(johnRoles.contains(realmRole3));
            Assert.assertFalse(johnRoles.contains(financeRole1));

            // Cleanup
            mary.deleteRoleMapping(realmRole2);
            mary.deleteRoleMapping(realmRole3);
        });
    }


    @Test
    public void test02_readOnlyRoleMappings() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.READ_ONLY);

            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");

            RoleModel realmRole1 = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");
            RoleModel realmRole3 = appRealm.getRole("realmRole3");
            if (realmRole3 == null) {
                realmRole3 = appRealm.addRole("realmRole3");
            }

            // Add some role mappings directly into LDAP
            ComponentModel roleMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "realmRolesMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            RoleLDAPStorageMapper roleMapper = LDAPTestUtils.getRoleMapper(roleMapperModel, ldapProvider, appRealm);

            LDAPObject maryLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "marykeycloak");
            roleMapper.addRoleMappingInLDAP("realmRole1", maryLdap);
            roleMapper.addRoleMappingInLDAP("realmRole2", maryLdap);

            // Add some role to model
            mary.grantRole(realmRole3);

            // Assert that mary has both LDAP and DB mapped roles
            Set<RoleModel> maryRoles = mary.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertTrue(maryRoles.contains(realmRole1));
            Assert.assertTrue(maryRoles.contains(realmRole2));
            Assert.assertTrue(maryRoles.contains(realmRole3));

            // Assert that access through DB will have just DB mapped role
            UserModel maryDB = session.userLocalStorage().getUserByUsername(appRealm, "marykeycloak");
            Set<RoleModel> maryDBRoles = maryDB.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertFalse(maryDBRoles.contains(realmRole1));
            Assert.assertFalse(maryDBRoles.contains(realmRole2));
            Assert.assertTrue(maryDBRoles.contains(realmRole3));

            mary.deleteRoleMapping(realmRole3);
            try {
                mary.deleteRoleMapping(realmRole1);
                Assert.fail("It wasn't expected to successfully delete LDAP role mappings in READ_ONLY mode");
            } catch (ModelException expected) {
            }

            // Delete role mappings directly in LDAP
            deleteRoleMappingsInLDAP(roleMapper, maryLdap, "realmRole1");
            deleteRoleMappingsInLDAP(roleMapper, maryLdap, "realmRole2");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");

            // Assert role mappings is not available
            Set<RoleModel> maryRoles = mary.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertFalse(maryRoles.contains(appRealm.getRole("realmRole1")));
            Assert.assertFalse(maryRoles.contains(appRealm.getRole("realmRole2")));
            Assert.assertFalse(maryRoles.contains(appRealm.getRole("realmRole3")));
        });
    }


    @Test
    public void test03_importRoleMappings() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.IMPORT);

            // Add some role mappings directly in LDAP
            ComponentModel roleMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "realmRolesMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            RoleLDAPStorageMapper roleMapper = LDAPTestUtils.getRoleMapper(roleMapperModel, ldapProvider, appRealm);

            LDAPObject robLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "robkeycloak");
            roleMapper.addRoleMappingInLDAP("realmRole1", robLdap);
            roleMapper.addRoleMappingInLDAP("realmRole2", robLdap);

            // Get user and check that he has requested roles from LDAP
            UserModel rob = session.users().getUserByUsername(appRealm, "robkeycloak");
            RoleModel realmRole1 = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");
            RoleModel realmRole3 = appRealm.getRole("realmRole3");
            if (realmRole3 == null) {
                realmRole3 = appRealm.addRole("realmRole3");
            }
            Set<RoleModel> robRoles = rob.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertTrue(robRoles.contains(realmRole1));
            Assert.assertTrue(robRoles.contains(realmRole2));
            Assert.assertFalse(robRoles.contains(realmRole3));

            // Add some role mappings in model and check that user has it
            rob.grantRole(realmRole3);
            robRoles = rob.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertTrue(robRoles.contains(realmRole3));

            // Delete some role mappings in LDAP and check that it doesn't have any effect and user still has role
            deleteRoleMappingsInLDAP(roleMapper, robLdap, "realmRole1");
            deleteRoleMappingsInLDAP(roleMapper, robLdap, "realmRole2");
            robRoles = rob.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertTrue(robRoles.contains(realmRole1));
            Assert.assertTrue(robRoles.contains(realmRole2));

            // Delete role mappings through model and verifies that user doesn't have them anymore
            rob.deleteRoleMapping(realmRole1);
            rob.deleteRoleMapping(realmRole2);
            rob.deleteRoleMapping(realmRole3);
            robRoles = rob.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertFalse(robRoles.contains(realmRole1));
            Assert.assertFalse(robRoles.contains(realmRole2));
            Assert.assertFalse(robRoles.contains(realmRole3));
        });
    }


    private static void deleteRoleMappingsInLDAP(RoleLDAPStorageMapper roleMapper, LDAPObject ldapUser, String roleName) {
        LDAPObject ldapRole1 = roleMapper.loadLDAPRoleByName(roleName);
        roleMapper.deleteRoleMappingInLDAP(ldapUser, ldapRole1);
    }


    /**
     * KEYCLOAK-5698
     */
    @Test
    public void test04_syncRoleMappings() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "johnrolemapper", "John", "RoleMapper", "johnrolemapper@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapProvider, john, "Password1");
            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY);
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            SynchronizationResult syncResult = usersSyncManager.syncChangedUsers(session.getKeycloakSessionFactory(),
                    appRealm.getId(), new UserStorageProviderModel(ctx.getLdapModel()));
            syncResult.getAdded();
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // make sure user is cached.
            UserModel johnRoleMapper = session.users().getUserByUsername(appRealm, "johnrolemapper");
            Assert.assertNotNull(johnRoleMapper);
            Assert.assertEquals(0, johnRoleMapper.getRealmRoleMappingsStream().count());

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Add some role mappings directly in LDAP
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            ComponentModel roleMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "realmRolesMapper");
            RoleLDAPStorageMapper roleMapper = LDAPTestUtils.getRoleMapper(roleMapperModel, ldapProvider, appRealm);

            LDAPObject johnLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "johnrolemapper");
            roleMapper.addRoleMappingInLDAP("realmRole1", johnLdap);
            roleMapper.addRoleMappingInLDAP("realmRole2", johnLdap);

            // Get user and check that he has requested roles from LDAP
            UserModel johnRoleMapper = session.users().getUserByUsername(appRealm, "johnrolemapper");
            RoleModel realmRole1 = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");

            Set<RoleModel> johnRoles = johnRoleMapper.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertFalse(johnRoles.contains(realmRole1));
            Assert.assertFalse(johnRoles.contains(realmRole2));
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Add some role mappings directly in LDAP
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            ComponentModel roleMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "realmRolesMapper");
            RoleLDAPStorageMapper roleMapper = LDAPTestUtils.getRoleMapper(roleMapperModel, ldapProvider, appRealm);

            LDAPObject johnLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "johnrolemapper");
            //not sure why it is here for second time, but it is failing for Active directory - mapping already exists
            if (!ctx.getLdapProvider().getLdapIdentityStore().getConfig().isActiveDirectory()){
                roleMapper.addRoleMappingInLDAP("realmRole1", johnLdap);
                roleMapper.addRoleMappingInLDAP("realmRole2", johnLdap);
            }

            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            SynchronizationResult syncResult = usersSyncManager.syncChangedUsers(session.getKeycloakSessionFactory(),
                    appRealm.getId(), new UserStorageProviderModel(ctx.getLdapModel()));
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Get user and check that he has requested roles from LDAP
            UserModel johnRoleMapper = session.users().getUserByUsername(appRealm, "johnrolemapper");
            RoleModel realmRole1 = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");

            Set<RoleModel> johnRoles = johnRoleMapper.getRealmRoleMappingsStream().collect(Collectors.toSet());
            Assert.assertTrue(johnRoles.contains(realmRole1));
            Assert.assertTrue(johnRoles.contains(realmRole2));
        });

    }


    // KEYCLOAK-5848
    // Test GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE with custom 'Member-Of LDAP Attribute'. As a workaround, we are testing this with custom attribute "street"
    // just because it's available on all the LDAP servers
    @Test
    public void test05_getRolesFromUserMemberOfStrategyTest() throws Exception {
        ComponentRepresentation realmRoleMapper = findMapperRepByName("realmRolesMapper");

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Create street attribute mapper
            LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "streetMapper", "street", LDAPConstants.STREET);

            // Find DN of "group1"
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "realmRolesMapper");
            RoleLDAPStorageMapper roleMapper = LDAPTestUtils.getRoleMapper(mapperModel, ctx.getLdapProvider(), appRealm);
            LDAPObject ldapRole = roleMapper.loadLDAPRoleByName("realmRole1");
            String ldapRoleDN = ldapRole.getDn().toString();

            // Create new user in LDAP. Add him some "street" referencing existing LDAP Group
            LDAPObject carlos = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "carloskeycloak", "Carlos", "Doel", "carlos.doel@email.org", ldapRoleDN, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), carlos, "Password1");

            // Update group mapper
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel,
                    RoleMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, RoleMapperConfig.GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE,
                    RoleMapperConfig.MEMBEROF_LDAP_ATTRIBUTE, LDAPConstants.STREET);
            appRealm.updateComponent(mapperModel);
        });

        ComponentRepresentation streetMapper = findMapperRepByName("streetMapper");

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Get user in Keycloak. Ensure that he is member of requested group
            UserModel carlos = session.users().getUserByUsername(appRealm, "carloskeycloak");
            Set<RoleModel> carlosRoles = carlos.getRealmRoleMappingsStream().collect(Collectors.toSet());

            RoleModel realmRole1 = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");

            Assert.assertTrue(carlosRoles.contains(realmRole1));
            Assert.assertFalse(carlosRoles.contains(realmRole2));
        });

        // Revert mappers
        testRealm().components().component(streetMapper.getId()).remove();
        testRealm().components().component(realmRoleMapper.getId()).remove();
        realmRoleMapper.setId(null);
        testRealm().components().add(realmRoleMapper);
    }

    @Test
    public void test06_newUserDefaultRolesImportModeTest() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel david = session.users().addUser(appRealm, "davidkeycloak");

            RoleModel defaultRole = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");

            Assert.assertNotNull(defaultRole);
            Assert.assertNotNull(realmRole2);

            // Set a default role on the realm
            appRealm.addToDefaultRoles(defaultRole);

            Set<RoleModel> davidRoles = david.getRealmRoleMappingsStream().collect(Collectors.toSet());

            // default role is not assigned directly
            Assert.assertFalse(davidRoles.contains(defaultRole));
            Assert.assertFalse(davidRoles.contains(realmRole2));

            // but david should have the role as effective
            Assert.assertTrue(david.hasRole(defaultRole));
            Assert.assertFalse(david.hasRole(realmRole2));
        });
    }
}
