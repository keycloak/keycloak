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
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

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
}
