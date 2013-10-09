package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource extends RoleContainerResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    public RealmAdminResource(UserModel admin, RealmModel realm) {
        super(realm);
        this.admin = admin;
        this.realm = realm;
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
        return oauth;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public RealmRepresentation getRealm() {
        return RealmManager.toRepresentation(realm);
    }


    @PUT
    @Consumes("application/json")
    public void updateRealm(final RealmRepresentation rep) {
        logger.info("updating realm: " + realm.getName());
        new RealmManager(session).updateRealm(rep, realm);
    }


    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm);
        resourceContext.initResource(users);
        return users;
    }



}
