package org.keycloak.social.facebook;

import org.codehaus.jackson.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.social.SocialIdentityProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FacebookIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    private static final String ID = "facebook";
    private static final String AUTH_URL = "https://graph.facebook.com/oauth/authorize";
    private static final String TOKEN_URL = "https://graph.facebook.com/oauth/access_token";
    private static final String PROFILE_URL = "https://graph.facebook.com/me";
    private static final String DEFAULT_SCOPE = "email";

    public FacebookIdentityProvider(OAuth2IdentityProviderConfig config) {
        super(config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
        config.setDefaultScope(DEFAULT_SCOPE);
    }

    @Override
    protected FederatedIdentity getFederatedIdentity(String accessToken) {
        try {
            JsonNode profile = SimpleHttp.doGet(PROFILE_URL).header("Authorization", "Bearer " + accessToken).asJson();

            String id = getJsonProperty(profile, "id");

            FederatedIdentity user = new FederatedIdentity(id);

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

            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
