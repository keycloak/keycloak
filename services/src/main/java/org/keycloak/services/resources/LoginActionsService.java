/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.Time;

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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginActionsService {

    protected static final Logger logger = Logger.getLogger(LoginActionsService.class);

    public static final String ACTION_COOKIE = "KEYCLOAK_ACTION";
    public static final String AUTHENTICATE_PATH = "authenticate";
    public static final String REGISTRATION_PATH = "registration";
    public static final String RESET_CREDENTIALS_PATH = "reset-credentials";

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

    private AuthenticationManager authManager;

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

    public static UriBuilder loginActionsBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path(RealmsResource.class, "getLoginActionsService");
    }

    public LoginActionsService(RealmModel realm, AuthenticationManager authManager, EventBuilder event) {
        this.realm = realm;
        this.authManager = authManager;
        this.event = event;
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

        boolean verifyCode(String code, String requiredAction) {
            if (!verifyCode(code)) {
                return false;
            } else if (!clientCode.isValidAction(requiredAction)) {
                event.client(clientCode.getClientSession().getClient());
                event.error(Errors.INVALID_CODE);
                response = ErrorPage.error(session, Messages.INVALID_CODE);
                return false;
            } else if (!clientCode.isActionActive(requiredAction)) {
                event.client(clientCode.getClientSession().getClient());
                event.clone().error(Errors.EXPIRED_CODE);
                if (clientCode.getClientSession().getAction().equals(ClientSessionModel.Action.AUTHENTICATE.name())) {
                    AuthenticationProcessor.resetFlow(clientCode.getClientSession());
                    response = processAuthentication(null, clientCode.getClientSession(), Messages.LOGIN_TIMEOUT);
                    return false;
                }
                response = ErrorPage.error(session, Messages.EXPIRED_CODE);
                return false;
            } else {
                return true;
            }
        }

        boolean verifyCode(String code, String requiredAction, String alternativeRequiredAction) {
            if (!verifyCode(code)) {
                return false;
            } else if (!(clientCode.isValidAction(requiredAction) || clientCode.isValidAction(alternativeRequiredAction))) {
                event.client(clientCode.getClientSession().getClient());
                event.error(Errors.INVALID_CODE);
                response = ErrorPage.error(session, Messages.INVALID_CODE);
                return false;
            } else if (!(clientCode.isActionActive(requiredAction) || clientCode.isActionActive(alternativeRequiredAction))) {
                event.client(clientCode.getClientSession().getClient());
                event.clone().error(Errors.EXPIRED_CODE);
                if (clientCode.getClientSession().getAction().equals(ClientSessionModel.Action.AUTHENTICATE.name())) {
                    AuthenticationProcessor.resetFlow(clientCode.getClientSession());
                    response = processAuthentication(null, clientCode.getClientSession(), Messages.LOGIN_TIMEOUT);
                } else {
                    if (clientCode.getClientSession().getUserSession() == null) {
                        session.sessions().removeClientSession(realm, clientCode.getClientSession());
                    }
                    response = ErrorPage.error(session, Messages.EXPIRED_CODE);
                }
                return false;
            } else {
                return true;
            }
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
            ClientSessionCode.ParseResult result = ClientSessionCode.parseResult(code, session, realm);
            clientCode = result.getCode();
            if (clientCode == null) {
                if (result.isClientSessionNotFound()) { // timeout
                    try {
                        ClientSessionModel clientSession = RestartLoginCookie.restartSession(session, realm, code);
                        if (clientSession != null) {
                            event.clone().detail(Details.RESTART_AFTER_TIMEOUT, "true").error(Errors.EXPIRED_CODE);
                            response = processFlow(null, clientSession, AUTHENTICATE_PATH, realm.getBrowserFlow(), Messages.LOGIN_TIMEOUT);
                            return false;
                        }
                    } catch (Exception e) {
                        logger.error("failed to parse RestartLoginCookie", e);
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
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();

        return processAuthentication(execution, clientSession, null);
    }

    protected Response processAuthentication(String execution, ClientSessionModel clientSession, String errorMessage) {
        return processFlow(execution, clientSession, AUTHENTICATE_PATH, realm.getBrowserFlow(), errorMessage);
    }

    protected Response processFlow(String execution, ClientSessionModel clientSession, String flowPath, AuthenticationFlowModel flow, String errorMessage) {
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setClientSession(clientSession)
                .setFlowPath(flowPath)
                .setFlowId(flow.getId())
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setProtector(authManager.getProtector())
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
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
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

    @Path(RESET_CREDENTIALS_PATH)
    @GET
    public Response resetCredentialsGET(@QueryParam("code") String code,
                                         @QueryParam("execution") String execution) {
        return resetCredentials(code, execution);
    }

    protected Response resetCredentials(String code, String execution) {
        event.event(EventType.RESET_PASSWORD);
        Checks checks = new Checks();
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
            return checks.response;
        }
        final ClientSessionCode clientCode = checks.clientCode;
        final ClientSessionModel clientSession = clientCode.getClientSession();

        return processResetCredentials(execution, clientSession, null);
    }

    protected Response processResetCredentials(String execution, ClientSessionModel clientSession, String errorMessage) {
        return processFlow(execution, clientSession, RESET_CREDENTIALS_PATH, realm.getResetCredentialsFlow(), errorMessage);
    }


    protected Response processRegistration(String execution, ClientSessionModel clientSession, String errorMessage) {
        return processFlow(execution, clientSession, REGISTRATION_PATH, realm.getRegistrationFlow(), errorMessage);
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
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();


        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

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
        if (!checks.verifyCode(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
            return checks.response;
        }

        ClientSessionCode clientCode = checks.clientCode;
        ClientSessionModel clientSession = clientCode.getClientSession();

        return processRegistration(execution, clientSession, null);
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
        event.event(EventType.LOGIN).detail(Details.RESPONSE_TYPE, "code");


        if (!checkSsl()) {
            return ErrorPage.error(session, Messages.HTTPS_REQUIRED);
        }

        String code = formData.getFirst("code");

        ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
        if (accessCode == null || !accessCode.isValid(ClientSessionModel.Action.OAUTH_GRANT.name())) {
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.INVALID_ACCESS_CODE);
        }
        ClientSessionModel clientSession = accessCode.getClientSession();
        event.detail(Details.CODE_ID, clientSession.getId());

        String redirect = clientSession.getRedirectUri();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();
        ClientModel client = clientSession.getClient();

        event.client(client)
                .user(user)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.REDIRECT_URI, redirect);

        event.detail(Details.AUTH_METHOD, userSession.getAuthMethod());
        event.detail(Details.USERNAME, userSession.getLoginUsername());
        if (userSession.isRememberMe()) {
            event.detail(Details.REMEMBER_ME, "true");
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE);
        }
        event.session(userSession);

        if (formData.containsKey("cancel")) {
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, clientSession.getAuthMethod());
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);
            event.error(Errors.REJECTED_BY_USER);
            return protocol.consentDenied(clientSession);
        }

        UserConsentModel grantedConsent = user.getConsentByClient(client.getId());
        if (grantedConsent == null) {
            grantedConsent = new UserConsentModel(client);
            user.addConsent(grantedConsent);
        }
        for (RoleModel role : accessCode.getRequestedRoles()) {
            grantedConsent.addGrantedRole(role);
        }
        for (ProtocolMapperModel protocolMapper : accessCode.getRequestedProtocolMappers()) {
            if (protocolMapper.isConsentRequired() && protocolMapper.getConsentText() != null) {
                grantedConsent.addGrantedProtocolMapper(protocolMapper);
            }
        }
        user.updateConsent(grantedConsent);

        event.detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        event.success();

        return authManager.redirectAfterSuccessfulFlow(session, realm, userSession, clientSession, request, uriInfo, clientConnection);
    }

    @Path("email-verification")
    @GET
    public Response emailVerification(@QueryParam("code") String code, @QueryParam("key") String key) {
        event.event(EventType.VERIFY_EMAIL);
        if (key != null) {
            Checks checks = new Checks();
            if (!checks.verifyCode(key, ClientSessionModel.Action.VERIFY_EMAIL.name())) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            UserSessionModel userSession = clientSession.getUserSession();
            UserModel user = userSession.getUser();
            initEvent(clientSession);
            user.setEmailVerified(true);

            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);

            event.event(EventType.VERIFY_EMAIL).detail(Details.EMAIL, user.getEmail()).success();

            String actionCookieValue = getActionCookie();
            if (actionCookieValue == null || !actionCookieValue.equals(userSession.getId())) {
                session.sessions().removeClientSession(realm, clientSession);
                return session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.EMAIL_VERIFIED)
                        .createInfoPage();
            }

            event = event.clone().removeDetail(Details.EMAIL).event(EventType.LOGIN);

            return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
        } else {
            Checks checks = new Checks();
            if (!checks.verifyCode(code, ClientSessionModel.Action.VERIFY_EMAIL.name())) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            UserSessionModel userSession = clientSession.getUserSession();
            initEvent(clientSession);

            createActionCookie(realm, uriInfo, clientConnection, userSession.getId());

            return session.getProvider(LoginFormsProvider.class)
                    .setClientSessionCode(accessCode.getCode())
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
            if (!checks.verifyCode(key, ClientSessionModel.Action.EXECUTE_ACTIONS.name())) {
                return checks.response;
            }
            ClientSessionModel clientSession = checks.clientCode.getClientSession();
            clientSession.setNote("END_AFTER_REQUIRED_ACTIONS", "true");
            clientSession.setNote(ClientSessionModel.Action.EXECUTE_ACTIONS.name(), "true");
            return AuthenticationManager.nextActionAfterAuthentication(session, clientSession.getUserSession(), clientSession, clientConnection, request, uriInfo, event);
        } else {
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.INVALID_CODE);
        }
    }

    private String getActionCookie() {
        Cookie cookie = headers.getCookies().get(ACTION_COOKIE);
        AuthenticationManager.expireCookie(realm, ACTION_COOKIE, AuthenticationManager.getRealmCookiePath(realm, uriInfo), realm.getSslRequired().isRequired(clientConnection), clientConnection);
        return cookie != null ? cookie.getValue() : null;
    }

    public static void createActionCookie(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, String sessionId) {
        CookieHelper.addCookie(ACTION_COOKIE, sessionId, AuthenticationManager.getRealmCookiePath(realm, uriInfo), null, null, -1, realm.getSslRequired().isRequired(clientConnection), true);
    }

    private void initEvent(ClientSessionModel clientSession) {
        event.event(EventType.LOGIN).client(clientSession.getClient())
                .user(clientSession.getUserSession().getUser())
                .session(clientSession.getUserSession().getId())
                .detail(Details.CODE_ID, clientSession.getId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.USERNAME, clientSession.getNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME))
                .detail(Details.RESPONSE_TYPE, "code");

        UserSessionModel userSession = clientSession.getUserSession();

        if (userSession != null) {
            event.detail(Details.AUTH_METHOD, userSession.getAuthMethod());
            event.detail(Details.USERNAME, userSession.getLoginUsername());
            if (userSession.isRememberMe()) {
                event.detail(Details.REMEMBER_ME, "true");
            }
        }
    }

    @Path("required-action")
    @POST
    public Response requiredActionPOST(@QueryParam("code") final String code,
                                       @QueryParam("action") String action) {
        return processRequireAction(code, action);



    }

    @Path("required-action")
    @GET
    public Response requiredActionGET(@QueryParam("code") final String code,
                                       @QueryParam("action") String action) {
        return processRequireAction(code, action);
    }

    public Response processRequireAction(final String code, String action) {
        event.event(EventType.CUSTOM_REQUIRED_ACTION);
        event.detail(Details.CUSTOM_REQUIRED_ACTION, action);
        if (action == null) {
            logger.error("required action query param was null");
            event.error(Errors.INVALID_CODE);
            throw new WebApplicationException(ErrorPage.error(session, Messages.INVALID_CODE));

        }

        RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, action);
        if (factory == null) {
            logger.error("required action provider was null");
            event.error(Errors.INVALID_CODE);
            throw new WebApplicationException(ErrorPage.error(session, Messages.INVALID_CODE));
        }
        RequiredActionProvider provider = factory.create(session);
        Checks checks = new Checks();
        if (!checks.verifyCode(code, action)) {
            return checks.response;
        }
        final ClientSessionCode clientCode = checks.clientCode;
        final ClientSessionModel clientSession = clientCode.getClientSession();

        if (clientSession.getUserSession() == null) {
            logger.error("user session was null");
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new WebApplicationException(ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE));
        }

        initEvent(clientSession);
        event.event(EventType.CUSTOM_REQUIRED_ACTION);


        RequiredActionContextResult context = new RequiredActionContextResult(clientSession.getUserSession(), clientSession, realm, event, session, request, clientSession.getUserSession().getUser(), factory) {
             @Override
            public String generateAccessCode(String action) {
                String clientSessionAction = clientSession.getAction();
                if (action.equals(clientSessionAction)) {
                    clientSession.setTimestamp(Time.currentTime());
                    return code;
                }
                ClientSessionCode code = new ClientSessionCode(getRealm(), getClientSession());
                code.setAction(action);
                return code.getCode();
            }

            @Override
            public void ignore() {
                throw new RuntimeException("Cannot call ignore within processAction()");
            }
        };
        provider.processAction(context);
        if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().success();
            // do both
            clientSession.removeRequiredAction(factory.getId());
            clientSession.getUserSession().getUser().removeRequiredAction(factory.getId());
            event.event(EventType.LOGIN);
            return AuthenticationManager.nextActionAfterAuthentication(session, clientSession.getUserSession(), clientSession, clientConnection, request, uriInfo, event);
        }
        if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            return context.getChallenge();
        }
        if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, context.getClientSession().getAuthMethod());
            protocol.setRealm(context.getRealm())
                    .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                    .setUriInfo(context.getUriInfo());
            event.detail(Details.CUSTOM_REQUIRED_ACTION, action).error(Errors.REJECTED_BY_USER);
            return protocol.consentDenied(context.getClientSession());
        }

        throw new RuntimeException("Unreachable");
    }

}
