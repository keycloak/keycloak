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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.forms.RegistrationPassword;
import org.keycloak.authentication.forms.RegistrationRecaptcha;
import org.keycloak.authentication.forms.RegistrationTermsAndConditions;
import org.keycloak.authentication.forms.RegistrationUserCreation;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    protected ErrorPage errorPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    private String idTokenHint;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void registerExistingUsernameForbidden() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerExistingUser@email", "roleRichUser", generatePassword());

        registerPage.assertCurrent();
        assertEquals("Username already exists.", registerPage.getInputAccountErrors().getUsernameError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("registerExistingUser@email", registerPage.getEmail());
        assertEquals("roleRichUser", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        EventAssertion.assertError(events.poll())
                .type(EventType.REGISTER_ERROR)
                .error("username_in_use")
                .userId(null)
                .clientId(oauth.getClientId())
                .details(Details.USERNAME, "rolerichuser")
                .details(Details.EMAIL, "registerexistinguser@email");
    }
 
    @Test
    public void registerExistingEmailForbidden() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "test-user@localhost", "registerExistingUser", generatePassword());

        registerPage.assertCurrent();
        assertEquals("Email already exists.", registerPage.getInputAccountErrors().getEmailError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("test-user@localhost", registerPage.getEmail());
        assertEquals("registerExistingUser", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        EventAssertion.expectRegisterError(events.poll()).error("email_in_use").clientId(oauth.getClientId()).details(Details.USERNAME, "registerexistinguser").details(Details.EMAIL, "test-user@localhost");
    }
 
    @Test
    public void registerExistingEmailAllowed() throws IOException {
        try (RealmAttributeUpdater rau = setDuplicateEmailsAllowed(true).update()) {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "test-user@localhost", "registerExistingEmailUser", generatePassword());

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, "registerExistingEmailUser").details(Details.EMAIL, "test-user@localhost").getEvent().getUserId();
            EventAssertion.expectLoginSuccess(events.poll()).details("username", "registerexistingemailuser").userId(userId);

            assertUserBasicRegisterAttributes(userId, "registerexistingemailuser", "test-user@localhost", "firstName", "lastName");

            managedRealm.admin().users().get(userId).remove();
        }
    }

    @Test
    public void registerUpperCaseEmail() throws IOException {
        String userId = registerUpperCaseAndGetUserId(false, generatePassword());
        assertThat(userId, notNullValue());
        managedRealm.admin().users().get(userId).remove();
    }

    @Test
    public void registerUpperCaseEmailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            String userId = registerUpperCaseAndGetUserId(true, generatePassword());
            assertThat(userId, notNullValue());
            managedRealm.admin().users().get(userId).remove();
        }
    }

    @Test
    public void registerUpperCaseEmailWithChangedEmailAsUsername() throws IOException {
        final String password = generatePassword();
        String userId = registerUpperCaseAndGetUserId(false, password);
        assertThat(userId, notNullValue());
        oauth.logoutForm().idTokenHint(idTokenHint).open();
        events.clear();

        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            oauth.openLoginForm();
            loginPage.assertCurrent();

            final String EMAIL = "TEST@localhost";
            loginPage.login(EMAIL, password);
            assertThat(RequestType.AUTH_RESPONSE, is(appPage.getRequestType()));

            EventAssertion.expectLoginSuccess(events.poll())
                    .details("username", EMAIL)
                    .userId(userId);
        } finally {
            assertThat(userId, notNullValue());
            managedRealm.admin().users().get(userId).remove();
        }
    }

    @Test
    public void registerUserInvalidPasswordConfirm() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserInvalidPasswordConfirm@email", "registerUserInvalidPasswordConfirm", generatePassword(), "invalid");

        registerPage.assertCurrent();
        assertEquals("Password confirmation doesn't match.", registerPage.getInputPasswordErrors().getPasswordConfirmError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("registerUserInvalidPasswordConfirm@email", registerPage.getEmail());
        assertEquals("registerUserInvalidPasswordConfirm", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        EventAssertion.expectRegisterError(events.poll())
                .error("invalid_registration")
                .clientId(oauth.getClientId())
                .details(Details.EMAIL, "registeruserinvalidpasswordconfirm@email")
                .details(Details.USERNAME, "registeruserinvalidpasswordconfirm");
    }

    @Test
    public void registerUserMissingPassword() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingPassword@email", "registerUserMissingPassword", null, null);

        registerPage.assertCurrent();
        assertEquals("Please specify password.", registerPage.getInputPasswordErrors().getPasswordError());

        EventAssertion.expectRegisterError(events.poll())
                .error("invalid_registration")
                .clientId(oauth.getClientId())
                .details(Details.USERNAME, "registerusermissingpassword")
                .details(Details.EMAIL, "registerusermissingpassword@email");
    }

    @Test
    public void registerPasswordPolicy() throws IOException {
        try (RealmAttributeUpdater rau = getRealmAttributeUpdater().setPasswordPolicy("length").update()) {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerPasswordPolicy@email", "registerPasswordPolicy", generatePassword(3));

            registerPage.assertCurrent();
            assertEquals("Invalid password: minimum length 8.", registerPage.getInputPasswordErrors().getPasswordError());

            EventAssertion.expectRegisterError(events.poll())
                    .error("invalid_registration")
                    .clientId(oauth.getClientId())
                    .details(Details.USERNAME, "registerpasswordpolicy")
                    .details(Details.EMAIL, "registerpasswordpolicy@email");

            registerPage.register("firstName", "lastName", "registerPasswordPolicy@email", "registerPasswordPolicy", generatePassword());
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, "registerPasswordPolicy").details(Details.EMAIL, "registerPasswordPolicy@email").getEvent().getUserId();

            EventAssertion.expectLoginSuccess(events.poll()).userId(userId).details(Details.USERNAME, "registerpasswordpolicy");
        }
    }

    @Test
    public void registerUserMissingUsername() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingUsername@email", null, generatePassword());

        registerPage.assertCurrent();
        assertEquals("Please specify username.", registerPage.getInputAccountErrors().getUsernameError());

        EventAssertion.expectRegisterError(events.poll())
                .error("invalid_registration")
                .clientId(oauth.getClientId())
                .details(Details.EMAIL, "registerusermissingusername@email")
                .withoutDetails(Details.USERNAME);
    }

    @Test
    public void registerUserManyErrors() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register(null, null, null, null, null, null);

        registerPage.assertCurrent();

        assertEquals("Please specify username.", registerPage.getInputAccountErrors().getUsernameError());
        assertThat(registerPage.getInputAccountErrors().getFirstNameError(), anyOf(
                containsString("Please specify first name"),
                containsString("Please specify this field")
        ));
        assertThat(registerPage.getInputAccountErrors().getLastNameError(), anyOf(
                containsString("Please specify last name"),
                containsString("Please specify this field")
        ));
        assertThat(registerPage.getInputAccountErrors().getEmailError(), anyOf(
                containsString("Please specify email"),
                containsString("Please specify this field")
        ));

        assertThat(registerPage.getInputPasswordErrors().getPasswordError(), is("Please specify password."));

        EventAssertion.expectRegisterError(events.poll())
                .error("invalid_registration").clientId(oauth.getClientId())
                .withoutDetails(Details.USERNAME)
                .withoutDetails(Details.EMAIL);
    }

    @Test
    public void registerUserMissingEmail() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", null, "registerUserMissingEmail", generatePassword());
        registerPage.assertCurrent();
        assertEquals("Please specify email.", registerPage.getInputAccountErrors().getEmailError());
        EventAssertion.expectRegisterError(events.poll())
                .error("invalid_registration")
                .clientId(oauth.getClientId())
                .details(Details.USERNAME, "registerusermissingemail")
                .withoutDetails(Details.EMAIL);
    }

    @Test
    public void registerUserInvalidEmail() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserInvalidEmailemail", "registerUserInvalidEmail", generatePassword());
        registerPage.assertCurrent();
        assertEquals("registerUserInvalidEmailemail", registerPage.getEmail());
        assertEquals("Invalid email address.", registerPage.getInputAccountErrors().getEmailError());
        EventAssertion.expectRegisterError(events.poll())
                .error("invalid_registration")
                .clientId(oauth.getClientId())
                .details(Details.USERNAME, "registeruserinvalidemail")
                .details(Details.EMAIL, "registeruserinvalidemailemail");
    }

    @Test
    public void registerUserSuccess() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        //contains few special characters we want to be sure they are allowed in username
        String username = "register.U-se@rS_uccess";

        registerPage.register("firstName", "lastName", "registerUserSuccess@email", username, generatePassword());

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, username).details(Details.EMAIL, "registerUserSuccess@email").getEvent().getUserId();
        assertUserRegistered(userId, username.toLowerCase(), "registerusersuccess@email");

        UserRepresentation user = getUser(userId);

        assertEquals(Map.of(UserModel.LOCALE, List.of("en")), user.getAttributes());
    }

    @Test
    public void registerUserChangedLocaleSuccess() {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        errorPage.openLanguage("Deutsch");
        assertEquals("Deutsch", errorPage.getLanguageDropdownText());

        registerPage.register("firstName", "lastName", "registerGerman@localhost", "registerGerman", generatePassword());

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, "registerGerman").details(Details.EMAIL, "registerGerman@localhost").getEvent().getUserId();
        assertUserRegistered(userId, "registergerman", "registerGerman@localhost");

        UserRepresentation user = getUser(userId);
        assertEquals(Map.of(UserModel.LOCALE, List.of("de")), user.getAttributes());
    }

    @Test
    public void registerUserSuccessEditUsernameDisabled() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        Boolean editUsernameAllowed = realm.isEditUsernameAllowed();
        Boolean registrationEmailAsUsername = realm.isRegistrationEmailAsUsername();
        realm.setEditUsernameAllowed(false);
        realm.setRegistrationEmailAsUsername(false);
        getCleanup().addCleanup(() -> {
            realm.setEditUsernameAllowed(editUsernameAllowed);
            realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            managedRealm.admin().update(realm);
        });
        managedRealm.admin().update(realm);
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        String username = KeycloakModelUtils.generateId();
        String email = username + "@email.com";
        registerPage.register("firstName", "lastName", email, username, generatePassword());

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, username).details(Details.EMAIL, email).getEvent().getUserId();
        assertUserRegistered(userId, username, email);
    }

    @Test
    public void registerUserSuccessEditUsernameEnabled() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        Boolean editUsernameAllowed = realm.isEditUsernameAllowed();
        Boolean registrationEmailAsUsername = realm.isRegistrationEmailAsUsername();
        realm.setEditUsernameAllowed(true);
        realm.setRegistrationEmailAsUsername(false);
        getCleanup().addCleanup(() -> {
            realm.setEditUsernameAllowed(editUsernameAllowed);
            realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            managedRealm.admin().update(realm);
        });
        managedRealm.admin().update(realm);
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        String username = KeycloakModelUtils.generateId();
        String email = username + "@email.com";
        registerPage.register("firstName", "lastName", email, username, generatePassword());

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, username).details(Details.EMAIL, email).getEvent().getUserId();
        assertUserRegistered(userId, username, email);
    }

    private UserRepresentation assertUserRegistered(String userId, String username, String email) {
        EventAssertion.expectLoginSuccess(events.poll()).details("username", username.toLowerCase()).userId(userId);

        UserRepresentation user = getUser(userId);
        Assertions.assertNotNull(user);
        Assertions.assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);
        assertUserBasicRegisterAttributes(userId, username, email, "firstName", "lastName");
        return user;
    }

    @Test
    public void registerUserUmlats() {
        oauth.openLoginForm();

        assertTrue(loginPage.isCurrent());

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("Äǜṳǚǘǖ", "Öṏṏ", "registeruserumlats@email", "registeruserumlats", generatePassword());

        String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, "registeruserumlats").details(Details.EMAIL, "registeruserumlats@email").getEvent().getUserId();
        EventAssertion.expectLoginSuccess(events.poll()).details("username", "registeruserumlats").userId(userId);

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm("test"), "registeruserumlats");

        assertEquals("Äǜṳǚǘǖ", userRepresentation.getFirstName());
        assertEquals("Öṏṏ", userRepresentation.getLastName());
    }

    // KEYCLOAK-3266
    @Test
    public void registerUserNotUsernamePasswordPolicy() throws IOException {
        try (RealmAttributeUpdater rau = getRealmAttributeUpdater().setPasswordPolicy("notUsername").update()) {
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());

            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "registerUserNotUsername", "registerUserNotUsername", "registerUserNotUsername");

            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: must not be equal to the username.", registerPage.getInputPasswordErrors().getPasswordError());

            // Case-sensitivity - still should not allow to create password when lower-cased
            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "registerUserNotUsername", "registerusernotusername", "registerusernotusername");

            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: must not be equal to the username.", registerPage.getInputPasswordErrors().getPasswordError());

            try (Response response = adminClient.realm("test").users().create(UserBuilder.create().username("registerUserNotUsername").build())) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            }

            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "registerUserNotUsername", "registerUserNotUsername", "registerUserNotUsername");

            assertTrue(registerPage.isCurrent());
            assertEquals("Username already exists.", registerPage.getInputAccountErrors().getUsernameError());

            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", null, generatePassword());

            assertTrue(registerPage.isCurrent());
            assertEquals("Please specify username.", registerPage.getInputAccountErrors().getUsernameError());
        }
    }

    // KEYCLOAK-27643
    @Test
    public void registerUserNotContainsUsernamePasswordPolicy() throws IOException {
        try (RealmAttributeUpdater rau = getRealmAttributeUpdater().setPasswordPolicy("notContainsUsername").update()) {
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());

            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserNotContainsUsername@email", "Bob", "Bob123", "Bob123");
            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: Can not contain the username.", registerPage.getInputPasswordErrors().getPasswordError());

            registerPage.register("firstName", "lastName", "registerUserNotContainsUsername@email", "Bob", "123Bob", "123Bob");
            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: Can not contain the username.", registerPage.getInputPasswordErrors().getPasswordError());

            // Case-sensitivity - still should not allow to create password when lower-cased
            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "Bob", "123bob", "123bob");

            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: Can not contain the username.", registerPage.getInputPasswordErrors().getPasswordError());

            try (Response response = adminClient.realm("test").users().create(UserBuilder.create().username("Bob").build())) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            }

            registerPage.register("firstName", "lastName", "registerUserNotContainsUsername@email", "Bob", "registerUserNotContainsUsername", "registerUserNotContainsUsername");

            assertTrue(registerPage.isCurrent());
            assertEquals("Username already exists.", registerPage.getInputAccountErrors().getUsernameError());

            registerPage.register("firstName", "lastName", "registerUserNotContainsUsername@email", null, generatePassword());

            assertTrue(registerPage.isCurrent());
            assertEquals("Please specify username.", registerPage.getInputAccountErrors().getUsernameError());
        }
    }

    // KEYCLOAK-12729
    @Test
    public void registerUserNotEmailPasswordPolicy() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true)
                .setPasswordPolicy("notEmail").update()) {

            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());

            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserNotEmail@email", "registerUserNotEmail@email", "registerUserNotEmail@email");

            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: must not be equal to the email.", registerPage.getInputPasswordErrors().getPasswordError());

            // Case-sensitivity - still should not allow to create password when lower-cased
            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserNotEmail@email", "registerusernotemail@email", "registerusernotemail@email");

            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: must not be equal to the email.", registerPage.getInputPasswordErrors().getPasswordError());
        }
    }

    private UserRepresentation getUser(String userId) {
        return managedRealm.admin().users().get(userId).toRepresentation();
    }

    @Test
    public void registerExistingUser_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "test-user@localhost", generatePassword());

            registerPage.assertCurrent();
            assertEquals("Email already exists.", registerPage.getInputAccountErrors().getEmailError());

            EventAssertion.expectRegisterError(events.poll()).error("email_in_use").clientId(oauth.getClientId()).details(Details.USERNAME, "test-user@localhost").details(Details.EMAIL, "test-user@localhost");
        }
    }

    @Test
    public void registerUserMissingOrInvalidEmail_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", null, generatePassword());
            registerPage.assertCurrent();
            assertEquals("Please specify email.", registerPage.getInputAccountErrors().getEmailError());
            EventAssertion.expectRegisterError(events.poll()).error("invalid_registration").clientId(oauth.getClientId()).withoutDetails(Details.USERNAME).withoutDetails(Details.EMAIL);

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserInvalidEmailemail", generatePassword());
            registerPage.assertCurrent();
            assertEquals("Invalid email address.", registerPage.getInputAccountErrors().getEmailError());
            EventAssertion.expectRegisterError(events.poll()).error("invalid_registration").clientId(oauth.getClientId()).details(Details.USERNAME, "registeruserinvalidemailemail").details(Details.EMAIL, "registeruserinvalidemailemail");
        }
    }

    @Test
    public void registerUserSuccess_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserSuccessE@email", generatePassword());

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, "registerUserSuccessE@email").details(Details.EMAIL, "registerUserSuccessE@email").getEvent().getUserId();
            EventAssertion.expectLoginSuccess(events.poll()).details("username", "registerusersuccesse@email").userId(userId);

            UserRepresentation user = getUser(userId);
            Assertions.assertNotNull(user);
            Assertions.assertNotNull(user.getCreatedTimestamp());
            // test that timestamp is current with 10s tollerance
            assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);
        }
    }

    @Test
    public void testEmailAsUsernameWhenEditUserNameDisabled() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true)
                .setEditUserNameAllowed(false)
                .update()) {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "alice@email", generatePassword());

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        }
    }

    @Test
    public void registerWithLoginHint() throws IOException {

        registerPage.openWithLoginHint("username_test");

        assertEquals("username_test", registerPage.getUsername());
    }

    @Test
    public void registerWithLoginHint_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            registerPage.openWithLoginHint("test@test.com");

            assertEquals("test@test.com", registerPage.getEmail());
        }
    }

    //KEYCLOAK-14161
    @Test
    public void customRegistrationPageFormTest() {
        String newFlowAlias = "register - custom";
        configureRegistrationFlowWithCustomRegistrationPageForm(newFlowAlias);

        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();
    }

    //KEYCLOAK-15244
    @Test
    public void registerUserMissingTermsAcceptance() {
        configureRegistrationFlowWithCustomRegistrationPageForm(UUID.randomUUID().toString(),
                AuthenticationExecutionModel.Requirement.REQUIRED);

        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            final String password = generatePassword();
            registerPage.register("firstName", "lastName", "registerUserMissingTermsAcceptance@email",
                    "registerUserMissingTermsAcceptance", password, password, null, false, null);

            registerPage.assertCurrent();
            assertEquals("You must agree to our terms and conditions.", registerPage.getInputAccountErrors().getTermsError());

            EventAssertion.expectRegisterError(events.poll()).error("invalid_registration").clientId(oauth.getClientId()).details(Details.USERNAME, "registerusermissingtermsacceptance").details(Details.EMAIL, "registerusermissingtermsacceptance@email");
        } finally {
            revertRegistrationFlow();
        }
    }

    //KEYCLOAK-15244
    @Test
    public void registerUserSuccessTermsAcceptance() {
        configureRegistrationFlowWithCustomRegistrationPageForm(UUID.randomUUID().toString(),
                AuthenticationExecutionModel.Requirement.REQUIRED);

        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            final String password = generatePassword();
            registerPage.register("firstName", "lastName", "registerUserSuccessTermsAcceptance@email",
                    "registerUserSuccessTermsAcceptance", password, password, null, true, null);

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, "registerUserSuccessTermsAcceptance").details(Details.EMAIL, "registerUserSuccessTermsAcceptance@email")
                    .getEvent().getUserId();
            UserRepresentation user = assertUserRegistered(userId, "registerUserSuccessTermsAcceptance", "registerUserSuccessTermsAcceptance@email");
            assertEquals(Map.of(UserModel.LOCALE, List.of("en")), user.getAttributes());
        } finally {
            configureRegistrationFlowWithCustomRegistrationPageForm(UUID.randomUUID().toString());
        }
    }

    @Test
    public void registerUserSuccessTermsAcceptanceWithRequiredActionEnabled() {
        configureRegistrationFlowWithCustomRegistrationPageForm(UUID.randomUUID().toString(),
                AuthenticationExecutionModel.Requirement.REQUIRED);

        // configure Terms and Conditions required action as enabled and default
        RequiredActionProviderRepresentation tacRep = managedRealm.admin().flows().getRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
        Assertions.assertNotNull(tacRep);
        tacRep.setEnabled(true);
        tacRep.setDefaultAction(true);
        managedRealm.admin().flows().updateRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name(), tacRep);

        try {
            oauth.openLoginForm();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            final String password = generatePassword();
            int currentTime = Time.currentTime();
            registerPage.register("firstName", "lastName", "registerUserSuccessTermsAcceptance2@email",
                    "registerUserSuccessTermsAcceptance2", password, password, null, true, null);

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, "registerUserSuccessTermsAcceptance2").details(Details.EMAIL, "registerUserSuccessTermsAcceptance2@email")
                    .getEvent().getUserId();
            UserRepresentation user = assertUserRegistered(userId, "registerUserSuccessTermsAcceptance2", "registerUserSuccessTermsAcceptance2@email");
            Assertions.assertNotNull(user.getAttributes());
            Assertions.assertNotNull(user.getAttributes().get(TermsAndConditions.USER_ATTRIBUTE));
            Assertions.assertEquals(1, user.getAttributes().get(TermsAndConditions.USER_ATTRIBUTE).size());
            Assertions.assertTrue(Integer.parseInt(user.getAttributes().get(TermsAndConditions.USER_ATTRIBUTE).get(0)) >= currentTime);
        } finally {
            tacRep.setEnabled(false);
            tacRep.setDefaultAction(false);
            managedRealm.admin().flows().updateRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name(), tacRep);
            configureRegistrationFlowWithCustomRegistrationPageForm(UUID.randomUUID().toString());
        }
    }

    @Test
    public void testRegisterShouldFailBeforeUserCreationWhenUserIsInContext() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.clickBackToLogin();
        loginPage.assertCurrent(managedRealm.admin().toRepresentation().getRealm());

        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword("test-user@localhost");

        driver.navigate().back();
        driver.navigate().back();
        events.clear();

        UIUtils.navigateBackWithRefresh(driver, errorPage);
        Assertions.assertEquals("Action expired. Please continue with login now.", errorPage.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.REGISTER_ERROR)
                .error(Errors.GENERIC_AUTHENTICATION_ERROR)
                .clientId(oauth.getClientId())
                .details(Details.EXISTING_USER, "test-user@localhost")
                .details(Details.AUTHENTICATION_ERROR_DETAIL, Errors.DIFFERENT_USER_AUTHENTICATING)
                .withoutDetails(Details.USERNAME, Details.EMAIL);
    }

    protected RealmAttributeUpdater configureRealmRegistrationEmailAsUsername(final boolean value) {
        return getRealmAttributeUpdater().setRegistrationEmailAsUsername(value);
    }

    protected RealmAttributeUpdater setDuplicateEmailsAllowed(boolean allowed) {
        return getRealmAttributeUpdater().setDuplicateEmailsAllowed(allowed);
    }

    protected RealmAttributeUpdater setVerifyEmail(boolean value) {
        return getRealmAttributeUpdater().setVerifyEmail(value);
    }

    private RealmAttributeUpdater getRealmAttributeUpdater() {
        return new RealmAttributeUpdater(managedRealm.admin());
    }

    /**
     * Helper method for registering user with upper case email
     *
     * @param emailAsUsername is flag `Email as username` enabled
     * @return user ID
     */
    private String registerUpperCaseAndGetUserId(boolean emailAsUsername, String password) {
        final String EMAIL = "TEST@localhost";
        final String USERNAME = "UPPERCASE";
        final String EMAIL_OR_USERNAME = emailAsUsername ? EMAIL : USERNAME;

        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        if (emailAsUsername) {
            registerPage.registerWithEmailAsUsername("firstName", "lastName", EMAIL, password, password);
        } else {
            registerPage.register("firstName", "lastName", EMAIL, USERNAME, password, password);
        }

        String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId()).details(Details.USERNAME, EMAIL_OR_USERNAME).details(Details.EMAIL, EMAIL)
                .getEvent()
                .getUserId();

        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll())
                .details("username", EMAIL_OR_USERNAME.toLowerCase())
                .userId(userId).getEvent();
        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        idTokenHint = tokenResponse.getIdToken();
        assertUserBasicRegisterAttributes(userId, emailAsUsername ? null : USERNAME, EMAIL, "firstName", "lastName");

        return userId;
    }

    private void assertUserBasicRegisterAttributes(String userId, String username, String email, String firstName, String lastName) {
        UserRepresentation user = getUser(userId);
        assertThat(user, notNullValue());

        if (username != null) {
            assertThat(username, Matchers.equalToIgnoringCase(user.getUsername()));
        }
        assertThat(email.toLowerCase(), is(user.getEmail()));
        assertThat(firstName, is(user.getFirstName()));
        assertThat(lastName, is(user.getLastName()));
    }

    private void configureRegistrationFlowWithCustomRegistrationPageForm(String newFlowAlias) {
        configureRegistrationFlowWithCustomRegistrationPageForm(newFlowAlias, AuthenticationExecutionModel.Requirement.DISABLED);
    }

    private void configureRegistrationFlowWithCustomRegistrationPageForm(String newFlowAlias, AuthenticationExecutionModel.Requirement termsAndConditionRequirement) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyRegistrationFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .clear()
                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, CookieAuthenticatorFactory.PROVIDER_ID)
                .addSubFlowExecution("Sub Flow", AuthenticationFlow.BASIC_FLOW, AuthenticationExecutionModel.Requirement.ALTERNATIVE, subflow -> subflow
                        .addSubFlowExecution("Sub sub Form Flow", AuthenticationFlow.FORM_FLOW, AuthenticationExecutionModel.Requirement.REQUIRED, subsubflow -> subsubflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, RegistrationUserCreation.PROVIDER_ID)
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, RegistrationPassword.PROVIDER_ID)
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.DISABLED, RegistrationRecaptcha.PROVIDER_ID)
                                .addAuthenticatorExecution(termsAndConditionRequirement, RegistrationTermsAndConditions.PROVIDER_ID)
                        )
                )
                .defineAsRegistrationFlow() // Activate this new flow
        );
    }

    private void revertRegistrationFlow() {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(DefaultAuthenticationFlows.REGISTRATION_FLOW)
                .defineAsRegistrationFlow()
        );
    }

}
