package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.resources.admin.RoleResource;
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
 * Sometimes its easier to just interact with roles by their ID instead of container/role-name
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleByIdResource extends RoleResource {
    public RoleByIdResource(RealmModel realm) {
        super(realm);
    }

    @Path("{role-id}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("role-id") String id) {
        RoleModel roleModel = getRoleModel(id);
        return getRole(roleModel);
    }

    protected RoleModel getRoleModel(String id) {
        RoleModel roleModel = realm.getRoleById(id);
        if (roleModel == null || roleModel.getName().startsWith(Constants.INTERNAL_ROLE)) {
            throw new NotFoundException("Could not find role with id: " + id);
        }
        return roleModel;
    }

    @Path("{role-id}")
    @DELETE
    @NoCache
    public void deleteRole(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        deleteRole(role);
    }

    @Path("{role-id}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("role-id") String id, final RoleRepresentation rep) {
        RoleModel role = getRoleModel(id);
        updateRole(rep, role);
    }

    @Path("{role-id}/composites")
    @POST
    @Consumes("application/json")
    public void addComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles) {
        RoleModel role = getRoleModel(id);
        addComposites(roles, role);
    }

    @Path("{role-id}/composites")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRoleComposites(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        return getRoleComposites(role);
    }

    @Path("{role-id}/composites/realm")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRealmRoleComposites(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        return getRealmRoleComposites(role);
    }

    @Path("{role-id}/composites/applications/{app}")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getApplicationRoleComposites(final @PathParam("role-id") String id,
                                                                final @PathParam("app") String appName) {
        RoleModel role = getRoleModel(id);
        return getApplicationRoleComposites(appName, role);
    }


    @Path("{role-id}/composites")
    @DELETE
    @Consumes("application/json")
    public void deleteComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles) {
        RoleModel role = getRoleModel(id);
        deleteComposites(roles, role);
    }


}
