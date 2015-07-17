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
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserClientRoleMappingsResource {
    protected static final Logger logger = Logger.getLogger(UserClientRoleMappingsResource.class);

    protected RealmModel realm;
    protected RealmAuth auth;
    protected UserModel user;
    protected ClientModel client;
    protected AdminEventBuilder adminEvent;
    private UriInfo uriInfo;

    public UserClientRoleMappingsResource(UriInfo uriInfo, RealmModel realm, RealmAuth auth, UserModel user, ClientModel client, AdminEventBuilder adminEvent) {
        this.uriInfo = uriInfo;
        this.realm = realm;
        this.auth = auth;
        this.user = user;
        this.client = client;
        this.adminEvent = adminEvent;
    }

    /**
     * Get client-level role mappings for this user for a specific app
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getClientRoleMappings() {
        auth.requireView();

        Set<RoleModel> mappings = user.getClientRoleMappings(client);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    /**
     * Get effective client-level role mappings.  This recurses any composite roles
     *
     * @return
     */
    @Path("composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getCompositeClientRoleMappings() {
        auth.requireView();

        Set<RoleModel> roles = client.getRoles();
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (user.hasRole(roleModel)) mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    /**
     * Get available client-level roles that can be mapped to the user
     *
     * @return
     */
    @Path("available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getAvailableClientRoleMappings() {
        auth.requireView();

        Set<RoleModel> available = client.getRoles();
        return getAvailableRoles(user, available);
    }

    public static List<RoleRepresentation> getAvailableRoles(UserModel user, Set<RoleModel> available) {
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel roleModel : available) {
            if (user.hasRole(roleModel)) continue;
            roles.add(roleModel);
        }

        List<RoleRepresentation> mappings = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            mappings.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mappings;
    }

    /**
     * Add client-level roles to the user role mapping.
     *
      * @param roles
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addClientRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = client.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            user.grantRole(roleModel);
        }
        adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo).representation(roles).success();

    }

    /**
     * Delete client-level roles from user role mapping.
     *
     * @param roles
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteClientRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = user.getClientRoleMappings(client);
            for (RoleModel roleModel : roleModels) {
                if (!(roleModel.getContainer() instanceof ClientModel)) {
                    ClientModel client = (ClientModel) roleModel.getContainer();
                    if (!client.getId().equals(this.client.getId())) continue;
                }
                user.deleteRoleMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = client.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                user.deleteRoleMapping(roleModel);
            }
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).representation(roles).success();
    }
}
