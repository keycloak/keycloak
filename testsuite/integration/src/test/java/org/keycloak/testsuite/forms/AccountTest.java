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

import org.apache.http.HttpResponse;
import org.junit.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserModel user = appRealm.getUser("test-user@localhost");

            ApplicationModel accountApp = appRealm.getApplicationNameMap().get(org.keycloak.models.Constants.ACCOUNT_APPLICATION);

            UserModel user2 = appRealm.addUser("test-user-no-access@localhost");
            user2.setEnabled(true);
            for (String r : accountApp.getDefaultRoles()) {
                appRealm.deleteRoleMapping(user2, accountApp.getRole(r));
            }
            UserCredentialModel creds = new UserCredentialModel();
            creds.setType(CredentialRepresentation.PASSWORD);
            creds.setValue("password");
            appRealm.updateCredential(user2, creds);
        }
    });

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

    @WebResource
    protected ErrorPage errorPage;

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

    //@Test
    public void returnToAppFromHeader() {
        appPage.open();
        appPage.openAccount();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(profilePage.isCurrent());
        profilePage.backToApplication();

        Assert.assertTrue(appPage.isCurrent());
    }

    //@Test
    public void returnToAppFromQueryParam() {
        driver.navigate().to(AccountUpdateProfilePage.PATH + "?referrer=test-app");
        loginPage.login("test-user@localhost", "password");
        Assert.assertTrue(profilePage.isCurrent());
        profilePage.backToApplication();

        Assert.assertTrue(appPage.isCurrent());
    }

    @Test
    public void changePassword() {
        changePasswordPage.open();
        loginPage.login("test-user@localhost", "password");

        changePasswordPage.changePassword("", "new-password", "new-password");

        Assert.assertEquals("Please specify password.", profilePage.getError());

        changePasswordPage.changePassword("password", "new-password", "new-password2");

        Assert.assertEquals("Password confirmation doesn't match", profilePage.getError());

        changePasswordPage.changePassword("password", "new-password", "new-password");

        Assert.assertEquals("Your password has been updated", profilePage.getSuccess());

        changePasswordPage.logout();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void changePasswordWithPasswordPolicy() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setPasswordPolicy(new PasswordPolicy("length"));
            }
        });

        try {
            changePasswordPage.open();
            loginPage.login("test-user@localhost", "password");

            changePasswordPage.changePassword("", "new", "new");

            Assert.assertEquals("Please specify password.", profilePage.getError());

            changePasswordPage.changePassword("password", "new-password", "new-password");

            Assert.assertEquals("Your password has been updated", profilePage.getSuccess());
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setPasswordPolicy(new PasswordPolicy(null));
                }
            });
        }
    }

    @Test
    public void changeProfile() {
        profilePage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals("", profilePage.getFirstName());
        Assert.assertEquals("", profilePage.getLastName());
        Assert.assertEquals("test-user@localhost", profilePage.getEmail());

        // All fields are required, so there should be an error when something is missing.
        profilePage.updateProfile("", "New last", "new@email.com");

        Assert.assertEquals("Please specify first name", profilePage.getError());
        Assert.assertEquals("", profilePage.getFirstName());
        Assert.assertEquals("", profilePage.getLastName());
        Assert.assertEquals("test-user@localhost", profilePage.getEmail());

        profilePage.updateProfile("New first", "", "new@email.com");

        Assert.assertEquals("Please specify last name", profilePage.getError());
        Assert.assertEquals("", profilePage.getFirstName());
        Assert.assertEquals("", profilePage.getLastName());
        Assert.assertEquals("test-user@localhost", profilePage.getEmail());

        profilePage.updateProfile("New first", "New last", "");

        Assert.assertEquals("Please specify email", profilePage.getError());
        Assert.assertEquals("", profilePage.getFirstName());
        Assert.assertEquals("", profilePage.getLastName());
        Assert.assertEquals("test-user@localhost", profilePage.getEmail());

        profilePage.updateProfile("New first", "New last", "new@email.com");

        Assert.assertEquals("Your account has been updated", profilePage.getSuccess());
        Assert.assertEquals("New first", profilePage.getFirstName());
        Assert.assertEquals("New last", profilePage.getLastName());
        Assert.assertEquals("new@email.com", profilePage.getEmail());
    }

    @Test
    public void setupTotp() {
        totpPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(totpPage.isCurrent());

        Assert.assertFalse(driver.getPageSource().contains("Remove Google"));

        // Error with false code
        totpPage.configure(totp.generate(totpPage.getTotpSecret() + "123"));

        Assert.assertEquals("Invalid authenticator code", profilePage.getError());

        totpPage.configure(totp.generate(totpPage.getTotpSecret()));

        Assert.assertEquals("Google authenticator configured.", profilePage.getSuccess());

        Assert.assertTrue(driver.getPageSource().contains("pficon-delete"));
    }

    @Test
    public void changeProfileNoAccess() throws Exception {
        profilePage.open();
        loginPage.login("test-user-no-access@localhost", "password");

        Assert.assertTrue(errorPage.isCurrent());
        Assert.assertEquals("No access", errorPage.getError());
    }

}
