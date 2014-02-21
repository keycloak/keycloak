package org.keycloak.social.github;

import org.json.JSONObject;
import org.keycloak.social.AbstractOAuth2Provider;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;
import org.keycloak.social.utils.SimpleHttp;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GitHubProvider extends AbstractOAuth2Provider {

    private static final String ID = "github";
    private static final String NAME = "GitHub";

    private static final String AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String PROFILE_URL = "https://api.github.com/user";

    private static final String DEFAULT_SCOPE = "user:email";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getScope() {
        return DEFAULT_SCOPE;
    }

    @Override
    protected String getAuthUrl() {
        return AUTH_URL;
    }

    @Override
    protected String getTokenUrl() {
        return TOKEN_URL;
    }

    @Override
    protected SocialUser getProfile(String accessToken) throws SocialProviderException {
        try {
            JSONObject profile = SimpleHttp.doGet(PROFILE_URL).header("Authorization", "Bearer " + accessToken).asJson();

            SocialUser user = new SocialUser(profile.get("id").toString());
            user.setName(profile.optString("name"));
            user.setEmail(profile.optString("email"));

            return user;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

}
