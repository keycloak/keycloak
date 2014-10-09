package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappedApplicationResource {
    protected RealmModel realm;
    private RealmAuth auth;
    protected ClientModel client;
    protected KeycloakSession session;
    protected ApplicationModel app;

    public ScopeMappedApplicationResource(RealmModel realm, RealmAuth auth, ClientModel client, KeycloakSession session, ApplicationModel app) {
        this.realm = realm;
        this.auth = auth;
        this.client = client;
        this.session = session;
        this.app = app;
    }

    /**
     * Get the roles associated with a client's scope for a specific application.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getApplicationScopeMappings() {
        auth.requireView();

        Set<RoleModel> mappings = app.getApplicationScopeMappings(client);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    /**
     * The available application-level roles that can be associated with the client's scope
     *
     * @return
     */
    @Path("available")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getAvailableApplicationScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = app.getRoles();
        return ScopeMappedResource.getAvailable(client, roles);
    }

    /**
     * Get effective application roles that are associated with the client's scope for a specific application.
     *
     * @return
     */
    @Path("composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getCompositeApplicationScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = app.getRoles();
        return ScopeMappedResource.getComposite(client, roles);
    }

    /**
     * Add application-level roles to the client's scope
     *
     * @param roles
     */
    @POST
    @Consumes("application/json")
    public void addApplicationScopeMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = app.getRole(role.getName());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            client.addScopeMapping(roleModel);
        }

    }

    /**
     * Remove application-level roles from the client's scope.
     *
     * @param roles
     */
    @DELETE
    @Consumes("application/json")
    public void deleteApplicationScopeMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = app.getApplicationScopeMappings(client);
            for (RoleModel roleModel : roleModels) {
                client.deleteScopeMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = app.getRole(role.getName());
                if (roleModel == null) {
                    throw new NotFoundException("Role not found");
                }
                client.deleteScopeMapping(roleModel);
            }
        }
    }
}
