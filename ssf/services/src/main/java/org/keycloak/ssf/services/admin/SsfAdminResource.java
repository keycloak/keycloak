package org.keycloak.ssf.services.admin;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.admin.SsfAdminSubjectRequest;
import org.keycloak.ssf.transmitter.admin.SsfAdminSubjectResponse;
import org.keycloak.ssf.transmitter.admin.SsfClientStreamRepresentation;
import org.keycloak.ssf.transmitter.admin.SsfConfigRepresentation;
import org.keycloak.ssf.transmitter.stream.DuplicateStreamConfigException;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfigInputRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamVerificationRequest;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.AdminSubjectResult;
import org.keycloak.ssf.transmitter.subject.SubjectManagementResult;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;
import org.keycloak.ssf.transmitter.support.SsfErrorRepresentation;

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

    protected final KeycloakSession session;

    protected final RealmModel realm;

    protected final AdminPermissionEvaluator auth;

    protected final AdminEventBuilder adminEvent;

    protected final SsfTransmitterProvider transmitter;

    public SsfAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, SsfTransmitterProvider transmitter) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.transmitter = transmitter;
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
        // Both fields are exposed as aliases so the admin UI can render a
        // human-readable selection list and pre-select the defaults against
        // the same option values. Both are filtered to events the transmitter
        // can actually emit — events contributed purely for inbound parsing
        // on the receiver side are intentionally excluded.
        config.setDefaultSupportedEvents(toEventAliases(transmitter, transmitter.getDefaultSupportedEvents()));
        config.setAvailableSupportedEvents(transmitter.getEmittableEventAliases());
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
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientIdentifier}/stream}
     */
    @GET
    @Path("clients/{clientIdentifier}/stream")
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
            @Parameter(description = "Internal client UUID (not the OAuth client_id)")
            @PathParam("clientIdentifier") String clientIdentifier) {

        auth.realm().requireViewRealm();

        ClientModel client = realm.getClientById(clientIdentifier);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        StreamConfig streamConfig = transmitter.streamStore().getStreamForClient(client);
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
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientIdentifier}/stream}.
     */
    @POST
    @Path("clients/{clientIdentifier}/stream")
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
            @Parameter(description = "Internal client UUID (not the OAuth client_id)")
            @PathParam("clientIdentifier") String clientIdentifier,
            StreamConfigInputRepresentation input) {

        ClientModel client = realm.getClientById(clientIdentifier);
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
            log.debugf(dsce, "Admin stream create rejected for client %s: duplicate stream", clientIdentifier);
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                    .entity(new SsfErrorRepresentation("stream_error", dsce.getMessage()))
                    .build());
        } catch (SsfException e) {
            log.debugf(e, "Admin stream create rejected for client %s: %s", clientIdentifier, e.getMessage());
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
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientIdentifier}/stream/verify}.
     */
    @POST
    @Path("clients/{clientIdentifier}/stream/verify")
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
            @Parameter(description = "Internal client UUID (not the OAuth client_id)")
            @PathParam("clientIdentifier") String clientIdentifier) {

        ClientModel client = realm.getClientById(clientIdentifier);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        auth.clients().requireManage(client);

        StreamConfig streamConfig = transmitter.streamStore().getStreamForClient(client);
        if (streamConfig == null) {
            throw new NotFoundException("No SSF stream registered for client");
        }

        StreamVerificationRequest verificationRequest = new StreamVerificationRequest();
        verificationRequest.setStreamId(streamConfig.getStreamId());
        // Per SSF §8.1.4.2-5 a transmitter-initiated verification MUST NOT
        // include a state nonce — only receiver-initiated requests may set
        // one — so we leave the state null here.

        boolean triggered = transmitter.verificationService().triggerVerification(verificationRequest);
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
     * Deletes the currently registered SSF stream for a receiver client so the
     * receiver can re-register with a fresh configuration. Returns 204 on
     * success, 404 if the client does not exist or has no registered stream.
     *
     * The endpoint is available via
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientIdentifier}/stream}
     */
    @DELETE
    @Path("clients/{clientIdentifier}/stream")
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
            @Parameter(description = "Internal client UUID (not the OAuth client_id)")
            @PathParam("clientIdentifier") String clientIdentifier) {

        ClientModel client = realm.getClientById(clientIdentifier);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        auth.clients().requireManage(client);

        boolean deleted = transmitter.streamStore().deleteStreamForClient(client);
        if (!deleted) {
            throw new NotFoundException("No SSF stream registered for client");
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
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientIdentifier}/subjects/add}.
     */
    @POST
    @Path("clients/{clientIdentifier}/subjects/add")
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
            @Parameter(description = "Internal client UUID (not the OAuth client_id)")
            @PathParam("clientIdentifier") String clientIdentifier,
            SsfAdminSubjectRequest request) {

        ClientModel client = realm.getClientById(clientIdentifier);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (request == null || request.getType() == null || request.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "type and value are required"))
                    .build();
        }


        SubjectManagementService svc = transmitter.subjectManagementService();
        AdminSubjectResult result = svc.addSubjectByAdmin(clientIdentifier, request.getType(), request.getValue());

        if (result.result() == SubjectManagementResult.OK) {
            return Response.ok(new SsfAdminSubjectResponse("added", result.entityType(), result.entityId())).build();
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
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientIdentifier}/subjects/remove}.
     */
    @POST
    @Path("clients/{clientIdentifier}/subjects/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF)
    @Operation(
            summary = "Remove subject from client notification scope",
            description = "Removes a subject from a receiver client's notification scope. Resolves the subject and clears the ssf.notify.<clientId> attribute from the resolved entity."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Client or subject not found")
    })
    public Response removeSubject(
            @Parameter(description = "Internal client UUID (not the OAuth client_id)")
            @PathParam("clientIdentifier") String clientIdentifier,
            SsfAdminSubjectRequest request) {

        ClientModel client = realm.getClientById(clientIdentifier);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (request == null || request.getType() == null || request.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "type and value are required"))
                    .build();
        }

        SubjectManagementService svc = transmitter.subjectManagementService();
        AdminSubjectResult result = svc.removeSubjectByAdmin(clientIdentifier, request.getType(), request.getValue());

        if (result.result() == SubjectManagementResult.OK) {
            return Response.noContent().build();
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
     * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientIdentifier}/subjects/ignore}.
     */
    @POST
    @Path("clients/{clientIdentifier}/subjects/ignore")
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
            @Parameter(description = "Internal client UUID (not the OAuth client_id)")
            @PathParam("clientIdentifier") String clientIdentifier,
            SsfAdminSubjectRequest request) {

        ClientModel client = realm.getClientById(clientIdentifier);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        auth.clients().requireManage(client);

        if (request == null || request.getType() == null || request.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "type and value are required"))
                    .build();
        }

        SubjectManagementService svc = transmitter.subjectManagementService();
        AdminSubjectResult result = svc.ignoreSubjectByAdmin(clientIdentifier, request.getType(), request.getValue());

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
