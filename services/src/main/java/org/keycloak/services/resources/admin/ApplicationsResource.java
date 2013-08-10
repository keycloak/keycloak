package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.ApplicationModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.Transaction;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
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
public class ApplicationsResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;

    public ApplicationsResource(UserModel admin, RealmModel realm) {
        this.admin = admin;
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ApplicationRepresentation> getResources() {
        return new Transaction() {
            @Override
            protected List<ApplicationRepresentation> callImpl() {
                List<ApplicationRepresentation> rep = new ArrayList<ApplicationRepresentation>();
                List<ApplicationModel> applicationModels = realm.getApplications();
                ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
                for (ApplicationModel applicationModel : applicationModels) {
                    rep.add(resourceManager.toRepresentation(applicationModel));
                }
                return rep;
            }
        }.call();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final @Context UriInfo uriInfo, final ApplicationRepresentation rep) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
                ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
                ApplicationModel applicationModel = resourceManager.createResource(realm, rep);
                return Response.created(uriInfo.getAbsolutePathBuilder().path(applicationModel.getId()).build()).build();
            }
        }.call();
    }

    @Path("{id}")
    public ApplicationResource getResource(final @PathParam("id") String id) {
        return new Transaction(false) {
            @Override
            protected ApplicationResource callImpl() {
                ApplicationModel applicationModel = realm.getApplicationById(id);
                if (applicationModel == null) {
                    throw new NotFoundException();
                }
                return new ApplicationResource(admin, realm, applicationModel);
            }
        }.call();

    }

}
