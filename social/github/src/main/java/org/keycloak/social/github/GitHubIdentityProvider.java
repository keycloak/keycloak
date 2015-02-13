package org.keycloak.social.github;

import org.codehaus.jackson.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.broker.provider.IdentityBrokerException;
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
    protected FederatedIdentity doGetFederatedIdentity(String accessToken) {
        try {
            JsonNode profile = SimpleHttp.doGet(PROFILE_URL).header("Authorization", "Bearer " + accessToken).asJson();

            FederatedIdentity user = new FederatedIdentity(getJsonProperty(profile, "id"));

            user.setUsername(getJsonProperty(profile, "login"));
            user.setName(getJsonProperty(profile, "name"));
            user.setEmail(getJsonProperty(profile, "email"));

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
