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

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/")
public class WelcomeResource {

    protected static final Logger logger = Logger.getLogger(WelcomeResource.class);

    private static final String KEYCLOAK_STATE_CHECKER = "WELCOME_STATE_CHECKER";

    @Context
    protected HttpHeaders headers;

    @Context
    private KeycloakSession session;

    public WelcomeResource() {
    }

    /**
     * Welcome page of Keycloak
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getWelcomePage() throws URISyntaxException {
        checkBootstrap();

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
    public Response createUser(final MultivaluedMap<String, String> formData) {
        checkBootstrap();

        if (!shouldBootstrap()) {
            return createWelcomePage(null, null);
        } else {
            if (!isLocal()) {
                ServicesLogger.LOGGER.rejectedNonLocalAttemptToCreateInitialUser(session.getContext().getConnection().getRemoteAddr());
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

            expireCsrfCookie();

            ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
            if (applianceBootstrap.isNoMasterUser()) {
                setBootstrap(false);
                applianceBootstrap.createMasterRealmUser(username, password);

                ServicesLogger.LOGGER.createdInitialAdminUser(username);
                return createWelcomePage("User created", null);
            } else {
                ServicesLogger.LOGGER.initialUserAlreadyCreated();
                return createWelcomePage(null, "Users already exists");
            }
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
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Response createWelcomePage(String successMessage, String errorMessage) {
        try {
            Theme theme = getTheme();

            Map<String, Object> map = new HashMap<>();

            map.put("productName", Version.NAME);
            map.put("productNameFull", Version.NAME_FULL);

            map.put("properties", theme.getProperties());
            map.put("adminUrl", session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path("/admin/").build());
            map.put("resourcesPath", "resources/" + Version.RESOURCES_VERSION + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName());
            map.put("resourcesCommonPath", "resources/" + Version.RESOURCES_VERSION + "/common/keycloak");

            boolean bootstrap = shouldBootstrap();
            map.put("bootstrap", bootstrap);
            if (bootstrap) {
                boolean isLocal = isLocal();
                map.put("localUser", isLocal);
                map.put("localAdminUrl", "http://localhost:8080/auth");
                map.put("adminUserCreationMessage","or use the add-user-keycloak script");

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
            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);

            ResponseBuilder rb = Response.status(errorMessage == null ? Status.OK : Status.BAD_REQUEST)
                    .entity(result)
                    .cacheControl(CacheControlUtil.noCache());
            return rb.build();
        } catch (Exception e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Theme getTheme() {
        try {
            return session.theme().getTheme(Theme.Type.WELCOME);
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkBootstrap() {
        if (shouldBootstrap())
            KeycloakApplication.BOOTSTRAP_ADMIN_USER.compareAndSet(true, new ApplianceBootstrap(session).isNoMasterUser());
    }

    private boolean shouldBootstrap() {
        return KeycloakApplication.BOOTSTRAP_ADMIN_USER.get();
    }

    private void setBootstrap(boolean value) {
        KeycloakApplication.BOOTSTRAP_ADMIN_USER.set(value);
    }

    private boolean isLocal() {
        try {
            ClientConnection clientConnection = session.getContext().getConnection();
            InetAddress remoteInetAddress = InetAddress.getByName(clientConnection.getRemoteAddr());
            InetAddress localInetAddress = InetAddress.getByName(clientConnection.getLocalAddr());
            String xForwardedFor = headers.getHeaderString("X-Forwarded-For");
            logger.debugf("Checking WelcomePage. Remote address: %s, Local address: %s, X-Forwarded-For header: %s", remoteInetAddress.toString(), localInetAddress.toString(), xForwardedFor);

            // Access through AJP protocol (loadbalancer) may cause that remoteAddress is "127.0.0.1".
            // So consider that welcome page accessed locally just if it was accessed really through "localhost" URL and without loadbalancer (x-forwarded-for header is empty).
            return isLocalAddress(remoteInetAddress) && isLocalAddress(localInetAddress) && xForwardedFor == null;
        } catch (UnknownHostException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isLocalAddress(InetAddress inetAddress) {
        return inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress();
    }

    private String setCsrfCookie() {
        String stateChecker = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
        String cookiePath = session.getContext().getUri().getPath();
        boolean secureOnly = session.getContext().getUri().getRequestUri().getScheme().equalsIgnoreCase("https");
        CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, stateChecker, cookiePath, null, null, 300, secureOnly, true);
        return stateChecker;
    }

    private void expireCsrfCookie() {
        String cookiePath = session.getContext().getUri().getPath();
        boolean secureOnly = session.getContext().getUri().getRequestUri().getScheme().equalsIgnoreCase("https");
        CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, "", cookiePath, null, null, 0, secureOnly, true);
    }

    private void csrfCheck(final MultivaluedMap<String, String> formData) {
        String formStateChecker = formData.getFirst("stateChecker");
        Cookie cookie = headers.getCookies().get(KEYCLOAK_STATE_CHECKER);
        if (cookie == null) {
            throw new ForbiddenException();
        }

        String cookieStateChecker = cookie.getValue();

        if (cookieStateChecker == null || !cookieStateChecker.equals(formStateChecker)) {
            throw new ForbiddenException();
        }
    }

}
