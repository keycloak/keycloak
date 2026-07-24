/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration.mappers;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyMappersTest {

    @Test
    public void testIsSpiBuildTimeProperty() {
        // Should return true for valid SPI build-time properties
        assertTrue(PropertyMappers.isSpiBuildTimeProperty("kc.spi.foo.bar--provider"));
        assertTrue(PropertyMappers.isSpiBuildTimeProperty("kc.spi.foo.bar--enabled"));
        assertTrue(PropertyMappers.isSpiBuildTimeProperty("kc.spi.foo.bar--provider-default"));

        // return false for non-build time properties
        //assertFalse(PropertyMappers.isSpiBuildTimeProperty("kc.spi.foo.bar-provider"));
        //assertFalse(PropertyMappers.isSpiBuildTimeProperty("kc.spi.foo.bar-enabled"));
        //assertFalse(PropertyMappers.isSpiBuildTimeProperty("kc.spi.foo.bar-provider-default"));
        assertFalse(PropertyMappers.isSpiBuildTimeProperty("some.other.property"));
        assertFalse(PropertyMappers.isSpiBuildTimeProperty("kc.spi.foo.bar"));
    }
}
