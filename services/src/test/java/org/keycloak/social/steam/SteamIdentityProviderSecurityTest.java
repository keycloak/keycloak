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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.broker.provider.IdentityBrokerException;

/**
 * Unit tests for Steam security gating in {@link SteamIdentityProvider}:
 * VAC bans, community bans, ban cooldowns, and Steam level filtering.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamIdentityProviderSecurityTest {

    private static final String STEAM_ID = "76561198000000000";

    private SteamIdentityProviderConfig config;
    private TestableSteamIdentityProvider provider;

    @Before
    public void setup() {
        config = new SteamIdentityProviderConfig();
        provider = new TestableSteamIdentityProvider(config);
    }

    @Test
    public void testVACBanned_WhenRejectEnabled_ThrowsException() throws Exception {
        config.getConfig().put("rejectVACBanned", "true");
        provider.mockBanJson = "{\"players\":[{\"VACBanned\":true,\"CommunityBanned\":false,\"DaysSinceLastBan\":10,\"NumberOfVACBans\":1,\"NumberOfGameBans\":0}]}";

        Assert.assertThrows(IdentityBrokerException.class, () -> provider.checkSecurityBans(STEAM_ID));
    }

    @Test
    public void testVACBanned_WhenRejectDisabled_Passes() throws Exception {
        config.getConfig().put("rejectVACBanned", "false");
        provider.mockBanJson = "{\"players\":[{\"VACBanned\":true,\"CommunityBanned\":false,\"DaysSinceLastBan\":10,\"NumberOfVACBans\":1,\"NumberOfGameBans\":0}]}";

        provider.checkSecurityBans(STEAM_ID); // should not throw
    }

    @Test
    public void testCommunityBanned_WhenRejectEnabled_ThrowsException() throws Exception {
        config.getConfig().put("rejectCommunityBanned", "true");
        provider.mockBanJson = "{\"players\":[{\"VACBanned\":false,\"CommunityBanned\":true,\"DaysSinceLastBan\":5,\"NumberOfVACBans\":0,\"NumberOfGameBans\":0}]}";

        Assert.assertThrows(IdentityBrokerException.class, () -> provider.checkSecurityBans(STEAM_ID));
    }

    @Test
    public void testBanCooldown_WhenBanTooRecent_ThrowsException() throws Exception {
        config.getConfig().put("minDaysSinceLastBan", "365");
        provider.mockBanJson = "{\"players\":[{\"VACBanned\":false,\"CommunityBanned\":false,\"DaysSinceLastBan\":100,\"NumberOfVACBans\":1,\"NumberOfGameBans\":0}]}";

        Assert.assertThrows(IdentityBrokerException.class, () -> provider.checkSecurityBans(STEAM_ID));
    }

    @Test
    public void testBanCooldown_WhenBanOldEnough_Passes() throws Exception {
        config.getConfig().put("minDaysSinceLastBan", "365");
        provider.mockBanJson = "{\"players\":[{\"VACBanned\":false,\"CommunityBanned\":false,\"DaysSinceLastBan\":400,\"NumberOfVACBans\":1,\"NumberOfGameBans\":0}]}";

        provider.checkSecurityBans(STEAM_ID);
    }

    @Test
    public void testEmptyPlayerList_Passes() throws Exception {
        config.getConfig().put("rejectVACBanned", "true");
        provider.mockBanJson = "{\"players\":[]}";

        provider.checkSecurityBans(STEAM_ID);
    }

    @Test
    public void testSteamLevel_WhenAboveMinimum_Passes() throws Exception {
        config.getConfig().put("minSteamLevel", "5");
        provider.mockLevelJson = "{\"response\":{\"player_level\":10}}";

        provider.checkSteamLevel(STEAM_ID);
    }

    @Test
    public void testSteamLevel_WhenExactlyAtMinimum_Passes() throws Exception {
        config.getConfig().put("minSteamLevel", "5");
        provider.mockLevelJson = "{\"response\":{\"player_level\":5}}";

        provider.checkSteamLevel(STEAM_ID);
    }

    @Test
    public void testSteamLevel_WhenBelowMinimum_ThrowsException() throws Exception {
        config.getConfig().put("minSteamLevel", "5");
        provider.mockLevelJson = "{\"response\":{\"player_level\":2}}";

        Assert.assertThrows(IdentityBrokerException.class, () -> provider.checkSteamLevel(STEAM_ID));
    }

    @Test
    public void testSteamLevel_WhenLevelIsZero_ThrowsException() throws Exception {
        config.getConfig().put("minSteamLevel", "1");
        provider.mockLevelJson = "{\"response\":{\"player_level\":0}}";

        Assert.assertThrows(IdentityBrokerException.class, () -> provider.checkSteamLevel(STEAM_ID));
    }

    @Test
    public void testSteamLevel_WhenMinimumIsZero_Passes() throws Exception {
        config.getConfig().put("minSteamLevel", "0");
        provider.mockLevelJson = "{\"response\":{\"player_level\":0}}";

        provider.checkSteamLevel(STEAM_ID);
    }

    @Test
    public void testSteamLevel_WhenApiReturnsNoLevelField_Passes() throws Exception {
        config.getConfig().put("minSteamLevel", "5");
        provider.mockLevelJson = "{\"response\":{}}";

        provider.checkSteamLevel(STEAM_ID);
    }

    /**
     * Concrete testable identity provider wrapper that injects target local JSON test buffers.
     */
    private static class TestableSteamIdentityProvider extends SteamIdentityProvider {
        public String mockBanJson;
        public String mockLevelJson;

        public TestableSteamIdentityProvider(SteamIdentityProviderConfig config) {
            super(null, config);
        }

        @Override
        protected JsonNode fetchBanPayloadFromSteam(String url) throws java.io.IOException {
            return new ObjectMapper().readTree(mockBanJson);
        }

        @Override
        protected JsonNode fetchSteamLevelFromSteam(String url) throws java.io.IOException {
            return new ObjectMapper().readTree(mockLevelJson);
        }
    }
}
