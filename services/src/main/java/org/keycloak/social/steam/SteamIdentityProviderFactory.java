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

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * Factory for creating instances of {@link SteamIdentityProvider}.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamIdentityProviderFactory extends AbstractIdentityProviderFactory<SteamIdentityProvider>
        implements SocialIdentityProviderFactory<SteamIdentityProvider> {

    public static final String PROVIDER_ID = "steam";

    @Override
    public String getName() {
        return "Steam";
    }

    @Override
    public SteamIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        SteamIdentityProviderConfig config = new SteamIdentityProviderConfig(model);

        String apiKey = config.getSteamApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Steam Web API Key is mandatory! Please configure it in the Identity Provider settings.");
        }

        return new SteamIdentityProvider(session, config);
    }

    @Override
    public SteamIdentityProviderConfig createConfig() {
        return new SteamIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()

                .property().name("steamApiKey")
                .label("Steam Web API Key")
                .helpText("Required to fetch user profiles (username/avatar) after authentication.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()

                .property().name("rejectVACBanned")
                .label("Reject VAC Banned Accounts")
                .helpText("If enabled, users with an active VAC ban will be denied login.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()

                .property().name("rejectCommunityBanned")
                .label("Reject Community Banned Accounts")
                .helpText("If enabled, users with Steam Community bans will be denied login.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false).add()

                .property().name("minDaysSinceLastBan")
                .label("Minimum Days Since Last Ban")
                .helpText("Allow previously banned users if their ban is older than this many days (0 to ignore).")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("0").add()

                .property().name("minSteamLevel")
                .label("Minimum Steam Level")
                .helpText("Block accounts below this Steam level (0 to ignore). Useful to filter out spam bots.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("0").add()

                .property().name("failClosedOnApiError")
                .label("Fail Closed on API Error")
                .helpText("If enabled, users will be denied login if Keycloak cannot reach the Valve API to verify ban status. If disabled (Fail Open), network errors will bypass the security check.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(true)
                .add()

                .build();
    }
}
