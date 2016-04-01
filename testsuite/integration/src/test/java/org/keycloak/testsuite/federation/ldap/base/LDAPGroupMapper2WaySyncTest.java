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

import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.federation.ldap.FederationTestUtils;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapper2WaySyncTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;
    private static String descriptionAttrName = null;

    @Rule
    public KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            Map<String,String> ldapConfig = ldapRule.getConfig();
            ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.WRITABLE.toString());
            ldapConfig.put(LDAPConstants.BATCH_SIZE_FOR_SYNC, "4"); // Issues with pagination on ApacheDS

            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap", -1, -1, 0);
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            descriptionAttrName = ldapFedProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? "displayName" : "description";

            // Add group mapper
            FederationTestUtils.addOrUpdateGroupMapper(appRealm, ldapModel, LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);

            // Remove all LDAP groups
            FederationTestUtils.removeAllLDAPGroups(session, appRealm, ldapModel, "groupsMapper");

            // Add some groups for testing into Keycloak
            removeAllModelGroups(appRealm);

            GroupModel group1 = appRealm.createGroup("group1");
            appRealm.moveGroup(group1, null);
            group1.setSingleAttribute(descriptionAttrName, "group1 - description1");

            GroupModel group11 = appRealm.createGroup("group11");
            appRealm.moveGroup(group11, group1);

            GroupModel group12 = appRealm.createGroup("group12");
            appRealm.moveGroup(group12, group1);
            group12.setSingleAttribute(descriptionAttrName, "group12 - description12");

            GroupModel group2 = appRealm.createGroup("group2");
            appRealm.moveGroup(group2, null);
        }
    });


    @Test
    public void test01_syncNoPreserveGroupInheritance() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            // Update group mapper to skip preserve inheritance and check it will pass now
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateUserFederationMapper(mapperModel);

            // Sync from Keycloak into LDAP
            UserFederationSyncResult syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromKeycloakToFederationProvider(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 4, 0, 0, 0);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");

            // Delete all KC groups now
            removeAllModelGroups(realm);
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group2"));
        } finally {
            keycloakRule.stopSession(session, true);
        }



        session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            // Sync from LDAP back into Keycloak
            UserFederationSyncResult syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 4, 0, 0, 0);

            // Assert groups are imported to keycloak. All are at top level
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(realm, "/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, "/group12");
            GroupModel kcGroup2 = KeycloakModelUtils.findGroupByPath(realm, "/group2");

            Assert.assertEquals(0, kcGroup1.getSubGroups().size());

            Assert.assertEquals("group1 - description1", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup11.getFirstAttribute(descriptionAttrName));
            Assert.assertEquals("group12 - description12", kcGroup12.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup2.getFirstAttribute(descriptionAttrName));

            // test drop non-existing works
            testDropNonExisting(session, realm, mapperModel, ldapProvider);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void test02_syncWithGroupInheritance() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            // Update group mapper to skip preserve inheritance and check it will pass now
            FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true");
            realm.updateUserFederationMapper(mapperModel);

            // Sync from Keycloak into LDAP
            UserFederationSyncResult syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromKeycloakToFederationProvider(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 4, 0, 0, 0);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");

            // Delete all KC groups now
            removeAllModelGroups(realm);
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group2"));
        } finally {
            keycloakRule.stopSession(session, true);
        }



        session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(ldapModel.getId(), "groupsMapper");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            // Sync from LDAP back into Keycloak
            UserFederationSyncResult syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
            FederationTestUtils.assertSyncEquals(syncResult, 4, 0, 0, 0);

            // Assert groups are imported to keycloak. All are at top level
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(realm, "/group1/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, "/group1/group12");
            GroupModel kcGroup2 = KeycloakModelUtils.findGroupByPath(realm, "/group2");

            Assert.assertEquals(2, kcGroup1.getSubGroups().size());

            Assert.assertEquals("group1 - description1", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup11.getFirstAttribute(descriptionAttrName));
            Assert.assertEquals("group12 - description12", kcGroup12.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup2.getFirstAttribute(descriptionAttrName));

            // test drop non-existing works
            testDropNonExisting(session, realm, mapperModel, ldapProvider);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    private static void removeAllModelGroups(RealmModel appRealm) {
        for (GroupModel group : appRealm.getTopLevelGroups()) {
            appRealm.removeGroup(group);
        }
    }

    private void testDropNonExisting(KeycloakSession session, RealmModel realm, UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider) {
        // Put some group directly to LDAP
        FederationTestUtils.createLDAPGroup(session, realm, ldapModel, "group3");

        // Sync and assert our group is still in LDAP
        UserFederationSyncResult syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromKeycloakToFederationProvider(mapperModel, ldapProvider, session, realm);
        FederationTestUtils.assertSyncEquals(syncResult, 0, 4, 0, 0);
        Assert.assertNotNull(FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, realm).loadLDAPGroupByName("group3"));

        // Change config to drop non-existing groups
        FederationTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "true");
        realm.updateUserFederationMapper(mapperModel);

        // Sync and assert group removed from LDAP
        syncResult = new GroupLDAPFederationMapperFactory().create(session).syncDataFromKeycloakToFederationProvider(mapperModel, ldapProvider, session, realm);
        FederationTestUtils.assertSyncEquals(syncResult, 0, 4, 1, 0);
        Assert.assertNull(FederationTestUtils.getGroupMapper(mapperModel, ldapProvider, realm).loadLDAPGroupByName("group3"));
    }
}
