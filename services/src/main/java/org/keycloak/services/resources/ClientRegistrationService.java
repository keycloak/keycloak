package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.KeycloakClientDescriptionConverter;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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

    private AppAuthManager authManager = new AppAuthManager();

    public ClientRegistrationService(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    public Response create(String description, @QueryParam("format") String format) {
        event.event(EventType.CLIENT_REGISTER);

        authenticate(true, null);

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

            logger.infov("Created client {0}", rep.getClientId());

            event.client(rep.getClientId()).success();
            return Response.created(uri).entity(rep).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        }
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("clientId") String clientId) {
        event.event(EventType.CLIENT_INFO);

        ClientModel client = authenticate(false, clientId);
        if (client == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(ModelToRepresentation.toRepresentation(client)).build();
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("clientId") String clientId, ClientRepresentation rep) {
        event.event(EventType.CLIENT_UPDATE).client(clientId);

        ClientModel client = authenticate(false, clientId);
        RepresentationToModel.updateClient(rep, client);

        logger.infov("Updated client {0}", rep.getClientId());

        event.success();
        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("{clientId}")
    public Response delete(@PathParam("clientId") String clientId) {
        event.event(EventType.CLIENT_DELETE).client(clientId);

        ClientModel client = authenticate(false, clientId);
        if (realm.removeClient(client.getId())) {
            event.success();
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private ClientModel authenticate(boolean create, String clientId) {
        String authorizationHeader = session.getContext().getRequestHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        boolean bearer = authorizationHeader != null && authorizationHeader.split(" ")[0].equalsIgnoreCase("Bearer");

        if (bearer) {
            AuthenticationManager.AuthResult authResult = authManager.authenticateBearerToken(session, realm);
            AccessToken.Access realmAccess = authResult.getToken().getResourceAccess(Constants.REALM_MANAGEMENT_CLIENT_ID);
            if (realmAccess != null) {
                if (realmAccess.isUserInRole(AdminRoles.MANAGE_CLIENTS)) {
                    return create ? null : realm.getClientByClientId(clientId);
                }

                if (create && realmAccess.isUserInRole(AdminRoles.CREATE_CLIENT)) {
                    return create ? null : realm.getClientByClientId(clientId);
                }
            }
        } else if (!create) {
            ClientModel client;

            try {
                AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, realm);
                client = clientAuth.getClient();

                if (client != null && !client.isPublicClient() && client.getClientId().equals(clientId)) {
                    return client;
                }
            } catch (Throwable t) {
            }
        }

        event.error(Errors.NOT_ALLOWED);

        throw new ForbiddenException();
    }

}
