/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.social.discord;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * @author <a href="mailto:me@prok.pw">Sergey Shatunov</a>
 */
public class DiscordIdentityProvider extends AbstractOAuth2IdentityProvider<DiscordIdentityProviderConfig>
        implements SocialIdentityProvider<DiscordIdentityProviderConfig> {

    public static final String AUTH_URL = "https://discord.com/api/oauth2/authorize";
    public static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    public static final String PROFILE_URL = "https://discord.com/api/users/@me";
    public static final String GUILDS_URL = "https://discord.com/api/users/@me/guilds";
    public static final String DEFAULT_SCOPE = "identify email";
    public static final String GUILDS_SCOPE = "guilds";

    public DiscordIdentityProvider(KeycloakSession session, DiscordIdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
    }

    @Override
    protected boolean supportsExternalExchange() {
        return true;
    }

    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        return PROFILE_URL;
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"));

        user.setUsername(getJsonProperty(profile, "username"));
        user.setEmail(getJsonProperty(profile, "email"));
        user.setIdpConfig(getConfig());
        user.setIdp(this);

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

        return user;
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            JsonNode profile = SimpleHttp.doGet(PROFILE_URL, session).header("Authorization", "Bearer " + accessToken).asJson();
            if (!isAllowedGuild(accessToken)) {
                throw new ErrorPageException(session, Response.Status.FORBIDDEN, Messages.INVALID_REQUESTER);
            }
            return extractIdentityFromProfile(null, profile);
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from discord.", e);
        }
    }

    protected boolean isAllowedGuild(String accessToken) {
        try {
            Set<Long> allowedGuilds = getConfig().getAllowedGuildIDs();
            if (allowedGuilds.isEmpty()) {
                return true;
            }
            JsonNode guilds = SimpleHttp.doGet(GUILDS_URL, session).header("Authorization", "Bearer " + accessToken).asJson();
            for (JsonNode guild : guilds) {
                Long guildId = Long.parseUnsignedLong(getJsonProperty(guild, "id"));
                if (allowedGuilds.contains(guildId)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain guilds the current user is a member of from discord.", e);
        }
    }

    @Override
    protected String getDefaultScopes() {
        if (getConfig().getAllowedGuildIDs().isEmpty()) {
            return DEFAULT_SCOPE;
        }
        return DEFAULT_SCOPE + " " + GUILDS_SCOPE;
    }
}
