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

import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import jakarta.ws.rs.core.Response;
import java.net.URI;

/**
 * Identity Provider implementation for Steam using OpenID 2.0.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamIdentityProvider extends AbstractIdentityProvider<SteamIdentityProviderConfig>
        implements SocialIdentityProvider<SteamIdentityProviderConfig> {

    private static final String STEAM_OPENID_LOGIN_URL = "https://steamcommunity.com/openid/login";
    private static final String STEAM_PLAYER_SUMMARIES_URL = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/";
    private static final String STEAM_PLAYER_BANS_URL = "https://api.steampowered.com/ISteamUser/GetPlayerBans/v1/";
    private static final String STEAM_PLAYER_LEVEL_URL = "https://api.steampowered.com/IPlayerService/GetSteamLevel/v1/";

    public SteamIdentityProvider(KeycloakSession session, SteamIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Response retrieveToken(KeycloakSession session, org.keycloak.models.FederatedIdentityModel identity) {
        return Response.noContent().build();
    }

    @Override
    public Response retrieveToken(KeycloakSession session, org.keycloak.models.FederatedIdentityModel identity, org.keycloak.models.UserSessionModel userSession, org.keycloak.models.UserModel user) {
        return Response.noContent().build();
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new SteamEndpoint(callback, this, session);
    }

    protected static class SteamEndpoint {
        private final AuthenticationCallback callback;
        private final SteamIdentityProvider provider;
        private final KeycloakSession session;

        public SteamEndpoint(AuthenticationCallback callback, SteamIdentityProvider provider, KeycloakSession session) {
            this.callback = callback;
            this.provider = provider;
            this.session = session;
        }

        @jakarta.ws.rs.GET
        public Response authResponse() {
            var queryParams = session.getContext().getUri().getQueryParameters();

            org.keycloak.sessions.AuthenticationSessionModel authSession = null;

            try {
                var verificationRequest = SimpleHttp.create(session).doPost(STEAM_OPENID_LOGIN_URL);

                for (var entry : queryParams.entrySet()) {
                    if (!entry.getKey().equals("openid.mode")) {
                        verificationRequest.param(entry.getKey(), entry.getValue().get(0));
                    }
                }
                verificationRequest.param("openid.mode", "check_authentication");

                String verificationResponse = verificationRequest.asString();

                if (verificationResponse == null || !verificationResponse.contains("is_valid:true")) {
                    throw new IdentityBrokerException("Steam OpenID signature validation failed.");
                }

                String claimedId = queryParams.getFirst("openid.claimed_id");
                if (claimedId == null || !claimedId.startsWith("https://steamcommunity.com/openid/id/")) {
                    throw new IdentityBrokerException("Invalid Steam ID format returned.");
                }

                String steamId64 = claimedId.replace("https://steamcommunity.com/openid/id/", "");

                String state = queryParams.getFirst("state");
                if (state == null) {
                    String returnTo = queryParams.getFirst("openid.return_to");
                    if (returnTo != null && returnTo.contains("state=")) {
                        state = returnTo.split("state=")[1].split("&")[0];
                    }
                }

                if (state == null) {
                    throw new IdentityBrokerException("Steam did not return the Keycloak state parameter.");
                }

                authSession = callback.getAndVerifyAuthenticationSession(state);
                if (authSession == null) {
                    throw new IdentityBrokerException("Session not found! If you are using localhost, your browser may be blocking Keycloak's tracking cookies.");
                }

                try {
                    provider.checkSecurityBans(steamId64);
                } catch (IdentityBrokerException e) {
                    return org.keycloak.services.ErrorPage.error(session, authSession,
                            jakarta.ws.rs.core.Response.Status.FORBIDDEN, e.getMessage());
                }

                try {
                    provider.checkSteamLevel(steamId64);
                } catch (IdentityBrokerException e) {
                    return org.keycloak.services.ErrorPage.error(session, authSession,
                            jakarta.ws.rs.core.Response.Status.FORBIDDEN, e.getMessage());
                }

                BrokeredIdentityContext userContext = new BrokeredIdentityContext(steamId64, provider.getConfig());
                userContext.setIdp(provider);
                userContext.setAuthenticationSession(authSession);

                provider.fetchSteamProfile(userContext, steamId64);

                return callback.authenticated(userContext);

            } catch (IdentityBrokerException e) {
                if (authSession != null) {
                    return org.keycloak.services.ErrorPage.error(session, authSession,
                            jakarta.ws.rs.core.Response.Status.BAD_REQUEST, e.getMessage());
                }
                throw e;
            } catch (jakarta.ws.rs.WebApplicationException e) {
                throw new IdentityBrokerException("Steam callback protocol violation.", e);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IdentityBrokerException("Malformed data structure returned by Steam.", e);
            } catch (Exception e) {
                throw new IdentityBrokerException("Unexpected infrastructure failure during Steam verification.", e);
            }
        }

    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        String callbackUri = request.getRedirectUri();
        String state = request.getState().getEncoded();

        String returnToUrl = jakarta.ws.rs.core.UriBuilder.fromUri(callbackUri)
                .queryParam("state", state)
                .build().toString();

        URI steamLoginUri = jakarta.ws.rs.core.UriBuilder.fromUri(STEAM_OPENID_LOGIN_URL)
                .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", returnToUrl)
                .queryParam("openid.realm", callbackUri)
                .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
                .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
                .build();

        return Response.seeOther(steamLoginUri).build();
    }

    protected void fetchSteamProfile(BrokeredIdentityContext userContext, String steamId64) {
        try {
            String apiKey = getConfig().getSteamApiKey();

            String testUrl = STEAM_PLAYER_SUMMARIES_URL + "?key=" + apiKey + "&steamids=0";
            var testResponse = SimpleHttp.create(session).doGet(testUrl).asResponse();
            if (testResponse.getStatus() == 403) {
                org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class).error("Steam API Key is invalid! Skipping profile fetch.");
                return;
            }

            String url = STEAM_PLAYER_SUMMARIES_URL + "?key=" + apiKey + "&steamids=" + steamId64;

            com.fasterxml.jackson.databind.JsonNode profile = SimpleHttp.create(session).doGet(url).asJson();
            com.fasterxml.jackson.databind.JsonNode players = profile.get("response").get("players");

            if (players.isArray() && players.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode player = players.get(0);

                userContext.getContextData().put(org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper.CONTEXT_JSON_NODE, player);

                if (player.has("personaname")) {
                    String rawName = player.get("personaname").asText();
                    // Replace anything that isn't a letter, number, dot, dash, or underscore with an underscore
                    String cleanName = rawName.replaceAll("[^a-zA-Z0-9_.-]", "_");
                    userContext.setUsername(cleanName);
                }

                if (player.has("avatarfull")) {
                    userContext.setUserAttribute("picture", player.get("avatarfull").asText());
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                    .warn("Failed to parse Steam player profiles payload due to invalid JSON structure.", e);
        } catch (java.io.IOException e) {
            org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                    .warn("Network transmission disruption while querying Steam player summaries.", e);
        } catch (Exception e) {
            org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                    .warn("Unanticipated system anomaly during Steam user profile extraction.", e);
        }
    }

    protected void checkSecurityBans(String steamId64) {
        if (!getConfig().isRejectVACBanned() && !getConfig().isRejectCommunityBanned() && getConfig().getMinDaysSinceLastBan() <= 0) {
            return;
        }

        try {
            String apiKey = getConfig().getSteamApiKey();
                String url = STEAM_PLAYER_BANS_URL
                    + "?key=" + apiKey
                    + "&steamids=" + steamId64;

            com.fasterxml.jackson.databind.JsonNode banPayload = fetchBanPayloadFromSteam(url);
            com.fasterxml.jackson.databind.JsonNode players = banPayload.get("players");

            if (players == null || !players.isArray() || players.size() == 0) {
                return; // Can't verify ban status, assume clean to avoid lockouts
            }

            com.fasterxml.jackson.databind.JsonNode banData = players.get(0);

            if (getConfig().isRejectVACBanned() && banData.has("VACBanned") && banData.get("VACBanned").asBoolean()) {
                throw new IdentityBrokerException("Login denied: Active Valve Anti-Cheat (VAC) ban detected on your Steam account.");
            }

            if (getConfig().isRejectCommunityBanned() && banData.has("CommunityBanned") && banData.get("CommunityBanned").asBoolean()) {
                throw new IdentityBrokerException("Login denied: Active Community ban detected on your Steam account.");
            }

            if (getConfig().getMinDaysSinceLastBan() > 0 && banData.has("DaysSinceLastBan")) {
                int daysSinceLastBan = banData.get("DaysSinceLastBan").asInt();

                int totalBans = 0;
                if (banData.has("NumberOfVACBans")) totalBans += banData.get("NumberOfVACBans").asInt();
                if (banData.has("NumberOfGameBans")) totalBans += banData.get("NumberOfGameBans").asInt();

                if (totalBans > 0 && daysSinceLastBan < getConfig().getMinDaysSinceLastBan()) {
                    int daysRemaining = getConfig().getMinDaysSinceLastBan() - daysSinceLastBan;
                    throw new IdentityBrokerException("Login denied: Your account safety cool-down is still active. Please retry in " + daysRemaining + " days.");
                }
            }

        } catch (IdentityBrokerException e) {
            throw e;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                    .warn("Steam Ban API returned an unrecognized payload structure.", e);
            if (getConfig().isFailClosedOnApiError()) throw new IdentityBrokerException("Security verification failed due to malformed Steam API response.");
        } catch (java.io.IOException e) {
            org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                    .warn("Steam Ban API unreachable due to transport failure.", e);
            if (getConfig().isFailClosedOnApiError()) throw new IdentityBrokerException("Security verification failed. Unable to reach Steam servers to verify ban status.");
        }
    }

    protected com.fasterxml.jackson.databind.JsonNode fetchBanPayloadFromSteam(String url) throws java.io.IOException {
        return org.keycloak.http.simple.SimpleHttp.create(session).doGet(url).asJson();
    }

    protected void checkSteamLevel(String steamId64) {
        if (getConfig().getMinSteamLevel() <= 0) {
            return;
        }

        try {
            String apiKey = getConfig().getSteamApiKey();
                String url = STEAM_PLAYER_LEVEL_URL
                    + "?key=" + apiKey
                    + "&steamid=" + steamId64;

            com.fasterxml.jackson.databind.JsonNode payload = fetchSteamLevelFromSteam(url);
            com.fasterxml.jackson.databind.JsonNode response = payload.get("response");

            if (response == null || !response.has("player_level")) {
                org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                        .warn("Steam Level API returned no player_level field. Bypassing check.");
                return;
            }

            int playerLevel = response.get("player_level").asInt();

            if (playerLevel < getConfig().getMinSteamLevel()) {
                throw new IdentityBrokerException(
                    "Login denied: Your Steam level (" + playerLevel + ") is below the minimum required level (" + getConfig().getMinSteamLevel() + ")."
                );
            }

        } catch (IdentityBrokerException e) {
            throw e;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                    .warn("Steam Level API returned an unrecognized payload structure.", e);
            if (getConfig().isFailClosedOnApiError()) throw new IdentityBrokerException("Security verification failed due to malformed Steam API response.");
        } catch (java.io.IOException e) {
            org.jboss.logging.Logger.getLogger(SteamIdentityProvider.class)
                    .warn("Steam Level API unreachable due to transport failure.", e);
            if (getConfig().isFailClosedOnApiError()) throw new IdentityBrokerException("Security verification failed. Unable to reach Steam servers to verify account level.");
        }
    }

    protected com.fasterxml.jackson.databind.JsonNode fetchSteamLevelFromSteam(String url) throws java.io.IOException {
        return SimpleHttp.create(session).doGet(url).asJson();
    }
}
