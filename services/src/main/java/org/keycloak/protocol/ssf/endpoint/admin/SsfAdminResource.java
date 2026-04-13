package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashSet;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.receiver.resources.SsfReceiverAdminResource;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * SsfAdmin resource to manage SSF related components.
 *
 * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf}
 */
public class SsfAdminResource {

    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AdminPermissionEvaluator auth;
    protected final AdminEventBuilder adminEvent;

    public SsfAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    /**
     * Returns the current SSF configuration for this realm, including default
     * values used by the SSF Transmitter (e.g. the set of event types supported
     * by default when a receiver client does not configure its own).
     *
     * Additional realm/transmitter-level SSF settings can be added to
     * {@link SsfConfigRepresentation} as the SSF feature evolves.
     *
     * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/config}
     */
    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public SsfConfigRepresentation getConfig() {

        auth.realm().requireViewRealm();

        SsfTransmitterProvider transmitter = Ssf.transmitter();

        SsfConfigRepresentation config = new SsfConfigRepresentation();
        config.setDefaultSupportedEvents(transmitter.getDefaultSupportedEvents());
        config.setAvailableSupportedEvents(transmitter.getKnownEventAliases());
        config.setDefaultPushEndpointConnectTimeoutMillis(
                Ssf.DEFAULT_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS);
        config.setDefaultPushEndpointSocketTimeoutMillis(
                Ssf.DEFAULT_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS);
        return config;
    }

    /**
     * Returns the current SSF stream state for a single receiver client, including
     * the events that the transmitter currently delivers to it.
     *
     * Returns 404 if the client does not exist or has no SSF stream registered yet
     * (i.e. the receiver has not created a stream via the SSF Transmitter API).
     *
     * The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/stream}
     */
    @GET
    @Path("clients/{clientId}/stream")
    @Produces(MediaType.APPLICATION_JSON)
    public SsfClientStreamRepresentation getClientStream(@PathParam("clientId") String clientId) {

        auth.realm().requireViewRealm();

        ClientModel client = realm.getClientById(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        ClientStreamStore store = new ClientStreamStore(session);
        StreamConfig streamConfig = store.getStreamForClient(client);
        if (streamConfig == null) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        SsfTransmitterProvider transmitter = Ssf.transmitter();

        SsfClientStreamRepresentation rep = new SsfClientStreamRepresentation();
        rep.setStreamId(streamConfig.getStreamId());
        rep.setAudience(streamConfig.getAudience());
        rep.setEventsSupported(toEventAliases(transmitter, streamConfig.getEventsSupported()));
        rep.setEventsRequested(toEventAliases(transmitter, streamConfig.getEventsRequested()));
        rep.setEventsDelivered(toEventAliases(transmitter, streamConfig.getEventsDelivered()));
        return rep;
    }

    /**
     * Deletes the currently registered SSF stream for a receiver client so the
     * receiver can re-register with a fresh configuration. Returns 204 on
     * success, 404 if the client does not exist or has no registered stream.
     *
     * The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/stream}
     */
    @DELETE
    @Path("clients/{clientId}/stream")
    public Response deleteClientStream(@PathParam("clientId") String clientId) {

        ClientModel client = realm.getClientById(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        auth.clients().requireManage(client);

        ClientStreamStore store = new ClientStreamStore(session);
        boolean deleted = store.deleteStreamForClient(client);
        if (!deleted) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        return Response.noContent().build();
    }

    /**
     * Converts the given full event type URIs to their event aliases (e.g.
     * {@code CaepCredentialChange}) using the transmitter's mapping. Unknown
     * event types are passed through unchanged so the admin UI still shows
     * something meaningful.
     */
    protected Set<String> toEventAliases(SsfTransmitterProvider transmitter, Set<String> eventTypes) {
        if (eventTypes == null) {
            return null;
        }
        Set<String> aliases = new LinkedHashSet<>();
        for (String eventType : eventTypes) {
            String alias = transmitter.resolveAliasForEventType(eventType);
            aliases.add(alias != null ? alias : eventType);
        }
        return aliases;
    }

    /**
     * Exposes the {@link SsfReceiverAdminResource} for managing SSF Receivers as a custom endpoint.
     *
     * Checks if the current user can access the SSF admin resource for receivers.
     *
     * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/receivers}
     * @return
     */
    @Path(Ssf.SSF_RECEIVERS_PATH)
    public SsfReceiverAdminResource receiverManagementEndpoint() {

        checkReceiverAdminResourceAccess();

        return receiverAdminResource();
    }

    /**
     * Provies the actual {@link SsfReceiverAdminResource}.
     * @return
     */
    protected SsfReceiverAdminResource receiverAdminResource() {
        return new SsfReceiverAdminResource(session, auth);
    }

    /**
     * Checks if the current user can access the SSF admin resource for receivers.
     */
    protected void checkReceiverAdminResourceAccess() {
        auth.realm().requireManageIdentityProviders();
    }
}
