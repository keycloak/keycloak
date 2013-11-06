package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleContainerResource {
    protected RoleContainerModel roleContainer;

    public RoleContainerResource(RoleContainerModel roleContainer) {
        this.roleContainer = roleContainer;
    }

    @Path("roles")
    @GET
    @NoCache
    @Produces("application/json")
    public List<RoleRepresentation> getRoles() {
        List<RoleModel> roleModels = roleContainer.getRoles();
        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roleModels) {
            RoleRepresentation role = new RoleRepresentation(roleModel.getName(), roleModel.getDescription());
            role.setId(roleModel.getId());
            roles.add(role);
        }
        return roles;
    }

    @Path("roles/{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("id") String id) {
        RoleModel roleModel = roleContainer.getRoleById(id);
        if (roleModel == null) {
            throw new NotFoundException();
        }
        RoleRepresentation rep = new RoleRepresentation(roleModel.getName(), roleModel.getDescription());
        rep.setId(roleModel.getId());
        return rep;
    }

    @Path("roles/{id}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("id") String id, final RoleRepresentation rep) {
        RoleModel role = roleContainer.getRoleById(id);
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
        if (roleContainer.getRole(rep.getName()) != null) {
            throw new InternalServerErrorException(); // todo appropriate status here.
        }
        RoleModel role = roleContainer.addRole(rep.getName());
        if (role == null) {
            throw new NotFoundException();
        }
        role.setDescription(rep.getDescription());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(role.getId()).build()).build();
    }
}
