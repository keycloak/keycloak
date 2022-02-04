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

import org.junit.Test;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_BLE;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_INTERNAL;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_NFC;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_USB;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnTransportsTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void usbTransport() {
        assertTransport(DEFAULT_USB.getOptions(), "USB");
    }

    @Test
    public void nfcTransport() {
        assertTransport(DEFAULT_NFC.getOptions(), "NFC");
    }

    @Test
    public void bluetoothTransport() {
        assertTransport(DEFAULT_BLE.getOptions(), "Bluetooth");
    }

    @Test
    public void internalTransport() {
        assertTransport(DEFAULT_INTERNAL.getOptions(), "Internal");
    }

    private void assertTransport(VirtualAuthenticatorOptions authenticator, String transportName) {
        getVirtualAuthManager().useAuthenticator(authenticator);
        registerDefaultUser();
        logout();

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.login(USERNAME, PASSWORD);

        webAuthnLoginPage.assertCurrent();

        WebAuthnAuthenticatorsList authenticatorsList = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticatorsList, notNullValue());

        List<WebAuthnAuthenticatorsList.WebAuthnAuthenticatorItem> items = authenticatorsList.getItems();
        assertThat(items, notNullValue());
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getTransport(), is(transportName));
    }

}
