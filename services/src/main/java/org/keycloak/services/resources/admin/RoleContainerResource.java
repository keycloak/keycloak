package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.Constants;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<RoleModel> roleModels = roleContainer.getRoles();
        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roleModels) {
            if (!roleModel.getName().startsWith(Constants.INTERNAL_ROLE)) {
                roles.add(ModelToRepresentation.toRepresentation(roleModel));
            }
        }
        return roles;
    }

    @Path("roles/{role-name}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("role-name") String roleName) {
        RoleModel roleModel = roleContainer.getRole(roleName);
        if (roleModel == null || roleModel.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        return ModelToRepresentation.toRepresentation(roleModel);
    }

    @Path("roles/{role-name}")
    @DELETE
    @NoCache
    public void deleteRole(final @PathParam("role-name") String roleName) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        if (!roleContainer.removeRoleById(role.getId())) {
            throw new NotFoundException();
        }
    }

    @Path("roles/{role-name}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("role-name") String roleName, final RoleRepresentation rep) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        role.setName(rep.getName());
        role.setDescription(rep.getDescription());
        role.setComposite(rep.isComposite());
    }

    @Path("roles/{role-name}/composites")
    @POST
    @Consumes("application/json")
    public void addComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        for (RoleRepresentation rep : roles) {
            RoleModel composite = roleContainer.getRole(rep.getName());
            if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
                throw new NotFoundException("Could not find composite role: " + rep.getName());
            }
            if (!role.isComposite()) role.setComposite(true);
            role.addCompositeRole(composite);
        }
    }

    @Path("roles/{role-name}/composites")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRoleComposites(final @PathParam("role-name") String roleName) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        if (!role.isComposite() || role.getComposites().size() == 0) return Collections.emptySet();

        Set<RoleRepresentation> composites = new HashSet<RoleRepresentation>(role.getComposites().size());
        for (RoleModel composite : role.getComposites()) {
            composites.add(ModelToRepresentation.toRepresentation(composite));
        }
        return composites;
    }


    @Path("roles/{role-name}/composites")
    @DELETE
    @Consumes("application/json")
    public void deleteComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        for (RoleRepresentation rep : roles) {
            RoleModel composite = roleContainer.getRole(rep.getName());
            if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
                throw new NotFoundException("Could not find composite role: " + rep.getName());
            }
            role.removeCompositeRole(composite);
        }
        if (role.getComposites().size() > 0) role.setComposite(false);
    }


    @Path("roles")
    @POST
    @Consumes("application/json")
    public Response createRole(final @Context UriInfo uriInfo, final RoleRepresentation rep) {
        if (roleContainer.getRole(rep.getName()) != null || rep.getName().startsWith(Constants.INTERNAL_ROLE)) {
            return Flows.errors().exists("Role with name " + rep.getName() + " already exists");
        }
        RoleModel role = roleContainer.addRole(rep.getName());
        if (role == null) {
            throw new NotFoundException();
        }
        role.setDescription(rep.getDescription());
        role.setComposite(rep.isComposite());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(role.getName()).build()).build();
    }
}
