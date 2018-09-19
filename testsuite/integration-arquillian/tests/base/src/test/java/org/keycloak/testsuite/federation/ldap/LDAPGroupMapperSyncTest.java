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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.SynchronizationResultRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Set;

import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperSyncTest extends AbstractLDAPTest {


    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Deployment
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class, AbstractLDAPTest.class)
                .addPackages(true,
                        "org.keycloak.testsuite",
                        "org.keycloak.testsuite.federation.ldap");
    }


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

            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group11, false);
            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, group12, true);

        });
    }



    @Before
    public void before() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            List<GroupModel> kcGroups = realm.getTopLevelGroups();
            for (GroupModel kcGroup : kcGroups) {
                realm.removeGroup(kcGroup);
            }
        });
    }

    @Test
    public void test01_syncNoPreserveGroupInheritance() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Add recursive group mapping to LDAP. Check that sync with preserve group inheritance will fail
            LDAPObject group1 = groupMapper.loadLDAPGroupByName("group1");
            LDAPObject group12 = groupMapper.loadLDAPGroupByName("group12");
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group12, group1, true);

            try {
                new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
                Assert.fail("Not expected group sync to pass");
            } catch (ModelException expected) {
                Assert.assertTrue(expected.getMessage().contains("Recursion detected"));
            }

            // Update group mapper to skip preserve inheritance and check it will pass now
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateComponent(mapperModel);

            new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);

            // Assert groups are imported to keycloak. All are at top level
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(realm, "/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, "/group12");

            Assert.assertEquals(0, kcGroup1.getSubGroups().size());

            Assert.assertEquals("group1 - description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup11.getFirstAttribute(descriptionAttrName));
            Assert.assertEquals("group12 - description", kcGroup12.getFirstAttribute(descriptionAttrName));

            // Cleanup - remove recursive mapping in LDAP
            LDAPUtils.deleteMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group12, group1);

            // Cleanup - revert group mapper config
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true");
            realm.updateComponent(mapperModel);

        });
    }


    @Test
    public void test02_syncWithGroupInheritance() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Sync groups with inheritance
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 3, 0, 0, 0);

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
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 0, 3, 0, 0);

            // Assert attributes changed in keycloak
            kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, "/group1/group12");
            Assert.assertEquals("group1 - changed description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup12.getFirstAttribute(descriptionAttrName));
        });
    }

    @Test
    public void test03_syncWithDropNonExistingGroups() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());

            // Sync groups with inheritance
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 3, 0, 0, 0);

            // Assert groups are imported to keycloak including their inheritance from LDAP
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, "/group1");
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group12"));

            Assert.assertEquals(2, kcGroup1.getSubGroups().size());

            // Create some new groups in keycloak
            GroupModel model1 = realm.createGroup("model1");
            realm.moveGroup(model1, null);
            GroupModel model2 = realm.createGroup("model2");
            realm.moveGroup(model2, kcGroup1);

            // Sync groups again from LDAP. Nothing deleted
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 0, 3, 0, 0);

            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group12"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/model1"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/model2"));

            // Update group mapper to drop non-existing groups during sync
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "true");
            realm.updateComponent(mapperModel);

            // Sync groups again from LDAP. Assert LDAP non-existing groups deleted
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            Assert.assertEquals(3, syncResult.getUpdated());
            Assert.assertTrue(syncResult.getRemoved() == 2);

            // Sync and assert groups updated
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/group12"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/model1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, "/group1/model2"));
        });
    }



    @Test
    public void test04_syncNoPreserveGroupInheritanceWithLazySync() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, realm);

            // Update group mapper to skip preserve inheritance
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false");
            realm.updateComponent(mapperModel);

            // Add user to LDAP and put him as member of group11
            LDAPTestUtils.removeAllLDAPUsers(ldapProvider, realm);
            LDAPObject johnLdap = LDAPTestUtils.addLDAPUser(ldapProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapProvider, johnLdap, "Password1");

            GroupMapperConfig groupMapperConfig = new GroupMapperConfig(mapperModel);
            LDAPObject ldapGroup = groupMapper.loadLDAPGroupByName("group11");
            LDAPUtils.addMember(ldapProvider, groupMapperConfig.getMembershipTypeLdapAttribute(), groupMapperConfig.getMembershipLdapAttribute(),
                    groupMapperConfig.getMembershipUserLdapAttribute(ldapProvider.getLdapIdentityStore().getConfig()), ldapGroup, johnLdap, true);

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

        });
    }


    @Test
    public void test05SyncRestAPI() {
        ComponentRepresentation groupMapperRep = findMapperRepByName("groupsMapper");

        try {
            // testing KEYCLOAK-3980 which threw an NPE because I was looking up the factory wrong.
            SynchronizationResultRepresentation syncResultRep = adminClient.realm("test").userStorage().syncMapperData( ldapModelId, groupMapperRep.getId(), "error");
            Assert.fail("Should throw 400");
        } catch (BadRequestException e) {
        }
    }

}
