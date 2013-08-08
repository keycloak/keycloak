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
public class RealmResourcesResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;

    public RealmResourcesResource(UserModel admin, RealmModel realm) {
        this.admin = admin;
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ResourceRepresentation> getResources() {
        return new Transaction() {
            @Override
            protected List<ResourceRepresentation> callImpl() {
                List<ResourceRepresentation> rep = new ArrayList<ResourceRepresentation>();
                List<ResourceModel> resourceModels = realm.getResources();
                ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
                for (ResourceModel resourceModel : resourceModels) {
                    rep.add(resourceManager.toRepresentation(resourceModel));
                }
                return rep;
            }
        }.call();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final @Context UriInfo uriInfo, final ResourceRepresentation rep) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
                ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
                ResourceModel resourceModel = resourceManager.createResource(realm, rep);
                return Response.created(uriInfo.getAbsolutePathBuilder().path(resourceModel.getId()).build()).build();
            }
        }.call();
    }

    @Path("{id}")
    public RealmResourceResource getResource(final @PathParam("id") String id) {
        return new Transaction(false) {
            @Override
            protected RealmResourceResource callImpl() {
                ResourceModel resourceModel = realm.getResourceById(id);
                if (resourceModel == null) {
                    throw new NotFoundException();
                }
                return new RealmResourceResource(admin, realm, resourceModel);
            }
        }.call();

    }

}
