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

package org.keycloak.social.gitlab;

import java.io.IOException;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorResponseException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GitLabIdentityProvider extends OIDCIdentityProvider  implements SocialIdentityProvider<OIDCIdentityProviderConfig> {

	public static final String AUTH_URL = "https://gitlab.com/oauth/authorize";
	public static final String TOKEN_URL = "https://gitlab.com/oauth/token";
	public static final String USER_INFO = "https://gitlab.com/api/v4/user";
	public static final String READ_USER_SCOPE = "read_user";

	public GitLabIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(USER_INFO);

		String defaultScope = config.getDefaultScope();

		if (defaultScope.equals(SCOPE_OPENID)) {
			config.setDefaultScope((READ_USER_SCOPE + " " + defaultScope).trim());
		}
	}

	protected String getUsernameFromUserInfo(JsonNode userInfo) {
		return getJsonProperty(userInfo, "username");
	}

	protected String getusernameClaimNameForIdToken() {
		return IDToken.NICKNAME;
	}

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return getUserInfoUrl();
	}

	@Override
	public boolean isIssuer(String issuer, MultivaluedMap<String, String> params) {
		String requestedIssuer = params.getFirst(OAuth2Constants.SUBJECT_ISSUER);
		if (requestedIssuer == null) requestedIssuer = issuer;
		return requestedIssuer.equals(getConfig().getAlias());
	}


	@Override
	protected BrokeredIdentityContext exchangeExternalTokenV1Impl(EventBuilder event, MultivaluedMap<String, String> params) {
		return exchangeExternalUserInfoValidationOnly(event, params);
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
		String id = getJsonProperty(profile, "id");
		if (id == null) {
			event.detail(Details.REASON, "id claim is null from user info json");
			event.error(Errors.INVALID_TOKEN);
			throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
		}
		return gitlabExtractFromProfile(profile);
	}

	private BrokeredIdentityContext gitlabExtractFromProfile(JsonNode profile) {
		String id = getJsonProperty(profile, "id");
		BrokeredIdentityContext identity = new BrokeredIdentityContext(id, getConfig());

		String name = getJsonProperty(profile, "name");
		String preferredUsername = getJsonProperty(profile, "username");
		String email = getJsonProperty(profile, "email");
		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, profile, getConfig().getAlias());

		identity.setId(id);
		identity.setName(name);
		identity.setEmail(email);

		identity.setBrokerUserId(getConfig().getAlias() + "." + id);

		if (preferredUsername == null) {
			preferredUsername = email;
		}

		if (preferredUsername == null) {
			preferredUsername = id;
		}

		identity.setUsername(preferredUsername);
		return identity;
	}


	protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
		SimpleHttpResponse response = null;
		int status = 0;

		for (int i = 0; i < 10; i++) {
			try {
				String userInfoUrl = getUserInfoUrl();
				response = SimpleHttp.create(session).doGet(userInfoUrl)
						.header("Authorization", "Bearer " + accessToken).asResponse();
				status = response.getStatus();
			} catch (IOException e) {
				logger.debug("Failed to invoke user info for external exchange", e);
			}
			if (status == 200) break;
			response.close();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if (status != 200) {
			logger.debug("Failed to invoke user info status: " + status);
			throw new IdentityBrokerException("Gitlab user info call failure");
		}
		JsonNode profile = null;
		try {
			profile = response.asJson();
		} catch (IOException e) {
			throw new IdentityBrokerException("Gitlab user info call failure");
		}
		String id = getJsonProperty(profile, "id");
		if (id == null) {
			throw new IdentityBrokerException("Gitlab id claim is null from user info json");
		}
		BrokeredIdentityContext identity = gitlabExtractFromProfile(profile);
		identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
		identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);
		processAccessTokenResponse(identity, tokenResponse);

		return identity;
	}






}
