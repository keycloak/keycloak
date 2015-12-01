package org.keycloak.services.clientregistration.oidc;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;
import org.keycloak.services.clientregistration.ClientRegistrationAuth;
import org.keycloak.services.clientregistration.ClientRegistrationException;
import org.keycloak.services.clientregistration.ErrorCodes;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationProvider extends AbstractClientRegistrationProvider {

    private static final Logger log = Logger.getLogger(OIDCClientRegistrationProvider.class);

    public OIDCClientRegistrationProvider(KeycloakSession session) {
        super(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOIDC(OIDCClientRepresentation clientOIDC) {
        if (clientOIDC.getClientId() != null) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier included", Response.Status.BAD_REQUEST);
        }

        try {
            ClientRepresentation client = DescriptionConverter.toInternal(clientOIDC);
            client = create(client);
            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
            clientOIDC = DescriptionConverter.toExternalResponse(client, uri);
            clientOIDC.setClientIdIssuedAt(Time.currentTime());
            return Response.created(uri).entity(clientOIDC).build();
        } catch (ClientRegistrationException cre) {
            log.error(cre.getMessage());
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client metadata invalid", Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOIDC(@PathParam("clientId") String clientId) {
        ClientRepresentation client = get(clientId);
        OIDCClientRepresentation clientOIDC = DescriptionConverter.toExternalResponse(client, session.getContext().getUri().getRequestUri());
        return Response.ok(clientOIDC).build();
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOIDC(@PathParam("clientId") String clientId, OIDCClientRepresentation clientOIDC) {
        try {
            ClientRepresentation client = DescriptionConverter.toInternal(clientOIDC);
            client = update(clientId, client);
            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
            clientOIDC = DescriptionConverter.toExternalResponse(client, uri);
            return Response.ok(clientOIDC).build();
        } catch (ClientRegistrationException cre) {
            log.error(cre.getMessage());
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client metadata invalid", Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Path("{clientId}")
    public void deleteOIDC(@PathParam("clientId") String clientId) {
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
