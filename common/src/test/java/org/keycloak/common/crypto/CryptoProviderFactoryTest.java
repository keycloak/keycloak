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

package org.keycloak.common.crypto;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CryptoProviderFactoryTest {

    @Test
    public void shouldSelectFactoryByNameAndPassFipsMode() {
        CryptoProvider expected = provider();
        AtomicReference<FipsMode> selectedMode = new AtomicReference<>();
        List<CryptoProviderFactory> factories = Arrays.asList(
                factory("default", provider(), new AtomicReference<>()),
                factory("glassless", expected, selectedMode));

        CryptoProvider actual = CryptoIntegration.detectProvider(factories, "glassless", FipsMode.STRICT);

        assertSame(expected, actual);
        assertEquals(FipsMode.STRICT, selectedMode.get());
    }

    @Test
    public void shouldRejectMissingFactory() {
        IllegalStateException cause = assertThrows(IllegalStateException.class,
                () -> CryptoIntegration.detectProvider(Collections.emptyList(), "glassless", FipsMode.STRICT));

        assertEquals("Expected one crypto provider named 'glassless', found 0", cause.getMessage());
    }

    @Test
    public void shouldRejectDuplicateFactories() {
        List<CryptoProviderFactory> factories = Arrays.asList(
                factory("glassless", provider(), new AtomicReference<>()),
                factory("glassless", provider(), new AtomicReference<>()));

        IllegalStateException cause = assertThrows(IllegalStateException.class,
                () -> CryptoIntegration.detectProvider(factories, "glassless", FipsMode.STRICT));

        assertEquals("Expected one crypto provider named 'glassless', found 2", cause.getMessage());
    }

    @Test
    public void shouldRejectNullProvider() {
        IllegalStateException cause = assertThrows(IllegalStateException.class,
                () -> CryptoIntegration.detectProvider(
                        Collections.singletonList(factory("glassless", null, new AtomicReference<>())), "glassless", FipsMode.STRICT));

        assertEquals("Crypto provider factory 'glassless' returned null", cause.getMessage());
    }

    private static CryptoProviderFactory factory(String name, CryptoProvider provider, AtomicReference<FipsMode> selectedMode) {
        return new CryptoProviderFactory() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public CryptoProvider create(FipsMode fipsMode) {
                selectedMode.set(fipsMode);
                return provider;
            }
        };
    }

    private static CryptoProvider provider() {
        return (CryptoProvider) Proxy.newProxyInstance(CryptoProvider.class.getClassLoader(),
                new Class<?>[] { CryptoProvider.class }, (proxy, method, args) -> null);
    }
}
