package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Base resource class for managing a realm's clients.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:tom@tutorials.de">Thomas Darimont</a>
 * @version $Revision: 1 $
 */
public class ClientsResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmModel realm;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;
    
    @Context
    protected KeycloakSession session;

    public ClientsResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        
        auth.init(RealmAuth.Resource.CLIENT);
    }

    /**
     * Get clients belonging to the realm
     *
     * Returns a list of clients belonging to the realm
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ClientRepresentation> getClients() {
        auth.requireAny();

        List<ClientRepresentation> rep = new ArrayList<>();
        List<ClientModel> clientModels = realm.getClients();

        boolean view = auth.hasView();
        for (ClientModel clientModel : clientModels) {
            if (view) {
                rep.add(ModelToRepresentation.toRepresentation(clientModel));
            } else {
                ClientRepresentation client = new ClientRepresentation();
                client.setId(clientModel.getId());
                client.setClientId(clientModel.getClientId());
                rep.add(client);
            }
        }
        return rep;
    }

    /**
     * Create a new client
     *
     * Client's client_id must be unique!
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClient(final @Context UriInfo uriInfo, final ClientRepresentation rep) {
        auth.requireManage();

        try {
            ClientModel clientModel = RepresentationToModel.createClient(session, realm, rep, true);
            
            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, clientModel.getId()).representation(rep).success();
            
            return Response.created(uriInfo.getAbsolutePathBuilder().path(clientModel.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        }
    }

    /**
     * Base path for managing a specific client.
     *
     * @param id id of client (not client-id)
     * @return
     */
    @Path("{id}")
    public ClientResource getClient(final @PathParam("id") String id) {

        ClientModel clientModel = realm.getClientById(id);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");
        }

        return selectPreparedClientResourceFor(clientModel);
    }

    /**
     * Base path for managing a specific client by client-id.
     *
     * @param clientId  client-id of client
     * @return
     */
    @Path("/by-client-id/{clientId}")
    public ClientResource getClientByClientId(final @PathParam("clientId") String clientId){

        ClientModel clientModel = realm.getClientByClientId(clientId);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");
        }

        return selectPreparedClientResourceFor(clientModel);
    }

    private ClientResource selectPreparedClientResourceFor(ClientModel clientModel) {

        session.getContext().setClient(clientModel);

        ClientResource clientResource = new ClientResource(realm, auth, clientModel, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientResource);
        return clientResource;
    }
}
