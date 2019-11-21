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

package org.keycloak.testsuite.federation.ldap.noimport;

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
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapper;
import org.keycloak.testsuite.federation.ldap.AbstractLDAPTest;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPRoleMappingsNoImportTest extends AbstractLDAPTest {


    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }


    @Override
    protected boolean isImportEnabled() {
        return false;
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
    public void test01ReadMappings() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY);

            ComponentModel roleMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "realmRolesMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            RoleLDAPStorageMapper roleMapper = LDAPTestUtils.getRoleMapper(roleMapperModel, ldapProvider, appRealm);


            LDAPObject maryLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "marykeycloak");
            roleMapper.addRoleMappingInLDAP("realmRole1", maryLdap);
            roleMapper.addRoleMappingInLDAP("realmRole2", maryLdap);
        } );

        testingClient.server().run(session -> {
            session.userCache().clear();
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);
            // make sure we are in no-import mode!
            Assert.assertNull(session.userLocalStorage().getUserByUsername("marykeycloak", appRealm));

            // This role should already exists as it was imported from LDAP
            RoleModel realmRole1 = appRealm.getRole("realmRole1");

            // This role should already exists as it was imported from LDAP
            RoleModel realmRole2 = appRealm.getRole("realmRole2");

            Set<RoleModel> maryRoles = mary.getRealmRoleMappings();
            Assert.assertTrue(maryRoles.contains(realmRole1));
            Assert.assertTrue(maryRoles.contains(realmRole2));

            // Add some role mappings directly into LDAP
            ComponentModel roleMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "realmRolesMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            RoleLDAPStorageMapper roleMapper = LDAPTestUtils.getRoleMapper(roleMapperModel, ldapProvider, appRealm);

            LDAPObject maryLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "marykeycloak");
            deleteRoleMappingsInLDAP(roleMapper, maryLdap, "realmRole1");
            deleteRoleMappingsInLDAP(roleMapper, maryLdap, "realmRole2");
        });

        testingClient.server().run(session -> {
            session.userCache().clear();
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);
            // This role should already exists as it was imported from LDAP
            RoleModel realmRole1 = appRealm.getRole("realmRole1");

            // This role should already exists as it was imported from LDAP
            RoleModel realmRole2 = appRealm.getRole("realmRole2");

            Set<RoleModel> maryRoles = mary.getRealmRoleMappings();
            Assert.assertFalse(maryRoles.contains(realmRole1));
            Assert.assertFalse(maryRoles.contains(realmRole2));
        });
    }


    @Test
    public void test02WriteMappings() {
        testingClient.server().run(session -> {
            session.userCache().clear();
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY);

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);

            // make sure we are in no-import mode
            Assert.assertNull(session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("marykeycloak", appRealm));

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

            RoleModel manageAccountRole = accountApp.getRole(AccountRoles.MANAGE_ACCOUNT);
            RoleModel financeRole1 = financeApp.getRole("financeRole1");
            john.grantRole(financeRole1);
            session.userCache().clear();
        });

        testingClient.server().run(session -> {
            session.userCache().clear();
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);

            // make sure we are in no-import mode
            Assert.assertNull(session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("marykeycloak", appRealm));

            RoleModel realmRole1 = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");
            RoleModel realmRole3 = appRealm.getRole("realmRole3");
            ClientModel accountApp = appRealm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            ClientModel financeApp = appRealm.getClientByClientId("finance");
            RoleModel manageAccountRole = accountApp.getRole(AccountRoles.MANAGE_ACCOUNT);
            RoleModel financeRole1 = financeApp.getRole("financeRole1");

            // 3 - Check that role mappings are in LDAP and hence available through federation

            Set<RoleModel> johnRoles = john.getRoleMappings();
            Assert.assertTrue(johnRoles.contains(realmRole1));
            Assert.assertFalse(johnRoles.contains(realmRole2));
            Assert.assertTrue(johnRoles.contains(realmRole3));
            Assert.assertTrue(johnRoles.contains(financeRole1));
            Assert.assertTrue(johnRoles.contains(manageAccountRole));

            Set<RoleModel> johnRealmRoles = john.getRealmRoleMappings();
            Assert.assertEquals(2, johnRealmRoles.size());
            Assert.assertTrue(johnRealmRoles.contains(realmRole1));
            Assert.assertTrue(johnRealmRoles.contains(realmRole3));

            // account roles are not mapped in LDAP. Those are in Keycloak DB
            Set<RoleModel> johnAccountRoles = john.getClientRoleMappings(accountApp);
            Assert.assertTrue(johnAccountRoles.contains(manageAccountRole));

            Set<RoleModel> johnFinanceRoles = john.getClientRoleMappings(financeApp);
            Assert.assertEquals(1, johnFinanceRoles.size());
            Assert.assertTrue(johnFinanceRoles.contains(financeRole1));

            // 4 - Delete some role mappings and check they are deleted

            john.deleteRoleMapping(realmRole3);
            john.deleteRoleMapping(realmRole1);
            john.deleteRoleMapping(financeRole1);

            johnRoles = john.getRoleMappings();
            Assert.assertFalse(johnRoles.contains(realmRole1));
            Assert.assertFalse(johnRoles.contains(realmRole2));
            Assert.assertFalse(johnRoles.contains(realmRole3));
            Assert.assertFalse(johnRoles.contains(financeRole1));

            // Cleanup
            mary.deleteRoleMapping(realmRole2);
            mary.deleteRoleMapping(realmRole3);
            session.userCache().clear();
        });
    }


    @Test
    public void test03_newUserDefaultRolesNoImportModeTest() throws Exception {

        // Check user group memberships
        testingClient.server().run(session -> {
            session.userCache().clear();
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addOrUpdateRoleLDAPMappers(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY);

            // Set a default role on the realm
            appRealm.addDefaultRole("realmRole1");

            UserModel david = session.users().addUser(appRealm, "davidkeycloak");

            // make sure we are in no-import mode
            Assert.assertNull(session.userLocalStorage().getUserByUsername("davidkeycloak", appRealm));

            RoleModel defaultRole = appRealm.getRole("realmRole1");
            RoleModel realmRole2 = appRealm.getRole("realmRole2");

            Assert.assertNotNull(defaultRole);
            Assert.assertNotNull(realmRole2);

            Set<RoleModel> davidRoles = david.getRealmRoleMappings();

            Assert.assertTrue(davidRoles.contains(defaultRole));
            Assert.assertFalse(davidRoles.contains(realmRole2));

            // Make sure john has not received the default role
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Set<RoleModel> johnRoles = john.getRealmRoleMappings();

            Assert.assertFalse(johnRoles.contains(defaultRole));
        });
    }

    private static void deleteRoleMappingsInLDAP(RoleLDAPStorageMapper roleMapper, LDAPObject ldapUser, String roleName) {
        LDAPObject ldapRole1 = roleMapper.loadLDAPRoleByName(roleName);
        roleMapper.deleteRoleMappingInLDAP(ldapUser, ldapRole1);
    }
}
