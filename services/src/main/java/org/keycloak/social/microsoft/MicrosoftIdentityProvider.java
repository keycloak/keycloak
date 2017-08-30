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

package org.keycloak.social.microsoft;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;

import org.keycloak.models.KeycloakSession;

import java.net.URLEncoder;

/**
 * 
 * Identity provider for Microsoft account. Uses OAuth 2 protocol of Windows Live Services as documented at <a href="https://msdn.microsoft.com/en-us/library/hh243647.aspx">https://msdn.microsoft.com/en-us/library/hh243647.aspx</a>  
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class MicrosoftIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    private static final Logger log = Logger.getLogger(MicrosoftIdentityProvider.class);

    public static final String AUTH_URL = "https://login.live.com/oauth20_authorize.srf";
    public static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    public static final String PROFILE_URL = "https://apis.live.net/v5.0/me";
    public static final String DEFAULT_SCOPE = "wl.basic,wl.emails";

    public MicrosoftIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            String URL = PROFILE_URL + "?access_token=" + URLEncoder.encode(accessToken, "UTF-8");
            if (log.isDebugEnabled()) {
                log.debug("Microsoft Live user profile request to: " + URL);
            }
            JsonNode profile = SimpleHttp.doGet(URL, session).asJson();

            String id = getJsonProperty(profile, "id");

            String email = null;
            if (profile.has("emails")) {
                email = getJsonProperty(profile.get("emails"), "preferred");
            }

            BrokeredIdentityContext user = new BrokeredIdentityContext(id);

            user.setUsername(email != null ? email : id);
            user.setFirstName(getJsonProperty(profile, "first_name"));
            user.setLastName(getJsonProperty(profile, "last_name"));
            if (email != null)
                user.setEmail(email);
            user.setIdpConfig(getConfig());
            user.setIdp(this);

            AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

            return user;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from Microsoft Live ID.", e);
        }
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }
}
