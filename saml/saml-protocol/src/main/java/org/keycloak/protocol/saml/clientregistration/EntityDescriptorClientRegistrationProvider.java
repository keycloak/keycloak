package org.keycloak.protocol.saml.clientregistration;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientregistration.ClientRegistrationAuth;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EntityDescriptorClientRegistrationProvider implements ClientRegistrationProvider {

    private static final Logger logger = Logger.getLogger(EntityDescriptorClientRegistrationProvider.class);

    private KeycloakSession session;
    private EventBuilder event;
    private ClientRegistrationAuth auth;

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
    public void setAuth(ClientRegistrationAuth auth) {
        this.auth = auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

}
