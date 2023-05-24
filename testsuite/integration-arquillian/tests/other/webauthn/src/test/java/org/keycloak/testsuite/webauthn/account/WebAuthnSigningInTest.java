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

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.keycloak.theme.DateTimeFormatterUtil;

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

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils.assertUserCredential;
import static org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils.testSetUpLink;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

public class WebAuthnSigningInTest extends AbstractWebAuthnAccountTest {

    @Test
    public void categoriesTest() {
        testContext.setTestRealmReps(emptyList()); // reimport realm after this test

        assertThat(signingInPage.getCategoriesCount(), is(3));
        assertThat(signingInPage.getCategoryTitle("basic-authentication"), is("Basic authentication"));
        assertThat(signingInPage.getCategoryTitle("two-factor"), is("Two-factor authentication"));
        assertThat(signingInPage.getCategoryTitle("passwordless"), is("Passwordless"));

        // Delete WebAuthn flow ==> Passwordless category should disappear
        testRealmResource().flows().deleteFlow(WEBAUTHN_FLOW_ID);
        refreshPageAndWaitForLoad();

        assertThat(signingInPage.getCategoriesCount(), is(2));
    }

    @Test
    public void twoFactorWebAuthnTest() {
        testWebAuthn(false);
    }

    @Test
    public void passwordlessWebAuthnTest() {
        testWebAuthn(true);
    }

    @Test
    public void createWebAuthnSameUserLabel() {
        final String SAME_LABEL = "key123";

        // Do we really allow to have several authenticators with the same user label??

        SigningInPage.UserCredential webAuthn = addWebAuthnCredential(SAME_LABEL, false);
        assertThat(webAuthn, notNullValue());
        SigningInPage.UserCredential passwordless = addWebAuthnCredential(SAME_LABEL, true);
        assertThat(passwordless, notNullValue());

        assertThat(webAuthnCredentialType.getUserCredentialsCount(), is(1));
        webAuthn = webAuthnCredentialType.getUserCredential(webAuthn.getId());
        assertThat(webAuthn, notNullValue());
        assertThat(webAuthn.getUserLabel(), is(SAME_LABEL));

        assertThat(webAuthnPwdlessCredentialType.getUserCredentialsCount(), is(1));
        passwordless = webAuthnPwdlessCredentialType.getUserCredential(passwordless.getId());
        assertThat(passwordless, notNullValue());
        assertThat(passwordless.getUserLabel(), is(SAME_LABEL));

        SigningInPage.UserCredential webAuthn2 = addWebAuthnCredential(SAME_LABEL, false);
        assertThat(webAuthn2, notNullValue());
        assertThat(webAuthn2.getUserLabel(), is(SAME_LABEL));

        assertThat(webAuthnCredentialType.getUserCredentialsCount(), is(2));

        SigningInPage.UserCredential passwordless2 = addWebAuthnCredential(SAME_LABEL, true);
        assertThat(passwordless2, notNullValue());
        assertThat(passwordless2.getUserLabel(), is(SAME_LABEL));

        assertThat(webAuthnPwdlessCredentialType.getUserCredentialsCount(), is(2));
    }

    @Test
    public void multipleSecurityKeys() {
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
    public void setUpLinksTest() {
        testSetUpLink(testRealmResource(), webAuthnCredentialType, WebAuthnRegisterFactory.PROVIDER_ID);
        testSetUpLink(testRealmResource(), webAuthnPwdlessCredentialType, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
    }

    @Test
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
    public void notDisplayAvailableAuthenticatorsPasswordless() {
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

        driver.navigate().refresh();

        webAuthnLoginPage.assertCurrent();
        authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));

        webAuthnLoginPage.clickAuthenticate();
        signingInPage.assertCurrent();
    }

    @Test
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
            expectedHelpText = "Use your security key for passwordless sign in.";
            providerId = WebAuthnPasswordlessRegisterFactory.PROVIDER_ID;
        } else {
            credentialType = webAuthnCredentialType;
            expectedHelpText = "Use your security key to sign in.";
            providerId = WebAuthnRegisterFactory.PROVIDER_ID;
        }

        assertThat(credentialType.isSetUp(), is(false));
        // no way to simulate registration cancellation

        assertThat("Set up link for \"" + credentialType.getType() + "\" is not visible", credentialType.isSetUpLinkVisible(), is(true));
        assertThat(credentialType.getTitle(), is("Security key"));
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

        RequiredActionProviderRepresentation requiredAction = new RequiredActionProviderRepresentation();
        requiredAction.setEnabled(false);
        testRealmResource().flows().updateRequiredAction(providerId, requiredAction);

        refreshPageAndWaitForLoad();

        assertThat("Set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isSetUpLinkVisible(), is(false));
        assertThat("Not set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isNotSetUpLabelVisible(), is(false));
        assertThat("Title for \"" + credentialType.getType() + "\" is not visible", credentialType.isTitleVisible(), is(true));
        assertThat(credentialType.getUserCredentialsCount(), is(2));

        testRemoveCredential(webAuthn1);
    }
}
