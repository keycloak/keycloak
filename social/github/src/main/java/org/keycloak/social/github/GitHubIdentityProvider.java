package org.keycloak.social.github;

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
public class GitHubIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://github.com/login/oauth/authorize";
	public static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
	public static final String PROFILE_URL = "https://api.github.com/user";
	public static final String DEFAULT_SCOPE = "user:email";

	public GitHubIdentityProvider(OAuth2IdentityProviderConfig config) {
		super(config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = JsonSimpleHttp.asJson(SimpleHttp.doGet(PROFILE_URL).header("Authorization", "Bearer " + accessToken));

			BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"));

			String username = getJsonProperty(profile, "login");
			user.setUsername(username);
			user.setName(getJsonProperty(profile, "name"));
			user.setEmail(getJsonProperty(profile, "email"));
			user.setIdpConfig(getConfig());
			user.setIdp(this);

			AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from github.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
