package org.keycloak.services.clientregistration.oidc;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientregistration.ClientRegistrationAuth;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationProvider implements ClientRegistrationProvider {

    private static final Logger logger = Logger.getLogger(OIDCClientRegistrationProvider.class);

    private KeycloakSession session;
    private EventBuilder event;
    private ClientRegistrationAuth auth;

    public OIDCClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response create(OIDCClientRepresentation clientOIDC) {
//        event.event(EventType.CLIENT_REGISTER);
//
//        auth.requireCreate();
//
//        ClientRepresentation client = DescriptionConverter.toInternal(clientOIDC);
//
//        try {
//            ClientModel clientModel = RepresentationToModel.createClient(session, session.getContext().getRealm(), client, true);
//
//            client = ModelToRepresentation.toRepresentation(clientModel);
//
//            String registrationAccessToken = TokenGenerator.createRegistrationAccessToken();
//
//            clientModel.setRegistrationToken(registrationAccessToken);
//
//            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build();
//
//            logger.infov("Created client {0}", client.getClientId());
//
//            event.client(client.getClientId()).success();
//
//            OIDCClientResponseRepresentation response = DescriptionConverter.toExternalResponse(client);
//
//            response.setClientName(client.getName());
//            response.setClientUri(client.getBaseUrl());
//
//            response.setClientSecret(client.getSecret());
//            response.setClientSecretExpiresAt(0);
//
//            response.setRedirectUris(client.getRedirectUris());
//
//            response.setRegistrationAccessToken(registrationAccessToken);
//            response.setRegistrationClientUri(uri.toString());
//
//            return Response.created(uri).entity(response).build();
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
