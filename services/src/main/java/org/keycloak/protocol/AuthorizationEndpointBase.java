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

package org.keycloak.protocol;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Common base class for Authorization REST endpoints implementation, which have to be implemented by each protocol.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class AuthorizationEndpointBase {

    private static final Logger logger = Logger.getLogger(AuthorizationEndpointBase.class);

    public static final String APP_INITIATED_FLOW = "APP_INITIATED_FLOW";

    protected RealmModel realm;
    protected EventBuilder event;
    protected AuthenticationManager authManager;

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected HttpRequest httpRequest;
    @Context
    protected KeycloakSession session;
    @Context
    protected ClientConnection clientConnection;

    public AuthorizationEndpointBase(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    protected AuthenticationProcessor createProcessor(AuthenticationSessionModel authSession, String flowId, String flowPath) {
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setAuthenticationSession(authSession)
                .setFlowPath(flowPath)
                .setFlowId(flowId)
                .setBrowserFlow(true)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(httpRequest);

        authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, flowPath);

        return processor;
    }

    /**
     * Common method to handle browser authentication request in protocols unified way.
     *
     * @param authSession for current request
     * @param protocol handler for protocol used to initiate login
     * @param isPassive set to true if login should be passive (without login screen shown)
     * @param redirectToAuthentication if true redirect to flow url.  If initial call to protocol is a POST, you probably want to do this.  This is so we can disable the back button on browser
     * @return response to be returned to the browser
     */
    protected Response handleBrowserAuthenticationRequest(AuthenticationSessionModel authSession, LoginProtocol protocol, boolean isPassive, boolean redirectToAuthentication) {
        AuthenticationFlowModel flow = getAuthenticationFlow();
        String flowId = flow.getId();
        AuthenticationProcessor processor = createProcessor(authSession, flowId, LoginActionsService.AUTHENTICATE_PATH);
        event.detail(Details.CODE_ID, authSession.getId());
        if (isPassive) {
            // OIDC prompt == NONE or SAML 2 IsPassive flag
            // This means that client is just checking if the user is already completely logged in.
            // We cancel login if any authentication action or required action is required
            try {
                if (processor.authenticateOnly() == null) {
                    // processor.attachSession();
                } else {
                    Response response = protocol.sendError(authSession, Error.PASSIVE_LOGIN_REQUIRED);
                    return response;
                }

                AuthenticationManager.setRolesAndMappersInSession(authSession);

                if (processor.nextRequiredAction() != null) {
                    Response response = protocol.sendError(authSession, Error.PASSIVE_INTERACTION_REQUIRED);
                    return response;
                }

                // Attach session once no requiredActions or other things are required
                processor.attachSession();
            } catch (Exception e) {
                return processor.handleBrowserException(e);
            }
            return processor.finishAuthentication(protocol);
        } else {
            try {
                RestartLoginCookie.setRestartCookie(session, realm, clientConnection, uriInfo, authSession);
                if (redirectToAuthentication) {
                    return processor.redirectToFlow();
                }
                return processor.authenticate();
            } catch (Exception e) {
                return processor.handleBrowserException(e);
            }
        }
    }

    protected AuthenticationFlowModel getAuthenticationFlow() {
        return realm.getBrowserFlow();
    }

    protected void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            event.error(Errors.SSL_REQUIRED);
            throw new ErrorPageException(session, Messages.HTTPS_REQUIRED);
        }
    }

    protected void checkRealm() {
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new ErrorPageException(session, Messages.REALM_NOT_ENABLED);
        }
    }

    protected AuthorizationEndpointChecks getOrCreateAuthenticationSession(ClientModel client, String requestState) {
        AuthenticationSessionManager manager = new AuthenticationSessionManager(session);
        String authSessionId = manager.getCurrentAuthenticationSessionId(realm);
        AuthenticationSessionModel authSession = authSessionId==null ? null : session.authenticationSessions().getAuthenticationSession(realm, authSessionId);

        if (authSession != null) {

            ClientSessionCode<AuthenticationSessionModel> check = new ClientSessionCode<>(session, realm, authSession);
            if (!check.isActionActive(ClientSessionCode.ActionType.LOGIN)) {

                logger.debugf("Authentication session '%s' exists, but is expired. Restart existing authentication session", authSession.getId());
                authSession.restartSession(realm, client);
                return new AuthorizationEndpointChecks(authSession);

            } else if (isNewRequest(authSession, client, requestState)) {
                // Check if we have lastProcessedExecution and restart the session just if yes. Otherwise update just client information from the AuthorizationEndpoint request.
                // This difference is needed, because of logout from JS applications in multiple browser tabs.
                if (hasProcessedExecution(authSession)) {
                    logger.debug("New request from application received, but authentication session already exists. Restart existing authentication session");
                    authSession.restartSession(realm, client);
                } else {
                    logger.debug("New request from application received, but authentication session already exists. Update client information in existing authentication session");
                    authSession.clearClientNotes(); // update client data
                    authSession.updateClient(client);
                }

                return new AuthorizationEndpointChecks(authSession);

            } else {
                logger.debug("Re-sent some previous request to Authorization endpoint. Likely browser 'back' or 'refresh' button.");

                // See if we have lastProcessedExecution note. If yes, we are expired. Also if we are in different flow than initial one. Otherwise it is browser refresh of initial username/password form
                if (!shouldShowExpirePage(authSession)) {
                    return new AuthorizationEndpointChecks(authSession);
                } else {
                    CacheControlUtil.noBackButtonCacheControlHeader();

                    Response response = new AuthenticationFlowURLHelper(session, realm, uriInfo)
                            .showPageExpired(authSession);
                    return new AuthorizationEndpointChecks(response);
                }
            }
        }

        UserSessionModel userSession = authSessionId==null ? null : session.sessions().getUserSession(realm, authSessionId);

        if (userSession != null) {
            logger.debugf("Sent request to authz endpoint. We don't have authentication session with ID '%s' but we have userSession. Will re-create authentication session with same ID", authSessionId);
            authSession = session.authenticationSessions().createAuthenticationSession(authSessionId, realm, client);
        } else {
            authSession = manager.createAuthenticationSession(realm, client, true);
            logger.debugf("Sent request to authz endpoint. Created new authentication session with ID '%s'", authSession.getId());
        }

        return new AuthorizationEndpointChecks(authSession);

    }

    private boolean hasProcessedExecution(AuthenticationSessionModel authSession) {
        String lastProcessedExecution = authSession.getAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION);
        return (lastProcessedExecution != null);
    }

    // See if we have lastProcessedExecution note. If yes, we are expired. Also if we are in different flow than initial one. Otherwise it is browser refresh of initial username/password form
    private boolean shouldShowExpirePage(AuthenticationSessionModel authSession) {
        if (hasProcessedExecution(authSession)) {
            return true;
        }

        String initialFlow = authSession.getClientNote(APP_INITIATED_FLOW);
        if (initialFlow == null) {
            initialFlow = LoginActionsService.AUTHENTICATE_PATH;
        }

        String lastFlow = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);
        // Check if we transitted between flows (eg. clicking "register" on login screen and then clicking browser 'back', which showed this page)
        if (!initialFlow.equals(lastFlow) && AuthenticationSessionModel.Action.AUTHENTICATE.toString().equals(authSession.getAction())) {
            logger.debugf("Transition between flows! Current flow: %s, Previous flow: %s", initialFlow, lastFlow);

            authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, initialFlow);
            authSession.removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
            return false;
        }

        return false;
    }

    // Try to see if it is new request from the application, or refresh of some previous request
    protected abstract boolean isNewRequest(AuthenticationSessionModel authSession, ClientModel clientFromRequest, String requestState);


    protected static class AuthorizationEndpointChecks {
        public final AuthenticationSessionModel authSession;
        public final Response response;

        private AuthorizationEndpointChecks(Response response) {
            this.authSession = null;
            this.response = response;
        }

        private AuthorizationEndpointChecks(AuthenticationSessionModel authSession) {
            this.authSession = authSession;
            this.response = null;
        }
    }

}