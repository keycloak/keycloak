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
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.PasswordToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
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

    public static UriBuilder loginActionsBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path(RealmsResource.class, "getLoginActionsService");
    }

    public static UriBuilder processLoginUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return processLoginUrl(baseUriBuilder);
    }

    public static UriBuilder processLoginUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = loginActionsBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "processLogin");
    }

    public static UriBuilder processOAuthUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return processOAuthUrl(baseUriBuilder);
    }

    public static UriBuilder processOAuthUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = loginActionsBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "processOAuth");
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

        boolean check(String code, ClientSessionModel.Action requiredAction) {
            if (!check(code)) {
                return false;
            } else if (!clientCode.isValid(requiredAction)) {
                event.error(Errors.INVALID_CODE);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.INVALID_CODE);
                return false;
            } else {
                return true;
            }
        }

        boolean check(String code, ClientSessionModel.Action requiredAction, ClientSessionModel.Action alternativeRequiredAction) {
            if (!check(code)) {
                return false;
            } else if (!(clientCode.isValid(requiredAction) || clientCode.isValid(alternativeRequiredAction))) {
                event.error(Errors.INVALID_CODE);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo,headers, Messages.INVALID_CODE);
                return false;
            } else {
                return true;
            }
        }

        public boolean check(String code) {
            if (!checkSsl()) {
                event.error(Errors.SSL_REQUIRED);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.HTTPS_REQUIRED);
                return false;
            }
            if (!realm.isEnabled()) {
                event.error(Errors.REALM_DISABLED);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REALM_NOT_ENABLED);
                return false;
            }
            clientCode = ClientSessionCode.parse(code, session, realm);
            if (clientCode == null) {
                event.error(Errors.INVALID_CODE);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.UNKNOWN_CODE);
                return false;
            }
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
    @Path("login")
    @GET
    public Response loginPage(@QueryParam("code") String code) {
        event.event(EventType.LOGIN);
        Checks checks = new Checks();
        if (!checks.check(code)) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();

        if (clientSession.getAction().equals(ClientSessionModel.Action.RECOVER_PASSWORD)) {
            TokenManager.dettachClientSession(session.sessions(), realm, clientSession);
            clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE);
        }

        LoginFormsProvider forms = Flows.forms(session, realm, clientSession.getClient(), uriInfo, headers)
                .setClientSessionCode(clientSessionCode.getCode());

        return forms.createLogin();
    }

    /**
     * protocol independent registration page entry point
     *
     * @param code
     * @return
     */
    @Path("registration")
    @GET
    public Response registerPage(@QueryParam("code") String code) {
        event.event(EventType.REGISTER);
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REGISTRATION_NOT_ALLOWED);
        }

        Checks checks = new Checks();
        if (!checks.check(code)) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        ClientSessionCode clientSessionCode = checks.clientCode;
        ClientSessionModel clientSession = clientSessionCode.getClientSession();


        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        return Flows.forms(session, realm, clientSession.getClient(), uriInfo, headers)
                .setClientSessionCode(clientSessionCode.getCode())
                .createRegistration();
    }

    /**
     * URL called after login page.  YOU SHOULD NEVER INVOKE THIS DIRECTLY!
     *
     * @param code
     * @param formData
     * @return
     */
    @Path("request/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processLogin(@QueryParam("code") String code,
                                 final MultivaluedMap<String, String> formData) {
        event.event(EventType.LOGIN);
        if (!checkSsl()) {
            event.error(Errors.SSL_REQUIRED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.HTTPS_REQUIRED);
        }

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REALM_NOT_ENABLED);
        }
        ClientSessionCode clientCode = ClientSessionCode.parse(code, session, realm);
        if (clientCode == null) {
            event.error(Errors.INVALID_CODE);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.UNKNOWN_CODE);
        }

        ClientSessionModel clientSession = clientCode.getClientSession();
        event.detail(Details.CODE_ID, clientSession.getId());

        if (!clientCode.isValid(ClientSessionModel.Action.AUTHENTICATE) || clientSession.getUserSession() != null) {
            clientCode.setAction(ClientSessionModel.Action.AUTHENTICATE);
            event.client(clientSession.getClient()).error(Errors.EXPIRED_CODE);
            return Flows.forms(this.session, realm, clientSession.getClient(), uriInfo, headers).setError(Messages.EXPIRED_CODE)
                    .setClientSessionCode(clientCode.getCode())
                    .createLogin();
        }

        String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);

        String rememberMe = formData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");

        event.client(clientSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.USERNAME, username);

        if (remember) {
            event.detail(Details.REMEMBER_ME, "true");
        }


        ClientModel client = clientSession.getClient();
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.UNKNOWN_LOGIN_REQUESTER);
        }
        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.LOGIN_REQUESTER_NOT_ENABLED);
        }

        if (formData.containsKey("cancel")) {
            event.error(Errors.REJECTED_BY_USER);
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, clientSession.getAuthMethod());
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);
            return protocol.cancelLogin(clientSession);
        }

        AuthenticationManager.AuthenticationStatus status = authManager.authenticateForm(session, clientConnection, realm, formData);

        if (remember) {
            authManager.createRememberMeCookie(realm, username, uriInfo, clientConnection);
        } else {
            authManager.expireRememberMeCookie(realm, uriInfo, clientConnection);
        }

        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);
        if (user != null) {
            event.user(user);
        }

        switch (status) {
            case SUCCESS:
            case ACTIONS_REQUIRED:
                UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "form", remember);
                TokenManager.attachClientSession(userSession, clientSession);
                event.session(userSession);
                return authManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
            case ACCOUNT_TEMPORARILY_DISABLED:
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                return Flows.forms(this.session, realm, client, uriInfo, headers)
                        .setError(Messages.ACCOUNT_TEMPORARILY_DISABLED)
                        .setFormData(formData)
                        .setClientSessionCode(clientCode.getCode())
                        .createLogin();
            case ACCOUNT_DISABLED:
                event.error(Errors.USER_DISABLED);
                return Flows.forms(this.session, realm, client, uriInfo, headers)
                        .setError(Messages.ACCOUNT_DISABLED)
                        .setClientSessionCode(clientCode.getCode())
                        .setFormData(formData).createLogin();
            case MISSING_TOTP:
                formData.remove(CredentialRepresentation.PASSWORD);

                String passwordToken = new JWSBuilder().jsonContent(new PasswordToken(realm.getName(), user.getId())).rsa256(realm.getPrivateKey());
                formData.add(CredentialRepresentation.PASSWORD_TOKEN, passwordToken);

                return Flows.forms(this.session, realm, client, uriInfo, headers)
                        .setFormData(formData)
                        .setClientSessionCode(clientCode.getCode())
                        .createLoginTotp();
            case INVALID_USER:
                event.error(Errors.USER_NOT_FOUND);
                return Flows.forms(this.session, realm, client, uriInfo, headers).setError(Messages.INVALID_USER)
                        .setFormData(formData)
                        .setClientSessionCode(clientCode.getCode())
                        .createLogin();
            default:
                event.error(Errors.INVALID_USER_CREDENTIALS);
                return Flows.forms(this.session, realm, client, uriInfo, headers).setError(Messages.INVALID_USER)
                        .setFormData(formData)
                        .setClientSessionCode(clientCode.getCode())
                        .createLogin();
        }
    }

    /**
     * Registration
     *
     * @param code
     * @param formData
     * @return
     */
    @Path("request/registration")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processRegister(@QueryParam("code") String code,
                                    final MultivaluedMap<String, String> formData) {
        event.event(EventType.REGISTER);
        if (!checkSsl()) {
            event.error(Errors.SSL_REQUIRED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.HTTPS_REQUIRED);
        }

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REALM_NOT_ENABLED);
        }
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REGISTRATION_NOT_ALLOWED);
        }
        ClientSessionCode clientCode = ClientSessionCode.parse(code, session, realm);
        if (clientCode == null) {
            event.error(Errors.INVALID_CODE);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.UNKNOWN_CODE);
        }
        if (!clientCode.isValid(ClientSessionModel.Action.AUTHENTICATE)) {
            event.error(Errors.INVALID_CODE);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.INVALID_CODE);
        }

        String username = formData.getFirst("username");
        String email = formData.getFirst("email");
        if (realm.isRegistrationEmailAsUsername()) {
            username = email;
            formData.putSingle(AuthenticationManager.FORM_USERNAME, username);
        }
        ClientSessionModel clientSession = clientCode.getClientSession();
        event.client(clientSession.getClient())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.REGISTER_METHOD, "form");

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REALM_NOT_ENABLED);
        }
        ClientModel client = clientSession.getClient();
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.UNKNOWN_LOGIN_REQUESTER);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.LOGIN_REQUESTER_NOT_ENABLED);
        }


        List<String> requiredCredentialTypes = new LinkedList<String>();
        for (RequiredCredentialModel m : realm.getRequiredCredentials()) {
            requiredCredentialTypes.add(m.getType());
        }

        // Validate here, so user is not created if password doesn't validate to passwordPolicy of current realm
        String errorMessage = Validation.validateRegistrationForm(realm, formData, requiredCredentialTypes);
        Object[] parameters = new Object[0];
        if (errorMessage == null) {
            PasswordPolicy.Error error = Validation.validatePassword(formData, realm.getPasswordPolicy());
            if(error != null){
                errorMessage = error.getMessage();
                parameters = error.getParameters();
            }
        }

        if (errorMessage != null) {
            event.error(Errors.INVALID_REGISTRATION);
            return Flows.forms(session, realm, client, uriInfo, headers)
                    .setError(errorMessage, parameters)
                    .setFormData(formData)
                    .setClientSessionCode(clientCode.getCode())
                    .createRegistration();
        }

        // Validate that user with this username doesn't exist in realm or any federation provider
        if (session.users().getUserByUsername(username, realm) != null) {
            event.error(Errors.USERNAME_IN_USE);
            return Flows.forms(session, realm, client, uriInfo, headers)
                    .setError(Messages.USERNAME_EXISTS)
                    .setFormData(formData)
                    .setClientSessionCode(clientCode.getCode())
                    .createRegistration();
        }

        // Validate that user with this email doesn't exist in realm or any federation provider
        if (session.users().getUserByEmail(email, realm) != null) {
            event.error(Errors.EMAIL_IN_USE);
            return Flows.forms(session, realm, client, uriInfo, headers)
                    .setError(Messages.EMAIL_EXISTS)
                    .setFormData(formData)
                    .setClientSessionCode(clientCode.getCode())
                    .createRegistration();
        }

        UserModel user = session.users().addUser(realm, username);
        user.setEnabled(true);
        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        user.setEmail(email);

        if (requiredCredentialTypes.contains(CredentialRepresentation.PASSWORD)) {
            UserCredentialModel credentials = new UserCredentialModel();
            credentials.setType(CredentialRepresentation.PASSWORD);
            credentials.setValue(formData.getFirst("password"));

            boolean passwordUpdateSuccessful;
            String passwordUpdateError = null;
            Object[] passwordUpdateErrorParameters = null;
            try {
                session.users().updateCredential(realm, user, UserCredentialModel.password(formData.getFirst("password")));
                passwordUpdateSuccessful = true;
            } catch (ModelException me) {
                passwordUpdateSuccessful = false;
                passwordUpdateError = me.getMessage();
                passwordUpdateErrorParameters = me.getParameters();
            } catch (Exception ape) {
                passwordUpdateSuccessful = false;
                passwordUpdateError = ape.getMessage();
            }

            // User already registered, but force him to update password
            if (!passwordUpdateSuccessful) {
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                return Flows.forms(session, realm, client, uriInfo, headers)
                        .setError(passwordUpdateError, passwordUpdateErrorParameters)
                        .setClientSessionCode(clientCode.getCode())
                        .createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            }
        }

        AttributeFormDataProcessor.process(formData, realm, user);

        event.user(user).success();
        event.reset();

        return processLogin(code, formData);
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
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.HTTPS_REQUIRED);
        }

        String code = formData.getFirst("code");

        ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
        if (accessCode == null || !accessCode.isValid(ClientSessionModel.Action.OAUTH_GRANT)) {
            event.error(Errors.INVALID_CODE);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.INVALID_ACCESS_CODE);
        }
        ClientSessionModel clientSession = accessCode.getClientSession();
        event.detail(Details.CODE_ID, clientSession.getId());

        String redirect = clientSession.getRedirectUri();

        event.client(clientSession.getClient())
                .user(clientSession.getUserSession().getUser())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.REDIRECT_URI, redirect);

        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession != null) {
            event.detail(Details.AUTH_METHOD, userSession.getAuthMethod());
            event.detail(Details.USERNAME, userSession.getLoginUsername());
            if (userSession.isRememberMe()) {
                event.detail(Details.REMEMBER_ME, "true");
            }
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            AuthenticationManager.logout(session, realm, userSession, uriInfo, clientConnection, headers);
            event.error(Errors.INVALID_CODE);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.SESSION_NOT_ACTIVE);
        }
        event.session(userSession);

        LoginProtocol protocol = session.getProvider(LoginProtocol.class, clientSession.getAuthMethod());
        protocol.setRealm(realm)
                .setHttpHeaders(headers)
                .setUriInfo(uriInfo);
        if (formData.containsKey("cancel")) {
            event.error(Errors.REJECTED_BY_USER);
            return protocol.consentDenied(clientSession);
        }

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
        if (!checks.check(code, ClientSessionModel.Action.UPDATE_PROFILE)) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();

        initEvent(clientSession);

        String error = Validation.validateUpdateProfileForm(formData);
        if (error != null) {
            return Flows.forms(session, realm, null, uriInfo, headers).setUser(user).setError(error)
                    .setClientSessionCode(accessCode.getCode())
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
                return Flows.forms(session, realm, null, uriInfo, headers).setUser(user).setError(Messages.EMAIL_EXISTS)
                        .setClientSessionCode(accessCode.getCode())
                        .createResponse(RequiredAction.UPDATE_PROFILE);
            }

            user.setEmail(email);
            user.setEmailVerified(false);
        }

        user.removeRequiredAction(RequiredAction.UPDATE_PROFILE);
        event.clone().event(EventType.UPDATE_PROFILE).success();

        if (emailChanged) {
            event.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, email).success();
        }

        return redirectOauth(user, accessCode, clientSession, userSession);
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateTotp(@QueryParam("code") String code,
                               final MultivaluedMap<String, String> formData) {
        event.event(EventType.UPDATE_TOTP);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.CONFIGURE_TOTP)) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();

        initEvent(clientSession);

        String totp = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");

        LoginFormsProvider loginForms = Flows.forms(session, realm, null, uriInfo, headers).setUser(user);
        if (Validation.isEmpty(totp)) {
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

        return redirectOauth(user, accessCode, clientSession, userSession);
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updatePassword(@QueryParam("code") String code,
                                   final MultivaluedMap<String, String> formData) {
        event.event(EventType.UPDATE_PASSWORD);
        Checks checks = new Checks();
        if (!checks.check(code, ClientSessionModel.Action.UPDATE_PASSWORD, ClientSessionModel.Action.RECOVER_PASSWORD)) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();

        initEvent(clientSession);

        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        LoginFormsProvider loginForms = Flows.forms(session, realm, null, uriInfo, headers).setUser(user);
        if (Validation.isEmpty(passwordNew)) {
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

        if (clientSession.getAction().equals(ClientSessionModel.Action.RECOVER_PASSWORD)) {
            String actionCookieValue = getActionCookie();
            if (actionCookieValue == null || !actionCookieValue.equals(userSession.getId())) {
                return Flows.forms(session, realm, clientSession.getClient(), uriInfo, headers).setSuccess(Messages.ACCOUNT_PASSWORD_UPDATED).createInfoPage();
            }
        }

        event = event.clone().event(EventType.LOGIN);

        return redirectOauth(user, accessCode, clientSession, userSession);
    }


    @Path("email-verification")
    @GET
    public Response emailVerification(@QueryParam("code") String code, @QueryParam("key") String key) {
        event.event(EventType.VERIFY_EMAIL);
        if (key != null) {
            Checks checks = new Checks();
            if (!checks.check(key, ClientSessionModel.Action.VERIFY_EMAIL)) {
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
                return Flows.forms(session, realm, clientSession.getClient(), uriInfo, headers).setSuccess(Messages.EMAIL_VERIFIED).createInfoPage();
            }

            event = event.clone().removeDetail(Details.EMAIL).event(EventType.LOGIN);

            return redirectOauth(user, accessCode, clientSession, userSession);
        } else {
            Checks checks = new Checks();
            if (!checks.check(code, ClientSessionModel.Action.VERIFY_EMAIL)) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            UserSessionModel userSession = clientSession.getUserSession();
            initEvent(clientSession);

            createActionCookie(realm, uriInfo, clientConnection, userSession.getId());

            return Flows.forms(session, realm, null, uriInfo, headers)
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
            if (!checks.check(key, ClientSessionModel.Action.RECOVER_PASSWORD)) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            return Flows.forms(session, realm, null, uriInfo, headers)
                    .setClientSessionCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        } else {
            return Flows.forms(session, realm, null, uriInfo, headers)
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
        if (!checkSsl()) {
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.HTTPS_REQUIRED);
        }
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REALM_NOT_ENABLED);
        }
        ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
        if (accessCode == null) {
            event.error(Errors.INVALID_CODE);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.UNKNOWN_CODE);
        }
        ClientSessionModel clientSession = accessCode.getClientSession();

        String username = formData.getFirst("username");

        ClientModel client = clientSession.getClient();
        if (client == null) {
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.UNKNOWN_LOGIN_REQUESTER);
        }
        if (!client.isEnabled()) {
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.LOGIN_REQUESTER_NOT_ENABLED);
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

            UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "form", false);
            event.session(userSession);
            TokenManager.attachClientSession(userSession, clientSession);

            accessCode.setAction(ClientSessionModel.Action.RECOVER_PASSWORD);

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
                return Flows.forms(this.session, realm, client, uriInfo, headers).setError(Messages.EMAIL_SENT_ERROR)
                        .setClientSessionCode(accessCode.getCode())
                        .createErrorPage();
            }

            createActionCookie(realm, uriInfo, clientConnection, userSession.getId());
        }

        return Flows.forms(session, realm, client,  uriInfo, headers).setSuccess(Messages.EMAIL_SENT).setClientSessionCode(accessCode.getCode()).createPasswordReset();
    }

    private String getActionCookie() {
        Cookie cookie = headers.getCookies().get(ACTION_COOKIE);
        AuthenticationManager.expireCookie(realm, ACTION_COOKIE, AuthenticationManager.getRealmCookiePath(realm, uriInfo), realm.getSslRequired().isRequired(clientConnection), clientConnection);
        return cookie != null ? cookie.getValue() : null;
    }

    public static void createActionCookie(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, String sessionId) {
        CookieHelper.addCookie(ACTION_COOKIE, sessionId, AuthenticationManager.getRealmCookiePath(realm, uriInfo), null, null, -1, realm.getSslRequired().isRequired(clientConnection), true);
    }

    private Response redirectOauth(UserModel user, ClientSessionCode accessCode, ClientSessionModel clientSession, UserSessionModel userSession) {
        return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
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
}
