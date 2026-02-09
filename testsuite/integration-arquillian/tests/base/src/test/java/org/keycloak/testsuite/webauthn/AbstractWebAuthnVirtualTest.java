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

import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
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
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.Credential;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverFirefox;
import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverInstanceOf;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Abstract class for WebAuthn tests which use Virtual Authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public abstract class AbstractWebAuthnVirtualTest extends AbstractChangeImportedUserPasswordsTest implements UseVirtualAuthenticators {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

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

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
    protected InfoPage infoPage;

    protected static final String ALL_ZERO_AAGUID = "00000000-0000-0000-0000-000000000000";
    protected static final String ALL_ONE_AAGUID = "11111111-1111-1111-1111-111111111111";
    protected static final String USERNAME = "UserWebAuthn";
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
        if (!isDriverFirefox(driver)) {
            this.virtualAuthenticatorManager = createDefaultVirtualManager(driver, getDefaultAuthenticatorOptions());
        }
        clearEventQueue();
    }

    @After
    @Override
    public void removeVirtualAuthenticator() {
        if (!isDriverFirefox(driver)) {
            virtualAuthenticatorManager.removeAuthenticator();
        }
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
        configureTestRealm(realmRepresentation);
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

    // Warning: The virtual authenticator manager is not initialized for Firefox Browser !!
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
        registerUser(USERNAME, generatePassword(USERNAME), EMAIL, authenticatorLabel, shouldSuccess);
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

        waitForPageToLoad();
    }

    private void tryRegisterAuthenticator(String authenticatorLabel) {
        tryRegisterAuthenticator(authenticatorLabel, 10);
    }

    /**
     * Helper method for registering Passkey
     * Sometimes, it's not possible to register the key, when the Resident Key is required
     * It seems it's related to Virtual authenticators provided by Selenium framework
     * Manual testing with Google Chrome authenticators works as expected
     */
    private void tryRegisterAuthenticator(String authenticatorLabel, int numberOfAllowedRetries) {
        final boolean hasResidentKey = Optional.ofNullable(getVirtualAuthManager())
                .map(VirtualAuthenticatorManager::getCurrent)
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
        authenticateUser(USERNAME, getPassword(USERNAME), shouldSuccess);
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

    /**
     * Changes the flow "browser-webauthn-forms" to use the passed authenticator as required.
     * @param realm The realm representation
     * @param providerId The provider Id to set as required
     */
    protected void switchExecutionInBrowserFormToProvider(RealmRepresentation realm, String providerId) {
        List<AuthenticationFlowRepresentation> flows = realm.getAuthenticationFlows();
        assertThat(flows, notNullValue());

        AuthenticationFlowRepresentation browserForm = flows.stream()
                .filter(f -> f.getAlias().equals("browser-webauthn-forms"))
                .findFirst()
                .orElse(null);
        assertThat("Cannot find 'browser-webauthn-forms' flow", browserForm, notNullValue());

        flows.removeIf(f -> f.getAlias().equals(browserForm.getAlias()));

        // set just one authenticator with the passkeys conditional UI
        AuthenticationExecutionExportRepresentation passkeysConditionalUI = new AuthenticationExecutionExportRepresentation();
        passkeysConditionalUI.setAuthenticator(providerId);
        passkeysConditionalUI.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        passkeysConditionalUI.setPriority(10);
        passkeysConditionalUI.setAuthenticatorFlow(false);
        passkeysConditionalUI.setUserSetupAllowed(false);

        browserForm.setAuthenticationExecutions(List.of(passkeysConditionalUI));
        flows.add(browserForm);

        realm.setAuthenticationFlows(flows);
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
            waitForPageToLoad();
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();
            infoPage.assertCurrent();
            assertEquals("You are logged out", infoPage.getInfo());
        } catch (Exception e) {
            throw new RuntimeException("Cannot logout user", e);
        }
    }

    protected String getExpectedMessageByDriver(Map<Class<? extends WebDriver>, String> values) {
        if (values == null || values.isEmpty()) return "";

        return values.entrySet()
                .stream()
                .filter(Objects::nonNull)
                .filter(f -> isDriverInstanceOf(driver, f.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse("");
    }

    protected String getExpectedMessageByDriver(String firefoxMessage, String chromeMessage) {
        final Map<Class<? extends WebDriver>, String> map = new HashMap<>();
        map.put(FirefoxDriver.class, firefoxMessage);
        map.put(ChromeDriver.class, chromeMessage);

        return getExpectedMessageByDriver(map);
    }

    protected void checkWebAuthnConfiguration(String residentKey, String userVerification) {
        WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
        assertThat(realmData, notNullValue());
        assertThat(realmData.getRpEntityName(), is("localhost"));
        assertThat(realmData.getRequireResidentKey(), is(residentKey));
        assertThat(realmData.getUserVerificationRequirement(), is(userVerification));
    }
}
