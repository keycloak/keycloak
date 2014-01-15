package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
    protected RealmModel realm;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    public ApplicationsResource(RealmModel realm) {
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ApplicationRepresentation> getApplications() {
        List<ApplicationRepresentation> rep = new ArrayList<ApplicationRepresentation>();
        List<ApplicationModel> applicationModels = realm.getApplications();
        ApplicationManager resourceManager = new ApplicationManager(new RealmManager(session));
        for (ApplicationModel applicationModel : applicationModels) {
            rep.add(resourceManager.toRepresentation(applicationModel));
        }
        return rep;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(final @Context UriInfo uriInfo, final ApplicationRepresentation rep) {
        if (realm.getApplicationNameMap().containsKey(rep.getName())) {
            return Flows.errors().exists("Application " + rep.getName() + " already exists");
        }
        ApplicationManager resourceManager = new ApplicationManager(new RealmManager(session));
        ApplicationModel applicationModel = resourceManager.createApplication(realm, rep);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(applicationModel.getName()).build()).build();
    }

    @Path("{app-name}")
    public ApplicationResource getApplication(final @PathParam("app-name") String name) {
        ApplicationModel applicationModel = realm.getApplicationByName(name);
        if (applicationModel == null) {
            throw new NotFoundException();
        }
        ApplicationResource applicationResource = new ApplicationResource(realm, applicationModel, session);
        resourceContext.initResource(applicationResource);
        return applicationResource;
    }

}
