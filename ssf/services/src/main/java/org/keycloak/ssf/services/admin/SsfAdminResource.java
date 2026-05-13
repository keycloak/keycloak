package org.keycloak.ssf.services.admin;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.jpa.entities.OutboxEntryStatus;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIds;
import org.keycloak.ssf.subject.SubjectResolution;
import org.keycloak.ssf.subject.SubjectResolver;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.admin.SsfAdminSubjectRequest;
import org.keycloak.ssf.transmitter.admin.SsfAdminSubjectResponse;
import org.keycloak.ssf.transmitter.admin.SsfClientStreamRepresentation;
import org.keycloak.ssf.transmitter.admin.SsfConfigRepresentation;
import org.keycloak.ssf.transmitter.admin.SsfEmitEventRequest;
import org.keycloak.ssf.transmitter.admin.SsfEmitEventResponse;
import org.keycloak.ssf.transmitter.admin.SsfEventRepresentation;
import org.keycloak.ssf.transmitter.admin.SsfEventStatsRepresentation;
import org.keycloak.ssf.transmitter.delivery.SseCaepEventConverter;
import org.keycloak.ssf.transmitter.emit.EmitEventResult;
import org.keycloak.ssf.transmitter.emit.EmitEventStatus;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.outbox.SsfOutboxKinds;
import org.keycloak.ssf.transmitter.stream.DuplicateStreamConfigException;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfigInputRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamVerificationRequest;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.AdminSubjectResult;
import org.keycloak.ssf.transmitter.subject.SubjectManagementResult;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.ssf.transmitter.support.SsfErrorRepresentation;
import org.keycloak.ssf.transmitter.support.SsfUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * SsfAdmin resource to manage SSF related components.
 *
 * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf}
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class SsfAdminResource {

    private static final Logger log = Logger.getLogger(SsfAdminResource.class);
    public static final String SSF_SYNTHETIC_EVENT_TYPE = "SSF_SYNTHETIC_EVENT";

    /**
     * Outbox entry kinds the admin REST endpoints aggregate across.
     * SSF stores PUSH and POLL deliveries under separate kinds in
     * the generic outbox; admin operations (stats, bulk delete,
     * queued purge) treat the pair as one logical surface so the
     * REST shape doesn't expose the implementation split.
     */
    public static final List<String> SSF_OUTBOX_KINDS =
            List.of(SsfOutboxKinds.PUSH, SsfOutboxKinds.POLL);

    protected final KeycloakSession session;

    protected final RealmModel realm;

    protected final AdminPermissionEvaluator auth;

    protected final AdminEventBuilder adminEvent;

    protected final SsfTransmitterProvider transmitter;

    public SsfAdminResource(KeycloakSession session,
                            RealmModel realm,
                            AdminPermissionEvaluator auth,
                            AdminEventBuilder adminEvent,
                            SsfTransmitterProvider transmitter) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.transmitter = transmitter;
    }

    /**
     * Convenience accessor — pulls the lazily-built subject management
     * service off the per-session transmitter provider. Older revisions
     * of this class held the service as a constructor-injected field;
     * the lazy-on-provider model keeps the constructor lean and means
     * a request that never touches subject management doesn't pay for
     * building it.
     */
    protected SubjectManagementService subjectManagementService() {
        return transmitter.subjectManagementService();
    }

    protected SsfStreamStore streamStore() {
        return transmitter.streamStore();
    }

    /**
     * Builds a 400 BAD_REQUEST response carrying an
     * {@link SsfErrorRepresentation} with {@code errorCode} as its
     * machine-readable category. Used by {@link #emitEvent} to surface
     * each {@link EmitEventStatus} client-error category with its own
     * specific code (e.g. {@code unknown_event_type},
     * {@code subject_not_found}) so callers and tests can distinguish
     * the failure reason from the wire response, while still preserving
     * any service-supplied detail message.
     */
    protected Response invalidRequest(String errorCode, String message, String fallback) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new SsfErrorRepresentation(errorCode,
                        message != null ? message : fallback))
                .build();
    }

    /**
     * Sums per-status counts from one outbox kind into the running
     * accumulator. Used by the realm- and owner-scoped stats endpoints
     * that aggregate across {@link #SSF_OUTBOX_KINDS}.
     */
    private static void mergeCounts(Map<OutboxEntryStatus, Long> accumulator,
                                    Map<OutboxEntryStatus, Long> increment) {
        for (Map.Entry<OutboxEntryStatus, Long> entry : increment.entrySet()) {
            accumulator.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
    }

    /**
     * Folds per-status oldest-{@code createdAt} timestamps from one
     * outbox kind into the running accumulator, keeping the earlier of
     * the two values per status.
     */
    private static void mergeOldest(Map<OutboxEntryStatus, Instant> accumulator,
                                    Map<OutboxEntryStatus, Instant> increment) {
        for (Map.Entry<OutboxEntryStatus, Instant> entry : increment.entrySet()) {
            accumulator.merge(entry.getKey(), entry.getValue(),
                    (a, b) -> a.isBefore(b) ? a : b);
        }
    }

    /**
     * Builds the wire-shape representation from the merged
     * (count, oldestCreatedAt) maps. Statuses with zero rows aren't
     * in the accumulator (the underlying GROUP BY doesn't synthesize
     * zero rows) so they're naturally absent from the response.
     */
    private static SsfEventStatsRepresentation toEventStatsRepresentation(
            Map<OutboxEntryStatus, Long> counts,
            Map<OutboxEntryStatus, Instant> oldest) {
        SsfEventStatsRepresentation rep = new SsfEventStatsRepresentation();
        Map<String, SsfEventStatsRepresentation.StatusEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<OutboxEntryStatus, Long> entry : counts.entrySet()) {
            entries.put(entry.getKey().name(),
                    new SsfEventStatsRepresentation.StatusEntry(entry.getValue(), oldest.get(entry.getKey())));
        }
        rep.setStatuses(entries);
        return rep;
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Get SSF realm configuration",
            description = "Returns the current SSF configuration for this realm, including transmitter defaults such as the set of event types supported by default when a receiver client does not configure its own."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfConfigRepresentation.class)))
    })
    public SsfConfigRepresentation getConfig() {

        auth.realm().requireViewRealm();

        SsfTransmitterConfig transmitterConfig = transmitter.getConfig();

        SsfConfigRepresentation config = new SsfConfigRepresentation();
        // All fields are exposed as aliases so the admin UI can render a
        // human-readable selection list and pre-select defaults against the
        // same option values. availableSupportedEvents covers everything a
        // receiver may legitimately request (registry minus stream-internal
        // lifecycle events); nativelyEmittedEvents is the informational
        // subset Keycloak fires from native event listeners — used purely
        // to badge entries in the UI, not as a delivery gate.
        config.setDefaultSupportedEvents(toEventAliases(transmitter, transmitter.getDefaultSupportedEvents()));
        config.setAvailableSupportedEvents(transmitter.getAvailableEventAliases());
        config.setNativelyEmittedEvents(transmitter.getNativelyEmittedEventAliases());
        config.setDefaultPushEndpointConnectTimeoutMillis(
                transmitterConfig.getPushEndpointConnectTimeoutMillis());
        config.setDefaultPushEndpointSocketTimeoutMillis(
                transmitterConfig.getPushEndpointSocketTimeoutMillis());
        config.setDefaultUserSubjectFormat(transmitterConfig.getUserSubjectFormat());
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Get SSF stream for client",
            description = "Returns the current SSF stream state for a single receiver client, including the events that the transmitter currently delivers to it."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfClientStreamRepresentation.class))),
            @APIResponse(responseCode = "404", description = "Client not found or no SSF stream registered")
    })
    public SsfClientStreamRepresentation getClientStream(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId) {

        auth.realm().requireViewRealm();

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        StreamConfig streamConfig = streamStore().getStreamForClient(client);
        if (streamConfig == null) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        return toClientStreamRepresentation(streamConfig, client);
    }

    /**
     * Admin-initiated creation of an SSF stream for an existing receiver client.
     *
     * <p>Mirrors the receiver-facing {@code POST /streams} flow but bypasses
     * the receiver-vs-transmitter profile validation for {@code iss}/
     * {@code aud}/{@code format}: the admin is trusted to set any of those
     * from the admin UI's create-stream form, and the admin UI surfaces
     * {@code aud} explicitly so the operator can point the stream at a
     * specific receiver feed URL (e.g. for Apple Business Manager feeds)
     * regardless of whether the client is on the SSF 1.0 or SSE_CAEP
     * profile.
     *
     * <p>Returns 201 with the created stream representation, 400 on a
     * validation error, 404 if the client does not exist, 409 if the
     * client already has a registered stream.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/stream}.
     */
    @POST
    @Path("clients/{clientId}/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Create SSF stream for client",
            description = "Admin-initiated creation of an SSF stream for an existing receiver client. Mirrors the receiver-facing POST /streams flow but bypasses the receiver-vs-transmitter profile validation for iss/aud/format."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = SsfClientStreamRepresentation.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Client not found"),
            @APIResponse(responseCode = "409", description = "Client already has a registered stream")
    })
    public Response createClientStream(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId,
            StreamConfigInputRepresentation input) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        auth.clients().requireManage(client);

        try {
            StreamConfig created = transmitter.streamService().createStreamAsAdmin(input, client);
            return Response.status(Response.Status.CREATED)
                    .entity(toClientStreamRepresentation(created, client))
                    .build();
        } catch (DuplicateStreamConfigException dsce) {
            log.debugf(dsce, "Admin stream create rejected for client %s: duplicate stream", clientId);
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                    .entity(new SsfErrorRepresentation("stream_error", dsce.getMessage()))
                    .build());
        } catch (SsfException e) {
            log.debugf(e, "Admin stream create rejected for client %s: %s", clientId, e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", e.getMessage()))
                    .build());
        }
    }

    /**
     * Builds the admin wire representation from a stored {@link StreamConfig}
     * and the receiver client that owns it. Shared by {@code getClientStream}
     * and {@code createClientStream} so both endpoints return exactly the
     * same shape.
     */
    protected SsfClientStreamRepresentation toClientStreamRepresentation(StreamConfig streamConfig, ClientModel client) {

        SsfClientStreamRepresentation rep = new SsfClientStreamRepresentation();
        rep.setStreamId(streamConfig.getStreamId());
        rep.setDescription(streamConfig.getDescription());
        if (streamConfig.getStatus() != null) {
            rep.setStatus(streamConfig.getStatus().name());
        }
        rep.setStatusReason(streamConfig.getStatusReason());
        rep.setAudience(streamConfig.getAudience());
        rep.setDelivery(streamConfig.getDelivery());
        rep.setEventsSupported(toEventAliases(transmitter, streamConfig.getEventsSupported()));
        rep.setEventsRequested(toEventAliases(transmitter, streamConfig.getEventsRequested()));
        rep.setEventsDelivered(toEventAliases(transmitter, streamConfig.getEventsDelivered()));
        rep.setCreatedAt(streamConfig.getCreatedAt());
        rep.setUpdatedAt(streamConfig.getUpdatedAt());
        String lastVerifiedAtRaw = client.getAttribute(ClientStreamStore.SSF_LAST_VERIFIED_AT_KEY);
        if (lastVerifiedAtRaw != null && !lastVerifiedAtRaw.isBlank()) {
            try {
                rep.setLastVerifiedAt(Integer.valueOf(lastVerifiedAtRaw));
            } catch (NumberFormatException ignored) {
                // Defensive: ignore a malformed attribute value rather than
                // failing the whole admin GET for this stream.
            }
        }
        return rep;
    }

    /**
     * Sends an unsolicited verification Security Event Token (SET) to a
     * receiver client's registered SSF stream. Triggers the same delivery
     * path the receiver-initiated {@code /streams/verify} endpoint uses,
     * but driven by an admin from the admin console so operators can
     * sanity-check a stream without credentials for the receiver side.
     *
     * <p>Returns 204 on success, 404 if the client does not exist or has
     * no registered stream. The push itself is fire-and-forget — the
     * dispatcher schedules the delivery on the push executor so this
     * endpoint returns as soon as the verification SET has been handed
     * off, not when the receiver has acknowledged it.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/stream/verify}.
     */
    @POST
    @Path("clients/{clientId}/stream/verify")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Verify SSF stream for client",
            description = "Sends an unsolicited verification Security Event Token (SET) to a receiver client's registered SSF stream. The push is fire-and-forget — the dispatcher schedules the delivery on the push executor."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "404", description = "Client not found or no SSF stream registered")
    })
    public Response verifyClientStream(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        auth.clients().requireManage(client);

        StreamConfig streamConfig = streamStore().getStreamForClient(client);
        if (streamConfig == null) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        StreamVerificationRequest verificationRequest = new StreamVerificationRequest();
        verificationRequest.setStreamId(streamConfig.getStreamId());
        // Per SSF §8.1.4.2-5 a transmitter-initiated verification MUST NOT
        // include a state nonce — only receiver-initiated requests may set
        // one — so we leave the state null here.

        boolean triggered = transmitter.verificationService().triggerVerification(verificationRequest,
                SsfMetricsBinder.VerificationInitiator.ADMIN);
        if (!triggered) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        // The ssf.lastVerifiedAt stamp is written centrally inside
        // StreamVerificationService.triggerVerification so every
        // verification entry point (receiver-initiated, admin-initiated,
        // transmitter-initiated post-create auto-fire) records a
        // consistent timestamp.

        return Response.noContent().build();
    }

    /**
     * Admin status-update for a receiver's registered stream. Funnels
     * through the same {@code StreamService.updateStreamStatus} path
     * the receiver-side {@code POST /streams/status} hits, so the
     * SSF spec-mandated {@code stream-updated} SET dispatch and the
     * outbox HELD ↔ PENDING alignment fire here too. Without this,
     * an admin flipping status from the console would just persist
     * the {@code ssf.status} client attribute and the receiver would
     * silently observe paused / enabled state on next poll without
     * the spec's transition signal.
     *
     * <p>If the request body's {@code reason} is null or blank we
     * substitute a stable {@code "Transmitter status override"}
     * marker so receivers can correlate the stream-updated SET with
     * a transmitter-side decision (vs. a receiver-driven status
     * update). The marker is intentionally written from the receiver's
     * perspective — receivers only see "the transmitter changed it",
     * not which actor on the transmitter side initiated the change.
     *
     * <p>Auth: {@code manage-clients} on the receiver — same surface
     * as every other admin SSF endpoint.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/stream/status}.
     */
    @POST
    @Path("clients/{clientId}/stream/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Update SSF stream status for client",
            description = "Admin-initiated stream status update. Funnels through the same StreamService.updateStreamStatus path as the receiver-side endpoint so the stream-updated SET dispatch and outbox alignment fire."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StreamStatus.class))),
            @APIResponse(responseCode = "400", description = "Bad Request — missing or invalid status"),
            @APIResponse(responseCode = "404", description = "Client not found or no SSF stream registered")
    })
    public Response updateClientStreamStatus(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId,
            StreamStatus newStatus) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (newStatus == null || newStatus.getStatus() == null || newStatus.getStatus().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "status is required"))
                    .build();
        }

        StreamConfig streamConfig = streamStore().getStreamForClient(client);
        if (streamConfig == null) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        // The receiver-side endpoint takes streamId from the body so a
        // single receiver could (in principle) target multiple streams
        // — admins always operate on the single registered stream for
        // the path-supplied client, so we stamp the streamId from
        // storage rather than trusting the body. Default the reason
        // when the admin didn't supply one.
        newStatus.setStreamId(streamConfig.getStreamId());
        if (newStatus.getReason() == null || newStatus.getReason().isBlank()) {
            newStatus.setReason("Transmitter status override");
        }

        StreamStatus updated = transmitter.streamService().updateStreamStatusAsAdmin(newStatus, client);
        if (updated == null) {
            throw new NotFoundException("No SSF stream registered for client");
        }
        return Response.ok(updated).build();
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Delete SSF stream for client",
            description = "Deletes the currently registered SSF stream for a receiver client so the receiver can re-register with a fresh configuration."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "404", description = "Client not found or no SSF stream registered")
    })
    public Response deleteClientStream(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        auth.clients().requireManage(client);

        // Look up the stream first so we can route through
        // streamService.deleteStream(streamId), which runs the
        // cascade-purge of pending outbox rows for this client. Going
        // straight to streamStore.deleteStreamForClient would bypass
        // the cascade and leave POLL rows orphaned.
        StreamConfig existingStream = streamStore().getStreamForClient(client);
        if (existingStream == null) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        boolean deleted = transmitter.streamService().deleteStream(existingStream.getStreamId());
        if (!deleted) {
            // Stream existed at lookup time but was deleted concurrently
            // — treat as idempotent-success rather than a 404 race.
            log.debugf("Admin stream delete found the stream gone between lookup and delete. clientId=%s streamId=%s",
                    client.getClientId(), existingStream.getStreamId());
        }

        return Response.noContent().build();
    }

    /**
     * Adds a subject to a receiver client's notification scope. Resolves
     * the subject by the admin shorthand type ({@code user-id},
     * {@code user-email}, {@code org-alias}) and sets the
     * {@code ssf.notify.<clientId>} attribute on the resolved entity.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/subjects/add}.
     */
    @POST
    @Path("clients/{clientId}/subjects/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Add subject to client notification scope",
            description = "Adds a subject to a receiver client's notification scope. Resolves the subject by the admin shorthand type (user-id, user-email, org-alias) and sets the ssf.notify.<clientId> attribute on the resolved entity."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfAdminSubjectResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Client or subject not found")
    })
    public Response addSubject(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId,
            SsfAdminSubjectRequest request) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (request == null || request.getType() == null || request.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "type and value are required"))
                    .build();
        }


        // svc takes the internal UUID, not the OAuth clientId.
        AdminSubjectResult result = subjectManagementService().addSubjectByAdmin(client.getId(), request.getType(), request.getValue());

        if (result.result() == SubjectManagementResult.OK) {
            return Response.ok(new SsfAdminSubjectResponse("notified", result.entityType(), result.entityId())).build();
        }
        if (result.result() == SubjectManagementResult.SUBJECT_NOT_FOUND) {
            throw new NotFoundException("Subject not found");
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new SsfErrorRepresentation("invalid_request", "unsupported subject type: " + request.getType()))
                .build();
    }

    /**
     * Removes a subject from a receiver client's notification scope.
     * Resolves the subject and clears the {@code ssf.notify.<clientId>}
     * attribute from the resolved entity.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/subjects/remove}.
     */
    @POST
    @Path("clients/{clientId}/subjects/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Remove subject from client notification scope",
            description = "Removes a subject from a receiver client's notification scope. Resolves the subject and clears the ssf.notify.<clientId> attribute from the resolved entity."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfAdminSubjectResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Client or subject not found")
    })
    public Response removeSubject(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId,
            SsfAdminSubjectRequest request) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (request == null || request.getType() == null || request.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "type and value are required"))
                    .build();
        }

        AdminSubjectResult result = subjectManagementService().removeSubjectByAdmin(client.getId(), request.getType(), request.getValue());

        if (result.result() == SubjectManagementResult.OK) {
            return Response.ok(new SsfAdminSubjectResponse("not_notified", result.entityType(), result.entityId())).build();
        }
        if (result.result() == SubjectManagementResult.SUBJECT_NOT_FOUND) {
            throw new NotFoundException("Subject not found");
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new SsfErrorRepresentation("invalid_request", "unsupported subject type: " + request.getType()))
                .build();
    }

    /**
     * Explicitly excludes a subject from a receiver client's notification
     * scope by setting {@code ssf.notify.<clientId>=false}. The subject
     * will not receive events even in {@code default_subjects=ALL} mode.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/subjects/ignore}.
     */
    @POST
    @Path("clients/{clientId}/subjects/ignore")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Ignore subject for client",
            description = "Explicitly excludes a subject from a receiver client's notification scope by setting ssf.notify.<clientId>=false. The subject will not receive events even in default_subjects=ALL mode."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfAdminSubjectResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Client or subject not found")
    })
    public Response ignoreSubject(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId,
            SsfAdminSubjectRequest request) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (request == null || request.getType() == null || request.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "type and value are required"))
                    .build();
        }

        AdminSubjectResult result = subjectManagementService().ignoreSubjectByAdmin(client.getId(), request.getType(), request.getValue());

        if (result.result() == SubjectManagementResult.OK) {
            return Response.ok(new SsfAdminSubjectResponse("ignored", result.entityType(), result.entityId())).build();
        }
        if (result.result() == SubjectManagementResult.SUBJECT_NOT_FOUND) {
            throw new NotFoundException("Subject not found");
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new SsfErrorRepresentation("invalid_request", "unsupported subject type: " + request.getType()))
                .build();
    }

    /**
     * Inspects a subject's effective notification state for a receiver
     * client without mutating anything. Read-only equivalent of
     * {@code /subjects/add}/{@code /remove}/{@code /ignore}: same
     * resolution semantics for {@code (type, value)}, but reports back
     * the state the dispatcher's subject gate would observe rather than
     * changing it.
     *
     * <p>Drives the admin UI's "Check" button so the displayed status
     * is authoritative — including {@code implicitly_included} when
     * {@code default_subjects=ALL} and no explicit per-user / per-org
     * attribute is set, and {@code notified_via_org} /
     * {@code ignored_via_org} when a user inherits state from one of
     * their organizations.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/subjects/check}.
     */
    @POST
    @Path("clients/{clientId}/subjects/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Check subject notification state for client",
            description = "Read-only inspection of a subject's effective notification state for a receiver client. Returns the state the dispatcher's subject gate would observe (notified, ignored, notified_via_org, ignored_via_org, implicitly_included, not_notified) without mutating any attribute."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfAdminSubjectResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Client or subject not found")
    })
    public Response checkSubject(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId,
            SsfAdminSubjectRequest request) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        // VIEW is enough for a read-only inspection — operators with
        // view-clients should be able to query subject state without
        // needing manage-clients.
        auth.clients().requireView(client);

        if (request == null || request.getType() == null || request.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "type and value are required"))
                    .build();
        }

        SubjectManagementService.AdminSubjectStatus status =
                subjectManagementService().inspectSubjectByAdmin(client, request.getType(), request.getValue());

        if ("not_found".equals(status.status())) {
            throw new NotFoundException("Subject not found");
        }
        if ("unsupported_format".equals(status.status())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "unsupported subject type: " + request.getType()))
                    .build();
        }
        return Response.ok(new SsfAdminSubjectResponse(
                status.status(), status.entityType(), status.entityId(), status.sourceOrgAlias())).build();
    }

    /**
     * Pushes a synthetic SSF event for a receiver client on behalf of a
     * trusted IAM management client whose service-account token has the
     * receiver-configured emit-events role.
     *
     * <p>This is the integration hook for environments where Keycloak
     * cannot natively observe upstream events — typical example is a
     * federated user store (LDAP / external IdM) that owns credential
     * changes, with Keycloak acting only as the SSF transmitter for an
     * downstream receiver such as Apple Business Manager. The management
     * client (e.g. an IAM workflow service) forwards the event by
     * calling this endpoint; the transmitter wraps the payload into a
     * signed SET and dispatches it through the same outbox + push path
     * native events take.
     *
     * <p>Authorization is intentionally narrow and bypasses
     * {@code requireManage}: the token must come from a client whose
     * service-account user holds the client role configured on the
     * receiver via {@link ClientStreamStore#SSF_EMIT_EVENTS_ROLE_KEY},
     * and the receiver must opt in via
     * {@link ClientStreamStore#SSF_ALLOW_EMIT_EVENTS_KEY}. This keeps
     * "emit events for receiver X" granular and decoupled from
     * {@code manage-clients}.
     *
     * <p>Receiver-side filters still apply: the event is only pushed if
     * its event type is in the receiver's {@code events_requested} set
     * and the resolved subject is dispatchable per the receiver's
     * {@code default_subjects} / {@code ssf.notify.<clientId>}
     * configuration. The dispatch outcome (dispatched vs. drop reason)
     * is reported in the response so emitter integrations can debug
     * their wiring without enabling verbose logging.
     *
     * <p>Console-only convenience: callers with {@code manage-clients}
     * on the receiver bypass the {@code allowEmitEvents} opt-in and
     * the role/service-account checks so the admin UI's "Pending
     * Events" tab can drive this endpoint without forcing operators
     * to configure an emit-events role just to use the console.
     *
     * <p>The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/events/emit}.
     */
    @POST
    @Path("clients/{clientId}/events/emit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Emit synthetic SSF event for receiver client",
            description = "Trusted-emitter endpoint that forwards a non-Keycloak-native event (e.g. an LDAP credential change) to the receiver's SSF stream. Looks the receiver up by OAuth clientId (unlike the other admin SSF endpoints which use the internal UUID). Bypasses requireManage; gated by the receiver-configured emit-events role and ssf.allowEmitEvents opt-in."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfEmitEventResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request — malformed payload or unknown event type"),
            @APIResponse(responseCode = "403", description = "Forbidden — receiver did not opt in, role not configured, caller is not a service account, or caller missing role"),
            @APIResponse(responseCode = "404", description = "Receiver client not found or has no SSF stream registered")
    })
    public Response emitEvent(
            @Parameter(description = "OAuth client_id of the receiver (not the internal UUID)")
            @PathParam("clientId") String clientId,
            SsfEmitEventRequest request) {

        ClientModel receiverClient = realm.getClientByClientId(clientId);
        if (receiverClient == null) {
            throw new NotFoundException("Client not found");
        }

        // 0. Admin-console fast path: a caller with manage-clients on
        //    the receiver bypasses the receiver's allowEmitEvents
        //    opt-in, the configured emit-events role check, and the
        //    service-account requirement. The admin already has full
        //    power over the receiver, so insisting they also configure
        //    a dedicated emitter role just to use the admin UI's
        //    "Pending Events" tab would be friction without any added
        //    safety. canManage is non-throwing so the trusted-emitter
        //    path below still runs for unprivileged service accounts.
        boolean adminCaller = auth.clients().canManage(receiverClient);

        if (!adminCaller) {
            // 1. Receiver must have explicitly opted in.
            boolean allowEmit = Boolean.parseBoolean(receiverClient.getAttribute(ClientStreamStore.SSF_ALLOW_EMIT_EVENTS_KEY));
            if (!allowEmit) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new SsfErrorRepresentation("emit_not_allowed",
                                "Receiver has not enabled synthetic event emission"))
                        .build();
            }

            // 2. Receiver must have a non-empty role configured. Empty role
            //    means misconfiguration — refuse rather than silently
            //    accepting unauthenticated emission.
            String configuredRole = receiverClient.getAttribute(ClientStreamStore.SSF_EMIT_EVENTS_ROLE_KEY);
            if (configuredRole == null || configuredRole.isBlank()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new SsfErrorRepresentation("emit_role_not_configured",
                                "Receiver has no emit-events role configured"))
                        .build();
            }

            // 3. Caller must be a service-account token. Strictly M2M —
            //    user-delegated tokens (admin-console sessions, password
            //    grant) shouldn't be able to forge events on behalf of
            //    other users.
            var adminAuth = auth.adminAuth();
            if (adminAuth == null || adminAuth.getUser() == null
                    || adminAuth.getUser().getServiceAccountClientLink() == null) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new SsfErrorRepresentation("not_service_account",
                                "Synthetic event emission requires a service account token"))
                        .build();
            }

            // 4. Caller must hold the configured role — value follows the
            //    same format the admin UI role picker produces: plain
            //    "roleName" checks as a realm role, "clientId.roleName"
            //    checks as a client role on the specified client (usually
            //    the receiver itself, for per-receiver scoping).
            if (!SsfAuthUtil.hasRole(adminAuth.getToken(), configuredRole)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new SsfErrorRepresentation("emit_role_missing",
                                "Caller does not hold the configured emit-events role"))
                        .build();
            }
        }

        // 5. Validate basic payload shape early — emitter service does
        //    a richer check too, but failing fast here gives clearer
        //    errors for missing top-level fields. Admin caller may use
        //    the subjectType/subjectValue shorthand instead of sub_id;
        //    trusted-emitter callers must always supply sub_id.
        if (request == null || request.getEventType() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            "eventType is required"))
                    .build();
        }
        boolean hasShorthand = request.getSubjectType() != null
                && request.getSubjectValue() != null
                && !request.getSubjectValue().isBlank();
        if (request.getSubjectId() == null && (!adminCaller || !hasShorthand)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            adminCaller
                                    ? "Either sub_id or (subjectType, subjectValue) is required"
                                    : "sub_id is required"))
                    .build();
        }

        // 6. Admin shorthand: delegate (subjectType, subjectValue)
        //    resolution to SubjectManagementService, which reuses the
        //    same resolver the /subjects endpoints use and additionally
        //    routes user subjects through the mapper (honoring the
        //    receiver's configured ssf.userSubjectFormat) and org
        //    subjects into a tenant-only complex subject. sub_id wins
        //    if both shapes are present so trusted-emitter flows that
        //    genuinely need a verbatim subject are unaffected.
        SubjectId subjectId = request.getSubjectId();
        if (subjectId == null && adminCaller) {
            StreamConfig stream = streamStore().getStreamForClient(receiverClient);
            if (stream == null) {
                throw new NotFoundException("No SSF stream registered for client");
            }
            try {
                subjectId = subjectManagementService().resolveSubjectForEmit(stream, request.getSubjectType(), request.getSubjectValue());
            } catch (SsfException e) {
                log.debugf(e, "Admin emit subject resolution failed");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new SsfErrorRepresentation("invalid_request", e.getMessage()))
                        .build();
            }
        }

        EmitEventResult emitResult = transmitter.eventEmitterService().emit(
                receiverClient,
                request.getEventType(),
                subjectId,
                request.getEvent());

        log.debugf("SSF synthetic emit. receiverClientId=%s adminCaller=%s eventType=%s status=%s jti=%s",
                receiverClient.getClientId(),
                adminCaller,
                request.getEventType(),
                emitResult.status(),
                emitResult.jti());

        // Map the emitter outcome to an HTTP response. Client-error
        // categories surface as 400 with a category-specific default
        // message (any service-supplied {@code emitResult.message()}
        // wins). Filter-drop categories return 200 like a successful
        // dispatch — the request itself was well-formed and accepted,
        // it just didn't produce a SET on the wire by policy. The
        // explicit listing of every {@link EmitEventStatus} value
        // makes adding a new status a compile-time visible change.
        // Each 4xx case uses the status's wire value as the error code
        // so callers can distinguish categories (unknown_event_type,
        // subject_not_found, ...) from the wire response without having
        // to parse a free-form description.
        String emitMessage = emitResult.message();
        String emitErrorCode = emitResult.status().wireValue();
        switch (emitResult.status()) {
            case INVALID_REQUEST:
                return invalidRequest(emitErrorCode, emitMessage,
                        "Event payload could not be deserialized for the given eventType");
            case INVALID_EVENT_DATA:
                return invalidRequest(emitErrorCode, emitMessage, "Invalid event data");
            case UNKNOWN_EVENT_TYPE:
                return invalidRequest(emitErrorCode, emitMessage, "Unknown eventType");
            case EVENT_TYPE_NOT_EMITTABLE:
                return invalidRequest(emitErrorCode, emitMessage,
                        "Requested event type not emittable");
            case SUBJECT_NOT_FOUND:
                return invalidRequest(emitErrorCode, emitMessage,
                        "Subject referenced by the request does not exist");
            case STREAM_NOT_FOUND:
                // Defensive — the early stream check above usually catches
                // this before emit() runs, but emit() can also return it
                // (e.g. on a stream that was deleted between check and emit).
                return invalidRequest(emitErrorCode, emitMessage,
                        "No SSF stream registered for client");
            case DISPATCHED:
            case DROPPED_UNSUBSCRIBED:
            case DROPPED_FILTERED:
                // Fall through to the shared audit-then-200 path below so
                // every accepted invocation produces an admin event,
                // regardless of whether the SET was actually dispatched
                // or filter-dropped. The drop reason is captured in the
                // audited representation via {@code status}.
                break;
        }

        // Audit the synthetic emission. Logged at the API-call granularity
        // (one entry per accepted /events/emit invocation regardless of
        // whether the SET was DISPATCHED or filter-dropped) so the admin
        // event log mirrors what the operator did, not what the dispatcher
        // chose to do downstream — the latter is captured in the result
        // `status` carried in the representation.
        //
        // Representation is a slim summary (event type, subject reference,
        // result status + jti). We deliberately do NOT include the verbatim
        // event body from the request, which can be arbitrarily large and
        // may carry payload-specific PII; admins who need that detail can
        // still grep the SSF metric / outbox row by jti.
        Map<String, Object> auditRep = createEmitEventAuditRepresentation(request, emitResult);
        UserModel user = auth.adminAuth().getUser();
        adminEvent.operation(OperationType.ACTION)
                .resource(SSF_SYNTHETIC_EVENT_TYPE)
                .resourcePath("ssf", "clients", receiverClient.getClientId(), "events", "emit")
                .authUser(user)
                .representation(auditRep)
                .success();

        return Response.ok(new SsfEmitEventResponse(
                emitResult.status().wireValue(),
                emitResult.jti(),
                emitResult.message())).build();
    }

    protected Map<String, Object> createEmitEventAuditRepresentation(SsfEmitEventRequest request, EmitEventResult emitResult) {
        Map<String, Object> auditRep = new LinkedHashMap<>();
        auditRep.put("eventType", request.getEventType());
        if (request.getSubjectType() != null) {
            auditRep.put("subjectType", request.getSubjectType());
        }
        if (request.getSubjectValue() != null) {
            auditRep.put("subjectValue", request.getSubjectValue());
        }
        auditRep.put("status", emitResult.status().wireValue());
        if (emitResult.jti() != null) {
            auditRep.put("jti", emitResult.jti());
        }
        if (request.getEvent() != null) {
            auditRep.put("eventData", request.getEvent());
        }
        return auditRep;
    }

    /**
     * Looks up a single outbox row by {@code (receiverClient, jti)} so
     * an admin can inspect the delivery state of a specific SET — used
     * by the admin UI's "Pending Events" tab. Returns the
     * operator-visible delivery metadata
     * ({@link SsfEventRepresentation}); the signed encoded SET
     * payload itself is intentionally not exposed because the operator
     * cares about delivery state, not wire bytes.
     *
     * <p>Scoped on the receiver client so an admin cannot probe rows
     * that belong to another receiver via this endpoint — to inspect
     * a different receiver's row, navigate to that receiver in the
     * admin UI first.
     */
    @GET
    @Path("clients/{clientId}/pending-events/{jti}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Lookup pending SSF event by jti",
            description = "Returns the SSF events metadata (status, delivery method, attempts, timestamps, last error) for the SET identified by jti, scoped to the given receiver client."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfEventRepresentation.class))),
            @APIResponse(responseCode = "404", description = "Client or pending event not found")
    })
    public SsfEventRepresentation getPendingEvent(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId,
            @Parameter(description = "JWT id of the SET")
            @PathParam("jti") String jti) {

        ClientModel receiverClient = realm.getClientByClientId(clientId);
        if (receiverClient == null) {
            throw new NotFoundException("Client not found");
        }

        auth.clients().requireView(receiverClient);

        OutboxStore store = transmitter.context().outboxStore(session);
        // jti uniqueness is per-(kind, owner) — look up across both
        // SSF kinds and use whichever matches; in normal operation a
        // given jti only exists under one kind for a given client.
        OutboxEntryEntity entity = null;
        for (String kind : SSF_OUTBOX_KINDS) {
            entity = store.findByOwnerAndCorrelationId(kind, receiverClient.getId(), jti);
            if (entity != null) {
                break;
            }
        }
        if (entity == null) {
            throw new NotFoundException("Pending event not found");
        }
        return toEventRepresentation(entity);
    }

    /**
     * Returns realm-scoped outbox counts and oldest-{@code createdAt}
     * timestamps grouped by status. Lets operators answer "is the
     * outbox draining or accumulating?" without scraping Prometheus
     * or hitting the database directly. Statuses with zero rows are
     * omitted from the response (the underlying SQL {@code GROUP BY}
     * doesn't synthesize zero rows).
     */
    @GET
    @Path("events/stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "SSF event stats for realm",
            description = "Returns counts and oldest-createdAt per SSF event status (PENDING / DELIVERED / DEAD_LETTER / HELD) for this realm."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfEventStatsRepresentation.class)))
    })
    public SsfEventStatsRepresentation getEventStats() {

        auth.realm().requireViewRealm();

        OutboxStore store = transmitter.context().outboxStore(session);
        Map<OutboxEntryStatus, Long> counts = new java.util.EnumMap<>(OutboxEntryStatus.class);
        Map<OutboxEntryStatus, Instant> oldest = new java.util.EnumMap<>(OutboxEntryStatus.class);
        for (String kind : SSF_OUTBOX_KINDS) {
            mergeCounts(counts, store.countStatusesForRealm(kind, realm.getId()));
            mergeOldest(oldest, store.oldestCreatedAtPerStatusForRealm(kind, realm.getId()));
        }
        return toEventStatsRepresentation(counts, oldest);
    }

    /**
     * Bulk-deletes events in this realm filtered by status, with
     * an optional {@code olderThan} ISO-8601 duration that further
     * narrows the delete to rows whose {@code createdAt} is older than
     * {@code now - olderThan}. Surfaced for operators who want to wipe
     * terminal rows ({@code DELIVERED} / {@code DEAD_LETTER}) without
     * waiting for the configured retention windows.
     *
     * <p>The {@code status} parameter is required to keep this from
     * accidentally functioning as a "delete everything in the realm"
     * command — that path already exists in the form of realm removal.
     * Requires {@code manage-realm}.
     */
    @DELETE
    @Path("events")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Bulk-delete SSF events for realm",
            description = "Deletes SSF events in this realm filtered by status, with an optional ISO-8601 olderThan duration. Returns the number of events deleted."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "400", description = "Invalid status or olderThan parameter")
    })
    public Response deleteEvents(
            @Parameter(description = "Event status to delete (PENDING, HELD, DELIVERED, DEAD_LETTER). Required.")
            @QueryParam("status") String statusParam,
            @Parameter(description = "Optional ISO-8601 duration (e.g. PT720H). Only events whose createdAt is older than now - olderThan are deleted.")
            @QueryParam("olderThan") String olderThanParam) {

        auth.realm().requireManageRealm();

        if (statusParam == null || statusParam.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            "status query parameter is required"))
                    .build();
        }
        OutboxEntryStatus status;
        try {
            status = OutboxEntryStatus.valueOf(statusParam.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            "Unknown status: " + statusParam))
                    .build();
        }

        Instant cutoff = null;
        if (olderThanParam != null && !olderThanParam.isBlank()) {
            try {
                Duration olderThan = Duration.parse(olderThanParam.trim());
                if (olderThan.isNegative() || olderThan.isZero()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new SsfErrorRepresentation("invalid_request",
                                    "olderThan must be a positive ISO-8601 duration"))
                            .build();
                }
                cutoff = Instant.now().minus(olderThan);
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new SsfErrorRepresentation("invalid_request",
                                "olderThan must be an ISO-8601 duration (e.g. PT720H): " + olderThanParam))
                        .build();
            }
        }

        OutboxStore store = transmitter.context().outboxStore(session);
        int deleted = 0;
        for (String kind : SSF_OUTBOX_KINDS) {
            deleted += (cutoff == null)
                    ? store.deleteByRealmAndStatus(kind, realm.getId(), status)
                    : store.deleteByRealmAndStatusOlderThan(kind, realm.getId(), status, cutoff);
        }

        // Audit trail — the parameters and result count are all the
        // operator needs to reconstruct what was deleted; the deleted
        // rows themselves are gone.
        Map<String, Object> auditRep = new LinkedHashMap<>();
        auditRep.put("status", status.name());
        if (olderThanParam != null) {
            auditRep.put("olderThan", olderThanParam);
        }
        auditRep.put("deleted", deleted);
        UserModel user = auth.adminAuth().getUser();
        adminEvent.operation(OperationType.DELETE)
                .resource(SSF_SYNTHETIC_EVENT_TYPE)
                .resourcePath("ssf", "events")
                .authUser(user)
                .representation(auditRep)
                .success();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        return Response.ok(body).build();
    }

    /**
     * Bulk-deletes every queued SSF event for this realm in a single
     * round-trip. "Queued" is the server-side
     * {@link OutboxEntryStatus#QUEUED} set (PENDING + HELD) — events that
     * haven't reached a terminal state. Drives the realm-settings
     * disable-on-save flow: the admin UI calls this endpoint *before*
     * persisting {@code ssf.transmitterEnabled=false} so the cleanup
     * runs while the SSF admin paths are still reachable.
     *
     * <p>Requires {@code manage-realm}. Writes a single admin event
     * audit entry rather than the multi-call equivalent so operators
     * can correlate the cleanup with the realm save in one trail.
     */
    @DELETE
    @Path("events/queued")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Delete queued SSF events for realm",
            description = "Deletes every SSF event in this realm whose status is queued (PENDING or HELD) in a single transaction. Returns the number of events deleted."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK")
    })
    public Response deleteQueuedEvents() {

        auth.realm().requireManageRealm();

        OutboxStore store = transmitter.context().outboxStore(session);
        int deleted = 0;
        for (String kind : SSF_OUTBOX_KINDS) {
            deleted += store.deleteQueuedByRealm(kind, realm.getId());
        }

        Map<String, Object> auditRep = new LinkedHashMap<>();
        auditRep.put("deleted", deleted);
        UserModel user = auth.adminAuth().getUser();
        adminEvent.operation(OperationType.DELETE)
                .resource(SSF_SYNTHETIC_EVENT_TYPE)
                .resourcePath("ssf", "events", "queued")
                .authUser(user)
                .representation(auditRep)
                .success();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        return Response.ok(body).build();
    }

    /**
     * Per-receiver counterpart to {@link #getEventStats()}. Returns
     * outbox counts and oldest-{@code createdAt} timestamps grouped by
     * status for a single receiver client. Lets operators answer
     * "which receiver is contributing to the realm-wide backlog?"
     * without falling back to Prometheus or direct database queries.
     */
    @GET
    @Path("clients/{clientId}/events/stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "SSF event stats for receiver client",
            description = "Returns counts and oldest-createdAt per SSF event status (PENDING / DELIVERED / DEAD_LETTER / HELD) for a single receiver client."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SsfEventStatsRepresentation.class))),
            @APIResponse(responseCode = "404", description = "Client not found")
    })
    public SsfEventStatsRepresentation getClientEventStats(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireView(client);

        OutboxStore store = transmitter.context().outboxStore(session);
        Map<OutboxEntryStatus, Long> counts = new java.util.EnumMap<>(OutboxEntryStatus.class);
        Map<OutboxEntryStatus, Instant> oldest = new java.util.EnumMap<>(OutboxEntryStatus.class);
        for (String kind : SSF_OUTBOX_KINDS) {
            mergeCounts(counts, store.countStatusesForOwner(kind, client.getId()));
            mergeOldest(oldest, store.oldestCreatedAtPerStatusForOwner(kind, client.getId()));
        }
        return toEventStatsRepresentation(counts, oldest);
    }

    /**
     * Per-receiver counterpart to {@link #deleteEvents(String, String)}.
     * Bulk-deletes SSF events for a single receiver client filtered by
     * status, with an optional ISO-8601 {@code olderThan} duration.
     * Useful for forensic cleanup of a single receiver's queue
     * (e.g. wiping dead-letters for one flaky integration) without
     * destroying the receiver's stream configuration — the existing
     * cascade-on-stream-delete is the destructive alternative.
     *
     * <p>Requires {@code manage} on the receiver client.
     */
    @DELETE
    @Path("clients/{clientId}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Bulk-delete SSF events for receiver client",
            description = "Deletes SSF events for the given receiver client filtered by status, with an optional ISO-8601 olderThan duration. Returns the number of events deleted."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "400", description = "Invalid status or olderThan parameter"),
            @APIResponse(responseCode = "404", description = "Client not found")
    })
    public Response deleteClientEvents(
            @Parameter(description = "OAuth client_id of the SSF Receiver client")
            @PathParam("clientId") String clientId,
            @Parameter(description = "Event status to delete (PENDING, HELD, DELIVERED, DEAD_LETTER). Required.")
            @QueryParam("status") String statusParam,
            @Parameter(description = "Optional ISO-8601 duration (e.g. PT720H). Only events whose createdAt is older than now - olderThan are deleted.")
            @QueryParam("olderThan") String olderThanParam) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (statusParam == null || statusParam.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            "status query parameter is required"))
                    .build();
        }
        OutboxEntryStatus status;
        try {
            status = OutboxEntryStatus.valueOf(statusParam.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            "Unknown status: " + statusParam))
                    .build();
        }

        Instant cutoff = null;
        if (olderThanParam != null && !olderThanParam.isBlank()) {
            try {
                Duration olderThan = Duration.parse(olderThanParam.trim());
                if (olderThan.isNegative() || olderThan.isZero()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new SsfErrorRepresentation("invalid_request",
                                    "olderThan must be a positive ISO-8601 duration"))
                            .build();
                }
                cutoff = Instant.now().minus(olderThan);
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new SsfErrorRepresentation("invalid_request",
                                "olderThan must be an ISO-8601 duration (e.g. PT720H): " + olderThanParam))
                        .build();
            }
        }

        // Owner-scoped: the OUTBOX_ENTRY OWNER_ID matches the client's
        // internal id (the same identifier dispatcher writes use).
        OutboxStore store = transmitter.context().outboxStore(session);
        int deleted = 0;
        for (String kind : SSF_OUTBOX_KINDS) {
            deleted += (cutoff == null)
                    ? store.deleteByOwnerAndStatus(kind, client.getId(), status)
                    : store.deleteByOwnerAndStatusOlderThan(kind, client.getId(), status, cutoff);
        }

        Map<String, Object> auditRep = new LinkedHashMap<>();
        auditRep.put("status", status.name());
        if (olderThanParam != null) {
            auditRep.put("olderThan", olderThanParam);
        }
        auditRep.put("deleted", deleted);
        UserModel user = auth.adminAuth().getUser();
        adminEvent.operation(OperationType.DELETE)
                .resource(SSF_SYNTHETIC_EVENT_TYPE)
                .resourcePath("ssf", "clients", client.getClientId(), "events")
                .authUser(user)
                .representation(auditRep)
                .success();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        return Response.ok(body).build();
    }

    /**
     * Per-receiver counterpart to {@link #deleteQueuedEvents()}. Bulk-
     * deletes every queued SSF event for one receiver client in a
     * single round-trip. Useful for forensic cleanup of a single
     * receiver's queue without destroying its stream configuration.
     *
     * <p>Requires {@code manage} on the receiver client.
     */
    @DELETE
    @Path("clients/{clientId}/events/queued")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Delete queued SSF events for receiver client",
            description = "Deletes every SSF event for the given receiver client whose status is queued (PENDING or HELD) in a single transaction. Returns the number of events deleted."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "404", description = "Client not found")
    })
    public Response deleteClientQueuedEvents(
            @Parameter(description = "OAuth client_id of the receiver")
            @PathParam("clientId") String clientId) {

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        OutboxStore store = transmitter.context().outboxStore(session);
        int deleted = 0;
        for (String kind : SSF_OUTBOX_KINDS) {
            deleted += store.deleteQueuedByOwner(kind, client.getId());
        }

        Map<String, Object> auditRep = new LinkedHashMap<>();
        auditRep.put("deleted", deleted);
        UserModel user = auth.adminAuth().getUser();
        adminEvent.operation(OperationType.DELETE)
                .resource(SSF_SYNTHETIC_EVENT_TYPE)
                .resourcePath("ssf", "clients", client.getClientId(), "events", "queued")
                .authUser(user)
                .representation(auditRep)
                .success();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        return Response.ok(body).build();
    }

    protected SsfEventRepresentation toEventRepresentation(OutboxEntryEntity entity) {
        SsfEventRepresentation rep = new SsfEventRepresentation();
        rep.setJti(entity.getCorrelationId());
        rep.setEventType(entity.getEntryType());
        rep.setDeliveryMethod(deliveryMethodLabel(entity.getEntryKind()));
        if (entity.getStatus() != null) {
            rep.setStatus(entity.getStatus().name());
        }
        rep.setAttempts(entity.getAttempts());
        if (entity.getCreatedAt() != null) {
            rep.setCreatedAt(entity.getCreatedAt().getEpochSecond());
        }
        if (entity.getNextAttemptAt() != null) {
            rep.setNextAttemptAt(entity.getNextAttemptAt().getEpochSecond());
        }
        if (entity.getDeliveredAt() != null) {
            rep.setDeliveredAt(entity.getDeliveredAt().getEpochSecond());
        }
        rep.setLastError(entity.getLastError());
        rep.setStreamId(entity.getContainerId());
        rep.setDecodedSet(decodeSet(entity.getPayload()));
        // Resolve the user from the same raw JWS payload. Keeps the
        // expensive decode to a single pass even though the admin
        // endpoint exposes both the full SET and the click-through
        // userId separately.
        Map<String, Object> subjectMap = extractSubjectIdFromEncodedSet(entity.getPayload());
        rep.setUserId(resolveUserIdFromSubject(subjectMap));
        return rep;
    }

    /**
     * Translates the row's generic {@code entry_kind} ("ssf-push" /
     * "ssf-poll") to the wire-shape "PUSH" / "POLL" the admin UI
     * already shows under {@code deliveryMethod} on
     * {@link SsfEventRepresentation}. Unknown kinds fall
     * through unchanged so future kinds remain visible.
     */
    protected String deliveryMethodLabel(String entryKind) {
        if (SsfOutboxKinds.PUSH.equals(entryKind)) {
            return "PUSH";
        }
        if (SsfOutboxKinds.POLL.equals(entryKind)) {
            return "POLL";
        }
        return entryKind;
    }

    /**
     * Decodes the encoded SET's JWS payload (no signature check — we
     * signed it ourselves) and returns the full claim set as a plain
     * JSON map so the admin UI can render the Security Event Token
     * verbatim. Returns {@code null} on parse failure.
     */
    protected Map<String, Object> decodeSet(String encodedSet) {
        if (encodedSet == null || encodedSet.isBlank()) {
            return null;
        }
        try {
            JWSInput jws = new JWSInput(encodedSet);
            JsonNode payload = JsonSerialization.readValue(jws.getContent(), JsonNode.class);
            if (!payload.isObject()) {
                return null;
            }
            return SsfUtil.treeToMap(payload);
        } catch (Exception e) {
            log.debugf(e, "Failed to decode encoded SET");
            return null;
        }
    }

    /**
     * Best-effort resolution of the {@code sub_id} to a Keycloak user
     * UUID so the Pending Events lookup can offer a click-through to
     * the user in the admin UI. Rebuilds a typed {@link SubjectId}
     * from the raw map (manually reading the {@code format}
     * discriminator to pick the concrete class — avoids the
     * abstract-class deserializer-dispatch recursion that's the whole
     * reason {@code subjectId} is kept untyped on the wire) and hands
     * it to {@link SubjectResolver}. Returns {@code null} for
     * org-only subjects, subjects whose format isn't user-identifying,
     * or unresolvable user references — the admin UI just omits the
     * click-through in those cases.
     */
    protected String resolveUserIdFromSubject(Map<String, Object> subjectMap) {
        if (subjectMap == null || subjectMap.isEmpty()) {
            return null;
        }
        SubjectId typed = toTypedSubjectId(subjectMap);
        if (typed == null) {
            return null;
        }
        SubjectResolution resolution = SubjectResolver.resolve(session, realm, typed);
        if (resolution instanceof SubjectResolution.User userRes) {
            return userRes.user().getId();
        }
        return null;
    }

    /**
     * Materialises a typed {@link SubjectId} from the raw wire map.
     * When the map carries a {@code format} discriminator (SSF 1.0
     * shape), looks up the concrete class through
     * {@link SubjectIds#getSubjectIdType} and deserialises into it.
     * When there's no {@code format} key (legacy SSE CAEP, where the
     * facets are sibling keys under {@code subject} without an outer
     * marker), falls back to {@link ComplexSubjectId}.
     */
    protected SubjectId toTypedSubjectId(Map<String, Object> subjectMap) {
        Object formatObj = subjectMap.get("format");
        try {
            if (formatObj instanceof String format && !format.isBlank()) {
                Class<? extends SubjectId> cls = SubjectIds.getSubjectIdType(format);
                if (cls == null) {
                    return null;
                }
                return JsonSerialization.mapper.convertValue(subjectMap, cls);
            }
            return JsonSerialization.mapper.convertValue(subjectMap, ComplexSubjectId.class);
        } catch (Exception e) {
            log.debugf(e, "Failed to rehydrate typed SubjectId from map");
            return null;
        }
    }

    /**
     * Decodes the encoded SET's JWS payload (no signature check — we
     * signed it ourselves and the bytes are already in our DB) and
     * returns the subject as a generic JSON map for the admin UI to
     * render verbatim. Two wire shapes are supported:
     *
     * <ul>
     *     <li><b>SSF 1.0</b> — top-level {@code sub_id} claim. Returned
     *         as-is with its {@code format} discriminator.</li>
     *     <li><b>Legacy SSE CAEP</b> — no top-level {@code sub_id}; the
     *         subject lives under {@code events.<type>.subject} with
     *         complex-subject facets (user / session / tenant / …) as
     *         sibling keys and no outer {@code format}. Returned as-is
     *         from there.</li>
     * </ul>
     *
     * <p>Returns {@code null} if the row is missing the encoded SET,
     * the JWS can't be parsed, or neither subject shape is present —
     * the admin UI renders that as "no subject block" rather than
     * failing the lookup. The map is intentionally untyped to skip
     * the {@link org.keycloak.ssf.subject.SubjectId} class hierarchy
     * (whose abstract-class deserializer dispatches via
     * {@code treeToValue} and would loop if invoked recursively here).
     */
    protected Map<String, Object> extractSubjectIdFromEncodedSet(String encodedSet) {
        if (encodedSet == null || encodedSet.isBlank()) {
            return null;
        }
        try {
            JWSInput jws = new JWSInput(encodedSet);
            JsonNode payload = JsonSerialization.readValue(jws.getContent(), JsonNode.class);

            // SSF 1.0: top-level sub_id.
            JsonNode subId = payload.path("sub_id");
            if (!subId.isMissingNode() && !subId.isNull() && subId.isObject()) {
                return SsfUtil.treeToMap(subId);
            }

            // Legacy SSE CAEP: events.<firstEventTypeUri>.subject.
            JsonNode events = payload.path("events");
            if (events.isObject() && events.fieldNames().hasNext()) {
                Map<String, Object> result = SseCaepEventConverter.extractSseSubjectIdMap(events);
                if (result != null) {
                    return result;
                }
            }

            return null;
        } catch (Exception e) {
            log.debugf(e, "Failed to extract sub_id from encoded SET");
            return null;
        }
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
}
