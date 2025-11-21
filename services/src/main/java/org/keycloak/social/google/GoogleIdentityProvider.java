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

import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorResponseException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GoogleIdentityProvider extends OIDCIdentityProvider implements SocialIdentityProvider<OIDCIdentityProviderConfig> {

    public static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    public static final String PROFILE_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    public static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
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
            if (connection != null && connection.getRemoteAddr() != null) {
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
    protected BrokeredIdentityContext exchangeExternalTokenV1Impl(EventBuilder event, MultivaluedMap<String, String> params) {
        return exchangeExternalUserInfoValidationOnly(event, params);
    }

    @Override
    protected BrokeredIdentityContext exchangeExternalTokenV2Impl(TokenExchangeContext tokenExchangeContext) {
        try {
            JsonNode tokenInfo = SimpleHttp.create(session).doGet(TOKEN_INFO_URL)
                    .header("Authorization", "Bearer " + tokenExchangeContext.getParams().getSubjectToken())
                    .asJson();
            if (tokenInfo == null || !tokenInfo.has(JsonWebToken.AUD) || !tokenInfo.get(JsonWebToken.AUD).asText().equals(getConfig().getClientId())) {
                logger.tracef("Invalid response or unmatching audience from the token-info endpoint from Google. Expected audience '%s' . Token info response: %s", getConfig().getClientId(), tokenInfo);
                EventBuilder event = tokenExchangeContext.getEvent();
                event.detail(Details.REASON, "Google token validation failed");
                event.error(Errors.INVALID_TOKEN);
                throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Google token validation failed", Response.Status.BAD_REQUEST);
            }
        } catch (ErrorResponseException ere) {
            throw ere;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not verify token info from google.", e);
        }

        return exchangeExternalUserInfoValidationOnly(tokenExchangeContext.getEvent(), tokenExchangeContext.getFormParams());
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
        boolean anyHostedDomain = hostedDomain == null || "*".equals(hostedDomain);

        if (anyHostedDomain) {
            return token;
        }

        Object receivedHdParam = token.getOtherClaims().get(OIDC_PARAMETER_HOSTED_DOMAINS);

        if (receivedHdParam == null) {
            throw new IdentityBrokerException("Identity token does not contain hosted domain parameter.");
        }

        if (List.of(hostedDomain.split(",")).contains(receivedHdParam))  {
            return token;
        }

        throw new IdentityBrokerException("Hosted domain does not match.");
    }

}
