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
package org.keycloak.crypto.elytron;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class WildFlyElytronProviderTest {

    @Test
    public void selectedProviderDoesNotFallBack() {
        CryptoProvider previous = CryptoIntegration.isInitialised() ? CryptoIntegration.getProvider() : null;
        Provider emptyProvider = new Provider("Empty", "1", "No ciphers") {
        };
        try {
            CryptoIntegration.setProvider(new WildFlyElytronProvider() {
                @Override
                public Provider getBouncyCastleProvider() {
                    return emptyProvider;
                }
            });

            assertThrows(NoSuchAlgorithmException.class,
                    () -> WildFlyElytronProvider.getCipher("AES/CBC/PKCS5Padding"));
        } finally {
            CryptoIntegration.setProvider(previous);
        }
    }
}
