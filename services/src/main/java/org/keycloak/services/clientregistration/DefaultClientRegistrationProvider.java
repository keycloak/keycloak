package org.keycloak.services.clientregistration;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultClientRegistrationProvider implements ClientRegistrationProvider {

    private KeycloakSession session;
    private EventBuilder event;
    private ClientRegistrationAuth auth;

    public DefaultClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(ClientRepresentation client) {
        event.event(EventType.CLIENT_REGISTER);

        auth.requireCreate();

        try {
            ClientModel clientModel = RepresentationToModel.createClient(session, session.getContext().getRealm(), client, true);
            client = ModelToRepresentation.toRepresentation(clientModel);

            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, clientModel);

            client.setRegistrationAccessToken(registrationAccessToken);

            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build();

            if (auth.isInitialAccessToken()) {
                ClientInitialAccessModel initialAccessModel = auth.getInitialAccessModel();
                initialAccessModel.decreaseRemainingCount();
            }

            event.client(client.getClientId()).success();
            return Response.created(uri).entity(client).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + client.getClientId() + " already exists");
        }
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("clientId") String clientId) {
        event.event(EventType.CLIENT_INFO);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireView(client);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client);

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        event.client(client.getClientId()).success();
        return Response.ok(rep).build();
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("clientId") String clientId, ClientRepresentation rep) {
        event.event(EventType.CLIENT_UPDATE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireUpdate(client);

        RepresentationToModel.updateClient(rep, client);
        rep = ModelToRepresentation.toRepresentation(client);

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        event.client(client.getClientId()).success();
        return Response.ok(rep).build();
    }

    @DELETE
    @Path("{clientId}")
    public Response delete(@PathParam("clientId") String clientId) {
        event.event(EventType.CLIENT_DELETE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireUpdate(client);

        if (session.getContext().getRealm().removeClient(client.getId())) {
            event.client(client.getClientId()).success();
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Override
    public void setAuth(ClientRegistrationAuth auth) {
        this.auth = auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public void close() {
    }

}
