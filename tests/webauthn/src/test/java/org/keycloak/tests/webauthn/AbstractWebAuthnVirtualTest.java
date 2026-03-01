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

package org.keycloak.tests.webauthn;

import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.AuthenticationFlowConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.tests.webauthn.authenticators.KcVirtualAuthenticator;
import org.keycloak.tests.webauthn.authenticators.UseVirtualAuthenticators;
import org.keycloak.tests.webauthn.authenticators.VirtualAuthenticatorManager;
import org.keycloak.tests.webauthn.page.WebAuthnErrorPage;
import org.keycloak.tests.webauthn.page.WebAuthnLoginPage;
import org.keycloak.tests.webauthn.page.WebAuthnRegisterPage;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.Credential;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Abstract class for WebAuthn tests which use Virtual Authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@KeycloakIntegrationTest
public abstract class AbstractWebAuthnVirtualTest implements UseVirtualAuthenticators {

    @InjectRealm(ref = "webauthn", config = WebAuthnRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectEvents(realmRef = "webauthn")
    Events events;

    @InjectOAuthClient(realmRef = "webauthn")
    OAuthClient oAuthClient;

    @InjectTestApp
    TestApp testApp;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected LoginUsernamePage loginUsernamePage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected RegisterPage registerPage;

    @InjectPage
    protected WebAuthnRegisterPage webAuthnRegisterPage;

    @InjectPage
    protected WebAuthnErrorPage webAuthnErrorPage;

    @InjectPage
    protected WebAuthnLoginPage webAuthnLoginPage;

    @InjectPage
    protected LogoutConfirmPage logoutConfirmPage;

    @InjectPage
    protected InfoPage infoPage;

    protected static final Logger LOGGER = Logger.getLogger(AbstractWebAuthnVirtualTest.class);
    protected static final String ALL_ZERO_AAGUID = "00000000-0000-0000-0000-000000000000";
    protected static final String ALL_ONE_AAGUID = "11111111-1111-1111-1111-111111111111";
    protected static final String USERNAME = "UserWebAuthn";
    protected static final String PASSWORD = generatePassword();
    protected static final String EMAIL = "UserWebAuthn@email";

    protected final static String base64EncodedPK =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg8_zMDQDYAxlU-Q"
                    + "hk1Dwkf0v18GZca1DMF3SaJ9HPdmShRANCAASNYX5lyVCOZLzFZzrIKmeZ2jwU"
                    + "RmgsJYxGP__fWN_S-j5sN4tT15XEpN_7QZnt14YvI6uvAgO0uJEboFaZlOEB";

    protected final static PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(Base64.getUrlDecoder().decode(base64EncodedPK));

    private VirtualAuthenticatorManager virtualAuthenticatorManager;

    @BeforeEach
    public void initWebAuthnTestRealm() {
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        if (isPasswordless()) {
            makePasswordlessRequiredActionDefault(realmRep);
            switchExecutionInBrowserFormToPasswordless(realmRep);
        }
        managedRealm.updateWithCleanup(r -> r.update(realmRep));

        setUpVirtualAuthenticator();
    }

    @AfterEach
    public void cleanup() {
        removeVirtualAuthenticator();
    }

    @Override
    public void setUpVirtualAuthenticator() {
        this.virtualAuthenticatorManager = createDefaultVirtualManager(driver.driver(), getDefaultAuthenticatorOptions());
        events.clear();
    }

    @Override
    public void removeVirtualAuthenticator() {
        virtualAuthenticatorManager.removeAuthenticator();
        events.clear();
    }

    public UserResource userResource() {
        return AdminApiUtil.findUserByUsernameId(managedRealm.admin(), USERNAME);
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

    public String getCredentialType() {
        return isPasswordless() ? WebAuthnCredentialModel.TYPE_PASSWORDLESS : WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }

    public boolean isPasswordless() {
        return false;
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
        oAuthClient.openRegistrationForm();

        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email, username, password, password);

        // User was registered. Now he needs to register WebAuthn credential
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
                webAuthnRegisterPage.assertCurrent();
                webAuthnRegisterPage.clickRegister();

                if (webAuthnRegisterPage.isRegisterAlertPresent()) {
                    webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);
                    return;
                } else {
                    webAuthnRegisterPage.assertCurrent();
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
        authenticateUser("test-user@localhost", "password", shouldSuccess);
    }

    protected void authenticateUser(String username, String password, boolean shouldSuccess) {
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(username, password);
        loginPage.submit();

        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();

        if (shouldSuccess) {
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());
        } else {
            displayErrorMessageIfPresent();
        }
    }

    protected String displayErrorMessageIfPresent() {
        if (webAuthnErrorPage.getExpectedPageId().equals(driver.page().getCurrentPageId())) {
            final String msg = webAuthnErrorPage.getError();
            LOGGER.info("Error message from Error Page: " + msg);
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
            oAuthClient.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();
            infoPage.assertCurrent();
            Assertions.assertEquals("You are logged out", infoPage.getInfo());
        } catch (Exception e) {
            throw new RuntimeException("Cannot logout user", e);
        }
    }

    protected String getExpectedMessageByDriver(Map<Class<? extends WebDriver>, String> values) {
        if (values == null || values.isEmpty()) return "";

        return values.entrySet()
                .stream()
                .filter(Objects::nonNull)
                .filter(f -> f.getKey().isAssignableFrom(driver.getClass()))
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
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        assertThat(realmRep, notNullValue());
        if(!isPasswordless()) {
            assertThat(realmRep.getWebAuthnPolicyRpEntityName(), is("localhost"));
            assertThat(realmRep.getWebAuthnPolicyRequireResidentKey(), is(residentKey));
            assertThat(realmRep.getWebAuthnPolicyUserVerificationRequirement(), is(userVerification));
        } else {
            assertThat(realmRep.getWebAuthnPolicyPasswordlessRpEntityName(), is("localhost"));
            assertThat(realmRep.getWebAuthnPolicyPasswordlessRequireResidentKey(), is(residentKey));
            assertThat(realmRep.getWebAuthnPolicyPasswordlessUserVerificationRequirement(), is(userVerification));
        }
    }

    protected static String generatePassword() {
        return SecretGenerator.getInstance().randomString(64);
    }

    public static class WebAuthnRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.name("webauthn").registrationAllowed(true);

            AuthenticationFlowConfigBuilder flowBuilder1 = builder
                    .addAuthenticationFlow("browser-webauthn", "browser based authentication", "basic-flow", true, false);
                    flowBuilder1.addAuthenticationExecutionWithAuthenticator("auth-cookie", "ALTERNATIVE", 10, false);
                    flowBuilder1.addAuthenticationExecutionWithAuthenticator("auth-spnego", "DISABLED", 20, false);
                    flowBuilder1.addAuthenticationExecutionWithAuthenticator("identity-provider-redirector", "DISABLED", 25, false);
                    flowBuilder1.addAuthenticationExecutionWithAliasFlow("browser-webauthn-organization", "ALTERNATIVE", 26, false);
                    flowBuilder1.addAuthenticationExecutionWithAliasFlow("browser-webauthn-forms","ALTERNATIVE", 30, false);

            builder.addAuthenticationFlow("browser-webauthn-organization", "", "basic-flow", false, true)
                .addAuthenticationExecutionWithAliasFlow("browser-webauthn-conditional-organization", "CONDITIONAL", 10, false);

            AuthenticationFlowConfigBuilder flowBuilder2 = builder.addAuthenticationFlow("browser-webauthn-conditional-organization", "Flow to determine if the organization identity-first login is to be used", "basic-flow", false, true);
            flowBuilder2.addAuthenticationExecutionWithAuthenticator("conditional-user-configured", "REQUIRED", 10, false);
            flowBuilder2.addAuthenticationExecutionWithAuthenticator("organization", "ALTERNATIVE" , 20, false);

            AuthenticationFlowConfigBuilder flowBuilder3 = builder.addAuthenticationFlow("browser-webauthn-forms", "Username, password, otp and other auth forms.", "basic-flow", false,false);
            flowBuilder3.addAuthenticationExecutionWithAuthenticator("auth-username-password-form", "REQUIRED", 10, false);
            flowBuilder3.addAuthenticationExecutionWithAuthenticator("auth-otp-form", "DISABLED" , 20, false);
            flowBuilder3.addAuthenticationExecutionWithAuthenticator("webauthn-authenticator", "REQUIRED", 21, false);

            AuthenticationFlowConfigBuilder flowBuilder4 = builder.addAuthenticationFlow("browser-webauthn-passwordless", "browser based authentication", "basic-flow", true, false);
            flowBuilder4.addAuthenticationExecutionWithAuthenticator("auth-cookie", "ALTERNATIVE", 10, false);
            flowBuilder4.addAuthenticationExecutionWithAliasFlow("browser-webauthn-passwordless-forms", "ALTERNATIVE", 30, false);

            AuthenticationFlowConfigBuilder flowBuilder5 = builder.addAuthenticationFlow("browser-webauthn-passwordless-forms", "Username, password, otp and other auth forms.", "basic-flow", false, false);
            flowBuilder5.addAuthenticationExecutionWithAuthenticator("auth-username-password-form", "REQUIRED", 10, false);
            flowBuilder5.addAuthenticationExecutionWithAuthenticator("webauthn-authenticator", "REQUIRED", 20, false);
            flowBuilder5.addAuthenticationExecutionWithAuthenticator("webauthn-authenticator-passwordless", "REQUIRED", 30, false);

            RequiredActionProviderRepresentation actionRep1 = new RequiredActionProviderRepresentation();
            actionRep1.setAlias("webauthn-register");
            actionRep1.setName("Webauthn Register");
            actionRep1.setProviderId("webauthn-register");
            actionRep1.setEnabled(true);
            actionRep1.setDefaultAction(true);
            actionRep1.setPriority(51);
            actionRep1.setConfig(Collections.emptyMap());

            builder.requiredAction(actionRep1);

            RequiredActionProviderRepresentation actionRep2 = new RequiredActionProviderRepresentation();
            actionRep2.setAlias("webauthn-register-passwordless");
            actionRep2.setName("Webauthn Register Passwordless");
            actionRep2.setProviderId("webauthn-register-passwordless");
            actionRep2.setEnabled(true);
            actionRep2.setDefaultAction(false);
            actionRep2.setPriority(52);
            actionRep2.setConfig(Collections.emptyMap());

            builder.requiredAction(actionRep2);

            builder.webAuthnPolicySignatureAlgorithms(List.of("ES256", "RS256", "RS1"))
                .webAuthnPolicyAttestationConveyancePreference("not specified")
                .webAuthnPolicyAuthenticatorAttachment("not specified")
                .webAuthnPolicyRequireResidentKey("not specified")
                .webAuthnPolicyUserVerificationRequirement("not specified")
                .webAuthnPolicyRpEntityName("keycloak-webauthn-2FA")
                .webAuthnPolicyCreateTimeout(60)
                .webAuthnPolicyAvoidSameAuthenticatorRegister(true);

            builder.webAuthnPolicyPasswordlessSignatureAlgorithms(List.of("ES256", "RS256", "RS1"))
                .webAuthnPolicyPasswordlessAttestationConveyancePreference("not specified")
                .webAuthnPolicyPasswordlessAuthenticatorAttachment("not specified")
                .webAuthnPolicyPasswordlessRequireResidentKey("not specified")
                .webAuthnPolicyPasswordlessUserVerificationRequirement("not specified")
                .webAuthnPolicyPasswordlessRpEntityName("keycloak-webauthn-passwordless-2FA")
                .webAuthnPolicyPasswordlessCreateTimeout(60)
                .webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(true);

            builder.browserFlow("browser-webauthn");

            builder.addUser(USERNAME).password(PASSWORD).name("WebAuthn", "User")
                    .email("webauthn-user@localhost").emailVerified(true);
            return builder;
        }
    }
}
