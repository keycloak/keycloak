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

package org.keycloak.social.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Iterator;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GitHubIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://github.com/login/oauth/authorize";
	public static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
	public static final String PROFILE_URL = "https://api.github.com/user";
	public static final String EMAIL_URL = "https://api.github.com/user/emails";
	public static final String DEFAULT_SCOPE = "user:email";

	public GitHubIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
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

		String username = getJsonProperty(profile, "login");
		user.setUsername(username);
		user.setName(getJsonProperty(profile, "name"));
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

			BrokeredIdentityContext user = extractIdentityFromProfile(null, profile);

			if (user.getEmail() == null) {
				user.setEmail(searchEmail(accessToken));
			}

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from github.", e);
		}
	}

	private String searchEmail(String accessToken) {
		try {
			ArrayNode emails = (ArrayNode) SimpleHttp.doGet(EMAIL_URL, session).header("Authorization", "Bearer " + accessToken).asJson();

			Iterator<JsonNode> loop = emails.elements();
			while (loop.hasNext()) {
				JsonNode mail = loop.next();
				if (mail.get("primary").asBoolean()) {
					return getJsonProperty(mail, "email");
				}
			}
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user email from github.", e);
		}
		throw new IdentityBrokerException("Primary email from github is not found.");
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
