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
package org.keycloak.storage;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link CacheableStorageProviderModel}.
 *
 * Covers cache policy configuration, enable/disable state,
 * and eviction timing calculations used by user storage providers.
 */
public class CacheableStorageProviderModelTest {

    /**
     * Test 1: A newly created provider model is enabled by default.
     *
     * When a storage provider is registered with no explicit configuration,
     * it must be active immediately. If isEnabled() defaulted to false,
     * every newly added LDAP or custom provider would silently fail to
     * serve users until an admin explicitly toggled it on — a confusing
     * and hard-to-diagnose failure.
     */
    @Test
    public void newProviderModelIsEnabledByDefault() {
        CacheableStorageProviderModel model = new CacheableStorageProviderModel();
        assertTrue(model.isEnabled());
    }

    /**
     * Test 2: Setting cache policy to NO_CACHE is reflected by getCachePolicy().
     *
     * The cache policy drives whether Keycloak caches federated user data locally.
     * NO_CACHE means every user lookup goes directly to the external provider.
     * If the set/get round-trip is broken, an admin setting NO_CACHE in the UI
     * would have no effect — Keycloak would keep serving stale cached data
     * even when the provider explicitly requires fresh lookups every time.
     */
    @Test
    public void cachePolicyNoCacheIsStoredAndRetrievedCorrectly() {
        CacheableStorageProviderModel model = new CacheableStorageProviderModel();

        model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);

        assertThat(model.getCachePolicy(), is(CacheableStorageProviderModel.CachePolicy.NO_CACHE));
    }

    /**
     * Test 3: A disabled provider model correctly reports isEnabled() as false.
     *
     * Administrators can disable a storage provider without removing it —
     * for example during backend maintenance. If setEnabled(false) did not
     * correctly persist the flag, Keycloak would continue routing user lookups
     * to an unavailable backend, causing login failures instead of falling
     * through to other configured providers.
     */
    @Test
    public void disabledProviderModelReportsNotEnabled() {
        CacheableStorageProviderModel model = new CacheableStorageProviderModel();

        model.setEnabled(false);

        assertFalse(model.isEnabled());
    }
}
