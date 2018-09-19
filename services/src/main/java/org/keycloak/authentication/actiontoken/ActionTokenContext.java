/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.actiontoken;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 *
 * @author hmlnarik
 */
public class ActionTokenContext<T extends JsonWebToken> {

    @FunctionalInterface
    public interface ProcessAuthenticateFlow {
        Response processFlow(boolean action, String execution, AuthenticationSessionModel authSession, String flowPath, AuthenticationFlowModel flow, String errorMessage, AuthenticationProcessor processor);
    };

    @FunctionalInterface
    public interface ProcessBrokerFlow {
        Response brokerLoginFlow(String authSessionId, String code, String execution, String clientId, String tabId, String flowPath);
    };

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UriInfo uriInfo;
    private final ClientConnection clientConnection;
    private final HttpRequest request;
    private EventBuilder event;
    private final ActionTokenHandler<T> handler;
    private AuthenticationSessionModel authenticationSession;
    private boolean authenticationSessionFresh;
    private String executionId;
    private final ProcessAuthenticateFlow processAuthenticateFlow;
    private final ProcessBrokerFlow processBrokerFlow;

    public ActionTokenContext(KeycloakSession session, RealmModel realm, UriInfo uriInfo,
      ClientConnection clientConnection, HttpRequest request,
      EventBuilder event, ActionTokenHandler<T> handler, String executionId,
      ProcessAuthenticateFlow processFlow, ProcessBrokerFlow processBrokerFlow) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.clientConnection = clientConnection;
        this.request = request;
        this.event = event;
        this.handler = handler;
        this.executionId = executionId;
        this.processAuthenticateFlow = processFlow;
        this.processBrokerFlow = processBrokerFlow;
    }

    public EventBuilder getEvent() {
        return event;
    }

    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public ClientConnection getClientConnection() {
        return clientConnection;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public AuthenticationSessionModel createAuthenticationSessionForClient(String clientId)
      throws UriBuilderException, IllegalArgumentException {
        AuthenticationSessionModel authSession;

        // set up the account service as the endpoint to call.
        ClientModel client = clientId != null ? realm.getClientByClientId(clientId) : SystemClientUtil.getSystemClient(realm);
        
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, true);
        authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String redirectUri = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString();
        authSession.setRedirectUri(redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));

        return authSession;
    }

    public boolean isAuthenticationSessionFresh() {
        return authenticationSessionFresh;
    }

    public AuthenticationSessionModel getAuthenticationSession() {
        return authenticationSession;
    }

    public void setAuthenticationSession(AuthenticationSessionModel authenticationSession, boolean isFresh) {
        this.authenticationSession = authenticationSession;
        this.authenticationSessionFresh = isFresh;
        if (this.event != null) {
            ClientModel client = authenticationSession == null ? null : authenticationSession.getClient();
            this.event.client((String) (client == null ? null : client.getClientId()));
        }
    }

    public ActionTokenHandler<T> getHandler() {
        return handler;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public Response processFlow(boolean action, String flowPath, AuthenticationFlowModel flow, String errorMessage, AuthenticationProcessor processor) {
        return processAuthenticateFlow.processFlow(action, getExecutionId(), getAuthenticationSession(), flowPath, flow, errorMessage, processor);
    }

    public Response brokerFlow(String authSessionId, String code, String flowPath) {
        ClientModel client = authenticationSession.getClient();
        return processBrokerFlow.brokerLoginFlow(authSessionId, code, getExecutionId(), client.getClientId(), authenticationSession.getTabId(), flowPath);
    }
}
