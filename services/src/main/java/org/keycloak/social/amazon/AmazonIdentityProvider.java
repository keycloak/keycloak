/*
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

package org.keycloak.social.amazon;

import com.fasterxml.jackson.databind.JsonNode;
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
 * @author x-falcon
 */
public class AmazonIdentityProvider extends AbstractOAuth2IdentityProvider<OAuth2IdentityProviderConfig> implements SocialIdentityProvider<OAuth2IdentityProviderConfig>  {

	public static final String AUTH_URL = "https://www.amazon.com/ap/oa";
	public static final String TOKEN_URL = "https://api.amazon.com/auth/o2/token";
	public static final String PROFILE_URL = "https://api.amazon.com/user/profile";
	public static final String DEFAULT_SCOPE = "profile";

	public AmazonIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
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
		BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "user_id"));

		user.setUsername(getJsonProperty(profile, "email"));
		String name = getJsonProperty(profile, "name");
		String[] names = name.split(" ");
		if(names.length>1){
			user.setFirstName(names[0]);
			user.setLastName(names[1]);
		}
		user.setEmail(getJsonProperty(profile, "email"));
		user.setIdpConfig(getConfig());
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;

	}


	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = SimpleHttp.doGet(PROFILE_URL, session).param("access_token", accessToken).asJson();

			BrokeredIdentityContext user = extractIdentityFromProfile(null, profile);

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from baidu.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
