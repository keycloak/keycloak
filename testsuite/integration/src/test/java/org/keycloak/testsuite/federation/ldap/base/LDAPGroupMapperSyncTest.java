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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.federation.ldap.mappers.membership.MembershipType;
import org.keycloak.federation.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapperFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
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
public class LDAPGroupMapperSyncTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;
    private static String descriptionAttrName = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
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
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Before
    public void before() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            List<GroupModel> kcGroups = realm.getTopLevelGroups();
            for (GroupModel kcGroup : kcGroups) {
                realm.removeGroup(kcGroup);
            }
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void test01_syncNoPreserveGroupInheritance() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            GroupLDAPFederationMapper groupMapper = FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Add recursive group mapping to LDAP. Check that sync with preserve group inheritance will fail
            LDAPObject group1 = groupMapper.loadLDAPGroupByName("group1");
            LDAPObject group12 = groupMapper.loadLDAPGroupByName("group12");
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, group12, group1, true);

            try {
                new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
                Assert.fail("Not expected group sync to pass");
            } catch (ModelException expected) {
                Assert.assertTrue(expected.getMessage().contains("Recursion detected"));
            }

            // Update group mapper to skip preserve inheritance and check it will pass now
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateUserFederationMapper(mapperModel);

            new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);

            // Assert groups are imported to keycloak. All are at top level
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(realm, "/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, "/group12");

            Assert.assertEquals(0, kcGroup1.getSubGroups().size());

            Assert.assertEquals("group1 - description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup11.getFirstAttribute(descriptionAttrName));
            Assert.assertEquals("group12 - description", kcGroup12.getFirstAttribute(descriptionAttrName));

            // Cleanup - remove recursive mapping in LDAP
            LDAPUtils.deleteMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, group12, group1, true);

        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void test02_syncWithGroupInheritance() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            GroupLDAPFederationMapper groupMapper = FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Sync groups with inheritance
            UserFederationSyncResult syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 3, 0, 0, 0);

            // Assert groups are imported to keycloak including their inheritance from LDAP
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group12"));
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(realm, "/group1/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, "/group1/group12");

            Assert.assertEquals(2, kcGroup1.getSubGroups().size());

            Assert.assertEquals("group1 - description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup11.getFirstAttribute(descriptionAttrName));
            Assert.assertEquals("group12 - description", kcGroup12.getFirstAttribute(descriptionAttrName));

            // Update description attributes in LDAP
            LDAPObject group1 = groupMapper.loadLDAPGroupByName("group1");
            group1.setSingleAttribute(descriptionAttrName, "group1 - changed description");
            ldapProvider.getLdapIdentityStore().update(group1);

            LDAPObject group12 = groupMapper.loadLDAPGroupByName("group12");
            group12.setAttribute(descriptionAttrName, null);
            ldapProvider.getLdapIdentityStore().update(group12);

            // Sync and assert groups updated
            syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 0, 3, 0, 0);

            // Assert attributes changed in keycloak
            kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, "/group1/group12");
            Assert.assertEquals("group1 - changed description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup12.getFirstAttribute(descriptionAttrName));
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void test03_syncWithDropNonExistingGroups() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            // Sync groups with inheritance
            UserFederationSyncResult syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 3, 0, 0, 0);

            // Assert groups are imported to keycloak including their inheritance from LDAP
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group12"));

            Assert.assertEquals(2, kcGroup1.getSubGroups().size());

            // Create some new groups in keycloak
            GroupModel model1 = realm.createGroup("model1");
            realm.moveGroup(model1, null);
            GroupModel model2 = realm.createGroup("model2");
            kcGroup1.addChild(model2);

            // Sync groups again from LDAP. Nothing deleted
            syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 0, 3, 0, 0);

            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group12"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/model1"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/model2"));

            // Update group mapper to drop non-existing groups during sync
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "true");
            realm.updateUserFederationMapper(mapperModel);

            // Sync groups again from LDAP. Assert LDAP non-existing groups deleted
            syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
            Assert.assertEquals(3, syncResult.getUpdated());
            Assert.assertTrue(syncResult.getRemoved() == 2);

            // Sync and assert groups updated
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group12"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/model1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/model2"));
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }



    @Test
    public void test04_syncNoPreserveGroupInheritanceWithLazySync() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            GroupLDAPFederationMapper groupMapper = FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Update group mapper to skip preserve inheritance
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateUserFederationMapper(mapperModel);

            // Add user to LDAP and put him as member of group11
            FederationTestUtils.removeAllLDAPUsers(ldapProvider, realm);
            LDAPObject johnLdap = FederationTestUtils.addLDAPUser(ldapProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            FederationTestUtils.updateLDAPPassword(ldapProvider, johnLdap, "Password1");
            groupMapper.addGroupMappingInLDAP("group11", johnLdap);

            // Assert groups not yet imported to Keycloak DB
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group12"));

            // Load user from LDAP to Keycloak DB
            UserModel john = session.users().getUserByUsername("johnkeycloak", realm);
            Set<GroupModel> johnGroups = john.getGroups();

            // Assert just those groups, which john was memberOf exists because they were lazily created
            GroupModel group1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(realm, "/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(realm, "/group12");
            Assert.assertNull(group1);
            Assert.assertNotNull(group11);
            Assert.assertNull(group12);

            Assert.assertEquals(1, johnGroups.size());
            Assert.assertTrue(johnGroups.contains(group11));

            // Delete group mapping
            john.leaveGroup(group11);

        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

}
