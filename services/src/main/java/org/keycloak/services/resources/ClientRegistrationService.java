package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.EventBuilder;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.KeycloakClientDescriptionConverter;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationService {

    protected static final Logger logger = Logger.getLogger(ClientRegistrationService.class);

    private RealmModel realm;

    private EventBuilder event;

    @Context
    private KeycloakSession session;

    public ClientRegistrationService(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    public Response create(String description, @QueryParam("format") String format) {
        if (format == null) {
            format = KeycloakClientDescriptionConverter.ID;
        }

        ClientDescriptionConverter converter = session.getProvider(ClientDescriptionConverter.class, format);
        if (converter == null) {
            throw new BadRequestException("Invalid format");
        }
        ClientRepresentation rep = converter.convertToInternal(description);

        try {
            ClientModel clientModel = RepresentationToModel.createClient(session, realm, rep, true);
            rep = ModelToRepresentation.toRepresentation(clientModel);
            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build();
            return Response.created(uri).entity(rep).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        }
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation get(@PathParam("clientId") String clientId) {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, realm);
        ClientModel client = clientAuth.getClient();
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        return ModelToRepresentation.toRepresentation(client);
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("clientId") String clientId, ClientRepresentation rep) {
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        RepresentationToModel.updateClient(rep, client);
    }

    @DELETE
    @Path("{clientId}")
    public void delete(@PathParam("clientId") String clientId) {
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        realm.removeClient(client.getId());
    }

}
