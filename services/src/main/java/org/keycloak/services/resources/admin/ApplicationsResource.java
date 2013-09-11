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
import javax.ws.rs.container.ResourceContext;
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
public class ApplicationsResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    public ApplicationsResource(UserModel admin, RealmModel realm) {
        this.admin = admin;
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ApplicationRepresentation> getResources() {
        List<ApplicationRepresentation> rep = new ArrayList<ApplicationRepresentation>();
        List<ApplicationModel> applicationModels = realm.getApplications();
        ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
        for (ApplicationModel applicationModel : applicationModels) {
            rep.add(resourceManager.toRepresentation(applicationModel));
        }
        return rep;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final @Context UriInfo uriInfo, final ApplicationRepresentation rep) {
        ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
        ApplicationModel applicationModel = resourceManager.createResource(realm, rep);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(applicationModel.getId()).build()).build();
    }

    @Path("{id}")
    public ApplicationResource getResource(final @PathParam("id") String id) {
        ApplicationModel applicationModel = realm.getApplicationById(id);
        if (applicationModel == null) {
            throw new NotFoundException();
        }
        ApplicationResource applicationResource = new ApplicationResource(admin, realm, applicationModel);
        resourceContext.initResource(applicationResource);
        return applicationResource;
    }

}
