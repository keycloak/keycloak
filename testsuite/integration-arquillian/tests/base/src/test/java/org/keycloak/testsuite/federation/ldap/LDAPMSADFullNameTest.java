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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

/**
 * Test for the MSAD setup with usernameAttribute=sAMAccountName, rdnAttribute=cn and fullNameMapper mapped to cn
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMSADFullNameTest extends AbstractLDAPTest {

    // Run this test just on MSAD and just when sAMAccountName is mapped to username
    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule()
            .assumeTrue((LDAPTestConfiguration ldapConfig) -> {

                String vendor = ldapConfig.getLDAPConfig().get(LDAPConstants.VENDOR);
                if (!vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY)) {
                    return false;
                }

                String usernameAttr = ldapConfig.getLDAPConfig().get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
                return usernameAttr.equalsIgnoreCase(LDAPConstants.SAM_ACCOUNT_NAME);

            });

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }


    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            UserStorageProviderModel ldapModel = ctx.getLdapModel();

            LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "password-app");

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
        });
    }



//    @Test
//    public void test01Sleep() throws Exception {
//        Thread.sleep(1000000);
//    }

    @Test
    public void test01_addUserWithoutFullName() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().addUser(appRealm, "johnkeycloak");
            john.setEmail("johnkeycloak@email.cz");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(john.getFederationLink());
            assertDnStartsWith(session, ctx, john, "cn=johnkeycloak");

            session.users().removeUser(appRealm, john);
        });
    }


    @Test
    public void test02_registerUserWithFullName() {

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("Johny", "Anthony", "johnyanth@check.cz", "johnkeycloak", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, ctx, john, "johnkeycloak", "Johny", "Anthony", true, "cn=Johny Anthony");

            session.users().removeUser(appRealm, john);
        });
    }


    @Test
    public void test03_addUserWithFirstNameOnly() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().addUser(appRealm, "johnkeycloak");
            john.setEmail("johnkeycloak@email.cz");
            john.setFirstName("Johnyyy");
            john.setEnabled(true);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, ctx, john, "johnkeycloak", "Johnyyy", "", true, "cn=Johnyyy");

            session.users().removeUser(appRealm, john);
        });
    }


    @Test
    public void test04_addUserWithLastNameOnly() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().addUser(appRealm, "johnkeycloak");
            john.setEmail("johnkeycloak@email.cz");
            john.setLastName("Anthonyy");
            john.setEnabled(true);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, ctx, john, "johnkeycloak", "", "Anthonyy", true, "cn=Anthonyy");

            session.users().removeUser(appRealm, john);
        });
    }


    @Test
    public void test05_registerUserWithFullNameSpecialChars() {

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("Jož,o", "Baříč", "johnyanth@check.cz", "johnkeycloak", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            assertUser(session, ctx, john, "johnkeycloak", "Jož,o", "Baříč", true, "cn=Jož\\,o Baříč");

            session.users().removeUser(appRealm, john);
        });
    }


    @Test
    public void test06_conflicts() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().addUser(appRealm, "existingkc");
            john.setFirstName("John");
            john.setLastName("Existing");
            john.setEnabled(true);

            UserModel john2 = session.users().addUser(appRealm, "existingkc1");
            john2.setEnabled(true);
        });

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

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel existingKc = session.users().getUserByUsername("existingkc", appRealm);
            assertUser(session, ctx, existingKc, "existingkc", "John", "Existing", true, "cn=John Existing");

            UserModel existingKc1 = session.users().getUserByUsername("existingkc1", appRealm);
            assertUser(session, ctx, existingKc1, "existingkc1", "", "", true, "cn=existingkc1");

            UserModel existingKc2 = session.users().getUserByUsername("existingkc2", appRealm);
            assertUser(session, ctx, existingKc2, "existingkc2", "John", "Existing", true, "cn=John Existing0");

            UserModel existingKc3 = session.users().getUserByUsername("existingkc3", appRealm);
            assertUser(session, ctx, existingKc3, "existingkc3", "John", "Existing", true, "cn=John Existing1");

            session.users().removeUser(appRealm, existingKc);
            session.users().removeUser(appRealm, existingKc1);
            session.users().removeUser(appRealm, existingKc2);
            session.users().removeUser(appRealm, existingKc3);
        });
    }


    private static void assertUser(KeycloakSession session, LDAPTestContext ctx, UserModel user, String expectedUsername, String expectedFirstName, String expectedLastName, boolean expectedEnabled, String expectedDn) {
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getFederationLink());
        Assert.assertEquals(user.getFederationLink(), ctx.getLdapModel().getId());
        Assert.assertEquals(expectedUsername, user.getUsername());
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastName, user.getLastName());
        Assert.assertEquals(expectedEnabled, user.isEnabled());
        assertDnStartsWith(session, ctx, user, expectedDn);
    }


    private static void assertDnStartsWith(KeycloakSession session, LDAPTestContext ctx, UserModel user, String expectedRDn) {
        String usersDn = ctx.getLdapProvider().getLdapIdentityStore().getConfig().getUsersDn();
        String userDN = user.getFirstAttribute(LDAPConstants.LDAP_ENTRY_DN);
        Assert.assertTrue(userDN.equalsIgnoreCase(expectedRDn + "," + usersDn));
    }

}
