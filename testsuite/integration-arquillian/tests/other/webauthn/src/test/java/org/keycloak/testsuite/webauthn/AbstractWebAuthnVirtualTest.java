/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.KcVirtualAuthenticator;
import org.keycloak.testsuite.webauthn.authenticators.UseVirtualAuthenticators;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorManager;
import org.keycloak.testsuite.webauthn.pages.WebAuthnErrorPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnRegisterPage;
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.updaters.PasswordLessRealmAttributeUpdater;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.virtualauthenticator.Credential;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import javax.ws.rs.core.Response;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * Abstract class for WebAuthn tests which use Virtual Authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
@AuthServerContainerExclude(REMOTE)
public abstract class AbstractWebAuthnVirtualTest extends AbstractTestRealmKeycloakTest implements UseVirtualAuthenticators {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected WebAuthnRegisterPage webAuthnRegisterPage;

    @Page
    protected WebAuthnErrorPage webAuthnErrorPage;

    @Page
    protected WebAuthnLoginPage webAuthnLoginPage;

    @Page
    protected AppPage appPage;

    protected static final String ALL_ZERO_AAGUID = "00000000-0000-0000-0000-000000000000";
    protected static final String ALL_ONE_AAGUID = "11111111-1111-1111-1111-111111111111";
    protected static final String USERNAME = "UserWebAuthn";
    protected static final String PASSWORD = "password";
    protected static final String EMAIL = "UserWebAuthn@email";

    protected final static String base64EncodedPK =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg8_zMDQDYAxlU-Q"
                    + "hk1Dwkf0v18GZca1DMF3SaJ9HPdmShRANCAASNYX5lyVCOZLzFZzrIKmeZ2jwU"
                    + "RmgsJYxGP__fWN_S-j5sN4tT15XEpN_7QZnt14YvI6uvAgO0uJEboFaZlOEB";

    protected final static PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(Base64.getUrlDecoder().decode(base64EncodedPK));

    private VirtualAuthenticatorManager virtualAuthenticatorManager;

    @Before
    @Override
    public void setUpVirtualAuthenticator() {
        this.virtualAuthenticatorManager = createDefaultVirtualManager(driver, getDefaultAuthenticatorOptions());
        clearEventQueue();
    }

    @After
    @Override
    public void removeVirtualAuthenticator() {
        virtualAuthenticatorManager.removeAuthenticator();
        clearEventQueue();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        if (isPasswordless()) {
            makePasswordlessRequiredActionDefault(realmRepresentation);
            switchExecutionInBrowserFormToPasswordless(realmRepresentation);
        }

        testRealms.add(realmRepresentation);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Override
    protected void postAfterAbstractKeycloak() {
        List<UserRepresentation> defaultUser = testRealm().users().search(USERNAME, true);
        if (defaultUser != null && !defaultUser.isEmpty()) {
            Response response = testRealm().users().delete(defaultUser.get(0).getId());
            assertThat(response, notNullValue());
            assertThat(response.getStatus(), is(204));
        }
    }

    public UserResource userResource() {
        return ApiUtil.findUserByUsernameId(testRealm(), USERNAME);
    }

    public VirtualAuthenticatorOptions getDefaultAuthenticatorOptions() {
        return DefaultVirtualAuthOptions.DEFAULT.getOptions();
    }

    public VirtualAuthenticatorManager getVirtualAuthManager() {
        return virtualAuthenticatorManager;
    }

    public void setVirtualAuthManager(VirtualAuthenticatorManager manager) {
        this.virtualAuthenticatorManager = manager;
    }

    public AbstractWebAuthnRealmUpdater<?> getWebAuthnRealmUpdater() {
        return isPasswordless() ? new PasswordLessRealmAttributeUpdater(testRealm()) : new WebAuthnRealmAttributeUpdater(testRealm());
    }

    public String getCredentialType() {
        return isPasswordless() ? WebAuthnCredentialModel.TYPE_PASSWORDLESS : WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }

    public boolean isPasswordless() {
        return false;
    }

    protected void clearEventQueue() {
        getTestingClient().testing().clearEventQueue();
    }

    public static VirtualAuthenticatorManager createDefaultVirtualManager(WebDriver webDriver, VirtualAuthenticatorOptions options) {
        VirtualAuthenticatorManager manager = new VirtualAuthenticatorManager(webDriver);
        manager.useAuthenticator(options);
        return manager;
    }

    // Registration

    protected void registerDefaultUser() {
        registerDefaultUser(true);
    }

    protected void registerDefaultUser(boolean shouldSuccess) {
        registerDefaultUser(SecretGenerator.getInstance().randomString(24), shouldSuccess);
    }

    protected void registerDefaultUser(String authenticatorLabel) {
        registerDefaultUser(authenticatorLabel, true);
    }

    private void registerDefaultUser(String authenticatorLabel, boolean shouldSuccess) {
        registerUser(USERNAME, PASSWORD, EMAIL, authenticatorLabel, shouldSuccess);
    }

    protected void registerUser(String username, String password, String email, String authenticatorLabel, boolean shouldSuccess) {
        loginPage.open();
        loginPage.clickRegister();

        waitForPageToLoad();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email, username, password, password);

        // User was registered. Now he needs to register WebAuthn credential
        waitForPageToLoad();
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        if (shouldSuccess) {
            events.clear();
            tryRegisterAuthenticator(authenticatorLabel);
        }
    }

    private void tryRegisterAuthenticator(String authenticatorLabel) {
        tryRegisterAuthenticator(authenticatorLabel, 10);
    }

    /**
     * Helper method for registering Security Key
     * Sometimes, it's not possible to register the key, when the Resident Key is required
     * It seems it's related to Virtual authenticators provided by Selenium framework
     * Manual testing with Google Chrome authenticators works as expected
     */
    private void tryRegisterAuthenticator(String authenticatorLabel, int numberOfAllowedRetries) {
        final boolean hasResidentKey = Optional.ofNullable(getVirtualAuthManager().getCurrent())
                .map(KcVirtualAuthenticator::getOptions)
                .map(KcVirtualAuthenticator.Options::hasResidentKey)
                .orElse(false);

        if (hasResidentKey && !webAuthnRegisterPage.isRegisterAlertPresent()) {
            for (int i = 0; i < numberOfAllowedRetries; i++) {
                events.clear();
                webAuthnErrorPage.clickTryAgain();
                waitForPageToLoad();
                webAuthnRegisterPage.assertCurrent();
                webAuthnRegisterPage.clickRegister();

                if (webAuthnRegisterPage.isRegisterAlertPresent()) {
                    webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);
                    return;
                } else {
                    WaitUtils.pause(200);
                }
            }
        } else {
            webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);
        }
    }

    // Authentication

    protected void authenticateDefaultUser() {
        authenticateDefaultUser(true);
    }

    protected void authenticateDefaultUser(boolean shouldSuccess) {
        authenticateUser(USERNAME, PASSWORD, shouldSuccess);
    }

    protected void authenticateUser(String username, String password, boolean shouldSuccess) {
        loginPage.open();
        loginPage.assertCurrent(TEST_REALM_NAME);
        loginPage.login(username, password);

        waitForPageToLoad();

        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();

        if (shouldSuccess) {
            waitForPageToLoad();
            appPage.assertCurrent();
        } else {
            displayErrorMessageIfPresent();
        }
    }

    protected String displayErrorMessageIfPresent() {
        if (webAuthnErrorPage.isCurrent()) {
            final String msg = webAuthnErrorPage.getError();
            log.info("Error message from Error Page: " + msg);
            return msg;
        }
        return null;
    }

    protected Credential getDefaultResidentKeyCredential() {
        byte[] credentialId = {1, 2, 3, 4};
        byte[] userHandle = {1};
        return Credential.createResidentCredential(credentialId, "localhost", privateKey, userHandle, 0);
    }

    protected Credential getDefaultNonResidentKeyCredential() {
        byte[] credentialId = {1, 2, 3, 4};
        return Credential.createNonResidentCredential(credentialId, "localhost", privateKey, 0);
    }

    protected static void makePasswordlessRequiredActionDefault(RealmRepresentation realm) {
        RequiredActionProviderRepresentation webAuthnProvider = realm.getRequiredActions()
                .stream()
                .filter(f -> f.getProviderId().equals(WebAuthnRegisterFactory.PROVIDER_ID))
                .findFirst()
                .orElse(null);
        assertThat(webAuthnProvider, notNullValue());

        webAuthnProvider.setEnabled(false);

        RequiredActionProviderRepresentation webAuthnPasswordlessProvider = realm.getRequiredActions()
                .stream()
                .filter(f -> f.getProviderId().equals(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID))
                .findFirst()
                .orElse(null);
        assertThat(webAuthnPasswordlessProvider, notNullValue());

        webAuthnPasswordlessProvider.setEnabled(true);
        webAuthnPasswordlessProvider.setDefaultAction(true);
    }

    // Switch WebAuthn authenticator with Passwordless authenticator in browser flow
    protected void switchExecutionInBrowserFormToPasswordless(RealmRepresentation realm) {
        List<AuthenticationFlowRepresentation> flows = realm.getAuthenticationFlows();
        assertThat(flows, notNullValue());

        AuthenticationFlowRepresentation browserForm = flows.stream()
                .filter(f -> f.getAlias().equals("browser-webauthn-forms"))
                .findFirst()
                .orElse(null);
        assertThat("Cannot find 'browser-webauthn-forms' flow", browserForm, notNullValue());

        flows.removeIf(f -> f.getAlias().equals(browserForm.getAlias()));

        List<AuthenticationExecutionExportRepresentation> browserFormExecutions = browserForm.getAuthenticationExecutions();
        assertThat("Flow 'browser-webauthn-forms' doesn't have any executions", browserForm, notNullValue());

        AuthenticationExecutionExportRepresentation webAuthn = browserFormExecutions.stream()
                .filter(f -> WebAuthnAuthenticatorFactory.PROVIDER_ID.equals(f.getAuthenticator()))
                .findFirst()
                .orElse(null);
        assertThat("Cannot find WebAuthn execution in Browser flow", webAuthn, notNullValue());

        browserFormExecutions.removeIf(f -> webAuthn.getAuthenticator().equals(f.getAuthenticator()));
        webAuthn.setAuthenticator(WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID);
        browserFormExecutions.add(webAuthn);
        browserForm.setAuthenticationExecutions(browserFormExecutions);
        flows.add(browserForm);

        realm.setAuthenticationFlows(flows);
    }

    protected void logout() {
        try {
            appPage.open();
            appPage.assertCurrent();
            appPage.logout();
        } catch (Exception e) {
            throw new RuntimeException("Cannot logout user", e);
        }
    }
}
