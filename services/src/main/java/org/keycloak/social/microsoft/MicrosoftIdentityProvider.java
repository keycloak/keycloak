/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.social.microsoft;

import java.util.Optional;

import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.validation.Validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 *
 * Identity provider for Microsoft account. Uses OAuth 2 protocol of Microsoft Graph as documented at
 * <a href="https://docs.microsoft.com/en-us/onedrive/developer/rest-api/getting-started/graph-oauth">https://docs.microsoft.com/en-us/onedrive/developer/rest-api/getting-started/graph-oauth</a>
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class MicrosoftIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    private static final Logger log = Logger.getLogger(MicrosoftIdentityProvider.class);

    private static final String AUTH_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize"; // authorization code endpoint
    private static final String TOKEN_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/v2.0/token"; // token endpoint
    private static final String PROFILE_URL = "https://graph.microsoft.com/v1.0/me/"; // user profile service endpoint
    private static final String DEFAULT_SCOPE = "User.read"; // the User.read scope should be sufficient to obtain all necessary user info

    public MicrosoftIdentityProvider(KeycloakSession session, MicrosoftIdentityProviderConfig config) {
        super(session, config);

        // Use multi-tenant 'common' endpoints if not specified.
        String tenant = Optional.ofNullable(config.getTenantId()).map(String::trim).orElse("common");

        config.setAuthorizationUrl(String.format(AUTH_URL_TEMPLATE, tenant));
        config.setTokenUrl(String.format(TOKEN_URL_TEMPLATE, tenant));
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
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            JsonNode profile = SimpleHttp.create(session).doGet(PROFILE_URL).auth(accessToken).asJson();
            if (profile.has("error") && !profile.get("error").isNull()) {
                throw new IdentityBrokerException("Error in Microsoft Graph API response. Payload: " + profile.toString());
            }
            return extractIdentityFromProfile(null, profile);
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from Microsoft Graph", e);
        }
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        String id = getJsonProperty(profile, "id");
        BrokeredIdentityContext user = new BrokeredIdentityContext(id, getConfig());

        String email = getJsonProperty(profile, "mail");
        if (email == null && profile.has("userPrincipalName")) {
            String username = getJsonProperty(profile, "userPrincipalName");
            if (Validation.isEmailValid(username)) {
                email = username;
            }
        }
        user.setUsername(email != null ? email : id);
        user.setFirstName(getJsonProperty(profile, "givenName"));
        user.setLastName(getJsonProperty(profile, "surname"));
        if (email != null)
            user.setEmail(email);
        user.setIdp(this);

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
        return user;
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }
}
