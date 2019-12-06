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
package org.keycloak.social.google;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GoogleIdentityProvider extends OIDCIdentityProvider implements SocialIdentityProvider<OIDCIdentityProviderConfig> {

    public static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    public static final String PROFILE_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    public static final String DEFAULT_SCOPE = "openid profile email";

    private static final String OIDC_PARAMETER_HOSTED_DOMAINS = "hd";
    private static final String OIDC_PARAMETER_ACCESS_TYPE = "access_type";
    private static final String ACCESS_TYPE_OFFLINE = "offline";

    public GoogleIdentityProvider(KeycloakSession session, GoogleIdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }

    @Override
    protected String getUserInfoUrl() {
        String uri = super.getUserInfoUrl();
        if (((GoogleIdentityProviderConfig)getConfig()).isUserIp()) {
            ClientConnection connection = session.getContext().getConnection();
            if (connection != null) {
                uri = KeycloakUriBuilder.fromUri(super.getUserInfoUrl()).queryParam("userIp", connection.getRemoteAddr()).build().toString();
            }

        }
        logger.debugv("GOOGLE userInfoUrl: {0}", uri);
        return uri;
    }

    @Override
    protected boolean supportsExternalExchange() {
        return true;
    }


    @Override
    public boolean isIssuer(String issuer, MultivaluedMap<String, String> params) {
        String requestedIssuer = params.getFirst(OAuth2Constants.SUBJECT_ISSUER);
        if (requestedIssuer == null) requestedIssuer = issuer;
        return requestedIssuer.equals(getConfig().getAlias());
    }


    @Override
    protected BrokeredIdentityContext exchangeExternalImpl(EventBuilder event, MultivaluedMap<String, String> params) {
        return exchangeExternalUserInfoValidationOnly(event, params);
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        UriBuilder uriBuilder = super.createAuthorizationUrl(request);
        final GoogleIdentityProviderConfig googleConfig = (GoogleIdentityProviderConfig) getConfig();
        String hostedDomain = googleConfig.getHostedDomain();

        if (hostedDomain != null) {
            uriBuilder.queryParam(OIDC_PARAMETER_HOSTED_DOMAINS, hostedDomain);
        }
        
        if (googleConfig.isOfflineAccess()) {
            uriBuilder.queryParam(OIDC_PARAMETER_ACCESS_TYPE, ACCESS_TYPE_OFFLINE);
        }
        
        return uriBuilder;
    }

    @Override
    protected JsonWebToken validateToken(final String encodedToken, final boolean ignoreAudience) {
        JsonWebToken token = super.validateToken(encodedToken, ignoreAudience);
        String hostedDomain = ((GoogleIdentityProviderConfig) getConfig()).getHostedDomain();

        if (hostedDomain == null) {
            return token;
        }

        Object receivedHdParam = token.getOtherClaims().get(OIDC_PARAMETER_HOSTED_DOMAINS);

        if (receivedHdParam == null) {
            throw new IdentityBrokerException("Identity token does not contain hosted domain parameter.");
        }

        if (hostedDomain.equals("*") || hostedDomain.equals(receivedHdParam))  {
            return token;
        }

        throw new IdentityBrokerException("Hosted domain does not match.");
    }

}
