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

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Providers;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.jose.jws.JWSInput;
import org.jboss.resteasy.jose.jws.crypto.RSAProvider;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.AbstractOAuthClient;
import org.keycloak.jaxrs.JaxrsOAuthClient;
import org.keycloak.models.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.email.EmailSender;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.FormFlows;
import org.keycloak.services.resources.flows.Pages;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.services.validation.Validation;
import org.picketlink.idm.credential.util.TimeBasedOTP;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountService {

    private static final Logger logger = Logger.getLogger(AccountService.class);

    public static final String ACCOUNT_IDENTITY_COOKIE = "KEYCLOAK_ACCOUNT_IDENTITY";

    private RealmModel realm;

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private Providers providers;

    private AuthenticationManager authManager = new AuthenticationManager();

    private ApplicationModel application;

    private TokenManager tokenManager;

    public AccountService(RealmModel realm, ApplicationModel application, TokenManager tokenManager) {
        this.realm = realm;
        this.application = application;
        this.tokenManager = tokenManager;
    }

    private Response forwardToPage(String path, String template) {
        AuthenticationManager.Auth auth = getAuth(false);
        if (auth != null) {
            return Flows.forms(realm, request, uriInfo).setUser(auth.getUser()).forwardToForm(template);
        } else {
            return login(path);
        }
    }

    @Path("")
    @OPTIONS
    public Response accountPreflight() {
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    @Path("")
    @GET
    public Response accountPage() {
        List<MediaType> types = headers.getAcceptableMediaTypes();
        if (types.contains(MediaType.WILDCARD_TYPE) || (types.contains(MediaType.TEXT_HTML_TYPE))) {
            return forwardToPage(null, Pages.ACCOUNT);
        } else if (types.contains(MediaType.APPLICATION_JSON_TYPE)) {
            AuthenticationManager.Auth auth = getAuth(true);
            return Cors.add(request, Response.ok(RealmManager.toRepresentation(auth.getUser()))).auth().allowedOrigins(auth.getClient()).build();
        } else {
            return Response.notAcceptable(Variant.VariantListBuilder.newInstance().mediaTypes(MediaType.TEXT_HTML_TYPE, MediaType.APPLICATION_JSON_TYPE).build()).build();
        }
    }

    @Path("social")
    @GET
    public Response socialPage() {
        return forwardToPage("social", Pages.SOCIAL);
    }

    @Path("totp")
    @GET
    public Response totpPage() {
        return forwardToPage("totp", Pages.TOTP);
    }

    @Path("password")
    @GET
    public Response passwordPage() {
        return forwardToPage("password", Pages.PASSWORD);
    }

    @Path("access")
    @GET
    public Response accessPage() {
        return forwardToPage("access", Pages.ACCESS);
    }

    @Path("")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processAccountUpdate(final MultivaluedMap<String, String> formData) {
        AuthenticationManager.Auth auth = getAuth(true);
        UserModel user = auth.getUser();

        String error = Validation.validateUpdateProfileForm(formData);
        if (error != null) {
            return Flows.forms(realm, request, uriInfo).setUser(user).setError(error).forwardToAccount();
        }

        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));
        user.setEmail(formData.getFirst("email"));

        return Flows.forms(realm, request, uriInfo).setUser(user).setError("accountUpdated")
                .setErrorType(FormFlows.MessageType.SUCCESS).forwardToAccount();
    }

    @Path("totp-remove")
    @GET
    public Response processTotpRemove() {
        AuthenticationManager.Auth auth = getAuth(true);
        UserModel user = auth.getUser();

        user.setTotp(false);
        return Flows.forms(realm, request, uriInfo).setError("successTotpRemoved").setErrorType(FormFlows.MessageType.SUCCESS)
                .setUser(user).forwardToTotp();
    }

    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processTotpUpdate(final MultivaluedMap<String, String> formData) {
        AuthenticationManager.Auth auth = getAuth(true);
        UserModel user = auth.getUser();

        String totp = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");

        FormFlows forms = Flows.forms(realm, request, uriInfo).setUser(user);
        if (Validation.isEmpty(totp)) {
            return forms.setError(Messages.MISSING_TOTP).forwardToTotp();
        } else if (!new TimeBasedOTP().validate(totp, totpSecret.getBytes())) {
            return forms.setError(Messages.INVALID_TOTP).forwardToTotp();
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.TOTP);
        credentials.setValue(totpSecret);
        realm.updateCredential(user, credentials);

        user.setTotp(true);

        return Flows.forms(realm, request, uriInfo).setError("successTotp").setErrorType(FormFlows.MessageType.SUCCESS)
                .setUser(user).forwardToTotp();
    }

    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processPasswordUpdate(final MultivaluedMap<String, String> formData) {
        AuthenticationManager.Auth auth = getAuth(true);
        UserModel user = auth.getUser();

        FormFlows forms = Flows.forms(realm, request, uriInfo).setUser(user);

        String password = formData.getFirst("password");
        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        if (Validation.isEmpty(passwordNew)) {
            return forms.setError(Messages.MISSING_PASSWORD).forwardToPassword();
        } else if (!passwordNew.equals(passwordConfirm)) {
            return forms.setError(Messages.INVALID_PASSWORD_CONFIRM).forwardToPassword();
        }

        if (Validation.isEmpty(password)) {
            return forms.setError(Messages.MISSING_PASSWORD).forwardToPassword();
        } else if (!realm.validatePassword(user, password)) {
            return forms.setError(Messages.INVALID_PASSWORD_EXISTING).forwardToPassword();
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(passwordNew);

        realm.updateCredential(user, credentials);

        return Flows.forms(realm, request, uriInfo).setUser(user).setError("accountPasswordUpdated")
                .setErrorType(FormFlows.MessageType.SUCCESS).forwardToPassword();
    }

    @Path("login-redirect")
    @GET
    public Response loginRedirect(@QueryParam("code") String code,
                                  @QueryParam("state") String state,
                                  @QueryParam("error") String error,
                                  @Context HttpHeaders headers) {
        try {
            if (error != null) {
                logger.debug("error from oauth");
                throw new ForbiddenException("error");
            }
            if (!realm.isEnabled()) {
                logger.debug("realm not enabled");
                throw new ForbiddenException();
            }
            UserModel client = application.getApplicationUser();
            if (!client.isEnabled() || !application.isEnabled()) {
                logger.debug("account management app not enabled");
                throw new ForbiddenException();
            }
            if (code == null) {
                logger.debug("code not specified");
                throw new BadRequestException();
            }
            if (state == null) {
                logger.debug("state not specified");
                throw new BadRequestException();
            }
            String path = new JaxrsOAuthClient().checkStateCookie(uriInfo, headers);

            JWSInput input = new JWSInput(code, providers);
            boolean verifiedCode = false;
            try {
                verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
            } catch (Exception ignored) {
                logger.debug("Failed to verify signature", ignored);
            }
            if (!verifiedCode) {
                logger.debug("unverified access code");
                throw new BadRequestException();
            }
            String key = input.readContent(String.class);
            AccessCodeEntry accessCode = tokenManager.pullAccessCode(key);
            if (accessCode == null) {
                logger.debug("bad access code");
                throw new BadRequestException();
            }
            if (accessCode.isExpired()) {
                logger.debug("access code expired");
                throw new BadRequestException();
            }
            if (!accessCode.getToken().isActive()) {
                logger.debug("access token expired");
                throw new BadRequestException();
            }
            if (!accessCode.getRealm().getId().equals(realm.getId())) {
                logger.debug("bad realm");
                throw new BadRequestException();

            }
            if (!client.getLoginName().equals(accessCode.getClient().getLoginName())) {
                logger.debug("bad client");
                throw new BadRequestException();
            }

            UriBuilder redirectBuilder = Urls.accountBase(uriInfo.getBaseUri());
            if (path != null) {
                redirectBuilder.path(path);
            }
            URI redirectUri = redirectBuilder.build(realm.getId());

            NewCookie cookie = authManager.createAccountIdentityCookie(realm, accessCode.getUser(), client, Urls.accountBase(uriInfo.getBaseUri()).build(realm.getId()));
            return Response.status(302).cookie(cookie).location(redirectUri).build();
        } finally {
            authManager.expireCookie(AbstractOAuthClient.OAUTH_TOKEN_REQUEST_STATE, uriInfo.getAbsolutePath().getPath());
        }
    }

    @Path("logout")
    @GET
    public Response logout() {
        // TODO Should use single-sign out via TokenService
        URI baseUri = Urls.accountBase(uriInfo.getBaseUri()).build(realm.getId());
        authManager.expireIdentityCookie(realm, uriInfo);
        authManager.expireAccountIdentityCookie(baseUri);
        return Response.status(302).location(baseUri).build();
    }

    private Response login(String path) {
        JaxrsOAuthClient oauth = new JaxrsOAuthClient();
        String authUrl = Urls.realmLoginPage(uriInfo.getBaseUri(), realm.getId()).toString();
        oauth.setAuthUrl(authUrl);

        oauth.setClientId(Constants.ACCOUNT_MANAGEMENT_APPLICATION);

        URI accountUri = Urls.accountPageBuilder(uriInfo.getBaseUri()).path(AccountService.class, "loginRedirect").build(realm.getId());

        oauth.setStateCookiePath(accountUri.getPath());
        return oauth.redirect(uriInfo, accountUri.toString(), path);
    }

    private AuthenticationManager.Auth getAuth(boolean required) {
        AuthenticationManager.Auth auth = authManager.authenticateAccountIdentity(realm, uriInfo, headers);
        if (auth == null && required) {
            throw new ForbiddenException();
        }
        return auth;
    }

}
