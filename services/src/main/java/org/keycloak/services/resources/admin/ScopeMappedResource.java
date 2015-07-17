package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for managing the scope mappings of a specific client.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappedResource {
    protected RealmModel realm;
    private RealmAuth auth;
    protected ClientModel client;
    protected KeycloakSession session;
    protected AdminEventBuilder adminEvent;

    public ScopeMappedResource(RealmModel realm, RealmAuth auth, ClientModel client, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.client = client;
        this.session = session;
        this.adminEvent = adminEvent;
    }

    /**
     * Get all scope mappings for this client
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public MappingsRepresentation getScopeMappings() {
        auth.requireView();

        MappingsRepresentation all = new MappingsRepresentation();
        Set<RoleModel> realmMappings = client.getRealmScopeMappings();
        if (realmMappings.size() > 0) {
            List<RoleRepresentation> realmRep = new ArrayList<RoleRepresentation>();
            for (RoleModel roleModel : realmMappings) {
                realmRep.add(ModelToRepresentation.toRepresentation(roleModel));
            }
            all.setRealmMappings(realmRep);
        }

        List<ClientModel> clients = realm.getClients();
        if (clients.size() > 0) {
            Map<String, ClientMappingsRepresentation> clientMappings = new HashMap<String, ClientMappingsRepresentation>();
            for (ClientModel client : clients) {
                Set<RoleModel> roleMappings = client.getClientScopeMappings(this.client);
                if (roleMappings.size() > 0) {
                    ClientMappingsRepresentation mappings = new ClientMappingsRepresentation();
                    mappings.setId(client.getId());
                    mappings.setClient(client.getClientId());
                    List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
                    mappings.setMappings(roles);
                    for (RoleModel role : roleMappings) {
                        roles.add(ModelToRepresentation.toRepresentation(role));
                    }
                    clientMappings.put(client.getClientId(), mappings);
                    all.setClientMappings(clientMappings);
                }
            }
        }
        return all;
    }

    /**
     * Get list of realm-level roles associated with this client's scope.
     *
     * @return
     */
    @Path("realm")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getRealmScopeMappings() {
        auth.requireView();

        Set<RoleModel> realmMappings = client.getRealmScopeMappings();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return realmMappingsRep;
    }

    /**
     * Get list of realm-level roles that are available to attach to this client's scope.
     *
     * @return
     */
    @Path("realm/available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getAvailableRealmScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = realm.getRoles();
        return getAvailable(client, roles);
    }

    public static List<RoleRepresentation> getAvailable(ClientModel client, Set<RoleModel> roles) {
        List<RoleRepresentation> available = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (client.hasScope(roleModel)) continue;
            available.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return available;
    }

    /**
     * Get all effective realm-level roles that are associated with this client's scope.  What this does is recurse
     * any composite roles associated with the client's scope and adds the roles to this lists.  The method is really
     * to show a comprehensive total view of realm-level roles associated with the client.
     *
     * @return
     */
    @Path("realm/composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getCompositeRealmScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = realm.getRoles();
        return getComposite(client, roles);
    }

    public static List<RoleRepresentation> getComposite(ClientModel client, Set<RoleModel> roles) {
        List<RoleRepresentation> composite = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (client.hasScope(roleModel)) composite.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return composite;
    }

    /**
     * Add a set of realm-level roles to the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addRealmScopeMappings(List<RoleRepresentation> roles) {
        auth.requireManage();

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            client.addScopeMapping(roleModel);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), role.getId()).representation(roles).success();
        }
    }

    /**
     * Remove a set of realm-level roles from the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteRealmScopeMappings(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = client.getRealmScopeMappings();
            for (RoleModel roleModel : roleModels) {
                client.deleteScopeMapping(roleModel);
            }
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(roles).success();
       } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException("Client not found");
                }
                client.deleteScopeMapping(roleModel);
                adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri(), roleModel.getId()).representation(roles).success();
            }
        }

    }

    @Path("clients/{client}")
    public ScopeMappedClientResource getClientByIdScopeMappings(@PathParam("client") String client) {
        ClientModel clientModel = realm.getClientById(client);
        if (clientModel == null) {
            throw new NotFoundException("Client not found");
        }
        return new ScopeMappedClientResource(realm, auth, this.client, session, clientModel, adminEvent);
    }
}
