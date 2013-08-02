package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
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
    public RealmSubResource getRealmResource(final @PathParam("realm") String id) {
        return new Transaction(false) {
            @Override
            protected RealmSubResource callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.getRealm(id);
                if (realm == null) {
                    logger.debug("realm not found");
                    throw new NotFoundException();
                }
                RealmSubResource realmResource = new RealmSubResource(realm);
                resourceContext.initResource(realmResource);
                return realmResource;
            }
        }.call();
    }


    @POST
    @Consumes("application/json")
    public Response importRealm(final RealmRepresentation rep) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel defaultRealm = realmManager.getRealm(RealmModel.DEFAULT_REALM);
                UserModel realmCreator = new AuthenticationManager().authenticateBearerToken(defaultRealm, headers);
                RoleModel creatorRole = defaultRealm.getRole(RegistrationService.REALM_CREATOR_ROLE);
                if (!defaultRealm.hasRole(realmCreator, creatorRole)) {
                    logger.warn("not a realm creator");
                    throw new NotAuthorizedException("Bearer");
                }
                RealmModel realm = realmManager.importRealm(rep, realmCreator);
                UriBuilder builder = uriInfo.getRequestUriBuilder().path(realm.getId());
                return Response.created(builder.build())
                        .entity(RealmSubResource.realmRep(realm, uriInfo))
                        .type(MediaType.APPLICATION_JSON_TYPE).build();
            }
        }.call();
    }
}
