package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/realms")
public class RealmsResource {
    protected static Logger logger = Logger.getLogger(RealmsResource.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpHeaders headers;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    protected TokenManager tokenManager;

    public RealmsResource(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public static UriBuilder realmBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getRealmResource");
    }

    @Path("{realm}/tokens")
    public TokenService getTokenService(final @PathParam("realm") String id) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) {
            logger.debug("realm not found");
            throw new NotFoundException();
        }
        TokenService tokenService = new TokenService(realm, tokenManager);
        resourceContext.initResource(tokenService);
        return tokenService;
    }

    @Path("{realm}/account")
    public AccountService getAccountService(final @PathParam("realm") String id) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) {
            logger.debug("realm not found");
            throw new NotFoundException();
        }

        ApplicationModel application = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APPLICATION);
        if (application == null || !application.isEnabled()) {
            logger.debug("account management not enabled");
            throw new NotFoundException();
        }

        AccountService accountService = new AccountService(realm, application, tokenManager);
        resourceContext.initResource(accountService);
        return accountService;
    }

    @Path("{realm}")
    public PublicRealmResource getRealmResource(final @PathParam("realm") String id) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) {
            logger.debug("realm not found");
            throw new NotFoundException();
        }
        PublicRealmResource realmResource = new PublicRealmResource(realm);
        resourceContext.initResource(realmResource);
        return realmResource;
    }


}
