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
package org.keycloak.testsuite.account;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.AccountService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountLogPage;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountSessionsPage;
import org.keycloak.testsuite.pages.AccountTotpPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);

            ApplicationModel accountApp = appRealm.getApplicationNameMap().get(org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_APP);

            UserModel user2 = manager.getSession().users().addUser(appRealm, "test-user-no-access@localhost");
            user2.setEnabled(true);
            for (String r : accountApp.getDefaultRoles()) {
                user2.deleteRoleMapping(accountApp.getRole(r));
            }
            UserCredentialModel creds = new UserCredentialModel();
            creds.setType(CredentialRepresentation.PASSWORD);
            creds.setValue("password");
            user2.updateCredential(creds);
        }
    });

    private static final UriBuilder BASE = UriBuilder.fromUri("http://localhost:8081/auth");
    private static final String ACCOUNT_URL = RealmsResource.accountUrl(BASE.clone()).build("test").toString();
    public static String ACCOUNT_REDIRECT = AccountService.loginRedirectUrl(BASE.clone()).build("test").toString();

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

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
    protected RegisterPage registerPage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

    @WebResource
    protected AccountUpdateProfilePage profilePage;

    @WebResource
    protected AccountTotpPage totpPage;

    @WebResource
    protected AccountLogPage logPage;

    @WebResource
    protected AccountSessionsPage sessionsPage;

    @WebResource
    protected ErrorPage errorPage;

    private TimeBasedOTP totp = new TimeBasedOTP();
    private String userId;

    @Before
    public void before() {
        oauth.state("mystate"); // keycloak enforces that a state param has been sent by client
        userId = keycloakRule.getUser("test", "test-user@localhost").getId();
    }

    @After
    public void after() {
        keycloakRule.update(new KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
                UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);

                UserCredentialModel cred = new UserCredentialModel();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue("password");

                user.updateCredential(cred);
            }
        });
    }

    @Test @Ignore
    public void runit() throws Exception {
        Thread.sleep(10000000);
    }

    @Test
    public void returnToAppFromQueryParam() {
        driver.navigate().to(AccountUpdateProfilePage.PATH + "?referrer=test-app");
        loginPage.login("test-user@localhost", "password");
        Assert.assertTrue(profilePage.isCurrent());
        profilePage.backToApplication();

        Assert.assertTrue(appPage.isCurrent());

        driver.navigate().to(AccountUpdateProfilePage.PATH + "?referrer=test-app&referrer_uri=http://localhost:8081/app?test");
        Assert.assertTrue(profilePage.isCurrent());
        profilePage.backToApplication();

        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals(appPage.baseUrl + "?test", driver.getCurrentUrl());

        driver.navigate().to(AccountUpdateProfilePage.PATH + "?referrer=test-app");
        Assert.assertTrue(profilePage.isCurrent());

        driver.findElement(By.linkText("Authenticator")).click();
        Assert.assertTrue(totpPage.isCurrent());

        driver.findElement(By.linkText("Account")).click();
        Assert.assertTrue(profilePage.isCurrent());

        profilePage.backToApplication();

        Assert.assertTrue(appPage.isCurrent());

        events.clear();
    }

    @Test
    public void changePassword() {
        changePasswordPage.open();
        loginPage.login("test-user@localhost", "password");

        String sessionId = events.expectLogin().client("account").detail(Details.REDIRECT_URI, ACCOUNT_REDIRECT + "?path=password").assertEvent().getSessionId();

        changePasswordPage.changePassword("", "new-password", "new-password");

        Assert.assertEquals("Please specify password.", profilePage.getError());

        changePasswordPage.changePassword("password", "new-password", "new-password2");

        Assert.assertEquals("Password confirmation doesn't match", profilePage.getError());

        changePasswordPage.changePassword("password", "new-password", "new-password");

        Assert.assertEquals("Your password has been updated", profilePage.getSuccess());

        events.expectAccount(EventType.UPDATE_PASSWORD).assertEvent();

        changePasswordPage.logout();

        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, changePasswordPage.getPath()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().session((String) null).error("invalid_user_credentials").assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();
    }

    @Test
    public void changePasswordWithPasswordPolicy() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setPasswordPolicy(new PasswordPolicy("length"));
            }
        });

        try {
            changePasswordPage.open();
            loginPage.login("test-user@localhost", "password");


            events.expectLogin().client("account").detail(Details.REDIRECT_URI, ACCOUNT_REDIRECT + "?path=password").assertEvent();

            changePasswordPage.changePassword("", "new", "new");

            Assert.assertEquals("Please specify password.", profilePage.getError());

            changePasswordPage.changePassword("password", "new-password", "new-password");

            Assert.assertEquals("Your password has been updated", profilePage.getSuccess());

            events.expectAccount(EventType.UPDATE_PASSWORD).assertEvent();
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
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

        events.expectLogin().client("account").detail(Details.REDIRECT_URI, ACCOUNT_REDIRECT).assertEvent();

        Assert.assertEquals("Tom", profilePage.getFirstName());
        Assert.assertEquals("Brady", profilePage.getLastName());
        Assert.assertEquals("test-user@localhost", profilePage.getEmail());

        // All fields are required, so there should be an error when something is missing.
        profilePage.updateProfile("", "New last", "new@email.com");

        Assert.assertEquals("Please specify first name.", profilePage.getError());
        Assert.assertEquals("", profilePage.getFirstName());
        Assert.assertEquals("New last", profilePage.getLastName());
        Assert.assertEquals("new@email.com", profilePage.getEmail());

        events.assertEmpty();

        profilePage.updateProfile("New first", "", "new@email.com");

        Assert.assertEquals("Please specify last name.", profilePage.getError());
        Assert.assertEquals("New first", profilePage.getFirstName());
        Assert.assertEquals("", profilePage.getLastName());
        Assert.assertEquals("new@email.com", profilePage.getEmail());

        events.assertEmpty();

        profilePage.updateProfile("New first", "New last", "");

        Assert.assertEquals("Please specify email.", profilePage.getError());
        Assert.assertEquals("New first", profilePage.getFirstName());
        Assert.assertEquals("New last", profilePage.getLastName());
        Assert.assertEquals("", profilePage.getEmail());

        events.assertEmpty();

        profilePage.clickCancel();

        Assert.assertEquals("Tom", profilePage.getFirstName());
        Assert.assertEquals("Brady", profilePage.getLastName());
        Assert.assertEquals("test-user@localhost", profilePage.getEmail());

        events.assertEmpty();

        profilePage.updateProfile("New first", "New last", "new@email.com");

        Assert.assertEquals("Your account has been updated", profilePage.getSuccess());
        Assert.assertEquals("New first", profilePage.getFirstName());
        Assert.assertEquals("New last", profilePage.getLastName());
        Assert.assertEquals("new@email.com", profilePage.getEmail());

        events.expectAccount(EventType.UPDATE_PROFILE).assertEvent();
        events.expectAccount(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();
    }

    @Test
    public void setupTotp() {
        totpPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().client("account").detail(Details.REDIRECT_URI, ACCOUNT_REDIRECT + "?path=totp").assertEvent();

        Assert.assertTrue(totpPage.isCurrent());

        Assert.assertFalse(driver.getPageSource().contains("Remove Google"));

        // Error with false code
        totpPage.configure(totp.generate(totpPage.getTotpSecret() + "123"));

        Assert.assertEquals("Invalid authenticator code", profilePage.getError());

        totpPage.configure(totp.generate(totpPage.getTotpSecret()));

        Assert.assertEquals("Mobile authenticator configured.", profilePage.getSuccess());

        events.expectAccount(EventType.UPDATE_TOTP).assertEvent();

        Assert.assertTrue(driver.getPageSource().contains("pficon-delete"));

        totpPage.removeTotp();

        events.expectAccount(EventType.REMOVE_TOTP).assertEvent();
    }

    @Test
    public void changeProfileNoAccess() throws Exception {
        profilePage.open();
        loginPage.login("test-user-no-access@localhost", "password");

        events.expectLogin().client("account").user(keycloakRule.getUser("test", "test-user-no-access@localhost").getId())
                .detail(Details.USERNAME, "test-user-no-access@localhost")
                .detail(Details.REDIRECT_URI, ACCOUNT_REDIRECT).assertEvent();

        Assert.assertTrue(errorPage.isCurrent());
        Assert.assertEquals("No access", errorPage.getError());
    }

    @Test
    public void viewLog() {
        keycloakRule.update(new KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setEventsEnabled(true);
            }
        });

        try {
            List<Event> expectedEvents = new LinkedList<Event>();

            loginPage.open();
            loginPage.clickRegister();

            registerPage.register("view", "log", "view-log@localhost", "view-log", "password", "password");

            expectedEvents.add(events.poll());
            expectedEvents.add(events.poll());

            profilePage.open();
            profilePage.updateProfile("view", "log2", "view-log@localhost");

            expectedEvents.add(events.poll());

            logPage.open();

            Assert.assertTrue(logPage.isCurrent());

            List<List<String>> actualEvents = logPage.getEvents();

            Assert.assertEquals(expectedEvents.size(), actualEvents.size());

            for (Event e : expectedEvents) {
                boolean match = false;
                for (List<String> a : logPage.getEvents()) {
                    if (e.getType().toString().replace('_', ' ').toLowerCase().equals(a.get(1)) &&
                            e.getIpAddress().equals(a.get(2)) &&
                            e.getClientId().equals(a.get(3))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    Assert.fail("Event not found " + e.getType());
                }
            }
        } finally {
            keycloakRule.update(new KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setEventsEnabled(false);
                }
            });
        }
    }

    @Test
    public void sessions() {
        loginPage.open();
        loginPage.clickRegister();

        registerPage.register("view", "sessions", "view-sessions@localhost", "view-sessions", "password", "password");

        Event registerEvent = events.expectRegister("view-sessions", "view-sessions@localhost").assertEvent();
        String userId = registerEvent.getUserId();

        events.expectLogin().user(userId).detail(Details.USERNAME, "view-sessions").assertEvent();

        sessionsPage.open();

        Assert.assertTrue(sessionsPage.isCurrent());

        List<List<String>> sessions = sessionsPage.getSessions();
        Assert.assertEquals(1, sessions.size());
        Assert.assertEquals("127.0.0.1", sessions.get(0).get(0));

        // Create second session
        WebDriver driver2 = WebRule.createWebDriver();
        try {
            OAuthClient oauth2 = new OAuthClient(driver2);
            oauth2.state("mystate");
            oauth2.doLogin("view-sessions", "password");

            Event login2Event = events.expectLogin().user(userId).detail(Details.USERNAME, "view-sessions").assertEvent();

            sessionsPage.open();
            sessions = sessionsPage.getSessions();
            Assert.assertEquals(2, sessions.size());

            sessionsPage.logoutAll();

            events.expectLogout(registerEvent.getSessionId());
            events.expectLogout(login2Event.getSessionId());
        } finally {
            driver2.close();
        }
    }

}
