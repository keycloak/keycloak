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
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import javax.naming.directory.SearchControls;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.testsuite.util.LDAPTestUtils.getGroupDescriptionLDAPAttrName;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.testing().ldap(TEST_REALM_NAME).prepareGroupsLDAPTest();
    }



    @Test
    public void test01_ldapOnlyGroupMappings() {
        test01_ldapOnlyGroupMappings(true);
    }


    protected void test01_ldapOnlyGroupMappings(boolean importEnabled) {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");

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

            // This group should already exists as it was imported from LDAP
            GroupModel groupWithSlashesInName = KeycloakModelUtils.findGroupByPath(appRealm, "Team 2016/2017");
            john.joinGroup(groupWithSlashesInName);
            mary.joinGroup(groupWithSlashesInName);

            // This group should already exists as it was imported from LDAP
            GroupModel groupChildWithSlashesInName = KeycloakModelUtils.findGroupByPath(appRealm, "defaultGroup1/Team Child 2018/2019");
            john.joinGroup(groupChildWithSlashesInName);
            mary.joinGroup(groupChildWithSlashesInName);

            Assert.assertEquals("Team SubChild 2020/2021", KeycloakModelUtils.findGroupByPath(appRealm, "defaultGroup1/Team Child 2018/2019/Team SubChild 2020/2021").getName());
            Assert.assertEquals("defaultGroup14", KeycloakModelUtils.findGroupByPath(appRealm, "defaultGroup13/Team SubChild 2022/2023/A/B/C/D/E/defaultGroup14").getName());
            Assert.assertEquals("Team SubChild 2026/2027", KeycloakModelUtils.findGroupByPath(appRealm, "Team Root 2024/2025/A/B/C/D/defaultGroup15/Team SubChild 2026/2027").getName());
        });


        // 2 - Check that group mappings are not in local Keycloak DB (They are in LDAP).
        if (importEnabled) {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();

                UserModel johnDb = session.userLocalStorage().getUserByUsername(appRealm, "johnkeycloak");
                Assert.assertEquals(2, johnDb.getGroupsStream().count());
                Assert.assertEquals(2, johnDb.getGroupsStream("Gr", 0, 10).count());
                Assert.assertEquals(1, johnDb.getGroupsStream("Gr", 1, 10).count());
                Assert.assertEquals(1, johnDb.getGroupsStream("Gr", 0, 1).count());
                Assert.assertEquals(1, johnDb.getGroupsStream("12", 0, 10).count());

                long dbGroupCount = johnDb.getGroupsCount();
                Assert.assertEquals(2, dbGroupCount);
            });
        }


        // 3 - Check that group mappings are in LDAP and hence available through federation
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");
            GroupModel groupTeam20162017 = KeycloakModelUtils.findGroupByPath(appRealm, "Team 2016/2017");
            GroupModel groupTeamChild20182019 = KeycloakModelUtils.findGroupByPath(appRealm, "defaultGroup1/Team Child 2018/2019");
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");

            Set<GroupModel> johnGroups = john.getGroupsStream().collect(Collectors.toSet());
            Assert.assertEquals(4, johnGroups.size());
            long groupCount = john.getGroupsCount();
            Assert.assertEquals(4, groupCount);
            Assert.assertTrue(johnGroups.contains(group1));
            Assert.assertFalse(johnGroups.contains(group11));
            Assert.assertTrue(johnGroups.contains(group12));
            Assert.assertTrue(johnGroups.contains(groupTeam20162017));
            Assert.assertTrue(johnGroups.contains(groupTeamChild20182019));

            Assert.assertEquals(2, john.getGroupsStream("gr", 0, 10).count());
            Assert.assertEquals(1, john.getGroupsStream("gr", 1, 10).count());
            Assert.assertEquals(1, john.getGroupsStream("gr", 0, 1).count());
            Assert.assertEquals(1, john.getGroupsStream("12", 0, 10).count());
            Assert.assertEquals(1, john.getGroupsStream("2017", 0, 10).count());
            Assert.assertEquals(1, john.getGroupsStream("2018", 0, 10).count());

            // 4 - Check through userProvider
            List<UserModel> group1Members = session.users().getGroupMembersStream(appRealm, group1, 0, 10)
                    .collect(Collectors.toList());
            List<UserModel> group11Members = session.users().getGroupMembersStream(appRealm, group11, 0, 10)
                    .collect(Collectors.toList());
            Stream<UserModel> group12Members = session.users().getGroupMembersStream(appRealm, group12, 0, 10);
            Stream<UserModel> groupTeam20162017Members = session.users().getGroupMembersStream(appRealm, groupTeam20162017, 0, 10);
            Stream<UserModel> groupTeam20182019Members = session.users().getGroupMembersStream(appRealm, groupTeamChild20182019, 0, 10);

            Assert.assertEquals(1, group1Members.size());
            Assert.assertEquals("johnkeycloak", group1Members.get(0).getUsername());
            Assert.assertEquals(1, group11Members.size());
            Assert.assertEquals("marykeycloak", group11Members.get(0).getUsername());
            Assert.assertEquals(2, group12Members.count());
            Assert.assertEquals(2, groupTeam20162017Members.count());
            Assert.assertEquals(2, groupTeam20182019Members.count());

            // 4 - Delete some group mappings and check they are deleted

            john.leaveGroup(group1);
            john.leaveGroup(group12);
            john.leaveGroup(groupTeam20162017);
            john.leaveGroup(groupTeamChild20182019);

            mary.leaveGroup(group1);
            mary.leaveGroup(group11);
            mary.leaveGroup(group12);
            mary.leaveGroup(groupTeam20162017);
            mary.leaveGroup(groupTeamChild20182019);

            Assert.assertEquals(0, john.getGroupsStream().count());
            
            groupCount = john.getGroupsCount();
            Assert.assertEquals(0, groupCount);
        });
    }

    @Test
    public void test02_readOnlyGroupMappings() {
        test02_readOnlyGroupMappings(true);
    }



    protected void test02_readOnlyGroupMappings(boolean importEnabled) {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.READ_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");

            // Add some group mappings directly into LDAP
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ctx.getLdapProvider(), appRealm);

            LDAPObject maryLdap = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "marykeycloak");
            groupMapper.addGroupMappingInLDAP(appRealm, group1, maryLdap);
            groupMapper.addGroupMappingInLDAP(appRealm, group11, maryLdap);
        });

        if (importEnabled) {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();

                UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
                GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
                GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
                GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

                // Add some group mapping to model
                mary.joinGroup(group12);

                // Assert that mary has both LDAP and DB mapped groups
                Set<GroupModel> maryGroups = mary.getGroupsStream().collect(Collectors.toSet());
                Assert.assertEquals(5, maryGroups.size());
                Assert.assertTrue(maryGroups.contains(group1));
                Assert.assertTrue(maryGroups.contains(group11));
                Assert.assertTrue(maryGroups.contains(group12));

                long groupCount = mary.getGroupsCount();
                Assert.assertEquals(5, groupCount);

                Assert.assertEquals(5, mary.getGroupsStream("gr", 0, 10).count());
                Assert.assertEquals(4, mary.getGroupsStream("gr", 1, 10).count());
                Assert.assertEquals(1, mary.getGroupsStream("gr", 0, 1).count());
                Assert.assertEquals(2, mary.getGroupsStream("12", 0, 10).count());
            });
        } else {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();

                UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
                GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

                // Add some group mapping to model. This should fail with no-import mode for LDAP provider READ_ONLY mode for the group mapper
                // as it is not allowed to update group mappings in LDAP nor in the DB
                try {
                    mary.joinGroup(group12);
                    Assert.fail("Not expected to successfully add group12 in no-import mode and READ_ONLY mode of the group mapper");
                } catch (ModelException me) {
                    // Ignore
                }
            });

            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();

                UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
                GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
                GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
                GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

                // Assert that mary has both LDAP and DB mapped groups
                Set<GroupModel> maryGroups = mary.getGroupsStream().collect(Collectors.toSet());
                Assert.assertEquals(4, maryGroups.size());
                Assert.assertTrue(maryGroups.contains(group1));
                Assert.assertTrue(maryGroups.contains(group11));
                Assert.assertFalse(maryGroups.contains(group12));

                long groupCount = mary.getGroupsCount();
                Assert.assertEquals(4, groupCount);

                Assert.assertEquals(4, mary.getGroupsStream("gr", 0, 10).count());
                Assert.assertEquals(3, mary.getGroupsStream("gr", 1, 10).count());
                Assert.assertEquals(1, mary.getGroupsStream("gr", 0, 1).count());
                Assert.assertEquals(1, mary.getGroupsStream("12", 0, 10).count());
            });
        }

        // Assert that access through DB will have just DB mapped groups
        if (importEnabled) {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();

                GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
                GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
                GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

                UserModel maryDB = session.userLocalStorage().getUserByUsername(appRealm, "marykeycloak");

                Set<GroupModel> maryDBGroups = maryDB.getGroupsStream().collect(Collectors.toSet());
                Assert.assertFalse(maryDBGroups.contains(group1));
                Assert.assertFalse(maryDBGroups.contains(group11));
                Assert.assertTrue(maryDBGroups.contains(group12));

                Assert.assertEquals(3, maryDB.getGroupsStream("Gr", 0, 10).count());
                Assert.assertEquals(2, maryDB.getGroupsStream("Gr", 1, 10).count());
                Assert.assertEquals(1, maryDB.getGroupsStream("Gr", 0, 1).count());
                Assert.assertEquals(2, maryDB.getGroupsStream("12", 0, 10).count());

                long dbGroupCount = maryDB.getGroupsCount();
                Assert.assertEquals(3, dbGroupCount);

                // Test the group mapping available for group12
                List<UserModel> group12Members = session.users().getGroupMembersStream(appRealm, group12, 0, 10)
                        .collect(Collectors.toList());
                Assert.assertEquals(1, group12Members.size());
                Assert.assertEquals("marykeycloak", group12Members.get(0).getUsername());

                UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
                mary.leaveGroup(group12);
            });
        } else {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();

                GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

                // Test the group mapping NOT available for group12
                Stream<UserModel> group12Members = session.users().getGroupMembersStream(appRealm, group12, 0, 10);
                Assert.assertEquals(0, group12Members.count());
            });
        }


        // Check through userProvider
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ctx.getLdapProvider(), appRealm);
            LDAPObject maryLdap = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "marykeycloak");

            List<UserModel> group1Members = session.users().getGroupMembersStream(appRealm, group1, 0, 10)
                    .collect(Collectors.toList());
            List<UserModel> group11Members = session.users().getGroupMembersStream(appRealm, group11, 0, 10)
                    .collect(Collectors.toList());
            Assert.assertEquals(1, group1Members.size());
            Assert.assertEquals("marykeycloak", group1Members.get(0).getUsername());
            Assert.assertEquals(1, group11Members.size());
            Assert.assertEquals("marykeycloak", group11Members.get(0).getUsername());

            try {
                mary.leaveGroup(group1);
                Assert.fail("It wasn't expected to successfully delete LDAP group mappings in READ_ONLY mode");
            } catch (ModelException expected) {
            }

            // Delete group mappings directly in LDAP
            LDAPObject ldapGroup = groupMapper.loadLDAPGroupByName("group1");
            groupMapper.deleteGroupMappingInLDAP(maryLdap, ldapGroup);

            ldapGroup = groupMapper.loadLDAPGroupByName("group11");
            groupMapper.deleteGroupMappingInLDAP(maryLdap, ldapGroup);
        });
    }


    @Test
    public void test03_importGroupMappings() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.IMPORT.toString());
            appRealm.updateComponent(mapperModel);

            // Add some group mappings directly in LDAP
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

            LDAPObject robLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "robkeycloak");
            groupMapper.addGroupMappingInLDAP(appRealm, group11, robLdap);
            groupMapper.addGroupMappingInLDAP(appRealm, group12, robLdap);

            // Get user and check that he has requested groups from LDAP
            UserModel rob = session.users().getUserByUsername(appRealm, "robkeycloak");
            Set<GroupModel> robGroups = rob.getGroupsStream().collect(Collectors.toSet());

            Assert.assertFalse(robGroups.contains(group1));
            Assert.assertTrue(robGroups.contains(group11));
            Assert.assertTrue(robGroups.contains(group12));

            Assert.assertEquals(4, rob.getGroupsStream("Gr", 0, 10).count());
            Assert.assertEquals(3, rob.getGroupsStream("Gr", 1, 10).count());
            Assert.assertEquals(1, rob.getGroupsStream("Gr", 0, 1).count());
            Assert.assertEquals(2, rob.getGroupsStream("12", 0, 10).count());

            long dbGroupCount = rob.getGroupsCount();
            Assert.assertEquals(4, dbGroupCount);

            // Check getGroupMembers
            Stream<UserModel> group1Members = session.users().getGroupMembersStream(appRealm, group1, 0, 10);
            List<UserModel> group11Members = session.users().getGroupMembersStream(appRealm, group11, 0, 10)
                    .collect(Collectors.toList());
            List<UserModel> group12Members = session.users().getGroupMembersStream(appRealm, group12, 0, 10)
                    .collect(Collectors.toList());

            Assert.assertEquals(0, group1Members.count());
            Assert.assertEquals(1, group11Members.size());
            Assert.assertEquals("robkeycloak", group11Members.get(0).getUsername());
            Assert.assertEquals(1, group12Members.size());
            Assert.assertEquals("robkeycloak", group12Members.get(0).getUsername());

            // Delete some group mappings in LDAP and check that it doesn't have any effect and user still has groups
            LDAPObject ldapGroup = groupMapper.loadLDAPGroupByName("group11");
            groupMapper.deleteGroupMappingInLDAP(robLdap, ldapGroup);

            ldapGroup = groupMapper.loadLDAPGroupByName("group12");
            groupMapper.deleteGroupMappingInLDAP(robLdap, ldapGroup);

            robGroups = rob.getGroupsStream().collect(Collectors.toSet());
            Assert.assertTrue(robGroups.contains(group11));
            Assert.assertTrue(robGroups.contains(group12));

            // Check getGroupMembers
            group1Members = session.users().getGroupMembersStream(appRealm, group1, 0, 10);
            group11Members = session.users().getGroupMembersStream(appRealm, group11, 0, 10)
                    .collect(Collectors.toList());
            group12Members = session.users().getGroupMembersStream(appRealm, group12, 0, 10)
                    .collect(Collectors.toList());

            Assert.assertEquals(0, group1Members.count());
            Assert.assertEquals(1, group11Members.size());
            Assert.assertEquals("robkeycloak", group11Members.get(0).getUsername());
            Assert.assertEquals(1, group12Members.size());
            Assert.assertEquals("robkeycloak", group12Members.get(0).getUsername());

            // Delete group mappings through model and verifies that user doesn't have them anymore
            rob.leaveGroup(group11);
            rob.leaveGroup(group12);
            Assert.assertEquals(2, rob.getGroupsStream().count());
        });
    }


    // KEYCLOAK-2682
    @Test
    public void test04_groupReferencingNonExistentMember() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            // Ignoring this test on ActiveDirectory as it's not allowed to have LDAP group referencing nonexistent member. KEYCLOAK-2682 was related to OpenLDAP TODO: Better solution than programmatic...
            LDAPConfig config = ctx.getLdapProvider().getLdapIdentityStore().getConfig();
            if (config.isActiveDirectory()) {
                return;
            }

            String descriptionAttrName = getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            // 1 - Add some group to LDAP for testing
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);
            LDAPObject group2 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group2", descriptionAttrName, "group2 - description");

            // 2 - Add one existing user rob to LDAP group
            LDAPObject jamesLdap = ldapProvider.loadLDAPUserByUsername(appRealm, "jameskeycloak");
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group2, jamesLdap);

            // 3 - Add non-existing user to LDAP group
            LDAPDn nonExistentDn = LDAPDn.fromString(ldapProvider.getLdapIdentityStore().getConfig().getUsersDn());
            nonExistentDn.addFirst(jamesLdap.getRdnAttributeNames().get(0), "nonexistent");
            LDAPObject nonExistentLdapUser = new LDAPObject();
            nonExistentLdapUser.setDn(nonExistentDn);
            LDAPUtils.addMember(ldapProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group2, nonExistentLdapUser);

            // 4 - Check group members. Just existing user rob should be present
            groupMapper.syncDataFromFederationProviderToKeycloak(appRealm);
            GroupModel kcGroup2 = KeycloakModelUtils.findGroupByPath(appRealm, "/group2");
            List<UserModel> groupUsers = session.users().getGroupMembersStream(appRealm, kcGroup2, 0, 5)
                    .collect(Collectors.toList());
            Assert.assertEquals(1, groupUsers.size());
            UserModel rob = groupUsers.get(0);
            Assert.assertEquals("jameskeycloak", rob.getUsername());

        });
    }


    // KEYCLOAK-5848
    // Test GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE with custom 'Member-Of LDAP Attribute'. As a workaround, we are testing this with custom attribute "street"
    // just because it's available on all the LDAP servers
    @Test
    public void test05_getGroupsFromUserMemberOfStrategyTest() throws Exception {
        ComponentRepresentation groupMapperRep = findMapperRepByName("groupsMapper");

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Create street attribute mapper
            LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "streetMapper", "street", LDAPConstants.STREET);

            // Find DN of "group1"
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ctx.getLdapProvider(), appRealm);
            LDAPObject ldapGroup = groupMapper.loadLDAPGroupByName("group1");
            String ldapGroupDN = ldapGroup.getDn().toString();

            // Create new user in LDAP. Add him some "street" referencing existing LDAP Group
            LDAPObject carlos = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "carloskeycloak", "Carlos", "Doel", "carlos.doel@email.org", ldapGroupDN, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), carlos, "Password1");

            // Update group mapper
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel,
                    GroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE,
                    GroupMapperConfig.MEMBEROF_LDAP_ATTRIBUTE, LDAPConstants.STREET);
            appRealm.updateComponent(mapperModel);
        });

        ComponentRepresentation streetMapperRep = findMapperRepByName("streetMapper");

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Get user in Keycloak. Ensure that he is member of requested group
            UserModel carlos = session.users().getUserByUsername(appRealm, "carloskeycloak");
            Set<GroupModel> carlosGroups = carlos.getGroupsStream().collect(Collectors.toSet());

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group11 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group11");
            GroupModel group12 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group12");

            Assert.assertTrue(carlosGroups.contains(group1));
            Assert.assertFalse(carlosGroups.contains(group11));
            Assert.assertFalse(carlosGroups.contains(group12));

            Assert.assertEquals(1, carlosGroups.size());
        });

        // Revert mappers
        testRealm().components().component(streetMapperRep.getId()).remove();
        groupMapperRep.getConfig().putSingle(GroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE);
        testRealm().components().component(groupMapperRep.getId()).update(groupMapperRep);
    }


    // KEYCLOAK-5017
    @Test
    public void test06_addingUserToNewKeycloakGroup() throws Exception {
        // Add some groups to Keycloak
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel group3 = appRealm.createGroup("group3");
            GroupModel group31 = appRealm.createGroup("group31", group3);
            GroupModel group32 = appRealm.createGroup("group32", group3);

            GroupModel group4 = appRealm.createGroup("group4");

            GroupModel group1 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1");
            GroupModel group14 = appRealm.createGroup("group14", group1);

        });

        // Add user to some newly created KC groups
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");

            GroupModel group4 =  KeycloakModelUtils.findGroupByPath(appRealm, "/group4");
            john.joinGroup(group4);

            GroupModel group31 = KeycloakModelUtils.findGroupByPath(appRealm, "/group3/group31");
            GroupModel group32 = KeycloakModelUtils.findGroupByPath(appRealm, "/group3/group32");

            john.joinGroup(group31);
            john.joinGroup(group32);

            GroupModel group14 = KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group14");
            john.joinGroup(group14);
        });

        // Check user group memberships
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");

            GroupModel group14 =  KeycloakModelUtils.findGroupByPath(appRealm, "/group1/group14");
            GroupModel group3 =  KeycloakModelUtils.findGroupByPath(appRealm, "/group3");
            GroupModel group31 = KeycloakModelUtils.findGroupByPath(appRealm, "/group3/group31");
            GroupModel group32 = KeycloakModelUtils.findGroupByPath(appRealm, "/group3/group32");
            GroupModel group4 =  KeycloakModelUtils.findGroupByPath(appRealm, "/group4");

            Set<GroupModel> groups = john.getGroupsStream().collect(Collectors.toSet());
            Assert.assertTrue(groups.contains(group14));
            Assert.assertFalse(groups.contains(group3));
            Assert.assertTrue(groups.contains(group31));
            Assert.assertTrue(groups.contains(group32));
            Assert.assertTrue(groups.contains(group4));

            long groupsCount = john.getGroupsCount();
            Assert.assertEquals(4, groupsCount);

            Assert.assertEquals(2, john.getGroupsStream("3", 0, 10).count());
            Assert.assertEquals(1, john.getGroupsStream("3", 1, 10).count());
            Assert.assertEquals(1, john.getGroupsStream("3", 1, 1).count());
            Assert.assertEquals(0, john.getGroupsStream("3", 1, 0).count());
            Assert.assertEquals(0, john.getGroupsStream("Keycloak", 0, 10).count());
        });
    }


    @Test
    public void test07_newUserDefaultGroupsImportModeTest() throws Exception {

        // Check user group memberships
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.IMPORT.toString());
            appRealm.updateComponent(mapperModel);

            UserModel david = session.users().addUser(appRealm, "davidkeycloak");

            GroupModel defaultGroup11 =  KeycloakModelUtils.findGroupByPath(appRealm, "/defaultGroup1/defaultGroup11");
            Assert.assertNotNull(defaultGroup11);

            GroupModel defaultGroup12 =  KeycloakModelUtils.findGroupByPath(appRealm, "/defaultGroup1/defaultGroup12");
            Assert.assertNotNull(defaultGroup12);

            GroupModel group31 = KeycloakModelUtils.findGroupByPath(appRealm, "/group3/group31");
            Assert.assertNotNull(group31);
            GroupModel group32 = KeycloakModelUtils.findGroupByPath(appRealm, "/group3/group32");
            Assert.assertNotNull(group32);
            GroupModel group4 =  KeycloakModelUtils.findGroupByPath(appRealm, "/group4");
            Assert.assertNotNull(group4);

            Set<GroupModel> groups = david.getGroupsStream().collect(Collectors.toSet());
            Assert.assertTrue(groups.contains(defaultGroup11));
            Assert.assertTrue(groups.contains(defaultGroup12));
            Assert.assertFalse(groups.contains(group31));
            Assert.assertFalse(groups.contains(group32));
            Assert.assertFalse(groups.contains(group4));

        });
    }

    private static LDAPObject searchObjectInBase(LDAPStorageProvider ldapProvider, String dn, String... attrs) {
        LDAPQuery q = new LDAPQuery(ldapProvider)
                            .setSearchDn(dn)
                            .setSearchScope(SearchControls.OBJECT_SCOPE);
        if (attrs != null) {
            for (String attr: attrs) {
                q.addReturningLdapAttribute(attr);
            }
        }
        return q.getFirstResult();
    }

    @Test
    public void test08_ldapOnlyGroupMappingsRanged() {
        testingClient.server().run(session -> {
            int membersToTest = 61; // try to do 3 pages (30+30+1)
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            // Ignoring this test on ActiveDirectory and rhds as it's currently impossible to import more than 60 users without timeout
            LDAPConfig ldapConfig = ctx.getLdapProvider().getLdapIdentityStore().getConfig();
            if (ldapConfig.isActiveDirectory() || LDAPConstants.VENDOR_RHDS.equals(ldapConfig.getVendor())) {
                return;
            }

            // create big grups that use ranged search
            String descriptionAttrName = getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            LDAPObject bigGroup = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "biggroup", descriptionAttrName, "biggroup - description");
            // create the users to use range search and add them to the group
            for (int i = 0; i < membersToTest; i++) {
                String username = String.format("user%02d", i);
                LDAPObject user = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, username, username, username, username + "@email.org", null, "1234");
                LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", bigGroup, user);
            }

            // check if ranged intercetor is in place and working
            GroupMapperConfig config = new GroupMapperConfig(mapperModel);
            bigGroup = LDAPGroupMapperTest.searchObjectInBase(ctx.getLdapProvider(), bigGroup.getDn().toString(), config.getMembershipLdapAttribute());
            Assert.assertNotNull(bigGroup.getAttributes().get(config.getMembershipLdapAttribute()));
            Assert.assertFalse(bigGroup.isRangeComplete(config.getMembershipLdapAttribute()));
            Assert.assertTrue(membersToTest > bigGroup.getAttributeAsSet(config.getMembershipLdapAttribute()).size());
            Assert.assertEquals(bigGroup.getCurrentRange(config.getMembershipLdapAttribute()), bigGroup.getAttributeAsSet(config.getMembershipLdapAttribute()).size() - 1);

            // now check the population of ranged attributes is OK
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);
            groupMapper.syncDataFromFederationProviderToKeycloak(appRealm);

            GroupModel kcBigGroup = KeycloakModelUtils.findGroupByPath(appRealm, "/biggroup");
            // check all the users have the group assigned
            for (int i = 0; i < membersToTest; i++) {
                UserModel kcUser = session.users().getUserByUsername(appRealm, String.format("user%02d", i));
                Assert.assertTrue("User contains biggroup " + i, kcUser.getGroupsStream().collect(Collectors.toSet()).contains(kcBigGroup));
            }
            // check the group contains all the users as member
            List<UserModel> groupMembers = session.users().getGroupMembersStream(appRealm, kcBigGroup, 0, membersToTest)
                    .collect(Collectors.toList());
            Assert.assertEquals(membersToTest, groupMembers.size());
            Set<String> usernames = groupMembers.stream().map(u -> u.getUsername()).collect(Collectors.toSet());
            for (int i = 0; i < membersToTest; i++) {
                Assert.assertTrue("Group contains user " + i, usernames.contains(String.format("user%02d", i)));
            }
        });
    }

    @Test
    public void test09_emptyMemberOnDeletionWorks() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");

            // Ignoring this test on ActiveDirectory (same for rhds) as it's not allowed to have LDAP group referencing nonexistent member. KEYCLOAK-2682 was related to OpenLDAP TODO: Better solution than programmatic...
            LDAPConfig ldapConfig = ctx.getLdapProvider().getLdapIdentityStore().getConfig();
            if (ldapConfig.isActiveDirectory() || LDAPConstants.VENDOR_RHDS.equals(ldapConfig.getVendor())) {
                return;
            }

            // create a group with an existing user alone
            String descriptionAttrName = getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            LDAPObject deleteGroup = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "deletegroup", descriptionAttrName, "deletegroup - description");
            LDAPObject maryLdap = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "marykeycloak");
            LDAPUtils.addMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, "not-used", deleteGroup, maryLdap);
            LDAPObject empty = new LDAPObject();
            empty.setDn(LDAPDn.fromString(LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE));
            LDAPUtils.deleteMember(ctx.getLdapProvider(), MembershipType.DN, LDAPConstants.MEMBER, descriptionAttrName, deleteGroup, empty);
            deleteGroup = LDAPGroupMapperTest.searchObjectInBase(ctx.getLdapProvider(), deleteGroup.getDn().toString(), LDAPConstants.MEMBER);
            Assert.assertNotNull(deleteGroup);
            Assert.assertEquals(1, deleteGroup.getAttributeAsSet(LDAPConstants.MEMBER).size());
            Assert.assertEquals(maryLdap.getDn(), LDAPDn.fromString(deleteGroup.getAttributeAsString(LDAPConstants.MEMBER)));

            // import into keycloak
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            GroupLDAPStorageMapper groupMapper = LDAPTestUtils.getGroupMapper(mapperModel, ldapProvider, appRealm);
            groupMapper.syncDataFromFederationProviderToKeycloak(appRealm);

            // check everything is OK
            GroupModel kcDeleteGroup = KeycloakModelUtils.findGroupByPath(appRealm, "/deletegroup");
            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
            List<UserModel> groupMembers = session.users().getGroupMembersStream(appRealm, kcDeleteGroup, 0, 5)
                    .collect(Collectors.toList());
            Assert.assertEquals(1, groupMembers.size());
            Assert.assertEquals("marykeycloak", groupMembers.get(0).getUsername());
            Set<GroupModel> maryGroups = mary.getGroupsStream().collect(Collectors.toSet());
            Assert.assertEquals(1, maryGroups.size());
            Assert.assertEquals("deletegroup", maryGroups.iterator().next().getName());

            // delete the group from mary to force schema violation and assingment of the empty value
            mary.leaveGroup(kcDeleteGroup);

            // check now the group has the empty member instead of mary
            deleteGroup = LDAPGroupMapperTest.searchObjectInBase(ctx.getLdapProvider(), deleteGroup.getDn().toString(), LDAPConstants.MEMBER);
            Assert.assertNotNull(deleteGroup);
            Assert.assertEquals(1, deleteGroup.getAttributeAsSet(LDAPConstants.MEMBER).size());
            Assert.assertEquals(LDAPDn.fromString(LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE), LDAPDn.fromString(deleteGroup.getAttributeAsString(LDAPConstants.MEMBER)));
        });
    }
}
