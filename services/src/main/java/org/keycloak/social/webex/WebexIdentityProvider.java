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

package org.keycloak.social.webex;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:daniele.strollo@gmail.com">Daniele Strollo</a>
 */
public class WebexIdentityProvider extends AbstractOAuth2IdentityProvider<OIDCIdentityProviderConfig> implements SocialIdentityProvider<OIDCIdentityProviderConfig>{

	public static final String AUTH_URL = "https://webexapis.com/v1/authorize";
	public static final String TOKEN_URL = "https://webexapis.com/v1/access_token";
	public static final String USER_INFO = "https://webexapis.com/v1/people/me";
	public static final String WEBEX_DEFAULT_SCOPE = "spark:people_read";
    public String api_scope = null;

	public WebexIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(USER_INFO);

        // Check user filled form field "default scopes" otherwise use pre build ones
        String defaultScope = config.getDefaultScope();

		if (defaultScope ==  null || defaultScope.trim().equals("")) {
			config.setDefaultScope(WEBEX_DEFAULT_SCOPE);
		}
        this.api_scope = config.getDefaultScope();
	}

	@Override
    protected String getDefaultScopes() {
        return this.api_scope;
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            final JsonNode profile = fetchProfile(accessToken);
            final BrokeredIdentityContext user = extractUserContext(profile);
            AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
            return user;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from Openshift.", e);
        }
    }

    private BrokeredIdentityContext extractUserContext(JsonNode profile) {
        final BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"));
        user.setUsername(getJsonProperty(profile, "userName"));
        user.setEmail(getJsonProperty(profile, "userName"));
        user.setName(getJsonProperty(profile, "displayName"));
        user.setIdpConfig(getConfig());
        user.setIdp(this);
        return user;
    }

    private JsonNode fetchProfile(String accessToken) throws IOException {
        return SimpleHttp.doGet(getConfig().getUserInfoUrl(), this.session)
                .header("Authorization", "Bearer " + accessToken)
                .asJson();
    }


    @Override
    protected boolean supportsExternalExchange() {
        return true;
    }

    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        return getConfig().getUserInfoUrl();
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        final BrokeredIdentityContext user = extractUserContext(profile);
        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
        return user;
    }
}
