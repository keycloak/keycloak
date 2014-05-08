package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
                uriInfo.getBaseUriBuilder().path(AdminRoot.class).path(AdminRoot.class, "getAdminConsole").path("index.html").build(master.getName())
        ).build();
    }

    @Path("index.html")
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
        logger.info("*** get console for realm: " + name);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        AdminConsole service = new AdminConsole(realm);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        logger.info("returning AdminConsole");
        return service;
    }


    protected Auth authenticateRealmAdminRequest(HttpHeaders headers) {
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
        UserModel user = authManager.authenticateBearerToken(realm, uriInfo, headers);
        if (user == null) {
            logger.debug("Token not valid");
            throw new UnauthorizedException("Bearer");
        }

        ApplicationModel consoleApp = realm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
        if (consoleApp == null) {
            throw new NotFoundException("Could not find admin console application");
        }
        Auth auth = new Auth(realm, user, consoleApp);
        return auth;


    }

    public static UriBuilder realmsUrl(UriInfo uriInfo) {
        return realmsUrl(uriInfo.getBaseUriBuilder());
    }

    public static UriBuilder realmsUrl(UriBuilder base) {
        return adminBaseUrl(base).path(AdminRoot.class, "getRealmsAdmin");
    }

    @Path("realms")
    public RealmsAdminResource getRealmsAdmin(@Context final HttpHeaders headers) {
        Auth auth = authenticateRealmAdminRequest(headers);
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
