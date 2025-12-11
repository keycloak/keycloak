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

package org.keycloak.broker.oidc;

import java.io.IOException;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakOIDCIdentityProvider extends OIDCIdentityProvider {

    public KeycloakOIDCIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);
        config.setAccessTokenJwt(true); // force access token JWT
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new KeycloakEndpoint(callback, realm, event, this);
    }

    protected static class KeycloakEndpoint extends OIDCEndpoint {

        private KeycloakOIDCIdentityProvider provider;

        public KeycloakEndpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event,
                KeycloakOIDCIdentityProvider provider) {
            super(callback, realm, event, provider);
            this.provider = provider;
        }

        @POST
        @Path(AdapterConstants.K_LOGOUT)
        public Response backchannelLogout(String input) {
            JWSInput token = null;
            try {
                token = new JWSInput(input);
            } catch (JWSInputException e) {
                logger.warn("Failed to verify logout request");
                return Response.status(400).build();
            }

            if (!provider.verify(token)) {
                logger.warn("Failed to verify logout request");
                return Response.status(400).build();
            }

            LogoutAction action = null;
            try {
                action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!validateAction(action)) return Response.status(400).build();
            if (action.getKeycloakSessionIds() != null) {
                for (String sessionId : action.getKeycloakSessionIds()) {
                    String brokerSessionId = provider.getConfig().getAlias() + "." + sessionId;
                    UserSessionModel userSession = session.sessions().getUserSessionByBrokerSessionId(realm, brokerSessionId);
                    if (userSession != null
                            && userSession.getState() != UserSessionModel.State.LOGGING_OUT
                            && userSession.getState() != UserSessionModel.State.LOGGED_OUT
                            ) {
                        AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, false);
                    }
                }

            }

            // TODO Empty content with ok makes no sense. Should it display a page? Or use noContent?
            session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
            return Response.ok().build();
        }

        protected boolean validateAction(AdminAction action)  {
            if (!action.validate()) {
                logger.warn("admin request failed, not validated" + action.getAction());
                return false;
            }
            if (action.isExpired()) {
                logger.warn("admin request failed, expired token");
                return false;
            }
            if (!provider.getConfig().getClientId().equals(action.getResource())) {
                logger.warn("Resource name does not match");
                return false;

            }
            return true;
        }

        @Override
        public SimpleHttpRequest generateTokenRequest(String authorizationCode) {
            return super.generateTokenRequest(authorizationCode)
                    .param(AdapterConstants.CLIENT_SESSION_STATE, "n/a");  // hack to get backchannel logout to work

        }

    }

    @Override
    protected BrokeredIdentityContext exchangeExternalTokenV1Impl(EventBuilder event, MultivaluedMap<String, String> params) {
        String subjectToken = params.getFirst(OAuth2Constants.SUBJECT_TOKEN);
        if (subjectToken == null) {
            event.detail(Details.REASON, OAuth2Constants.SUBJECT_TOKEN + " param unset");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "token not set", Response.Status.BAD_REQUEST);
        }
        String subjectTokenType = params.getFirst(OAuth2Constants.SUBJECT_TOKEN_TYPE);
        if (subjectTokenType == null) {
            subjectTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
        }
        return validateJwt(event, subjectToken, subjectTokenType);
    }



}
