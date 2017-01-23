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

import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * Test for the MSAD setup with usernameAttribute=sAMAccountName, rdnAttribute=cn and fullNameMapper mapped to cn
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMSADFullNameTest {

    // Run this test just on MSAD and just when sAMAccountName is mapped to username
    private static LDAPRule ldapRule = new LDAPRule((Map<String, String> ldapConfig) -> {

        String vendor = ldapConfig.get(LDAPConstants.VENDOR);
        if (!vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY)) {
            return true;
        }

        String usernameAttr = ldapConfig.get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
        return !usernameAttr.equalsIgnoreCase(LDAPConstants.SAM_ACCOUNT_NAME);

    });

    private static ComponentModel ldapModel = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            LDAPTestUtils.addLocalUser(manager.getSession(), appRealm, "marykeycloak", "mary@test.com", "password-app");

            MultivaluedHashMap<String,String> ldapConfig = LDAPTestUtils.getLdapRuleConfig(ldapRule);
            ldapConfig.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setLastSync(0);
            model.setChangedSyncPeriod(-1);
            model.setFullSyncPeriod(-1);
            model.setName("test-ldap");
            model.setPriority(1);
            model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
            model.getConfig().addAll(ldapConfig);

            ldapModel = appRealm.addComponentModel(model);
            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add some new for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            // Remove the mapper for "username-cn" and create new mapper for fullName
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "username-cn");
            Assert.assertNotNull(mapperModel);
            appRealm.removeComponent(mapperModel);

            mapperModel = KeycloakModelUtils.createComponentModel("fullNameWritable", ldapModel.getId(), FullNameLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN,
                    FullNameLDAPStorageMapper.READ_ONLY, "false",
                    FullNameLDAPStorageMapper.WRITE_ONLY, "true");
            appRealm.addComponentModel(mapperModel);

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        }
    });


    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

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

    @WebResource
    protected AccountUpdateProfilePage profilePage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;



//    @Test
//    public void test01Sleep() throws Exception {
//        Thread.sleep(1000000);
//    }

    @Test
    public void test01_addUserWithoutFullName() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel john = session.users().addUser(appRealm, "johnkeycloak");
            john.setEmail("johnkeycloak@email.cz");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(john.getFederationLink());
            assertDnStartsWith(session, john, "cn=johnkeycloak");

            session.users().removeUser(appRealm, john);
        } finally {
            keycloakRule.stopSession(session, true);
        }

    }


    @Test
    public void test02_registerUserWithFullName() {

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("Johny", "Anthony", "johnyanth@check.cz", "johnkeycloak", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, john, "johnkeycloak", "Johny", "Anthony", true, "cn=Johny Anthony");

            session.users().removeUser(appRealm, john);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Test
    public void test03_addUserWithFirstNameOnly() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel john = session.users().addUser(appRealm, "johnkeycloak");
            john.setEmail("johnkeycloak@email.cz");
            john.setFirstName("Johnyyy");
            john.setEnabled(true);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, john, "johnkeycloak", "Johnyyy", "", true, "cn=Johnyyy");

            session.users().removeUser(appRealm, john);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Test
    public void test04_addUserWithLastNameOnly() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel john = session.users().addUser(appRealm, "johnkeycloak");
            john.setEmail("johnkeycloak@email.cz");
            john.setLastName("Anthonyy");
            john.setEnabled(true);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, john, "johnkeycloak", "", "Anthonyy", true, "cn=Anthonyy");

            session.users().removeUser(appRealm, john);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Test
    public void test05_registerUserWithFullNameSpecialChars() {

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("Jož,o", "Baříč", "johnyanth@check.cz", "johnkeycloak", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, john, "johnkeycloak", "Jož,o", "Baříč", true, "cn=Jož\\,o Baříč");

            session.users().removeUser(appRealm, john);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Test
    public void test06_conflicts() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel john = session.users().addUser(appRealm, "existingkc");
            john.setFirstName("John");
            john.setLastName("Existing");
            john.setEnabled(true);

            UserModel john2 = session.users().addUser(appRealm, "existingkc1");
            john2.setEnabled(true);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("John", "Existing", "johnyanth@check.cz", "existingkc", "Password1", "Password1");
        Assert.assertEquals("Username already exists.", registerPage.getError());

        registerPage.register("John", "Existing", "johnyanth@check.cz", "existingkc2", "Password1", "Password1");
        appPage.logout();

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("John", "Existing", "johnyanth2@check.cz", "existingkc3", "Password1", "Password1");

        session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel existingKc = session.users().getUserByUsername("existingkc", appRealm);
            assertUser(session, existingKc, "existingkc", "John", "Existing", true, "cn=John Existing");

            UserModel existingKc1 = session.users().getUserByUsername("existingkc1", appRealm);
            assertUser(session, existingKc1, "existingkc1", "", "", true, "cn=existingkc1");

            UserModel existingKc2 = session.users().getUserByUsername("existingkc2", appRealm);
            assertUser(session, existingKc2, "existingkc2", "John", "Existing", true, "cn=John Existing0");

            UserModel existingKc3 = session.users().getUserByUsername("existingkc3", appRealm);
            assertUser(session, existingKc3, "existingkc3", "John", "Existing", true, "cn=John Existing1");

            session.users().removeUser(appRealm, existingKc);
            session.users().removeUser(appRealm, existingKc1);
            session.users().removeUser(appRealm, existingKc2);
            session.users().removeUser(appRealm, existingKc3);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    private void assertUser(KeycloakSession session, UserModel user, String expectedUsername, String expectedFirstName, String expectedLastName, boolean expectedEnabled, String expectedDn) {
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getFederationLink());
        Assert.assertEquals(user.getFederationLink(), ldapModel.getId());
        Assert.assertEquals(expectedUsername, user.getUsername());
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastName, user.getLastName());
        Assert.assertEquals(expectedEnabled, user.isEnabled());
        assertDnStartsWith(session, user, expectedDn);
    }


    private void assertDnStartsWith(KeycloakSession session, UserModel user, String expectedRDn) {
        String usersDn = LDAPTestUtils.getLdapProvider(session, ldapModel).getLdapIdentityStore().getConfig().getUsersDn();
        String userDN = user.getFirstAttribute(LDAPConstants.LDAP_ENTRY_DN);
        Assert.assertTrue(userDN.equalsIgnoreCase(expectedRDn + "," + usersDn));
    }

}
