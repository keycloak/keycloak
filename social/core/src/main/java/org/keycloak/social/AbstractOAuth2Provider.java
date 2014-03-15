package org.keycloak.social;

import org.json.JSONObject;
import org.keycloak.OAuth2Constants;
import org.keycloak.social.utils.SimpleHttp;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractOAuth2Provider implements SocialProvider {

    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String CODE = "code";
    private static final String GRANT_TYPE = "grant_type";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String RESPONSE_TYPE = "response_type";
    private static final String SCOPE = "scope";
    private static final String STATE = "state";

    private static final String TOKEN_REGEX = "access_token=([^&]+)";

    @Override
    public abstract String getId();

    @Override
    public abstract String getName();

    protected abstract String getScope();

    protected abstract String getAuthUrl();

    protected abstract String getTokenUrl();

    protected abstract SocialUser getProfile(String accessToken) throws SocialProviderException;

    @Override
    public AuthRequest getAuthUrl(SocialProviderConfig config) throws SocialProviderException {
        String state = UUID.randomUUID().toString();

        return AuthRequest.create(state, getAuthUrl()).setQueryParam(CLIENT_ID, config.getKey())
                .setQueryParam(RESPONSE_TYPE, CODE).setQueryParam(SCOPE, getScope())
                .setQueryParam(REDIRECT_URI, config.getCallbackUrl()).setQueryParam(STATE, state).setAttribute(STATE, state).build();
    }

    @Override
    public SocialUser processCallback(SocialProviderConfig config, AuthCallback callback) throws SocialProviderException {
        String error = callback.getQueryParam(OAuth2Constants.ERROR);
        if (error != null) {
            if (error.equals("access_denied")) {
                throw new SocialAccessDeniedException();
            } else {
                throw new SocialProviderException(error);
            }
        }

        try {
            String code = callback.getQueryParam(CODE);

            if (!callback.getQueryParam(STATE).equals(callback.getAttribute(STATE))) {
                throw new SocialProviderException("Invalid state");
            }

            String response = SimpleHttp.doPost(getTokenUrl()).param(CODE, code).param(CLIENT_ID, config.getKey())
                    .param(CLIENT_SECRET, config.getSecret())
                    .param(REDIRECT_URI, config.getCallbackUrl())
                    .param(GRANT_TYPE, AUTHORIZATION_CODE).asString();

            String accessToken;

            if (response.startsWith("{")) {
                accessToken = new JSONObject(response).getString(ACCESS_TOKEN);
            } else {
                Matcher matcher = Pattern.compile(TOKEN_REGEX).matcher(response);
                if (matcher.find()) {
                    accessToken = matcher.group(1);
                } else {
                    throw new SocialProviderException("Invalid response, could not find token");
                }
            }

            return getProfile(accessToken);
        } catch (IOException e) {
            throw new SocialProviderException(e);
        }
    }

}
