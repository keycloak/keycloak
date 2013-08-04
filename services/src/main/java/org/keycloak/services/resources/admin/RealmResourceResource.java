package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.ResourceRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.Transaction;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmResourceResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;
    protected ResourceModel resourceModel;

    public RealmResourceResource(UserModel admin, RealmModel realm, ResourceModel resourceModel) {
        this.admin = admin;
        this.realm = realm;
        this.resourceModel = resourceModel;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(final ResourceRepresentation rep) {
        new Transaction() {
            @Override
            protected void runImpl() {
                ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
                resourceManager.updateResource(rep, resourceModel);
            }
        }.run();
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceRepresentation getResource(final @PathParam("id") String id) {
        return new Transaction() {
            @Override
            protected ResourceRepresentation callImpl() {
               ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
                return resourceManager.toRepresentation(resourceModel);
            }
        }.call();
    }
}
