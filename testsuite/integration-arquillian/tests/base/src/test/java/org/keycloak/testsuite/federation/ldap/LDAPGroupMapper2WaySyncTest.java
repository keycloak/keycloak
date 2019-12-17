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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapper2WaySyncTest extends AbstractLDAPTest {


    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().put(LDAPConstants.BATCH_SIZE_FOR_SYNC, "4"); // Issues with pagination on ApacheDS
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }


    @Before
    public void before() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            // Add group mapper
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);

            // Remove all LDAP groups
            LDAPTestUtils.removeAllLDAPGroups(session, appRealm, ctx.getLdapModel(), "groupsMapper");

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

        });
    }


    @Test
    public void test01_syncNoPreserveGroupInheritance() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());

            // Update group mapper to skip preserve inheritance and check it will pass now
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateComponent(mapperModel);

            // Sync from Keycloak into LDAP
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromKeycloakToFederationProvider(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 4, 0, 0, 0);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            // Delete all KC groups now
            removeAllModelGroups(realm);
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group2"));
        });



        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");

            // Sync from LDAP back into Keycloak
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 4, 0, 0, 0);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");

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
            testDropNonExisting(session, ctx, mapperModel);
        });
    }

    @Test
    public void test02_syncWithGroupInheritance() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());

            // Update group mapper to skip preserve inheritance and check it will pass now
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "false");
            realm.updateComponent(mapperModel);

            // Sync from Keycloak into LDAP
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromKeycloakToFederationProvider(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 4, 0, 0, 0);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            // Delete all KC groups now
            removeAllModelGroups(realm);
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group2"));
        });


        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            // Sync from LDAP back into Keycloak
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 4, 0, 0, 0);

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
            testDropNonExisting(session, ctx, mapperModel);
        });
    }


    private static void removeAllModelGroups(RealmModel appRealm) {
        for (GroupModel group : appRealm.getTopLevelGroups()) {
            appRealm.removeGroup(group);
        }
    }

    private static void testDropNonExisting(KeycloakSession session, LDAPTestContext ctx, ComponentModel mapperModel) {
        RealmModel realm = ctx.getRealm();

        // Put some group directly to LDAP
        LDAPTestUtils.createLDAPGroup(session, realm, ctx.getLdapModel(), "group3");

        // Sync and assert our group is still in LDAP
        SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromKeycloakToFederationProvider(realm);
        LDAPTestAsserts.assertSyncEquals(syncResult, 0, 4, 0, 0);
        Assert.assertNotNull(LDAPTestUtils.getGroupMapper(mapperModel, ctx.getLdapProvider(), realm).loadLDAPGroupByName("group3"));

        // Change config to drop non-existing groups
        LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "true");
        realm.updateComponent(mapperModel);

        // Sync and assert group removed from LDAP
        syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromKeycloakToFederationProvider(realm);
        LDAPTestAsserts.assertSyncEquals(syncResult, 0, 4, 1, 0);
        Assert.assertNull(LDAPTestUtils.getGroupMapper(mapperModel, ctx.getLdapProvider(), realm).loadLDAPGroupByName("group3"));
    }
}
