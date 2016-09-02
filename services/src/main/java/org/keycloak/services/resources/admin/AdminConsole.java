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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.Config;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.theme.BrowserSecurityHeaderSetup;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.utils.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdminConsole {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @Context
    protected KeycloakSession session;

    @Context
    protected Providers providers;

    @Context
    protected KeycloakApplication keycloak;

    protected AppAuthManager authManager;
    protected RealmModel realm;

    public AdminConsole(RealmModel realm) {
        this.realm = realm;
        this.authManager = new AppAuthManager();
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
        return new ClientManager().toInstallationRepresentation(realm, consoleApp, keycloak.getBaseUri(uriInfo));

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
        URI redirect = AdminRoot.adminConsoleUrl(uriInfo).build(realm.getName());

        return Response.status(302).location(
                OIDCLoginProtocolService.logoutUrl(uriInfo).queryParam("redirect_uri", redirect.toString()).build(realm.getName())
        ).build();
    }

    /**
     * Main page of this realm's admin console
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @NoCache
    public Response getMainPage() throws URISyntaxException, IOException, FreeMarkerException {
        if (!uriInfo.getRequestUri().getPath().endsWith("/")) {
            return Response.status(302).location(uriInfo.getRequestUriBuilder().path("/").build()).build();
        } else {
            Theme theme = AdminRoot.getTheme(session, realm);

            Map<String, Object> map = new HashMap<>();

            URI baseUri = uriInfo.getBaseUri();

            String authUrl = baseUri.toString();
            authUrl = authUrl.substring(0, authUrl.length() - 1);

            map.put("authUrl", authUrl);
            map.put("resourceUrl", Urls.themeRoot(baseUri) + "/admin/" + theme.getName());
            map.put("masterRealm", Config.getAdminRealm());
            map.put("resourceVersion", Version.RESOURCES_VERSION);
            map.put("properties", theme.getProperties());

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8).language(Locale.ENGLISH).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
            return builder.build();
        }
    }

    @GET
    @Path("{indexhtml: index.html}") // this expression is a hack to get around jaxdoclet generation bug.  Doesn't like index.html
    public Response getIndexHtmlRedirect() {
        return Response.status(302).location(uriInfo.getRequestUriBuilder().path("../").build()).build();
    }

    @GET
    @Path("messages.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Properties getMessages(@QueryParam("lang") String lang) {
        return AdminRoot.getMessages(session, realm, "admin-messages", lang);
    }

}
