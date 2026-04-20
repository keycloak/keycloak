package org.keycloak.ssf.transmitter.resources;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.transmitter.stream.DuplicateStreamConfigException;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfigInputRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamConfigUpdateRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.ssf.transmitter.support.SsfErrorRepresentation;
import org.keycloak.util.JsonSerialization;

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
 * Endpoint for managing SSF streams.
 */
public class SsfStreamManagementResource {

    private static final Logger log = Logger.getLogger(SsfStreamManagementResource.class);

    protected final KeycloakSession session;

    protected final StreamService streamService;

    public SsfStreamManagementResource(KeycloakSession session, StreamService streamService) {
        this.session = session;
        this.streamService = streamService;
    }

    /**
     * Creates a new stream.
     *
     * @param streamConfig The stream configuration
     * @return The created stream configuration
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Create stream",
            description = "Creates a new SSF stream for the authenticated receiver client (SSF 1.0 §7.1.1.1)."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = StreamConfig.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "409", description = "Duplicate stream configuration")
    })
    public Response createStream(StreamConfigInputRepresentation input) {

        if (!SsfAuthUtil.canManage()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            ClientModel receiverClient = session.getContext().getClient();
            StreamConfig createdStream = streamService.createStream(input, receiverClient);
            log.debugf("Created stream: %s", JsonSerialization.writeValueAsPrettyString(createdStream));
            var responseBuilder = Response.status(Response.Status.CREATED);
            return responseBuilder.entity(createdStream)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        } catch (DuplicateStreamConfigException dsce) {
            log.debugf(dsce, "Error creating stream: Duplicate stream configuration");
            return Response.status(Response.Status.CONFLICT)
                    .entity(new SsfErrorRepresentation("stream_error", dsce.getMessage()))
                    .build();
        } catch (SsfException e) {
            log.debugf(e, "Rejected stream create: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.errorf(e, "Error creating stream");
            return Response.serverError()
                    .entity(new SsfErrorRepresentation("stream_error", "Failed to create stream"))
                    .build();
        }
    }

    /**
     * Gets a stream by ID.
     *
     * @param streamId The stream ID
     * @return The stream configuration
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Get stream(s)",
            description = "Returns the stream for the given stream_id or, if stream_id is omitted, all streams owned by the authenticated receiver client (SSF 1.0 §7.1.1.2)."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StreamConfig.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found")
    })
    public Response getStream(
            @Parameter(description = "Identifier of the stream to return. If omitted, all streams for the authenticated client are returned.")
            @QueryParam("stream_id") String streamId) {

        if (!SsfAuthUtil.canRead()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            if (streamId != null) {
                return getStreamById(streamId);
            }

            return Response.ok(getStreams())
                    .build();
        } catch (Exception e) {
            log.errorf(e, "Error getting stream with streamId=%s", streamId);
            return Response.serverError()
                    .entity(new SsfErrorRepresentation("stream_error", "Failed to load stream"))
                    .build();
        }
    }

    protected List<StreamConfig> getStreams() {
        return streamService.getStreamsByClient(session.getContext().getClient());
    }

    protected Response getStreamById(String streamId) {
        if (!isCurrentClientStream(streamId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        StreamConfig stream = streamService.getStream(streamId);
        if (stream != null) {
            return Response.ok(stream).build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Updates a stream.
     *
     * @param streamConfig The updated stream configuration
     * @return The updated stream configuration
     */
    @PATCH
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Update stream",
            description = "Partially updates a stream configuration (SSF 1.0 §7.1.1.3)."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StreamConfig.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found")
    })
    public Response updateStream(StreamConfigUpdateRepresentation update) {

        if (!SsfAuthUtil.canManage()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (update == null || update.getStreamId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", "Stream ID is required"))
                    .build();
        }

        String streamId = update.getStreamId();

        if (!isCurrentClientStream(streamId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            StreamConfig updatedStream = streamService.updateStream(update);

            if (updatedStream == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(updatedStream).build();
        } catch (SsfException e) {
            log.debugf(e, "Rejected stream update streamId=%s: %s", streamId, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.errorf(e, "Error updating stream streamId=%s", streamId);
            return Response.serverError()
                    .entity(new SsfErrorRepresentation("stream_error", "Failed to update stream"))
                    .build();
        }
    }

    /**
     * Replace a stream.
     *
     * @param streamConfig The updated stream configuration
     * @return The updated stream configuration
     */
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Replace stream",
            description = "Replaces the full stream configuration (SSF 1.0 §7.1.1.4)."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StreamConfig.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found")
    })
    public Response replaceStream(StreamConfigUpdateRepresentation update) {

        if (!SsfAuthUtil.canManage()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (update == null || update.getStreamId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", "Stream ID is required"))
                    .build();
        }

        String streamId = update.getStreamId();

        if (!isCurrentClientStream(streamId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            StreamConfig replacedStream = streamService.replaceStream(update);

            if (replacedStream == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(replacedStream).build();
        } catch (SsfException e) {
            log.debugf(e, "Rejected stream replace streamId=%s: %s", streamId, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.errorf(e, "Error replacing stream streamId=%s", streamId);
            return Response.serverError()
                    .entity(new SsfErrorRepresentation("stream_error", "Error updating stream"))
                    .build();
        }
    }

    /**
     * Deletes a stream.
     *
     * @param streamId The stream ID
     * @return A response indicating success or failure
     */
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Delete stream",
            description = "Deletes an SSF stream (SSF 1.0 §7.1.1.5). If stream_id is omitted, the caller's stored stream id is used."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found")
    })
    public Response deleteStream(
            @Parameter(description = "Identifier of the stream to delete. If omitted, the caller's stored stream id is used.")
            @QueryParam("stream_id") String streamId) {

        if (!SsfAuthUtil.canManage()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (streamId == null) {
            // try to use stored streamId from client attributes
            ClientModel client = session.getContext().getClient();
            streamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
        }

        if (streamId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", "Stream ID is required"))
                    .build();
        }

        if (!isCurrentClientStream(streamId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            boolean deleted = streamService.deleteStream(streamId);

            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            ClientModel client = session.getContext().getClient();
            if (client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY) != null) {
                client.removeAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
            }

            return Response.noContent().build();
        } catch (Exception e) {
            log.errorf(e, "Error deleting stream streamId=%s", streamId);
            return Response.serverError()
                    .entity(new SsfErrorRepresentation("stream_error", "Error deleting stream"))
                    .build();
        }
    }

    /**
     * Determines if the given stream ID matches the currently authenticated client's stream ID.
     *
     * @param streamId The stream ID to be validated against the client's associated stream ID.
     * @return {@code true} if the given stream ID matches the client's associated stream ID;
     *         {@code false} otherwise.
     */
    protected boolean isCurrentClientStream(String streamId) {
        ClientModel client = session.getContext().getClient();
        String clientStreamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
        if (clientStreamId == null || !clientStreamId.equals(streamId)) {
            log.debugf("Stream access denied. clientId=%s requestedStreamId=%s clientStreamId=%s", client.getClientId(), streamId, clientStreamId);
            return false;
        }
        return true;
    }
}
