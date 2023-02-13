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

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;

import org.keycloak.services.validation.Validation;

/**
 * 
 * Identity provider for Microsoft account. Uses OAuth 2 protocol of Microsoft Graph as documented at
 * <a href="https://docs.microsoft.com/en-us/onedrive/developer/rest-api/getting-started/graph-oauth">https://docs.microsoft.com/en-us/onedrive/developer/rest-api/getting-started/graph-oauth</a>
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class MicrosoftIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    private static final Logger log = Logger.getLogger(MicrosoftIdentityProvider.class);

    public static final String AUTH_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"; // authorization code endpoint
    public static final String TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token"; // token endpoint
    public static final String PROFILE_URL = "https://graph.microsoft.com/v1.0/me/"; // user profile service endpoint
    public static final String DEFAULT_SCOPE = "User.read"; // the User.read scope should be sufficient to obtain all necessary user info

    public MicrosoftIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
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
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            JsonNode profile = SimpleHttp.doGet(PROFILE_URL, session).auth(accessToken).asJson();
            return extractIdentityFromProfile(null, profile);
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from Microsoft Graph", e);
        }
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        String id = getJsonProperty(profile, "id");
        BrokeredIdentityContext user = new BrokeredIdentityContext(id);

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
        user.setIdpConfig(getConfig());
        user.setIdp(this);

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
        return user;
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }
}
