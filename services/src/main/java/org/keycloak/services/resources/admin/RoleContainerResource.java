package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleContainerResource extends RoleResource {
    private final RealmModel realm;
    private final RealmAuth auth;
    protected RoleContainerModel roleContainer;

    public RoleContainerResource(RealmModel realm, RealmAuth auth, RoleContainerModel roleContainer) {
        super(realm);
        this.realm = realm;
        this.auth = auth;
        this.roleContainer = roleContainer;
    }

    @Path("")
    @GET
    @NoCache
    @Produces("application/json")
    public List<RoleRepresentation> getRoles() {
        auth.requireAny();

        Set<RoleModel> roleModels = roleContainer.getRoles();
        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roleModels) {
            if (!roleModel.getName().startsWith(Constants.INTERNAL_ROLE)) {
                roles.add(ModelToRepresentation.toRepresentation(roleModel));
            }
        }
        return roles;
    }

    @Path("")
    @POST
    @Consumes("application/json")
    public Response createRole(final @Context UriInfo uriInfo, final RoleRepresentation rep) {
        auth.requireManage();

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

    @Path("{role-name}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("role-name") String roleName) {
        auth.requireView();

        RoleModel roleModel = roleContainer.getRole(roleName);
        if (roleModel == null || roleModel.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        return getRole(roleModel);
    }

    @Path("{role-name}")
    @DELETE
    @NoCache
    public void deleteRole(final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        deleteRole(role);
    }

    @Path("{role-name}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("role-name") String roleName, final RoleRepresentation rep) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        updateRole(rep, role);
    }

    @Path("{role-name}/composites")
    @POST
    @Consumes("application/json")
    public void addComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        addComposites(roles, role);
    }

    @Path("{role-name}/composites")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRoleComposites(final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        return getRoleComposites(role);
    }

    @Path("{role-name}/composites/realm")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRealmRoleComposites(final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        return getRealmRoleComposites(role);
    }

    @Path("{role-name}/composites/application/{app}")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getApplicationRoleComposites(final @PathParam("role-name") String roleName,
                                                                final @PathParam("app") String appName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        return getApplicationRoleComposites(appName, role);
    }


    @Path("{role-name}/composites")
    @DELETE
    @Consumes("application/json")
    public void deleteComposites(final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null || role.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        deleteComposites(roles, role);
    }


}
