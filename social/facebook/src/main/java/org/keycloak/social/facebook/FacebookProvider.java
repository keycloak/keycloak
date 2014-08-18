package org.keycloak.social.facebook;

import org.codehaus.jackson.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.social.AbstractOAuth2Provider;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;
import org.keycloak.social.utils.SimpleHttp;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FacebookProvider extends AbstractOAuth2Provider {
    protected static final Logger logger = Logger.getLogger(FacebookProvider.class);

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
            JsonNode profile = SimpleHttp.doGet(PROFILE_URL).header("Authorization", "Bearer " + accessToken).asJson();


            JsonNode id = profile.get("id");
            JsonNode username = profile.get("username");
            JsonNode email = profile.get("email");

            //logger.info("email is null: " + email == null);
            //logger.info("username is null: " + username == null);

            if (username == null) username = email == null ? id : email;

            SocialUser user = new SocialUser(id.getTextValue(), username.getTextValue());
            user.setName(profile.has("first_name") ? profile.get("first_name").getTextValue() : null,
                    profile.has("last_name") ? profile.get("last_name").getTextValue() : null);
            user.setEmail(profile.has("email") ? email.getTextValue() : null);

            return user;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

}
