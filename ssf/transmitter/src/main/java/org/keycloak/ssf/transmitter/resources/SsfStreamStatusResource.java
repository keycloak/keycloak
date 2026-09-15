package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.transmitter.stream.StreamService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.ssf.transmitter.support.SsfErrorRepresentation;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * Endpoint for managing SSF stream status.
 */
public class SsfStreamStatusResource {

    private static final Logger log = Logger.getLogger(SsfStreamStatusResource.class);

    protected final KeycloakSession session;

    protected final StreamService streamService;

    public SsfStreamStatusResource(KeycloakSession session, StreamService streamService) {
        this.session = session;
        this.streamService = streamService;
    }

    /**
     * Gets the status of a stream.
     *
     * @param streamId The stream ID
     * @return The stream status
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Get stream status",
            description = "Returns the current status of the given stream (SSF 1.0 §7.1.2.1)."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StreamStatus.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found")
    })
    public Response getStreamStatus(
            @Parameter(description = "Identifier of the stream whose status to return.")
            @QueryParam("stream_id") String streamId) {

        if (!SsfAuthUtil.canRead()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (streamId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", "Stream ID is required"))
                    .build();
        }

        if (!isClientStream(streamId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            StreamStatus streamStatus = streamService.getStreamStatus(streamId);

            if (streamStatus != null) {
                return Response.ok(streamStatus).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error getting stream status", e);
            return Response.serverError().build();
        }
    }

    /**
     * Updates the status of a stream.
     *
     * @param streamStatus The updated stream status
     * @return The updated stream status
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Update stream status",
            description = "Updates the status of the given stream, e.g. to pause or disable it (SSF 1.0 §7.1.2.2)."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StreamStatus.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found")
    })
    public Response updateStreamStatus(StreamStatus streamStatus) {

        if (!SsfAuthUtil.canManage()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (streamStatus.getStreamId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", "Stream ID is required"))
                    .build();
        }

        if (!isClientStream(streamStatus.getStreamId())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            StreamStatus updatedStatus = streamService.updateStreamStatus(streamStatus);

            if (updatedStatus != null) {
                return Response.ok(updatedStatus).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error updating stream status", e);
            return Response.serverError().build();
        }
    }

    protected boolean isClientStream(String streamId) {
        ClientModel client = session.getContext().getClient();
        String clientStreamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
        if (clientStreamId == null || !clientStreamId.equals(streamId)) {
            log.debugf("Stream access denied. clientId=%s requestedStreamId=%s clientStreamId=%s", client.getClientId(), streamId, clientStreamId);
            return false;
        }
        return true;
    }
}
