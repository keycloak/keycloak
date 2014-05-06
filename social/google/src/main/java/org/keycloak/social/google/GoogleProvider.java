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

import org.codehaus.jackson.JsonNode;
import org.keycloak.social.AbstractOAuth2Provider;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;
import org.keycloak.social.utils.SimpleHttp;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GoogleProvider extends AbstractOAuth2Provider {

    private static final String ID = "google";
    private static final String NAME = "Google";

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private static final String TOKEN_URL = "https://accounts.google.com/o/oauth2/token";
    private static final String PROFILE_URL = "https://www.googleapis.com/plus/v1/people/me/openIdConnect";

    private static final String DEFAULT_SCOPE = "openid profile email";

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

            SocialUser user = new SocialUser(profile.get("sub").getTextValue(), profile.get("email").getTextValue());
            user.setName(profile.has("given_name") ? profile.get("given_name").getTextValue() : null,
                    profile.has("family_name") ? profile.get("family_name").getTextValue() : null);
            user.setEmail(profile.has("email") ? profile.get("email").getTextValue() : null);

            return user;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

}
