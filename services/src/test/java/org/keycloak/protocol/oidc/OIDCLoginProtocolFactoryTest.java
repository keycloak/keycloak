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

package org.keycloak.protocol.oidc;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.utils.ScopeUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that {@link OIDCLoginProtocolFactory#resolveMaxLengthCacheSize} never lets a misconfigured value make the
 * underlying cache unbounded.
 */
public class OIDCLoginProtocolFactoryTest {

    private final OIDCLoginProtocolFactory factory = new OIDCLoginProtocolFactory();

    @Test
    public void usesDefaultWhenNotConfigured() {
        assertEquals(OIDCLoginProtocolFactory.DEFAULT_REQ_PARAMS_MAX_LENGTH_CACHE_SIZE,
                factory.resolveMaxLengthCacheSize(scopeWith(null)));
    }

    @Test
    public void usesConfiguredPositiveValue() {
        assertEquals(42, factory.resolveMaxLengthCacheSize(scopeWith("42")));
    }

    @Test
    public void fallsBackToDefaultForZero() {
        assertEquals(OIDCLoginProtocolFactory.DEFAULT_REQ_PARAMS_MAX_LENGTH_CACHE_SIZE,
                factory.resolveMaxLengthCacheSize(scopeWith("0")));
    }

    @Test
    public void fallsBackToDefaultForNegativeValue() {
        assertEquals(OIDCLoginProtocolFactory.DEFAULT_REQ_PARAMS_MAX_LENGTH_CACHE_SIZE,
                factory.resolveMaxLengthCacheSize(scopeWith("-1")));
    }

    private org.keycloak.Config.Scope scopeWith(String configuredValue) {
        Map<String, String> properties = new HashMap<>();
        if (configuredValue != null) {
            properties.put(OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_PARAMS_MAX_LENGTH_CACHE_SIZE, configuredValue);
        }
        return ScopeUtil.createScope(properties);
    }
}
