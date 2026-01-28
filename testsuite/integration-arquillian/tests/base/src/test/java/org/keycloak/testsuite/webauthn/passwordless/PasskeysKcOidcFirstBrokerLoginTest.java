/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.passwordless;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.broker.AbstractBrokerTest;
import org.keycloak.testsuite.broker.AbstractInitializedBaseBrokerTest;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.BrokerTestConstants;
import org.keycloak.testsuite.broker.BrokerTestTools;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.broker.oidc.TestKeycloakOidcIdentityProviderFactory;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.BrowserDriverUtil;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.KcVirtualAuthenticator;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorManager;
import org.keycloak.testsuite.webauthn.pages.WebAuthnErrorPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnRegisterPage;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author rmartinc
 */
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class PasskeysKcOidcFirstBrokerLoginTest extends AbstractInitializedBaseBrokerTest {

    @Page
    protected RegisterPage registerPage;

    @Page
    protected WebAuthnRegisterPage webAuthnRegisterPage;

    @Page
    protected WebAuthnLoginPage webAuthnLoginPage;

    @Page
    protected WebAuthnErrorPage webAuthnErrorPage;

    private VirtualAuthenticatorManager virtualAuthenticatorManager;

    @Before
    @Override
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        AuthenticationManagementResource authRes = adminClient.realm(bc.consumerRealmName()).flows();
        RequiredActionProviderRepresentation reqAction = authRes.getRequiredAction(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        reqAction.setEnabled(Boolean.TRUE);
        reqAction.setDefaultAction(Boolean.TRUE);
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        authRes.updateRequiredAction(reqAction.getAlias(), reqAction);
    }

    @Before
    public void setUpVirtualAuthenticator() {
        if (!BrowserDriverUtil.isDriverFirefox(driver)) {
            virtualAuthenticatorManager = AbstractWebAuthnVirtualTest.createDefaultVirtualManager(driver, DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());
        }
    }

    @After
    public void removeVirtualAuthenticator() {
        if (!BrowserDriverUtil.isDriverFirefox(driver)) {
            virtualAuthenticatorManager.removeAuthenticator();
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {

            private final String userPassword = generatePassword();

            @Override
            public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                IdentityProviderRepresentation idp = BrokerTestTools.createIdentityProvider(
                        BrokerTestConstants.IDP_OIDC_ALIAS, TestKeycloakOidcIdentityProviderFactory.ID);
                applyDefaultConfiguration(idp.getConfig(), syncMode);
                return idp;
            }

            @Override
            public RealmRepresentation createConsumerRealm() {
                RealmRepresentation consumerRealm = super.createConsumerRealm();
                consumerRealm.setRegistrationAllowed(Boolean.TRUE);
                consumerRealm.setWebAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE);
                return consumerRealm;
            }

            @Override
            public String getUserPassword() {
                return userPassword;
            }
        };
    }

    @Test
    public void testLinkAccountByReauthenticationWithPassword() {
        oauth.realm(bc.consumerRealmName());
        registerUser("consumer", bc.getUserPassword(), BrokerTestConstants.USER_EMAIL, generatePassword(24));

        logout();

        oauth.client("broker-app").realm(bc.consumerRealmName()).openLoginForm();

        logInWithBroker(bc);

        BrokerTestTools.waitForPage(driver, "account already exists", false);
        Assert.assertTrue(idpConfirmLinkPage.isCurrent());
        Assert.assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickLinkAccount();

        Assert.assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

        Assert.assertThrows(NoSuchElementException.class, () -> loginPage.findSocialButton(bc.getIDPAlias()));
        Assert.assertThrows(NoSuchElementException.class, () -> loginPage.clickRegister());
        webAuthnLoginPage.isCurrent();

        loginPage.login(bc.getUserPassword());
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(ApiUtil.findUserByUsername(adminClient.realm(bc.consumerRealmName()), "consumer").getId(), 1);
    }

    @Test
    public void testLinkAccountByReauthenticationWithExternalPasskey() {
        oauth.realm(bc.consumerRealmName());
        registerUser("consumer", bc.getUserPassword(), BrokerTestConstants.USER_EMAIL, generatePassword(24));

        logout();

        oauth.client("broker-app").realm(bc.consumerRealmName()).openLoginForm();

        logInWithBroker(bc);

        BrokerTestTools.waitForPage(driver, "account already exists", false);
        Assert.assertTrue(idpConfirmLinkPage.isCurrent());
        Assert.assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickLinkAccount();

        Assert.assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

        Assert.assertThrows(NoSuchElementException.class, () -> loginPage.findSocialButton(bc.getIDPAlias()));
        Assert.assertThrows(NoSuchElementException.class, () -> loginPage.clickRegister());
        webAuthnLoginPage.isCurrent();

        loginPage.login(generatePassword()); // invalid password
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        webAuthnLoginPage.clickAuthenticate();
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(ApiUtil.findUserByUsername(adminClient.realm(bc.consumerRealmName()), "consumer").getId(), 1);
    }

    @Test
    public void testLinkAccountByReauthenticationWithDiscoverablePasskey() throws IOException {
        virtualAuthenticatorManager.useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        oauth.realm(bc.consumerRealmName());
        registerUser("consumer", bc.getUserPassword(), BrokerTestConstants.USER_EMAIL, generatePassword(24));

        logout();

        // disable passkeys here to not login automatically using the discoverable passkey and allow select the IdP
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(adminClient.realm(bc.consumerRealmName()))
                .setWebAuthnPolicyPasswordlessPasskeysEnabled(Boolean.FALSE)
                .update()) {
            oauth.client("broker-app").realm(bc.consumerRealmName()).openLoginForm();
            logInWithBroker(bc);
        }

        BrokerTestTools.waitForPage(driver, "account already exists", false);
        Assert.assertTrue(idpConfirmLinkPage.isCurrent());
        Assert.assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickLinkAccount();

        // login is automatically now via discoverable passkey
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(ApiUtil.findUserByUsername(adminClient.realm(bc.consumerRealmName()), "consumer").getId(), 1);
    }

    protected void registerUser(String username, String password, String email, String authenticatorLabel) {
        oauth.client("broker-app").realm(bc.consumerRealmName()).openLoginForm();
        loginPage.clickRegister();

        WaitUtils.waitForPageToLoad();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email, username, password, password);

        // User was registered. Now he needs to register WebAuthn credential
        WaitUtils.waitForPageToLoad();
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        tryRegisterAuthenticator(authenticatorLabel, 10);

        WaitUtils.waitForPageToLoad();
    }

    private void tryRegisterAuthenticator(String authenticatorLabel, int numberOfAllowedRetries) {
        final boolean hasResidentKey = Optional.ofNullable(virtualAuthenticatorManager)
                .map(VirtualAuthenticatorManager::getCurrent)
                .map(KcVirtualAuthenticator::getOptions)
                .map(KcVirtualAuthenticator.Options::hasResidentKey)
                .orElse(false);

        if (hasResidentKey && !webAuthnRegisterPage.isRegisterAlertPresent()) {
            for (int i = 0; i < numberOfAllowedRetries; i++) {
                webAuthnErrorPage.clickTryAgain();
                WaitUtils.waitForPageToLoad();
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

    protected void logout() {
        try {
            WaitUtils.waitForPageToLoad();
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();
            infoPage.assertCurrent();
            Assert.assertEquals("You are logged out", infoPage.getInfo());
        } catch (Exception e) {
            throw new RuntimeException("Cannot logout user", e);
        }
    }
}
