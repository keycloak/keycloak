package org.keycloak.services.resources.account;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.Version;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.theme.BrowserSecurityHeaderSetup;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.utils.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by st on 29/03/17.
 */
public class AccountConsole {

    @Context
    protected UriInfo uriInfo;
    private RealmModel realm;
    private Theme theme;

    public AccountConsole(RealmModel realm, Theme theme) {
        this.realm = realm;
        this.theme = theme;
    }

    @GET
    @NoCache
    public Response getMainPage() throws URISyntaxException, IOException, FreeMarkerException {
        if (!uriInfo.getRequestUri().getPath().endsWith("/")) {
            return Response.status(302).location(uriInfo.getRequestUriBuilder().path("/").build()).build();
        } else {
            Map<String, Object> map = new HashMap<>();

            URI baseUri = uriInfo.getBaseUri();

            String authUrl = baseUri.toString();
            authUrl = authUrl.substring(0, authUrl.length() - 1);

            map.put("authUrl", authUrl);
            map.put("resourceUrl", Urls.themeRoot(baseUri) + "/account/" + theme.getName());
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
    @Path("index.html")
    public Response getIndexHtmlRedirect() {
        return Response.status(302).location(uriInfo.getRequestUriBuilder().path("../").build()).build();
    }

}
