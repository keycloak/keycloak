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

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.SynchronizationResultRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.keycloak.testsuite.util.LDAPTestUtils.getGroupDescriptionLDAPAttrName;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperSyncTest extends AbstractLDAPTest {

    private static Logger logger = Logger.getLogger(LDAPGroupMapperSyncTest.class);

    public static final String TEST_LDAP_GROUPS_SYNC_LINEAR_TIME_GROUPS_COUNT = "test.ldap.groups.sync.linear.time.groups.count";
    public static final String TEST_LDAP_GROUPS_SYNC_LINEAR_TIME_TEST_PERIOD = "test.ldap.groups.sync.linear.time.test.period";

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

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            // Add group mapper
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);

            // Remove all LDAP groups
            LDAPTestUtils.removeAllLDAPGroups(session, appRealm, ctx.getLdapModel(), "groupsMapper");

            // Add some groups for testing
            LDAPObject group1 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group1", descriptionAttrName, "group1 - description");
            LDAPObject group11 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group11");
            LDAPObject group12 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group12", descriptionAttrName, "group12 - description");

            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group11);
            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group12);
        });
    }



    @Before
    public void before() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            realm.getTopLevelGroupsStream().forEach(realm::removeGroup);
        });
    }

    private void testSyncNoPreserveGroupInheritance() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Add recursive group mapping to LDAP. Check that sync with preserve group inheritance will fail
            LDAPObject group1 = groupMapper.loadLDAPGroupByName("group1");
            LDAPObject group12 = groupMapper.loadLDAPGroupByName("group12");
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group12, group1);

            try {
                new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
                Assert.fail("Not expected group sync to pass");
            } catch (ModelException expected) {
                Assert.assertTrue(expected.getMessage().contains("Recursion detected"));
            }

        });

        // Update group mapper to skip preserve inheritance
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            ctx.getRealm().updateComponent(mapperModel);

        });

        // Run the LDAP sync again and check it will pass now
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "groupsMapper");

            new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Assert groups are imported to keycloak. All are at top level
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1");
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(session, realm, "/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(session, realm, "/group12");

            Assert.assertEquals(0, kcGroup1.getSubGroupsStream().count());

            Assert.assertEquals("group1 - description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup11.getFirstAttribute(descriptionAttrName));
            Assert.assertEquals("group12 - description", kcGroup12.getFirstAttribute(descriptionAttrName));

            // Cleanup - remove recursive mapping in LDAP
            LDAPObject group1 = groupMapper.loadLDAPGroupByName("group1");
            LDAPObject group12 = groupMapper.loadLDAPGroupByName("group12");
            LDAPUtils.deleteMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group12, group1);

        });

        // Cleanup - revert (non-default) group mapper config
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true");
            ctx.getRealm().updateComponent(mapperModel);

        });
    }

    @Test
    public void test01_syncNoPreserveGroupInheritance() throws Exception {
        testSyncNoPreserveGroupInheritance();
    }

    @Test
    public void test02_syncNoPreserveGroupInheritanceWithOneGroupMissing() throws Exception {
        Assume.assumeFalse("AD does not allow missing DN in group members",
                LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(ldapRule.getConfig().get(LDAPConstants.VENDOR)));

        testingClient.server().run(session -> {
            // create a non-existent group first in group1
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);
            LDAPObject group1 = groupMapper.loadLDAPGroupByName("group1");
            LDAPObject nonExistentChild = new LDAPObject();
            LDAPDn nonExistentChildDn = group1.getDn().getParentDn();
            nonExistentChildDn.addFirst(LDAPConstants.UID, "non-existent-child");
            nonExistentChild.setDn(nonExistentChildDn);
            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, nonExistentChild);
        });

        testSyncNoPreserveGroupInheritance();
    }

    @Test
    public void test03_syncWithGroupInheritance() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // KEYCLOAK-11415 - This test requires the group mapper to be configured with preserve group inheritance
            // set to 'true' (the default setting). If preservation of group inheritance isn't configured, some of
            // the previous test(s) failed to cleanup properly. Check the requirement as part of running the test
            Assert.assertEquals(mapperModel.getConfig().getFirst("preserve.group.inheritance"), "true");

            // Sync groups with inheritance
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 3, 0, 0, 0);

            // Assert groups are imported to keycloak including their inheritance from LDAP
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1");
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group12"));
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group12");

            Assert.assertEquals(2, kcGroup1.getSubGroupsStream().count());

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
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 0, 3, 0, 0);

            // Assert attributes changed in keycloak
            kcGroup1 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1");
            kcGroup12 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group12");
            Assert.assertEquals("group1 - changed description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup12.getFirstAttribute(descriptionAttrName));
        });
    }


    @Test
    public void test04_syncWithDropNonExistingGroups() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());

            // KEYCLOAK-11415 - This test requires the group mapper to be configured with preserve group inheritance
            // set to 'true' (the default setting). If preservation of group inheritance isn't configured, some of
            // the previous test(s) failed to cleanup properly. Check the requirement as part of running the test
            Assert.assertEquals(mapperModel.getConfig().getFirst("preserve.group.inheritance"), "true");

            // Sync groups with inheritance
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 3, 0, 0, 0);

            // Assert groups are imported to keycloak including their inheritance from LDAP
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1");
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group12"));

            Assert.assertEquals(2, kcGroup1.getSubGroupsStream().count());

            // Create some new groups in keycloak
            GroupModel model1 = realm.createGroup("model1");
            GroupModel model2 = realm.createGroup("model2", kcGroup1);

            // Sync groups again from LDAP. Nothing deleted
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 0, 3, 0, 0);

            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group12"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/model1"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/model2"));

            // Update group mapper to drop non-existing groups during sync
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "true");
            realm.updateComponent(mapperModel);

            // Sync groups again from LDAP. Assert LDAP non-existing groups deleted
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            Assert.assertEquals(3, syncResult.getUpdated());
            Assert.assertTrue(syncResult.getRemoved() == 2);

            // Sync and assert groups updated
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/group12"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(session, realm, "/model1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1/model2"));
        });
    }


    @Test
    public void test05_syncNoPreserveGroupInheritanceWithLazySync() throws Exception {
        // Update group mapper to skip preserve inheritance
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            ctx.getRealm().updateComponent(mapperModel);

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Add user to LDAP and put him as member of group11
            LDAPTestUtils.removeAllLDAPUsers(ldapProvider, realm);
            LDAPObject johnLdap = LDAPTestUtils.addLDAPUser(ldapProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapProvider, johnLdap, "Password1");

            GroupMapperConfig groupMapperConfig = new GroupMapperConfig(mapperModel);
            LDAPObject ldapGroup = groupMapper.loadLDAPGroupByName("group11");
            LDAPUtils.addMember(ldapProvider, groupMapperConfig.getMembershipTypeLdapAttribute(), groupMapperConfig.getMembershipLdapAttribute(),
                    groupMapperConfig.getMembershipUserLdapAttribute(ldapProvider.getLdapIdentityStore().getConfig()), ldapGroup, johnLdap);

            // Assert groups not yet imported to Keycloak DB
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(session, realm, "/group12"));

            // Load user from LDAP to Keycloak DB
            UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
            Set<GroupModel> johnGroups = john.getGroupsStream().collect(Collectors.toSet());

            // Assert just those groups, which john was memberOf exists because they were lazily created
            GroupModel group1 = KeycloakModelUtils.findGroupByPath(session, realm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(session, realm, "/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(session, realm, "/group12");
            Assert.assertNull(group1);
            Assert.assertNotNull(group11);
            Assert.assertNull(group12);

            Assert.assertEquals(1, johnGroups.size());
            Assert.assertTrue(johnGroups.contains(group11));

            // Delete group mapping
            john.leaveGroup(group11);

        });

        // Cleanup - revert (non-default) group mapper config
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true");
            ctx.getRealm().updateComponent(mapperModel);

        });
    }


    @Test
    public void test06SyncRestAPI() {
        ComponentRepresentation groupMapperRep = findMapperRepByName("groupsMapper");

        try {
            // testing KEYCLOAK-3980 which threw an NPE because I was looking up the factory wrong.
            SynchronizationResultRepresentation syncResultRep = adminClient.realm("test").userStorage().syncMapperData( ldapModelId, groupMapperRep.getId(), "error");
            Assert.fail("Should throw 400");
        } catch (BadRequestException e) {
        }
    }


    // KEYCLOAK-8253 - Test if synchronization of large number of LDAP groups takes linear time
    @Ignore("This test is not suitable for regular CI testing due to higher time / performance demand")
    @Test
    public void test07_ldapGroupsSyncHasLinearTimeComplexity() throws Exception {
        // Count of LDAP groups to test the duration of the sync operation. Defaults to 30k unless overridden via system property
        final int GROUPS_COUNT = (System.getProperties().containsKey(TEST_LDAP_GROUPS_SYNC_LINEAR_TIME_GROUPS_COUNT)) ?
                Integer.valueOf(System.getProperty(TEST_LDAP_GROUPS_SYNC_LINEAR_TIME_GROUPS_COUNT)) : 30000;
        // Period on how often (per how many groups) to perform the LDAP groups sync test & report the results back.
        // Defaults to 1k unless overridden via system property
        final int TEST_PERIOD = (System.getProperties().containsKey(TEST_LDAP_GROUPS_SYNC_LINEAR_TIME_TEST_PERIOD)) ?
                Integer.valueOf(System.getProperty(TEST_LDAP_GROUPS_SYNC_LINEAR_TIME_TEST_PERIOD)) : 1000;

        // Reset 'batchSizeForSync' configuration option to the default value of 'LDAPConstants.BATCH_SIZE_FOR_SYNC'
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.BATCH_SIZE_FOR_SYNC, Integer.toString(1000));
            ctx.getRealm().updateComponent(ctx.getLdapModel());

            // Set group mapper to skip preservation of inheritance to test group creation
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            ctx.getRealm().updateComponent(mapperModel);

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            String descriptionAttrName = getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);

            // Remove all LDAP groups
            LDAPTestUtils.removeAllLDAPGroups(session, appRealm, ctx.getLdapModel(), "groupsMapper");

            /* The following for loop doesn't really test true time it took to synchronize N * TEST_PERIOD LDAP groups.
             * Instead of that, in this test only time of syncing last TEST_PERIOD groups is reported. The previously
             *  created groups, existing as the result of (N-1)-th iteration are "just" updated.
             *  Also see NOTE: and the subsequent for loop, commented out, below for details.
             */
            Long elapsedTime = Long.valueOf(0);
            for (int i = 1; i <= GROUPS_COUNT; i++) {
                LDAPTestUtils.createLDAPGroup(session,
                                              appRealm,
                                              ctx.getLdapModel(),
                                              String.format("group-%s", i),
                                              descriptionAttrName,
                                              String.format("Testing group-%s, created at: %s", i, new Date().toString())
                );
                if (i != 0 && i % TEST_PERIOD == 0) {
                    // Start the timer
                    elapsedTime = new Date().getTime();
                    // Sync the LDAP groups
                    groupMapper.syncDataFromFederationProviderToKeycloak(appRealm);
                    elapsedTime = new Date().getTime() - elapsedTime;
                    logger.debugf("Synced %s LDAP groups in %s ms", Long.valueOf(i), elapsedTime);
                }
            }

            /* NOTE: The nested for loop below would be better test to check duration of groups syncing,
             *       since it would delete the LDAP groups created in (N - 1)-th iteration, create count
             *       of LDAP required by N-th iteration, and report back the syncing time. But it is commented
             *       out, because in the current form Apache DS always returns HTTP 505 Internal Server error for
             *       upon reaching 3k groups - in 3-th iteration of the main for loop
             *
            long elapsedTime = new Long(0);
            for (int i = 1; i <= GROUPS_COUNT; i++) {
                int groupsPerIteration = i * TEST_PERIOD;
                logger.debugf("Creating %s LDAP groups", groupsPerIteration);
                for (int j = 1; j <= groupsPerIteration; j++) {
                    LDAPTestUtils.createLDAPGroup(session,
                                                  appRealm,
                                                  ctx.getLdapModel(),
                                                  String.format("group-%s", j),
                                                  descriptionAttrName,
                                                  String.format("Testing group-%s, created at: %s", j, new Date().toString())
                    );
                }
                logger.debugf("Done creating %s LDAP groups!", groupsPerIteration);
                elapsedTime = new Date().getTime();
                groupMapper.syncDataFromFederationProviderToKeycloak(appRealm);
                logger.debugf("Synced %s LDAP groups in %s ms", groupsPerIteration, new Date().getTime() - elapsedTime);
                if (appRealm.getTopLevelGroups().size() != 0) {
                    LDAPTestUtils.removeAllLDAPGroups(session, appRealm, ctx.getLdapModel(), "groupsMapper");
                }
            }*/

        });

    }

    @Test
    public void test08_flatSynchWithBatchSizeLessThanNumberOfGroups() {
        // update the LDAP config to use pagination and a batch size that is less than the number of LDAP groups.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPTestUtils.updateConfigOptions(ldapModel, LDAPConstants.PAGINATION, "true", LDAPConstants.BATCH_SIZE_FOR_SYNC, "1");
            realm.updateComponent(ldapModel);

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateComponent(mapperModel);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");

            // check right config is in place.
            Assert.assertEquals(ctx.getLdapModel().getConfig().getFirst(LDAPConstants.PAGINATION), "true");
            Assert.assertEquals(ctx.getLdapModel().getConfig().getFirst(LDAPConstants.BATCH_SIZE_FOR_SYNC), "1");
            Assert.assertEquals(mapperModel.getConfig().getFirst(GroupMapperConfig.PRESERVE_GROUP_INHERITANCE), "false");

            // synch groups a first time - imports all new groups into keycloak.
            LDAPStorageMapper groupMapper = new GroupLDAPStorageMapperFactory().create(session, mapperModel);
            SynchronizationResult syncResult = groupMapper.syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 3, 0, 0, 0);

            // check all groups were imported as top level groups with no subgroups.
            Stream.of("/group1", "/group11", "/group12").forEach(path -> {
                GroupModel kcGroup = KeycloakModelUtils.findGroupByPath(session, realm, path);
                Assert.assertNotNull(kcGroup);
                Assert.assertEquals(0, kcGroup.getSubGroupsStream().count());
            });

            // re-synch groups, updating previously imported groups.
            syncResult = groupMapper.syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 0, 3, 0, 0);
        });

        // restore pagination, batch size and preserve inheritance configs.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            ldapModel.getConfig().putSingle(LDAPConstants.PAGINATION, "false");
            ldapModel.getConfig().remove(LDAPConstants.BATCH_SIZE_FOR_SYNC);
            realm.updateComponent(ldapModel);

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateComponent(mapperModel);
        });
    }

}
