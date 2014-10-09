package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.ClientConnection;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserApplicationRoleMappingsResource {
    protected static final Logger logger = Logger.getLogger(UserApplicationRoleMappingsResource.class);

    protected RealmModel realm;
    protected RealmAuth auth;
    protected UserModel user;
    protected ApplicationModel application;

    public UserApplicationRoleMappingsResource(RealmModel realm, RealmAuth auth, UserModel user, ApplicationModel application) {
        this.realm = realm;
        this.auth = auth;
        this.user = user;
        this.application = application;
    }

    /**
     * Get application-level role mappings for this user for a specific app
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getApplicationRoleMappings() {
        auth.requireView();

        logger.debug("getApplicationRoleMappings");

        Set<RoleModel> mappings = user.getApplicationRoleMappings(application);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        logger.debugv("getApplicationRoleMappings.size() = {0}", mapRep.size());
        return mapRep;
    }

    /**
     * Get effective application-level role mappings.  This recurses any composite roles
     *
     * @return
     */
    @Path("composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getCompositeApplicationRoleMappings() {
        auth.requireView();

        logger.debug("getCompositeApplicationRoleMappings");

        Set<RoleModel> roles = application.getRoles();
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (user.hasRole(roleModel)) mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        logger.debugv("getCompositeApplicationRoleMappings.size() = {0}", mapRep.size());
        return mapRep;
    }

    /**
     * Get available application-level roles that can be mapped to the user
     *
     * @return
     */
    @Path("available")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getAvailableApplicationRoleMappings() {
        auth.requireView();

        Set<RoleModel> available = application.getRoles();
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
     * Add application-level roles to the user role mapping.
     *
      * @param roles
     */
    @POST
    @Consumes("application/json")
    public void addApplicationRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debug("addApplicationRoleMapping");
        for (RoleRepresentation role : roles) {
            RoleModel roleModel = application.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            user.grantRole(roleModel);
        }

    }

    /**
     * Delete application-level roles from user role mapping.
     *
     * @param roles
     */
    @DELETE
    @Consumes("application/json")
    public void deleteApplicationRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = user.getApplicationRoleMappings(application);
            for (RoleModel roleModel : roleModels) {
                if (!(roleModel.getContainer() instanceof ApplicationModel)) {
                    ApplicationModel app = (ApplicationModel) roleModel.getContainer();
                    if (!app.getId().equals(application.getId())) continue;
                }
                user.deleteRoleMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = application.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                user.deleteRoleMapping(roleModel);
            }
        }
    }
}
