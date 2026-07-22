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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FipsProviderTest {

    @Test
    public void shouldResolveProviderOptions() {
        assertEquals(FipsProvider.AUTO, FipsProvider.valueOfOption("auto"));
        assertEquals(FipsProvider.BOUNCY_CASTLE, FipsProvider.valueOfOption("bouncycastle"));
        assertEquals(FipsProvider.GLASSLESS, FipsProvider.valueOfOption("glassless"));
        assertEquals("auto", FipsProvider.AUTO.toString());
        assertEquals("bouncycastle", FipsProvider.BOUNCY_CASTLE.toString());
        assertEquals("glassless", FipsProvider.GLASSLESS.toString());
        assertThrows(IllegalArgumentException.class, () -> FipsProvider.valueOfOption("unknown"));
    }

    @Test
    public void shouldSelectGlasslessWhenBouncyCastleIsUnavailable() {
        assertEquals(FipsProvider.GLASSLESS, FipsProvider.AUTO.resolve(new ClassLoader(null) {
        }));
    }

    @Test
    public void shouldSelectBouncyCastleWhenItsProviderClassIsAvailable() {
        ClassLoader classLoader = new ClassLoader(null) {
            @Override
            public java.net.URL getResource(String name) {
                return "org/bouncycastle/jcajce/provider/BouncyCastleFipsProvider.class".equals(name)
                        ? FipsProviderTest.class.getResource("FipsProviderTest.class")
                        : null;
            }
        };

        assertEquals(FipsProvider.BOUNCY_CASTLE, FipsProvider.AUTO.resolve(classLoader));
    }

    @Test
    public void shouldPreserveExplicitProviderSelection() {
        ClassLoader classLoader = new ClassLoader(null) {
            @Override
            public java.net.URL getResource(String name) {
                return FipsProviderTest.class.getResource("FipsProviderTest.class");
            }
        };

        assertEquals(FipsProvider.GLASSLESS, FipsProvider.GLASSLESS.resolve(classLoader));
        assertEquals(FipsProvider.BOUNCY_CASTLE, FipsProvider.BOUNCY_CASTLE.resolve(new ClassLoader(null) {
        }));
    }

    @Test
    public void shouldKeepExistingProviderClassNames() {
        assertEquals("org.keycloak.crypto.fips.FIPS1402Provider", FipsMode.NON_STRICT.getProviderClassName());
        assertEquals("org.keycloak.crypto.fips.Fips1402StrictCryptoProvider", FipsMode.STRICT.getProviderClassName());
        assertEquals("org.keycloak.crypto.def.DefaultCryptoProvider", FipsMode.DISABLED.getProviderClassName());
    }
}
