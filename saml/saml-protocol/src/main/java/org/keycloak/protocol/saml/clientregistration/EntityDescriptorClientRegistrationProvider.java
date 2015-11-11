package org.keycloak.protocol.saml.clientregistration;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.saml.EntityDescriptorDescriptionConverter;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.clientregistration.ClientRegAuth;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EntityDescriptorClientRegistrationProvider implements ClientRegistrationProvider {

    private static final Logger logger = Logger.getLogger(EntityDescriptorClientRegistrationProvider.class);

    private KeycloakSession session;
    private EventBuilder event;
    private ClientRegAuth auth;

    public EntityDescriptorClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

//    @POST
//    @Consumes(MediaType.APPLICATION_XML)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response create(String descriptor) {
//        event.event(EventType.CLIENT_REGISTER);
//
//        auth.requireCreate();
//
//        ClientRepresentation client = session.getProvider(ClientDescriptionConverter.class, EntityDescriptorDescriptionConverter.ID).convertToInternal(descriptor);
//
//        try {
//            ClientModel clientModel = RepresentationToModel.createClient(session, session.getContext().getRealm(), client, true);
//            client = ModelToRepresentation.toRepresentation(clientModel);
//            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build();
//
//            logger.infov("Created client {0}", client.getClientId());
//
//            event.client(client.getClientId()).success();
//
//            return Response.created(uri).entity(client).build();
//        } catch (ModelDuplicateException e) {
//            return ErrorResponse.exists("Client " + client.getClientId() + " already exists");
//        }
//    }

    @Override
    public void close() {
    }

    @Override
    public void setAuth(ClientRegAuth auth) {
        this.auth = auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

}
