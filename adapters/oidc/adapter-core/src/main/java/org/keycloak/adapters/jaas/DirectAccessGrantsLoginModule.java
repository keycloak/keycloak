/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.jaas;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Login module based on Resource Owner password credentials grant from OAuth2 specs. It's supposed to be used in environments. which
 * can't rely on HTTP (like SSH authentication for instance). It needs that Direct Grant is enabled on particular realm in Keycloak.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirectAccessGrantsLoginModule extends AbstractKeycloakLoginModule {

    private static final Logger log = Logger.getLogger(DirectAccessGrantsLoginModule.class);

    public static final String SCOPE_OPTION = "scope";

    private String refreshToken;
    private String scope;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.scope = (String)options.get(SCOPE_OPTION);

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

        if (scope != null) {
            formparams.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scope));
        }

        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

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
                OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(is, OAuth2ErrorRepresentation.class);
                errorBuilder.append(", OAuth2 error. Error: " + errorRep.getError())
                        .append(", Error description: " + errorRep.getErrorDescription());
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

        AdapterTokenVerifier.VerifiedTokens tokens = AdapterTokenVerifier.verifyTokens(tokenResponse.getToken(), tokenResponse.getIdToken(), deployment);
        return postTokenVerification(tokenResponse.getToken(), tokens.getAccessToken());
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

                List<NameValuePair> formparams = new ArrayList<>();
                ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);
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
                            OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(is, OAuth2ErrorRepresentation.class);
                            errorBuilder.append(", OAuth2 error. Error: " + errorRep.getError())
                                    .append(", Error description: " + errorRep.getErrorDescription());

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
