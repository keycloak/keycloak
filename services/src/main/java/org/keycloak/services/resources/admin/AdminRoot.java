/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Encode;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.WelcomeResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.services.resources.admin.info.ServerInfoAdminResource;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.logging.Logger;

/**
 * Root resource for admin console and admin REST API
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
@Path("/admin")
public class AdminRoot {
    protected static final Logger logger = Logger.getLogger(AdminRoot.class);

    protected TokenManager tokenManager;

    @Context
    protected KeycloakSession session;

    public AdminRoot() {
        this.tokenManager = new TokenManager();
    }

    public static UriBuilder adminBaseUrl(UriInfo uriInfo) {
        return adminBaseUrl(uriInfo.getBaseUriBuilder());
    }

    public static UriBuilder adminBaseUrl(UriBuilder base) {
        return base.path(AdminRoot.class);
    }

    /**
     * Convenience path to master realm admin console
     *
     * @exclude
     * @return
     */
    @GET
    @Operation(hidden = true)
    public Response masterRealmAdminConsoleRedirect() {
        KeycloakUriInfo adminUriInfo = session.getContext().getUri(UrlType.ADMIN);
        if (shouldRedirect(adminUriInfo)) {
            RealmModel master = new RealmManager(session).getKeycloakAdminstrationRealm();
            return Response.status(302).location(
                    adminUriInfo.getBaseUriBuilder().path(AdminRoot.class).path(AdminRoot.class, "getAdminConsole").path("/").build(master.getName())
            ).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    boolean shouldRedirect(KeycloakUriInfo adminUriInfo) {
        if (!isAdminConsoleEnabled()) {
            return false;
        }
        KeycloakUriInfo frontEndUriInfo = session.getContext().getUri();
        String frontEndUrl = frontEndUriInfo.getBaseUri().toString();
        String adminUrl = adminUriInfo.getBaseUri().toString();

        if (adminUrl.equals(frontEndUrl)) {
            return true; // admin is the same as front-end, we're not leaking information
        }
        String requestUrl = frontEndUriInfo.getRequestUri().toString();

        // if we're using the admin url or are local, it's also safe to redirect
        return requestUrl.startsWith(adminUrl) || WelcomeResource.isLocal(session);
    }

    /**
     * Convenience path to master realm admin console
     *
     * @exclude
     * @return
     */
    @Path("index.{html:html}") // expression is actually "index.html" but this is a hack to get around jax-doclet bug
    @GET
    @Operation(hidden = true)
    public Response masterRealmAdminConsoleRedirectHtml() {

        if (!isAdminConsoleEnabled()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return masterRealmAdminConsoleRedirect();
    }

    protected void resolveRealmAndUpdateSession(String name, KeycloakSession session) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found.  Did you type in a bad URL?");
        }
        session.getContext().setRealm(realm);
    }


    public static UriBuilder adminConsoleUrl(UriInfo uriInfo) {
        return adminConsoleUrl(uriInfo.getBaseUriBuilder());
    }

    public static UriBuilder adminConsoleUrl(UriBuilder base) {
        return adminBaseUrl(base).path(AdminRoot.class, "getAdminConsole");
    }

    /**
     * path to realm admin console ui
     *
     * @exclude
     * @param name Realm name (not id!)
     * @return
     */
    @Path("{realm}/console")
    @Operation(hidden = true)
    public AdminConsole getAdminConsole(final @PathParam("realm") String name) {

        if (!isAdminConsoleEnabled()) {
            throw new NotFoundException();
        }

        resolveRealmAndUpdateSession(name, session);

        return new AdminConsole(session);
    }


    public static AdminAuth authenticateRealmAdminRequest(KeycloakSession session) {
        HttpHeaders headers = session.getContext().getRequestHeaders();

        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = Encode.decodePath(token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1));
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }
        session.getContext().setRealm(realm);

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(session.getContext().getConnection())
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        session.getContext().setBearerToken(authResult.token());

        return new AdminAuth(realm, authResult.token(), authResult.user(), authResult.client());
    }

    public static UriBuilder realmsUrl(UriInfo uriInfo) {
        return realmsUrl(uriInfo.getBaseUriBuilder());
    }

    public static UriBuilder realmsUrl(UriBuilder base) {
        return adminBaseUrl(base).path(AdminRoot.class, "getRealmsAdmin");
    }

    /**
     * Base Path to realm admin REST interface
     *
     * @param headers
     * @return
     */
    @Path("realms")
    public RealmsAdminResource getRealmsAdmin() {
        HttpRequest request = getHttpRequest();

        if (!isAdminApiEnabled()) {
            throw new NotFoundException();
        }

        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new RealmsAdminResourcePreflight(session, null, tokenManager, request);
        }

        AdminAuth auth = authenticateRealmAdminRequest(session);
        if (auth != null) {
            if (logger.isDebugEnabled()) {
                logger.debugf("authenticated admin access for: %s", auth.getUser().getUsername());
            }
        }

        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().add();

        return new RealmsAdminResource(session, auth, tokenManager);
    }

    @Path("{any:.*}")
    @OPTIONS
    @Operation(hidden = true)
    public Object preFlight() {
        if (!isAdminApiEnabled()) {
            throw new NotFoundException();
        }

        return new AdminCorsPreflightService();
    }

    /**
     * General information about the server
     *
     * @param headers
     * @return
     */
    @Path("serverinfo")
    public Object getServerInfo() {

        if (!isAdminApiEnabled()) {
            throw new NotFoundException();
        }

        HttpRequest request = getHttpRequest();

        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new AdminCorsPreflightService();
        }

        AdminAuth auth = authenticateRealmAdminRequest(session);
        if (!AdminPermissions.realms(session, auth).isAdmin()) {
            throw new ForbiddenException();
        }

        if (auth != null) {
            logger.debugf("authenticated admin access for: %s", auth.getUser().getUsername());
        }

        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").auth().add();

        return new ServerInfoAdminResource(session, auth);
    }

    private HttpRequest getHttpRequest() {
        return session.getContext().getHttpRequest();
    }

    public static Theme getTheme(KeycloakSession session, RealmModel realm) throws IOException {
        return session.theme().getTheme(Theme.Type.ADMIN);
    }

    public static Properties getMessages(KeycloakSession session, RealmModel realm, String lang) {
        try {
            Theme theme = getTheme(session, realm);
            Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.ENGLISH;
            return theme.getMessages(locale);
        } catch (IOException e) {
            logger.error("Failed to load messages from theme", e);
            return new Properties();
        }
    }

    public static Properties getMessages(KeycloakSession session, RealmModel realm, String lang, String... bundles) {
        Properties compound = new Properties();
        for (String bundle : bundles) {
            Properties current = getMessages(session, realm, lang, bundle);
            compound.putAll(current);
        }
        return compound;
    }

    private static Properties getMessages(KeycloakSession session, RealmModel realm, String lang, String bundle) {
        try {
            Theme theme = getTheme(session, realm);
            Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.ENGLISH;
            return theme.getMessages(bundle, locale);
        } catch (IOException e) {
            logger.error("Failed to load messages from theme", e);
            return new Properties();
        }
    }

    private static boolean isAdminApiEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.ADMIN_API);
    }

    private static boolean isAdminConsoleEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.ADMIN_V2);
    }
}
