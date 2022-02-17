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
package org.keycloak.testsuite.webauthn;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
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
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.pages.SelectAuthenticatorPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.events.EventType.CUSTOM_REQUIRED_ACTION;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

public class WebAuthnRegisterAndLoginTest extends AbstractWebAuthnVirtualTest {

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernamePage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected SelectAuthenticatorPage selectAuthenticatorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        List<String> acceptableAaguids = new ArrayList<>();
        acceptableAaguids.add("00000000-0000-0000-0000-000000000000");
        acceptableAaguids.add("6d44ba9b-f6ec-2e49-b930-0c8fe920cb73");

        realmRepresentation.setWebAuthnPolicyAcceptableAaguids(acceptableAaguids);

        testRealms.add(realmRepresentation);
    }

    @Test
    public void registerUserSuccess() throws IOException {
        String username = "registerUserSuccess";
        String password = "password";
        String email = "registerUserSuccess@email";
        String userId = null;

        try (RealmAttributeUpdater rau = updateRealmWithDefaultWebAuthnSettings(testRealm()).update()) {

            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            String authenticatorLabel = SecretGenerator.getInstance().randomString(24);
            registerPage.register("firstName", "lastName", email, username, password, password);

            // User was registered. Now he needs to register WebAuthn credential
            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);

            appPage.assertCurrent();
            assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
            appPage.openAccount();

            // confirm that registration is successfully completed
            userId = events.expectRegister(username, email).assertEvent().getUserId();
            // confirm registration event
            EventRepresentation eventRep = events.expectRequiredAction(CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                    .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID)
                    .assertEvent();
            String regPubKeyCredentialId = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

            // confirm login event
            String sessionId = events.expectLogin()
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                    .assertEvent().getSessionId();
            // confirm user registered
            assertUserRegistered(userId, username.toLowerCase(), email.toLowerCase());
            assertRegisteredCredentials(userId, ALL_ZERO_AAGUID, "none");

            events.clear();

            // logout by user
            appPage.logout();
            // confirm logout event
            events.expectLogout(sessionId)
                    .user(userId)
                    .assertEvent();

            // login by user
            loginPage.open();
            loginPage.login(username, password);

            webAuthnLoginPage.assertCurrent();

            final WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators.getCount(), is(1));
            assertThat(authenticators.getLabels(), Matchers.contains(authenticatorLabel));

            webAuthnLoginPage.clickAuthenticate();

            appPage.assertCurrent();
            assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
            appPage.openAccount();

            // confirm login event
            sessionId = events.expectLogin()
                    .user(userId)
                    .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, regPubKeyCredentialId)
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, Boolean.FALSE.toString())
                    .assertEvent().getSessionId();

            events.clear();
            // logout by user
            appPage.logout();
            // confirm logout event
            events.expectLogout(sessionId)
                    .user(userId)
                    .assertEvent();
        } finally {
            removeFirstCredentialForUser(userId, WebAuthnCredentialModel.TYPE_TWOFACTOR);
        }
    }

    @Test
    public void webAuthnPasswordlessAlternativeWithWebAuthnAndPassword() throws IOException {
        String userId = null;

        final String WEBAUTHN_LABEL = "webauthn";
        final String PASSWORDLESS_LABEL = "passwordless";

        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm())
                .setBrowserFlow(webAuthnTogetherPasswordlessFlow())
                .update()) {

            UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost");
            assertThat(user, notNullValue());
            user.getRequiredActions().add(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);

            UserResource userResource = testRealm().users().get(user.getId());
            assertThat(userResource, notNullValue());
            userResource.update(user);

            user = userResource.toRepresentation();
            assertThat(user, notNullValue());
            assertThat(user.getRequiredActions(), hasItem(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID));

            userId = user.getId();

            loginUsernamePage.open();
            loginUsernamePage.login("test-user@localhost");

            passwordPage.assertCurrent();
            passwordPage.login("password");

            events.clear();

            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.registerWebAuthnCredential(PASSWORDLESS_LABEL);

            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.registerWebAuthnCredential(WEBAUTHN_LABEL);

            appPage.assertCurrent();

            events.expectRequiredAction(CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, PASSWORDLESS_LABEL)
                    .assertEvent();

            events.expectRequiredAction(CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL)
                    .assertEvent();

            final String sessionID = events.expectLogin()
                    .user(userId)
                    .assertEvent()
                    .getSessionId();

            events.clear();

            appPage.logout();

            events.expectLogout(sessionID)
                    .user(userId)
                    .assertEvent();

            // Password + WebAuthn security key
            loginUsernamePage.open();
            loginUsernamePage.assertCurrent();
            loginUsernamePage.login("test-user@localhost");

            passwordPage.assertCurrent();
            passwordPage.login("password");

            webAuthnLoginPage.assertCurrent();

            final WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators.getCount(), is(1));
            assertThat(authenticators.getLabels(), Matchers.contains(WEBAUTHN_LABEL));

            webAuthnLoginPage.clickAuthenticate();

            appPage.assertCurrent();
            appPage.logout();

            // Only passwordless login
            loginUsernamePage.open();
            loginUsernamePage.login("test-user@localhost");

            passwordPage.assertCurrent();
            passwordPage.assertTryAnotherWayLinkAvailability(true);
            passwordPage.clickTryAnotherWayLink();

            selectAuthenticatorPage.assertCurrent();
            assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                    is("Use your security key for passwordless sign in."));
            selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);

            webAuthnLoginPage.assertCurrent();
            assertThat(webAuthnLoginPage.getAuthenticators().getCount(), is(0));

            webAuthnLoginPage.clickAuthenticate();

            appPage.assertCurrent();
            appPage.logout();
        } finally {
            removeFirstCredentialForUser(userId, WebAuthnCredentialModel.TYPE_TWOFACTOR, WEBAUTHN_LABEL);
            removeFirstCredentialForUser(userId, WebAuthnCredentialModel.TYPE_PASSWORDLESS, PASSWORDLESS_LABEL);
        }
    }

    @Test
    public void webAuthnTwoFactorAndWebAuthnPasswordlessTogether() throws IOException {
        // Change binding to browser-webauthn-passwordless. This is flow, which contains both "webauthn" and "webauthn-passwordless" authenticator
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm()).setBrowserFlow("browser-webauthn-passwordless").update()) {
            // Login as test-user@localhost with password
            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            errorPage.assertCurrent();

            // User is not allowed to register passwordless authenticator in this flow
            assertThat(events.poll().getError(), is("invalid_user_credentials"));
            assertThat(errorPage.getError(), is("Cannot login, credential setup required."));
        }
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
                    Assert.fail();
                }
            }
        });
    }

    protected UserRepresentation getUser(String userId) {
        return testRealm().users().get(userId).toRepresentation();
    }

    protected List<CredentialRepresentation> getCredentials(String userId) {
        return testRealm().users().get(userId).credentials();
    }

    private static WebAuthnRealmAttributeUpdater updateRealmWithDefaultWebAuthnSettings(RealmResource resource) {
        return new WebAuthnRealmAttributeUpdater(resource)
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
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server(TEST_REALM_NAME).run(session -> {
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

    private void removeFirstCredentialForUser(String userId, String credentialType) {
        removeFirstCredentialForUser(userId, credentialType, null);
    }

    /**
     * Remove first occurring credential from user with specific credentialType
     *
     * @param userId          userId
     * @param credentialType  type of credential
     * @param assertUserLabel user label of credential
     */
    private void removeFirstCredentialForUser(String userId, String credentialType, String assertUserLabel) {
        if (userId == null || credentialType == null) return;

        final UserResource userResource = testRealm().users().get(userId);

        final CredentialRepresentation credentialRep = userResource.credentials()
                .stream()
                .filter(credential -> credentialType.equals(credential.getType()))
                .findFirst().orElse(null);

        assertThat(credentialRep, notNullValue());
        if (assertUserLabel != null) {
            assertThat(credentialRep.getUserLabel(), is(assertUserLabel));
        }
        userResource.removeCredential(credentialRep.getId());
    }
}
