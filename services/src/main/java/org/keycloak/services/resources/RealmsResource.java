package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.social.SocialRequestManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    ResourceContext resourceContext;

    protected TokenManager tokenManager;

    protected SocialRequestManager socialRequestManager;

    public RealmsResource(TokenManager tokenManager, SocialRequestManager socialRequestManager) {
        this.tokenManager = tokenManager;
        this.socialRequestManager = socialRequestManager;
    }

    public static UriBuilder realmBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getRealmResource");
    }

    @Path("{realm}/tokens")
    public TokenService getTokenService(final @PathParam("realm") String id) {
        return new Transaction(false) {
            @Override
            protected TokenService callImpl() {
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
        }.call();

    }

    @Path("{realm}/social")
    public SocialService getSocialService(final @PathParam("realm") String id) {
        return new Transaction(false) {
            @Override
            protected SocialService callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.getRealm(id);
                if (realm == null) {
                    logger.debug("realm not found");
                    throw new NotFoundException();
                }
                SocialService socialService = new SocialService(realm, tokenManager, socialRequestManager);
                resourceContext.initResource(socialService);
                return socialService;
            }
        }.call();
    }

    @Path("{realm}")
    public PublicRealmResource getRealmResource(final @PathParam("realm") String id) {
        return new Transaction(false) {
            @Override
            protected PublicRealmResource callImpl() {
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
        }.call();
    }


}
