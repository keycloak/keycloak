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

package org.keycloak.testsuite.webauthn.account;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_BLE;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_INTERNAL;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_NFC;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_USB;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for checking localization for authenticator transport media name
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class WebAuthnTransportLocaleTest extends AbstractWebAuthnAccountTest {

    @Test
    public void localizationTransportUSB() {
        assertLocalizationIndividual(DEFAULT_USB.getOptions(), "USB", "USB");
    }

    @Test
    public void localizationTransportNFC() {
        assertLocalizationIndividual(DEFAULT_NFC.getOptions(), "NFC", "NFC");
    }

    @Test
    public void localizationTransportBluetooth() {
        assertLocalizationIndividual(DEFAULT_BLE.getOptions(), "Bluetooth", "Bluetooth");
    }

    @Test
    public void localizationTransportInternal() {
        assertLocalizationIndividual(DEFAULT_INTERNAL.getOptions(), "Internal", "Interní");
    }

    @Test
    public void multipleTransports() throws IOException {
        final String AUTHENTICATOR_LABEL = "authenticator#";
        final Integer EXPECTED_COUNT = 5;

        final BiConsumer<DefaultVirtualAuthOptions, Integer> addAndVerifyAuthenticator = (options, number) -> {
            getWebAuthnManager().useAuthenticator(options.getOptions());
            addWebAuthnCredential(AUTHENTICATOR_LABEL + number);

            int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
            assertThat(webAuthnCount, is(number));
        };

        addAndVerifyAuthenticator.accept(DEFAULT_INTERNAL, 1);
        addAndVerifyAuthenticator.accept(DEFAULT_BLE, 2);
        addAndVerifyAuthenticator.accept(DEFAULT_NFC, 3);
        addAndVerifyAuthenticator.accept(DEFAULT_USB, 4);
        addAndVerifyAuthenticator.accept(DEFAULT, 5);

        setUpWebAuthnFlow("webAuthnFlow");

        logout();
        signingInPage.navigateTo();
        loginToAccount();
        webAuthnLoginPage.assertCurrent();

        final Supplier<List<WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem>> getItems = () -> {
            final WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators, notNullValue());
            assertThat(authenticators.getCount(), is(EXPECTED_COUNT));

            final List<WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem> list = authenticators.getItems();
            assertThat(list, notNullValue());
            assertThat(list.size(), is(EXPECTED_COUNT));
            return list;
        };

        final BiConsumer<String, Integer> assertAuthenticatorTransport = (transport, number) -> {
            List<WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem> list = getItems.get();
            assertThat(list, notNullValue());
            WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem item = list.get(number - 1);

            assertThat(item, notNullValue());
            assertThat(item.getName(), is(AUTHENTICATOR_LABEL + number));
            assertThat(item.getTransport(), is(transport));
        };

        assertAuthenticatorTransport.accept("Internal", 1);
        assertAuthenticatorTransport.accept("Bluetooth", 2);
        assertAuthenticatorTransport.accept("NFC", 3);
        assertAuthenticatorTransport.accept("USB", 4);
        assertAuthenticatorTransport.accept("USB", 5);

        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();

        logout();
        signingInPage.navigateTo();
        loginToAccount();
        webAuthnLoginPage.assertCurrent();

        try (Closeable c = setLocalesUpdater(Locale.ENGLISH.getLanguage(), "cs").update()) {

            driver.navigate().refresh();
            webAuthnLoginPage.openLanguage("Čeština");

            assertAuthenticatorTransport.accept("Interní", 1);
            assertAuthenticatorTransport.accept("Bluetooth", 2);
            assertAuthenticatorTransport.accept("NFC", 3);
            assertAuthenticatorTransport.accept("USB", 4);
            assertAuthenticatorTransport.accept("USB", 5);
        }
    }

    private void assertLocalizationIndividual(VirtualAuthenticatorOptions options, String originalName, String localizedText) {
        final Consumer<String> checkTransportName = (requiredName) -> {
            WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators, notNullValue());
            assertThat(authenticators.getCount(), is(1));
            assertThat(authenticators.getLabels(), Matchers.contains("authenticator#1"));

            List<WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem> items = authenticators.getItems();
            assertThat(items, notNullValue());
            assertThat(items.size(), is(1));

            WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem item = items.get(0);
            assertThat(item, notNullValue());
            assertThat(item.getTransport(), is(requiredName));
        };

        try (Closeable c = setLocalesUpdater(Locale.ENGLISH.getLanguage(), "cs").update()) {

            getWebAuthnManager().useAuthenticator(options);
            addWebAuthnCredential("authenticator#1");

            final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
            assertThat(webAuthnCount, is(1));

            setUpWebAuthnFlow("webAuthnFlow");
            logout();

            signingInPage.navigateTo();
            loginToAccount();

            webAuthnLoginPage.assertCurrent();

            checkTransportName.accept(originalName);

            webAuthnLoginPage.openLanguage("Čeština");

            checkTransportName.accept(localizedText);

            webAuthnLoginPage.clickAuthenticate();
            signingInPage.assertCurrent();
        } catch (IOException e) {
            throw new RuntimeException("Cannot update locale.", e);
        }
    }
}
