package org.keycloak.social.facebook;

import org.codehaus.jackson.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.oidc.util.JsonSimpleHttp;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.social.SocialIdentityProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FacebookIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://graph.facebook.com/oauth/authorize";
	public static final String TOKEN_URL = "https://graph.facebook.com/oauth/access_token";
	public static final String PROFILE_URL = "https://graph.facebook.com/me";
	public static final String DEFAULT_SCOPE = "email";

	public FacebookIdentityProvider(OAuth2IdentityProviderConfig config) {
		super(config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = JsonSimpleHttp.asJson(SimpleHttp.doGet(PROFILE_URL).header("Authorization", "Bearer " + accessToken));

			String id = getJsonProperty(profile, "id");

			BrokeredIdentityContext user = new BrokeredIdentityContext(id);

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
			} else {
				lastName = " " + lastName;
			}

			user.setName(firstName + lastName);
			user.setIdpConfig(getConfig());
			user.setIdp(this);

			AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from facebook.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
