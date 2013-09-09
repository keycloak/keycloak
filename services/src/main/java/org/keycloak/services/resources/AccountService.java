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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
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

    protected AuthenticationManager authManager = new AuthenticationManager();

    public AccountService(RealmModel realm) {
        this.realm = realm;
    }

    @Path("access")
    @GET
    public Response accessPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    return Flows.forms(realm, request).setUser(user).forwardToAccess();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

    @Path("")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processAccountUpdate(final MultivaluedMap<String, String> formData) {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    user.setFirstName(formData.getFirst("firstName"));
                    user.setLastName(formData.getFirst("lastName"));
                    user.setEmail(formData.getFirst("email"));

                    return Flows.forms(realm, request).setUser(user).forwardToAccount();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processTotpUpdate(final MultivaluedMap<String, String> formData) {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    FormFlows forms = Flows.forms(realm, request);

                    String totp = formData.getFirst("totp");
                    String totpSecret = formData.getFirst("totpSecret");

                    String error = null;

                    if (Validation.isEmpty(totp)) {
                        error = Messages.MISSING_TOTP;
                    } else if (!new TimeBasedOTP().validate(totp, totpSecret.getBytes())) {
                        error = Messages.INVALID_TOTP;
                    }
                    
                    if (error != null) {
                        return forms.setError(error).forwardToTotp();
                    }

                    UserCredentialModel credentials = new UserCredentialModel();
                    credentials.setType(CredentialRepresentation.TOTP);
                    credentials.setValue(formData.getFirst("totpSecret"));
                    realm.updateCredential(user, credentials);

                    if (!user.isEnabled() && "REQUIRED".equals(user.getAttribute("KEYCLOAK_TOTP"))) {
                        user.setEnabled(true);
                    }

                    user.setAttribute("KEYCLOAK_TOTP", "ENABLED");

                    return Flows.forms(realm, request).setUser(user).forwardToTotp();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processPasswordUpdate(final MultivaluedMap<String, String> formData) {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    FormFlows forms = Flows.forms(realm, request).setUser(user);

                    String password = formData.getFirst("password");
                    String passwordNew = formData.getFirst("password-new");
                    String passwordConfirm = formData.getFirst("password-confirm");

                    String error = null;

                    if (Validation.isEmpty(password)) {
                        error = Messages.MISSING_PASSWORD;
                    } else if (Validation.isEmpty(passwordNew)) {
                        error = Messages.MISSING_PASSWORD;
                    } else if (!passwordNew.equals(passwordConfirm)) {
                        error = Messages.INVALID_PASSWORD_CONFIRM;
                    } else if (!realm.validatePassword(user, password)) {
                        error = Messages.INVALID_PASSWORD_EXISTING;
                    }

                    if (error != null) {
                        return forms.setError(error).forwardToPassword();
                    }

                    UserCredentialModel credentials = new UserCredentialModel();
                    credentials.setType(CredentialRepresentation.PASSWORD);
                    credentials.setValue(passwordNew);

                    realm.updateCredential(user, credentials);

                    return Flows.forms(realm, request).setUser(user).forwardToPassword();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

    @Path("")
    @GET
    public Response accountPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    return Flows.forms(realm, request).setUser(user).forwardToAccount();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

    @Path("social")
    @GET
    public Response socialPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    return Flows.forms(realm, request).setUser(user).forwardToSocial();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

    @Path("totp")
    @GET
    public Response totpPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    return Flows.forms(realm, request).setUser(user).forwardToTotp();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

    @Path("password")
    @GET
    public Response passwordPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    return Flows.forms(realm, request).setUser(user).forwardToPassword();
                } else {
                    return Response.status(Status.FORBIDDEN).build();
                }
            }
        }.call();
    }

}
