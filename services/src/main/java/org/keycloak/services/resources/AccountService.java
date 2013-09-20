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
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserModel.RequiredAction;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.FormFlows;
import org.keycloak.services.resources.flows.OAuthFlows;
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
        UserModel user = getUser(RequiredAction.UPDATE_PROFILE);
        if (user != null) {
            user.setFirstName(formData.getFirst("firstName"));
            user.setLastName(formData.getFirst("lastName"));
            user.setEmail(formData.getFirst("email"));

            Response response = redirectOauth();
            if (response != null) {
                return response;
            } else {
                return Flows.forms(realm, request, uriInfo).setUser(user).forwardToAccount();
            }
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    private UserModel getUser(RequiredAction action) {
        if (uriInfo.getQueryParameters().containsKey(FormFlows.CODE)) {
            AccessCodeEntry accessCodeEntry = getAccessCodeEntry(uriInfo.getQueryParameters().getFirst(FormFlows.CODE));
            if (accessCodeEntry == null) {
                return null;
            }

            String loginName = accessCodeEntry.getUser().getLoginName();
            UserModel user = realm.getUser(loginName);
            if (!user.getRequiredActions().contains(action)) {
                return null;
            }
            if (!accessCodeEntry.getUser().getRequiredActions().contains(action)) {
                return null;
            }
            return user;
        } else {
            return getUserFromAuthManager();
        }
    }

    private UserModel getUserFromAuthManager() {
        return authManager.authenticateIdentityCookie(realm, uriInfo, headers);
    }

    private AccessCodeEntry getAccessCodeEntry(String code) {
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

        return accessCodeEntry;
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processTotpUpdate(final MultivaluedMap<String, String> formData) {
        UserModel user = getUser(RequiredAction.CONFIGURE_TOTP);
        if (user != null) {
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

            user.setTotp(true);

            Response response = redirectOauth();
            if (response != null) {
                return response;
            } else {
                return Flows.forms(realm, request, uriInfo).setUser(user).forwardToTotp();
            }
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @Path("email-verify")
    @GET
    public Response processEmailVerification(@QueryParam("code") String code) {
        AccessCodeEntry accessCodeEntry = getAccessCodeEntry(code);
        String loginName = accessCodeEntry.getUser().getLoginName();
        UserModel user = realm.getUser(loginName);
        if (user != null) {
            user.setEmailVerified(true);
            user.removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);

            Response response = redirectOauth();
            if (response != null) {
                return response;
            } else {
                return Flows.forms(realm, request, uriInfo).setUser(user).forwardToVerifyEmail();
            }
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    private Response redirectOauth() {
        String redirect = uriInfo.getQueryParameters().getFirst("redirect_uri");
        if (redirect != null) {
            AccessCodeEntry accessCode = getAccessCodeEntry(uriInfo.getQueryParameters().getFirst(FormFlows.CODE));
            String state = uriInfo.getQueryParameters().getFirst("state");
            return Flows.oauth(realm, request, uriInfo, authManager, tokenManager).redirectAccessCode(accessCode, state,
                    redirect);
        } else {
            return null;
        }
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processPasswordUpdate(final MultivaluedMap<String, String> formData) {
        UserModel user = getUser(RequiredAction.RESET_PASSWORD);
        if (user != null) {
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

            if (user.getRequiredActions() == null || !user.getRequiredActions().contains(RequiredAction.RESET_PASSWORD)) {
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

            user.removeRequiredAction(RequiredAction.RESET_PASSWORD);
            user.setStatus(UserModel.Status.ENABLED);

            authManager.expireIdentityCookie(realm, uriInfo);
            new ResourceAdminManager().singleLogOut(realm, user.getLoginName());
            
            return Flows.forms(realm, request, uriInfo).forwardToLogin();
        } else {
            return Response.status(Status.FORBIDDEN).build();
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
        UserModel user = getUser(RequiredAction.RESET_PASSWORD);
        if (user != null) {
            return Flows.forms(realm, request, uriInfo).setUser(user).forwardToPassword();
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @Path("password-reset")
    @GET
    public Response resetPassword(@QueryParam("username") final String username,
            @QueryParam("client_id") final String clientId, @QueryParam("scope") final String scopeParam,
            @QueryParam("state") final String state, @QueryParam("redirect_uri") final String redirect) {

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!realm.isEnabled()) {
            return oauth.forwardToSecurityFailure("Realm not enabled.");
        }
        if (!realm.isResetPasswordAllowed()) {
            return oauth.forwardToSecurityFailure("Password reset not permitted, contact admin.");
        }

        UserModel client = realm.getUser(clientId);
        if (client == null) {
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }
        if (!client.isEnabled()) {
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        // String username = formData.getFirst("username");
        UserModel user = realm.getUser(username);
        user.addRequiredAction(RequiredAction.RESET_PASSWORD);
        user.setStatus(UserModel.Status.ACTIONS_REQUIRED);

        AccessCodeEntry accessCode = tokenManager.createAccessCode(scopeParam, state, redirect, realm, client, user);
        accessCode.setExpiration(System.currentTimeMillis() / 1000 + realm.getAccessCodeLifespanUserAction());

        if (user.getEmail() == null) {
            return oauth.forwardToSecurityFailure("Email address not set, contact admin");
        }

        new EmailSender().sendPasswordReset(user, realm, accessCode.getCode(), uriInfo);
        // TODO Add info message
        return Flows.forms(realm, request, uriInfo).forwardToLogin();
    }

}
