package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.DefaultOptionsMethodException;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/admin")
public class AdminRoot {
    protected static final Logger logger = Logger.getLogger(AdminRoot.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    protected AppAuthManager authManager;
    protected TokenManager tokenManager;

    @Context
    protected KeycloakSession session;

    public AdminRoot(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.authManager = new AppAuthManager(null);
    }

    public static UriBuilder adminBaseUrl(UriInfo uriInfo) {
        return adminBaseUrl(uriInfo.getBaseUriBuilder());
    }

    public static UriBuilder adminBaseUrl(UriBuilder base) {
        return base.path(AdminRoot.class);
    }



    @GET
    public Response masterRealmAdminConsoleRedirect() {
        RealmModel master = new RealmManager(session).getKeycloakAdminstrationRealm();
        return Response.status(302).location(
                uriInfo.getBaseUriBuilder().path(AdminRoot.class).path(AdminRoot.class, "getAdminConsole").path("/").build(master.getName())
        ).build();
    }

    @Path("index.{hack:html}")
    @GET
    public Response masterRealmAdminConsoleRedirectHtml() {
        return masterRealmAdminConsoleRedirect();
    }

    protected RealmModel locateRealm(String name, RealmManager realmManager) {
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm " + name + " not found");
        }
        return realm;
    }


    public static UriBuilder adminConsoleUrl(UriInfo uriInfo) {
        return adminConsoleUrl(uriInfo.getBaseUriBuilder());
    }

    public static UriBuilder adminConsoleUrl(UriBuilder base) {
        return adminBaseUrl(base).path(AdminRoot.class, "getAdminConsole");
    }

    @Path("{realm}/console")
    public AdminConsole getAdminConsole(final @PathParam("realm") String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        AdminConsole service = new AdminConsole(realm);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }


    protected AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
        String tokenString = authManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new UnauthorizedException("Bearer");
        JWSInput input = new JWSInput(tokenString);
        AccessToken token;
        try {
            token = input.readJsonContent(AccessToken.class);
        } catch (IOException e) {
            throw new UnauthorizedException("Bearer token format error");
        }
        String realmName = token.getAudience();
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new UnauthorizedException("Unknown realm in token");
        }
        AuthenticationManager.AuthResult authResult = authManager.authenticateBearerToken(realm, uriInfo, headers);
        if (authResult == null) {
            logger.debug("Token not valid");
            throw new UnauthorizedException("Bearer");
        }

        ClientModel client = realm.findClient(token.getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");

        }

        return new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
    }

    public static UriBuilder realmsUrl(UriInfo uriInfo) {
        return realmsUrl(uriInfo.getBaseUriBuilder());
    }

    public static UriBuilder realmsUrl(UriBuilder base) {
        return adminBaseUrl(base).path(AdminRoot.class, "getRealmsAdmin");
    }

    @Path("realms")
    public RealmsAdminResource getRealmsAdmin(@Context final HttpHeaders headers) {
        if (request.getHttpMethod().equalsIgnoreCase("OPTIONS")) {
            logger.info("*** CORS ADMIN PREFLIGHT!!!!");
            Response response = Cors.add(request, Response.ok()).preflight().allowedMethods("GET", "PUT", "POST", "DELETE").auth().build();
            throw new WebApplicationException(response);
        }

        AdminAuth auth = authenticateRealmAdminRequest(headers);
        if (auth != null) {
            logger.info("authenticated admin access for: " + auth.getUser().getLoginName());
        }

        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").auth().build(response);

        RealmsAdminResource adminResource = new RealmsAdminResource(auth, tokenManager);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        //resourceContext.initResource(adminResource);
        return adminResource;
    }

    @Path("serverinfo")
    public ServerInfoAdminResource getServerInfo(@Context final HttpHeaders headers) {
        ServerInfoAdminResource adminResource = new ServerInfoAdminResource();
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        //resourceContext.initResource(adminResource);
        return adminResource;
    }

}
