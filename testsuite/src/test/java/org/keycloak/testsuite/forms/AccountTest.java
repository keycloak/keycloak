/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.forms;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountTotpPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;
import org.picketlink.idm.credential.util.TimeBasedOTP;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

    @WebResource
    protected AccountUpdateProfilePage profilePage;

    @WebResource
    protected AccountTotpPage totpPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    @After
    public void after() {
        keycloakRule.configure(new KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
                UserModel user = appRealm.getUser("test-user@localhost");

                UserCredentialModel cred = new UserCredentialModel();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue("password");

                appRealm.updateCredential(user, cred);
            }
        });
    }

    @Test
    public void changePassword() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        changePasswordPage.open();
        changePasswordPage.changePassword("password", "new-password", "new-password");

        oauth.openLogout();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals("Invalid username or password", loginPage.getError());

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void changeProfile() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        profilePage.open();

        Assert.assertEquals("", profilePage.getFirstName());
        Assert.assertEquals("", profilePage.getLastName());
        Assert.assertEquals("test-user@localhost", profilePage.getEmail());

        profilePage.updateProfile("New first", "New last", "new@email.com");

        Assert.assertEquals("New first", profilePage.getFirstName());
        Assert.assertEquals("New last", profilePage.getLastName());
        Assert.assertEquals("new@email.com", profilePage.getEmail());
    }

    @Test
    public void setupTotp() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        totpPage.open();

        Assert.assertTrue(totpPage.isCurrent());

        Assert.assertFalse(driver.getPageSource().contains("Remove Google"));

        totpPage.configure(totp.generate(totpPage.getTotpSecret()));

        Assert.assertTrue(driver.getPageSource().contains("Remove Google"));
    }

}
