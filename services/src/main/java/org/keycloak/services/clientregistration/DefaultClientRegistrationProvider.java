package org.keycloak.services.clientregistration;

import org.jboss.logging.Logger;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultClientRegistrationProvider implements ClientRegistrationProvider {

    private static final Logger logger = Logger.getLogger(DefaultClientRegistrationProvider.class);

    private KeycloakSession session;
    private EventBuilder event;
    private RealmModel realm;

    public DefaultClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(ClientRepresentation client) {
        event.event(EventType.CLIENT_REGISTER);

        authenticate(true, null);

        try {
            ClientModel clientModel = RepresentationToModel.createClient(session, realm, client, true);
            client = ModelToRepresentation.toRepresentation(clientModel);
            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build();

            logger.infov("Created client {0}", client.getClientId());

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

    @Override
    public void close() {

    }


    private ClientModel authenticate(boolean create, String clientId) {
        String authorizationHeader = session.getContext().getRequestHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        boolean bearer = authorizationHeader != null && authorizationHeader.split(" ")[0].equalsIgnoreCase("Bearer");

        if (bearer) {
            AuthenticationManager.AuthResult authResult = new AppAuthManager().authenticateBearerToken(session, realm);
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

    @Override
    public void setRealm(RealmModel realm) {
this.realm = realm;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

}
