package org.keycloak.admin.ui.rest;

import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.keycloak.admin.ui.rest.model.ClientIdSessionType.SessionType;
import org.keycloak.admin.ui.rest.model.SessionRepresentation;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.StringUtil;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import static org.keycloak.admin.ui.rest.model.ClientIdSessionType.SessionType.OFFLINE;
import static org.keycloak.admin.ui.rest.model.ClientIdSessionType.SessionType.REGULAR;

public class SessionsResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public SessionsResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

    @GET
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all sessions of the current realm also the once that use offline tokens",
            description = "This endpoint returns a list of sessions and the clients that have been used including offline tokens"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = SessionRepresentation.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public Stream<SessionRepresentation> realmSessions(@QueryParam("type") @DefaultValue("ALL") final SessionType type,
                                                       @QueryParam("search") @DefaultValue("") final String search,
                                                       @QueryParam("first") @DefaultValue("0") int first,
                                                       @QueryParam("max") @DefaultValue("10") int max) {
        auth.realm().requireViewRealm();

        var stream = switch (type) {
            case OFFLINE -> streamOffline();
            case REGULAR -> streamRegular();
            case ALL -> Stream.concat(streamRegular(), streamOffline());
        };

        return applySearch(search, stream).skip(first).limit(max);
    }

    @GET
    @Path("client")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all sessions of the passed client containing regular and offline",
            description = "This endpoint returns a list of sessions and the clients that have been used including offline tokens"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = SessionRepresentation.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public Stream<SessionRepresentation> clientSessions(@QueryParam("clientId") final String clientId,
                                                        @QueryParam("type") @DefaultValue("ALL") final SessionType type,
                                                        @QueryParam("search") @DefaultValue("") final String search,
                                                        @QueryParam("first") @DefaultValue("0") int first,
                                                        @QueryParam("max") @DefaultValue("10") int max) {
        ClientModel clientModel = realm.getClientById(clientId);
        auth.clients().requireView(clientModel);

        var stream = switch (type) {
            case OFFLINE -> streamOffline(clientModel);
            case REGULAR -> streamRegular(clientModel);
            case ALL -> Stream.concat(streamRegular(clientModel), streamOffline(clientModel));
        };
        return applySearch(search, stream).skip(first).limit(max);
    }

    private Stream<SessionRepresentation> applySearch(String search, Stream<SessionRepresentation> result) {
        result = result.filter(sessionRep ->
            auth.users().canView(session.users().getUserById(realm, sessionRep.getUserId()))
        );
        if (!StringUtil.isBlank(search)) {
            String searchTrimmed = search.trim();
            result = result.filter(s -> s.getUsername().contains(searchTrimmed) || s.getIpAddress().contains(searchTrimmed)
                    || s.getClients().values().stream().anyMatch(c -> c.contains(searchTrimmed)));
        }
        return result;
    }

    private Stream<SessionRepresentation> streamRegular() {
        return session.sessions().readOnlyStreamUserSessions(realm)
                .map(s -> toRepresentation(s, REGULAR));
    }

    private Stream<SessionRepresentation> streamOffline() {
        return session.sessions().readOnlyStreamOfflineUserSessions(realm)
                .map(s -> toRepresentation(s, OFFLINE));
    }

    private Stream<SessionRepresentation> streamRegular(ClientModel client) {
        return session.sessions().readOnlyStreamUserSessions(realm, client, -1, -1)
                .map(s -> toRepresentation(s, REGULAR));
    }

    private Stream<SessionRepresentation> streamOffline(ClientModel client) {
        return session.sessions().readOnlyStreamOfflineUserSessions(realm, client, -1, -1)
                .map(s -> toRepresentation(s, OFFLINE));
    }

    private static SessionRepresentation toRepresentation(UserSessionModel session, SessionType type) {
        SessionRepresentation rep = new SessionRepresentation();
        rep.setId(session.getId());
        rep.setStart(Time.toMillis(session.getStarted()));
        rep.setLastAccess(Time.toMillis(session.getLastSessionRefresh()));
        rep.setUsername(session.getUser().getUsername());
        rep.setUserId(session.getUser().getId());
        rep.setIpAddress(session.getIpAddress());
        rep.setType(type);
        for (AuthenticatedClientSessionModel clientSession : session.getAuthenticatedClientSessions().values()) {
            ClientModel client = clientSession.getClient();
            rep.getClients().put(client.getId(), client.getClientId());
        }
        rep.setTransientUser(LightweightUserAdapter.isLightweightUser(session.getUser().getId()));
        return rep;
    }
}
