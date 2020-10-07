/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.Set;
import java.util.stream.Collectors;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperSyncWithGroupsPathTest extends AbstractLDAPTest {

    private static final String LDAP_GROUPS_PATH = "/Applications/App1";

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

            // Create groups path
            GroupModel parentGroup = appRealm.createGroup("Applications");
            appRealm.createGroup("App1", parentGroup);

            // Add group mapper
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName, GroupMapperConfig.LDAP_GROUPS_PATH, LDAP_GROUPS_PATH);

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

            GroupModel groupsPathGroup = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH);
            
            // Subgroup stream needs to be collected, because otherwise we can end up with finding group with id that is
            // already removed
            groupsPathGroup.getSubGroupsStream().collect(Collectors.toSet())
                    .forEach(realm::removeGroup);
        });
    }

    @Test
    public void test01_syncWithGroupInheritance() {
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
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1");
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group11"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group12"));
            GroupModel kcGroup11 = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group11");
            GroupModel kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group12");

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
            kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1");
            kcGroup12 = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group12");
            Assert.assertEquals("group1 - changed description", kcGroup1.getFirstAttribute(descriptionAttrName));
            Assert.assertNull(kcGroup12.getFirstAttribute(descriptionAttrName));
        });
    }

    @Test
    public void test02_syncWithDropNonExistingGroups() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper");

            // KEYCLOAK-11415 - This test requires the group mapper to be configured with preserve group inheritance
            // set to 'true' (the default setting). If preservation of group inheritance isn't configured, some of
            // the previous test(s) failed to cleanup properly. Check the requirement as part of running the test
            Assert.assertEquals(mapperModel.getConfig().getFirst("preserve.group.inheritance"), "true");

            // Sync groups with inheritance
            SynchronizationResult syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 3, 0, 0, 0);

            // Assert groups are imported to keycloak including their inheritance from LDAP
            GroupModel kcGroup1 = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1");
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group12"));

            Assert.assertEquals(2, kcGroup1.getSubGroupsStream().count());

            // Create some new groups in keycloak
            GroupModel groupsPathGroup = KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH);
            realm.createGroup("model1", groupsPathGroup);
            realm.createGroup("model2", kcGroup1);
            realm.createGroup("outside");

            // Sync groups again from LDAP. Nothing deleted
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            LDAPTestAsserts.assertSyncEquals(syncResult, 0, 3, 0, 0);

            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group12"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/model1"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/model2"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/outside"));

            // Update group mapper to drop non-existing groups during sync
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "true");
            realm.updateComponent(mapperModel);

            // Sync groups again from LDAP. Assert LDAP non-existing groups deleted
            syncResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
            Assert.assertEquals(3, syncResult.getUpdated());
            Assert.assertTrue(syncResult.getRemoved() == 2);

            // Sync and assert groups updated
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group11"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/group12"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/model1"));
            Assert.assertNull(KeycloakModelUtils.findGroupByPath(realm, LDAP_GROUPS_PATH + "/group1/model2"));
            Assert.assertNotNull(KeycloakModelUtils.findGroupByPath(realm, "/outside"));
        });
    }

    // KEYCLOAK-14892
    @Test
    public void test03_createConfigurationWithoutGroupPath() throws Exception {
        ComponentRepresentation groupMapperRep = findMapperRepByName("groupsMapper");

        groupMapperRep.setId(null);
        groupMapperRep.setName("different");
        groupMapperRep.getConfig().remove(GroupMapperConfig.LDAP_GROUPS_PATH);
        groupMapperRep.getConfig().remove(GroupMapperConfig.LDAP_GROUPS_PATH);

        // Test that attempt to create configuration without LDAP_GROUPS_PATH configured should be still allowed due the backwards compatibility
        Response response = adminClient.realm("test").components().add(groupMapperRep);
        String newMapperId = ApiUtil.getCreatedId(response);

        try {
            response.close();

            // Load the mapper and assert default group path set
            ComponentRepresentation newMapper = adminClient.realm("test").components().component(newMapperId).toRepresentation();
            Assert.assertEquals(GroupMapperConfig.DEFAULT_LDAP_GROUPS_PATH, newMapper.getConfig().getFirst(GroupMapperConfig.LDAP_GROUPS_PATH));
        } finally {
            // revert new mapper
            adminClient.realm("test").components().component(newMapperId).remove();
        }
    }
}
