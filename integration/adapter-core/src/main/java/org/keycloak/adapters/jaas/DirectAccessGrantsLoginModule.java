package org.keycloak.adapters.jaas;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.VerificationException;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

/**
 * Login module based on Resource Owner password credentials grant from OAuth2 specs. It's supposed to be used in environments. which
 * can't rely on HTTP (like SSH authentication for instance). It needs that Direct Grant is enabled on particular realm in Keycloak.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirectAccessGrantsLoginModule extends AbstractKeycloakLoginModule {

    private static final Logger log = Logger.getLogger(DirectAccessGrantsLoginModule.class);

    private String refreshToken;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);

        // This is used just for logout
        Iterator<RefreshTokenHolder> iterator = subject.getPrivateCredentials(RefreshTokenHolder.class).iterator();
        if (iterator.hasNext()) {
            refreshToken = iterator.next().refreshToken;
        }
    }

    @Override
    protected Auth doAuth(String username, String password) throws IOException, VerificationException {
        return directGrantAuth(username, password);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    protected Auth directGrantAuth(String username, String password) throws IOException, VerificationException {
        String authServerBaseUrl = deployment.getAuthServerBaseUrl();
        URI directGrantUri = KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.TOKEN_PATH).build(deployment.getRealm());
        HttpPost post = new HttpPost(directGrantUri);

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
        formparams.add(new BasicNameValuePair("username", username));
        formparams.add(new BasicNameValuePair("password", password));

        if (deployment.isPublicClient()) { // if client is public access type
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, deployment.getResourceName()));
        } else {
            String clientId = deployment.getResourceName();
            String clientSecret = deployment.getResourceCredentials().get("secret");
            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);
        }
        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);

        HttpClient client = deployment.getClient();
        HttpResponse response = client.execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 200) {
            StringBuilder errorBuilder = new StringBuilder("Login failed. Invalid status: " + status);
            if (entity != null) {
                InputStream is = entity.getContent();
                Map<String, String> errors = (Map<String, String>) JsonSerialization.readValue(is, Map.class);
                errorBuilder.append(", OAuth2 error. Error: " + errors.get(OAuth2Constants.ERROR))
                        .append(", Error description: " + errors.get(OAuth2Constants.ERROR_DESCRIPTION));
            }
            String error = errorBuilder.toString();
            log.warn(error);
            throw new IOException(error);
        }

        if (entity == null) {
            throw new IOException("No Entity");
        }

        InputStream is = entity.getContent();
        AccessTokenResponse tokenResponse = JsonSerialization.readValue(is, AccessTokenResponse.class);

        // refreshToken will be saved to privateCreds of Subject for now
        refreshToken = tokenResponse.getRefreshToken();

        return bearerAuth(tokenResponse.getToken());
    }

    @Override
    public boolean commit() throws LoginException {
        boolean superCommit = super.commit();

        // refreshToken will be saved to privateCreds of Subject for now
        if (refreshToken != null) {
            RefreshTokenHolder refreshTokenHolder = new RefreshTokenHolder();
            refreshTokenHolder.refreshToken = refreshToken;
            subject.getPrivateCredentials().add(refreshTokenHolder);
        }

        return superCommit;
    }

    @Override
    public boolean logout() throws LoginException {
        if (refreshToken != null) {
            try {
                URI logoutUri = deployment.getLogoutUrl().clone().build();
                HttpPost post = new HttpPost(logoutUri);

                List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                if (deployment.isPublicClient()) { // if client is public access type
                    formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, deployment.getResourceName()));
                } else {
                    String clientId = deployment.getResourceName();
                    String clientSecret = deployment.getResourceCredentials().get("secret");
                    String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                    post.setHeader("Authorization", authorization);
                }

                formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));

                UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
                post.setEntity(form);

                HttpClient client = deployment.getClient();
                HttpResponse response = client.execute(post);
                int status = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                if (status != 204) {
                    StringBuilder errorBuilder = new StringBuilder("Logout of refreshToken failed. Invalid status: " + status);
                    if (entity != null) {
                        InputStream is = entity.getContent();
                        if (status == 400) {
                            Map<String, String> errors = (Map<String, String>) JsonSerialization.readValue(is, Map.class);
                            errorBuilder.append(", OAuth2 error. Error: " + errors.get(OAuth2Constants.ERROR))
                                    .append(", Error description: " + errors.get(OAuth2Constants.ERROR_DESCRIPTION));

                        } else {
                            if (is != null) is.close();
                        }
                    }

                    // Should do something better than warn if logout failed? Perhaps update of refresh tokens on existing subject might be supported too...
                    log.warn(errorBuilder.toString());
                }
            } catch (IOException ioe) {
                log.warn(ioe);
            }
        }

        return super.logout();
    }

    private static class RefreshTokenHolder implements Serializable {
        private String refreshToken;
    }
}
