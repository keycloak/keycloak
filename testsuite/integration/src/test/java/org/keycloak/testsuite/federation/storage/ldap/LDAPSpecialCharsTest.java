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

package org.keycloak.testsuite.federation.storage.ldap;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.keycloak.models.AdminRoles.ADMIN;
import static org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPSpecialCharsTest {

    private static LDAPRule ldapRule = new LDAPRule();

    static ComponentModel ldapModel = null;
    static String descriptionAttrName = null;


    private static KeycloakRule keycloakRule = new KeycloakRule(new LDAPGroupMapperTest.GroupTestKeycloakSetup(ldapRule) {

        @Override
        protected void postSetup() {
            LDAPSpecialCharsTest.ldapModel = this.ldapModel;
            LDAPSpecialCharsTest.descriptionAttrName = this.descriptionAttrName;
        }

    });


    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);


    protected Keycloak adminClient;

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected RegisterPage registerPage;

    @WebResource
    protected LoginPage loginPage;


    @Before
    public void before() {
        adminClient = Keycloak.getInstance(AUTH_SERVER_ROOT, MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    @After
    public void after() {
        adminClient.close();
    }


    @Test
    public void test01_userSearch() {
        List<UserRepresentation> users = adminClient.realm("test").users().search("j*", 0, 10, false);
        Assert.assertEquals(3, users.size());

        List<String> usernames = users.stream().map((UserRepresentation user) -> {

            return user.getUsername();

        }).collect(Collectors.toList());
        Collections.sort(usernames);

        Assert.assertEquals("jamees,key*cložak)ppp", usernames.get(0));
        Assert.assertEquals("jameskeycloak", usernames.get(1));
        Assert.assertEquals("johnkeycloak", usernames.get(2));
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
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm,ldapModel, "groupsMapper");
            LDAPTestUtils.updateGroupMapperConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            UserModel specialUser = session.users().getUserByUsername("jamees,key*cložak)ppp", appRealm);
            Assert.assertNotNull(specialUser);

            // 1 - Grant some groups in LDAP

            // This group should already exists as it was imported from LDAP
            GroupModel specialGroup = KeycloakModelUtils.findGroupByPath(appRealm, "/group-spec,ia*l_characžter)s");
            Assert.assertNotNull(specialGroup);

            specialUser.joinGroup(specialGroup);

            // 3 - Check that group mappings are in LDAP and hence available through federation

            Set<GroupModel> userGroups = specialUser.getGroups();
            Assert.assertEquals(1, userGroups.size());
            Assert.assertTrue(userGroups.contains(specialGroup));

            // 4 - Check through userProvider
            List<UserModel> groupMembers = session.users().getGroupMembers(appRealm, specialGroup, 0, 10);

            Assert.assertEquals(1, groupMembers.size());
            Assert.assertEquals("jamees,key*cložak)ppp", groupMembers.get(0).getUsername());

            // 4 - Delete some group mappings and check they are deleted

            specialUser.leaveGroup(specialGroup);

            userGroups = specialUser.getGroups();
            Assert.assertEquals(0, userGroups.size());

        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

}
