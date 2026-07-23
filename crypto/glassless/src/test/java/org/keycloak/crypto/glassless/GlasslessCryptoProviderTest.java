/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.crypto.glassless;

import java.security.Provider;
import java.security.Security;

import org.keycloak.crypto.glassless.GlasslessCryptoProvider.GlasslessFipsStatus;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class GlasslessCryptoProviderTest {

    @After
    public void cleanup() {
        Security.removeProvider("GlaSSLess");
    }

    @Test
    public void shouldRegisterGlasslessAtHighestPriority() {
        GlasslessCryptoProvider provider = new GlasslessCryptoProvider(new TestProvider(), false, status(false, false, false));

        assertEquals("GlaSSLess", provider.getBouncyCastleProvider().getName());
        assertEquals("GlaSSLess", Security.getProviders()[0].getName());
    }

    @Test
    public void shouldRejectStrictModeWhenFipsModeIsDisabled() {
        IllegalStateException cause = assertThrows(IllegalStateException.class,
                () -> new GlasslessCryptoProvider(new TestProvider(), true, status(false, true, true)));

        assertTrue(cause.getMessage().contains("active OpenSSL FIPS provider"));
        assertEquals(null, Security.getProvider("GlaSSLess"));
    }

    @Test
    public void shouldRejectStrictModeWhenFipsProviderIsUnavailable() {
        IllegalStateException cause = assertThrows(IllegalStateException.class,
                () -> new GlasslessCryptoProvider(new TestProvider(), true, status(true, false, true)));

        assertTrue(cause.getMessage().contains("active OpenSSL FIPS provider"));
        assertEquals(null, Security.getProvider("GlaSSLess"));
    }

    @Test
    public void shouldRejectStrictModeWhenOpenSslFipsDefaultPropertiesAreDisabled() {
        IllegalStateException cause = assertThrows(IllegalStateException.class,
                () -> new GlasslessCryptoProvider(new TestProvider(), true, status(true, true, false)));

        assertTrue(cause.getMessage().contains("active OpenSSL FIPS provider"));
        assertEquals(null, Security.getProvider("GlaSSLess"));
    }

    @Test
    public void shouldEnableStrictModeWithOpenSslFips() {
        GlasslessCryptoProvider provider = new GlasslessCryptoProvider(new TestProvider(), true, status(true, true, true));

        assertEquals("GlaSSLess", Security.getProviders()[0].getName());
    }

    @Test
    public void shouldFormatProviderConfiguration() {
        assertEquals(String.join(System.lineSeparator(),
                "Glassless provider configuration:",
                "Cipher (2):",
                "  AES/GCM/NoPadding",
                "  RSA/ECB/OAEPPadding",
                "Signature (1):",
                "  SHA256withRSA"), GlasslessCryptoProvider.formatProviderConfiguration(new TestProvider()));
    }

    private static GlasslessFipsStatus status(boolean fipsMode, boolean fipsProviderAvailable, boolean openSslFipsEnabled) {
        return new GlasslessFipsStatus(fipsMode, fipsProviderAvailable, openSslFipsEnabled);
    }

    private static class TestProvider extends Provider {

        private TestProvider() {
            super("GlaSSLess", "test", "Test Glassless provider");
            putService(new Service(this, "Signature", "SHA256withRSA", TestProvider.class.getName(), null, null));
            putService(new Service(this, "Cipher", "RSA/ECB/OAEPPadding", TestProvider.class.getName(), null, null));
            putService(new Service(this, "Cipher", "AES/GCM/NoPadding", TestProvider.class.getName(), null, null));
        }
    }
}
