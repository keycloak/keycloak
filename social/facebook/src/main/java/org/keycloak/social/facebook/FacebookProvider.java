package org.keycloak.social.facebook;

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
public class FacebookProvider extends AbstractOAuth2Provider {

    private static final String ID = "facebook";
    private static final String NAME = "Facebook";

    private static final String AUTH_URL = "https://graph.facebook.com/oauth/authorize";
    private static final String TOKEN_URL = "https://graph.facebook.com/oauth/access_token";
    private static final String PROFILE_URL = "https://graph.facebook.com/me";

    private static final String DEFAULT_SCOPE = "email";

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

            SocialUser user = new SocialUser(profile.getString("id"));
            user.setName(profile.optString("first_name"), profile.optString("last_name"));
            user.setEmail(profile.optString("email"));

            return user;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

    @Override
    public AuthRequest getAuthUrl(SocialProviderConfig config) throws SocialProviderException {
        if (config.getCallbackUrl().contains("//localhost")) {
            String callbackUrl = config.getCallbackUrl().replace("//localhost", "//127.0.0.1");
            config = new SocialProviderConfig(config.getKey(), config.getSecret(), callbackUrl);
        }
        return super.getAuthUrl(config);
    }

}
