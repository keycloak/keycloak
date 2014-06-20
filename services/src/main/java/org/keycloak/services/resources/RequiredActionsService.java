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
import org.keycloak.OAuth2Constants;
import org.keycloak.audit.Audit;
import org.keycloak.audit.Details;
import org.keycloak.audit.Errors;
import org.keycloak.audit.EventType;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ClientConnection;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.services.validation.Validation;
import org.keycloak.authentication.AuthenticationProviderException;
import org.keycloak.authentication.AuthenticationProviderManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.HashSet;
import java.util.Set;
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
    protected ProviderSession providerSession;

    private TokenManager tokenManager;

    private Audit audit;

    public RequiredActionsService(RealmModel realm, TokenManager tokenManager, Audit audit) {
        this.realm = realm;
        this.tokenManager = tokenManager;
        this.audit = audit;
    }

    @Path("profile")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateProfile(final MultivaluedMap<String, String> formData) {
        AccessCodeEntry accessCode = getAccessCodeEntry(RequiredAction.UPDATE_PROFILE);
        if (accessCode == null) {
            return unauthorized();
        }

        UserModel user = getUser(accessCode);

        initAudit(accessCode);

        String error = Validation.validateUpdateProfileForm(formData);
        if (error != null) {
            return Flows.forms(providerSession, realm, uriInfo).setUser(user).setError(error).createResponse(RequiredAction.UPDATE_PROFILE);
        }

        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        String email = formData.getFirst("email");
        String oldEmail = user.getEmail();
        boolean emailChanged = oldEmail != null ? !oldEmail.equals(email) : email != null;

        user.setEmail(email);

        user.removeRequiredAction(RequiredAction.UPDATE_PROFILE);
        accessCode.removeRequiredAction(RequiredAction.UPDATE_PROFILE);

        audit.clone().event(EventType.UPDATE_PROFILE).success();
        if (emailChanged) {
            user.setEmailVerified(false);
            audit.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, email).success();
        }

        return redirectOauth(user, accessCode);
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateTotp(final MultivaluedMap<String, String> formData) {
        AccessCodeEntry accessCode = getAccessCodeEntry(RequiredAction.CONFIGURE_TOTP);
        if (accessCode == null) {
            return unauthorized();
        }

        UserModel user = getUser(accessCode);

        initAudit(accessCode);

        String totp = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");

        LoginFormsProvider loginForms = Flows.forms(providerSession, realm, uriInfo).setUser(user);
        if (Validation.isEmpty(totp)) {
            return loginForms.setError(Messages.MISSING_TOTP).createResponse(RequiredAction.CONFIGURE_TOTP);
        } else if (!new TimeBasedOTP().validate(totp, totpSecret.getBytes())) {
            return loginForms.setError(Messages.INVALID_TOTP).createResponse(RequiredAction.CONFIGURE_TOTP);
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.TOTP);
        credentials.setValue(totpSecret);
        user.updateCredential(credentials);

        user.setTotp(true);

        user.removeRequiredAction(RequiredAction.CONFIGURE_TOTP);
        accessCode.removeRequiredAction(RequiredAction.CONFIGURE_TOTP);

        audit.clone().event(EventType.UPDATE_TOTP).success();

        return redirectOauth(user, accessCode);
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updatePassword(final MultivaluedMap<String, String> formData) {
        logger.debug("updatePassword");
        AccessCodeEntry accessCode = getAccessCodeEntry(RequiredAction.UPDATE_PASSWORD);
        if (accessCode == null) {
            logger.debug("updatePassword access code is null");
            return unauthorized();
        }
        logger.debug("updatePassword has access code");

        UserModel user = getUser(accessCode);

        initAudit(accessCode);

        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        LoginFormsProvider loginForms = Flows.forms(providerSession, realm, uriInfo).setUser(user);
        if (Validation.isEmpty(passwordNew)) {
            return loginForms.setError(Messages.MISSING_PASSWORD).createResponse(RequiredAction.UPDATE_PASSWORD);
        } else if (!passwordNew.equals(passwordConfirm)) {
            return loginForms.setError(Messages.NOTMATCH_PASSWORD).createResponse(RequiredAction.UPDATE_PASSWORD);
        }

        try {
            boolean updateSuccessful = AuthenticationProviderManager.getManager(realm, providerSession).updatePassword(user, passwordNew);
            if (!updateSuccessful) {
                return loginForms.setError("Password update failed").createResponse(RequiredAction.UPDATE_PASSWORD);
            }
        } catch (AuthenticationProviderException ape) {
            return loginForms.setError(ape.getMessage()).createResponse(RequiredAction.UPDATE_PASSWORD);
        }

        logger.debug("updatePassword updated credential");

        user.removeRequiredAction(RequiredAction.UPDATE_PASSWORD);
        if (accessCode != null) {
            accessCode.removeRequiredAction(RequiredAction.UPDATE_PASSWORD);
        }

        audit.clone().event(EventType.UPDATE_PASSWORD).success();

        return redirectOauth(user, accessCode);
    }


    @Path("email-verification")
    @GET
    public Response emailVerification() {
        if (uriInfo.getQueryParameters().containsKey("key")) {
            AccessCodeEntry accessCode = tokenManager.parseCode(uriInfo.getQueryParameters().getFirst("key"), realm);
            if (accessCode == null || accessCode.isExpired()
                    || !accessCode.hasRequiredAction(RequiredAction.VERIFY_EMAIL)) {
                return unauthorized();
            }

            UserModel user = getUser(accessCode);

            initAudit(accessCode);

            user.setEmailVerified(true);

            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);
            accessCode.removeRequiredAction(RequiredAction.VERIFY_EMAIL);

            audit.clone().event(EventType.VERIFY_EMAIL).detail(Details.EMAIL, accessCode.getUser().getEmail()).success();

            return redirectOauth(user, accessCode);
        } else {
            AccessCodeEntry accessCode = getAccessCodeEntry(RequiredAction.VERIFY_EMAIL);
            if (accessCode == null) {
                return unauthorized();
            }

            initAudit(accessCode);
            //audit.clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, accessCode.getUser().getEmail()).success();

            return Flows.forms(providerSession, realm, uriInfo).setAccessCode(accessCode.getCode()).setUser(accessCode.getUser())
                    .createResponse(RequiredAction.VERIFY_EMAIL);
        }
    }

    @Path("password-reset")
    @GET
    public Response passwordReset() {
        if (uriInfo.getQueryParameters().containsKey("key")) {
            AccessCodeEntry accessCode = tokenManager.parseCode(uriInfo.getQueryParameters().getFirst("key"), realm);
            accessCode.setAuthMethod("form");
            if (accessCode == null || accessCode.isExpired()
                    || !accessCode.hasRequiredAction(RequiredAction.UPDATE_PASSWORD)) {
                return unauthorized();
            }

            return Flows.forms(providerSession, realm, uriInfo).setAccessCode(accessCode.getCode()).createResponse(RequiredAction.UPDATE_PASSWORD);
        } else {
            return Flows.forms(providerSession, realm, uriInfo).createPasswordReset();
        }
    }

    @Path("password-reset")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sendPasswordReset(final MultivaluedMap<String, String> formData) {
        String username = formData.getFirst("username");

        String scopeParam = uriInfo.getQueryParameters().getFirst(OAuth2Constants.SCOPE);
        String state = uriInfo.getQueryParameters().getFirst(OAuth2Constants.STATE);
        String redirect = uriInfo.getQueryParameters().getFirst(OAuth2Constants.REDIRECT_URI);
        String clientId = uriInfo.getQueryParameters().getFirst(OAuth2Constants.CLIENT_ID);

        AuthenticationManager authManager = new AuthenticationManager(providerSession);

        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            return Flows.oauth(providerSession, realm, request, uriInfo, authManager, tokenManager).forwardToSecurityFailure(
                    "Unknown login requester.");
        }
        if (!client.isEnabled()) {
            return Flows.oauth(providerSession, realm, request, uriInfo, authManager, tokenManager).forwardToSecurityFailure(
                    "Login requester not enabled.");
        }

        audit.event(EventType.SEND_RESET_PASSWORD).client(clientId)
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.USERNAME, username);

        UserModel user = realm.getUser(username);
        if (user == null && username.contains("@")) {
            user = realm.getUserByEmail(username);
        }

        if (user == null) {
            logger.warn("Failed to send password reset email: user not found");
            audit.error(Errors.USER_NOT_FOUND);
        } else {
            Set<RequiredAction> requiredActions = new HashSet<RequiredAction>(user.getRequiredActions());
            requiredActions.add(RequiredAction.UPDATE_PASSWORD);

            UserSessionModel session = realm.createUserSession(user, clientConnection.getRemoteAddr());
            audit.session(session);

            AccessCodeEntry accessCode = tokenManager.createAccessCode(scopeParam, state, redirect, realm, client, user, session);
            accessCode.setRequiredActions(requiredActions);
            accessCode.setAuthMethod("form");
            accessCode.setUsernameUsed(username);

            try {
                UriBuilder builder = Urls.loginPasswordResetBuilder(uriInfo.getBaseUri());
                builder.queryParam("key", accessCode.getCode());

                String link = builder.build(realm.getName()).toString();
                long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

                providerSession.getProvider(EmailProvider.class).setRealm(realm).setUser(user).sendPasswordReset(link, expiration);

                audit.user(user).detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, accessCode.getCodeId()).success();
            } catch (EmailException e) {
                logger.error("Failed to send password reset email", e);
                return Flows.forms(providerSession, realm, uriInfo).setError("emailSendError").createErrorPage();
            }
        }

        return Flows.forms(providerSession, realm, uriInfo).setSuccess("emailSent").createPasswordReset();
    }

    private AccessCodeEntry getAccessCodeEntry(RequiredAction requiredAction) {
        String code = uriInfo.getQueryParameters().getFirst(OAuth2Constants.CODE);
        if (code == null) {
            logger.debug("getAccessCodeEntry code as not in query param");
            return null;
        }

        AccessCodeEntry accessCodeEntry = tokenManager.parseCode(code, realm);
        if (accessCodeEntry == null) {
            logger.debug("getAccessCodeEntry access code entry null");
            return null;
        }

        if (accessCodeEntry.isExpired()) {
            logger.debugv("getAccessCodeEntry: access code id: {0}", accessCodeEntry.getCodeId());
            logger.debugv("getAccessCodeEntry access code entry expired");
            return null;
        }

        if (accessCodeEntry.getRequiredActions() == null || !accessCodeEntry.getRequiredActions().contains(requiredAction)) {
            logger.debugv("getAccessCodeEntry required actions null || entry does not contain required action: {0}|{1}", (accessCodeEntry.getRequiredActions() == null),!accessCodeEntry.getRequiredActions().contains(requiredAction) );
            return null;
        }

        return accessCodeEntry;
    }

    private UserModel getUser(AccessCodeEntry accessCode) {
        return realm.getUser(accessCode.getUser().getLoginName());
    }

    private Response redirectOauth(UserModel user, AccessCodeEntry accessCode) {
        if (accessCode == null) {
            return null;
        }

        Set<RequiredAction> requiredActions = user.getRequiredActions();
        if (!requiredActions.isEmpty()) {
            return Flows.forms(providerSession, realm, uriInfo).setAccessCode(accessCode.getCode()).setUser(user)
                    .createResponse(requiredActions.iterator().next());
        } else {
            logger.debugv("redirectOauth: redirecting to: {0}", accessCode.getRedirectUri());
            accessCode.resetExpiration();

            AuthenticationManager authManager = new AuthenticationManager(providerSession);

            UserSessionModel session = realm.getUserSession(accessCode.getSessionState());
            if (!AuthenticationManager.isSessionValid(realm, session)) {
                AuthenticationManager.logout(realm, session, uriInfo);
                return Flows.oauth(providerSession, realm, request, uriInfo, authManager, tokenManager).redirectError(accessCode.getClient(), "access_denied", accessCode.getState(), accessCode.getRedirectUri());
            }
            audit.session(session);

            audit.success();

            return Flows.oauth(providerSession, realm, request, uriInfo, authManager, tokenManager).redirectAccessCode(accessCode,
                    session, accessCode.getState(), accessCode.getRedirectUri());
        }
    }

    private void initAudit(AccessCodeEntry accessCode) {
        audit.event(EventType.LOGIN).client(accessCode.getClient())
                .user(accessCode.getUser())
                .session(accessCode.getSessionState())
                .detail(Details.CODE_ID, accessCode.getCodeId())
                .detail(Details.REDIRECT_URI, accessCode.getRedirectUri())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, accessCode.getAuthMethod())
                .detail(Details.USERNAME, accessCode.getUsernameUsed());

        if (accessCode.isRememberMe()) {
            audit.detail(Details.REMEMBER_ME, "true");
        }
    }

    private Response unauthorized() {
        return Flows.forms(providerSession, realm, uriInfo).setError("Unauthorized request").createErrorPage();
    }

}
