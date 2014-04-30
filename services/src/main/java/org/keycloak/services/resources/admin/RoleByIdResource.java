package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;
import java.util.Set;

/**
 * Sometimes its easier to just interact with roles by their ID instead of container/role-name
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleByIdResource extends RoleResource {
    protected static final Logger logger = Logger.getLogger(RoleByIdResource.class);
    private final RealmModel realm;
    private final RealmAuth auth;

    public RoleByIdResource(RealmModel realm, RealmAuth auth) {
        super(realm);

        this.realm = realm;
        this.auth = auth;
    }

    @Path("{role-id}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("role-id") String id) {
        RoleModel roleModel = getRoleModel(id);
        auth.requireView();
        return getRole(roleModel);
    }

    protected RoleModel getRoleModel(String id) {
        RoleModel roleModel = realm.getRoleById(id);
        if (roleModel == null) {
            throw new NotFoundException("Could not find role with id: " + id);
        }

        RealmAuth.Resource r = null;
        if (roleModel.getContainer() instanceof RealmModel) {
            r = RealmAuth.Resource.REALM;
        } else if (roleModel.getContainer() instanceof ApplicationModel) {
            r = RealmAuth.Resource.APPLICATION;
        } else if (roleModel.getContainer() instanceof OAuthClientModel) {
            r = RealmAuth.Resource.CLIENT;
        } else if (roleModel.getContainer() instanceof UserModel) {
            r = RealmAuth.Resource.USER;
        }
        auth.init(r);

        return roleModel;
    }

    @Path("{role-id}")
    @DELETE
    @NoCache
    public void deleteRole(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        deleteRole(role);
    }

    @Path("{role-id}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("role-id") String id, final RoleRepresentation rep) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        updateRole(rep, role);
    }

    @Path("{role-id}/composites")
    @POST
    @Consumes("application/json")
    public void addComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        addComposites(roles, role);
    }

    @Path("{role-id}/composites")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRoleComposites(final @PathParam("role-id") String id) {

        logger.info("*** getRoleComposites: '" + id + "'");
        RoleModel role = getRoleModel(id);
        auth.requireView();
        return getRoleComposites(role);
    }

    @Path("{role-id}/composites/realm")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getRealmRoleComposites(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        auth.requireView();
        return getRealmRoleComposites(role);
    }

    @Path("{role-id}/composites/applications/{app}")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<RoleRepresentation> getApplicationRoleComposites(final @PathParam("role-id") String id,
                                                                final @PathParam("app") String appName) {
        RoleModel role = getRoleModel(id);
        auth.requireView();
        return getApplicationRoleComposites(appName, role);
    }


    @Path("{role-id}/composites")
    @DELETE
    @Consumes("application/json")
    public void deleteComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        deleteComposites(roles, role);
    }

}
