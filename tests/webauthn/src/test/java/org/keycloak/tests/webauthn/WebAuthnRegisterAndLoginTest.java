/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.webauthn;

import jakarta.ws.rs.core.Response;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventMatchers;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.testframework.ui.page.SelectAuthenticatorPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.webauthn.pages.WebAuthnAuthenticatorsListPage;
import org.keycloak.tests.webauthn.realm.WebAuthnRealmConfigBuilder;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

@KeycloakIntegrationTest
public class WebAuthnRegisterAndLoginTest extends AbstractWebAuthnVirtualTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    PasswordPage passwordPage;

    @InjectPage
    SelectAuthenticatorPage selectAuthenticatorPage;

    @BeforeEach
    public void initWebAuthnTest() {
        RealmRepresentation realmRepresentation;
        try {
            realmRepresentation = JsonSerialization.readValue(WebAuthnRegisterAndLoginTest.class.getResourceAsStream("testrealm-webauthn.json"), RealmRepresentation.class);
        } catch (IOException exp) {
            throw new RuntimeException("JSON file cannot be loaded: ",exp);
        }

        List<String> acceptableAaguids = new ArrayList<>();
        acceptableAaguids.add("00000000-0000-0000-0000-000000000000");
        acceptableAaguids.add("6d44ba9b-f6ec-2e49-b930-0c8fe920cb73");

        realmRepresentation.setWebAuthnPolicyAcceptableAaguids(acceptableAaguids);
        setUpVirtualAuthenticator(realmRepresentation);
    }

    @AfterEach
    public void cleanupWebAuthnTest() {
        removeVirtualAuthenticator();
    }

    @Test
    public void registerUserSuccess() throws IOException {
        String username = "registerUserSuccess";
        String email = "registerUserSuccess@email";
        String password = generatePassword();
        String userId = null;

        managedRealm.admin().update(updateRealmWithDefaultWebAuthnSettings(managedRealm.admin()).build());

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();

        String authenticatorLabel = SecretGenerator.getInstance().randomString(24);
        registerPage.register("firstName", "lastName", email, username, password);

        // User was registered. Now he needs to register WebAuthn credential
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        //appPage.openAccount();

        // confirm that registration is successfully completed
        EventRepresentation event = events.poll();
        Assertions.assertEquals(event.getType(), EventType.REGISTER.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        Assertions.assertEquals(event.getClientId(),
                Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app")).toRepresentation().getId());
        Assertions.assertEquals(event.getDetails().get(Details.USERNAME), username);
        Assertions.assertEquals(event.getDetails().get(Details.EMAIL), email);
        Assertions.assertEquals(event.getDetails().get(Details.REGISTER_METHOD), "form");
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());

        userId = event.getUserId();

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.CUSTOM_REQUIRED_ACTION.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), authenticatorLabel);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR), ALL_ZERO_AAGUID);

        String regPubKeyCredentialId1 = event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.UPDATE_CREDENTIAL.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), authenticatorLabel);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR), ALL_ZERO_AAGUID);

        String regPubKeyCredentialId2 = event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

        Assertions.assertEquals(event.getType(), EventType.REGISTER.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        Assertions.assertEquals(event.getClientId(),
                Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app")).toRepresentation().getId());
        Assertions.assertEquals(event.getDetails().get(Details.USERNAME), username);
        Assertions.assertEquals(event.getDetails().get(Details.EMAIL), email);
        Assertions.assertEquals(event.getDetails().get(Details.REGISTER_METHOD), "form");
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());

        assertThat(regPubKeyCredentialId1, equalTo(regPubKeyCredentialId2));

        event = events.poll();

        // confirm login event
        Assertions.assertEquals(event.getType(), EventType.LOGIN.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), authenticatorLabel);

        // confirm user registered
        assertUserRegistered(userId, username.toLowerCase(), email.toLowerCase());
        assertRegisteredCredentials(userId, ALL_ZERO_AAGUID, "none");

        events.clear();

        // logout by user
        logout();

        event = events.poll();
        // confirm logout event
        Assertions.assertEquals(event.getType(), EventType.LOGOUT.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        Assertions.assertEquals(event.getClientId(),
                Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "account")).toRepresentation().getId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());

        // login by user
        oAuthClient.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();

        webAuthnLoginPage.assertCurrent();

        final WebAuthnAuthenticatorsListPage authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));
        assertThat(authenticators.getLabels(), Matchers.contains(authenticatorLabel));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        //appPage.openAccount();

        event = events.poll();

        // confirm login event
        Assertions.assertEquals(event.getType(), EventType.LOGIN.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR), regPubKeyCredentialId2);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.USER_VERIFICATION_CHECKED), Boolean.FALSE.toString());

        events.clear();
        // logout by user
        logout();

        event = events.poll();
        // confirm logout event
        Assertions.assertEquals(event.getType(), EventType.LOGOUT.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        Assertions.assertEquals(event.getClientId(),
                Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "account")).toRepresentation().getId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
    }

    @Test
    public void webAuthnPasswordlessAlternativeWithWebAuthnAndPassword() throws IOException {
        String userId = null;

        final String WEBAUTHN_LABEL = "webauthn";
        final String PASSWORDLESS_LABEL = "passwordless";
        String username = "test-user@localhost";
        String password = "password";

        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow(webAuthnTogetherPasswordlessFlow());

        managedRealm.admin().update(realm);

        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), username);
        assertThat(user, notNullValue());
        user.getRequiredActions().add(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);

        UserResource userResource = managedRealm.admin().users().get(user.getId());
        assertThat(userResource, notNullValue());
        userResource.update(user);

        user = userResource.toRepresentation();
        assertThat(user, notNullValue());
        assertThat(user.getRequiredActions(), hasItem(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID));

        userId = user.getId();

        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly("test-user@localhost");
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.login(password);

        events.clear();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(WEBAUTHN_LABEL);

        webAuthnRegisterPage.assertCurrent();

        EventRepresentation event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.CUSTOM_REQUIRED_ACTION.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), WEBAUTHN_LABEL);

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.UPDATE_CREDENTIAL.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), WEBAUTHN_LABEL);

        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(PASSWORDLESS_LABEL);

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.CUSTOM_REQUIRED_ACTION.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), PASSWORDLESS_LABEL);

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.UPDATE_CREDENTIAL.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), PASSWORDLESS_LABEL);

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.LOGIN.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), WEBAUTHN_LABEL);

        events.clear();

        logout();

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.LOGOUT.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        Assertions.assertEquals(event.getClientId(),
                Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "account")).toRepresentation().getId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());

        // Password + WebAuthn Passkey
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLoginWithUsernameOnly(username);
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.login(password);

        webAuthnLoginPage.assertCurrent();

        final WebAuthnAuthenticatorsListPage authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));
        assertThat(authenticators.getLabels(), Matchers.contains(WEBAUTHN_LABEL));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        logout();

        // Only passwordless login
        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly(username);
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.clickTryAnotherWayLink();

        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                is("Use your Passkey for passwordless sign in."));
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);

        webAuthnLoginPage.assertCurrent();
        assertThat(webAuthnLoginPage.getAuthenticators().getCount(), is(0));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        logout();
    }

    @Test
    public void webAuthnPasswordlessShouldFailIfUserIsDeletedInBetween() throws IOException {

        final String WEBAUTHN_LABEL = "webauthn";
        final String PASSWORDLESS_LABEL = "passwordless";

        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow(webAuthnTogetherPasswordlessFlow());

        managedRealm.admin().update(realm);

        String username = "webauthn-tester@localhost";
        String password = generatePassword();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setFirstName("WebAuthN");
        user.setLastName("Tester");

        String userId = AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, password, false);

        user = AdminApiUtil.findUserByUsername(managedRealm.admin(), username);

        assertThat(user, notNullValue());
        user.getRequiredActions().add(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);

        UserResource userResource = managedRealm.admin().users().get(user.getId());
        assertThat(userResource, notNullValue());
        userResource.update(user);

        user = userResource.toRepresentation();
        assertThat(user, notNullValue());
        assertThat(user.getRequiredActions(), hasItem(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID));

        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly(username);
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.login(password);

        events.clear();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(WEBAUTHN_LABEL);

        webAuthnRegisterPage.assertCurrent();

        EventRepresentation event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.CUSTOM_REQUIRED_ACTION.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), WEBAUTHN_LABEL);

        event = events.poll();

        Assertions.assertEquals(event.getType(), EventType.UPDATE_CREDENTIAL.toString());
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        Assertions.assertEquals(event.getUserId(), AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId());
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        Assertions.assertEquals(event.getDetails().get(Details.REDIRECT_URI), testApp.getRedirectionUri());
        Assertions.assertEquals(event.getDetails().get(Details.CONSENT), Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        Assertions.assertEquals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION), WebAuthnRegisterFactory.PROVIDER_ID);
        Assertions.assertEquals(event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR), WEBAUTHN_LABEL);

        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(PASSWORDLESS_LABEL);

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));

        logout();

        // Password + WebAuthn Passkey
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLoginWithUsernameOnly(username);
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.login(password);

        webAuthnLoginPage.assertCurrent();

        final WebAuthnAuthenticatorsListPage authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));
        assertThat(authenticators.getLabels(), Matchers.contains(WEBAUTHN_LABEL));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        logout();

        // Only passwordless login
        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly(username);
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.clickTryAnotherWayLink();

        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                is("Use your Passkey for passwordless sign in."));
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);

        webAuthnLoginPage.assertCurrent();
        assertThat(webAuthnLoginPage.getAuthenticators().getCount(), is(0));

        // remove testuser before user authenticates via webauthn
        try (Response resp = managedRealm.admin().users().delete(userId)) {
            // ignore
        }

        webAuthnLoginPage.clickAuthenticate();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), is("Unknown user authenticated by the Passkey."));
    }

    @Test
    public void webAuthnTwoFactorAndWebAuthnPasswordlessTogether() throws IOException {
        // Change binding to browser-webauthn-passwordless. This is flow, which contains both "webauthn" and "webauthn-passwordless" authenticator
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow("browser-webauthn-passwordless");
        // Login as test-user@localhost with password
        oAuthClient.openLoginForm();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        errorPage.assertCurrent();

        // User is not allowed to register passwordless authenticator in this flow
        assertThat(events.poll().getError(), is("invalid_user_credentials"));
        assertThat(errorPage.getError(), is("Cannot login, credential setup required."));
    }

    private void assertUserRegistered(String userId, String username, String email) {
        UserRepresentation user = getUser(userId);
        assertThat(user, notNullValue());
        assertThat(user.getCreatedTimestamp(), notNullValue());

        // test that timestamp is current with 60s tollerance
        assertThat((System.currentTimeMillis() - user.getCreatedTimestamp()) < 60000, is(true));

        // test user info is set from form
        assertThat(user.getUsername(), is(username.toLowerCase()));
        assertThat(user.getEmail(), is(email.toLowerCase()));
        assertThat(user.getFirstName(), is("firstName"));
        assertThat(user.getLastName(), is("lastName"));
    }

    private void assertRegisteredCredentials(String userId, String aaguid, String attestationStatementFormat) {
        List<CredentialRepresentation> credentials = getCredentials(userId);
        credentials.forEach(i -> {
            if (WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(i.getType())) {
                try {
                    WebAuthnCredentialData data = JsonSerialization.readValue(i.getCredentialData(), WebAuthnCredentialData.class);
                    assertThat(data.getAaguid(), is(aaguid));
                    assertThat(data.getAttestationStatementFormat(), is(attestationStatementFormat));
                } catch (IOException e) {
                    Assertions.fail();
                }
            }
        });
    }

    private UserRepresentation getUser(String userId) {
        return managedRealm.admin().users().get(userId).toRepresentation();
    }

    private List<CredentialRepresentation> getCredentials(String userId) {
        return managedRealm.admin().users().get(userId).credentials();
    }

    private static WebAuthnRealmConfigBuilder updateRealmWithDefaultWebAuthnSettings(RealmResource resource) {
        return new WebAuthnRealmConfigBuilder(resource.toRepresentation())
                .setWebAuthnPolicySignatureAlgorithms(Collections.singletonList("ES256"))
                .setWebAuthnPolicyAttestationConveyancePreference("none")
                .setWebAuthnPolicyAuthenticatorAttachment("cross-platform")
                .setWebAuthnPolicyRequireResidentKey("No")
                .setWebAuthnPolicyRpId(null)
                .setWebAuthnPolicyUserVerificationRequirement("preferred")
                .setWebAuthnPolicyAcceptableAaguids(Collections.singletonList(ALL_ZERO_AAGUID));
    }

    /**
     * This flow contains:
     * <p>
     * UsernameForm REQUIRED
     * Subflow REQUIRED
     * ** WebAuthnPasswordlessAuthenticator ALTERNATIVE
     * ** sub-subflow ALTERNATIVE
     * **** PasswordForm ALTERNATIVE
     * **** WebAuthnAuthenticator ALTERNATIVE
     *
     * @return flow alias
     */
    private String webAuthnTogetherPasswordlessFlow() {
        final String newFlowAlias = "browser-together-webauthn-flow";
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        runOnServer.run(session -> {
            FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .inForms(forms -> forms
                            .clear()
                            .addAuthenticatorExecution(REQUIRED, UsernameFormFactory.PROVIDER_ID)
                            .addSubFlowExecution(REQUIRED, subFlow -> subFlow
                                    .addAuthenticatorExecution(ALTERNATIVE, WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID)
                                    .addSubFlowExecution(ALTERNATIVE, passwordFlow -> passwordFlow
                                            .addAuthenticatorExecution(REQUIRED, PasswordFormFactory.PROVIDER_ID)
                                            .addAuthenticatorExecution(REQUIRED, WebAuthnAuthenticatorFactory.PROVIDER_ID))
                            ))
                    .defineAsBrowserFlow();
        });
        return newFlowAlias;
    }
}
