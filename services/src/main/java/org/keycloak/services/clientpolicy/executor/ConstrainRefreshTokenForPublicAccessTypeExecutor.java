/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRefreshContext;
import org.keycloak.services.clientpolicy.context.TokenRefreshResponseContext;
import org.keycloak.services.util.UserSessionUtil;

import static org.keycloak.services.clientpolicy.executor.ConstrainRefreshTokenForPublicAccessTypeExecutorFactory.ONE_TIME_USE;
import static org.keycloak.services.clientpolicy.executor.ConstrainRefreshTokenForPublicAccessTypeExecutorFactory.SENDER_CONSTRAINED;

public class ConstrainRefreshTokenForPublicAccessTypeExecutor implements ClientPolicyExecutorProvider<ConstrainRefreshTokenForPublicAccessTypeExecutor.Configuration> {
    private static final Logger logger = Logger.getLogger(ConstrainRefreshTokenForPublicAccessTypeExecutor.class);
    private final KeycloakSession session;
    private final HttpRequest request;
    private final RealmModel realm;
    private final ClientModel client;

    private final String REFRESH_TOKEN_USED = "refresh_token_used";

    private ConstrainRefreshTokenForPublicAccessTypeExecutor.Configuration configuration;

    public ConstrainRefreshTokenForPublicAccessTypeExecutor(KeycloakSession session) {
        this.session = session;
        request = session.getContext().getHttpRequest();
        realm = session.getContext().getRealm();
        client = session.getContext().getClient();
    }

    @Override
    public void setupConfiguration(ConstrainRefreshTokenForPublicAccessTypeExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<ConstrainRefreshTokenForPublicAccessTypeExecutor.Configuration> getExecutorConfigurationClass() {
        return ConstrainRefreshTokenForPublicAccessTypeExecutor.Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty(ONE_TIME_USE)
        protected Boolean onetimeUse = false;

        @JsonProperty(SENDER_CONSTRAINED)
        protected Boolean senderConstrained = false;

        public Boolean getOnetimeUse() {
            return onetimeUse;
        }

        public void setOnetimeUse(Boolean onetimeUse) {
            this.onetimeUse = onetimeUse;
        }

        public Boolean getSenderConstrained() {
            return senderConstrained;
        }

        public void setSenderConstrained(Boolean senderConstrained) {
            this.senderConstrained = senderConstrained;
        }
    }

    @Override
    public String getProviderId() {
        return ConstrainRefreshTokenForPublicAccessTypeExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (client != null && client.isPublicClient()) {
            if (configuration != null && configuration.getOnetimeUse()) {
                executeOneTimeUse(context);
            }
            if (configuration != null && configuration.getSenderConstrained()) {
                checkIsUseMtlsHokToken(context);
            }
        }
    }

    private void checkIsUseMtlsHokToken(ClientPolicyContext context) throws ClientPolicyException {
        ClientPolicyEvent event = context.getEvent();
        switch (event) {
            case TOKEN_REQUEST:
            case TOKEN_REFRESH:
            case RESOURCE_OWNER_PASSWORD_CREDENTIALS_REQUEST:
            case SERVICE_ACCOUNT_TOKEN_REQUEST:
            case DEVICE_TOKEN_REQUEST:
            case BACKCHANNEL_TOKEN_REQUEST:
                OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(client);
                if (!clientConfig.isUseMtlsHokToken()) {
                    throw new ClientPolicyException(Errors.INVALID_REGISTRATION, "Not permitted to disable OAuth 2.0 Mutual TLS Certificate Bound Access Tokens.");
                }
                break;
        }
    }

    private void executeOneTimeUse(ClientPolicyContext context) throws ClientPolicyException {
        ClientPolicyEvent event = context.getEvent();
        UserSessionModel userSession;
        String refreshTokenUsed;
        switch (event) {
            case TOKEN_REFRESH:
                userSession = getUserSession(((TokenRefreshContext) context).getParams());
                if (userSession == null) {
                    return;
                }
                refreshTokenUsed = userSession.getNote(REFRESH_TOKEN_USED);
                if (refreshTokenUsed != null) {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_GRANT, "Refresh token cannot be reused.");
                }
                break;
            case TOKEN_REFRESH_RESPONSE:
                TokenRefreshResponseContext tokenRefreshResponseContext = (TokenRefreshResponseContext) context;
                userSession = getUserSession(tokenRefreshResponseContext.getParams());
                if (userSession == null) {
                    return;
                }
                refreshTokenUsed = userSession.getNote(REFRESH_TOKEN_USED);
                if (refreshTokenUsed == null) {
                    userSession.setNote(REFRESH_TOKEN_USED, "true");
                }
                TokenManager.AccessTokenResponseBuilder builder = tokenRefreshResponseContext.getAccessTokenResponseBuilder();
                builder.refreshToken(null); // drop rotated refresh token before building a response of a token refresh request
                logger.trace("A rotated refresh token was suppressed.");
                break;
        }
    }

    private UserSessionModel getUserSession(MultivaluedMap<String, String> params) {
        String encodedRefreshToken = params.getFirst(OAuth2Constants.REFRESH_TOKEN);
        TokenManager tokenManager = new TokenManager();
        RefreshToken refreshToken;
        try {
            refreshToken = tokenManager.verifyRefreshToken(session, realm, client, request, encodedRefreshToken, true);
        } catch (OAuthErrorException e) {
            return null;
        }
        EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection())
                .event(EventType.INTROSPECT_TOKEN)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
        return UserSessionUtil.findValidSession(session, realm, refreshToken, event, client);
    }
}
