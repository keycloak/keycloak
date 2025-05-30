/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.protocol.oidc.par.endpoints.request;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import static org.keycloak.protocol.oidc.par.endpoints.ParEndpoint.PAR_CREATED_TIME;
import static org.keycloak.protocol.oidc.par.endpoints.ParEndpoint.PAR_DPOP_PROOF_JKT;
import static org.keycloak.protocol.oidc.par.endpoints.ParEndpoint.PAR_REQUEST_KEY;

/**
 * Parse the parameters from PAR
 *
 */
public class AuthzEndpointParParser extends AuthzEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(AuthzEndpointParParser.class);

    private final KeycloakSession session;
    private final ClientModel client;
    private final String key;
    private final Map<String, String> requestParams;
    private String invalidRequestMessage = null;

    public AuthzEndpointParParser(KeycloakSession session, ClientModel client, String requestUri) {
        super(session);
        this.session = session;
        this.client = client;
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        try {
            this.key = requestUri.substring(ParEndpoint.REQUEST_URI_PREFIX_LENGTH);
        } catch (RuntimeException re) {
            logger.warnf(re,"Unable to parse request_uri: %s", requestUri);
            throw new RuntimeException("Unable to parse request_uri");
        }
        Map<String, String> retrievedRequest = singleUseStore.remove(key);
        if (retrievedRequest == null) {
            // retrieve the data from the authentication session if it is a reload
            retrievedRequest = getRetrievedRequestFromAuthSession();
            if (retrievedRequest == null) {
                throw new RuntimeException("PAR not found. not issued or used multiple times.");
            }
        }

        RealmModel realm = session.getContext().getRealm();
        int expiresIn = realm.getParPolicy().getRequestUriLifespan();
        String created = retrievedRequest.get(PAR_CREATED_TIME);
        if (created != null && Time.currentTimeMillis() - Long.parseLong(created) < expiresIn * 1000) {
            requestParams = retrievedRequest;
        } else {
            throw new RuntimeException("PAR expired.");
        }
        // If DPoP Proof existed with PAR request, its public key needs to be matched with the one with Token Request afterward
        String dpopJkt = retrievedRequest.get(PAR_DPOP_PROOF_JKT);
        if (dpopJkt != null) {
            session.setAttribute(PAR_DPOP_PROOF_JKT, dpopJkt);
        }
    }

    @Override
    public void parseRequest(AuthorizationEndpointRequest request) {
        String requestParam = requestParams.get(OIDCLoginProtocol.REQUEST_PARAM);

        if (requestParam != null) {
            // parses the request object if PAR was registered using JAR
            // parameters from requets object have precedence over those sent directly in the request
            new ParEndpointRequestObjectParser(session, requestParam, client).parseRequest(request);
        } else {
            super.parseRequest(request);
        }
        // add the specific par data as additional params to be later added into the auth session
        request.getAdditionalReqParams().put(PAR_REQUEST_KEY, key);
        request.getAdditionalReqParams().put(PAR_CREATED_TIME, requestParams.get(PAR_CREATED_TIME));
        if (requestParams.containsKey(PAR_DPOP_PROOF_JKT)) {
            request.getAdditionalReqParams().put(PAR_DPOP_PROOF_JKT, requestParams.get(PAR_DPOP_PROOF_JKT));
        }
    }

    @Override
    protected String getParameter(String paramName) {
        return requestParams.get(paramName);
    }

    @Override
    protected Integer getIntParameter(String paramName) {
        String paramVal = requestParams.get(paramName);
        return paramVal == null ? null : Integer.valueOf(paramVal);
    }

    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }

    @Override
    protected Set<String> keySet() {
        return requestParams.keySet();
    }

    private Map<String, String> getRetrievedRequestFromAuthSession() {
        AuthenticationSessionManager authSessionManager = new AuthenticationSessionManager(session);
        RootAuthenticationSessionModel existingRootAuthSession = authSessionManager.getCurrentRootAuthenticationSession(session.getContext().getRealm());
        if (existingRootAuthSession == null) {
            return null;
        }
        String restartCookie = RestartLoginCookie.getRestartCookie(session);
        if (restartCookie == null) {
            return null;
        }
        AuthenticationSessionModel authSession = null;
        try {
            authSession = RestartLoginCookie.restartSession(session,
                    session.getContext().getRealm(), existingRootAuthSession, client.getClientId(), restartCookie);
        } catch (Exception e) {
            logger.tracef("Error restarting session for client", e);
        }
        if (authSession == null) {
            return null;
        }
        // check the session contains the par key with the same key
        String keyInSession = authSession.getClientNote(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + PAR_REQUEST_KEY);
        if (!key.equals(keyInSession)) {
            return null;
        }
        return authSession.getClientNotes().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().startsWith(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX)
                                ? entry.getKey().substring(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX.length())
                                : entry.getKey(),
                        Map.Entry::getValue,
                        (value1, value2) -> value1));
    }

}
