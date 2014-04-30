package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
    protected RealmModel realm;
    private RealmAuth auth;

    @Context
    protected KeycloakSession session;

    public ApplicationsResource(RealmModel realm, RealmAuth auth) {
        this.realm = realm;
        this.auth = auth;

        auth.init(RealmAuth.Resource.APPLICATION);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ApplicationRepresentation> getApplications() {
        auth.requireAny();

        List<ApplicationRepresentation> rep = new ArrayList<ApplicationRepresentation>();
        List<ApplicationModel> applicationModels = realm.getApplications();
        ApplicationManager resourceManager = new ApplicationManager(new RealmManager(session));

        boolean view = auth.hasView();
        for (ApplicationModel applicationModel : applicationModels) {
            if (view) {
                rep.add(resourceManager.toRepresentation(applicationModel));
            } else {
                ApplicationRepresentation app = new ApplicationRepresentation();
                app.setName(applicationModel.getName());
                rep.add(app);
            }
        }
        return rep;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(final @Context UriInfo uriInfo, final ApplicationRepresentation rep) {
        auth.requireManage();

        ApplicationManager resourceManager = new ApplicationManager(new RealmManager(session));
        try {
            ApplicationModel applicationModel = resourceManager.createApplication(realm, rep);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(applicationModel.getName()).build()).build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Application " + rep.getName() + " already exists");
        }
    }

    @Path("{app-name}")
    public ApplicationResource getApplication(final @PathParam("app-name") String name) {
        ApplicationModel applicationModel = realm.getApplicationByName(name);
        if (applicationModel == null) {
            throw new NotFoundException("Could not find application: " + name);
        }
        ApplicationResource applicationResource = new ApplicationResource(realm, auth, applicationModel, session);
        ResteasyProviderFactory.getInstance().injectProperties(applicationResource);
        //resourceContext.initResource(applicationResource);
        return applicationResource;
    }

}
