package org.keycloak.admin.ui.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.keycloak.admin.ui.rest.model.SessionId;
import org.keycloak.admin.ui.rest.model.SessionId.SessionType;
import org.keycloak.admin.ui.rest.model.SessionRepresentation;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.admin.ui.rest.model.SessionId.SessionType.*;

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
                                                       @QueryParam("search") @DefaultValue("") final String search, @QueryParam("first")
                                                       @DefaultValue("0") int first, @QueryParam("max") @DefaultValue("10") int max) {
        auth.realm().requireViewRealm();

        Stream<SessionId> sessionIdStream = Stream.<SessionId>builder().build();
        if (type == ALL || type == REGULAR) {
            final Map<String, Long> clientSessionStats = session.sessions().getActiveClientSessionStats(realm, false);
            sessionIdStream = Stream.concat(sessionIdStream, clientSessionStats
                    .keySet().stream().map(i -> new SessionId(i, REGULAR)));
        }
        if (type == ALL || type == OFFLINE) {
            sessionIdStream = Stream.concat(sessionIdStream, session.sessions().getActiveClientSessionStats(realm, true)
                    .keySet().stream().map(i -> new SessionId(i, OFFLINE)));
        }

        Stream<SessionRepresentation> result = sessionIdStream.flatMap((sessionId) -> {
            ClientModel clientModel = realm.getClientById(sessionId.getClientId());
            switch (sessionId.getType()) {
                case REGULAR:
                    return session.sessions().getUserSessionsStream(realm, clientModel)
                            .map(s -> toUserSessionRepresentation(s, sessionId.getClientId(), REGULAR));
                case OFFLINE:
                    return session.sessions()
                            .getOfflineUserSessionsStream(realm, clientModel, null, null)
                            .map(s -> toUserSessionRepresentation(s, sessionId.getClientId(), OFFLINE));
            }
            return Stream.<SessionRepresentation>builder().build();
        }).distinct();

        if (!search.equals("")) {
            result = result.filter(s -> s.getUsername().contains(search) || s.getIpAddress().contains(search)
                    || s.getClients().values().stream().anyMatch(c -> c.contains(search)));
        }
        return result.skip(first).limit(max);
    }

    private SessionRepresentation toUserSessionRepresentation(final UserSessionModel userSession, String clientId, SessionType type) {
        SessionRepresentation rep = toRepresentation(userSession, type);

        // Update lastSessionRefresh with the timestamp from clientSession
        userSession.getAuthenticatedClientSessions().entrySet().stream()
                .filter(entry -> Objects.equals(clientId, entry.getKey()))
                .findFirst().ifPresent(result -> rep.setLastAccess(Time.toMillis(result.getValue().getTimestamp())));
        return rep;
    }

    public static SessionRepresentation toRepresentation(UserSessionModel session, SessionType type) {
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
