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

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.ExplainedVerificationException;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ActionTokenHandler;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.authentication.actiontoken.ExplainedTokenVerificationException;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionTokenHandler;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.services.util.BrowserHistoryHelper;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.LocaleUtil;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.util.Map;

import static org.keycloak.authentication.actiontoken.DefaultActionToken.ACTION_TOKEN_BASIC_CHECKS;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginActionsService {

    private static final Logger logger = Logger.getLogger(LoginActionsService.class);

    public static final String AUTHENTICATE_PATH = "authenticate";
    public static final String REGISTRATION_PATH = "registration";
    public static final String RESET_CREDENTIALS_PATH = "reset-credentials";
    public static final String REQUIRED_ACTION = "required-action";
    public static final String FIRST_BROKER_LOGIN_PATH = "first-broker-login";
    public static final String POST_BROKER_LOGIN_PATH = "post-broker-login";

    public static final String RESTART_PATH = "restart";

    public static final String FORWARDED_ERROR_MESSAGE_NOTE = "forwardedErrorMessage";

    public static final String SESSION_CODE = "session_code";
    public static final String AUTH_SESSION_ID = "auth_session_id";
    
    public static final String CANCEL_AIA = "cancel-aia";

    private RealmModel realm;

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private ClientConnection clientConnection;

    @Context
    protected Providers providers;

    @Context
    protected KeycloakSession session;

    private EventBuilder event;

    public static UriBuilder loginActionsBaseUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return loginActionsBaseUrl(baseUriBuilder);
    }

    public static UriBuilder authenticationFormProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "authenticateForm");
    }

    public static UriBuilder requiredActionProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "requiredActionPOST");
    }

    public static UriBuilder actionTokenProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "executeActionToken");
    }

    public static UriBuilder registrationFormProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "processRegister");
    }

    public static UriBuilder firstBrokerLoginProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "firstBrokerLoginGet");
    }

    public static UriBuilder postBrokerLoginProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "postBrokerLoginGet");
    }

    public static UriBuilder loginActionsBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path(RealmsResource.class, "getLoginActionsService");
    }

    public LoginActionsService(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
        CacheControlUtil.noBackButtonCacheControlHeader();
    }

    private boolean checkSsl() {
        if (session.getContext().getUri().getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }

    private SessionCodeChecks checksForCode(String authSessionId, String code, String execution, String clientId, String tabId, String flowPath) {
        SessionCodeChecks res = new SessionCodeChecks(realm, session.getContext().getUri(), request, clientConnection, session, event, authSessionId, code, execution, clientId, tabId, flowPath);
        res.initialVerify();
        return res;
    }


    protected URI getLastExecutionUrl(String flowPath, String executionId, String clientId, String tabId) {
        return new AuthenticationFlowURLHelper(session, realm, session.getContext().getUri())
                .getLastExecutionUrl(flowPath, executionId, clientId, tabId);
    }


    /**
     * protocol independent page for restart of the flow
     *
     * @return
     */
    @Path(RESTART_PATH)
    @GET
    public Response restartSession(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                   @QueryParam(Constants.CLIENT_ID) String clientId,
                                   @QueryParam(Constants.TAB_ID) String tabId) {
        event.event(EventType.RESTART_AUTHENTICATION);
        SessionCodeChecks checks = new SessionCodeChecks(realm, session.getContext().getUri(), request, clientConnection, session, event, authSessionId, null, null, clientId,  tabId, null);

        AuthenticationSessionModel authSession = checks.initialVerifyAuthSession();
        if (authSession == null) {
            return checks.getResponse();
        }

        String flowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
        if (flowPath == null) {
            flowPath = AUTHENTICATE_PATH;
        }

        // See if we already have userSession attached to authentication session. This means restart of authentication session during re-authentication
        // We logout userSession in this case
        UserSessionModel userSession = new AuthenticationSessionManager(session).getUserSession(authSession);
        if (userSession != null) {
            logger.debugf("Logout of user session %s when restarting flow during re-authentication", userSession.getId());
            AuthenticationManager.backchannelLogout(session, userSession, false);
        }

        AuthenticationProcessor.resetFlow(authSession, flowPath);

        URI redirectUri = getLastExecutionUrl(flowPath, null, authSession.getClient().getClientId(), tabId);
        logger.debugf("Flow restart requested. Redirecting to %s", redirectUri);
        return Response.status(Response.Status.FOUND).location(redirectUri).build();
    }


    /**
     * protocol independent login page entry point
     *
     * @param code
     * @return
     */
    @Path(AUTHENTICATE_PATH)
    @GET
    public Response authenticate(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                 @QueryParam(SESSION_CODE) String code,
                                 @QueryParam(Constants.EXECUTION) String execution,
                                 @QueryParam(Constants.CLIENT_ID) String clientId,
                                 @QueryParam(Constants.TAB_ID) String tabId) {
        event.event(EventType.LOGIN);

        SessionCodeChecks checks = checksForCode(authSessionId, code, execution, clientId, tabId, AUTHENTICATE_PATH);
        if (!checks.verifyActiveAndValidAction(AuthenticationSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.getResponse();
        }

        AuthenticationSessionModel authSession = checks.getAuthenticationSession();
        boolean actionRequest = checks.isActionRequest();

        processLocaleParam(authSession);

        return processAuthentication(actionRequest, execution, authSession, null);
    }

    protected void processLocaleParam(AuthenticationSessionModel authSession) {
        LocaleUtil.processLocaleParam(session, realm, authSession);
    }

    protected Response processAuthentication(boolean action, String execution, AuthenticationSessionModel authSession, String errorMessage) {
        return processFlow(action, execution, authSession, AUTHENTICATE_PATH, AuthenticationFlowResolver.resolveBrowserFlow(authSession), errorMessage, new AuthenticationProcessor());
    }

    protected Response processFlow(boolean action, String execution, AuthenticationSessionModel authSession, String flowPath, AuthenticationFlowModel flow, String errorMessage, AuthenticationProcessor processor) {
        processor.setAuthenticationSession(authSession)
                .setFlowPath(flowPath)
                .setBrowserFlow(true)
                .setFlowId(flow.getId())
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(request);
        if (errorMessage != null) {
            processor.setForwardedErrorMessage(new FormMessage(null, errorMessage));
        }

        // Check the forwarded error message, which was set by previous HTTP request
        String forwardedErrorMessage = authSession.getAuthNote(FORWARDED_ERROR_MESSAGE_NOTE);
        if (forwardedErrorMessage != null) {
            authSession.removeAuthNote(FORWARDED_ERROR_MESSAGE_NOTE);
            processor.setForwardedErrorMessage(new FormMessage(null, forwardedErrorMessage));
        }


        Response response;
        try {
            if (action) {
                response = processor.authenticationAction(execution);
            } else {
                response = processor.authenticate();
            }
        } catch (WebApplicationException e) {
            response = e.getResponse();
            authSession = processor.getAuthenticationSession();
        } catch (Exception e) {
            response = processor.handleBrowserException(e);
            authSession = processor.getAuthenticationSession(); // Could be changed (eg. Forked flow)
        }

        return BrowserHistoryHelper.getInstance().saveResponseAndRedirect(session, authSession, response, action, request);
    }

    /**
     * URL called after login page.  YOU SHOULD NEVER INVOKE THIS DIRECTLY!
     *
     * @param code
     * @return
     */
    @Path(AUTHENTICATE_PATH)
    @POST
    public Response authenticateForm(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                     @QueryParam(SESSION_CODE) String code,
                                     @QueryParam(Constants.EXECUTION) String execution,
                                     @QueryParam(Constants.CLIENT_ID) String clientId,
                                     @QueryParam(Constants.TAB_ID) String tabId) {
        return authenticate(authSessionId, code, execution, clientId, tabId);
    }

    @Path(RESET_CREDENTIALS_PATH)
    @POST
    public Response resetCredentialsPOST(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                         @QueryParam(SESSION_CODE) String code,
                                         @QueryParam(Constants.EXECUTION) String execution,
                                         @QueryParam(Constants.CLIENT_ID) String clientId,
                                         @QueryParam(Constants.TAB_ID) String tabId,
                                         @QueryParam(Constants.KEY) String key) {
        if (key != null) {
            return handleActionToken(key, execution, clientId, tabId);
        }

        event.event(EventType.RESET_PASSWORD);

        return resetCredentials(authSessionId, code, execution, clientId, tabId);
    }

    /**
     * Endpoint for executing reset credentials flow.  If token is null, a authentication session is created with the account
     * service as the client.  Successful reset sends you to the account page.  Note, account service must be enabled.
     *
     * @param code
     * @param execution
     * @return
     */
    @Path(RESET_CREDENTIALS_PATH)
    @GET
    public Response resetCredentialsGET(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                        @QueryParam(SESSION_CODE) String code,
                                        @QueryParam(Constants.EXECUTION) String execution,
                                        @QueryParam(Constants.CLIENT_ID) String clientId,
                                        @QueryParam(Constants.TAB_ID) String tabId) {
        ClientModel client = realm.getClientByClientId(clientId);
        AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).getCurrentAuthenticationSession(realm, client, tabId);
        processLocaleParam(authSession);

        // we allow applications to link to reset credentials without going through OAuth or SAML handshakes
        if (authSession == null && code == null) {
            if (!realm.isResetPasswordAllowed()) {
                event.event(EventType.RESET_PASSWORD);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

            }
            authSession = createAuthenticationSessionForClient(clientId);
            return processResetCredentials(false, null, authSession, null);
        }

        event.event(EventType.RESET_PASSWORD);
        return resetCredentials(authSessionId, code, execution, clientId, tabId);
    }

    AuthenticationSessionModel createAuthenticationSessionForClient(String clientID)
            throws UriBuilderException, IllegalArgumentException {
        AuthenticationSessionModel authSession;

        ClientModel client = session.clients().getClientByClientId(realm, clientID);
        String redirectUri;

        if (client == null) {
            client = SystemClientUtil.getSystemClient(realm);
            redirectUri = Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName()).toString();
        } else {
            redirectUri = RedirectUtils.getFirstValidRedirectUri(session, client.getRootUrl(), client.getRedirectUris());
        }

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, true);
        authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        //authSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setRedirectUri(redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

        return authSession;
    }

    /**
     * @param code
     * @param execution
     * @return
     */
    protected Response resetCredentials(String authSessionId, String code, String execution, String clientId, String tabId) {
        SessionCodeChecks checks = checksForCode(authSessionId, code, execution, clientId, tabId, RESET_CREDENTIALS_PATH);
        if (!checks.verifyActiveAndValidAction(AuthenticationSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.USER)) {
            return checks.getResponse();
        }
        final AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        if (!realm.isResetPasswordAllowed()) {
            event.error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

        }

        return processResetCredentials(checks.isActionRequest(), execution, authSession, null);
    }

    /**
     * Handles a given token using the given token handler. If there is any {@link VerificationException} thrown
     * in the handler, it is handled automatically here to reduce boilerplate code.
     *
     * @param key
     * @param execution
     * @return
     */
    @Path("action-token")
    @GET
    public Response executeActionToken(@QueryParam(AUTH_SESSION_ID) String authSessionId,
                                       @QueryParam(Constants.KEY) String key,
                                       @QueryParam(Constants.EXECUTION) String execution,
                                       @QueryParam(Constants.CLIENT_ID) String clientId,
                                       @QueryParam(Constants.TAB_ID) String tabId) {
        return handleActionToken(key, execution, clientId, tabId);
    }

    protected <T extends JsonWebToken & ActionTokenKeyModel> Response handleActionToken(String tokenString, String execution, String clientId, String tabId) {
        T token;
        ActionTokenHandler<T> handler;
        ActionTokenContext<T> tokenContext;
        String eventError = null;
        String defaultErrorMessage = null;

        AuthenticationSessionModel authSession = null;

        // Setup client, so error page will contain "back to application" link
        ClientModel client = null;
        if (clientId != null) {
            client = realm.getClientByClientId(clientId);
        }
        AuthenticationSessionManager authenticationSessionManager = new AuthenticationSessionManager(session);
        if (client != null) {
            session.getContext().setClient(client);
            authSession = authenticationSessionManager.getCurrentAuthenticationSession(realm, client, tabId);
        }

        event.event(EventType.EXECUTE_ACTION_TOKEN);

        // First resolve action token handler
        try {
            if (tokenString == null) {
                throw new ExplainedTokenVerificationException(null, Errors.NOT_ALLOWED, Messages.INVALID_REQUEST);
            }

            TokenVerifier<DefaultActionTokenKey> tokenVerifier = TokenVerifier.create(tokenString, DefaultActionTokenKey.class);
            DefaultActionTokenKey aToken = tokenVerifier.getToken();

            event
              .detail(Details.TOKEN_ID, aToken.getId())
              .detail(Details.ACTION, aToken.getActionId())
              .user(aToken.getUserId());

            handler = resolveActionTokenHandler(aToken.getActionId());
            eventError = handler.getDefaultEventError();
            defaultErrorMessage = handler.getDefaultErrorMessage();

            if (! realm.isEnabled()) {
                throw new ExplainedTokenVerificationException(aToken, Errors.REALM_DISABLED, Messages.REALM_NOT_ENABLED);
            }
            if (! checkSsl()) {
                throw new ExplainedTokenVerificationException(aToken, Errors.SSL_REQUIRED, Messages.HTTPS_REQUIRED);
            }

            TokenVerifier<DefaultActionTokenKey> verifier = tokenVerifier
                    .withChecks(
                            // Token introspection checks
                            TokenVerifier.IS_ACTIVE,
                            new TokenVerifier.RealmUrlCheck(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName())),
                            ACTION_TOKEN_BASIC_CHECKS
                    );

            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            verifier.verify();

            token = TokenVerifier.create(tokenString, handler.getTokenClass()).getToken();
        } catch (TokenNotActiveException ex) {
            if (authSession != null) {
                event.clone().error(Errors.EXPIRED_CODE);
                String flowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
                if (flowPath == null) {
                    flowPath = AUTHENTICATE_PATH;
                }
                AuthenticationProcessor.resetFlow(authSession, flowPath);

                // Process correct flow
                return processFlowFromPath(flowPath, authSession, Messages.EXPIRED_ACTION_TOKEN_SESSION_EXISTS);
            }

            return handleActionTokenVerificationException(null, ex, Errors.EXPIRED_CODE, Messages.EXPIRED_ACTION_TOKEN_NO_SESSION);
        } catch (ExplainedTokenVerificationException ex) {
            return handleActionTokenVerificationException(null, ex, ex.getErrorEvent(), ex.getMessage());
        } catch (ExplainedVerificationException ex) {
            return handleActionTokenVerificationException(null, ex, ex.getErrorEvent(), ex.getMessage());
        } catch (VerificationException ex) {
            return handleActionTokenVerificationException(null, ex, eventError, defaultErrorMessage);
        }

        // Now proceed with the verification and handle the token
        tokenContext = new ActionTokenContext(session, realm, session.getContext().getUri(), clientConnection, request, event, handler, execution, this::processFlow, this::brokerLoginFlow);

        try {
            String tokenAuthSessionCompoundId = handler.getAuthenticationSessionIdFromToken(token, tokenContext, authSession);

            if (tokenAuthSessionCompoundId != null) {
                // This can happen if the token contains ID but user opens the link in a new browser
                String sessionId = AuthenticationSessionCompoundId.encoded(tokenAuthSessionCompoundId).getRootSessionId();
                LoginActionsServiceChecks.checkNotLoggedInYet(tokenContext, authSession, sessionId);
            }

            if (authSession == null) {
                authSession = handler.startFreshAuthenticationSession(token, tokenContext);
                tokenContext.setAuthenticationSession(authSession, true);
            } else if (tokenAuthSessionCompoundId == null ||
              ! LoginActionsServiceChecks.doesAuthenticationSessionFromCookieMatchOneFromToken(tokenContext, authSession, tokenAuthSessionCompoundId)) {
                // There exists an authentication session but no auth session ID was received in the action token
                logger.debugf("Authentication session in progress but no authentication session ID was found in action token %s, restarting.", token.getId());
                authenticationSessionManager.removeAuthenticationSession(realm, authSession, false);

                authSession = handler.startFreshAuthenticationSession(token, tokenContext);
                tokenContext.setAuthenticationSession(authSession, true);

                processLocaleParam(authSession);
            }

            initLoginEvent(authSession);
            event.event(handler.eventType());

            LoginActionsServiceChecks.checkIsUserValid(token, tokenContext);
            LoginActionsServiceChecks.checkIsClientValid(token, tokenContext);
            
            session.getContext().setClient(authSession.getClient());

            TokenVerifier.createWithoutSignature(token)
              .withChecks(handler.getVerifiers(tokenContext))
              .verify();

            authSession = tokenContext.getAuthenticationSession();
            event = tokenContext.getEvent();
            event.event(handler.eventType());

            if (! handler.canUseTokenRepeatedly(token, tokenContext)) {
                LoginActionsServiceChecks.checkTokenWasNotUsedYet(token, tokenContext);
                authSession.setAuthNote(AuthenticationManager.INVALIDATE_ACTION_TOKEN, token.serializeKey());
            }

            authSession.setAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID, token.getUserId());

            authSession.setAuthNote(Constants.KEY, tokenString);

            return handler.handleToken(token, tokenContext);
        } catch (ExplainedTokenVerificationException ex) {
            return handleActionTokenVerificationException(tokenContext, ex, ex.getErrorEvent(), ex.getMessage());
        } catch (LoginActionsServiceException ex) {
            Response response = ex.getResponse();
            return response == null
              ? handleActionTokenVerificationException(tokenContext, ex, eventError, defaultErrorMessage)
              : response;
        } catch (VerificationException ex) {
            return handleActionTokenVerificationException(tokenContext, ex, eventError, defaultErrorMessage);
        }
    }

    private Response processFlowFromPath(String flowPath, AuthenticationSessionModel authSession, String errorMessage) {
        if (AUTHENTICATE_PATH.equals(flowPath)) {
            return processAuthentication(false, null, authSession, errorMessage);
        } else if (REGISTRATION_PATH.equals(flowPath)) {
            return processRegistration(false, null, authSession, errorMessage);
        } else if (RESET_CREDENTIALS_PATH.equals(flowPath)) {
            return processResetCredentials(false, null, authSession, errorMessage);
        } else {
            return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, errorMessage == null ? Messages.INVALID_REQUEST : errorMessage);
        }
    }

    private <T extends JsonWebToken> ActionTokenHandler<T> resolveActionTokenHandler(String actionId) throws VerificationException {
        if (actionId == null) {
            throw new VerificationException("Action token operation not set");
        }
        ActionTokenHandler<T> handler = session.getProvider(ActionTokenHandler.class, actionId);

        if (handler == null) {
            throw new VerificationException("Invalid action token operation");
        }
        return handler;
    }

    private Response handleActionTokenVerificationException(ActionTokenContext<?> tokenContext, VerificationException ex, String eventError, String errorMessage) {
        if (tokenContext != null && tokenContext.getAuthenticationSession() != null) {
            new AuthenticationSessionManager(session).removeAuthenticationSession(realm, tokenContext.getAuthenticationSession(), true);
        }

        event
          .detail(Details.REASON, ex == null ? "<unknown>" : ex.getMessage())
          .error(eventError == null ? Errors.INVALID_CODE : eventError);
        return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, errorMessage == null ? Messages.INVALID_CODE : errorMessage);
    }

    protected Response processResetCredentials(boolean actionRequest, String execution, AuthenticationSessionModel authSession, String errorMessage) {
        AuthenticationProcessor authProcessor = new ResetCredentialsActionTokenHandler.ResetCredsAuthenticationProcessor();

        return processFlow(actionRequest, execution, authSession, RESET_CREDENTIALS_PATH, realm.getResetCredentialsFlow(), errorMessage, authProcessor);
    }


    protected Response processRegistration(boolean action, String execution, AuthenticationSessionModel authSession, String errorMessage) {
        return processFlow(action, execution, authSession, REGISTRATION_PATH, realm.getRegistrationFlow(), errorMessage, new AuthenticationProcessor());
    }


    /**
     * protocol independent registration page entry point
     *
     * @param code
     * @return
     */
    @Path(REGISTRATION_PATH)
    @GET
    public Response registerPage(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                 @QueryParam(SESSION_CODE) String code,
                                 @QueryParam(Constants.EXECUTION) String execution,
                                 @QueryParam(Constants.CLIENT_ID) String clientId,
                                 @QueryParam(Constants.TAB_ID) String tabId) {
        return registerRequest(authSessionId, code, execution, clientId,  tabId,false);
    }


    /**
     * Registration
     *
     * @param code
     * @return
     */
    @Path(REGISTRATION_PATH)
    @POST
    public Response processRegister(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                    @QueryParam(SESSION_CODE) String code,
                                    @QueryParam(Constants.EXECUTION) String execution,
                                    @QueryParam(Constants.CLIENT_ID) String clientId,
                                    @QueryParam(Constants.TAB_ID) String tabId) {
        return registerRequest(authSessionId, code, execution, clientId,  tabId,true);
    }


    private Response registerRequest(String authSessionId, String code, String execution, String clientId, String tabId, boolean isPostRequest) {
        event.event(EventType.REGISTER);
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.REGISTRATION_NOT_ALLOWED);
        }

        SessionCodeChecks checks = checksForCode(authSessionId, code, execution, clientId, tabId, REGISTRATION_PATH);
        if (!checks.verifyActiveAndValidAction(AuthenticationSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.getResponse();
        }

        AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        processLocaleParam(authSession);

        AuthenticationManager.expireIdentityCookie(realm, session.getContext().getUri(), clientConnection);

        return processRegistration(checks.isActionRequest(), execution, authSession, null);
    }


    @Path(FIRST_BROKER_LOGIN_PATH)
    @GET
    public Response firstBrokerLoginGet(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                        @QueryParam(SESSION_CODE) String code,
                                        @QueryParam(Constants.EXECUTION) String execution,
                                        @QueryParam(Constants.CLIENT_ID) String clientId,
                                        @QueryParam(Constants.TAB_ID) String tabId) {
        return brokerLoginFlow(authSessionId, code, execution, clientId, tabId, FIRST_BROKER_LOGIN_PATH);
    }

    @Path(FIRST_BROKER_LOGIN_PATH)
    @POST
    public Response firstBrokerLoginPost(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                         @QueryParam(SESSION_CODE) String code,
                                         @QueryParam(Constants.EXECUTION) String execution,
                                         @QueryParam(Constants.CLIENT_ID) String clientId,
                                         @QueryParam(Constants.TAB_ID) String tabId) {
        return brokerLoginFlow(authSessionId, code, execution, clientId, tabId, FIRST_BROKER_LOGIN_PATH);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @GET
    public Response postBrokerLoginGet(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                       @QueryParam(SESSION_CODE) String code,
                                       @QueryParam(Constants.EXECUTION) String execution,
                                       @QueryParam(Constants.CLIENT_ID) String clientId,
                                       @QueryParam(Constants.TAB_ID) String tabId) {
        return brokerLoginFlow(authSessionId, code, execution, clientId, tabId, POST_BROKER_LOGIN_PATH);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @POST
    public Response postBrokerLoginPost(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                        @QueryParam(SESSION_CODE) String code,
                                        @QueryParam(Constants.EXECUTION) String execution,
                                        @QueryParam(Constants.CLIENT_ID) String clientId,
                                        @QueryParam(Constants.TAB_ID) String tabId) {
        return brokerLoginFlow(authSessionId, code, execution, clientId, tabId, POST_BROKER_LOGIN_PATH);
    }


    protected Response brokerLoginFlow(String authSessionId, String code, String execution, String clientId, String tabId, String flowPath) {
        boolean firstBrokerLogin = flowPath.equals(FIRST_BROKER_LOGIN_PATH);

        EventType eventType = firstBrokerLogin ? EventType.IDENTITY_PROVIDER_FIRST_LOGIN : EventType.IDENTITY_PROVIDER_POST_LOGIN;
        event.event(eventType);

        SessionCodeChecks checks = checksForCode(authSessionId, code, execution, clientId, tabId, flowPath);
        if (!checks.verifyActiveAndValidAction(AuthenticationSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.getResponse();
        }
        event.detail(Details.CODE_ID, code);
        final AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        processLocaleParam(authSession);

        String noteKey = firstBrokerLogin ? AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE : PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT;
        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, noteKey);
        if (serializedCtx == null) {
            ServicesLogger.LOGGER.notFoundSerializedCtxInClientSession(noteKey);
            throw new WebApplicationException(ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, "Not found serialized context in authenticationSession."));
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(session, authSession);
        final String identityProviderAlias = brokerContext.getIdpConfig().getAlias();

        String flowId = firstBrokerLogin ? brokerContext.getIdpConfig().getFirstBrokerLoginFlowId() : brokerContext.getIdpConfig().getPostBrokerLoginFlowId();
        if (flowId == null) {
            ServicesLogger.LOGGER.flowNotConfigForIDP(identityProviderAlias);
            throw new WebApplicationException(ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, "Flow not configured for identity provider"));
        }
        AuthenticationFlowModel brokerLoginFlow = realm.getAuthenticationFlowById(flowId);
        if (brokerLoginFlow == null) {
            ServicesLogger.LOGGER.flowNotFoundForIDP(flowId, identityProviderAlias);
            throw new WebApplicationException(ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, "Flow not found for identity provider"));
        }

        event.detail(Details.IDENTITY_PROVIDER, identityProviderAlias)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, brokerContext.getUsername());

        AuthenticationProcessor processor = new AuthenticationProcessor() {

            @Override
            public Response authenticateOnly() throws AuthenticationFlowException {
                Response challenge = super.authenticateOnly();
                if (challenge != null) {
                    if ("true".equals(authenticationSession.getAuthNote(FORWARDED_PASSIVE_LOGIN))) {
                        // forwarded passive login is incompatible with challenges created by the broker flows.
                        logger.errorf("Challenge encountered when executing %s flow. Auth requests with prompt=none are incompatible with challenges", flowPath);
                        LoginProtocol protocol = session.getProvider(LoginProtocol.class, authSession.getProtocol());
                        protocol.setRealm(realm)
                                .setHttpHeaders(headers)
                                .setUriInfo(session.getContext().getUri())
                                .setEventBuilder(event);
                        return protocol.sendError(authSession, Error.PASSIVE_INTERACTION_REQUIRED);
                    }
                }
                return challenge;
            }

            @Override
            protected Response authenticationComplete() {
                if (firstBrokerLogin) {
                    authSession.setAuthNote(AbstractIdpAuthenticator.FIRST_BROKER_LOGIN_SUCCESS, identityProviderAlias);
                } else {
                    String authStateNoteKey = PostBrokerLoginConstants.PBL_AUTH_STATE_PREFIX + identityProviderAlias;
                    authSession.setAuthNote(authStateNoteKey, "true");
                }

                return redirectToAfterBrokerLoginEndpoint(authSession, firstBrokerLogin);
            }

        };

        return processFlow(checks.isActionRequest(), execution, authSession, flowPath, brokerLoginFlow, null, processor);
    }

    private Response redirectToAfterBrokerLoginEndpoint(AuthenticationSessionModel authSession, boolean firstBrokerLogin) {
        return redirectToAfterBrokerLoginEndpoint(session, realm, session.getContext().getUri(), authSession, firstBrokerLogin);
    }

    public static Response redirectToAfterBrokerLoginEndpoint(KeycloakSession session, RealmModel realm, UriInfo uriInfo, AuthenticationSessionModel authSession, boolean firstBrokerLogin) {
        ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
        authSession.getParentSession().setTimestamp(Time.currentTime());

        String clientId = authSession.getClient().getClientId();
        String tabId = authSession.getTabId();
        URI redirect = firstBrokerLogin ? Urls.identityProviderAfterFirstBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getOrGenerateCode(), clientId, tabId) :
                Urls.identityProviderAfterPostBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getOrGenerateCode(), clientId, tabId) ;
        logger.debugf("Redirecting to '%s' ", redirect);

        return Response.status(302).location(redirect).build();
    }

    /**
     * OAuth grant page.  You should not invoked this directly!
     *
     * @return
     */
    @Path("consent")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processConsent() {
        MultivaluedMap<String, String> formData = request.getDecodedFormParameters();
        event.event(EventType.LOGIN);
        String code = formData.getFirst(SESSION_CODE);
        String clientId = session.getContext().getUri().getQueryParameters().getFirst(Constants.CLIENT_ID);
        String tabId = session.getContext().getUri().getQueryParameters().getFirst(Constants.TAB_ID);
        SessionCodeChecks checks = checksForCode(null, code, null, clientId, tabId, REQUIRED_ACTION);
        if (!checks.verifyRequiredAction(AuthenticationSessionModel.Action.OAUTH_GRANT.name())) {
            return checks.getResponse();
        }

        AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        initLoginEvent(authSession);

        UserModel user = authSession.getAuthenticatedUser();
        ClientModel client = authSession.getClient();


        if (formData.containsKey("cancel")) {
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authSession.getProtocol());
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(session.getContext().getUri())
                    .setEventBuilder(event);
            Response response = protocol.sendError(authSession, Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
            return response;
        }

        UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());
        if (grantedConsent == null) {
            grantedConsent = new UserConsentModel(client);
            session.users().addConsent(realm, user.getId(), grantedConsent);
        }

        // Update may not be required if all clientScopes were already granted (May happen for example with prompt=consent)
        boolean updateConsentRequired = false;

        for (String clientScopeId : authSession.getClientScopes()) {
            ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(realm, client, clientScopeId);
            if (clientScope != null) {
                if (!grantedConsent.isClientScopeGranted(clientScope) && clientScope.isDisplayOnConsentScreen()) {
                    grantedConsent.addGrantedClientScope(clientScope);
                    updateConsentRequired = true;
                }
            } else {
                logger.warnf("Client scope or client with ID '%s' not found", clientScopeId);
            }
        }

        if (updateConsentRequired) {
            session.users().updateConsent(realm, user.getId(), grantedConsent);
        }

        event.detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        event.success();

        ClientSessionContext clientSessionCtx = AuthenticationProcessor.attachSession(authSession, null, session, realm, clientConnection, event);
        return AuthenticationManager.redirectAfterSuccessfulFlow(session, realm, clientSessionCtx.getClientSession().getUserSession(), clientSessionCtx, request, session.getContext().getUri(), clientConnection, event, authSession);
    }

    private void initLoginEvent(AuthenticationSessionModel authSession) {
        String responseType = authSession.getClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        if (responseType == null) {
            responseType = "code";
        }
        String respMode = authSession.getClientNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        OIDCResponseMode responseMode = OIDCResponseMode.parse(respMode, OIDCResponseType.parse(responseType));

        event.event(EventType.LOGIN).client(authSession.getClient())
                .detail(Details.CODE_ID, authSession.getParentSession().getId())
                .detail(Details.REDIRECT_URI, authSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, authSession.getProtocol())
                .detail(Details.RESPONSE_TYPE, responseType)
                .detail(Details.RESPONSE_MODE, responseMode.toString().toLowerCase());

        UserModel authenticatedUser = authSession.getAuthenticatedUser();
        if (authenticatedUser != null) {
            event.user(authenticatedUser)
                    .detail(Details.USERNAME, authenticatedUser.getUsername());
        }

        String attemptedUsername = authSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
        if (attemptedUsername != null) {
            event.detail(Details.USERNAME, attemptedUsername);
        }

        String rememberMe = authSession.getAuthNote(Details.REMEMBER_ME);
        if (rememberMe==null || !rememberMe.equalsIgnoreCase("true")) {
            rememberMe = "false";
        }
        event.detail(Details.REMEMBER_ME, rememberMe);

        Map<String, String> userSessionNotes = authSession.getUserSessionNotes();
        String identityProvider = userSessionNotes.get(Details.IDENTITY_PROVIDER);
        if (identityProvider != null) {
            event.detail(Details.IDENTITY_PROVIDER, identityProvider)
                    .detail(Details.IDENTITY_PROVIDER_USERNAME, userSessionNotes.get(Details.IDENTITY_PROVIDER_USERNAME));
        }
    }

    @Path(REQUIRED_ACTION)
    @POST
    public Response requiredActionPOST(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                       @QueryParam(SESSION_CODE) final String code,
                                       @QueryParam(Constants.EXECUTION) String action,
                                       @QueryParam(Constants.CLIENT_ID) String clientId,
                                       @QueryParam(Constants.TAB_ID) String tabId) {
        return processRequireAction(authSessionId, code, action, clientId, tabId);
    }

    @Path(REQUIRED_ACTION)
    @GET
    public Response requiredActionGET(@QueryParam(AUTH_SESSION_ID) String authSessionId, // optional, can get from cookie instead
                                      @QueryParam(SESSION_CODE) final String code,
                                      @QueryParam(Constants.EXECUTION) String action,
                                      @QueryParam(Constants.CLIENT_ID) String clientId,
                                      @QueryParam(Constants.TAB_ID) String tabId) {
        return processRequireAction(authSessionId, code, action, clientId, tabId);
    }

    private Response processRequireAction(final String authSessionId, final String code, String action, String clientId, String tabId) {
        event.event(EventType.CUSTOM_REQUIRED_ACTION);

        SessionCodeChecks checks = checksForCode(authSessionId, code, action, clientId, tabId, REQUIRED_ACTION);
        if (!checks.verifyRequiredAction(action)) {
            return checks.getResponse();
        }

        AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        processLocaleParam(authSession);

        if (!checks.isActionRequest()) {
            initLoginEvent(authSession);
            event.event(EventType.CUSTOM_REQUIRED_ACTION);
            return AuthenticationManager.nextActionAfterAuthentication(session, authSession, clientConnection, request, session.getContext().getUri(), event);
        }

        initLoginEvent(authSession);
        event.event(EventType.CUSTOM_REQUIRED_ACTION);
        event.detail(Details.CUSTOM_REQUIRED_ACTION, action);

        RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, action);
        if (factory == null) {
            ServicesLogger.LOGGER.actionProviderNull();
            event.error(Errors.INVALID_CODE);
            throw new WebApplicationException(ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_CODE));
        }
        RequiredActionContextResult context = new RequiredActionContextResult(authSession, realm, event, session, request, authSession.getAuthenticatedUser(), factory) {
            @Override
            public void ignore() {
                throw new RuntimeException("Cannot call ignore within processAction()");
            }
        };
        RequiredActionProvider provider = null;
        try {
            provider = AuthenticationManager.createRequiredAction(context);
        }  catch (AuthenticationFlowException e) {
            if (e.getResponse() != null) {
                return e.getResponse();
            }
            throw new WebApplicationException(ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.DISPLAY_UNSUPPORTED));
        }


        Response response;
        
        if (isCancelAppInitiatedAction(factory.getId(), authSession, context)) {
            provider.initiatedActionCanceled(session, authSession);
            AuthenticationManager.setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.CANCELLED, authSession);
            context.success();
        } else {
            provider.processAction(context);
        }

        if (action != null) {
            authSession.setAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION, action);
        }

        if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().success();
            initLoginEvent(authSession);
            event.event(EventType.LOGIN);
            authSession.removeRequiredAction(factory.getId());
            authSession.getAuthenticatedUser().removeRequiredAction(factory.getId());
            authSession.removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
            AuthenticationManager.setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.SUCCESS, authSession);

            response = AuthenticationManager.nextActionAfterAuthentication(session, authSession, clientConnection, request, session.getContext().getUri(), event);
        } else if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            response = context.getChallenge();
        } else if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
            response = interruptionResponse(context, authSession, action, Error.CONSENT_DENIED);
        } else {
            throw new RuntimeException("Unreachable");
        }

        return BrowserHistoryHelper.getInstance().saveResponseAndRedirect(session, authSession, response, true, request);
    }
    
    private Response interruptionResponse(RequiredActionContextResult context, AuthenticationSessionModel authSession, String action, Error error) {
        LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, authSession.getProtocol());
        protocol.setRealm(context.getRealm())
                .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                .setUriInfo(context.getUriInfo())
                .setEventBuilder(event);

        event.detail(Details.CUSTOM_REQUIRED_ACTION, action);
        
        event.error(Errors.REJECTED_BY_USER);
        return protocol.sendError(authSession, error);
    }
    
    private boolean isCancelAppInitiatedAction(String providerId, AuthenticationSessionModel authSession, RequiredActionContextResult context) {
        if (providerId.equals(authSession.getClientNote(Constants.KC_ACTION_EXECUTING))) {
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
            boolean userRequestedCancelAIA = formData.getFirst(CANCEL_AIA) != null;
            return userRequestedCancelAIA;
        }
        return false;
    }

}
