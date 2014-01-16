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

import org.json.JSONObject;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.utils.SimpleHttp;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;

import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GoogleProvider implements SocialProvider {

    private static final String DEFAULT_RESPONSE_TYPE = "code";

    private static final String AUTH_PATH = "https://accounts.google.com/o/oauth2/auth";

    private static final String TOKEN_PATH = "https://accounts.google.com/o/oauth2/token";

    private static final String PROFILE_PATH = "https://www.googleapis.com/plus/v1/people/me/openIdConnect";

    private static final String DEFAULT_SCOPE = "openid profile email";

    @Override
    public String getId() {
        return "google";
    }

    @Override
    public AuthRequest getAuthUrl(SocialProviderConfig config) throws SocialProviderException {
        String state = UUID.randomUUID().toString();

        return AuthRequest.create(state, AUTH_PATH).setQueryParam("client_id", config.getKey())
                .setQueryParam("response_type", DEFAULT_RESPONSE_TYPE).setQueryParam("scope", DEFAULT_SCOPE)
                .setQueryParam("redirect_uri", config.getCallbackUrl()).setQueryParam("state", state).setAttribute("state", state).build();
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

            JSONObject token = SimpleHttp.doPost(TOKEN_PATH).param("code", code).param("client_id", config.getKey())
                    .param("client_secret", config.getSecret())
                    .param("redirect_uri", config.getCallbackUrl())
                    .param("grant_type", "authorization_code").asJson();

            String accessToken = token.getString("access_token");

            JSONObject profile = SimpleHttp.doGet(PROFILE_PATH).header("Authorization", "Bearer " + accessToken).asJson();

            SocialUser user = new SocialUser(profile.getString("sub"));

            user.setUsername(profile.getString("email"));

            user.setFirstName(profile.optString("given_name"));
            user.setLastName(profile.optString("family_name"));
            user.setEmail(profile.optString("email"));

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
