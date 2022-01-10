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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.SynchronizationResultRepresentation;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.WaitUtils;

import javax.ws.rs.BadRequestException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPSyncTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        // Don't sync registrations in this test
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            // Add group mapper
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);
            // Remove all LDAP groups
            LDAPTestUtils.removeAllLDAPGroups(session, appRealm, ctx.getLdapModel(), "groupsMapper");
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            ldapModel.put(LDAPConstants.SYNC_REGISTRATIONS, "false");
            appRealm.updateComponent(ldapModel);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "password-app");

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add 5 new users for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            for (int i=1 ; i<=5 ; i++) {
                LDAPObject ldapUser = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", null, "12" + i);
                LDAPTestUtils.updateLDAPPassword(ldapFedProvider, ldapUser, "Password1");
            }

        });
    }


//    @Test
//    public void test01runit() throws Exception {
//        Thread.sleep(10000000);
//    }

    @Test
    public void test01LDAPSync() {
        // wait a bit
        WaitUtils.pause(getLDAPRule().getSleepTime());

        // Sync 5 users from LDAP
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = usersSyncManager.syncAllUsers(sessionFactory, "test", ctx.getLdapModel());
            LDAPTestAsserts.assertSyncEquals(syncResult, 5, 0, 0, 0);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel testRealm = ctx.getRealm();
            UserProvider userProvider = session.userLocalStorage();

            // Assert users imported
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user1", "User1FN", "User1LN", "user1@email.org", "121");
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user2", "User2FN", "User2LN", "user2@email.org", "122");
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user3", "User3FN", "User3LN", "user3@email.org", "123");
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user4", "User4FN", "User4LN", "user4@email.org", "124");
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org", "125");

            // Assert lastSync time updated
            Assert.assertTrue(ctx.getLdapModel().getLastSync() > 0);
            testRealm.getUserStorageProvidersStream().forEachOrdered(persistentFedModel -> {
                if (LDAPStorageProviderFactory.PROVIDER_NAME.equals(persistentFedModel.getProviderId())) {
                    Assert.assertTrue(persistentFedModel.getLastSync() > 0);
                } else {
                    // Dummy provider has still 0
                    Assert.assertEquals(0, persistentFedModel.getLastSync());
                }
            });
        });

        // wait a bit
        WaitUtils.pause(getLDAPRule().getSleepTime());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel testRealm = ctx.getRealm();
            UserProvider userProvider = session.userLocalStorage();
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();

            // Add user to LDAP and update 'user5' in LDAP
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), testRealm, "user6", "User6FN", "User6LN", "user6@email.org", null, "126");
            LDAPObject ldapUser5 = ctx.getLdapProvider().loadLDAPUserByUsername(testRealm, "user5");
            // NOTE: Changing LDAP attributes directly here
            ldapUser5.setSingleAttribute(LDAPConstants.EMAIL, "user5Updated@email.org");
            ldapUser5.setSingleAttribute(LDAPConstants.POSTAL_CODE, "521");
            ctx.getLdapProvider().getLdapIdentityStore().update(ldapUser5);

            // Assert still old users in local provider
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org", "125");
            Assert.assertNull(userProvider.getUserByUsername(testRealm, "user6"));

            // Trigger partial sync
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = usersSyncManager.syncChangedUsers(sessionFactory, "test", ctx.getLdapModel());
            LDAPTestAsserts.assertSyncEquals(syncResult, 1, 1, 0, 0);
        });

        testingClient.server().run(session -> {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userLocalStorage();
            // Assert users updated in local provider
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5updated@email.org", "521");
            LDAPTestAsserts.assertUserImported(userProvider, testRealm, "user6", "User6FN", "User6LN", "user6@email.org", "126");
        });
    }


    @Test
    public void test02duplicateUsernameAndEmailSync() {

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            LDAPTestUtils.addLocalUser(session, ctx.getRealm(), "user7", "user7@email.org", "password");

            // Add user to LDAP with duplicated username "user7"
            LDAPObject duplicatedLdapUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "user7", "User7FN", "User7LN", "user7-something@email.org", null, "126");

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            // Assert syncing from LDAP fails due to duplicated username
            SynchronizationResult result = new UserStorageSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ctx.getLdapModel());
            Assert.assertEquals(1, result.getFailed());

            // Remove "user7" from LDAP
            LDAPObject duplicatedLdapUser = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "user7");
            ctx.getLdapProvider().getLdapIdentityStore().remove(duplicatedLdapUser);

            // Add user to LDAP with duplicated email "user7@email.org"
            duplicatedLdapUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "user7-something", "User7FNN", "User7LNL", "user7@email.org", null, "126");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            // Assert syncing from LDAP fails due to duplicated email
            SynchronizationResult result = new UserStorageSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ctx.getLdapModel());
            Assert.assertEquals(1, result.getFailed());
            Assert.assertNull(session.userLocalStorage().getUserByUsername(ctx.getRealm(), "user7-something"));

            // Update LDAP user to avoid duplicated email
            LDAPObject duplicatedLdapUser = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "user7-something");
            duplicatedLdapUser.setSingleAttribute(LDAPConstants.EMAIL, "user7-changed@email.org");
            ctx.getLdapProvider().getLdapIdentityStore().update(duplicatedLdapUser);

            // Assert user successfully synced now
            result = new UserStorageSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ctx.getLdapModel());
            Assert.assertEquals(0, result.getFailed());
        });

        // Assert user was imported. Use another transaction for that
        testingClient.server().run(session -> {
            RealmModel testRealm = session.realms().getRealm("test");
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), testRealm, "user7-something", "User7FNN", "User7LNL", "user7-changed@email.org", "126");
        });
    }

    @Test
    public void test03LDAPSyncWhenUsernameChanged() {

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

            // Add user to LDAP
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "beckybecks", "Becky", "Becks", "becky-becks@email.org", null, "123");
            SynchronizationResult syncResult = new UserStorageSyncManager().syncAllUsers(sessionFactory, "test", ctx.getLdapModel());
            Assert.assertEquals(0, syncResult.getFailed());
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel testRealm = ctx.getRealm();
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();

            // Update user 'beckybecks' in LDAP
            LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(testRealm, "beckybecks");
            // NOTE: Changing LDAP Username directly here
            String userNameLdapAttributeName = ctx.getLdapProvider().getLdapIdentityStore().getConfig().getUsernameLdapAttribute();
            ldapUser.setSingleAttribute(userNameLdapAttributeName, "beckyupdated");
            ldapUser.setSingleAttribute(LDAPConstants.EMAIL, "becky-updated@email.org");
            ctx.getLdapProvider().getLdapIdentityStore().update(ldapUser);

            // Assert still old users in local provider
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), testRealm, "beckybecks", "Becky", "Becks", "becky-becks@email.org", "123");

            // Trigger partial sync
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = usersSyncManager.syncChangedUsers(sessionFactory, "test", ctx.getLdapModel());
            Assert.assertEquals(0, syncResult.getFailed());
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userLocalStorage();
            // Assert users updated in local provider
            LDAPTestAsserts.assertUserImported(session.users(), testRealm, "beckyupdated", "Becky", "Becks", "becky-updated@email.org", "123");
            UserModel updatedLocalUser = userProvider.getUserByUsername(testRealm, "beckyupdated");
            LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(testRealm, "beckyupdated");
            // Assert old user 'beckybecks' does not exists locally
            Assert.assertNull(userProvider.getUserByUsername(testRealm, "beckybecks"));
            // Assert UUID didn't change
            Assert.assertEquals(updatedLocalUser.getAttributeStream(LDAPConstants.LDAP_ID).findFirst().get(),ldapUser.getUuid());
        });
    }


    // KEYCLOAK-1571
    @Test
    public void test04SameUUIDAndUsernameSync() {
        String origUuidAttrName = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            // Remove all users from model
            session.userLocalStorage().getUsersStream(ctx.getRealm(), true)
                    .collect(Collectors.toList())
                    .forEach(user -> session.userLocalStorage().removeUser(ctx.getRealm(), user));

            // Change name of UUID attribute to same like usernameAttribute
            String uidAttrName = ctx.getLdapProvider().getLdapIdentityStore().getConfig().getUsernameLdapAttribute();
            String origUuidAttrNamee = ctx.getLdapModel().get(LDAPConstants.UUID_LDAP_ATTRIBUTE);
            ctx.getLdapModel().put(LDAPConstants.UUID_LDAP_ATTRIBUTE, uidAttrName);

            // Need to change this due to ApacheDS pagination bug (For other LDAP servers, pagination works fine) TODO: Remove once ApacheDS upgraded and pagination is fixed
            ctx.getLdapModel().put(LDAPConstants.BATCH_SIZE_FOR_SYNC, "10");
            ctx.getRealm().updateComponent(ctx.getLdapModel());

            return origUuidAttrNamee;

        }, String.class);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = new UserStorageSyncManager().syncAllUsers(sessionFactory, "test", ctx.getLdapModel());
            Assert.assertEquals(0, syncResult.getFailed());

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            // Assert users imported with correct LDAP_ID
            LDAPTestAsserts.assertUserImported(session.users(), ctx.getRealm(), "user1", "User1FN", "User1LN", "user1@email.org", "121");
            LDAPTestAsserts.assertUserImported(session.users(), ctx.getRealm(), "user2", "User2FN", "User2LN", "user2@email.org", "122");
            UserModel user1 = session.users().getUserByUsername(ctx.getRealm(), "user1");
            Assert.assertEquals("user1", user1.getFirstAttribute(LDAPConstants.LDAP_ID));
        });

        // Revert config changes
        ComponentRepresentation ldapRep = testRealm().components().component(ldapModelId).toRepresentation();
        ldapRep.getConfig().putSingle(LDAPConstants.UUID_LDAP_ATTRIBUTE, origUuidAttrName);
        testRealm().components().component(ldapModelId).update(ldapRep);
    }

    // KEYCLOAK-1728
    @Test
    public void test05MissingLDAPUsernameSync() {
        String origUsernameAttrName = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            // Remove all users from model
            session.userLocalStorage().getUsersStream(ctx.getRealm(), true)
                    .peek(user -> System.out.println("trying to delete user: " + user.getUsername()))
                    .collect(Collectors.toList())
                    .forEach(user -> {
                        UserCache userCache = session.userCache();
                        if (userCache != null) {
                            userCache.evict(ctx.getRealm(), user);
                        }
                        session.userLocalStorage().removeUser(ctx.getRealm(), user);
                    });

            // Add street mapper and add some user including street
            ComponentModel streetMapper = LDAPTestUtils.addUserAttributeMapper(ctx.getRealm(), ctx.getLdapModel(), "streetMapper", "street", LDAPConstants.STREET);
            LDAPObject streetUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "user8", "User8FN", "User8LN", "user8@email.org", "user8street", "126");

            // Change name of username attribute name to street
            String origUsernameAttrNamee = ctx.getLdapModel().get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, "street");

            // Need to change this due to ApacheDS pagination bug (For other LDAP servers, pagination works fine) TODO: Remove once ApacheDS upgraded and pagination is fixed
            ctx.getLdapModel().put(LDAPConstants.BATCH_SIZE_FOR_SYNC, "10");
            ctx.getRealm().updateComponent(ctx.getLdapModel());

            return origUsernameAttrNamee;

        }, String.class);

        // Just user8 synced. All others failed to sync
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = new UserStorageSyncManager().syncAllUsers(sessionFactory, "test", ctx.getLdapModel());
            Assert.assertEquals(1, syncResult.getAdded());
            Assert.assertTrue(syncResult.getFailed() > 0);
        });

        // Revert config changes
        ComponentRepresentation ldapRep = testRealm().components().component(ldapModelId).toRepresentation();
        if (origUsernameAttrName == null) {
            ldapRep.getConfig().remove(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
        } else {
            ldapRep.getConfig().putSingle(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, origUsernameAttrName);
        }
        testRealm().components().component(ldapModelId).update(ldapRep);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            // Revert config changes
            ComponentModel streetMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "streetMapper");
            ctx.getRealm().removeComponent(streetMapper);
        });
    }

    // KEYCLOAK-10770 user-storage/{id}/sync should return 400 instead of 404
    @Test
    public void test06SyncRestAPIMissingAction() {
        ComponentRepresentation ldapRep = testRealm().components().component(ldapModelId).toRepresentation();

        try {
            SynchronizationResultRepresentation syncResultRep = adminClient.realm("test").userStorage().syncUsers( ldapModelId, null);
            Assert.fail("Should throw 400");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof BadRequestException);
        }
    }

    // KEYCLOAK-10770 user-storage/{id}/sync should return 400 instead of 404
    @Test
    public void test07SyncRestAPIWrongAction() {
        ComponentRepresentation ldapRep = testRealm().components().component(ldapModelId).toRepresentation();

        try {
            SynchronizationResultRepresentation syncResultRep = adminClient.realm("test").userStorage().syncUsers( ldapModelId, "wrong action");
            Assert.fail("Should throw 400");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void test08LDAPGroupSyncAfterGroupRename() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            // Add group mapper
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.READ_ONLY, descriptionAttrName);

            LDAPObject group1 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group1", descriptionAttrName, "group1 - description");
            LDAPObject group2 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group2", descriptionAttrName, "group2 - description");
            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", group2, group1);

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            ctx.getRealm().updateComponent(mapperModel);

            // sync groups to Keycloak
            new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(appRealm);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            Assert.assertEquals("group1 - description", kcGroup1.getFirstAttribute(descriptionAttrName));
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            // Add group mapper
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName);

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);
            LDAPObject group1Loaded = groupMapper.loadLDAPGroupByName("group1");

            // update group name and description
            group1Loaded.setSingleAttribute(group1Loaded.getRdnAttributeNames().get(0), "group5");
            group1Loaded.setSingleAttribute(descriptionAttrName, "group5 - description");
            LDAPTestUtils.updateLDAPGroup(session, appRealm, ctx.getLdapModel(), group1Loaded);

            // sync to Keycloak should pass without an error
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(appRealm);
            Assert.assertThat(syncResult.getFailed(), Matchers.is(0));
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // load previously synced group (a new group has been created in Keycloak)
            GroupModel kcGroup5 = KeycloakModelUtils.findGroupByPath(appRealm, "/group5");
            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            Assert.assertEquals("group5 - description", kcGroup5.getFirstAttribute(descriptionAttrName));
        });
    }

    // KEYCLOAK-14696
    @Test
    public void test09MembershipUsingDifferentAttributes() throws Exception {
        final Map<String, String> previousConf = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Remove all users from model
            session.userLocalStorage().getUsersStream(ctx.getRealm(), true)
                    .peek(user -> System.out.println("trying to delete user: " + user.getUsername()))
                    .collect(Collectors.toList())
                    .forEach(user -> {
                        UserCache userCache = session.userCache();
                        if (userCache != null) {
                            userCache.evict(ctx.getRealm(), user);
                        }
                        session.userLocalStorage().removeUser(ctx.getRealm(), user);
                    });

            Map<String, String> orig = new HashMap<>();
            orig.put(LDAPConstants.RDN_LDAP_ATTRIBUTE, ctx.getLdapModel().getConfig().getFirst(LDAPConstants.RDN_LDAP_ATTRIBUTE));
            orig.put(LDAPConstants.USERS_DN, ctx.getLdapModel().getConfig().getFirst(LDAPConstants.USERS_DN));
            orig.put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, ctx.getLdapModel().getConfig().getFirst(LDAPConstants.USERNAME_LDAP_ATTRIBUTE));

            // create an OU and this test will work below it, set RDN to CN and username to uid/samaccountname
            LDAPTestUtils.addLdapOU(ctx.getLdapProvider(), "KC14696");
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.USERS_DN, "ou=KC14696," + orig.get(LDAPConstants.USERS_DN));
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.RDN_LDAP_ATTRIBUTE, LDAPConstants.CN);
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.USERNAME_LDAP_ATTRIBUTE,
                    ctx.getLdapProvider().getLdapIdentityStore().getConfig().isActiveDirectory()? LDAPConstants.SAM_ACCOUNT_NAME : LDAPConstants.UID);

            ctx.getRealm().updateComponent(ctx.getLdapModel());

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "username");
            mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE,
                    ctx.getLdapProvider().getLdapIdentityStore().getConfig().isActiveDirectory()? LDAPConstants.SAM_ACCOUNT_NAME : LDAPConstants.UID);
            ctx.getRealm().updateComponent(mapperModel);

            LDAPTestUtils.addUserAttributeMapper(appRealm, LDAPTestUtils.getLdapProviderModel(appRealm), "cnMapper", "firstName", LDAPConstants.CN);

            return orig;
        }, Map.class);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // create a user8 inside the usersDn
            LDAPObject user8 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "user8", "User8FN", "User8LN", "user8@email.org", "user8street", "126");

            // create a sample ou inside usersDn
            LDAPTestUtils.addLdapOU(ctx.getLdapProvider(), "sample-org");

            // create a user below the sample org with the same common-name but different username
            String usersDn = ctx.getLdapModel().get(LDAPConstants.USERS_DN);
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.USERS_DN, "ou=sample-org," + usersDn);
            ctx.getRealm().updateComponent(ctx.getLdapModel());
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "user8bis", "User8FN", "User8LN", "user8bis@email.org", "user8street", "126");

            // get back to parent usersDn
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.USERS_DN, usersDn);
            ctx.getRealm().updateComponent(ctx.getLdapModel());

            // create a group with user8 as a member
            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            LDAPObject user8Group = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "user8group", descriptionAttrName, "user8group - description");
            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", user8Group, user8);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = new UserStorageSyncManager().syncAllUsers(sessionFactory, "test", ctx.getLdapModel());
            Assert.assertEquals(2, syncResult.getAdded());
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel user8Group = KeycloakModelUtils.findGroupByPath(appRealm, "/user8group");
            Assert.assertNotNull(user8Group);
            UserModel user8 = session.users().getUserByUsername(appRealm, "user8");
            Assert.assertNotNull(user8);
            UserModel user8Bis = session.users().getUserByUsername(appRealm, "user8bis");
            Assert.assertNotNull(user8Bis);
            Assert.assertTrue("User user8 contains the group", user8.getGroupsStream().collect(Collectors.toSet()).contains(user8Group));
            Assert.assertFalse("User user8bis does not contain the group", user8Bis.getGroupsStream().collect(Collectors.toSet()).contains(user8Group));
            List<String> members = session.users().getGroupMembersStream(appRealm, user8Group).map(u -> u.getUsername()).collect(Collectors.toList());
            Assert.assertEquals("Group contains only user8", members, Collections.singletonList("user8"));
        });

        // revert changes
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            session.users().removeImportedUsers(appRealm, ldapModelId);
            LDAPTestUtils.removeLDAPUserByUsername(ctx.getLdapProvider(), appRealm, ctx.getLdapProvider().getLdapIdentityStore().getConfig(), "user8");
            LDAPTestUtils.removeLDAPUserByUsername(ctx.getLdapProvider(), appRealm, ctx.getLdapProvider().getLdapIdentityStore().getConfig(), "user8bis");
            LDAPObject ou = new LDAPObject();
            ou.setDn(LDAPDn.fromString("ou=sample-org,ou=KC14696," + previousConf.get(LDAPConstants.USERS_DN)));
            ctx.getLdapProvider().getLdapIdentityStore().remove(ou);
            ou.setDn(LDAPDn.fromString("ou=KC14696," + previousConf.get(LDAPConstants.USERS_DN)));
            ctx.getLdapProvider().getLdapIdentityStore().remove(ou);

            for (Map.Entry<String, String> e : previousConf.entrySet()) {
                if (e.getValue() == null) {
                    ctx.getLdapModel().getConfig().remove(e.getKey());
                } else {
                    ctx.getLdapModel().getConfig().putSingle(e.getKey(), e.getValue());
                }
            }
            ctx.getRealm().updateComponent(ctx.getLdapModel());

            ComponentModel cnMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "cnMapper");
            ctx.getRealm().removeComponent(cnMapper);

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "username");
            mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, ctx.getLdapProvider().getLdapIdentityStore().getConfig().getUsernameLdapAttribute());
            ctx.getRealm().updateComponent(mapperModel);
        });
    }
}
