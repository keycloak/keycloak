package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;

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
    protected KeycloakSession identitySession;

    @Context
    ResourceContext resourceContext;

    protected TokenManager tokenManager;

    public RealmsResource(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public static UriBuilder realmBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getRealmResource");
    }

    @Path("{realm}/tokens")
    public TokenService getTokenService(@PathParam("realm") String id) {
        RealmManager realmManager = new RealmManager(identitySession);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) {
            logger.debug("realm not found");
            throw new NotFoundException();
        }
        TokenService tokenService = new TokenService(realm, tokenManager);
        resourceContext.initResource(tokenService);
        return tokenService;

    }


    @Path("{realm}")
    public RealmSubResource getRealmResource(@PathParam("realm") String id) {
        RealmManager realmManager = new RealmManager(identitySession);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) {
            logger.debug("realm not found");
            throw new NotFoundException();
        }
        RealmSubResource realmResource = new RealmSubResource(realm);
        resourceContext.initResource(realmResource);
        return realmResource;

    }


    @POST
    @Consumes("application/json")
    public Response importRealm(RealmRepresentation rep) {
        identitySession.getTransaction().begin();
        RealmModel realm;
        try {
            RealmManager realmManager = new RealmManager(identitySession);
            RealmModel defaultRealm = realmManager.getRealm(RealmModel.DEFAULT_REALM);
            UserModel realmCreator = new AuthenticationManager().authenticateBearerToken(defaultRealm, headers);
            RoleModel creatorRole = defaultRealm.getRole(RegistrationService.REALM_CREATOR_ROLE);
            if (!defaultRealm.hasRole(realmCreator, creatorRole)) {
                logger.warn("not a realm creator");
                throw new NotAuthorizedException("Bearer");
            }
            realm = realmManager.importRealm(rep, realmCreator);
            identitySession.getTransaction().commit();
        } catch (RuntimeException re) {
            identitySession.getTransaction().rollback();
            throw re;
        }
        UriBuilder builder = uriInfo.getRequestUriBuilder().path(realm.getId());
        return Response.created(builder.build())
                .entity(RealmSubResource.realmRep(realm, uriInfo))
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
