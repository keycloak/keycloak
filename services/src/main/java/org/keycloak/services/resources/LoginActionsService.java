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

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.requiredactions.VerifyEmail;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.CookieHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginActionsService {

    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    public static final String ACTION_COOKIE = "KEYCLOAK_ACTION";
    public static final String AUTHENTICATE_PATH = "authenticate";
    public static final String REGISTRATION_PATH = "registration";
    public static final String RESET_CREDENTIALS_PATH = "reset-credentials";
    public static final String REQUIRED_ACTION = "required-action";
    public static final String FIRST_BROKER_LOGIN_PATH = "first-broker-login";
    public static final String POST_BROKER_LOGIN_PATH = "post-broker-login";

    private RealmModel realm;

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

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
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }


    private class Checks {
        ClientSessionCode clientCode;
        Response response;
        ClientSessionCode.ParseResult result;

        boolean verifyCode(String code, String requiredAction, ClientSessionCode.ActionType actionType) {
            if (!verifyCode(code)) {
                return false;
            }
            if (!clientCode.isValidAction(requiredAction)) {
                ClientSessionModel clientSession = clientCode.getClientSession();
                if (ClientSessionModel.Action.REQUIRED_ACTIONS.name().equals(clientSession.getAction())) {
                    response = redirectToRequiredActions(code);
                    return false;
                } else if (clientSession.getUserSession() != null && clientSession.getUserSession().getState() == UserSessionModel.State.LOGGED_IN) {
                    response = session.getProvider(LoginFormsProvider.class)
                            .setSuccess(Messages.ALREADY_LOGGED_IN)
                            .createInfoPage();
                    return false;
                }
            }
            if (!isActionActive(actionType)) return false;
            return true;
       }

        private boolean isValidAction(String requiredAction) {
            if (!clientCode.isValidAction(requiredAction)) {
                invalidAction();
                return false;
            }
            return true;
        }

        private void invalidAction() {
            event.client(clientCode.getClientSession().getClient());
            event.error(Errors.INVALID_CODE);
            response = ErrorPage.error(session, Messages.INVALID_CODE);
        }

        private boolean isActionActive(ClientSessionCode.ActionType actionType) {
            if (!clientCode.isActionActive(actionType)) {
                event.client(clientCode.getClientSession().getClient());
                event.clone().error(Errors.EXPIRED_CODE);
                if (clientCode.getClientSession().getAction().equals(ClientSessionModel.Action.AUTHENTICATE.name())) {
                    AuthenticationProcessor.resetFlow(clientCode.getClientSession());
                    response = processAuthentication(null, clientCode.getClientSession(), Messages.LOGIN_TIMEOUT);
                    return false;
                }
                response = ErrorPage.error(session, Messages.EXPIRED_CODE);
                return false;
            }
            return true;
        }

        public boolean verifyCode(String code) {
            if (!checkSsl()) {
                event.error(Errors.SSL_REQUIRED);
                response = ErrorPage.error(session, Messages.HTTPS_REQUIRED);
                return false;
            }
            if (!realm.isEnabled()) {
                event.error(Errors.REALM_DISABLED);
                response = ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
                return false;
            }
            result = ClientSessionCode.parseResult(code, session, realm);
            clientCode = result.getCode();
            if (clientCode == null) {
                if (result.isClientSessionNotFound()) { // timeout
                    try {
                        ClientSessionModel clientSession = RestartLoginCookie.restartSession(session, realm, code);
                        if (clientSession != null) {
                            event.clone().detail(Details.RESTART_AFTER_TIMEOUT, "true").error(Errors.EXPIRED_CODE);
                            response = processFlow(null, clientSession, AUTHENTICATE_PATH, realm.getBrowserFlow(), Messages.LOGIN_TIMEOUT, new AuthenticationProcessor());
                            return false;
                        }
                    } catch (Exception e) {
                        logger.failedToParseRestartLoginCookie(e);
                    }
                }
                event.error(Errors.INVALID_CODE);
                response = ErrorPage.error(session, Messages.INVALID_CODE);
                return false;
            }
            ClientSessionModel clientSession = clientCode.getClientSession();
            if (clientSession == null) {
                event.error(Errors.INVALID_CODE);
                response = ErrorPage.error(session, Messages.INVALID_CODE);
                return false;
            }
            event.detail(Details.CODE_ID, clientSession.getId());
            ClientModel client = clientSession.getClient();
            if (client == null) {
                event.error(Errors.CLIENT_NOT_FOUND);
                response = ErrorPage.error(session, Messages.UNKNOWN_LOGIN_REQUESTER);
                session.sessions().removeClientSession(realm, clientSession);
                return false;
            }
            if (!client.isEnabled()) {
                event.error(Errors.CLIENT_NOT_FOUND);
                response = ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED);
                session.sessions().removeClientSession(realm, clientSession);
                return false;
            }
            session.getContext().setClient(client);
            return true;
        }

        public boolean verifyRequiredAction(String code, String executedAction) {
            if (!verifyCode(code)) {
                return false;
            }
            if (!isValidAction(ClientSessionModel.Action.REQUIRED_ACTIONS.name())) return false;
            if (!isActionActive(ClientSessionCode.ActionType.USER)) return false;

            final ClientSessionModel clientSession = clientCode.getClientSession();

            final UserSessionModel userSession = clientSession.getUserSession();
            if (userSession == null) {
                logger.userSessionNull();
                event.error(Errors.USER_SESSION_NOT_FOUND);
                throw new WebApplicationException(ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE));
            }
            if (!AuthenticationManager.isSessionValid(realm, userSession)) {
                AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
                event.error(Errors.INVALID_CODE);
                response = ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE);
                return false;
            }

            if (executedAction == null && userSession != null) { // do next required action only if user is already authenticated
                initEvent(clientSession);
                event.event(EventType.LOGIN);
                response = AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
                return false;
            }

            if (!executedAction.equals(clientSession.getNote(AuthenticationManager.CURRENT_REQUIRED_ACTION))) {
                logger.debug("required action doesn't match current required action");
                clientSession.removeNote(AuthenticationManager.CURRENT_REQUIRED_ACTION);
                response = redirectToRequiredActions(code);
                return false;
            }
            return true;

        }
    }


    /**
     * protocol independent login page entry point
     *
     * @param code
     * @return
     */
    @Path(AUTHENTICATE_PATH)
    @GET
    public Response authenticate(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        event.event(EventType.LOGIN);
        Checks checks = new Checks();
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();

        return processAuthentication(execution, clientSession, null);
    }

    protected Response processAuthentication(String execution, ClientSessionModel clientSession, String errorMessage) {
        return processFlow(execution, clientSession, AUTHENTICATE_PATH, realm.getBrowserFlow(), errorMessage, new AuthenticationProcessor());
    }

    protected Response processFlow(String execution, ClientSessionModel clientSession, String flowPath, AuthenticationFlowModel flow, String errorMessage, AuthenticationProcessor processor) {
        processor.setClientSession(clientSession)
                .setFlowPath(flowPath)
                .setBrowserFlow(true)
                .setFlowId(flow.getId())
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);
        if (errorMessage != null) processor.setForwardedErrorMessage(new FormMessage(null, errorMessage));

        try {
            if (execution != null) {
                return processor.authenticationAction(execution);
            } else {
                return processor.authenticate();
            }
        } catch (Exception e) {
            return processor.handleBrowserException(e);
        }
    }

    /**
     * URL called after login page.  YOU SHOULD NEVER INVOKE THIS DIRECTLY!
     *
     * @param code
     * @return
     */
    @Path(AUTHENTICATE_PATH)
    @POST
    public Response authenticateForm(@QueryParam("code") String code,
                                     @QueryParam("execution") String execution) {
        event.event(EventType.LOGIN);
        Checks checks = new Checks();
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }
        final ClientSessionCode clientCode = checks.clientCode;
        final ClientSessionModel clientSession = clientCode.getClientSession();

        return processAuthentication(execution, clientSession, null);
    }

    @Path(RESET_CREDENTIALS_PATH)
    @POST
    public Response resetCredentialsPOST(@QueryParam("code") String code,
                                         @QueryParam("execution") String execution) {
        return resetCredentials(code, execution);
    }

    /**
     * Endpoint for executing reset credentials flow.  If code is null, a client session is created with the account
     * service as the client.  Successful reset sends you to the account page.  Note, account service must be enabled.
     *
     * @param code
     * @param execution
     * @return
     */
    @Path(RESET_CREDENTIALS_PATH)
    @GET
    public Response resetCredentialsGET(@QueryParam("code") String code,
                                         @QueryParam("execution") String execution) {
        // we allow applications to link to reset credentials without going through OAuth or SAML handshakes
        //
        if (code == null) {
            if (!realm.isResetPasswordAllowed()) {
                event.event(EventType.RESET_PASSWORD);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

            }
            // set up the account service as the endpoint to call.
            ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
            clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
            //clientSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
            clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
            String redirectUri = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString();
            clientSession.setRedirectUri(redirectUri);
            clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
            clientSession.setNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
            clientSession.setNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
            clientSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
            return processResetCredentials(null, clientSession, null);
        }
        return resetCredentials(code, execution);
    }

    protected Response resetCredentials(String code, String execution) {
        event.event(EventType.RESET_PASSWORD);
        Checks checks = new Checks();
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.USER)) {
            return checks.response;
        }
        final ClientSessionCode clientCode = checks.clientCode;
        final ClientSessionModel clientSession = clientCode.getClientSession();

        if (!realm.isResetPasswordAllowed()) {
            event.client(clientCode.getClientSession().getClient());
            event.error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

        }

        return processResetCredentials(execution, clientSession, null);
    }

    protected Response processResetCredentials(String execution, ClientSessionModel clientSession, String errorMessage) {
        AuthenticationProcessor authProcessor = new AuthenticationProcessor() {

            @Override
            protected Response authenticationComplete() {
                boolean firstBrokerLoginInProgress = (clientSession.getNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE) != null);
                if (firstBrokerLoginInProgress) {

                    UserModel linkingUser = AbstractIdpAuthenticator.getExistingUser(session, realm, clientSession);
                    if (!linkingUser.getId().equals(clientSession.getAuthenticatedUser().getId())) {
                        return ErrorPage.error(session, Messages.IDENTITY_PROVIDER_DIFFERENT_USER_MESSAGE, clientSession.getAuthenticatedUser().getUsername(), linkingUser.getUsername());
                    }

                    logger.debugf("Forget-password flow finished when authenticated user '%s' after first broker login.", linkingUser.getUsername());

                    return redirectToAfterBrokerLoginEndpoint(clientSession, true);
                } else {
                    return super.authenticationComplete();
                }
            }
        };

        return processFlow(execution, clientSession, RESET_CREDENTIALS_PATH, realm.getResetCredentialsFlow(), errorMessage, authProcessor);
    }


    protected Response processRegistration(String execution, ClientSessionModel clientSession, String errorMessage) {
        return processFlow(execution, clientSession, REGISTRATION_PATH, realm.getRegistrationFlow(), errorMessage, new AuthenticationProcessor());
    }


    /**
     * protocol independent registration page entry point
     *
     * @param code
     * @return
     */
    @Path(REGISTRATION_PATH)
    @GET
    public Response registerPage(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        event.event(EventType.REGISTER);
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorPage.error(session, Messages.REGISTRATION_NOT_ALLOWED);
        }

        Checks checks = new Checks();
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();


        AuthenticationManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        return processRegistration(execution, clientSession, null);
    }


    /**
     * Registration
     *
     * @param code
     * @return
     */
    @Path(REGISTRATION_PATH)
    @POST
    public Response processRegister(@QueryParam("code") String code,
                                    @QueryParam("execution") String execution) {
        event.event(EventType.REGISTER);
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorPage.error(session, Messages.REGISTRATION_NOT_ALLOWED);
        }
        Checks checks = new Checks();
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }

        ClientSessionCode clientCode = checks.clientCode;
        ClientSessionModel clientSession = clientCode.getClientSession();

        return processRegistration(execution, clientSession, null);
    }


    @Path(FIRST_BROKER_LOGIN_PATH)
    @GET
    public Response firstBrokerLoginGet(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, true);
    }

    @Path(FIRST_BROKER_LOGIN_PATH)
    @POST
    public Response firstBrokerLoginPost(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, true);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @GET
    public Response postBrokerLoginGet(@QueryParam("code") String code,
                                       @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, false);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @POST
    public Response postBrokerLoginPost(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, false);
    }


    protected Response brokerLoginFlow(String code, String execution, final boolean firstBrokerLogin) {
        EventType eventType = firstBrokerLogin ? EventType.IDENTITY_PROVIDER_FIRST_LOGIN : EventType.IDENTITY_PROVIDER_POST_LOGIN;
        event.event(eventType);

        Checks checks = new Checks();
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        final ClientSessionModel clientSessionn = clientSessionCode.getClientSession();

        String noteKey = firstBrokerLogin ? AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE : PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT;
        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromClientSession(clientSessionn, noteKey);
        if (serializedCtx == null) {
            logger.notFoundSerializedCtxInClientSession(noteKey);
            throw new WebApplicationException(ErrorPage.error(session, "Not found serialized context in clientSession."));
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(session, clientSessionn);
        final String identityProviderAlias = brokerContext.getIdpConfig().getAlias();

        String flowId = firstBrokerLogin ? brokerContext.getIdpConfig().getFirstBrokerLoginFlowId() : brokerContext.getIdpConfig().getPostBrokerLoginFlowId();
        if (flowId == null) {
            logger.flowNotConfigForIDP(identityProviderAlias);
            throw new WebApplicationException(ErrorPage.error(session, "Flow not configured for identity provider"));
        }
        AuthenticationFlowModel brokerLoginFlow = realm.getAuthenticationFlowById(flowId);
        if (brokerLoginFlow == null) {
            logger.flowNotFoundForIDP(flowId, identityProviderAlias);
            throw new WebApplicationException(ErrorPage.error(session, "Flow not found for identity provider"));
        }

        event.detail(Details.IDENTITY_PROVIDER, identityProviderAlias)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, brokerContext.getUsername());


        AuthenticationProcessor processor = new AuthenticationProcessor() {

            @Override
            protected Response authenticationComplete() {
                if (!firstBrokerLogin) {
                    String authStateNoteKey = PostBrokerLoginConstants.PBL_AUTH_STATE_PREFIX + identityProviderAlias;
                    clientSessionn.setNote(authStateNoteKey, "true");
                }

                return redirectToAfterBrokerLoginEndpoint(clientSession, firstBrokerLogin);
            }

        };

        String flowPath = firstBrokerLogin ? FIRST_BROKER_LOGIN_PATH : POST_BROKER_LOGIN_PATH;
        return processFlow(execution, clientSessionn, flowPath, brokerLoginFlow, null, processor);
    }

    private Response redirectToAfterBrokerLoginEndpoint(ClientSessionModel clientSession, boolean firstBrokerLogin) {
        ClientSessionCode accessCode = new ClientSessionCode(realm, clientSession);
        clientSession.setTimestamp(Time.currentTime());

        URI redirect = firstBrokerLogin ? Urls.identityProviderAfterFirstBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getCode()) :
                Urls.identityProviderAfterPostBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getCode()) ;
        logger.debugf("Redirecting to '%s' ", redirect);

        return Response.status(302).location(redirect).build();
    }

    /**
     * OAuth grant page.  You should not invoked this directly!
     *
     * @param formData
     * @return
     */
    @Path("consent")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processConsent(final MultivaluedMap<String, String> formData) {
        event.event(EventType.LOGIN);
        String code = formData.getFirst("code");
        Checks checks = new Checks();
        if (!checks.verifyRequiredAction(code, ClientSessionModel.Action.OAUTH_GRANT.name())) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();

        initEvent(clientSession);

        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();
        ClientModel client = clientSession.getClient();


        if (formData.containsKey("cancel")) {
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, clientSession.getAuthMethod());
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo)
                    .setEventBuilder(event);
            Response response = protocol.sendError(clientSession, Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
            return response;
        }

        UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user, client.getId());
        if (grantedConsent == null) {
            grantedConsent = new UserConsentModel(client);
            session.users().addConsent(realm, user, grantedConsent);
        }
        for (RoleModel role : accessCode.getRequestedRoles()) {
            grantedConsent.addGrantedRole(role);
        }
        for (ProtocolMapperModel protocolMapper : accessCode.getRequestedProtocolMappers()) {
            if (protocolMapper.isConsentRequired() && protocolMapper.getConsentText() != null) {
                grantedConsent.addGrantedProtocolMapper(protocolMapper);
            }
        }
        session.users().updateConsent(realm, user, grantedConsent);

        event.detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        event.success();

        return AuthenticationManager.redirectAfterSuccessfulFlow(session, realm, userSession, clientSession, request, uriInfo, clientConnection, event);
    }

    @Path("email-verification")
    @GET
    public Response emailVerification(@QueryParam("code") String code, @QueryParam("key") String key) {
        event.event(EventType.VERIFY_EMAIL);
        if (key != null) {
            Checks checks = new Checks();
            if (!checks.verifyCode(code, ClientSessionModel.Action.REQUIRED_ACTIONS.name(), ClientSessionCode.ActionType.USER)) {
                if (checks.clientCode == null && checks.result.isClientSessionNotFound() || checks.result.isIllegalHash()) {
                   return ErrorPage.error(session, Messages.STALE_VERIFY_EMAIL_LINK);
                }
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            if (!ClientSessionModel.Action.VERIFY_EMAIL.name().equals(clientSession.getNote(AuthenticationManager.CURRENT_REQUIRED_ACTION))) {
                logger.reqdActionDoesNotMatch();
                event.error(Errors.INVALID_CODE);
                throw new WebApplicationException(ErrorPage.error(session, Messages.STALE_VERIFY_EMAIL_LINK));
            }

            UserSessionModel userSession = clientSession.getUserSession();
            UserModel user = userSession.getUser();
            initEvent(clientSession);
            event.event(EventType.VERIFY_EMAIL).detail(Details.EMAIL, user.getEmail());

            String keyFromSession = clientSession.getNote(Constants.VERIFY_EMAIL_KEY);
            clientSession.removeNote(Constants.VERIFY_EMAIL_KEY);
            if (!key.equals(keyFromSession)) {
                logger.invalidKeyForEmailVerification();
                event.error(Errors.INVALID_USER_CREDENTIALS);
                throw new WebApplicationException(ErrorPage.error(session, Messages.INVALID_CODE));
            }

            user.setEmailVerified(true);

            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);

            event.success();

            String actionCookieValue = getActionCookie();
            if (actionCookieValue == null || !actionCookieValue.equals(userSession.getId())) {
                session.sessions().removeClientSession(realm, clientSession);
                return session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.EMAIL_VERIFIED)
                        .createInfoPage();
            }

            event = event.clone().removeDetail(Details.EMAIL).event(EventType.LOGIN);

            return AuthenticationProcessor.redirectToRequiredActions(realm, clientSession, uriInfo);
        } else {
            Checks checks = new Checks();
            if (!checks.verifyCode(code, ClientSessionModel.Action.REQUIRED_ACTIONS.name(), ClientSessionCode.ActionType.USER)) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            UserSessionModel userSession = clientSession.getUserSession();
            initEvent(clientSession);

            createActionCookie(realm, uriInfo, clientConnection, userSession.getId());

            VerifyEmail.setupKey(clientSession);

            return session.getProvider(LoginFormsProvider.class)
                    .setClientSessionCode(accessCode.getCode())
                    .setClientSession(clientSession)
                    .setUser(userSession.getUser())
                    .createResponse(RequiredAction.VERIFY_EMAIL);
        }
    }

    /**
     * Initiated by admin, not the user on login
     *
     * @param key
     * @return
     */
    @Path("execute-actions")
    @GET
    public Response executeActions(@QueryParam("key") String key) {
        event.event(EventType.EXECUTE_ACTIONS);
        if (key != null) {
            Checks checks = new Checks();
            if (!checks.verifyCode(key, ClientSessionModel.Action.EXECUTE_ACTIONS.name(), ClientSessionCode.ActionType.USER)) {
                return checks.response;
            }
            ClientSessionModel clientSession = checks.clientCode.getClientSession();
            // verify user email as we know it is valid as this entry point would never have gotten here.
            clientSession.getUserSession().getUser().setEmailVerified(true);
            clientSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
            clientSession.setNote(ClientSessionModel.Action.EXECUTE_ACTIONS.name(), "true");
            return AuthenticationProcessor.redirectToRequiredActions(realm, clientSession, uriInfo);
        } else {
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.INVALID_CODE);
        }
    }

    private String getActionCookie() {
        return getActionCookie(headers, realm, uriInfo, clientConnection);
    }

    public static String getActionCookie(HttpHeaders headers, RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection) {
        Cookie cookie = headers.getCookies().get(ACTION_COOKIE);
        AuthenticationManager.expireCookie(realm, ACTION_COOKIE, AuthenticationManager.getRealmCookiePath(realm, uriInfo), realm.getSslRequired().isRequired(clientConnection), clientConnection);
        return cookie != null ? cookie.getValue() : null;
    }

    public static void createActionCookie(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, String sessionId) {
        CookieHelper.addCookie(ACTION_COOKIE, sessionId, AuthenticationManager.getRealmCookiePath(realm, uriInfo), null, null, -1, realm.getSslRequired().isRequired(clientConnection), true);
    }

    private void initEvent(ClientSessionModel clientSession) {
        UserSessionModel userSession = clientSession.getUserSession();

        String responseType = clientSession.getNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        if (responseType == null) {
            responseType = "code";
        }
        String respMode = clientSession.getNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        OIDCResponseMode responseMode = OIDCResponseMode.parse(respMode, OIDCResponseType.parse(responseType));

        event.event(EventType.LOGIN).client(clientSession.getClient())
                .user(userSession.getUser())
                .session(userSession.getId())
                .detail(Details.CODE_ID, clientSession.getId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.USERNAME, clientSession.getNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME))
                .detail(Details.AUTH_METHOD, userSession.getAuthMethod())
                .detail(Details.USERNAME, userSession.getLoginUsername())
                .detail(Details.RESPONSE_TYPE, responseType)
                .detail(Details.RESPONSE_MODE, responseMode.toString().toLowerCase())
                .detail(Details.IDENTITY_PROVIDER, userSession.getNote(Details.IDENTITY_PROVIDER))
                .detail(Details.IDENTITY_PROVIDER_USERNAME, userSession.getNote(Details.IDENTITY_PROVIDER_USERNAME));

        if (userSession.isRememberMe()) {
            event.detail(Details.REMEMBER_ME, "true");
        }
    }

    @Path(REQUIRED_ACTION)
    @POST
    public Response requiredActionPOST(@QueryParam("code") final String code,
                                       @QueryParam("action") String action) {
        return processRequireAction(code, action);



    }

    @Path(REQUIRED_ACTION)
    @GET
    public Response requiredActionGET(@QueryParam("code") final String code,
                                       @QueryParam("action") String action) {
        return processRequireAction(code, action);
    }

    public Response processRequireAction(final String code, String action) {
        event.event(EventType.CUSTOM_REQUIRED_ACTION);
        event.detail(Details.CUSTOM_REQUIRED_ACTION, action);
        Checks checks = new Checks();
        if (!checks.verifyRequiredAction(code, action)) {
            return checks.response;
        }
        final ClientSessionCode clientCode = checks.clientCode;
        final ClientSessionModel clientSession = clientCode.getClientSession();

        final UserSessionModel userSession = clientSession.getUserSession();

        RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, action);
        if (factory == null) {
            logger.actionProviderNull();
            event.error(Errors.INVALID_CODE);
            throw new WebApplicationException(ErrorPage.error(session, Messages.INVALID_CODE));
        }
        RequiredActionProvider provider = factory.create(session);

        initEvent(clientSession);
        event.event(EventType.CUSTOM_REQUIRED_ACTION);


        RequiredActionContextResult context = new RequiredActionContextResult(userSession, clientSession, realm, event, session, request, userSession.getUser(), factory) {
            @Override
            public void ignore() {
                throw new RuntimeException("Cannot call ignore within processAction()");
            }
        };
        provider.processAction(context);
        if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().success();
            initEvent(clientSession);
            event.event(EventType.LOGIN);
            clientSession.removeRequiredAction(factory.getId());
            userSession.getUser().removeRequiredAction(factory.getId());
            clientSession.removeNote(AuthenticationManager.CURRENT_REQUIRED_ACTION);

            if (AuthenticationManager.isActionRequired(session, userSession, clientSession, clientConnection, request, uriInfo, event)) {
                // redirect to a generic code URI so that browser refresh will work
                return redirectToRequiredActions(code);
            } else {
                return AuthenticationManager.finishedRequiredActions(session, userSession, clientSession, clientConnection, request, uriInfo, event);

            }
        }
        if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            return context.getChallenge();
        }
        if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, context.getClientSession().getAuthMethod());
            protocol.setRealm(context.getRealm())
                    .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                    .setUriInfo(context.getUriInfo())
                    .setEventBuilder(event);

            event.detail(Details.CUSTOM_REQUIRED_ACTION, action);
            Response response = protocol.sendError(context.getClientSession(), Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
            return response;

        }

        throw new RuntimeException("Unreachable");
    }

    public Response redirectToRequiredActions(String code) {
        URI redirect = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION)
                .queryParam(OAuth2Constants.CODE, code).build(realm.getName());
        return Response.status(302).location(redirect).build();
    }

}
