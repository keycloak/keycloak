package org.keycloak.protocol.ssf.transmitter.resources;

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
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.support.SsfAuthUtil;
import org.keycloak.protocol.ssf.support.SsfErrorRepresentation;
import org.keycloak.protocol.ssf.transmitter.stream.DuplicateStreamConfigException;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;
import org.keycloak.protocol.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * Endpoint for managing SSF streams.
 */
public class StreamManagementResource {

    private static final Logger log = Logger.getLogger(StreamManagementResource.class);

    private final StreamService streamService;

    public StreamManagementResource(StreamService streamService) {
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
    public Response createStream(StreamConfig streamConfig) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            StreamConfig createdStream = streamService.createStream(streamConfig);
            log.debugf("Created stream: %s", JsonSerialization.writeValueAsPrettyString(createdStream));
            var responseBuilder = Response.status(Response.Status.CREATED);
            return responseBuilder.entity(createdStream)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        } catch (DuplicateStreamConfigException dsce) {
            log.errorf(dsce, "Error creating stream: Duplicate stream configuration");
            return Response.status(Response.Status.CONFLICT)
                    .entity(new SsfErrorRepresentation("stream_error", dsce.getMessage()))
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
    public Response getStream(@QueryParam("stream_id") String streamId) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_READ)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            if (streamId != null) {
                return getStreamById(streamId);
            }

            return getStreams();
        } catch (Exception e) {
            log.errorf(e, "Error getting stream with streamId=%s", streamId);
            return Response.serverError()
                    .entity(new SsfErrorRepresentation("stream_error", "Failed to load stream"))
                    .build();
        }
    }

    protected Response getStreams() {
        List<StreamConfig> streams = streamService.getAvailableStreams();
        return Response.ok(streams).build();
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
    public Response updateStream(StreamConfig streamConfig) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (streamConfig == null || streamConfig.getStreamId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", "Stream ID is required"))
                    .build();
        }

        if (!isCurrentClientStream(streamConfig.getStreamId())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            StreamConfig updatedStream = streamService.updateStream(streamConfig);

            if (updatedStream == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(updatedStream).build();
        } catch (Exception e) {
            log.errorf(e,"Error updating stream steamId=%s", streamConfig.getStreamId());
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
    public Response replaceStream(StreamConfig streamConfig) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (streamConfig == null || streamConfig.getStreamId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("stream_error", "Stream ID is required"))
                    .build();
        }

        if (!isCurrentClientStream(streamConfig.getStreamId())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            StreamConfig replacedStream = streamService.replaceStream(streamConfig);

            if (replacedStream == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(replacedStream).build();
        } catch (Exception e) {
            log.errorf(e,"Error replacing stream steamId=%s", streamConfig.getStreamId());
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
    public Response deleteStream(@QueryParam("stream_id") String streamId) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (streamId == null) {
            // try to use stored streamId from client attributes
            ClientModel client = KeycloakSessionUtil.getKeycloakSession().getContext().getClient();
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

            ClientModel client = KeycloakSessionUtil.getKeycloakSession().getContext().getClient();
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
        ClientModel client = KeycloakSessionUtil.getKeycloakSession().getContext().getClient();
        String clientStreamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
        if (clientStreamId == null || !clientStreamId.equals(streamId)) {
            log.debugf("Stream access denied. clientId=%s requestedStreamId=%s clientStreamId=%s", client.getClientId(), streamId, clientStreamId);
            return false;
        }
        return true;
    }
}
