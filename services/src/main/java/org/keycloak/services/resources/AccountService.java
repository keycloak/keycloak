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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.jboss.resteasy.jose.jws.JWSInput;
import org.jboss.resteasy.jose.jws.crypto.RSAProvider;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.email.EmailSender;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.FormFlows;
import org.keycloak.services.validation.Validation;
import org.picketlink.idm.credential.util.TimeBasedOTP;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountService {

    private RealmModel realm;

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    protected Providers providers;

    protected AuthenticationManager authManager = new AuthenticationManager();

    private TokenManager tokenManager;

    public AccountService(RealmModel realm, TokenManager tokenManager) {
        this.realm = realm;
        this.tokenManager = tokenManager;
    }

    @Path("access")
    @GET
    public Response accessPage() {
        UserModel user = getUserFromAuthManager();
        if (user != null) {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToAccess();
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @Path("")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processAccountUpdate(final MultivaluedMap<String, String> formData) {
        AccessCodeEntry accessCodeEntry = getAccessCodeEntry(RequiredAction.UPDATE_PROFILE);
        UserModel user = accessCodeEntry != null ? getUserFromAccessCode(accessCodeEntry) : getUserFromAuthManager();
        if (user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));
        user.setEmail(formData.getFirst("email"));

        user.removeRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        if (accessCodeEntry != null) {
            accessCodeEntry.getRequiredActions().remove(UserModel.RequiredAction.UPDATE_PROFILE);
        }

        if (accessCodeEntry != null) {
            return redirectOauth(user, accessCodeEntry);
        } else {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToAccount();
        }
    }

    private UserModel getUserFromAccessCode(AccessCodeEntry accessCodeEntry) {
        String loginName = accessCodeEntry.getUser().getLoginName();
        return realm.getUser(loginName);
    }

    private UserModel getUserFromAuthManager() {
        return authManager.authenticateIdentityCookie(realm, uriInfo, headers);
    }

    private AccessCodeEntry getAccessCodeEntry(RequiredAction requiredAction) {
        String code = uriInfo.getQueryParameters().getFirst(FormFlows.CODE);
        if (code == null) {
            return null;
        }

        JWSInput input = new JWSInput(code, providers);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
        } catch (Exception ignored) {
            return null;
        }

        if (!verifiedCode) {
            return null;
        }

        String key = input.readContent(String.class);
        AccessCodeEntry accessCodeEntry = tokenManager.getAccessCode(key);
        if (accessCodeEntry == null) {
            return null;
        }

        if (accessCodeEntry.isExpired()) {
            return null;
        }

        if (accessCodeEntry.getRequiredActions() == null || !accessCodeEntry.getRequiredActions().contains(requiredAction)) {
            return null;
        }

        return accessCodeEntry;
    }

    @Path("totp-remove")
    @GET
    public Response processTotpRemove() {
        UserModel user = getUserFromAuthManager();
        user.setTotp(false);
        return Flows.forms(realm, request, uriInfo).setUser(user).forwardToTotp();
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processTotpUpdate(final MultivaluedMap<String, String> formData) {
        AccessCodeEntry accessCodeEntry = getAccessCodeEntry(RequiredAction.CONFIGURE_TOTP);
        UserModel user = accessCodeEntry != null ? getUserFromAccessCode(accessCodeEntry) : getUserFromAuthManager();
        if (user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        FormFlows forms = Flows.forms(realm, request, uriInfo);

        String totp = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");

        String error = null;

        if (Validation.isEmpty(totp)) {
            error = Messages.MISSING_TOTP;
        } else if (!new TimeBasedOTP().validate(totp, totpSecret.getBytes())) {
            error = Messages.INVALID_TOTP;
        }

        if (error != null) {
            return forms.setError(error).setUser(user).forwardToTotp();
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.TOTP);
        credentials.setValue(formData.getFirst("totpSecret"));
        realm.updateCredential(user, credentials);

        user.removeRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        if (accessCodeEntry != null) {
            accessCodeEntry.getRequiredActions().remove(UserModel.RequiredAction.CONFIGURE_TOTP);
        }

        user.setTotp(true);

        if (accessCodeEntry != null) {
            return redirectOauth(user, accessCodeEntry);
        } else {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToTotp();
        }
    }

    @Path("password-reset")
    @GET
    public Response passwordReset() {
        return Flows.forms(realm, request, uriInfo).forwardToPasswordReset();
    }

    @Path("password-reset")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sendPasswordReset(final MultivaluedMap<String, String> formData) {
        String username = formData.getFirst("username");
        String email = formData.getFirst("email");

        String scopeParam = uriInfo.getQueryParameters().getFirst("scope");
        String state = uriInfo.getQueryParameters().getFirst("state");
        String redirect = uriInfo.getQueryParameters().getFirst("redirect_uri");
        String clientId = uriInfo.getQueryParameters().getFirst("client_id");

        UserModel client = realm.getUser(clientId);
        if (client == null) {
            return Flows.oauth(realm, request, uriInfo, authManager, tokenManager).forwardToSecurityFailure(
                    "Unknown login requester.");
        }
        if (!client.isEnabled()) {
            return Flows.oauth(realm, request, uriInfo, authManager, tokenManager).forwardToSecurityFailure(
                    "Login requester not enabled.");
        }

        UserModel user = realm.getUser(username);
        if (user == null || !email.equals(user.getEmail())) {
            return Flows.forms(realm, request, uriInfo).setError("emailError").forwardToPasswordReset();
        }

        Set<RequiredAction> requiredActions = new HashSet<RequiredAction>(user.getRequiredActions());
        requiredActions.add(RequiredAction.UPDATE_PASSWORD);

        AccessCodeEntry accessCode = tokenManager.createAccessCode(scopeParam, state, redirect, realm, client, user);
        accessCode.setRequiredActions(requiredActions);
        accessCode.setExpiration(System.currentTimeMillis() / 1000 + realm.getAccessCodeLifespanUserAction());

        new EmailSender().sendPasswordReset(user, realm, accessCode, uriInfo);

        return Flows.forms(realm, request, uriInfo).setError("emailSent").setErrorType(FormFlows.ErrorType.SUCCESS)
                .forwardToPasswordReset();
    }

    @Path("email-verification")
    @GET
    public Response emailVerification() {
        if (uriInfo.getQueryParameters().containsKey("key")) {
            AccessCodeEntry accessCode = tokenManager.getAccessCode(uriInfo.getQueryParameters().getFirst("key"));
            if (accessCode == null || accessCode.isExpired()
                    || !accessCode.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL)) {
                return Response.status(Status.FORBIDDEN).build();
            }

            String loginName = accessCode.getUser().getLoginName();
            UserModel user = realm.getUser(loginName);
            user.setEmailVerified(true);
            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);

            accessCode.getRequiredActions().remove(RequiredAction.VERIFY_EMAIL);

            return redirectOauth(user, accessCode);
        } else {
            AccessCodeEntry accessCode = getAccessCodeEntry(RequiredAction.VERIFY_EMAIL);
            UserModel user = accessCode != null ? getUserFromAccessCode(accessCode) : null;
            if (user == null) {
                return Response.status(Status.FORBIDDEN).build();
            }

            return Flows.forms(realm, request, uriInfo).setAccessCode(accessCode).setUser(user)
                    .forwardToAction(RequiredAction.VERIFY_EMAIL);
        }
    }

    private Response redirectOauth(UserModel user, AccessCodeEntry accessCode) {
        if (accessCode == null) {
            return null;
        }

        Set<RequiredAction> requiredActions = user.getRequiredActions();
        if (!requiredActions.isEmpty()) {
            return Flows.forms(realm, request, uriInfo).setAccessCode(accessCode).setUser(user)
                    .forwardToAction(requiredActions.iterator().next());
        } else {
            accessCode.setExpiration((System.currentTimeMillis() / 1000) + realm.getAccessCodeLifespan());
            return Flows.oauth(realm, request, uriInfo, authManager, tokenManager).redirectAccessCode(accessCode,
                    accessCode.getState(), accessCode.getRedirectUri());
        }
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processPasswordUpdate(final MultivaluedMap<String, String> formData) {
        AccessCodeEntry accessCode = getAccessCodeEntry(RequiredAction.UPDATE_PASSWORD);
        UserModel user = accessCode != null ? getUserFromAccessCode(accessCode) : getUserFromAuthManager();
        if (user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        boolean loginAction = accessCode != null;

        FormFlows forms = Flows.forms(realm, request, uriInfo).setUser(user);

        String password = formData.getFirst("password");
        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        String error = null;

        if (Validation.isEmpty(passwordNew)) {
            error = Messages.MISSING_PASSWORD;
        } else if (!passwordNew.equals(passwordConfirm)) {
            error = Messages.INVALID_PASSWORD_CONFIRM;
        }

        if (!loginAction) {
            if (Validation.isEmpty(password)) {
                error = Messages.MISSING_PASSWORD;
            } else if (!realm.validatePassword(user, password)) {
                error = Messages.INVALID_PASSWORD_EXISTING;
            }
        }

        if (error != null) {
            return forms.setError(error).forwardToPassword();
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(passwordNew);

        realm.updateCredential(user, credentials);

        user.removeRequiredAction(RequiredAction.UPDATE_PASSWORD);
        if (accessCode != null) {
            accessCode.getRequiredActions().remove(UserModel.RequiredAction.UPDATE_PASSWORD);
        }

        if (accessCode != null) {
            return redirectOauth(user, accessCode);
        } else {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToPassword();
        }
    }

    @Path("")
    @GET
    public Response accountPage() {
        UserModel user = getUserFromAuthManager();
        if (user != null) {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToAccount();
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @Path("social")
    @GET
    public Response socialPage() {
        UserModel user = getUserFromAuthManager();
        if (user != null) {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToSocial();
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @Path("totp")
    @GET
    public Response totpPage() {
        UserModel user = getUserFromAuthManager();
        if (user != null) {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToTotp();
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @Path("password")
    @GET
    public Response passwordPage() {
        if (uriInfo.getQueryParameters().containsKey("key")) {
            AccessCodeEntry accessCode = tokenManager.getAccessCode(uriInfo.getQueryParameters().getFirst("key"));
            if (accessCode == null || accessCode.isExpired()
                    || !accessCode.getRequiredActions().contains(RequiredAction.UPDATE_PASSWORD)) {
                return Response.status(Status.FORBIDDEN).build();
            }

            return Flows.forms(realm, request, uriInfo).setAccessCode(accessCode)
                    .forwardToAction(RequiredAction.UPDATE_PASSWORD);
        } else {
            UserModel user = getUserFromAuthManager();
            if (user == null) {
                return Response.status(Status.FORBIDDEN).build();
            }
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToPassword();
        }
    }
}
