package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.admin.OperationType;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappedClientResource {
    protected RealmModel realm;
    private RealmAuth auth;
    protected ClientModel client;
    protected KeycloakSession session;
    protected ClientModel scopedClient;
    protected AdminEventBuilder adminEvent;
    
    public ScopeMappedClientResource(RealmModel realm, RealmAuth auth, ClientModel client, KeycloakSession session, ClientModel scopedClient, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.client = client;
        this.session = session;
        this.scopedClient = scopedClient;
        this.adminEvent = adminEvent;
    }

    /**
     * Get the roles associated with a client's scope for a specific client.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getClientScopeMappings() {
        auth.requireView();

        Set<RoleModel> mappings = scopedClient.getClientScopeMappings(client);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    /**
     * The available client-level roles that can be associated with the client's scope
     *
     * @return
     */
    @Path("available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getAvailableClientScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = scopedClient.getRoles();
        return ScopeMappedResource.getAvailable(client, roles);
    }

    /**
     * Get effective client roles that are associated with the client's scope for a specific client.
     *
     * @return
     */
    @Path("composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getCompositeClientScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = scopedClient.getRoles();
        return ScopeMappedResource.getComposite(client, roles);
    }

    /**
     * Add client-level roles to the client's scope
     *
     * @param roles
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addClientScopeMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = scopedClient.getRole(role.getName());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            client.addScopeMapping(roleModel);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), roleModel.getId()).representation(roles).success();
        }
    }

    /**
     * Remove client-level roles from the client's scope.
     *
     * @param roles
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteClientScopeMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = scopedClient.getClientScopeMappings(client);
            for (RoleModel roleModel : roleModels) {
                client.deleteScopeMapping(roleModel);
            }
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(roles).success();
        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = scopedClient.getRole(role.getName());
                if (roleModel == null) {
                    throw new NotFoundException("Role not found");
                }
                client.deleteScopeMapping(roleModel);
                adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri(), roleModel.getId()).representation(roles).success();
            }
        }
    }
}
