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
import org.jboss.resteasy.spi.InternalServerErrorException;
import org.keycloak.ClientConnection;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.AbstractFormAuthenticator;
import org.keycloak.authentication.authenticators.UsernamePasswordFormFactory;
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
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.Time;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginActionsService {

    protected static final Logger logger = Logger.getLogger(LoginActionsService.class);

    public static final String ACTION_COOKIE = "KEYCLOAK_ACTION";

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

        boolean check(String code, String requiredAction) {
            if (!check(code)) {
                return false;
            } else if (!clientCode.isValidAction(requiredAction)) {
                event.client(clientCode.getClientSession().getClient());
                event.error(Errors.INVALID_CODE);
                response = ErrorPage.error(session, Messages.INVALID_CODE);
                return false;
            } else if (!clientCode.isActionActive(requiredAction)) {
                event.client(clientCode.getClientSession().getClient());
                event.error(Errors.EXPIRED_CODE);
                response = ErrorPage.error(session, Messages.EXPIRED_CODE);
                return false;
            } else {
                return true;
            }
        }

        boolean check(String code, String requiredAction, String alternativeRequiredAction) {
            if (!check(code)) {
                return false;
            } else if (!(clientCode.isValidAction(requiredAction) || clientCode.isValidAction(alternativeRequiredAction))) {
                event.client(clientCode.getClientSession().getClient());
                event.error(Errors.INVALID_CODE);
                response = ErrorPage.error(session, Messages.INVALID_CODE);
                return false;
            } else if (!(clientCode.isActionActive(requiredAction) || clientCode.isActionActive(alternativeRequiredAction))) {
                event.client(clientCode.getClientSession().getClient());
                event.error(Errors.EXPIRED_CODE);
                if (clientCode.getClientSession().getAction().equals(ClientSessionModel.Action.AUTHENTICATE.name())) {
                    AuthenticationProcessor.resetFlow(clientCode.getClientSession());
                    response = processAuthentication(null, clientCode.getClientSession());
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

        public boolean check(String code) {
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
            clientCode = ClientSessionCode.parse(code, session, realm);
            if (clientCode == null) {
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
     *
     * @param code
     * @return
     */
    @Path("authenticate")
    @GET
    public Response authenticate(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        event.event(EventType.LOGIN);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionModel.Action.RECOVER_PASSWORD.name())) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();

        if (clientSession.getAction().equals(ClientSessionModel.Action.RECOVER_PASSWORD.name())) {
            TokenManager.dettachClientSession(session.sessions(), realm, clientSession);
            clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        }

        return processAuthentication(execution, clientSession);
    }

    protected Response processAuthentication(String execution, ClientSessionModel clientSession) {
        String flowAlias = DefaultAuthenticationFlows.BROWSER_FLOW;
        return processFlow(execution, clientSession, flowAlias);
    }

    protected Response processFlow(String execution, ClientSessionModel clientSession, String flowAlias) {
        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setClientSession(clientSession)
                .setFlowId(flow.getId())
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setProtector(authManager.getProtector())
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);

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
    @Path("authenticate")
    @POST
    public Response authenticateForm(@QueryParam("code") String code,
                                     @QueryParam("execution") String execution) {
        event.event(EventType.LOGIN);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
            return checks.response;
        }
        final ClientSessionCode clientCode = checks.clientCode;
        final ClientSessionModel clientSession = clientCode.getClientSession();

        return processAuthentication(execution, clientSession);
    }

    protected Response processRegistration(String execution, ClientSessionModel clientSession) {
        String flowAlias = DefaultAuthenticationFlows.REGISTRATION_FLOW;
        return processFlow(execution, clientSession, flowAlias);
    }



    /**
     * protocol independent registration page entry point
     *
     * @param code
     * @return
     */
    @Path("registration")
    @GET
    public Response registerPage(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        event.event(EventType.REGISTER);
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorPage.error(session, Messages.REGISTRATION_NOT_ALLOWED);
        }

        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();


        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        return processRegistration(execution, clientSession);
    }


    /**
     * Registration
     *
     * @param code
     * @return
     */
    @Path("registration")
    @POST
    public Response processRegister(@QueryParam("code") String code,
                                    @QueryParam("execution") String execution) {
        event.event(EventType.REGISTER);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.AUTHENTICATE.name())) {
            return checks.response;
        }
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorPage.error(session, Messages.REGISTRATION_NOT_ALLOWED);
        }

        ClientSessionCode clientCode = checks.clientCode;
        ClientSessionModel clientSession = clientCode.getClientSession();

        return processRegistration(execution, clientSession);
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

    @Path("profile")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateProfile(@QueryParam("code") String code,
                                  final MultivaluedMap<String, String> formData) {
        event.event(EventType.UPDATE_PROFILE);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.UPDATE_PROFILE.name())) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();

        initEvent(clientSession);

        List<FormMessage> errors = Validation.validateUpdateProfileForm(formData);
        if (errors != null && !errors.isEmpty()) {
            return session.getProvider(LoginFormsProvider.class)
                    .setClientSessionCode(accessCode.getCode())
                    .setUser(user)
                    .setErrors(errors)
                    .createResponse(RequiredAction.UPDATE_PROFILE);
        }

        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        String email = formData.getFirst("email");

        String oldEmail = user.getEmail();
        boolean emailChanged = oldEmail != null ? !oldEmail.equals(email) : email != null;

        if (emailChanged) {
            UserModel userByEmail = session.users().getUserByEmail(email, realm);

            // check for duplicated email
            if (userByEmail != null && !userByEmail.getId().equals(user.getId())) {
                return session.getProvider(LoginFormsProvider.class)
                        .setUser(user)
                        .setError(Messages.EMAIL_EXISTS)
                        .setClientSessionCode(accessCode.getCode())
                        .createResponse(RequiredAction.UPDATE_PROFILE);
            }

            user.setEmail(email);
            user.setEmailVerified(false);
        }

        AttributeFormDataProcessor.process(formData, realm, user);
        
        user.removeRequiredAction(RequiredAction.UPDATE_PROFILE);
        event.clone().event(EventType.UPDATE_PROFILE).success();

        if (emailChanged) {
            event.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, email).success();
        }

        return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateTotp(@QueryParam("code") String code,
                               final MultivaluedMap<String, String> formData) {
        event.event(EventType.UPDATE_TOTP);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.CONFIGURE_TOTP.name())) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();

        initEvent(clientSession);

        String totp = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");

        LoginFormsProvider loginForms = session.getProvider(LoginFormsProvider.class).setUser(user);
        if (Validation.isBlank(totp)) {
            return loginForms.setError(Messages.MISSING_TOTP)
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.CONFIGURE_TOTP);
        } else if (!new TimeBasedOTP().validate(totp, totpSecret.getBytes())) {
            return loginForms.setError(Messages.INVALID_TOTP)
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.CONFIGURE_TOTP);
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.TOTP);
        credentials.setValue(totpSecret);
        session.users().updateCredential(realm, user, credentials);

        user.setTotp(true);

        user.removeRequiredAction(RequiredAction.CONFIGURE_TOTP);

        event.clone().event(EventType.UPDATE_TOTP).success();

        return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updatePassword(@QueryParam("code") String code,
                                   final MultivaluedMap<String, String> formData) {
        event.event(EventType.UPDATE_PASSWORD);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.UPDATE_PASSWORD.name(), ClientSessionModel.Action.RECOVER_PASSWORD.name())) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();

        initEvent(clientSession);

        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        LoginFormsProvider loginForms = session.getProvider(LoginFormsProvider.class)
                .setUser(user);
        if (Validation.isBlank(passwordNew)) {
            return loginForms.setError(Messages.MISSING_PASSWORD)
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        } else if (!passwordNew.equals(passwordConfirm)) {
            return loginForms.setError(Messages.NOTMATCH_PASSWORD)
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        }

        try {
            session.users().updateCredential(realm, user, UserCredentialModel.password(passwordNew));
        } catch (ModelException me) {
            return loginForms.setError(me.getMessage(), me.getParameters())
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        } catch (Exception ape) {
            return loginForms.setError(ape.getMessage())
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        }

        user.removeRequiredAction(RequiredAction.UPDATE_PASSWORD);

        event.event(EventType.UPDATE_PASSWORD).success();

        if (clientSession.getAction().equals(ClientSessionModel.Action.RECOVER_PASSWORD.name())) {
            String actionCookieValue = getActionCookie();
            if (actionCookieValue == null || !actionCookieValue.equals(userSession.getId())) {
                return session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.ACCOUNT_PASSWORD_UPDATED)
                        .createInfoPage();
            }
        }

        event = event.clone().event(EventType.LOGIN);

        return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
    }


    @Path("email-verification")
    @GET
    public Response emailVerification(@QueryParam("code") String code, @QueryParam("key") String key) {
        event.event(EventType.VERIFY_EMAIL);
        if (key != null) {
            Checks checks = new Checks();
            if (!checks.check(key, ClientSessionModel.Action.VERIFY_EMAIL.name())) {
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
                return session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.EMAIL_VERIFIED)
                        .createInfoPage();
            }

            event = event.clone().removeDetail(Details.EMAIL).event(EventType.LOGIN);

            return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
        } else {
            Checks checks = new Checks();
            if (!checks.check(code, ClientSessionModel.Action.VERIFY_EMAIL.name())) {
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

    @Path("password-reset")
    @GET
    public Response passwordReset(@QueryParam("code") String code, @QueryParam("key") String key) {
        event.event(EventType.RESET_PASSWORD);
        if (key != null) {
            Checks checks = new Checks();
            if (!checks.check(key, ClientSessionModel.Action.RECOVER_PASSWORD.name())) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            return session.getProvider(LoginFormsProvider.class)
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        } else {
            return session.getProvider(LoginFormsProvider.class)
                    .setClientSessionCode(code)
                    .createPasswordReset();
        }
    }

    @Path("password-reset")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sendPasswordReset(@QueryParam("code") String code,
                                      final MultivaluedMap<String, String> formData) {
        event.event(EventType.SEND_RESET_PASSWORD);
        Checks checks = new Checks();
        if (!checks.check(code)) {
            return checks.response;
        }
        final ClientSessionCode accessCode = checks.clientCode;
        final ClientSessionModel clientSession = accessCode.getClientSession();
        ClientModel client = clientSession.getClient();


        String username = formData.getFirst("username");
        if(username == null || username.isEmpty()) {
            event.error(Errors.USERNAME_MISSING);
            return session.getProvider(LoginFormsProvider.class)
                    .setError(Messages.MISSING_USERNAME)
                    .setClientSessionCode(accessCode.getCode())
                    .createPasswordReset();
        }

        event.client(client.getClientId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.USERNAME, username);

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null && username.contains("@")) {
            user = session.users().getUserByEmail(username, realm);
        }

        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
        } else if(!user.isEnabled()) {
            event.user(user).error(Errors.USER_DISABLED);
        }
        else if(user.getEmail() == null || user.getEmail().trim().length() == 0) {
            event.user(user).error(Errors.INVALID_EMAIL);
        } else{
            event.user(user);

            UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "form", false, null, null);
            event.session(userSession);
            TokenManager.attachClientSession(userSession, clientSession);

            accessCode.setAction(ClientSessionModel.Action.RECOVER_PASSWORD.name());

            try {
                UriBuilder builder = Urls.loginPasswordResetBuilder(uriInfo.getBaseUri());
                builder.queryParam("key", accessCode.getCode());

                String link = builder.build(realm.getName()).toString();
                long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

                this.session.getProvider(EmailProvider.class).setRealm(realm).setUser(user).sendPasswordReset(link, expiration);

                event.detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, clientSession.getId()).success();
            } catch (EmailException e) {
                event.error(Errors.EMAIL_SEND_FAILED);
                logger.error("Failed to send password reset email", e);
                return session.getProvider(LoginFormsProvider.class)
                        .setError(Messages.EMAIL_SENT_ERROR)
                        .setClientSessionCode(accessCode.getCode())
                        .createErrorPage();
            }

            createActionCookie(realm, uriInfo, clientConnection, userSession.getId());
        }

        return session.getProvider(LoginFormsProvider.class)
                .setSuccess(Messages.EMAIL_SENT)
                .setClientSessionCode(accessCode.getCode())
                .createPasswordReset();
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

    @Path("required-actions/{action}")
    public Object requiredAction(@QueryParam("code") final String code,
                                 @PathParam("action") String action) {
        event.event(EventType.LOGIN);
        if (action == null) {
            logger.error("required action query param was null");
            event.error(Errors.INVALID_CODE);
            throw new WebApplicationException(ErrorPage.error(session, Messages.INVALID_CODE));

        }

        RequiredActionProvider provider = session.getProvider(RequiredActionProvider.class, action);
        if (provider == null) {
            logger.error("required action provider was null");
            event.error(Errors.INVALID_CODE);
            throw new WebApplicationException(ErrorPage.error(session, Messages.INVALID_CODE));
        }
        Checks checks = new Checks();
        if (!checks.check(code, action)) {
            return checks.response;
        }
        final ClientSessionCode clientCode = checks.clientCode;
        final ClientSessionModel clientSession = clientCode.getClientSession();

        if (clientSession.getUserSession() == null) {
            logger.error("user session was null");
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new WebApplicationException(ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE));
        }


        RequiredActionContext context = new RequiredActionContext() {
            @Override
            public EventBuilder getEvent() {
                return event;
            }

            @Override
            public UserModel getUser() {
                return getUserSession().getUser();
            }

            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public ClientSessionModel getClientSession() {
                return clientSession;
            }

            @Override
            public UserSessionModel getUserSession() {
                return clientSession.getUserSession();
            }

            @Override
            public ClientConnection getConnection() {
                return clientConnection;
            }

            @Override
            public UriInfo getUriInfo() {
                return uriInfo;
            }

            @Override
            public KeycloakSession getSession() {
                return session;
            }

            @Override
            public HttpRequest getHttpRequest() {
                return request;
            }

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
       };
        return provider.jaxrsService(context);



    }

}
