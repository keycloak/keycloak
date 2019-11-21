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
import org.keycloak.OAuth2Constants;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.List;
import java.util.Set;

import static org.keycloak.testsuite.util.LDAPTestUtils.getGroupDescriptionLDAPAttrName;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPSpecialCharsTest extends AbstractLDAPTest {


    // Skip this test for MSAD with sAMAccountName as it is not allowed to use specialCharacters in sAMAccountName attribute
    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule()
            .assumeTrue((LDAPTestConfiguration ldapConfig) -> {

                String vendor = ldapConfig.getLDAPConfig().get(LDAPConstants.VENDOR);
                String usernameAttr = ldapConfig.getLDAPConfig().get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);

                boolean skip = (vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY) && usernameAttr.equalsIgnoreCase(LDAPConstants.SAM_ACCOUNT_NAME));
                return !skip;

            });

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }


    @Override
    protected void afterImportTestRealm() {
        testingClient.testing().ldap(TEST_REALM_NAME).prepareGroupsLDAPTest();

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            String descriptionAttrName = getGroupDescriptionLDAPAttrName(ctx.getLdapProvider());

            LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group-spec,ia*l_characžter)s", descriptionAttrName, "group-special-characters");
            LDAPTestUtils.createLDAPGroup(session, appRealm, ctx.getLdapModel(), "group/with/three/slashes", descriptionAttrName, "group-with-three-slashes");

            // Resync LDAP groups to Keycloak DB
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            new GroupLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(appRealm);

            LDAPObject james2 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "jamees,key*cložak)ppp", "James2", "Brown2", "james2@email.org", null, "89102");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), james2, "Password1");
        });
    }


    @Test
    public void test01_userSearch() {
        List<UserRepresentation> users = adminClient.realm("test").users().search("j*", 0, 10);

        assertContainsUsername(users, "jamees,key*cložak)ppp");
        assertContainsUsername(users, "jameskeycloak");
        assertContainsUsername(users, "johnkeycloak");
    }


    private void assertContainsUsername(List<UserRepresentation> users, String username) {
        boolean found = users.stream().filter((UserRepresentation user) -> {

            return username.equals(user.getUsername());

        }).findFirst().isPresent();

        if (!found) {
            Assert.fail("Username " + username + " not found in the list");
        }
    }


    @Test
    public void test02_loginWithSpecialCharacter() {
        // Fail login with wildcard
        loginPage.open();
        loginPage.login("john*", "Password1");
        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        // Fail login with wildcard
        loginPage.login("j*", "Password1");
        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        // Success login as username exactly match
        loginPage.login("jamees,key*cložak)ppp", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
    }


    @Test
    public void test03_specialCharUserJoiningSpecialCharGroup() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            UserModel specialUser = session.users().getUserByUsername("jamees,key*cložak)ppp", appRealm);
            Assert.assertNotNull(specialUser);

            // 1 - Grant some groups in LDAP

            // This group should already exists as it was imported from LDAP
            GroupModel specialGroup = KeycloakModelUtils.findGroupByPath(appRealm, "/group-spec,ia*l_characžter)s");
            Assert.assertNotNull(specialGroup);

            specialUser.joinGroup(specialGroup);

            GroupModel groupWithSlashes = KeycloakModelUtils.findGroupByPath(appRealm, "/group/with/three/slashes");
            Assert.assertNotNull(groupWithSlashes);

            specialUser.joinGroup(groupWithSlashes);

            // 2 - Check that group mappings are in LDAP and hence available through federation

            Set<GroupModel> userGroups = specialUser.getGroups();
            Assert.assertEquals(2, userGroups.size());
            Assert.assertTrue(userGroups.contains(specialGroup));

            // 3 - Check through userProvider
            List<UserModel> groupMembers = session.users().getGroupMembers(appRealm, specialGroup, 0, 10);

            Assert.assertEquals(1, groupMembers.size());
            Assert.assertEquals("jamees,key*cložak)ppp", groupMembers.get(0).getUsername());

            groupMembers = session.users().getGroupMembers(appRealm, groupWithSlashes, 0, 10);

            Assert.assertEquals(1, groupMembers.size());
            Assert.assertEquals("jamees,key*cložak)ppp", groupMembers.get(0).getUsername());

            // 4 - Delete some group mappings and check they are deleted

            specialUser.leaveGroup(specialGroup);
            specialUser.leaveGroup(groupWithSlashes);

            userGroups = specialUser.getGroups();
            Assert.assertEquals(0, userGroups.size());

        });
    }

}
