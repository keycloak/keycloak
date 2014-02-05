package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource extends RoleContainerResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;
    private TokenManager tokenManager;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    public RealmAdminResource(UserModel admin, RealmModel realm, TokenManager tokenManager) {
        super(realm, realm);
        this.admin = admin;
        this.realm = realm;
        this.tokenManager = tokenManager;
    }

    @Path("applications")
    public ApplicationsResource getApplications() {
        ApplicationsResource applicationsResource = new ApplicationsResource(realm);
        resourceContext.initResource(applicationsResource);
        return applicationsResource;
    }

    @Path("oauth-clients")
    public OAuthClientsResource getOAuthClients() {
        OAuthClientsResource oauth = new OAuthClientsResource(realm, session);
        resourceContext.initResource(oauth);
        return oauth;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public RealmRepresentation getRealm() {
        return ModelToRepresentation.toRepresentation(realm);
    }


    @PUT
    @Consumes("application/json")
    public void updateRealm(final RealmRepresentation rep) {
        logger.debug("updating realm: " + realm.getName());
        new RealmManager(session).updateRealm(rep, realm);
    }

    @DELETE
    public void deleteRealms() {
        if (!session.removeRealm(realm.getId())) {
            throw new NotFoundException();
        }
    }

    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm, tokenManager);
        resourceContext.initResource(users);
        return users;
    }

    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
        RoleByIdResource resource = new RoleByIdResource(realm);
        resourceContext.initResource(resource);
        return resource;

    }



}
