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

import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakOIDCIdentityProvider extends OIDCIdentityProvider {

    public static final String VALIDATED_ACCESS_TOKEN = "VALIDATED_ACCESS_TOKEN";

    public KeycloakOIDCIdentityProvider(OIDCIdentityProviderConfig config) {
        super(config);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new KeycloakEndpoint(callback, realm, event);
    }

    @Override
    protected void processAccessTokenResponse(BrokeredIdentityContext context, PublicKey idpKey, AccessTokenResponse response) {
        JsonWebToken access = validateToken(idpKey, response.getToken());
        context.getContextData().put(VALIDATED_ACCESS_TOKEN, access);
    }

    protected class KeycloakEndpoint extends OIDCEndpoint {
        public KeycloakEndpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            super(callback, realm, event);
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
            PublicKey key = getExternalIdpKey();
            if (key != null) {
                if (!verify(token, key)) {
                    logger.warn("Failed to verify logout request");
                    return Response.status(400).build();
                }
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
                    String brokerSessionId = getConfig().getAlias() + "." + sessionId;
                    UserSessionModel userSession = session.sessions().getUserSessionByBrokerSessionId(realm, brokerSessionId);
                    if (userSession != null
                            && userSession.getState() != UserSessionModel.State.LOGGING_OUT
                            && userSession.getState() != UserSessionModel.State.LOGGED_OUT
                            ) {
                        AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, false);
                    }
                }

            }
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
            if (!getConfig().getClientId().equals(action.getResource())) {
                logger.warn("Resource name does not match");
                return false;

            }
            return true;
        }

        @Override
        public SimpleHttp generateTokenRequest(String authorizationCode) {
            return super.generateTokenRequest(authorizationCode)
                    .param(AdapterConstants.CLIENT_SESSION_STATE, "n/a");  // hack to get backchannel logout to work

        }



    }


}
