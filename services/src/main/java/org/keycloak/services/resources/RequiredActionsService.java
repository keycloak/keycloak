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
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AccessCode;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.protocol.OpenIdConnectProtocol;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionsService {

    protected static final Logger logger = Logger.getLogger(RequiredActionsService.class);

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

    private TokenManager tokenManager;

    private EventBuilder event;

    public RequiredActionsService(RealmModel realm, TokenManager tokenManager, EventBuilder event) {
        this.realm = realm;
        this.tokenManager = tokenManager;
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
            if (!checkSsl()) {
                event.error(Errors.SSL_REQUIRED);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "HTTPS required");
                return false;
            }
            if (!realm.isEnabled()) {
                event.error(Errors.REALM_DISABLED);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Realm not enabled.");
                return false;
            }
            clientCode = ClientSessionCode.parse(code, session, realm);
            if (clientCode == null) {
                event.error(Errors.INVALID_CODE);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Unknown code, please login again through your application.");
                return false;
            }
            if (!clientCode.isValid(requiredAction)) {
                event.error(Errors.INVALID_CODE);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid code, please login again through your application.");
            }
            return true;
        }
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
            return Flows.forms(session, realm, null, uriInfo).setUser(user).setError(error)
                    .setAccessCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PROFILE);
        }

        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        String email = formData.getFirst("email");
        String oldEmail = user.getEmail();
        boolean emailChanged = oldEmail != null ? !oldEmail.equals(email) : email != null;

        user.setEmail(email);

        user.removeRequiredAction(RequiredAction.UPDATE_PROFILE);

        event.clone().event(EventType.UPDATE_PROFILE).success();
        if (emailChanged) {
            user.setEmailVerified(false);
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

        LoginFormsProvider loginForms = Flows.forms(session, realm, null, uriInfo).setUser(user);
        if (Validation.isEmpty(totp)) {
            return loginForms.setError(Messages.MISSING_TOTP)
                    .setAccessCode(accessCode.getCode())
                    .createResponse(RequiredAction.CONFIGURE_TOTP);
        } else if (!new TimeBasedOTP().validate(totp, totpSecret.getBytes())) {
            return loginForms.setError(Messages.INVALID_TOTP)
                    .setAccessCode(accessCode.getCode())
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
        if (!checks.check(code, ClientSessionModel.Action.UPDATE_PASSWORD)) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        ClientSessionModel clientSession = accessCode.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();

        initEvent(clientSession);

        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        LoginFormsProvider loginForms = Flows.forms(session, realm, null, uriInfo).setUser(user);
        if (Validation.isEmpty(passwordNew)) {
            return loginForms.setError(Messages.MISSING_PASSWORD)
                    .setAccessCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        } else if (!passwordNew.equals(passwordConfirm)) {
            return loginForms.setError(Messages.NOTMATCH_PASSWORD)
                    .setAccessCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        }

        try {
            session.users().updateCredential(realm, user, UserCredentialModel.password(passwordNew));
        } catch (Exception ape) {
            return loginForms.setError(ape.getMessage())
                    .setAccessCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        }

        user.removeRequiredAction(RequiredAction.UPDATE_PASSWORD);

        event.clone().event(EventType.UPDATE_PASSWORD).success();
        return redirectOauth(user, accessCode, clientSession, userSession);

        // Redirect to account management to login if password reset was initiated by admin
        /* here while refactoring,  ok to remove when you want
        if (accessCode.getSessionState() == null) {
            return Response.seeOther(Urls.accountPage(uriInfo.getBaseUri(), realm.getId())).build();
        } else {
            return redirectOauth(user, accessCode);
        }
        */
    }


    @Path("email-verification")
    @GET
    public Response emailVerification(@QueryParam("code") String code) {
        event.event(EventType.VERIFY_EMAIL);
        if (uriInfo.getQueryParameters().containsKey("key")) {
            Checks checks = new Checks();
            if (!checks.check(code, ClientSessionModel.Action.VERIFY_EMAIL)) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            UserSessionModel userSession = clientSession.getUserSession();
            UserModel user = userSession.getUser();
            String key = uriInfo.getQueryParameters().getFirst("key");
            String keyNote = clientSession.getNote("key");
            if (key == null || !key.equals(keyNote)) {
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Somebody is trying to illegally change your email.");
            }
            initEvent(clientSession);
            user.setEmailVerified(true);

            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);

            event.clone().event(EventType.VERIFY_EMAIL).detail(Details.EMAIL, user.getEmail()).success();

            return redirectOauth(user, accessCode, clientSession, userSession);
        } else {
            Checks checks = new Checks();
            if (!checks.check(code, ClientSessionModel.Action.VERIFY_EMAIL)) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            String verifyCode = UUID.randomUUID().toString();
            clientSession.setNote("key", verifyCode);
            UserSessionModel userSession = clientSession.getUserSession();
            UserModel user = userSession.getUser();

            initEvent(clientSession);

            return Flows.forms(session, realm, null, uriInfo)
                    .setAccessCode(accessCode.getCode())
                    .setVerifyCode(verifyCode)
                    .setUser(userSession.getUser())
                    .createResponse(RequiredAction.VERIFY_EMAIL);
        }
    }

    @Path("password-reset")
    @GET
    public Response passwordReset(@QueryParam("code") String code) {
        event.event(EventType.SEND_RESET_PASSWORD);
        if (uriInfo.getQueryParameters().containsKey("key")) {
            Checks checks = new Checks();
            if (!checks.check(code, ClientSessionModel.Action.UPDATE_PASSWORD)) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = accessCode.getClientSession();
            UserSessionModel userSession = clientSession.getUserSession();
            UserModel user = userSession.getUser();
            String key = uriInfo.getQueryParameters().getFirst("key");
            String keyNote = clientSession.getNote("key");
            if (key == null || !key.equals(keyNote)) {
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Somebody is trying to illegally change your password.");
            }
            return Flows.forms(session, realm, null, uriInfo)
                    .setAccessCode(accessCode.getCode())
                    .createResponse(RequiredAction.UPDATE_PASSWORD);
        } else {
            return Flows.forms(session, realm, null, uriInfo)
                    .setAccessCode(code)
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
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "HTTPS required");
        }
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Realm not enabled.");
        }
        ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
        if (accessCode == null) {
            event.error(Errors.INVALID_CODE);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Unknown code, please login again through your application.");
        }
        ClientSessionModel clientSession = accessCode.getClientSession();

        String username = formData.getFirst("username");

        ClientModel client = clientSession.getClient();
        if (client == null) {
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo,
                    "Unknown login requester.");
        }
        if (!client.isEnabled()) {
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo,
                    "Login requester not enabled.");
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
        } else {
            UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "form", false);
            event.session(userSession);
            TokenManager.attachClientSession(userSession, clientSession);

            accessCode.setRequiredAction(RequiredAction.UPDATE_PASSWORD);

            try {
                UriBuilder builder = Urls.loginPasswordResetBuilder(uriInfo.getBaseUri());
                builder.queryParam("code", accessCode.getCode());
                String verifyCode = UUID.randomUUID().toString();
                clientSession.setNote("key", verifyCode);
                builder.queryParam("key", verifyCode);

                String link = builder.build(realm.getName()).toString();
                long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

                this.session.getProvider(EmailProvider.class).setRealm(realm).setUser(user).sendPasswordReset(link, expiration);

                event.user(user).detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, clientSession.getId()).success();
            } catch (EmailException e) {
                logger.error("Failed to send password reset email", e);
                return Flows.forms(this.session, realm, client, uriInfo).setError("emailSendError")
                        .setAccessCode(accessCode.getCode())
                        .createErrorPage();
            }
        }

        return Flows.forms(session, realm, client,  uriInfo).setSuccess("emailSent").createPasswordReset();
    }

    private Response redirectOauth(UserModel user, ClientSessionCode accessCode, ClientSessionModel clientSession, UserSessionModel userSession) {
        if (accessCode == null) {
            return null;
        }

        Set<RequiredAction> requiredActions = user.getRequiredActions();
        if (!requiredActions.isEmpty()) {
            accessCode.setRequiredAction(requiredActions.iterator().next());
            return Flows.forms(session, realm, null, uriInfo)
                    .setAccessCode(accessCode.getCode())
                    .setUser(user)
                    .createResponse(requiredActions.iterator().next());
        } else {
            logger.debugv("Redirecting to: {0}", clientSession.getRedirectUri());
            accessCode.setAction(ClientSessionModel.Action.CODE_TO_TOKEN);

            AuthenticationManager authManager = new AuthenticationManager();

            if (!AuthenticationManager.isSessionValid(realm, userSession)) {
                AuthenticationManager.logout(session, realm, userSession, uriInfo, clientConnection);
                return Flows.oauth(this.session, realm, request, uriInfo, clientConnection, authManager, tokenManager)
                        .redirectError(clientSession.getClient(), "access_denied", clientSession.getNote(OpenIdConnectProtocol.STATE_PARAM), clientSession.getRedirectUri());
            }
            event.session(userSession);

            event.success();

            return Flows.oauth(this.session, realm, request, uriInfo, clientConnection, authManager, tokenManager)
                    .redirectAccessCode(accessCode,
                            userSession, clientSession.getNote(OpenIdConnectProtocol.STATE_PARAM), clientSession.getRedirectUri());
        }
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
