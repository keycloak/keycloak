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

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

/**
 * Identity Provider Mapper that grants a specific Keycloak role if the user
 * owns a designated application (game) on Steam.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamGameRoleMapper extends AbstractIdentityProviderMapper {

    private static final String[] COMPATIBLE_PROVIDERS = new String[] { SteamIdentityProviderFactory.PROVIDER_ID };
    public static final String PROVIDER_ID = "steam-game-role-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayCategory() {
        return "Role Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Steam Game Ownership to Role";
    }

    @Override
    public String getHelpText() {
        return "Queries the Steam API to verify game ownership. If the user owns the specified App ID, they are granted the mapped Keycloak role.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name("appId")
                .label("Steam App ID")
                .helpText("The numerical Steam Application ID (e.g., 730 for CS:GO) the user must own.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()

                .property().name("role")
                .label("Role")
                .helpText("The Keycloak role to grant if ownership is verified.")
                .type(ProviderConfigProperty.ROLE_TYPE)
                .required(true)
                .add()
                .build();
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        assignRoleIfGameOwned(session, realm, user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        assignRoleIfGameOwned(session, realm, user, mapperModel, context);
    }

    private void assignRoleIfGameOwned(KeycloakSession session, RealmModel realm, UserModel user,
        IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

    String targetAppId = mapperModel.getConfig().get("appId");
    String roleName = mapperModel.getConfig().get("role");

    if (targetAppId == null || roleName == null || targetAppId.trim().isEmpty()) {
        return;
    }

    int targetAppIdInt;
    try {
        targetAppIdInt = Integer.parseInt(targetAppId.trim());
    } catch (NumberFormatException e) {
        org.jboss.logging.Logger.getLogger(SteamGameRoleMapper.class)
                .warnf("Configured Steam App ID '%s' is not a valid integer.", targetAppId);
        return;
    }

    String steamId64 = session.users()
        .getFederatedIdentitiesStream(realm, user)
        .filter(fi -> SteamIdentityProviderFactory.PROVIDER_ID.equals(fi.getIdentityProvider()))
        .map(fi -> fi.getUserId())
        .findFirst()
        .orElse(null);

    String apiKey = Optional.ofNullable(
        session.getProvider(org.keycloak.models.IdentityProviderStorageProvider.class)
               .getByAlias(SteamIdentityProviderFactory.PROVIDER_ID))
        .map(idp -> idp.getConfig().get("steamApiKey"))
        .filter(key -> key != null && !key.trim().isEmpty())
        .orElse(null);

    if (apiKey == null) {
        return;
    }

    try {
        String url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="
                + apiKey + "&steamid=" + steamId64 + "&include_played_free_games=1";

        // Memory-safe streaming check
        boolean ownsGame = isGameOwned(session, url, targetAppIdInt);

        if (ownsGame) {
            RoleModel role;
            if (roleName.contains(".")) {
                String[] parts = roleName.split("\\.", 2);
                org.keycloak.models.ClientModel client = realm.getClientByClientId(parts[0]);
                role = client != null ? client.getRole(parts[1]) : null;
            } else {
                role = realm.getRole(roleName);
            }

            if (role != null) {
                user.grantRole(role);
            }
        }
    } catch (Exception e) {
        org.jboss.logging.Logger.getLogger(SteamGameRoleMapper.class)
                .warn("Could not verify Steam game ownership for role mapping.", e);
    }
}

    /**
     * Isolated robust network call using Jackson Streaming API to prevent memory spikes
     * on accounts with thousands of owned games. Protected for unit testing.
     */
    protected boolean isGameOwned(KeycloakSession session, String url, int targetAppId) throws java.io.IOException {
        var response = SimpleHttp.create(session).doGet(url).asResponse();
        int status = response.getStatus();

        if (status != 200) {
            org.jboss.logging.Logger.getLogger(SteamGameRoleMapper.class)
                    .warnf("Steam owned games API returned HTTP %d for URL: %s", status, url);
            return false;
        }

        String body = response.asString();
        try (java.io.InputStream is = new java.io.ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                com.fasterxml.jackson.core.JsonParser parser = new com.fasterxml.jackson.core.JsonFactory().createParser(is)) {

            while (!parser.isClosed()) {
                com.fasterxml.jackson.core.JsonToken token = parser.nextToken();

                // Break condition if EOF is reached
                if (token == null) break;

                if (com.fasterxml.jackson.core.JsonToken.FIELD_NAME.equals(token) && "appid".equals(parser.currentName())) {
                    parser.nextToken(); // Move to the integer value
                    if (parser.getIntValue() == targetAppId) {
                        return true; // Game found, immediately close stream and return
                    }
                }
            }
        } catch (Exception e) {
            org.jboss.logging.Logger.getLogger(SteamGameRoleMapper.class)
                    .warn("Stream parsing error while reading Steam owned games payload.", e);
        }
        return false;
    }
}
