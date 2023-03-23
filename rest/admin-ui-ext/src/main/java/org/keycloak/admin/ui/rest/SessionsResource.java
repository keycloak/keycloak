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
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

        Stream<SessionId> clientIds = Stream.<SessionId>builder().build();
        long clientSessionsCount = 0L;
        if (type == ALL || type == REGULAR) {
            final Map<String, Long> clientSessionStats = session.sessions().getActiveClientSessionStats(realm, false);
            clientSessionsCount = clientSessionStats.values().stream().reduce(0L, Long::sum);
            clientIds = Stream.concat(clientIds, clientSessionStats
                    .keySet().stream().map(i -> new SessionId(i, REGULAR)));
        }
        if (type == ALL || type == OFFLINE) {
            clientIds = Stream.concat(clientIds, session.sessions().getActiveClientSessionStats(realm, true)
                    .keySet().stream().map(i -> new SessionId(i, OFFLINE)));
        }


        final List<SessionId> sessionIds = clientIds.skip(first).limit(max).collect(Collectors.toList());
        Stream<SessionRepresentation> result = Stream.<SessionRepresentation>builder().build();
        for (SessionId sessionId : sessionIds) {
            ClientModel clientModel = realm.getClientById(sessionId.getClientId());
            switch (sessionId.getType()) {
                case REGULAR:
                    result = Stream.concat(result, session.sessions().getUserSessionsStream(realm, clientModel)
                            .map(s -> toUserSessionRepresentation(s, sessionId.getClientId(), REGULAR)));
                    break;
                case OFFLINE:
                    result = Stream.concat(result, session.sessions()
                            .getOfflineUserSessionsStream(realm, clientModel, Math.max((int) (first - clientSessionsCount), 0), max)
                            .map(s -> toUserSessionRepresentation(s, sessionId.getClientId(), OFFLINE)));
                    break;
            }
        }

        if (!search.equals("")) {
            return result.filter(s -> s.getUsername().contains(search) || s.getIpAddress().contains(search));
        }
        return result;
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
        return rep;
    }
}
