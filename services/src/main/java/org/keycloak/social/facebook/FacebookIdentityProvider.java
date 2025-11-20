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

package org.keycloak.social.facebook;

import java.io.IOException;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.services.ErrorResponseException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FacebookIdentityProvider extends AbstractOAuth2IdentityProvider<FacebookIdentityProviderConfig> implements SocialIdentityProvider<FacebookIdentityProviderConfig> {

	public static final String AUTH_URL = "https://graph.facebook.com/oauth/authorize";
	public static final String TOKEN_URL = "https://graph.facebook.com/oauth/access_token";
	public static final String PROFILE_URL = "https://graph.facebook.com/me?fields=id,name,email,first_name,last_name";
    public static final String DEBUG_TOKEN_URL = "https://graph.facebook.com/debug_token";
	public static final String DEFAULT_SCOPE = "email";
	protected static final String PROFILE_URL_FIELDS_SEPARATOR = ",";

	public FacebookIdentityProvider(KeycloakSession session, FacebookIdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			final String fetchedFields = getConfig().getFetchedFields();
			final String url = StringUtil.isNotNull(fetchedFields)
					? String.join(PROFILE_URL_FIELDS_SEPARATOR, PROFILE_URL, fetchedFields)
					: PROFILE_URL;
			JsonNode profile = SimpleHttp.create(session).doGet(url).header("Authorization", "Bearer " + accessToken).asJson();
			return extractIdentityFromProfile(null, profile);
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from facebook.", e);
		}
	}

    private void verifyToken(String accessToken) throws IOException {
        JsonNode response = SimpleHttp.create(session).doGet(DEBUG_TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getConfig().getClientId() + "|" + getConfig().getClientSecret())
                .param("input_token", accessToken)
                .asJson();

        JsonNode errorNode = response.get("error");
        if (errorNode != null) {
            String errorMessage = getJsonProperty(errorNode, "message");
            throw new RuntimeException("Error message:  " + errorMessage);
        }

        JsonNode dataNode = response.get("data");
        if (dataNode == null || dataNode.isNull()) {
            throw new RuntimeException("Invalid token debug response: 'data' field is missing.");
        }

        String appId = getJsonProperty(dataNode, "app_id");
        if (!getConfig().getClientId().equals(appId)) {
            throw new RuntimeException("Client ID does not match the app_id in the access token debug response.");
        }
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
		String id = getJsonProperty(profile, "id");

		BrokeredIdentityContext user = new BrokeredIdentityContext(id, getConfig());

		String email = getJsonProperty(profile, "email");

		user.setEmail(email);

		String username = getJsonProperty(profile, "username");

		if (username == null) {
            if (email != null) {
                username = email;
            } else {
                username = id;
            }
        }

		user.setUsername(username);

		String firstName = getJsonProperty(profile, "first_name");
		String lastName = getJsonProperty(profile, "last_name");

		if (lastName == null) {
		    lastName = "";
		}

		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}

    @Override
    protected BrokeredIdentityContext exchangeExternalTokenV2Impl(TokenExchangeContext tokenExchangeContext) {
        String subjectToken = tokenExchangeContext.getFormParams().getFirst(OAuth2Constants.SUBJECT_TOKEN);
        if (subjectToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "token not set", Response.Status.BAD_REQUEST);
        }
        try {
            verifyToken(subjectToken);
            return doGetFederatedIdentity(subjectToken);
        }
        catch (Exception e) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
