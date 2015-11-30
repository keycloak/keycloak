package org.keycloak.services.clientregistration;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultClientRegistrationProvider extends AbstractClientRegistrationProvider {

    public DefaultClientRegistrationProvider(KeycloakSession session) {
        super(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDefault(ClientRepresentation client) {
        client = create(client);
        URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
        return Response.created(uri).entity(client).build();
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefault(@PathParam("clientId") String clientId) {
        ClientRepresentation client = get(clientId);
        return Response.ok(client).build();
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDefault(@PathParam("clientId") String clientId, ClientRepresentation client) {
        client = update(clientId, client);
        return Response.ok(client).build();
    }

    @DELETE
    @Path("{clientId}")
    public void deleteDefault(@PathParam("clientId") String clientId) {
        delete(clientId);
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
