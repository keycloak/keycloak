package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
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
public class RoleContainerResource extends RoleResource {
    protected RoleContainerModel roleContainer;

    public RoleContainerResource(RealmModel realm, RoleContainerModel roleContainer) {
        super(realm);
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
        return Response.created(uriInfo.getAbsolutePathBuilder().path(role.getName()).build()).build();
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
        return getRole(roleModel);
    }

    @Path("roles/{role-name}")
    @DELETE
    @NoCache
    public void deleteRole(final @PathParam("role-name") String roleName) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        deleteRole(role);
    }

    @Path("roles/{role-name}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("role-name") String roleName, final RoleRepresentation rep) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        updateRole(rep, role);
    }

    @Path("roles/{role-name}/composites")
    @POST
    @Consumes("application/json")
    public void addComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        addComposites(roles, role);
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
        return getRoleComposites(role);
    }

    @Path("roles/{role-name}/composites/realm")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRealmRoleComposites(final @PathParam("role-name") String roleName) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        return getRealmRoleComposites(role);
    }

    @Path("roles/{role-name}/composites/application/{app}")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getApplicationRoleComposites(final @PathParam("role-name") String roleName,
                                                                final @PathParam("app") String appName) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        return getApplicationRoleComposites(appName, role);
    }


    @Path("roles/{role-name}/composites")
    @DELETE
    @Consumes("application/json")
    public void deleteComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        deleteComposites(roles, role);
    }


}
