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

package org.keycloak.social.bitbucket;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BitbucketIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://bitbucket.org/site/oauth2/authorize";
	public static final String TOKEN_URL = "https://bitbucket.org/site/oauth2/access_token";
	public static final String USER_URL = "https://api.bitbucket.org/2.0/user";
	public static final String EMAIL_SCOPE = "email";
	public static final String ACCOUNT_SCOPE = "account";
	public static final String DEFAULT_SCOPE = ACCOUNT_SCOPE;

	public BitbucketIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		String defaultScope = config.getDefaultScope();

		if (defaultScope ==  null || defaultScope.trim().equals("")) {
			config.setDefaultScope(ACCOUNT_SCOPE);
		}
	}

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = SimpleHttp.doGet(USER_URL, session).header("Authorization", "Bearer " + accessToken).asJson();

			String type = getJsonProperty(profile, "type");
			if (type == null) {
				throw new IdentityBrokerException("Could not obtain account information from bitbucket.");

			}
			if (type.equals("error")) {
				JsonNode errorNode = profile.get("error");
				if (errorNode != null) {
					String errorMsg = getJsonProperty(errorNode, "message");
					throw new IdentityBrokerException("Could not obtain account information from bitbucket.  Error: " + errorMsg);
				} else {
					throw new IdentityBrokerException("Could not obtain account information from bitbucket.");
				}
			}
			if (!type.equals("user")) {
				logger.debug("Unknown object type: " + type);
				throw new IdentityBrokerException("Could not obtain account information from bitbucket.");

			}
			BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "account_id"));

			String username = getJsonProperty(profile, "username");
			user.setUsername(username);
			user.setIdpConfig(getConfig());
			user.setIdp(this);

			AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

			return user;
		} catch (Exception e) {
			if (e instanceof IdentityBrokerException) throw (IdentityBrokerException)e;
			throw new IdentityBrokerException("Could not obtain user profile from github.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
