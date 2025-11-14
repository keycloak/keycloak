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
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.common.util.Environment;
import org.keycloak.common.util.UriUtils;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.Urls;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.util.ViteManifest;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.SecureContextResolver;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.models.UserModel.IS_TEMP_ADMIN_ATTR_NAME;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdminConsole {
    protected static final Logger logger = Logger.getLogger(AdminConsole.class);

    protected final ClientConnection clientConnection;

    protected final HttpRequest request;

    protected final HttpResponse response;

    protected final KeycloakSession session;

    protected final RealmModel realm;

    public AdminConsole(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
        this.request = session.getContext().getHttpRequest();
        this.response = session.getContext().getHttpResponse();
    }

    public static class WhoAmI {
        protected String userId;
        protected String realm;
        protected String displayName;
        protected Locale locale;
        protected boolean isTemporary;

        @JsonProperty("createRealm")
        protected boolean createRealm;
        @JsonProperty("realm_access")
        protected Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();

        public WhoAmI() {
        }

        public WhoAmI(String userId, String realm, String displayName, boolean createRealm, Map<String, Set<String>> realmAccess, Locale locale, boolean isTemporary) {
            this.userId = userId;
            this.realm = realm;
            this.displayName = displayName;
            this.createRealm = createRealm;
            this.realmAccess = realmAccess;
            this.locale = locale;
            this.isTemporary = isTemporary;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isCreateRealm() {
            return createRealm;
        }

        public void setCreateRealm(boolean createRealm) {
            this.createRealm = createRealm;
        }

        public Map<String, Set<String>> getRealmAccess() {
            return realmAccess;
        }

        public void setRealmAccess(Map<String, Set<String>> realmAccess) {
            this.realmAccess = realmAccess;
        }

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(Locale locale) {
            this.locale = locale;
        }

        @JsonProperty(value = "locale")
        public String getLocaleLanguageTag() {
            return locale != null ? locale.toLanguageTag() : null;
        }

        public boolean isTemporary() {
            return isTemporary;
        }

        public void setTemporary(boolean temporary) {
            isTemporary = temporary;
        }
    }

    /**
     * Adapter configuration for the admin console for this realm
     *
     * @return
     */
    @Path("config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ClientManager.InstallationAdapterConfig config() {
        ClientModel consoleApp = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (consoleApp == null) {
            throw new NotFoundException("Could not find admin console client");
        }
        return new ClientManager(new RealmManager(session)).toInstallationRepresentation(realm, consoleApp, session.getContext().getUri().getBaseUri());    }

    @Path("whoami")
    @OPTIONS
    public Response whoAmIPreFlight() {
        return new AdminCorsPreflightService().preflight();
    }

    /**
     * Permission information
     *
     * @param currentRealm
     * @return
     */
    @Path("whoami")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response whoAmI(@QueryParam("currentRealm") String currentRealm) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ADMIN_API)) {
            throw new NotFoundException();
        }

        RealmManager realmManager = new RealmManager(session);
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(session.getContext().getRequestHeaders())
                .authenticate();

        if (authResult == null) {
            throw new NotAuthorizedException("Bearer");
        }

        final String issuedFor = authResult.token().getIssuedFor();
        if (!Constants.ADMIN_CONSOLE_CLIENT_ID.equals(issuedFor)) {
            if (issuedFor == null) {
                throw new ForbiddenException("No azp claim in the token");
            }
            // check the attribute to see if the app is defined as an admin console
            ClientModel client  = session.clients().getClientByClientId(realm, issuedFor);
            if (client == null || !Boolean.parseBoolean(client.getAttribute(Constants.SECURITY_ADMIN_CONSOLE_ATTR))) {
                throw new ForbiddenException("Token issued for an application that is not the admin console: " + issuedFor);
            }
        }

        UserModel user= authResult.user();
        String displayName;
        if ((user.getFirstName() != null && !user.getFirstName().trim().equals("")) || (user.getLastName() != null && !user.getLastName().trim().equals(""))) {
            displayName = user.getFirstName();
            if (user.getLastName() != null) {
                displayName = displayName != null ? displayName + " " + user.getLastName() : user.getLastName();
            }
        } else {
            displayName = user.getUsername();
        }

        RealmModel masterRealm = getAdminstrationRealm(realmManager);
        Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();
        if (masterRealm == null)
            throw new NotFoundException("No realm found");
        boolean createRealm = false;
        if (realm.equals(masterRealm)) {
            logger.debug("setting up realm access for a master realm user");
            RoleModel createRealmRole = masterRealm.getRole(AdminRoles.CREATE_REALM);
            if (createRealmRole != null) {
                createRealm = user.hasRole(createRealmRole);
            }
            addMasterRealmAccess(user, currentRealm != null ? currentRealm : realm.getName(), realmAccess);
        } else {
            logger.debug("setting up realm access for a realm user");
            addRealmAccess(realm, user, realmAccess);
        }

        if (realmAccess.isEmpty() || realmAccess.values().iterator().next().isEmpty()) {
            // if the user has no access in the realm just return forbidden/403
            throw new ForbiddenException("No realm access");
        }

        Locale locale = session.getContext().resolveLocale(user);

        return Cors.builder()
                .allowedOrigins(authResult.token())
                .allowedMethods("GET")
                .auth()
                .add(Response.ok(new WhoAmI(user.getId(), realm.getName(), displayName, createRealm, realmAccess, locale, Boolean.parseBoolean(user.getFirstAttribute(IS_TEMP_ADMIN_ATTR_NAME)))));
    }

    private void addRealmAccess(RealmModel realm, UserModel user, Map<String, Set<String>> realmAdminAccess) {
        RealmManager realmManager = new RealmManager(session);
        ClientModel realmAdminApp = realm.getClientByClientId(realmManager.getRealmAdminClientId(realm));
        getRealmAdminAccess(realm, realmAdminApp, user, realmAdminAccess);
    }

    private void addMasterRealmAccess(UserModel user, String currentRealm, Map<String, Set<String>> realmAdminAccess) {
        final RealmModel realm = session.realms().getRealmByName(currentRealm);
        if (realm != null) {
            getRealmAdminAccess(realm, realm.getMasterAdminClient(), user, realmAdminAccess);
        } else {
            throw new NotFoundException("Realm not found");
        }
    }

    private void getRealmAdminAccess(RealmModel realm, ClientModel client, UserModel user, Map<String, Set<String>> realmAdminAccess) {
        Set<String> realmRoles = client.getRolesStream()
          .filter(user::hasRole)
          .map(RoleModel::getName)
          .collect(Collectors.toSet());

        realmAdminAccess.put(realm.getName(), realmRoles);
    }

    /**
     * Logout from the admin console
     *
     * @return
     */
    @Path("logout")
    @GET
    @NoCache
    public Response logout() {
        URI redirect = AdminRoot.adminConsoleUrl(session.getContext().getUri(UrlType.ADMIN)).build(realm.getName());

        return Response.status(302).location(
                OIDCLoginProtocolService.logoutUrl(session.getContext().getUri(UrlType.ADMIN)).queryParam("post_logout_redirect_uri", redirect.toString()).build(realm.getName())
        ).build();
    }

    protected RealmModel getAdminstrationRealm(RealmManager realmManager) {
        return realmManager.getKeycloakAdminstrationRealm();
    }

    /**
     * Main page of this realm's admin console.
     */
    @GET
    @NoCache
    public Response getMainPage() throws IOException, FreeMarkerException {
        final var baseUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        final var adminUriInfo = session.getContext().getUri(UrlType.ADMIN);

        // Redirect to a URL with a trailing slash if the current URL doesn't have one.
        if (!adminUriInfo.getRequestUri().getPath().endsWith("/")) {
            return Response.status(302).location(adminUriInfo.getRequestUriBuilder().path("/").build()).build();
        } else {
            // Get the base URLs of the server and admin console.
            final var serverBaseUri = baseUriInfo.getBaseUri();
            final var adminBaseUri = adminUriInfo.getBaseUri();

            // Strip any trailing slashes from the URLs.
            final var serverBaseUrl = serverBaseUri.toString().replaceFirst("/+$", "");
            final var adminBaseUrl = adminBaseUri.toString().replaceFirst("/+$", "");

            final var map = new HashMap<String, Object>();
            final var theme = AdminRoot.getTheme(session, realm);
            final var isSecureContext = SecureContextResolver.isSecureContext(session);

            map.put("isSecureContext", isSecureContext);
            map.put("serverBaseUrl", serverBaseUrl);
            map.put("adminBaseUrl", adminBaseUrl);
            // TODO: Some variables are deprecated and only exist to provide backwards compatibility for older themes, they should be removed in a future version.
            // Note that these should be removed from the template of the Administration Console as well.
            map.put("authServerUrl", serverBaseUrl); // Superseded by 'serverBaseUrl', remove in the future.
            map.put("authUrl", adminBaseUrl); // Superseded by 'adminBaseUrl', remove in the future.
            map.put("consoleBaseUrl", Urls.adminConsoleRoot(adminBaseUri, realm.getName()).getPath());
            map.put("resourceUrl", Urls.themeRoot(adminBaseUri).getPath() + "/admin/" + theme.getName());
            map.put("resourceCommonUrl", Urls.themeRoot(adminBaseUri).getPath() + "/common/keycloak");
            map.put("masterRealm", Config.getAdminRealm());
            map.put("resourceVersion", Version.RESOURCES_VERSION);
            map.put("loginRealm", realm.getName());
            map.put("clientId", Constants.ADMIN_CONSOLE_CLIENT_ID);
            map.put("properties", theme.getProperties());
            map.put("darkMode", "true".equals(theme.getProperties().getProperty("darkMode"))
                    && realm.getAttribute("darkMode", true));

            final var devServerUrl = Environment.isDevMode() ? System.getenv(ViteManifest.ADMIN_VITE_URL) : null;

            if (devServerUrl != null) {
                map.put("devServerUrl", devServerUrl);
            }

            final var manifestFile = theme.getResourceAsStream(".vite/manifest.json");

            if (devServerUrl == null && manifestFile != null) {
                final var manifest = ViteManifest.parseFromInputStream(manifestFile);
                final var entryChunk = manifest.getEntryChunk();
                final var entryStyles = entryChunk.css().orElse(new String[] {});
                final var entryScript = entryChunk.file();
                final var entryImports = entryChunk.imports().orElse(new String[] {});

                map.put("entryStyles", entryStyles);
                map.put("entryScript", entryScript);
                map.put("entryImports", entryImports);
            }

            final var freeMarkerUtil = session.getProvider(FreeMarkerProvider.class);
            final var result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            final var builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8).language(Locale.ENGLISH).entity(result);

            // Allow iframes to be embedded from the server if the admin console is running on a different URL.
            if (!adminBaseUri.equals(serverBaseUri)) {
                session.getProvider(SecurityHeadersProvider.class).options().allowFrameSrc(UriUtils.getOrigin(serverBaseUri));
            }

            return builder.build();
        }
    }

    @GET
    @Path("{indexhtml: index.html}") // this expression is a hack to get around jaxdoclet generation bug.  Doesn't like index.html
    public Response getIndexHtmlRedirect() {
        return Response.status(302).location(session.getContext().getUri(UrlType.ADMIN).getRequestUriBuilder().path("../").build()).build();
    }

    @GET
    @Path("messages.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Properties getMessages(@QueryParam("lang") String lang) {
        return AdminRoot.getMessages(session, realm, lang, "admin-messages");
    }

}
