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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.pages.AppPage.RequestType;

import org.keycloak.testsuite.util.*;
import javax.mail.internet.MimeMessage;

import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RegisterTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected AccountUpdateProfilePage accountPage;

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void registerExistingUsernameForbidden() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerExistingUser@email", "roleRichUser", "password", "password");

        registerPage.assertCurrent();
        assertEquals("Username already exists.", registerPage.getError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("registerExistingUser@email", registerPage.getEmail());
        assertEquals("", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        events.expectRegister("roleRichUser", "registerExistingUser@email")
                .removeDetail(Details.EMAIL)
                .user((String) null).error("username_in_use").assertEvent();
    }
 
    @Test
    public void registerExistingEmailForbidden() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "test-user@localhost", "registerExistingUser", "password", "password");

        registerPage.assertCurrent();
        assertEquals("Email already exists.", registerPage.getError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("", registerPage.getEmail());
        assertEquals("registerExistingUser", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        events.expectRegister("registerExistingUser", "registerExistingUser@email")
                .removeDetail(Details.EMAIL)
                .user((String) null).error("email_in_use").assertEvent();
    }
 
    @Test
    public void registerExistingEmailAllowed() {
        setDuplicateEmailsAllowed(true);
                
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "test-user@localhost", "registerExistingEmailUser", "password", "password");

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister("registerExistingEmailUser", "test-user@localhost").assertEvent().getUserId();
        events.expectLogin().detail("username", "registerexistingemailuser").user(userId).assertEvent();

        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        assertEquals("registerexistingemailuser", user.getUsername());
        assertEquals("test-user@localhost", user.getEmail());
        assertEquals("firstName", user.getFirstName());
        assertEquals("lastName", user.getLastName());

        testRealm().users().get(userId).remove();
        setDuplicateEmailsAllowed(false);
    }

    @Test
    public void registerUserInvalidPasswordConfirm() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserInvalidPasswordConfirm@email", "registerUserInvalidPasswordConfirm", "password", "invalid");

        registerPage.assertCurrent();
        assertEquals("Password confirmation doesn't match.", registerPage.getError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("registerUserInvalidPasswordConfirm@email", registerPage.getEmail());
        assertEquals("registerUserInvalidPasswordConfirm", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        events.expectRegister("registerUserInvalidPasswordConfirm", "registerUserInvalidPasswordConfirm@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .user((String) null).error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserMissingPassword() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingPassword@email", "registerUserMissingPassword", null, null);

        registerPage.assertCurrent();
        assertEquals("Please specify password.", registerPage.getError());

        events.expectRegister("registerUserMissingPassword", "registerUserMissingPassword@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .user((String) null).error("invalid_registration").assertEvent();
    }

    @Test
    public void registerPasswordPolicy() {
        /*keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setPasswordPolicy(new PasswordPolicy("length"));
            }
        });*/
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setPasswordPolicy("length");
        testRealm().update(realm);

        try {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerPasswordPolicy@email", "registerPasswordPolicy", "pass", "pass");

            registerPage.assertCurrent();
            assertEquals("Invalid password: minimum length 8.", registerPage.getError());

            events.expectRegister("registerPasswordPolicy", "registerPasswordPolicy@email")
                    .removeDetail(Details.USERNAME)
                    .removeDetail(Details.EMAIL)
                    .user((String) null).error("invalid_registration").assertEvent();

            registerPage.register("firstName", "lastName", "registerPasswordPolicy@email", "registerPasswordPolicy", "password", "password");
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = events.expectRegister("registerPasswordPolicy", "registerPasswordPolicy@email").assertEvent().getUserId();

            events.expectLogin().user(userId).detail(Details.USERNAME, "registerpasswordpolicy").assertEvent();
        } finally {
            /*keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setPasswordPolicy(new PasswordPolicy(null));
                }
            });*/
        }
    }

    @Test
    public void registerUserMissingUsername() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingUsername@email", null, "password", "password");

        registerPage.assertCurrent();
        assertEquals("Please specify username.", registerPage.getError());

        events.expectRegister(null, "registerUserMissingUsername@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserManyErrors() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register(null, null, null, null, null, null);

        registerPage.assertCurrent();

        assertEquals("Please specify username.\n" +
                "Please specify first name.\n" +
                "Please specify last name.\n" +
                "Please specify email.\n" +
                "Please specify password.", registerPage.getError());

        events.expectRegister(null, "registerUserMissingUsername@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserMissingEmail() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", null, "registerUserMissingEmail", "password", "password");
        registerPage.assertCurrent();
        assertEquals("Please specify email.", registerPage.getError());
        events.expectRegister("registerUserMissingEmail", null)
                .removeDetail("email")
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserInvalidEmail() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserInvalidEmailemail", "registerUserInvalidEmail", "password", "password");
        registerPage.assertCurrent();
        assertEquals("registerUserInvalidEmailemail", registerPage.getEmail());
        assertEquals("Invalid email address.", registerPage.getError());
        events.expectRegister("registerUserInvalidEmail", "registerUserInvalidEmailemail")
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserSuccess@email", "registerUserSuccess", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister("registerUserSuccess", "registerUserSuccess@email").assertEvent().getUserId();
        assertUserRegistered(userId, "registerusersuccess", "registerusersuccess@email");
    }

    private void assertUserRegistered(String userId, String username, String email) {
        events.expectLogin().detail("username", username.toLowerCase()).user(userId).assertEvent();

        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);
        // test user info is set from form
        assertEquals(username.toLowerCase(), user.getUsername());
        assertEquals(email.toLowerCase(), user.getEmail());
        assertEquals("firstName", user.getFirstName());
        assertEquals("lastName", user.getLastName());
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // GreenMailRule is not working atm
    public void registerUserSuccessWithEmailVerification() throws Exception {
        RealmRepresentation realm = testRealm().toRepresentation();
        boolean origVerifyEmail = realm.isVerifyEmail();

        try {
            realm.setVerifyEmail(true);
            testRealm().update(realm);

            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserSuccessWithEmailVerification@email", "registerUserSuccessWithEmailVerification", "password", "password");
            verifyEmailPage.assertCurrent();

            String userId = events.expectRegister("registerUserSuccessWithEmailVerification", "registerUserSuccessWithEmailVerification@email").assertEvent().getUserId();

            {
                assertTrue("Expecting verify email", greenMail.waitForIncomingEmail(1000, 1));

                events.expect(EventType.SEND_VERIFY_EMAIL)
                  .detail(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                  .user(userId)
                  .assertEvent();

                MimeMessage message = greenMail.getLastReceivedMessage();
                String link = MailUtils.getPasswordResetEmailLink(message);

                driver.navigate().to(link);
            }

            events.expectRequiredAction(EventType.VERIFY_EMAIL)
              .detail(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
              .user(userId)
              .assertEvent();

            assertUserRegistered(userId, "registerUserSuccessWithEmailVerification", "registerUserSuccessWithEmailVerification@email");

            appPage.assertCurrent();
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            // test that timestamp is current with 10s tollerance
            // test user info is set from form
        } finally {
            realm.setVerifyEmail(origVerifyEmail);
            testRealm().update(realm);
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // GreenMailRule is not working atm
    public void registerUserSuccessWithEmailVerificationWithResend() throws Exception {
        RealmRepresentation realm = testRealm().toRepresentation();
        boolean origVerifyEmail = realm.isVerifyEmail();
        try {
            realm.setVerifyEmail(true);
            testRealm().update(realm);

            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserSuccessWithEmailVerificationWithResend@email", "registerUserSuccessWithEmailVerificationWithResend", "password", "password");
            verifyEmailPage.assertCurrent();

            String userId = events.expectRegister("registerUserSuccessWithEmailVerificationWithResend", "registerUserSuccessWithEmailVerificationWithResend@email").assertEvent().getUserId();

            {
                assertTrue("Expecting verify email", greenMail.waitForIncomingEmail(1000, 1));

                events.expect(EventType.SEND_VERIFY_EMAIL)
                  .detail(Details.EMAIL, "registerUserSuccessWithEmailVerificationWithResend@email".toLowerCase())
                  .user(userId)
                  .assertEvent();

                verifyEmailPage.clickResendEmail();
                verifyEmailPage.assertCurrent();

                assertTrue("Expecting second verify email", greenMail.waitForIncomingEmail(1000, 1));

                events.expect(EventType.SEND_VERIFY_EMAIL)
                  .detail(Details.EMAIL, "registerUserSuccessWithEmailVerificationWithResend@email".toLowerCase())
                  .user(userId)
                  .assertEvent();

                MimeMessage message = greenMail.getLastReceivedMessage();
                String link = MailUtils.getPasswordResetEmailLink(message);

                driver.navigate().to(link);
            }

            events.expectRequiredAction(EventType.VERIFY_EMAIL)
              .detail(Details.EMAIL, "registerUserSuccessWithEmailVerificationWithResend@email".toLowerCase())
              .user(userId)
              .assertEvent();

            assertUserRegistered(userId, "registerUserSuccessWithEmailVerificationWithResend", "registerUserSuccessWithEmailVerificationWithResend@email");

            appPage.assertCurrent();
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            // test that timestamp is current with 10s tollerance
            // test user info is set from form
        } finally {
            realm.setVerifyEmail(origVerifyEmail);
            testRealm().update(realm);
        }
    }

    @Test
    public void registerUserUmlats() {
        loginPage.open();

        assertTrue(loginPage.isCurrent());

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("Äǜṳǚǘǖ", "Öṏṏ", "registeruserumlats@email", "registeruserumlats", "password", "password");

        String userId = events.expectRegister("registeruserumlats", "registeruserumlats@email").assertEvent().getUserId();
        events.expectLogin().detail("username", "registeruserumlats").user(userId).assertEvent();

        accountPage.open();
        assertTrue(accountPage.isCurrent());

        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        assertEquals("Äǜṳǚǘǖ", user.getFirstName());
        assertEquals("Öṏṏ", user.getLastName());

        assertEquals("Äǜṳǚǘǖ", accountPage.getFirstName());
        assertEquals("Öṏṏ", accountPage.getLastName());
    }

    // KEYCLOAK-3266
    @Test
    public void registerUserNotUsernamePasswordPolicy() {
        adminClient.realm("test").update(RealmBuilder.create().passwordPolicy("notUsername").build());

        loginPage.open();

        assertTrue(loginPage.isCurrent());

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "registerUserNotUsername", "registerUserNotUsername", "registerUserNotUsername");

        assertTrue(registerPage.isCurrent());
        assertEquals("Invalid password: must not be equal to the username.", registerPage.getError());

        adminClient.realm("test").users().create(UserBuilder.create().username("registerUserNotUsername").build());

        registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "registerUserNotUsername", "registerUserNotUsername", "registerUserNotUsername");

        assertTrue(registerPage.isCurrent());
        assertEquals("Username already exists.", registerPage.getError());

        registerPage.register("firstName", "lastName", "registerUserNotUsername@email", null, "password", "password");

        assertTrue(registerPage.isCurrent());
        assertEquals("Please specify username.", registerPage.getError());
    }

    protected UserRepresentation getUser(String userId) {
        return testRealm().users().get(userId).toRepresentation();
    }

    @Test
    public void registerExistingUser_emailAsUsername() {
        configureRealmRegistrationEmailAsUsername(true);

        try {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "test-user@localhost", "password", "password");

            registerPage.assertCurrent();
            assertEquals("Email already exists.", registerPage.getError());

            events.expectRegister("test-user@localhost", "test-user@localhost").user((String) null).error("email_in_use").assertEvent();
        } finally {
            configureRealmRegistrationEmailAsUsername(false);
        }
    }

    @Test
    public void registerUserMissingOrInvalidEmail_emailAsUsername() {
        configureRealmRegistrationEmailAsUsername(true);

        try {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", null, "password", "password");
            registerPage.assertCurrent();
            assertEquals("Please specify email.", registerPage.getError());
            events.expectRegister(null, null).removeDetail("username").removeDetail("email").error("invalid_registration").assertEvent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserInvalidEmailemail", "password", "password");
            registerPage.assertCurrent();
            assertEquals("Invalid email address.", registerPage.getError());
            events.expectRegister("registerUserInvalidEmailemail", "registerUserInvalidEmailemail").error("invalid_registration").assertEvent();
        } finally {
            configureRealmRegistrationEmailAsUsername(false);
        }
    }

    @Test
    public void registerUserSuccess_emailAsUsername() {
        configureRealmRegistrationEmailAsUsername(true);

        try {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserSuccessE@email", "password", "password");

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = events.expectRegister("registerUserSuccessE@email", "registerUserSuccessE@email").assertEvent().getUserId();
            events.expectLogin().detail("username", "registerusersuccesse@email").user(userId).assertEvent();

            UserRepresentation user = getUser(userId);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getCreatedTimestamp());
            // test that timestamp is current with 10s tollerance
            Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);

        } finally {
            configureRealmRegistrationEmailAsUsername(false);
        }
    }

    protected void configureRealmRegistrationEmailAsUsername(final boolean value) {
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setRegistrationEmailAsUsername(value);
        testRealm().update(realm);
    }

    private void setDuplicateEmailsAllowed(boolean allowed) {
        RealmRepresentation testRealm = testRealm().toRepresentation();
        testRealm.setDuplicateEmailsAllowed(allowed);
        testRealm().update(testRealm);
    }

}
