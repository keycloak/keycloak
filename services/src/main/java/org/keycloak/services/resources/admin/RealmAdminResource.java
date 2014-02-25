package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.Auth;
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
    protected Auth auth;
    protected RealmModel realm;
    private TokenManager tokenManager;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    public RealmAdminResource(Auth auth, RealmModel realm, TokenManager tokenManager) {
        super(realm, realm);
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;
    }

    @Path("applications")
    public ApplicationsResource getApplications() {
        auth.require(AdminRoles.getAdminApp(realm), AdminRoles.MANAGE_APPLICATIONS);

        ApplicationsResource applicationsResource = new ApplicationsResource(realm);
        resourceContext.initResource(applicationsResource);
        return applicationsResource;
    }

    @Path("oauth-clients")
    public OAuthClientsResource getOAuthClients() {
        auth.require(AdminRoles.getAdminApp(realm), AdminRoles.MANAGE_CLIENTS);

        OAuthClientsResource oauth = new OAuthClientsResource(realm, session);
        resourceContext.initResource(oauth);
        return oauth;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public RealmRepresentation getRealm() {
        String realmAdminApp = AdminRoles.getAdminApp(realm);
        if (auth.has(realmAdminApp, AdminRoles.MANAGE_REALM)) {
            return ModelToRepresentation.toRepresentation(realm);
        } else {
            auth.requireOneOf(AdminRoles.getAdminApp(realm), AdminRoles.ALL_REALM_ROLES);

            RealmRepresentation rep = new RealmRepresentation();
            rep.setId(realm.getId());
            rep.setRealm(realm.getName());

            return rep;
        }
    }

    @PUT
    @Consumes("application/json")
    public void updateRealm(final RealmRepresentation rep) {
        auth.require(AdminRoles.getAdminApp(realm), AdminRoles.MANAGE_REALM);

        logger.debug("updating realm: " + realm.getName());
        new RealmManager(session).updateRealm(rep, realm);
    }

    @DELETE
    public void deleteRealms() {
        auth.require(AdminRoles.getAdminApp(realm), AdminRoles.MANAGE_REALM);

        if (!new RealmManager(session).removeRealm(realm)) {
            throw new NotFoundException();
        }
    }

    @Path("users")
    public UsersResource users() {
        auth.require(AdminRoles.getAdminApp(realm), AdminRoles.MANAGE_USERS);

        UsersResource users = new UsersResource(realm, tokenManager);
        resourceContext.initResource(users);
        return users;
    }

    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
        auth.require(AdminRoles.getAdminApp(realm), AdminRoles.MANAGE_REALM);

        RoleByIdResource resource = new RoleByIdResource(realm);
        resourceContext.initResource(resource);
        return resource;
    }

}
