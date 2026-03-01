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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.keycloak.WebAuthnConstants;
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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.testframework.ui.page.SelectAuthenticatorPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class WebAuthnRegisterAndLoginTest extends AbstractWebAuthnVirtualTest {

    @InjectRunOnServer(realmRef = "webauthn")
    RunOnServerClient runOnServer;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    PasswordPage passwordPage;

    @InjectPage
    SelectAuthenticatorPage selectAuthenticatorPage;

    @BeforeEach
    public void customizeWebAuthnTestRealm() {
        List<String> acceptableAaguids = new ArrayList<>();
        acceptableAaguids.add("00000000-0000-0000-0000-000000000000");
        acceptableAaguids.add("6d44ba9b-f6ec-2e49-b930-0c8fe920cb73");

        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyAcceptableAaguids(acceptableAaguids));
    }

    @Test
    public void registerUserSuccess() {
        String username = "registerUserSuccess";
        String email = "registerUserSuccess@email";
        String password = generatePassword();
        String userId;

        updateRealmWithDefaultWebAuthnSettings();

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();

        String authenticatorLabel = SecretGenerator.getInstance().randomString(24);
        registerPage.register("firstName", "lastName", email, username, password);

        // User was registered. Now he needs to register WebAuthn credential
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);

        Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

        // confirm that registration is successfully completed
        userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId();

        EventAssertion.assertSuccess(events.poll()).type(EventType.REGISTER).sessionId(null)
                .userId(userId)
                .clientId(Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app")).toRepresentation().getClientId())
                .details(Details.USERNAME, username)
                .details(Details.EMAIL, email)
                .details(Details.REGISTER_METHOD, "form")
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri());


        EventRepresentation event = events.poll();

        EventAssertion.assertSuccess(event).type(EventType.CUSTOM_REQUIRED_ACTION).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .details(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID);

        String regPubKeyCredentialId1 = event.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

        EventRepresentation event2 = events.poll();

        EventAssertion.assertSuccess(event2).type(EventType.UPDATE_CREDENTIAL).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .details(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID);

        String regPubKeyCredentialId2 = event2.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

        assertThat(regPubKeyCredentialId1, equalTo(regPubKeyCredentialId2));

        // confirm login event
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).hasSessionId().userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel);

        // confirm user registered
        assertUserRegistered(userId, username.toLowerCase(), email.toLowerCase());
        assertRegisteredCredentials(userId, ALL_ZERO_AAGUID, "none");

        events.clear();

        // logout by user
        logout();

        // confirm logout event
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT).hasSessionId().userId(userId)
                .clientId(Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "account")).toRepresentation().getClientId());

        // login by user
        oAuthClient.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();

        assertThat(webAuthnLoginPage.getCount(), is(1));
        assertThat(webAuthnLoginPage.getLabels(), Matchers.contains(authenticatorLabel));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

        // confirm login event
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).hasSessionId().userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, regPubKeyCredentialId2)
                .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, Boolean.FALSE.toString());

        events.clear();
        // logout by user
        logout();

        // confirm logout event
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT).hasSessionId().userId(userId)
                .clientId(Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "account")).toRepresentation().getClientId());
    }

    @Test
    public void webAuthnPasswordlessAlternativeWithWebAuthnAndPassword() {
        String userId;

        final String WEBAUTHN_LABEL = "webauthn";
        final String PASSWORDLESS_LABEL = "passwordless";

        managedRealm.updateWithCleanup(r -> r.browserFlow(webAuthnTogetherPasswordlessFlow()));
        final UserRepresentation cleanupUser = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME);
        managedRealm.cleanup().add(r -> r.users().get(cleanupUser.getId()).update(cleanupUser));

        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME);

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
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(USERNAME);
        loginUsernamePage.submit();

        passwordPage.assertCurrent();
        passwordPage.fillPassword(PASSWORD);
        passwordPage.submit();

        events.clear();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(WEBAUTHN_LABEL);

        webAuthnRegisterPage.assertCurrent();

        EventAssertion.assertSuccess(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL);

        EventAssertion.assertSuccess(events.poll()).type(EventType.UPDATE_CREDENTIAL).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL);

        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(PASSWORDLESS_LABEL);

        Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, PASSWORDLESS_LABEL);

        EventAssertion.assertSuccess(events.poll()).type(EventType.UPDATE_CREDENTIAL).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, PASSWORDLESS_LABEL);

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).hasSessionId().userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);

        events.clear();

        logout();

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT).hasSessionId().userId(userId)
                .clientId(Objects.requireNonNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "account")).toRepresentation().getClientId());

        // Password + WebAuthn Passkey
        oAuthClient.openLoginForm();
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(USERNAME);
        loginUsernamePage.submit();

        passwordPage.assertCurrent();
        passwordPage.fillPassword(PASSWORD);
        passwordPage.submit();

        webAuthnLoginPage.assertCurrent();

        assertThat(webAuthnLoginPage.getCount(), is(1));
        assertThat(webAuthnLoginPage.getLabels(), Matchers.contains(WEBAUTHN_LABEL));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());
        logout();

        // Only passwordless login
        oAuthClient.openLoginForm();
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(USERNAME);
        loginUsernamePage.submit();

        passwordPage.assertCurrent();
        passwordPage.clickTryAnotherWayLink();

        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                is("Use your Passkey for passwordless sign in."));
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);

        webAuthnLoginPage.assertCurrent();
        assertThat(webAuthnLoginPage.getCount(), is(0));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());
        logout();
    }

    @Test
    public void webAuthnPasswordlessShouldFailIfUserIsDeletedInBetween() {
        final String WEBAUTHN_LABEL = "webauthn";
        final String PASSWORDLESS_LABEL = "passwordless";

        managedRealm.updateWithCleanup(r -> r.browserFlow(webAuthnTogetherPasswordlessFlow()));

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
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(username);
        loginUsernamePage.submit();

        passwordPage.assertCurrent();
        passwordPage.fillPassword(password);
        passwordPage.submit();

        events.clear();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(WEBAUTHN_LABEL);

        webAuthnRegisterPage.assertCurrent();

        EventAssertion.assertSuccess(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL);

        EventAssertion.assertSuccess(events.poll()).type(EventType.UPDATE_CREDENTIAL).sessionId(null).userId(userId).isCodeId()
                .details(Details.REDIRECT_URI, testApp.getRedirectionUri())
                .details(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .details(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL);

        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(PASSWORDLESS_LABEL);

        Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

        logout();

        // Password + WebAuthn Passkey
        oAuthClient.openLoginForm();
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(username);
        loginUsernamePage.submit();

        passwordPage.assertCurrent();
        passwordPage.fillPassword(password);
        passwordPage.submit();

        webAuthnLoginPage.assertCurrent();

        assertThat(webAuthnLoginPage.getCount(), is(1));
        assertThat(webAuthnLoginPage.getLabels(), Matchers.contains(WEBAUTHN_LABEL));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());
        logout();

        // Only passwordless login
        oAuthClient.openLoginForm();
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(username);
        loginUsernamePage.submit();

        passwordPage.assertCurrent();
        passwordPage.clickTryAnotherWayLink();

        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                is("Use your Passkey for passwordless sign in."));
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);

        webAuthnLoginPage.assertCurrent();
        assertThat(webAuthnLoginPage.getCount(), is(0));

        // remove testuser before user authenticates via webauthn
        try (Response resp = managedRealm.admin().users().delete(userId)) {
            // ignore
        }

        webAuthnLoginPage.clickAuthenticate();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), is("Unknown user authenticated by the Passkey."));
    }

    @Test
    public void webAuthnTwoFactorAndWebAuthnPasswordlessTogether() {
        // Change binding to browser-webauthn-passwordless. This is flow, which contains both "webauthn" and "webauthn-passwordless" authenticator
        managedRealm.updateWithCleanup(r -> r.browserFlow("browser-webauthn-passwordless"));
        // Login as webauthn-user with password
        oAuthClient.openLoginForm();
        loginPage.fillLogin(USERNAME, PASSWORD);
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

    private void updateRealmWithDefaultWebAuthnSettings() {
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicySignatureAlgorithms(List.of("ES256")));
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyAttestationConveyancePreference("none"));
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyAuthenticatorAttachment("cross-platform"));
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyRequireResidentKey("No"));
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyRpId(null));
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyUserVerificationRequirement("preferred"));
        managedRealm.updateWithCleanup(r -> r.webAuthnPolicyAcceptableAaguids(List.of(ALL_ZERO_AAGUID)));
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
