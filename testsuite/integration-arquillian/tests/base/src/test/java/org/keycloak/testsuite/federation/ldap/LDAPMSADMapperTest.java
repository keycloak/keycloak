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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMSADMapperTest extends AbstractLDAPTest {

    // Run this test just on MSAD
    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule()
            .assumeTrue((LDAPTestConfiguration ldapConfig) -> {

                // TODO: This is skipped as it requires that MSAD server is set to not allow weak passwords (There needs to be pwdProperties=1 set on MSAD side).
                // TODO: Currently we can't rely on it. See KEYCLOAK-4276
                return false;
                // return LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(vendor);

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

            LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "password-app");

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());

            // Delete all LDAP users and add some new for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        });
    }



//    @Rule
//    public WebRule webRule = new WebRule(this);
//
//    @WebResource
//    protected OAuthClient oauth;
//
//    @WebResource
//    protected WebDriver driver;
//
//    @WebResource
//    protected AppPage appPage;
//
//    @WebResource
//    protected RegisterPage registerPage;
//
//    @WebResource
//    protected LoginPage loginPage;
//
//    @WebResource
//    protected AccountUpdateProfilePage profilePage;
//
//    @WebResource
//    protected AccountPasswordPage changePasswordPage;
//

    @Page
    protected LoginPasswordUpdatePage passwordUpdatePage;


    @Test
    public void test01RegisterUserWithWeakPasswordFirst() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Weak password. This will fail to update password to MSAD due to password policy.
        registerPage.register("firstName", "lastName", "email2@check.cz", "registerUserSuccess2", "password", "password");

        // Another weak password
        passwordUpdatePage.assertCurrent();
        passwordUpdatePage.changePassword("pass", "pass");
        Assert.assertEquals("Invalid password: new password doesn't match password policies.", passwordUpdatePage.getError());

        // Strong password. Successfully update password and being redirected to the app
        passwordUpdatePage.changePassword("Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername("registerUserSuccess2", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ctx.getLdapModel().getId());
            Assert.assertEquals("registerusersuccess2", user.getUsername());
            Assert.assertEquals("firstName", user.getFirstName());
            Assert.assertEquals("lastName", user.getLastName());
            Assert.assertTrue(user.isEnabled());
            Assert.assertEquals(0, user.getRequiredActions().size());
        });
    }
}
