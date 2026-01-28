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

package org.keycloak.services.resources;

import java.io.IOException;
import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.ClientData;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.services.util.BrowserHistoryHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import org.jboss.logging.Logger;

import static org.keycloak.services.managers.AuthenticationManager.authenticateIdentityCookie;


public class SessionCodeChecks {

    private static final Logger logger = Logger.getLogger(SessionCodeChecks.class);

    private AuthenticationSessionModel authSession;
    private ClientSessionCode<AuthenticationSessionModel> clientCode;
    private Response response;
    private boolean actionRequest;

    private final RealmModel realm;
    private final UriInfo uriInfo;
    private final HttpRequest request;
    private final ClientConnection clientConnection;
    private final KeycloakSession session;
    private final EventBuilder event;

    private final String code;
    private final String execution;
    private final String clientId;
    private final String clientDataString;
    private final String tabId;
    private final String flowPath;
    private final String authSessionId;


    public SessionCodeChecks(RealmModel realm, UriInfo uriInfo, HttpRequest request, ClientConnection clientConnection, KeycloakSession session, EventBuilder event,
                             String authSessionId, String code, String execution, String clientId, String tabId, String clientData, String flowPath) {
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.request = request;
        this.clientConnection = clientConnection;
        this.session = session;
        this.event = event;

        this.code = code;
        this.execution = execution;
        this.clientId = clientId;
        this.tabId = tabId;
        this.flowPath = flowPath;
        this.authSessionId = authSessionId;
        this.clientDataString = clientData;
    }


    public AuthenticationSessionModel getAuthenticationSession() {
        return authSession;
    }


    private boolean failed() {
        return response != null;
    }


    public Response getResponse() {
        return response;
    }


    public ClientSessionCode<AuthenticationSessionModel> getClientCode() {
        return clientCode;
    }

    public boolean isActionRequest() {
        return actionRequest;
    }


    private boolean checkSsl() {
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }


    public AuthenticationSessionModel initialVerifyAuthSession() {
        // Basic realm checks
        if (!checkSsl()) {
            event.error(Errors.SSL_REQUIRED);
            response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.HTTPS_REQUIRED);
            return null;
        }
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.REALM_NOT_ENABLED);
            return null;
        }

        // Setup client to be shown on error/info page based on "client_id" parameter
        logger.debugf("Will use client '%s' in back-to-application link", clientId);
        ClientModel client = null;
        if (clientId != null) {
            client = realm.getClientByClientId(clientId);
        }
        if (client != null) {
            session.getContext().setClient(client);
            setClientToEvent(client);
        }


        // object retrieve
        AuthenticationSessionManager authSessionManager = new AuthenticationSessionManager(session);
        AuthenticationSessionModel authSession = null;
        if (authSessionId != null)
            authSession = authSessionManager.getAuthenticationSessionByEncodedIdAndClient(realm, authSessionId, client, tabId);
        AuthenticationSessionModel authSessionCookie = authSessionManager.getCurrentAuthenticationSession(realm, client, tabId);

        if (authSession != null && authSessionCookie != null && !authSession.getParentSession().getId().equals(authSessionCookie.getParentSession().getId())) {
            event.detail(Details.REASON, "cookie does not match auth_session query parameter");
            event.error(Errors.INVALID_CODE);
            response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_CODE);
            return null;

        }

        ClientData clientData;
        try {
            clientData = ClientData.decodeClientDataFromParameter(clientDataString);
        } catch (RuntimeException | IOException e) {
            logger.debugf(e, "ClientData parameter in invalid format. ClientData parameter was %s", clientDataString);
            event.detail(Details.REASON, "Invalid client data: " + e.getMessage());
            event.error(Errors.INVALID_REQUEST);
            response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            return null;
        }

        if (authSession != null) {
            session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authSession);
            return authSession;
        }

        if (authSessionCookie != null) {
            session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authSessionCookie);
            return authSessionCookie;

        }

        // Otherwise just try to restart from the cookie
        RootAuthenticationSessionModel existingRootAuthSession = authSessionManager.getCurrentRootAuthenticationSession(realm);
        response = restartAuthenticationSessionFromCookie(existingRootAuthSession);

        // if restart from cookie was not found check if the user is already authenticated
        if (response.getStatus() != Response.Status.FOUND.getStatusCode()) {
            AuthenticationManager.AuthResult authResult = authenticateIdentityCookie(session, realm, false);

            if (authResult != null && authResult.session() != null) {
                response = null;

                if (client != null && clientData != null) {
                    LoginProtocol protocol = session.getProvider(LoginProtocol.class, client.getProtocol());
                    protocol.setRealm(realm)
                            .setHttpHeaders(session.getContext().getRequestHeaders())
                            .setUriInfo(session.getContext().getUri())
                            .setEventBuilder(event);
                    response = protocol.sendError(client, clientData, LoginProtocol.Error.ALREADY_LOGGED_IN);
                    event.detail(Details.REDIRECTED_TO_CLIENT, "true");
                }

                if (response == null) {
                    LoginFormsProvider loginForm = session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authSession)
                            .setSuccess(Messages.ALREADY_LOGGED_IN);

                    if (client == null) {
                        loginForm.setAttribute(Constants.SKIP_LINK, true);
                    }

                    response = loginForm.createInfoPage();
                    event.detail(Details.REDIRECTED_TO_CLIENT, "false");
                }
                event.error(Errors.ALREADY_LOGGED_IN);
            } else {
                event.error(Errors.COOKIE_NOT_FOUND);
            }
        }

        return null;
    }


    public boolean initialVerify() {
        // Basic realm checks and authenticationSession retrieve
        authSession = initialVerifyAuthSession();
        if (authSession == null) {
            return false;
        }
        session.getContext().setAuthenticationSession(authSession);

        // Check cached response from previous action request
        response = BrowserHistoryHelper.getInstance().loadSavedResponse(session, authSession);
        if (response != null) {
            return false;
        }

        // Client checks
        event.detail(Details.CODE_ID, authSession.getParentSession().getId());
        ClientModel client = authSession.getClient();
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            session.getProvider(LoginFormsProvider.class).setDetachedAuthSession();
            response = ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.UNKNOWN_LOGIN_REQUESTER);
            clientCode.removeExpiredClientSession();
            return false;
        }

        setClientToEvent(client);
        session.getContext().setClient(client);

        if (checkClientDisabled(client)) {
            event.error(Errors.CLIENT_DISABLED);
            session.getProvider(LoginFormsProvider.class).setDetachedAuthSession();
            response = ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.LOGIN_REQUESTER_NOT_ENABLED);
            clientCode.removeExpiredClientSession();
            return false;
        }


        // Check if it's action or not
        if (code == null) {
            String lastExecFromSession = authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
            String lastFlow = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);

            // Check if we transitted between flows (eg. clicking "register" on login screen)
            if (execution == null && !flowPath.equals(lastFlow)) {
                logger.debugf("Transition between flows! Current flow: %s, Previous flow: %s", flowPath, lastFlow);

                // Don't allow moving to different flow if I am on requiredActions already
                if (AuthenticationSessionModel.Action.AUTHENTICATE.name().equals(authSession.getAction())) {
                    authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, flowPath);
                    authSession.removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                    lastExecFromSession = null;
                }
            }

            if (execution == null || execution.equals(lastExecFromSession) || CommonClientSessionModel.ExecutionStatus.CHALLENGED.equals(authSession.getExecutionStatus().get(execution))) {
                // Allow refresh of previous page
                clientCode = new ClientSessionCode<>(session, realm, authSession);
                actionRequest = false;

                // Allow refresh, but rewrite browser history
                if (execution == null && lastExecFromSession != null) {
                    logger.debugf("Parameter 'execution' is not in the request, but flow wasn't changed. Will update browser history");
                    session.setAttribute(BrowserHistoryHelper.SHOULD_UPDATE_BROWSER_HISTORY, true);
                }

                return true;
            } else {
                response = showPageExpired(authSession);
                return false;
            }
        } else {
            ClientSessionCode.ParseResult<AuthenticationSessionModel> result = ClientSessionCode.parseResult(code, tabId, session, realm, client, event, authSession);
            clientCode = result.getCode();
            if (clientCode == null) {

                // In case that is replayed action, but sent to the same FORM like actual FORM, we just re-render the page
                if (ObjectUtil.isEqualOrBothNull(execution, authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION))) {
                    String latestFlowPath = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);
                    if (latestFlowPath != null) {
                        String clientData = AuthenticationProcessor.getClientData(session, authSession);
                        URI redirectUri = getLastExecutionUrl(latestFlowPath, execution, tabId, clientData);

                        logger.debugf("Invalid action code, but execution matches. So just redirecting to %s", redirectUri);
                        authSession.setAuthNote(LoginActionsService.FORWARDED_ERROR_MESSAGE_NOTE, Messages.EXPIRED_ACTION);
                        response = Response.status(Response.Status.FOUND).location(redirectUri).build();
                        return false;
                    }
                }
                response = showPageExpired(authSession);
                return false;

            }


            actionRequest = true;
            if (execution != null) {
                authSession.setAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION, execution);
            }
            return true;
        }
    }

    // Client is not null
    protected void setClientToEvent(ClientModel client) {
        event.client(client);
    }


    public boolean verifyActiveAndValidAction(String expectedAction, ClientSessionCode.ActionType actionType) {
        if (failed()) {
            return false;
        }

        if (!isActionActive(actionType)) {
            return false;
        }

        if (!clientCode.isValidAction(expectedAction)) {
            AuthenticationSessionModel authSession = getAuthenticationSession();
            if (AuthenticationSessionModel.Action.REQUIRED_ACTIONS.name().equals(authSession.getAction())) {
                logger.debugf("Incorrect action '%s' . User authenticated already.", authSession.getAction());
                response = showPageExpired(authSession);
                return false;
            } else {
                logger.errorf("Bad action. Expected action '%s', current action '%s'", expectedAction, authSession.getAction());
                response = ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.EXPIRED_CODE);
                return false;
            }
        }

        return true;
    }


    protected boolean isActionActive(ClientSessionCode.ActionType actionType) {
        if (!clientCode.isActionActive(actionType)) {
            event.clone().error(Errors.EXPIRED_CODE);

            AuthenticationProcessor.resetFlow(authSession, LoginActionsService.AUTHENTICATE_PATH);

            authSession.setAuthNote(LoginActionsService.FORWARDED_ERROR_MESSAGE_NOTE, Messages.LOGIN_TIMEOUT);

            String clientData = AuthenticationProcessor.getClientData(session, authSession);
            URI redirectUri = getLastExecutionUrl(LoginActionsService.AUTHENTICATE_PATH, null, tabId, clientData);
            logger.debugf("Flow restart after timeout. Redirecting to %s", redirectUri);
            response = Response.status(Response.Status.FOUND).location(redirectUri).build();
            return false;
        }
        return true;
    }


    public boolean verifyRequiredAction(String executedAction) {
        if (failed()) {
            return false;
        }

        if (!clientCode.isValidAction(AuthenticationSessionModel.Action.REQUIRED_ACTIONS.name())) {
            logger.debugf("Expected required action, but session action is '%s' . Showing expired page now.", authSession.getAction());
            event.error(Errors.INVALID_CODE);

            response = showPageExpired(authSession);

            return false;
        }

        if (!isActionActive(ClientSessionCode.ActionType.USER)) {
            return false;
        }

        if (actionRequest) {
            String currentRequiredAction = authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
            if (executedAction == null || !executedAction.equals(currentRequiredAction)) {
                logger.debug("required action doesn't match current required action");
                response = redirectToRequiredActions(currentRequiredAction);
                return false;
            }
        }
        return true;
    }


    protected Response restartAuthenticationSessionFromCookie(RootAuthenticationSessionModel existingRootSession) {
        logger.debug("Authentication session not found. Trying to restart from cookie.");
        AuthenticationSessionModel authSession = null;

        String cook = RestartLoginCookie.getRestartCookie(session);
        if (cook == null) {
            return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.COOKIE_NOT_FOUND);
        }

        try {
            authSession = RestartLoginCookie.restartSession(session, realm, existingRootSession, clientId, cook);
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToParseRestartLoginCookie(e);
        }

        if (authSession != null) {

            event.clone();
            event.detail(Details.RESTART_AFTER_TIMEOUT, "true");
            event.error(Errors.EXPIRED_CODE);

            String warningMessage = Messages.LOGIN_TIMEOUT;
            authSession.setAuthNote(LoginActionsService.FORWARDED_ERROR_MESSAGE_NOTE, warningMessage);

            String flowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
            if (flowPath == null) {
                flowPath = LoginActionsService.AUTHENTICATE_PATH;
            }

            //set redirect uri and other notes from client data parameter
            try {
                ClientData clientData = ClientData.decodeClientDataFromParameter(clientDataString);
                if (RedirectUtils.verifyRedirectUri(session, clientData.getRedirectUri(), authSession.getClient()) != null) {
                    authSession.setRedirectUri(clientData.getRedirectUri());
                    authSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, clientData.getResponseType());
                    authSession.setClientNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM, clientData.getResponseMode());
                    authSession.setClientNote(OIDCLoginProtocol.STATE_PARAM, clientData.getState());
                }
            } catch (Exception e) {
                logger.debugf(e, "ClientData parameter in invalid format. ClientData parameter was %s", clientDataString);
            }

            String clientData = AuthenticationProcessor.getClientData(session, authSession);
            URI redirectUri = getLastExecutionUrl(flowPath, null, authSession.getTabId(), clientData);
            logger.debugf("Authentication session restart from cookie succeeded. Redirecting to %s", redirectUri);
            return Response.status(Response.Status.FOUND).location(redirectUri).build();
        } else {
            // Finally need to show error as all the fallbacks failed
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_CODE);
        }
    }


    private Response redirectToRequiredActions(String action) {
        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION);

        if (action != null) {
            uriBuilder.queryParam(Constants.EXECUTION, action);
        }

        ClientModel client = authSession.getClient();
        uriBuilder.queryParam(Constants.CLIENT_ID, client.getClientId())
                .queryParam(Constants.TAB_ID, authSession.getTabId())
                .queryParam(Constants.CLIENT_DATA, AuthenticationProcessor.getClientData(session, authSession));

        URI redirect = uriBuilder.build(realm.getName());
        return Response.status(302).location(redirect).build();
    }


    private URI getLastExecutionUrl(String flowPath, String executionId, String tabId, String clientData) {
        return new AuthenticationFlowURLHelper(session, realm, uriInfo)
                .getLastExecutionUrl(flowPath, executionId, clientId, tabId, clientData);
    }


    private Response showPageExpired(AuthenticationSessionModel authSession) {
        return new AuthenticationFlowURLHelper(session, realm, uriInfo)
                .showPageExpired(authSession);
    }

    protected KeycloakSession getSession() {
        return session;
    }

    protected EventBuilder getEvent() {
        return event;
    }

    protected boolean checkClientDisabled(ClientModel client) {
        return !client.isEnabled();
    }
}
