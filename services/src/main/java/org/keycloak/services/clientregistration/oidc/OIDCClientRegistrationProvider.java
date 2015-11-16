package org.keycloak.services.clientregistration.oidc;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCClientDescriptionConverter;
import org.keycloak.protocol.oidc.representations.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.clientregistration.ClientRegAuth;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.TokenGenerator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationProvider implements ClientRegistrationProvider {

    private static final Logger logger = Logger.getLogger(OIDCClientRegistrationProvider.class);

    private KeycloakSession session;
    private EventBuilder event;
    private ClientRegAuth auth;

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
//            clientModel.setRegistrationSecret(registrationAccessToken);
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
    public void setAuth(ClientRegAuth auth) {
        this.auth = auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

}
