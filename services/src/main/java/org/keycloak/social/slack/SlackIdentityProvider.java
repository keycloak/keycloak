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

package org.keycloak.social.slack;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;

/**
 * @author Leopold Schabel
 */
public class SlackIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://slack.com/oauth/authorize";
	public static final String TOKEN_URL = "https://slack.com/api/oauth.access";
	public static final String PROFILE_URL = "https://slack.com/api/users.identity";
	public static final String DEFAULT_SCOPE = "identity.basic identity.email identity.team";

	public SlackIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode raw = SimpleHttp.doGet(PROFILE_URL,session).param("token", accessToken).asJson();
			
			JsonNode user = raw.get("user");
			JsonNode team = raw.get("team");

			logger.debug(user.toString());

			String id = getJsonProperty(user, "id");
			String team_id = getJsonProperty(team, "id");

			BrokeredIdentityContext profile = new BrokeredIdentityContext(String.format("%s-%s", team_id, id));

			String displayname = getJsonProperty(user, "name");

			String email = getJsonProperty(user, "email");

			profile.setUsername(displayname);
			profile.setEmail(email);
			profile.setIdpConfig(getConfig());
			profile.setIdp(this);

			AbstractJsonUserAttributeMapper.storeUserProfileForMapper(profile, user, getConfig().getAlias());

			return profile;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from slack.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
