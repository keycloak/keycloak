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
package org.keycloak.social.steam;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Unit test for {@link org.keycloak.social.steam.SteamIdentityProviderConfig}
 * and configuration defaults.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamIdentityProviderConfigTest {

    /**
     * Test constructor with empty config to ensure default security policies use safe defaults.
     */
    @Test
    public void testSteamIdentityProviderConfigDefaults() {
        SteamIdentityProviderConfig config = new SteamIdentityProviderConfig();

        assertNull("API Key should be null by default", config.getSteamApiKey());
        assertFalse("VAC Bans should not be rejected by default", config.isRejectVACBanned());
        assertFalse("Community Bans should not be rejected by default", config.isRejectCommunityBanned());
        assertEquals("Minimum ban days should default to 0", 0, config.getMinDaysSinceLastBan());
    }

    /**
     * Test configuration overrides representing admin inputs from the Keycloak UI.
     */
    @Test
    public void testSteamIdentityProviderConfigOverrides() {
        SteamIdentityProviderConfig config = new SteamIdentityProviderConfig();

        config.getConfig().put("steamApiKey", "ABCDEF1234567890");
        config.getConfig().put("rejectVACBanned", "true");
        config.getConfig().put("rejectCommunityBanned", "true");
        config.getConfig().put("minDaysSinceLastBan", "365");

        assertEquals("ABCDEF1234567890", config.getSteamApiKey());
        assertTrue("Should reject VAC bans when overridden", config.isRejectVACBanned());
        assertTrue("Should reject Community bans when overridden", config.isRejectCommunityBanned());
        assertEquals("Should parse minimum days correctly", 365, config.getMinDaysSinceLastBan());
    }

    /**
     * Test safety fallbacks if an admin enters non-numeric text into the integer field.
     */
    @Test
    public void testSteamIdentityProviderConfigInvalidIntegerFallback() {
        SteamIdentityProviderConfig config = new SteamIdentityProviderConfig();

        config.getConfig().put("minDaysSinceLastBan", "not_a_number");

        assertEquals("Should safely fallback to 0 on invalid string", 0, config.getMinDaysSinceLastBan());
    }
}
