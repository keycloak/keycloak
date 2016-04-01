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

package org.keycloak.testsuite.federation.ldap.base;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.federation.ldap.mappers.membership.MembershipType;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.federation.ldap.FederationTestUtils;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;
    private static String descriptionAttrName = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            FederationTestUtils.addLocalUser(manager.getSession(), appRealm, "mary", "mary@test.com", "password-app");
            FederationTestUtils.addLocalUser(manager.getSession(), appRealm, "john", "john@test.com", "password-app");

            Map<String,String> ldapConfig = ldapRule.getConfig();
            ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.WRITABLE.toString());

            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap", -1, -1, 0);
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            descriptionAttrName = ldapFedProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? "displayName" : "description";

            // Add group mapper
            FederationTestUtils.addOrUpdateGroupMapper(appRealm, ldapModel, LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);

            // Remove all LDAP groups
            FederationTestUtils.removeAllLDAPGroups(session, appRealm, ldapModel, "groupsMapper");

            // Add some groups for testing
            LDAPObject group1 = FederationTestUtils.createLDAPGroup(manager.getSession(), appRealm, ldapModel, "group1", descriptionAttrName, "group1 - description");
            LDAPObject group11 = FederationTestUtils.createLDAPGroup(manager.getSession(), appRealm, ldapModel, "group11");
            LDAPObject group12 = FederationTestUtils.createLDAPGroup(manager.getSession(), appRealm, ldapModel, "group12", descriptionAttrName, "group12 - description");

            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, group1, group11, false);
            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, group1, group12, true);

            // Sync LDAP groups to Keycloak DB
            UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapFedProvider, session, appRealm);

            // Delete all LDAP users
            FederationTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            // Add some LDAP users for testing
            LDAPObject john = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            FederationTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            LDAPObject mary = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "5678");
            FederationTestUtils.updateLDAPPassword(ldapFedProvider, mary, "Password1");

            LDAPObject rob = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "robkeycloak", "Rob", "Brown", "rob@email.org", null, "8910");
            FederationTestUtils.updateLDAPPassword(ldapFedProvider, rob, "Password1");

            LDAPObject james = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jameskeycloak", "James", "Brown", "james@email.org", null, "8910");
            FederationTestUtils.updateLDAPPassword(ldapFedProvider, james, "Password1");

        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Test
    public void test01_ldapOnlyGroupMappings() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");

            UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateUserFederationMapper(mapperModel);

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);

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

            // 2 - Check that group mappings are not in local Keycloak DB (They are in LDAP).

            UserModel johnDb = session.userStorage().getUserByUsername("johnkeycloak", appRealm);
            Set<GroupModel> johnDbGroups = johnDb.getGroups();
            Assert.assertEquals(0, johnDbGroups.size());

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
            mary.leaveGroup(group12);

            johnGroups = john.getGroups();
            Assert.assertEquals(0, johnGroups.size());

        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void test02_readOnlyGroupMappings() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            System.out.println("starting test02_readOnlyGroupMappings");
            RealmModel appRealm = session.realms().getRealmByName("test");

            UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.READ_ONLY.toString());
            appRealm.updateUserFederationMapper(mapperModel);

            UserModel mary = session.users().getUserByUsername("marykeycloak", appRealm);

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

            // Add some group mappings directly into LDAP
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            GroupLDAPFederationMapper groupMapper = FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);

            LDAPObject maryLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "marykeycloak");
            groupMapper.addGroupMappingInLDAP("group1", maryLdap);
            groupMapper.addGroupMappingInLDAP("group11", maryLdap);

            // Add some group mapping to model
            mary.joinGroup(group12);

            // Assert that mary has both LDAP and DB mapped groups
            Set<GroupModel> maryGroups = mary.getGroups();
            Assert.assertEquals(3, maryGroups.size());
            Assert.assertTrue(maryGroups.contains(group1));
            Assert.assertTrue(maryGroups.contains(group11));
            Assert.assertTrue(maryGroups.contains(group12));

            // Assert that access through DB will have just DB mapped groups
            System.out.println("******");
            UserModel maryDB = session.userStorage().getUserByUsername("marykeycloak", appRealm);
            Set<GroupModel> maryDBGroups = maryDB.getGroups();
            Assert.assertFalse(maryDBGroups.contains(group1));
            Assert.assertFalse(maryDBGroups.contains(group11));
            Assert.assertTrue(maryDBGroups.contains(group12));

            // Check through userProvider
            List<UserModel> group1Members = session.users().getGroupMembers(appRealm, group1, 0, 10);
            List<UserModel> group11Members = session.users().getGroupMembers(appRealm, group11, 0, 10);
            List<UserModel> group12Members = session.users().getGroupMembers(appRealm, group12, 0, 10);
            Assert.assertEquals(1, group1Members.size());
            Assert.assertEquals("marykeycloak", group1Members.get(0).getUsername());
            Assert.assertEquals(1, group11Members.size());
            Assert.assertEquals("marykeycloak", group11Members.get(0).getUsername());
            Assert.assertEquals(1, group12Members.size());
            Assert.assertEquals("marykeycloak", group12Members.get(0).getUsername());

            mary.leaveGroup(group12);
            try {
                mary.leaveGroup(group1);
                Assert.fail("It wasn't expected to successfully delete LDAP group mappings in READ_ONLY mode");
            } catch (ModelException expected) {
            }

            // Delete role mappings directly in LDAP
            deleteGroupMappingsInLDAP(groupMapper, maryLdap, "group1");
            deleteGroupMappingsInLDAP(groupMapper, maryLdap, "group11");
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void test03_importGroupMappings() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");

            UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.IMPORT.toString());
            appRealm.updateUserFederationMapper(mapperModel);

            // Add some group mappings directly in LDAP
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            GroupLDAPFederationMapper groupMapper = FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);

            LDAPObject robLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "robkeycloak");
            groupMapper.addGroupMappingInLDAP("group11", robLdap);
            groupMapper.addGroupMappingInLDAP("group12", robLdap);

            // Get user and check that he has requested groupa from LDAP
            UserModel rob = session.users().getUserByUsername("robkeycloak", appRealm);
            Set<GroupModel> robGroups = rob.getGroups();

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

            Assert.assertFalse(robGroups.contains(group1));
            Assert.assertTrue(robGroups.contains(group11));
            Assert.assertTrue(robGroups.contains(group12));

            // Delete some group mappings in LDAP and check that it doesn't have any effect and user still has groups
            deleteGroupMappingsInLDAP(groupMapper, robLdap, "group11");
            deleteGroupMappingsInLDAP(groupMapper, robLdap, "group12");
            robGroups = rob.getGroups();
            Assert.assertTrue(robGroups.contains(group11));
            Assert.assertTrue(robGroups.contains(group12));

            // Delete group mappings through model and verifies that user doesn't have them anymore
            rob.leaveGroup(group11);
            rob.leaveGroup(group12);
            robGroups = rob.getGroups();
            Assert.assertEquals(0, robGroups.size());
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }


    // KEYCLOAK-2682
    @Test
    public void test04_groupReferencingNonExistentMember() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            // Ignoring this test on ActiveDirectory as it's not allowed to have LDAP group referencing nonexistent member. KEYCLOAK-2682 was related to OpenLDAP TODO: Better solution than programmatic...
            LDAPConfig config = FederationTestUtils.getLdapProvider(session, ldapModel).getLdapIdentityStore().getConfig();
            if (config.isActiveDirectory()) {
                return;
            }

            RealmModel appRealm = session.realms().getRealmByName("test");

            UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateUserFederationMapper(mapperModel);

            // 1 - Add some group to LDAP for testing
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            GroupLDAPFederationMapper groupMapper = FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);
            LDAPObject group2 = FederationTestUtils.createLDAPGroup(session, appRealm, ldapModel, "group2", descriptionAttrName, "group2 - description");

            // 2 - Add one existing user rob to LDAP group
            LDAPObject jamesLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "jameskeycloak");
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, group2, jamesLdap, false);

            // 3 - Add non-existing user to LDAP group
            LDAPDn nonExistentDn = LDAPDn.fromString(ldapProvider.getLdapIdentityStore().getConfig().getUsersDn());
            nonExistentDn.addFirst(jamesLdap.getRdnAttributeName(), "nonexistent");
            LDAPObject nonExistentLdapUser = new LDAPObject();
            nonExistentLdapUser.setDn(nonExistentDn);
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, group2, nonExistentLdapUser, true);

            // 4 - Check group members. Just existing user rob should be present
            groupMapper.syncDataFromFederationProviderToKeycloak();
            GroupModel kcGroup2 = KeycloakModelUtils.findGroupByPath(appRealm, "/group2");
            List<UserModel> groupUsers = session.users().getGroupMembers(appRealm, kcGroup2, 0, 5);
            Assert.assertEquals(1, groupUsers.size());
            UserModel rob = groupUsers.get(0);
            Assert.assertEquals("jameskeycloak", rob.getUsername());

        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    private void deleteGroupMappingsInLDAP(GroupLDAPFederationMapper groupMapper, LDAPObject ldapUser, String groupName) {
        LDAPObject ldapGroup = groupMapper.loadLDAPGroupByName(groupName);
        groupMapper.deleteGroupMappingInLDAP(ldapUser, ldapGroup);
    }
}
