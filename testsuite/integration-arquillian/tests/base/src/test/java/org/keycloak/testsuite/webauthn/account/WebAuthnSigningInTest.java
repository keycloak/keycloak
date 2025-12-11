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

package org.keycloak.testsuite.webauthn.account;

import java.io.Closeable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.page.AbstractPatternFlyAlert;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.pages.DeviceActivityPage;
import org.keycloak.testsuite.webauthn.pages.SigningInPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.updaters.PasswordLessRealmAttributeUpdater;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;
import org.keycloak.theme.DateTimeFormatterUtil;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxDriver;

import static java.util.Collections.emptyList;

import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.webauthn.utils.SigningInPageUtils.assertUserCredential;
import static org.keycloak.testsuite.webauthn.utils.SigningInPageUtils.testSetUpLink;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

public class WebAuthnSigningInTest extends AbstractWebAuthnAccountTest {

    @Page
    protected DeviceActivityPage deviceActivityPage;

    @Test
    public void categoriesTest() {
        testContext.setTestRealmReps(emptyList()); // reimport realm after this test

        assertThat(signingInPage.getCategoriesCount(), is(3));
        assertThat(signingInPage.getCategoryTitle("basic-authentication"), is("Basic authentication"));
        assertThat(signingInPage.getCategoryTitle("two-factor"), is("Two-factor authentication"));
        assertThat(signingInPage.getCategoryTitle("passwordless"), is("Passwordless"));

        // Delete WebAuthn flow ==> Passwordless category should disappear
        testRealmResource().flows().deleteFlow(WEBAUTHN_FLOW_ID);
        deviceActivityPage.navigateToUsingSidebar();
        signingInPage.navigateToUsingSidebar();

        assertThat(signingInPage.getCategoriesCount(), is(2));
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void twoFactorWebAuthnTest() {
        testWebAuthn(false);
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void passwordlessWebAuthnTest() {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());
        testWebAuthn(true);
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void createWebAuthnSameUserLabel() {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        final String SAME_LABEL = "key123";

        SigningInPage.UserCredential webAuthn = addWebAuthnCredential(SAME_LABEL, true);
        assertThat(webAuthn, notNullValue());

        SigningInPage.CredentialType credentialType = webAuthnPwdlessCredentialType;

        AbstractPatternFlyAlert.waitUntilHidden();

        credentialType.clickSetUpLink();
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(SAME_LABEL);
        waitForPageToLoad();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), is("Failed to register your Passkey.\nDevice already exists with the same name"));
        webAuthnErrorPage.clickTryAgain();

        webAuthnRegisterPage.assertCurrent();
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void multipleSecurityKeys() {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        final String LABEL = "SecurityKey#";

        List<SigningInPage.UserCredential> createdCredentials = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            SigningInPage.UserCredential key = addWebAuthnCredential(LABEL + i, i % 2 == 0);
            assertThat(key, notNullValue());
            createdCredentials.add(key);
        }

        final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
        assertThat(webAuthnCount, is(5));

        final int passwordlessCount = webAuthnPwdlessCredentialType.getUserCredentialsCount();
        assertThat(passwordlessCount, is(5));

        UserResource userResource = testRealmResource().users().get(testUser.getId());
        assertThat(userResource, notNullValue());

        List<CredentialRepresentation> list = userResource.credentials();
        assertThat(list, notNullValue());
        assertThat(list, not(empty()));

        final List<String> credentialsLabels = list.stream()
                .map(CredentialRepresentation::getUserLabel)
                .collect(Collectors.toList());

        assertThat(credentialsLabels, notNullValue());
        final List<String> createdCredentialsLabels = createdCredentials.stream()
                .map(SigningInPage.UserCredential::getUserLabel)
                .collect(Collectors.toList());

        assertThat(credentialsLabels.containsAll(createdCredentialsLabels), is(true));

        final List<SigningInPage.UserCredential> credentials = createdCredentials.stream()
                .filter(key -> key.getUserLabel().equals(LABEL + 0)
                        || key.getUserLabel().equals(LABEL + 1))
                .collect(Collectors.toList());

        assertThat(credentials, hasSize(2));

        testRemoveCredential(credentials.get(0));
        testRemoveCredential(credentials.get(1));

        assertThat(webAuthnCredentialType.getUserCredentialsCount(), is(4));
        assertThat(webAuthnPwdlessCredentialType.getUserCredentialsCount(), is(4));
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void avoidSameAuthenticatorRegister() throws IOException {
        avoidSameAuthenticatorRegister(new WebAuthnRealmAttributeUpdater(testRealmResource()), webAuthnCredentialType);
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void avoidSameAuthenticatorRegisterPasswordless() throws IOException {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());
        avoidSameAuthenticatorRegister(new PasswordLessRealmAttributeUpdater(testRealmResource()), webAuthnPwdlessCredentialType);
    }

    @Test
    public void setUpLinksTest() {
        testSetUpLink(testRealmResource(), webAuthnCredentialType, WebAuthnRegisterFactory.PROVIDER_ID, deviceActivityPage);
        testSetUpLink(testRealmResource(), webAuthnPwdlessCredentialType, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID, deviceActivityPage);
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void displayAvailableAuthenticators() {
        addWebAuthnCredential("authenticator#1");
        addWebAuthnCredential("authenticator#2");

        final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
        assertThat(webAuthnCount, is(2));

        setUpWebAuthnFlow("webAuthnFlow");
        logout();

        signingInPage.navigateTo();
        loginToAccount();

        webAuthnLoginPage.assertCurrent();

        WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(2));
        assertThat(authenticators.getLabels(), Matchers.contains("authenticator#1", "authenticator#2"));

        webAuthnLoginPage.clickAuthenticate();
        signingInPage.assertCurrent();
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void notDisplayAvailableAuthenticatorsPasswordless() {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        addWebAuthnCredential("authenticator#1", true);
        addWebAuthnCredential("authenticator#2", true);

        final int passwordlessCount = webAuthnPwdlessCredentialType.getUserCredentialsCount();
        assertThat(passwordlessCount, is(2));

        setUpWebAuthnFlow("passwordlessFlow", true);
        logout();

        signingInPage.navigateTo();
        loginToAccount();

        webAuthnLoginPage.assertCurrent();
        assertThat(webAuthnLoginPage.getAuthenticators().getCount(), is(0));

        webAuthnLoginPage.clickAuthenticate();
        signingInPage.assertCurrent();
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void availableAuthenticatorsAfterRemove(){
        addWebAuthnCredential("authenticator#1");
        addWebAuthnCredential("authenticator#2");

        final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
        assertThat(webAuthnCount, is(2));

        setUpWebAuthnFlow("webAuthnFlow");
        logout();

        signingInPage.navigateTo();
        loginToAccount();

        webAuthnLoginPage.assertCurrent();

        WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(2));
        assertThat(authenticators.getLabels(), Matchers.contains("authenticator#1", "authenticator#2"));

        final String credentialId = testUserResource().credentials()
                .stream()
                .filter(Objects::nonNull)
                .filter(f -> WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(f.getType()))
                .map(CredentialRepresentation::getId)
                .findFirst()
                .orElse(null);

        assertThat(credentialId, notNullValue());
        testUserResource().removeCredential(credentialId);

        refreshPageAndWaitForLoad();

        webAuthnLoginPage.assertCurrent();
        authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));

        webAuthnLoginPage.clickAuthenticate();
        signingInPage.assertCurrent();
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void checkAuthenticatorTimeLocale() throws ParseException, IOException {
        addWebAuthnCredential("authenticator#1");

        final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
        assertThat(webAuthnCount, is(1));

        setUpWebAuthnFlow("webAuthnFlow");
        logout();

        signingInPage.navigateTo();
        loginToAccount();

        webAuthnLoginPage.assertCurrent();

        WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));
        assertThat(authenticators.getLabels(), Matchers.contains("authenticator#1"));

        WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem item = authenticators.getItems().get(0);
        assertThat(item, notNullValue());
        assertThat(item.getName(), is("authenticator#1"));

        final String dateEnglishString = item.getCreatedDate();
        assertThat(dateEnglishString, notNullValue());

        DateFormat format = DateTimeFormatterUtil.getDefaultDateFormat(Locale.ENGLISH);
        final Date dateEnglish = format.parse(dateEnglishString);
        assertThat(dateEnglish, notNullValue());

        webAuthnLoginPage.clickAuthenticate();
        signingInPage.assertCurrent();

        logout();

        try (Closeable c = setLocalesUpdater(Locale.CHINA.getLanguage()).update()) {
            signingInPage.navigateTo();
            loginToAccount();

            webAuthnLoginPage.assertCurrent();

            authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators.getCount(), is(1));
            item = webAuthnLoginPage.getAuthenticators().getItems().get(0);

            final String dateChineseString = item.getCreatedDate();
            assertThat(dateChineseString, notNullValue());

            format = DateTimeFormatterUtil.getDefaultDateFormat(Locale.CHINA);
            final Date dateChinese = format.parse(dateChineseString);
            assertThat(dateChinese, notNullValue());

            assertThat(dateEnglishString, is(not(dateChineseString)));
            assertThat(dateEnglish, is(dateChinese));

            webAuthnLoginPage.clickAuthenticate();
            signingInPage.assertCurrent();

            logout();
        }

        try (Closeable c = setLocalesUpdater("xx", Locale.ENGLISH.getLanguage()).update()) {
            signingInPage.navigateTo();
            loginToAccount();

            webAuthnLoginPage.assertCurrent();

            authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators.getCount(), is(1));
            item = webAuthnLoginPage.getAuthenticators().getItems().get(0);

            final String dateInvalidString = item.getCreatedDate();
            assertThat(dateInvalidString, notNullValue());

            assertThat(dateInvalidString, is(dateEnglishString));
        }
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void userAuthenticatorTimeLocale() throws IOException {
        Consumer<String> checkCreatedAtLabels = (requiredLabel) ->
                webAuthnLoginPage.getAuthenticators()
                        .getItems()
                        .stream()
                        .map(WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem::getCreatedLabel)
                        .forEach(f -> assertThat(f, is(requiredLabel)));

        try (Closeable c = setLocalesUpdater(Locale.ENGLISH.getLanguage(), "cs").update()) {
            addWebAuthnCredential("authenticator#1");
            addWebAuthnCredential("authenticator#2");

            final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
            assertThat(webAuthnCount, is(2));

            setUpWebAuthnFlow("webAuthnFlow");
            logout();

            signingInPage.navigateTo();
            loginToAccount();

            webAuthnLoginPage.assertCurrent();

            WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators.getCount(), is(2));
            assertThat(authenticators.getLabels(), Matchers.contains("authenticator#1", "authenticator#2"));

            checkCreatedAtLabels.accept("Created");

            webAuthnLoginPage.openLanguage("Čeština");
            checkCreatedAtLabels.accept("Vytvořeno");

            webAuthnLoginPage.clickAuthenticate();
            signingInPage.assertCurrent();
        }
    }

    @Test
    public void cancelRegistration() {
        checkCancelRegistration(false);
    }

    @Test
    public void cancelPasswordlessRegistration() {
        checkCancelRegistration(true);
    }

    private void avoidSameAuthenticatorRegister(AbstractWebAuthnRealmUpdater updater, SigningInPage.CredentialType type) throws IOException {
        try (Closeable c = updater.setWebAuthnPolicyAvoidSameAuthenticatorRegister(Boolean.TRUE).update()) {
            // register first credential successfully
            addWebAuthnCredential("label1", type.getType().equals(WebAuthnCredentialModel.TYPE_PASSWORDLESS));
            assertThat(type.getUserCredentialsCount(), is(1));
            // register the second credential and expect the error
            type.clickSetUpLink();
            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();
            waitForPageToLoad();
            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString(
                    "The user attempted to register an authenticator that contains one of the credentials already registered with the relying party."));
        }
    }

    private void checkCancelRegistration(boolean passwordless) {
        SigningInPage.CredentialType credentialType = passwordless ? webAuthnPwdlessCredentialType : webAuthnCredentialType;

        credentialType.clickSetUpLink();
        waitForPageToLoad();
        webAuthnRegisterPage.assertCurrent();
        assertThat(webAuthnRegisterPage.isAIA(), is(true));
        webAuthnRegisterPage.cancelAIA();

        waitForPageToLoad();
        signingInPage.assertCurrent();
    }

    private void testWebAuthn(boolean passwordless) {
        testContext.setTestRealmReps(emptyList());

        SigningInPage.CredentialType credentialType;
        final String expectedHelpText;
        final String providerId;

        if (passwordless) {
            credentialType = webAuthnPwdlessCredentialType;
            expectedHelpText = "Use your Passkey for passwordless sign in.";
            providerId = WebAuthnPasswordlessRegisterFactory.PROVIDER_ID;
        } else {
            credentialType = webAuthnCredentialType;
            expectedHelpText = "Use your Passkey to sign in.";
            providerId = WebAuthnRegisterFactory.PROVIDER_ID;
        }

        assertThat(credentialType.isSetUp(), is(false));
        // no way to simulate registration cancellation

        assertThat("Set up link for \"" + credentialType.getType() + "\" is not visible", credentialType.isSetUpLinkVisible(), is(true));
        assertThat(credentialType.getTitle(), is("Passkey"));
        assertThat(credentialType.getHelpText(), is(expectedHelpText));

        final String label1 = "WebAuthn is convenient";
        final String label2 = "but not yet widely adopted";

        SigningInPage.UserCredential webAuthn1 = addWebAuthnCredential(label1, passwordless);
        assertThat(credentialType.isSetUp(), is(true));
        assertThat(credentialType.getUserCredentialsCount(), is(1));
        assertUserCredential(label1, true, webAuthn1);

        SigningInPage.UserCredential webAuthn2 = addWebAuthnCredential(label2, passwordless);
        assertThat(credentialType.getUserCredentialsCount(), is(2));
        assertUserCredential(label2, true, webAuthn2);

        RequiredActionProviderRepresentation requiredAction = testRealmResource().flows().getRequiredAction(providerId);
        requiredAction.setEnabled(false);
        testRealmResource().flows().updateRequiredAction(providerId, requiredAction);

        deviceActivityPage.navigateToUsingSidebar();
        signingInPage.navigateToUsingSidebar();

        assertThat("Set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isSetUpLinkVisible(), is(false));
        assertThat("Not set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isNotSetUpLabelVisible(), is(false));
        assertThat("Title for \"" + credentialType.getType() + "\" is not visible", credentialType.isTitleVisible(), is(true));
        assertThat(credentialType.getUserCredentialsCount(), is(2));

        testRemoveCredential(webAuthn1);
        requiredAction.setEnabled(true);
        testRealmResource().flows().updateRequiredAction(providerId, requiredAction);
    }
}
