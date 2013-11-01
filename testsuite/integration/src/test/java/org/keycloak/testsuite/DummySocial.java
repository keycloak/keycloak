package org.keycloak.testsuite;

import java.util.UUID;

import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;

public class DummySocial implements SocialProvider {

    private static final String AUTH_PATH = "http://localhost:8081/dummy-social/auth";

    @Override
    public String getId() {
        return "dummy";
    }

    @Override
    public AuthRequest getAuthUrl(SocialProviderConfig config) throws SocialProviderException {
        String state = UUID.randomUUID().toString();

        return AuthRequest.create(state, AUTH_PATH).setQueryParam("response_type", "token")
                .setQueryParam("redirect_uri", config.getCallbackUrl()).setQueryParam("state", state).setAttribute("state", state).build();
    }

    @Override
    public String getRequestIdParamName() {
        return "state";
    }

    @Override
    public String getName() {
        return "Dummy Provider";
    }

    @Override
    public SocialUser processCallback(SocialProviderConfig config, AuthCallback callback) throws SocialProviderException {
        if (!callback.getQueryParam("state").equals(callback.getAttribute("state"))) {
            throw new SocialProviderException("Invalid state");
        }

        String username = callback.getQueryParam("access_token");
        SocialUser user = new SocialUser(username);
        user.setEmail(username + "@dummy-social");
        user.setUsername(username);
        return user;
    }

}
