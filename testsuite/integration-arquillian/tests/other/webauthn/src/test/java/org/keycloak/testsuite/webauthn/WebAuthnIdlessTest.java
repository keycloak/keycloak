/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.*;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnRegisterPage;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.WebAuthnConstants.OPTION_DISCOURAGED;
import static org.keycloak.WebAuthnConstants.OPTION_REQUIRED;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;
import static org.keycloak.testsuite.webauthn.utils.PropertyRequirement.NO;
import static org.keycloak.testsuite.webauthn.utils.PropertyRequirement.YES;
import static org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Protocol;
import static org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Transport;

@IgnoreBrowserDriver(FirefoxDriver.class)
public class WebAuthnIdlessTest extends AbstractWebAuthnVirtualTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected WebAuthnLoginPage webAuthnLoginPage;

    @Page
    protected WebAuthnRegisterPage webAuthnRegisterPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernamePage;

    @Page
    protected SelectAuthenticatorPage selectAuthenticatorPage;

    private static final Logger logger = Logger.getLogger(WebAuthnIdlessTest.class);

    protected final static String username = "test-user@localhost";
    protected final static String password = "password";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        testRealms.add(realmRepresentation);
    }

    // Register webauthn-passwordless credential (resident key)
    // Authenticate IDLess (resident key)
    @Test
    public void testWebAuthnIDLessLogin() throws IOException {

        configureUser(username, false, true, true);
        initializeAuthenticator(true, true, true, true);
        setWebAuthnRealmSettings(false, false, true, true);

        // Trigger webauthn-passwordless setup (resident key)
        setUpUsernamePasswordFlow("username-password-flow");
        String credentialId = usernamePasswordAuthWithAuthSetup(username, true, true);

        setUpIDLessOnlyFlow("idless-only-flow");
        idlessAuthentication(username, credentialId, false, true);

    }

    // Register webauthn-passwordless credential (non-resident key)
    // Authenticate IDLess (non-resident key): should fail
    @Test
    public void testWebAuthnIDLessWithNonResidentCredentialLogin() throws IOException {

        configureUser(username, false, true, true);
        initializeAuthenticator(false, true, true, true);
        setWebAuthnRealmSettings(false, false, false, true);

        // Trigger webauthn-passwordless (non resident key setup)
        setUpUsernamePasswordFlow("username-password-flow");
        String credentialId = usernamePasswordAuthWithAuthSetup(username, true, false);

        setUpIDLessOnlyFlow("idless-only-flow");
        idlessAuthentication(username, credentialId, false, false);

    }

    // Authenticate IDLess with no webauthn-passwordless credential registered: should fail
    @Test
    public void testWebAuthnIDLessWithNoWebAuthnPasswordlessCredentialLogin() throws IOException {

        configureUser(username, true, true, true);
        initializeAuthenticator(false, true, true, true);
        setWebAuthnRealmSettings(false, false, true, true);

        setUpIDLessOnlyFlow("idless-only-flow");
        idlessAuthentication(username, null, false, false);

    }



    // Register webauthn-passwordless credential (resident key)
    // Register webauthn credential (non resident key)
    // Assert 'Try another way' with security key on first step (before any form input)
    // Authenticate UsernamePassword + WebAuthn (non resident key)
    // Authenticate Username + WebAuthnPasswordless (resident key)
    // Authenticate IDLess (resident key)
    @Test
    public void testWebAuthnIDLessAndWebAuthnAndWebAuthnPasswordlessLogin() throws IOException {

        initializeAuthenticator(true, true, true, true);
        setWebAuthnRealmSettings(false, false, true, true);

        // Trigger webauthn-passwordless (resident key) setup
        configureUser(username, false, true, true);
        setUpUsernamePasswordFlow("username-password-flow");
        String webAuthnPasswordlessCredId = usernamePasswordAuthWithAuthSetup(username, true, true);

        // Trigger webauthn (non resident key) setup
        configureUser(username, true, false, false);
        setUpUsernamePasswordFlow("username-password-flow");
        String webAuthnCredId = usernamePasswordAuthWithAuthSetup(username, false, false);

        setUpIDLessAndWebAuthnAndPasswordlessFlow("webauthn-webauthnpasswordless-idless");

        // Check tryAnotherWay link on first step page
        checkTryAnotherWay();
        // UsernamePasswordForm + WebAuthn
        usernamePasswordAndWebAuthnAuthentication(username, webAuthnCredId);
        // UsernameForm + WebAuthnPasswordless
        usernameAndWebAuthnPasswordlessAuthentication(username, webAuthnPasswordlessCredId);
        // WebAuthnIDLess
        idlessAuthentication(username, webAuthnPasswordlessCredId, true, true);

    }


    protected String usernamePasswordAuthWithAuthSetup(String username, boolean isPasswordless, boolean withResidentKey) {

        String raProviderID = isPasswordless ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID :
                WebAuthnRegisterFactory.PROVIDER_ID;
        String credType = isPasswordless ? WebAuthnCredentialModel.TYPE_PASSWORDLESS: WebAuthnCredentialModel.TYPE_TWOFACTOR;
        String userId = getUserRepresentation(username).getId();
        UserResource userRes = testRealm().users().get(userId);

        assertThat(userRes.credentials().stream().filter(cred ->
                cred.getType().equals(credType)).collect(Collectors.toList()).size(), is(0));

        assertThat(getVirtualAuthManager().getCurrent().getAuthenticator().getCredentials().stream().filter(cred ->
                cred.isResidentCredential() == isPasswordless).collect(Collectors.toList()).size(), is(0));

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.login(username, password);

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        String labelPrefix = isPasswordless ? "wapl-" : "wa-";
        String authenticatorLabel = labelPrefix + SecretGenerator.getInstance().randomString(24);
        webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);

        appPage.assertCurrent();
        assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
        EventRepresentation eventRep = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, raProviderID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID)
                .assertEvent();
        String credentialId = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

        assertThat(userRes.credentials().stream()
                .filter(cred -> cred.getType().equals(credType))
                .filter(cred -> cred.getUserLabel().equals(authenticatorLabel))
                .collect(Collectors.toList()).size(), is(1));
        assertThat(getVirtualAuthManager().getCurrent().getAuthenticator().getCredentials().stream()
                .filter(cred -> cred.isResidentCredential() == withResidentKey)
                .collect(Collectors.toList()).size(), is(1));
        if (withResidentKey) {
            assertThat(getVirtualAuthManager().getCurrent().getAuthenticator().getCredentials().stream()
                    .filter(cred -> cred.isResidentCredential())
                    .filter(cred -> (new String(cred.getUserHandle())).equals(userId))
                    .collect(Collectors.toList()).size(), is(1));
        }

        String sessionId = events.expectLogin()
                .user(userId)
                .assertEvent().getSessionId();
        events.clear();
        logout();
        events.expectLogout(sessionId)
                .removeDetail(Details.REDIRECT_URI)
                .user(userId)
                .assertEvent();
        return credentialId;
    }

    protected void checkTryAnotherWay() {

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.assertTryAnotherWayLinkAvailability(true);
        loginPage.clickTryAnotherWayLink();
        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.USERNAMEPASSWORD),
                is("Sign in by entering your username and password."));
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.USERNAME),
                is("Start sign in by entering your username"));
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                is("Use your security key for passwordless sign in."));
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.USERNAMEPASSWORD);
        loginPage.assertCurrent();
        loginPage.clickTryAnotherWayLink();
        selectAuthenticatorPage.assertCurrent();
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.USERNAME);
        loginUsernamePage.assertCurrent();
        loginUsernamePage.clickTryAnotherWayLink();
        selectAuthenticatorPage.assertCurrent();

    }


    protected void usernamePasswordAndWebAuthnAuthentication(String username, String credentialId) {

        String userId = getUserRepresentation(username).getId();

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.assertTryAnotherWayLinkAvailability(true);
        loginPage.clickTryAnotherWayLink();
        selectAuthenticatorPage.assertCurrent();
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.USERNAMEPASSWORD);
        loginPage.assertCurrent();
        loginPage.login(username, password);
        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();
        appPage.assertCurrent();

        String sessionId = events.expectLogin()
                .user(userId)
                .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, credentialId)
                .detail("web_authn_authenticator_user_verification_checked", Boolean.FALSE.toString())
                .assertEvent().getSessionId();

        events.clear();
        logout();
        events.expectLogout(sessionId)
                .removeDetail(Details.REDIRECT_URI)
                .user(userId)
                .assertEvent();
    }

    protected void usernameAndWebAuthnPasswordlessAuthentication(String username, String credentialId) {

        String userId = getUserRepresentation(username).getId();

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.assertTryAnotherWayLinkAvailability(true);
        loginPage.clickTryAnotherWayLink();
        selectAuthenticatorPage.assertCurrent();
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.USERNAME);
        loginUsernamePage.assertCurrent();
        loginUsernamePage.login(username);
        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();
        appPage.assertCurrent();

        String sessionId = events.expectLogin()
                .user(userId)
                .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, credentialId)
                .detail("web_authn_authenticator_user_verification_checked", Boolean.TRUE.toString())
                .assertEvent().getSessionId();

        events.clear();
        logout();
        events.expectLogout(sessionId)
                .removeDetail(Details.REDIRECT_URI)
                .user(userId)
                .assertEvent();
    }

    protected void idlessAuthentication(String username, String credentialId, boolean tryAnotherMethod, boolean shouldSuccess) {

        String userId = getUserRepresentation(username).getId();

        loginPage.open();
        loginPage.assertCurrent();
        if (tryAnotherMethod) {
            loginPage.assertTryAnotherWayLinkAvailability(true);
            loginPage.clickTryAnotherWayLink();
            selectAuthenticatorPage.assertCurrent();
            selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);
        }
        webAuthnLoginPage.assertCurrent();

        webAuthnLoginPage.clickAuthenticate();

        if (shouldSuccess) {
            appPage.assertCurrent();

            String sessionId = events.expectLogin()
                    .user(userId)
                    .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, credentialId)
                    .detail("web_authn_authenticator_user_verification_checked", Boolean.TRUE.toString())
                    .assertEvent().getSessionId();

            events.clear();
            logout();
            events.expectLogout(sessionId)
                    .removeDetail(Details.REDIRECT_URI)
                    .user(userId)
                    .assertEvent();
        }
        else {
            loginPage.assertCurrent();
            assertThat(loginPage.getError(), containsString("Failed to authenticate by the Security key."));
        }
    }


    protected void setWebAuthnRealmSettings(boolean waRequireRK, boolean waRequireUV, boolean waplRequireRK, boolean waplRequireUV ) {

        String waRequireRKString = waRequireRK ? YES.getValue() : NO.getValue();
        String waRequireUVString = waRequireUV ? OPTION_REQUIRED : OPTION_DISCOURAGED;
        String waplRequireRKString = waplRequireRK ? YES.getValue() : NO.getValue();
        String waplRequireUVString = waplRequireUV ? OPTION_REQUIRED : OPTION_DISCOURAGED;

        RealmRepresentation realmRep = testRealm().toRepresentation();

        realmRep.setWebAuthnPolicyPasswordlessRequireResidentKey(waplRequireRKString);
        realmRep.setWebAuthnPolicyPasswordlessUserVerificationRequirement(waplRequireUVString);
        realmRep.setWebAuthnPolicyPasswordlessRpEntityName("localhost");
        realmRep.setWebAuthnPolicyPasswordlessRpId("localhost");

        realmRep.setWebAuthnPolicyRequireResidentKey(waRequireRKString);
        realmRep.setWebAuthnPolicyUserVerificationRequirement(waRequireUVString);
        realmRep.setWebAuthnPolicyRpEntityName("localhost");
        realmRep.setWebAuthnPolicyRpId("localhost");

        testRealm().update(realmRep);
        realmRep = testRealm().toRepresentation();

        assertThat(realmRep.getWebAuthnPolicyPasswordlessRequireResidentKey(), containsString(waplRequireRKString));
        assertThat(realmRep.getWebAuthnPolicyPasswordlessUserVerificationRequirement(), containsString(waplRequireUVString));
        assertThat(realmRep.getWebAuthnPolicyPasswordlessRpEntityName(), containsString("localhost"));
        assertThat(realmRep.getWebAuthnPolicyPasswordlessRpId(), containsString("localhost"));

        assertThat(realmRep.getWebAuthnPolicyRequireResidentKey(), containsString(waRequireRKString));
        assertThat(realmRep.getWebAuthnPolicyUserVerificationRequirement(), containsString(waRequireUVString));
        assertThat(realmRep.getWebAuthnPolicyRpEntityName(), containsString("localhost"));
        assertThat(realmRep.getWebAuthnPolicyRpId(), containsString("localhost"));
    }

    protected void initializeAuthenticator(boolean hasRK, boolean hasUV, boolean isVerified, boolean isConsenting) {

        getVirtualAuthManager().removeAuthenticator();
        getVirtualAuthManager().useAuthenticator(getDefaultAuthenticatorOptions()
                .setHasResidentKey(hasRK)
                .setHasUserVerification(hasUV)
                .setIsUserVerified(isVerified)
                .setIsUserConsenting(isConsenting)
                .setTransport(Transport.USB)
                .setProtocol(Protocol.CTAP2));

        getVirtualAuthManager().getCurrent().getAuthenticator().removeAllCredentials();

        assertThat(getVirtualAuthManager().getCurrent().getOptions().hasResidentKey(), is(hasRK));
        assertThat(getVirtualAuthManager().getCurrent().getOptions().isUserConsenting(), is(isConsenting));
        assertThat(getVirtualAuthManager().getCurrent().getOptions().isUserVerified(), is(isVerified));
        assertThat(getVirtualAuthManager().getCurrent().getOptions().hasUserVerification(), is(hasUV));
        assertThat(getVirtualAuthManager().getCurrent().getOptions().getProtocol(), is(Protocol.CTAP2));
        assertThat(getVirtualAuthManager().getCurrent().getOptions().getTransport(), is(Transport.USB));
        assertThat(getVirtualAuthManager().getCurrent().getAuthenticator().getCredentials().size(), is(0));

    }

    protected UserRepresentation getUserRepresentation(String username)
    {
        if (username != null)
            return ApiUtil.findUserByUsername(testRealm(), username);
        else
            return null;
    }

    protected void configureUser(String username, boolean registerWA, boolean registerWAPL, boolean resetCred) {

        UserRepresentation user = getUserRepresentation(username);
        assertThat(user, notNullValue());

        // Clear existing required actions
        user.getRequiredActions().clear();

        if (registerWAPL) {
            user.getRequiredActions().add(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        }
        if (registerWA) {
            user.getRequiredActions().add(WebAuthnRegisterFactory.PROVIDER_ID);
        }

        UserResource userResource = testRealm().users().get(user.getId());
        assertThat(userResource, notNullValue());
        userResource.update(user);

        if (resetCred) {
            // Remove existing webauthn credentials
            Predicate<CredentialRepresentation> isWebAuthnPasswordless = item -> item.getType().equals(WebAuthnCredentialModel.TYPE_PASSWORDLESS);
            Predicate<CredentialRepresentation> isWebAuthn = item -> item.getType().equals(WebAuthnCredentialModel.TYPE_TWOFACTOR);
            userResource.credentials().stream()
                    .filter(isWebAuthnPasswordless.or(isWebAuthn))
                    .forEach(item -> userResource.removeCredential(item.getId()));
            // User should only have password credential set at this stage
            assertThat(userResource.credentials().size(), is(1));
            assertThat(userResource.credentials().get(0).getType(), is(CredentialRepresentation.PASSWORD));
        }

        user = userResource.toRepresentation();
        assertThat(user, notNullValue());
        if (registerWA)
            assertThat(user.getRequiredActions(), hasItem(WebAuthnRegisterFactory.PROVIDER_ID));
        if (registerWAPL)
            assertThat(user.getRequiredActions(), hasItem(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID));

    }

    /* Set auth flow to:
        UsernamePasswordForm + WebAuthn (ALTERNATIVE)
        UsernameForm + WebAuthnPasswordless (ALTERNATIVE)
        IDLess (ALTERNATIVE)
     */
    private void setUpIDLessAndWebAuthnAndPasswordlessFlow(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addSubFlowExecution(ALTERNATIVE, subFlow -> subFlow
                                .addAuthenticatorExecution(REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                                .addAuthenticatorExecution(REQUIRED, WebAuthnAuthenticatorFactory.PROVIDER_ID))
                        .addSubFlowExecution(ALTERNATIVE, subFlow -> subFlow
                                .addAuthenticatorExecution(REQUIRED, UsernameFormFactory.PROVIDER_ID)
                                .addAuthenticatorExecution(REQUIRED, WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID))
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow() // Activate this new flow
        );
    }

    private void setUpIDLessOnlyFlow(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(REQUIRED, WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow() // Activate this new flow
        );
    }

    private void setUpUsernamePasswordFlow(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow() // Activate this new flow
        );
    }

}
