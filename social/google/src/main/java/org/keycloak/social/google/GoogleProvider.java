/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.social.google;

import java.util.UUID;

import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.AuthRequestBuilder;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.api.services.oauth2.model.Userinfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GoogleProvider implements SocialProvider {

    private static final String DEFAULT_RESPONSE_TYPE = "code";

    private static final String AUTH_PATH = "https://accounts.google.com/o/oauth2/auth";

    private static final String DEFAULT_SCOPE = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email";

    private static final JacksonFactory JSON_FACTORY = new JacksonFactory();

    private static final NetHttpTransport TRANSPORT = new NetHttpTransport();

    @Override
    public String getId() {
        return "google";
    }

    @Override
    public AuthRequest getAuthUrl(SocialProviderConfig config) throws SocialProviderException {
        String state = UUID.randomUUID().toString();
        
        AuthRequestBuilder b = AuthRequestBuilder.create(state, AUTH_PATH).setQueryParam("client_id", config.getKey())
                .setQueryParam("response_type", DEFAULT_RESPONSE_TYPE).setQueryParam("scope", DEFAULT_SCOPE)
                .setQueryParam("redirect_uri", config.getCallbackUrl()).setQueryParam("state", state);

        b.setAttribute("state", state);

        return b.build();
    }

    @Override
    public String getName() {
        return "Google";
    }

    @Override
    public SocialUser processCallback(SocialProviderConfig config, AuthCallback callback) throws SocialProviderException {
        String code = callback.getQueryParam(DEFAULT_RESPONSE_TYPE);

        try {
            if (!callback.getQueryParam("state").equals(callback.getAttribute("state"))) {
                throw new SocialProviderException("Invalid state");
            }

            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(TRANSPORT, JSON_FACTORY,
                    config.getKey(), config.getSecret(), code, config.getCallbackUrl().toString())
                    .execute();

            GoogleCredential credential = new GoogleCredential.Builder().setJsonFactory(JSON_FACTORY).setTransport(TRANSPORT)
                    .setClientSecrets(config.getKey(), config.getSecret()).build()
                    .setFromTokenResponse(tokenResponse);

            Oauth2 oauth2 = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, credential).build();

            Tokeninfo tokenInfo = oauth2.tokeninfo().setAccessToken(credential.getAccessToken()).execute();

            if (tokenInfo.containsKey("error")) {
                throw new SocialProviderException((String) tokenInfo.get("error"));
            }

            Userinfo userInfo = oauth2.userinfo().get().execute();

            SocialUser user = new SocialUser(userInfo.getId());
            user.setFirstName(userInfo.getGivenName());
            user.setLastName(userInfo.getFamilyName());
            user.setEmail(userInfo.getEmail());

            return user;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

    @Override
    public String getRequestIdParamName() {
        return "state";
    }

}
