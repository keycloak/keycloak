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
package org.keycloak.services.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.SecureContextResolver;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Provider
@Path("/")
public class WelcomeResource {

    protected static final Logger logger = Logger.getLogger(WelcomeResource.class);

    private volatile Boolean shouldBootstrap;

    @Context
    KeycloakSession session;

    /**
     * Welcome page of Keycloak
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getWelcomePage() throws URISyntaxException {
        String requestUri = session.getContext().getUri().getRequestUri().toString();
        if (!requestUri.endsWith("/")) {
            return Response.seeOther(new URI(requestUri + "/")).build();
        } else {
            return createWelcomePage(null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response createUser() {
        HttpRequest request = session.getContext().getHttpRequest();
        MultivaluedMap<String, String> formData = request.getDecodedFormParameters();

        if (!shouldBootstrap()) {
            return createWelcomePage(null, null);
        } else {
            if (!isLocal(session)) {
                ServicesLogger.LOGGER.rejectedNonLocalAttemptToCreateInitialUser(session.getContext().getConnection().getRemoteHost());
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }

            csrfCheck(formData);

            String username = formData.getFirst("username");
            String password = formData.getFirst("password");
            String passwordConfirmation = formData.getFirst("passwordConfirmation");

            if (username != null) {
                username = username.trim();
            }

            if (username == null || username.length() == 0) {
                return createWelcomePage(null, "Username is missing");
            }

            if (password == null || password.length() == 0) {
                return createWelcomePage(null, "Password is missing");
            }

            if (!password.equals(passwordConfirmation)) {
                return createWelcomePage(null, "Password and confirmation doesn't match");
            }


            try {
                ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
                applianceBootstrap.createMasterRealmUser(username, password, false);
            } catch (ModelException e) {
                session.getTransactionManager().rollback();
                logger.error("Error creating the administrative user", e);
                return createWelcomePage(null, "Error creating the administrative user: " + e.getMessage());
            }

            expireCsrfCookie();

            shouldBootstrap = false;
            return createWelcomePage("User created", null);
        }
    }

    /**
     * Resources for welcome page
     *
     * @param path
     * @return
     */
    @GET
    @Path("/welcome-content/{path}")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getResource(@PathParam("path") String path) {
        try {
            InputStream resource = getTheme().getResourceAsStream(path);
            if (resource != null) {
                String contentType = MimeTypeUtil.getContentType(path);
                Response.ResponseBuilder builder = Response.ok(resource).type(contentType).cacheControl(CacheControlUtil.getDefaultCacheControl());
                return builder.build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (IOException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Response createWelcomePage(String successMessage, String errorMessage) {
        try {
            Theme theme = getTheme();

            if(Objects.isNull(theme)) {
                logger.error("Theme is null please check the \"--spi-theme--default\" parameter");
                errorMessage = "The theme is null";
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST)
                        .entity(errorMessage)
                        .cacheControl(CacheControlUtil.noCache());
                return rb.build();
            }

            boolean bootstrap = shouldBootstrap();
            boolean adminConsoleEnabled = isAdminConsoleEnabled();
            Properties themeProperties = theme.getProperties();
            boolean redirectToAdmin = Boolean.parseBoolean(themeProperties.getProperty("redirectToAdmin", "false"));
            URI adminUrl = session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path("/admin/").build();

            // Redirect to the Administration Console if the administrative user already exists.
            if (redirectToAdmin && !bootstrap && adminConsoleEnabled && successMessage == null) {
                return Response.status(302).location(adminUrl).build();
            }

            Map<String, Object> map = new HashMap<>();
            String commonPath = themeProperties.getProperty("common", "common/keycloak");

            map.put("bootstrap", bootstrap);
            map.put("adminConsoleEnabled", adminConsoleEnabled);
            map.put("properties", themeProperties);
            map.put("adminUrl", adminUrl);
            map.put("baseUrl", session.getContext().getUri(UrlType.FRONTEND).getBaseUri());
            map.put("productName", Version.NAME);
            map.put("resourcesPath", "resources/" + Version.RESOURCES_VERSION + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName());
            map.put("resourcesCommonPath", "resources/" + Version.RESOURCES_VERSION + "/" + commonPath);

            boolean isLocal = isLocal(session);
            map.put("localUser", isLocal);

            if (bootstrap) {
                String localAdminUrl = session.getContext().getUri(UrlType.LOCAL_ADMIN).getBaseUri().toString();
                String adminCreationMessage = getAdminCreationMessage();
                map.put("localAdminUrl", localAdminUrl);
                map.put("adminUserCreationMessage", adminCreationMessage);

                if (isLocal) {
                    String stateChecker = setCsrfCookie();
                    map.put("stateChecker", stateChecker);
                }
            }
            if (successMessage != null) {
                map.put("successMessage", successMessage);
            }
            if (errorMessage != null) {
                map.put("errorMessage", errorMessage);
            }
            FreeMarkerProvider freeMarkerUtil = session.getProvider(FreeMarkerProvider.class);
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);

            ResponseBuilder rb = Response.status(errorMessage == null ? Status.OK : Status.BAD_REQUEST)
                    .entity(result)
                    .cacheControl(CacheControlUtil.noCache());
            return rb.build();
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private static boolean isAdminConsoleEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.ADMIN_V2);
    }

    private Theme getTheme() {
        try {
            return session.theme().getTheme(Theme.Type.WELCOME);
        } catch (IOException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected String getAdminCreationMessage() {
        return "or use a bootstrap-admin command";
    }

    private boolean shouldBootstrap() {
        if (shouldBootstrap == null) {
            synchronized (this) {
                if (shouldBootstrap == null) {
                    shouldBootstrap = new ApplianceBootstrap(session).isNoMasterUser();
                }
            }
        }
        return shouldBootstrap;
    }

    public static boolean isLocal(KeycloakSession session) {
        ClientConnection clientConnection = session.getContext().getConnection();
        String remoteAddress = clientConnection.getRemoteAddr();
        String localAddress = clientConnection.getLocalAddr();
        HttpRequest request = session.getContext().getHttpRequest();
        HttpHeaders headers = request.getHttpHeaders();
        String xForwardedFor = headers.getHeaderString("X-Forwarded-For");
        String forwarded = headers.getHeaderString("Forwarded");
        logger.debugf("Checking isLocal. Remote address: %s, Local address: %s, X-Forwarded-For header: %s, Forwarded header: %s", remoteAddress, localAddress, xForwardedFor, forwarded);

        // Consider that welcome page accessed locally just if it was accessed really through "localhost" URL and without loadbalancer (x-forwarded-for and forwarded header is empty).
        return xForwardedFor == null && forwarded == null && SecureContextResolver.isLocalAddress(remoteAddress) && SecureContextResolver.isLocalAddress(localAddress);
    }

    private String setCsrfCookie() {
        String stateChecker = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
        session.getProvider(CookieProvider.class).set(CookieType.WELCOME_CSRF, stateChecker);
        return stateChecker;
    }

    private void expireCsrfCookie() {
        session.getProvider(CookieProvider.class).expire(CookieType.WELCOME_CSRF);
    }

    private void csrfCheck(final MultivaluedMap<String, String> formData) {
        String formStateChecker = formData.getFirst("stateChecker");
        String cookieStateChecker = session.getProvider(CookieProvider.class).get(CookieType.WELCOME_CSRF);

        if (cookieStateChecker == null || !cookieStateChecker.equals(formStateChecker)) {
            throw new ForbiddenException();
        }
    }
}
