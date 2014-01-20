package org.keycloak.social.facebook;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.UUID;

/**
 * Social provider for Facebook
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookProvider implements SocialProvider {

    private static final String AUTHENTICATION_ENDPOINT_URL = "https://graph.facebook.com/oauth/authorize";

    private static final String ACCESS_TOKEN_ENDPOINT_URL = "https://graph.facebook.com/oauth/access_token";

    private static final String PROFILE_ENDPOINT_URL = "https://graph.facebook.com/me";

    private static final String DEFAULT_RESPONSE_TYPE = "code";

    private static final String DEFAULT_SCOPE = "email";

    @Override
    public String getId() {
        return "facebook";
    }

    @Override
    public AuthRequest getAuthUrl(SocialProviderConfig config) throws SocialProviderException {
        String state = UUID.randomUUID().toString();

        String redirectUri = config.getCallbackUrl();
        redirectUri = redirectUri.replace("//localhost", "//127.0.0.1");

        return AuthRequest.create(state, AUTHENTICATION_ENDPOINT_URL).setQueryParam("client_id", config.getKey())
                .setQueryParam("response_type", DEFAULT_RESPONSE_TYPE).setQueryParam("scope", DEFAULT_SCOPE)
                .setQueryParam("redirect_uri", redirectUri).setQueryParam("state", state).setAttribute("state", state).build();
    }

    @Override
    public String getRequestIdParamName() {
        return "state";
    }

    @Override
    public String getName() {
        return "Facebook";
    }

    @Override
    public SocialUser processCallback(SocialProviderConfig config, AuthCallback callback) throws SocialProviderException {
        String code = callback.getQueryParam(DEFAULT_RESPONSE_TYPE);

        try {
            if (!callback.getQueryParam("state").equals(callback.getAttribute("state"))) {
                throw new SocialProviderException("Invalid state");
            }

            ResteasyClient client = new ResteasyClientBuilder()
                    .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY).build();

            String accessToken = loadAccessToken(code, config, client);

            FacebookUser facebookUser = loadUser(accessToken, client);

            SocialUser socialUser = new SocialUser(facebookUser.getId());
            socialUser.setUsername(facebookUser.getUsername());

            // This could happen with Facebook testing users
            if (facebookUser.getUsername() == null || facebookUser.getUsername().length() == 0) {
                socialUser.setUsername(facebookUser.getId());
            }

            socialUser.setEmail(facebookUser.getEmail());
            socialUser.setLastName(facebookUser.getLastName());
            socialUser.setFirstName(facebookUser.getFirstName());

            return socialUser;
        } catch (SocialProviderException spe) {
            throw spe;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

    protected String loadAccessToken(String code, SocialProviderConfig config, ResteasyClient client) throws SocialProviderException {
        Form form = new Form();
        form.param("grant_type", "authorization_code")
                .param("code", code)
                .param("client_id", config.getKey())
                .param("client_secret", config.getSecret())
                .param("redirect_uri", config.getCallbackUrl());

        Response response = client.target(ACCESS_TOKEN_ENDPOINT_URL).request().post(Entity.form(form));

        if (response.getStatus() != 200) {
            String errorTokenResponse = response.readEntity(String.class);
            throw new SocialProviderException("Access token request to Facebook failed. Status: " + response.getStatus() + ", response: " + errorTokenResponse);
        }

        String accessTokenResponse = response.readEntity(String.class);
        return parseParameter(accessTokenResponse, "access_token");
    }

    protected FacebookUser loadUser(String accessToken, ResteasyClient client) throws SocialProviderException {
        URI userDetailsUri = UriBuilder.fromUri(PROFILE_ENDPOINT_URL)
                .queryParam("access_token", accessToken)
                .queryParam("fields", "id,name,username,first_name,last_name,email")
                .build();

        Response response = client.target(userDetailsUri).request()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .get();
        if (response.getStatus() != 200) {
            String errorTokenResponse = response.readEntity(String.class);
            throw new SocialProviderException("Request to Facebook for obtaining user failed. Status: " + response.getStatus() + ", response: " + errorTokenResponse);
        }

        return response.readEntity(FacebookUser.class);
    }

    // Parses value of given parameter from input string like "my_param=abcd&another_param=xyz"
    private String parseParameter(String input, String paramName) {
        int start = input.indexOf(paramName + "=");
        if (start != -1) {
            input = input.substring(start + paramName.length() + 1);
            int end = input.indexOf("&");
            return end==-1 ? input : input.substring(0, end);
        } else {
            throw new IllegalArgumentException("Parameter " + paramName + " not available in response " + input);
        }

    }
}
