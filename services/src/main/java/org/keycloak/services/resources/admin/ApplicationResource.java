package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceManager;
import org.keycloak.services.models.*;

import javax.ws.rs.*;
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
public class ApplicationResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;
    protected ApplicationModel application;

    @Context
    protected KeycloakSession session;

    public ApplicationResource(UserModel admin, RealmModel realm, ApplicationModel applicationModel) {
        this.admin = admin;
        this.realm = realm;
        this.application = applicationModel;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(final ApplicationRepresentation rep) {
        ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
        resourceManager.updateResource(rep, application);
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationRepresentation getResource(final @PathParam("id") String id) {
        ResourceManager resourceManager = new ResourceManager(new RealmManager(session));
        return resourceManager.toRepresentation(application);
    }

    @Path("roles")
    @GET
    @NoCache
    @Produces("application/json")
    public List<RoleRepresentation> getRoles() {
        List<RoleModel> roleModels = application.getRoles();
        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roleModels) {
            roles.add(RealmManager.toRepresentation(roleModel));
        }
        return roles;
    }

    @Path("roles/{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("id") String id) {
        RoleModel roleModel = application.getRoleById(id);
        if (roleModel == null) {
            throw new NotFoundException();
        }
        return RealmManager.toRepresentation(roleModel);
    }


    @Path("roles/{id}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("id") String id, final RoleRepresentation rep) {
        RoleModel role = application.getRoleById(id);
        if (role == null) {
            throw new NotFoundException();
        }
        role.setName(rep.getName());
        role.setDescription(rep.getDescription());
    }

    @Path("roles")
    @POST
    @Consumes("application/json")
    public Response createRole(final @Context UriInfo uriInfo, final RoleRepresentation rep) {
        if (application.getRole(rep.getName()) != null) { // no duplicates
            throw new InternalServerErrorException(); // todo appropriate status here.
        }
        RoleModel role = application.addRole(rep.getName());
        if (role == null) {
            throw new NotFoundException();
        }
        role.setDescription(rep.getDescription());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(role.getId()).build()).build();
    }

}
