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
        UserModel user = getUserFromAuthManager();
        if (user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));
        user.setEmail(formData.getFirst("email"));

        return Flows.forms(realm, request, uriInfo).setUser(user).forwardToAccount();
    }

    @Path("totp-remove")
    @GET
    public Response processTotpRemove() {
        UserModel user = getUserFromAuthManager();
        user.setTotp(false);
        return Flows.forms(realm, request, uriInfo).setError("successTotpRemoved").setErrorType(FormFlows.MessageType.SUCCESS)
                .setUser(user).forwardToTotp();
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processTotpUpdate(final MultivaluedMap<String, String> formData) {
        UserModel user = getUserFromAuthManager();
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

        user.setTotp(true);

        return Flows.forms(realm, request, uriInfo).setError("successTotp").setErrorType(FormFlows.MessageType.SUCCESS)
                .setUser(user).forwardToTotp();
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processPasswordUpdate(final MultivaluedMap<String, String> formData) {
        UserModel user = getUserFromAuthManager();
        if (user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        FormFlows forms = Flows.forms(realm, request, uriInfo).setUser(user);

        String password = formData.getFirst("password");
        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        if (Validation.isEmpty(passwordNew)) {
            forms.setError(Messages.MISSING_PASSWORD).forwardToPassword();
        } else if (!passwordNew.equals(passwordConfirm)) {
            forms.setError(Messages.INVALID_PASSWORD_CONFIRM).forwardToPassword();
        }

        if (Validation.isEmpty(password)) {
            forms.setError(Messages.MISSING_PASSWORD).forwardToPassword();
        } else if (!realm.validatePassword(user, password)) {
            forms.setError(Messages.INVALID_PASSWORD_EXISTING).forwardToPassword();
        }


        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(passwordNew);

        realm.updateCredential(user, credentials);

        return Flows.forms(realm, request, uriInfo).setUser(user).forwardToPassword();
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
        UserModel user = getUserFromAuthManager();
        if (user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }
        return Flows.forms(realm, request, uriInfo).setUser(user).forwardToPassword();
    }

    private UserModel getUserFromAuthManager() {
        return authManager.authenticateIdentityCookie(realm, uriInfo, headers);
    }
}
