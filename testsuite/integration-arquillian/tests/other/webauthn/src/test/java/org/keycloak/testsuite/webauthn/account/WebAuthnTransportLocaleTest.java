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

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_BLE;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_INTERNAL;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_NFC;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_USB;

/**
 * Test for checking localization for authenticator transport media name
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
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
