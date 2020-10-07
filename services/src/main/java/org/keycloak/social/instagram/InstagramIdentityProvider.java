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

package org.keycloak.social.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InstagramIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://api.instagram.com/oauth/authorize";
	public static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
	public static final String PROFILE_URL = "https://graph.instagram.com/me";
	public static final String PROFILE_FIELDS = "id,username";
	public static final String DEFAULT_SCOPE = "user_profile";
	public static final String LEGACY_ID_FIELD = "ig_id";
	
	public InstagramIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			// try to get the profile incl. legacy Instagram ID to allow existing users to log in
			JsonNode profile = fetchUserProfile(accessToken, true);
			// ig_id field will get deprecated in the future and eventually might stop working (returning error)
			if (!profile.has("id")) {
				logger.debugf("Could not fetch user profile from instagram. Trying without %s.", LEGACY_ID_FIELD);
				profile = fetchUserProfile(accessToken, false);
			}
			
			logger.debug(profile.toString());

			// it's not documented whether the new ID system can or cannot have conflicts with the legacy system, therefore
			// we're using a custom prefix just to be sure
			String id = "graph_" + getJsonProperty(profile, "id");
	  		String username = getJsonProperty(profile, "username");
			String legacyId = getJsonProperty(profile, LEGACY_ID_FIELD);

			BrokeredIdentityContext user = new BrokeredIdentityContext(id);
			user.setUsername(username);
			user.setIdpConfig(getConfig());
			user.setIdp(this);
			if (legacyId != null && !legacyId.isEmpty()) {
				user.setLegacyId(legacyId);
			}

			AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from instagram.", e);
		}
	}

	protected JsonNode fetchUserProfile(String accessToken, boolean includeIgId) throws IOException {
		String fields = PROFILE_FIELDS;
		if (includeIgId) {
			fields += "," + LEGACY_ID_FIELD;
		}

		return SimpleHttp.doGet(PROFILE_URL,session)
				.param("access_token", accessToken)
				.param("fields", fields)
				.asJson();
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
