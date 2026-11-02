/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.naming.directory.SearchControls;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.translation.GrantTarget;
import org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig;
import org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipLDAPStorageMapperFactory;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperCustomMemberOfTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
    }

    // Test GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE with custom 'Member-Of LDAP Attribute'. As a workaround, we are testing this with custom attribute "street"
    // just because it's available on all the LDAP servers
    @Test
    public void getGroupsFromUserMemberOfStrategyWithFilterTest() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Create group mapper
            String descriptionAttrName = LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());
            LDAPTestUtils.addOrUpdateGroupMapper(appRealm, ctx.getLdapModel(), LDAPGroupMapperMode.LDAP_ONLY, descriptionAttrName, 
                    GroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE,
                    GroupMapperConfig.MEMBEROF_LDAP_ATTRIBUTE, LDAPConstants.STREET,
                    GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "false",
                    GroupMapperConfig.GROUPS_LDAP_FILTER, "(cn=ldap-group2)");

            LDAPObject group1 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "ldap-group1", descriptionAttrName, "group1 - description");
            LDAPObject group2 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "ldap-group2", descriptionAttrName, "group2 - description");
            LDAPObject group3 = LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "ldap-group3", descriptionAttrName, "group3 - description");

            // Create new user in LDAP. Add him some "street" referencing existing LDAP Group
            LDAPObject carlos = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "carloskeycloak", "Carlos", "Doel", "carlos.doel@email.org", "street", "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), carlos, "Password1");

            // add "memberOf" attributes (simulated by "street" attr) to Carlos
            LDAPUtils.addMember(LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel()), MembershipType.DN, LDAPConstants.STREET, "not-used", carlos, group1);
            LDAPUtils.addMember(LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel()), MembershipType.DN, LDAPConstants.STREET, "not-used", carlos, group2);
            LDAPUtils.addMember(LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel()), MembershipType.DN, LDAPConstants.STREET, "not-used", carlos, group3);

            // Sync LDAP groups to Keycloak DB - it should bring only group2 due to the defined filter 
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            SynchronizationResult syncDataResult = new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(appRealm);
            assertThat(syncDataResult.getAdded(), equalTo(1));
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Get user in Keycloak. It should not load any new groups where Carlos is memberOf due to defined filter
            UserModel carlos = session.users().getUserByUsername(appRealm, "carloskeycloak");
            List<String> carlosGroups = carlos.getGroupsStream().map(GroupModel::getName).collect(Collectors.toList());

            assertThat(carlosGroups, hasSize(1));
            assertThat(carlosGroups, hasItem("ldap-group2"));
        });
    }

    @Test
    public void translatedMembershipGrantsRealmRolesClientRolesAndGroups() {
        try {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel realm = ctx.getRealm();
                LDAPObject mary = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), realm, "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "1234");
                List.of("myapp-Operator-users", "myapp-Special-users", "Unrelated").forEach(group -> ctx.getLdapProvider().getLdapIdentityStore().addMemberToGroup(mary.getDn().getLdapName(), LDAPConstants.STREET, "cn=" + group + ",ou=Groups,dc=keycloak,dc=org"));
                List.of("Operator", "Auditor", "Special").forEach(realm::addRole);
                realm.getClientByClientId("admin-cli").addRole("Operator");
                realm.createGroup("Operator", realm.createGroup("apps"));
                setGroupsMapperMode(realm, ctx, LDAPGroupMapperMode.READ_ONLY);
                addTranslatedMembershipMapper(realm, ctx.getLdapModel(), TranslatedMembershipConfig.TRANSLATION_TABLE, "[{\"key\":\"myapp-Special-users\",\"value\":\"Auditor\"}]");
                addTranslatedMembershipMapper(realm, ctx.getLdapModel(), TranslatedMembershipConfig.TARGET, GrantTarget.CLIENT_ROLE, TranslatedMembershipConfig.CLIENT_ID, "admin-cli");
                addTranslatedMembershipMapper(realm, ctx.getLdapModel(), TranslatedMembershipConfig.TARGET, GrantTarget.GROUP, TranslatedMembershipConfig.TRANSLATION_TEMPLATE, "/apps/$1");
            });
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                UserModel mary = session.users().getUserByUsername(ctx.getRealm(), "marykeycloak");
                assertThat(mary.getRealmRoleMappingsStream().map(RoleModel::getName).filter(List.of("Operator", "Auditor", "Special")::contains).toList(), containsInAnyOrder("Operator", "Auditor"));
                assertThat(List.of(mary.hasRole(ctx.getRealm().getClientByClientId("admin-cli").getRole("Operator")), mary.isMemberOf(KeycloakModelUtils.findGroupByPath(session, ctx.getRealm(), "/apps/Operator"))), equalTo(List.of(true, true)));
                assertThrows(ModelException.class, () -> mary.deleteRoleMapping(ctx.getRealm().getRole("Operator")));
                ctx.getLdapProvider().getLdapIdentityStore().removeMemberFromGroup(ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "marykeycloak").getDn().getLdapName(), LDAPConstants.STREET, "cn=myapp-Operator-users,ou=Groups,dc=keycloak,dc=org");
                UserStorageUtil.userCache(session).evict(ctx.getRealm(), mary);
            });
            testingClient.server().run(session -> {
                RealmModel realm = LDAPTestContext.init(session).getRealm();
                UserModel mary = session.users().getUserByUsername(realm, "marykeycloak");
                assertThat(mary.getRealmRoleMappingsStream().map(RoleModel::getName).filter(List.of("Operator", "Auditor", "Special")::contains).toList(), containsInAnyOrder("Auditor"));
                assertThat(List.of(mary.hasRole(realm.getClientByClientId("admin-cli").getRole("Operator")), mary.isMemberOf(KeycloakModelUtils.findGroupByPath(session, realm, "/apps/Operator"))), equalTo(List.of(false, false)));
            });
        } finally {
            removeTranslatedMembership("marykeycloak", List.of("Operator", "Auditor", "Special"));
        }
    }

    @Test
    public void translatedMembershipReadsEveryRangeOfMembershipAttribute() {
        Assume.assumeFalse("Skip for Active Directory and RHDS", List.of(LDAPConstants.VENDOR_ACTIVE_DIRECTORY, LDAPConstants.VENDOR_RHDS).contains(getLDAPRule().getConfig().get(LDAPConstants.VENDOR)));
        List<String> rangedRoles = IntStream.range(0, 61).mapToObj(i -> String.format("ranged-%02d", i)).toList();
        try {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel realm = ctx.getRealm();
                LDAPObject rob = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), realm, "robkeycloak", "Rob", "Range", "rob@email.org", null, "1234");
                ctx.getLdapProvider().getLdapIdentityStore().addMemberToGroup(rob.getDn().getLdapName(), LDAPConstants.OBJECT_CLASS, "extensibleObject");
                rangedRoles.forEach(role -> ctx.getLdapProvider().getLdapIdentityStore().addMemberToGroup(rob.getDn().getLdapName(), LDAPConstants.MEMBER, "cn=" + role + ",ou=Groups,dc=keycloak,dc=org"));
                rangedRoles.forEach(realm::addRole);
                addTranslatedMembershipMapper(realm, ctx.getLdapModel(), TranslatedMembershipConfig.MEMBERSHIP_ATTRIBUTE, LDAPConstants.MEMBER, TranslatedMembershipConfig.TRANSLATION_PATTERN, "ranged-\\d+", TranslatedMembershipConfig.TRANSLATION_TEMPLATE, "$0");
                assertThat(new LDAPQuery(ctx.getLdapProvider()).setSearchDn(rob.getDn().getLdapName()).setSearchScope(SearchControls.OBJECT_SCOPE).addReturningLdapAttribute(LDAPConstants.MEMBER).getFirstResult().isRangeComplete(LDAPConstants.MEMBER), is(false));
            });
            testingClient.server().run(session -> assertThat(session.users().getUserByUsername(LDAPTestContext.init(session).getRealm(), "robkeycloak").getRealmRoleMappingsStream().map(RoleModel::getName).toList(), hasItems(rangedRoles.toArray(String[]::new))));
        } finally {
            removeTranslatedMembership("robkeycloak", rangedRoles);
        }
    }

    private static void addTranslatedMembershipMapper(RealmModel realm, ComponentModel ldapModel, String... config) {
        ComponentModel mapper = KeycloakModelUtils.createComponentModel("translated-" + KeycloakModelUtils.generateId(), ldapModel.getId(),
                TranslatedMembershipLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(), TranslatedMembershipConfig.MEMBERSHIP_ATTRIBUTE, LDAPConstants.STREET, TranslatedMembershipConfig.TRANSLATION_PATTERN, "myapp-(.+)-users", TranslatedMembershipConfig.TRANSLATION_TEMPLATE, "$1");
        LDAPTestUtils.updateConfigOptions(mapper, config);
        realm.addComponentModel(mapper);
    }

    private static void setGroupsMapperMode(RealmModel realm, LDAPTestContext ctx, LDAPGroupMapperMode mode) {
        if (LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "groupsMapper") != null) LDAPTestUtils.addOrUpdateGroupMapper(realm, ctx.getLdapModel(), mode, LDAPTestUtils.getGroupDescriptionLDAPAttrName(ctx.getLdapProvider()));
    }

    private void removeTranslatedMembership(String username, List<String> roles) {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            realm.getComponentsStream(ctx.getLdapModel().getId(), LDAPStorageMapper.class.getName()).filter(mapper -> mapper.getProviderId().equals(TranslatedMembershipLDAPStorageMapperFactory.PROVIDER_ID)).toList().forEach(realm::removeComponent);
            Optional.ofNullable(session.users().getUserByUsername(realm, username)).ifPresent(user -> session.users().removeUser(realm, user));
            LDAPTestUtils.removeLDAPUserByUsername(ctx.getLdapProvider(), realm, ctx.getLdapProvider().getLdapIdentityStore().getConfig(), username);
            roles.forEach(role -> Optional.ofNullable(realm.getRole(role)).ifPresent(realm::removeRole));
            Optional.ofNullable(realm.getClientByClientId("admin-cli").getRole("Operator")).ifPresent(role -> role.getContainer().removeRole(role));
            Optional.ofNullable(KeycloakModelUtils.findGroupByPath(session, realm, "/apps")).ifPresent(realm::removeGroup);
            setGroupsMapperMode(realm, ctx, LDAPGroupMapperMode.LDAP_ONLY);
        });
    }
}
