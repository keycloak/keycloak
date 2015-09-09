package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

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
    private AdminEventBuilder adminEvent;

    @Context
    private KeycloakSession session;

    @Context
    private UriInfo uriInfo;

    public RoleByIdResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        super(realm);

        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    /**
     * Get a specific role's representation
     *
     * @param id id of role
     * @return
     */
    @Path("{role-id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RoleRepresentation getRole(final @PathParam("role-id") String id) {
        RoleModel roleModel = getRoleModel(id);
        auth.requireView();
        return getRole(roleModel);
    }

    protected RoleModel getRoleModel(String id) {
        RoleModel roleModel = realm.getRoleById(id);
        if (roleModel == null) {
            throw new NotFoundException("Could not find role with id");
        }

        RealmAuth.Resource r = null;
        if (roleModel.getContainer() instanceof RealmModel) {
            r = RealmAuth.Resource.REALM;
        } else if (roleModel.getContainer() instanceof ClientModel) {
            r = RealmAuth.Resource.CLIENT;
        } else if (roleModel.getContainer() instanceof UserModel) {
            r = RealmAuth.Resource.USER;
        }
        auth.init(r);
        return roleModel;
    }

    /**
     * Delete this role
     *
     * @param id id of role
     */
    @Path("{role-id}")
    @DELETE
    @NoCache
    public void deleteRole(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        deleteRole(role);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }

    /**
     * Update this role
     *
     * @param id id of role
     * @param rep
     */
    @Path("{role-id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRole(final @PathParam("role-id") String id, final RoleRepresentation rep) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        updateRole(rep, role);
        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();
    }

    /**
     * Make this role a composite role by associating some child roles to it.
     *
     * @param id
     * @param roles
     */
    @Path("{role-id}/composites")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        addComposites(adminEvent, uriInfo, roles, role);
    }

    /**
     * If this role is a composite, return a set of its children
     *
     * @param id
     * @return
     */
    @Path("{role-id}/composites")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRoleComposites(final @PathParam("role-id") String id) {

        if (logger.isDebugEnabled()) logger.debug("*** getRoleComposites: '" + id + "'");
        RoleModel role = getRoleModel(id);
        auth.requireView();
        return getRoleComposites(role);
    }

    /**
     * Return a set of realm-level roles that are in the role's composite
     *
     * @param id
     * @return
     */
    @Path("{role-id}/composites/realm")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRealmRoleComposites(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        auth.requireView();
        return getRealmRoleComposites(role);
    }

    /**
     * Return a set of client-level roles for a specific client that are in the role's composite
     *
     * @param id
     * @param client
     * @return
     */
    @Path("{role-id}/composites/clients/{client}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getClientRoleComposites(final @PathParam("role-id") String id,
                                                                final @PathParam("client") String client) {
        RoleModel role = getRoleModel(id);
        auth.requireView();
        ClientModel clientModel = realm.getClientById(client);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");
        }
        return getClientRoleComposites(clientModel, role);
    }

    /**
     * Return a set of client-level roles for a specific client that are in the role's composite
     *
     * @param role
     * @param client
     * @return
     */
    @Path("{role-id}/composites/clients/{client}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getClientByIdRoleComposites(final @PathParam("role-id") String role,
                                                                final @PathParam("client") String client) {
        RoleModel roleModel = getRoleModel(role);
        auth.requireView();
        ClientModel clientModel = realm.getClientById(client);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");

        }
        return getClientRoleComposites(clientModel, roleModel);
    }

    /**
     * Remove the listed set of roles from this role's composite
     *
     * @param id
     * @param roles
     */
    @Path("{role-id}/composites")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles) {
        RoleModel role = getRoleModel(id);
        auth.requireManage();
        deleteComposites(roles, role);
        
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).representation(roles).success();
    }

}
