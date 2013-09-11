package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceManager;
import org.keycloak.services.models.ApplicationModel;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;
    protected ApplicationModel applicationModel;

    @Context
    protected KeycloakSession session;

    public ApplicationResource(UserModel admin, RealmModel realm, ApplicationModel applicationModel) {
        this.admin = admin;
        this.realm = realm;
        this.applicationModel = applicationModel;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(final ApplicationRepresentation rep) {
        ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
        resourceManager.updateResource(rep, applicationModel);
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationRepresentation getResource(final @PathParam("id") String id) {
        ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
        return resourceManager.toRepresentation(applicationModel);
    }
}
