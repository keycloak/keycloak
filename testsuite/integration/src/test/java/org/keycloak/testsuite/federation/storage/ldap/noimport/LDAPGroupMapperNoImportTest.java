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

package org.keycloak.testsuite.federation.storage.ldap.noimport;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.testsuite.federation.storage.ldap.LDAPTestUtils;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperNoImportTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static ComponentModel ldapModel = null;
    private static String descriptionAttrName = null;


    static class GroupTestKeycloakSetup extends KeycloakRule.KeycloakSetup {

        private final LDAPRule ldapRule;

        ComponentModel ldapModel = null;
        String descriptionAttrName = null;


        public GroupTestKeycloakSetup(LDAPRule ldapRule) {
            this.ldapRule = ldapRule;
        }


        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            LDAPTestUtils.addLocalUser(manager.getSession(), appRealm, "mary", "mary@test.com", "password-app");
            LDAPTestUtils.addLocalUser(manager.getSession(), appRealm, "john", "john@test.com", "password-app");

            MultivaluedHashMap<String,String> ldapConfig = LDAPTestUtils.getLdapRuleConfig(ldapRule);
            ldapConfig.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setLastSync(0);
            model.setChangedSyncPeriod(-1);
            model.setFullSyncPeriod(-1);
            model.setName("test-ldap");
            model.setPriority(0);
            model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
            model.setConfig(ldapConfig);
            model.setImportEnabled(false);

            ldapModel = appRealm.addComponentModel(model);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            descriptionAttrName = ldapFedProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? "displayName" : "description";

            // Add group mapper
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ldapModel, LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);

            // Remove all LDAP groups
            LDAPTestUtils.removeAllLDAPGroups(session, appRealm, ldapModel, "groupsMapper");

            // Add some groups for testing
            LDAPObject group1 = LDAPTestUtils.createLDAPGroup(manager.getSession(), appRealm, ldapModel, "group1", descriptionAttrName, "group1 - description");
            LDAPObject group11 = LDAPTestUtils.createLDAPGroup(manager.getSession(), appRealm, ldapModel, "group11");
            LDAPObject group12 = LDAPTestUtils.createLDAPGroup(manager.getSession(), appRealm, ldapModel, "group12", descriptionAttrName, "group12 - description");

            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group11, false);
            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group12, true);

            // Sync LDAP groups to Keycloak DB
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "groupsMapper");
            new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(appRealm);

            // Delete all LDAP users
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            // Add some LDAP users for testing
            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            LDAPObject mary = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "5678");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, mary, "Password1");

            LDAPObject rob = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "robkeycloak", "Rob", "Brown", "rob@email.org", null, "8910");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, rob, "Password1");

            LDAPObject james = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jameskeycloak", "James", "Brown", "james@email.org", null, "8910");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, james, "Password1");

            postSetup(appRealm, ldapFedProvider);
        }


        void postSetup(RealmModel appRealm, LDAPStorageProvider ldapProvider) {
            LDAPGroupMapperNoImportTest.ldapModel = this.ldapModel;
            LDAPGroupMapperNoImportTest.descriptionAttrName = this.descriptionAttrName;
        }

    }


    private static KeycloakRule keycloakRule = new KeycloakRule(new GroupTestKeycloakSetup(ldapRule));

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Test
    public void test01ReadGroupMappings() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);

            LDAPObject maryLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "marykeycloak");
            groupMapper.addGroupMappingInLDAP(appRealm, "group1", maryLdap);
            groupMapper.addGroupMappingInLDAP(appRealm, "group11", maryLdap);
        } finally {
            keycloakRule.stopSession(session, true);
        }
        session = keycloakRule.startSession();
        try {
            session.userCache().clear();
            System.out.println("starting test01ReadGroupMappings");
            RealmModel appRealm = session.realms().getRealmByName("test");

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            Assert.assertNotNull(group1);
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            Assert.assertNotNull(group11);
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");
            Assert.assertNotNull(group12);


            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);
            // make sure we are in no-import mode
            Assert.assertNull(session.userLocalStorage().getUserByUsername("marykeycloak", appRealm));
            // Assert that mary has both LDAP and DB mapped groups
            Set<GroupModel> maryGroups = mary.getGroups();
            Assert.assertEquals(2, maryGroups.size());
            Assert.assertTrue(maryGroups.contains(group1));
            Assert.assertTrue(maryGroups.contains(group11));

            // Check through userProvider
            List<UserModel> group1Members = session.users().getGroupMembers(appRealm, group1, 0, 10);
            List<UserModel> group11Members = session.users().getGroupMembers(appRealm, group11, 0, 10);
            Assert.assertEquals(1, group1Members.size());
            Assert.assertEquals("marykeycloak", group1Members.get(0).getUsername());
            Assert.assertEquals(1, group11Members.size());
            Assert.assertEquals("marykeycloak", group11Members.get(0).getUsername());

            // Delete role mappings directly in LDAP
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm,ldapModel, "groupsMapper");
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);
            LDAPObject maryLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "marykeycloak");
            deleteGroupMappingsInLDAP(groupMapper, maryLdap, "group1");
            deleteGroupMappingsInLDAP(groupMapper, maryLdap, "group11");
        } finally {
            keycloakRule.stopSession(session, true);
        }

    }

    @Test
    public void test02WriteGroupMappings() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);

            // make sure we are in no-import mode
            Assert.assertNull(session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("marykeycloak", appRealm));

            // 1 - Grant some groups in LDAP

            // This group should already exists as it was imported from LDAP
            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            john.joinGroup(group1);

            // This group should already exists as it was imported from LDAP
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            mary.joinGroup(group11);

            // This group should already exists as it was imported from LDAP
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");
            john.joinGroup(group12);
            mary.joinGroup(group12);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            session.userCache().clear();  // clear cache to make sure we're reloading from LDAP.
            RealmModel appRealm = session.realms().getRealmByName("test");
            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);

            // make sure we are in no-import mode
            Assert.assertNull(session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("marykeycloak", appRealm));

            // 3 - Check that group mappings are in LDAP and hence available through federation

            Set<GroupModel> johnGroups = john.getGroups();
            Assert.assertEquals(2, johnGroups.size());
            Assert.assertTrue(johnGroups.contains(group1));
            Assert.assertFalse(johnGroups.contains(group11));
            Assert.assertTrue(johnGroups.contains(group12));

            // 4 - Check through userProvider
            List<UserModel> group1Members = session.users().getGroupMembers(appRealm, group1, 0, 10);
            List<UserModel> group11Members = session.users().getGroupMembers(appRealm, group11, 0, 10);
            List<UserModel> group12Members = session.users().getGroupMembers(appRealm, group12, 0, 10);

            Assert.assertEquals(1, group1Members.size());
            Assert.assertEquals("johnkeycloak", group1Members.get(0).getUsername());
            Assert.assertEquals(1, group11Members.size());
            Assert.assertEquals("marykeycloak", group11Members.get(0).getUsername());
            Assert.assertEquals(2, group12Members.size());

            // 4 - Delete some group mappings and check they are deleted

            john.leaveGroup(group1);
            john.leaveGroup(group12);

            mary.leaveGroup(group1);
            mary.leaveGroup(group11);
            mary.leaveGroup(group12);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            session.userCache().clear();  // clear cache to make sure we're reloading from LDAP.
            RealmModel appRealm = session.realms().getRealmByName("test");

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);

            // make sure we are in no-import mode
            Assert.assertNull(session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("marykeycloak", appRealm));
            session.userCache().clear();  // clear cache to make sure we're reloading from LDAP.
            Set<GroupModel> johnGroups = john.getGroups();
            Assert.assertEquals(0, johnGroups.size());
            Set<GroupModel> maryGroups = mary.getGroups();
            Assert.assertEquals(0, maryGroups.size());

        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    private void deleteGroupMappingsInLDAP(GroupLDAPStorageMapper groupMapper, LDAPObject ldapUser, String groupName) {
        LDAPObject ldapGroup = groupMapper.loadLDAPGroupByName(groupName);
        groupMapper.deleteGroupMappingInLDAP(ldapUser, ldapGroup);
    }
}
