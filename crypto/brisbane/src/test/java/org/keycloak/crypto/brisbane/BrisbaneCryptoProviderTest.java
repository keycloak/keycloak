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

package org.keycloak.crypto.brisbane;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

public class BrisbaneCryptoProviderTest {

    @After
    public void cleanup() {
        Security.removeProvider("JipherJCE");
    }

    @Test
    public void shouldRegisterJipherAtHighestPriority() {
        Provider jipher = new Provider("JipherJCE", "test", "Test Jipher provider") {
        };

        BrisbaneCryptoProvider provider = new BrisbaneCryptoProvider(jipher, true);

        assertThat(provider.getBouncyCastleProvider(), is(jipher));
        assertThat(Security.getProviders()[0], is(jipher));
        assertThrows(NoSuchAlgorithmException.class, provider::getAesCbcCipher);
        assertThrows(IllegalArgumentException.class,
                () -> provider.getAlgorithmProvider(JWEAlgorithmProvider.class, CryptoConstants.RSA1_5));
    }

    @Test
    public void shouldRejectPreviouslyRegisteredJipher() {
        Security.addProvider(new Provider("JipherJCE", "test", "Test Jipher provider") {
        });

        IllegalStateException cause = assertThrows(IllegalStateException.class, BrisbaneCryptoProvider::new);

        assertThat(cause.getMessage(), is("Brisbane must be configured before Jipher is registered"));
    }
}
