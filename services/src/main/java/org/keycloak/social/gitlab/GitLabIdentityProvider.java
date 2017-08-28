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

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GitLabIdentityProvider extends OIDCIdentityProvider  implements SocialIdentityProvider<OIDCIdentityProviderConfig> {

	public static final String AUTH_URL = "https://gitlab.com/oauth/authorize";
	public static final String TOKEN_URL = "https://gitlab.com/oauth/token";
	public static final String USER_INFO = "https://gitlab.com/api/v4/user";
	public static final String API_SCOPE = "api";

	public GitLabIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(USER_INFO);

		String defaultScope = config.getDefaultScope();

		if (defaultScope.equals(SCOPE_OPENID)) {
			config.setDefaultScope((API_SCOPE + " " + defaultScope).trim());
		}
	}

	protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
		String id = idToken.getSubject();
		BrokeredIdentityContext identity = new BrokeredIdentityContext(id);
		String name = (String)idToken.getOtherClaims().get(IDToken.NAME);
		String preferredUsername = (String)idToken.getOtherClaims().get(IDToken.NICKNAME);
		String email = (String)idToken.getOtherClaims().get(IDToken.EMAIL);

		if (getConfig().getDefaultScope().contains(API_SCOPE)) {
			String userInfoUrl = getUserInfoUrl();
			if (userInfoUrl != null && !userInfoUrl.isEmpty() && (id == null || name == null || preferredUsername == null || email == null)) {
				JsonNode userInfo = SimpleHttp.doGet(userInfoUrl, session)
						.header("Authorization", "Bearer " + accessToken).asJson();

				name = getJsonProperty(userInfo, "name");
				preferredUsername = getJsonProperty(userInfo, "username");
				email = getJsonProperty(userInfo, "email");
				AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, userInfo, getConfig().getAlias());
			}
		}
		identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
		identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);
		processAccessTokenResponse(identity, tokenResponse);

		identity.setId(id);
		identity.setName(name);
		identity.setEmail(email);

		identity.setBrokerUserId(getConfig().getAlias() + "." + id);
		if (tokenResponse.getSessionState() != null) {
			identity.setBrokerSessionId(getConfig().getAlias() + "." + tokenResponse.getSessionState());
		}

		if (preferredUsername == null) {
			preferredUsername = email;
		}

		if (preferredUsername == null) {
			preferredUsername = id;
		}

		identity.setUsername(preferredUsername);
		return identity;
	}




}
