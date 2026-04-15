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
import org.keycloak.ssf.Ssf;
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
    public Response createStream(StreamConfigInputRepresentation input) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
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
            log.errorf(dsce, "Error creating stream: Duplicate stream configuration");
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
        List<StreamConfig> streams = streamService.getStreamsByClient(session.getContext().getClient());
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
    public Response updateStream(StreamConfigUpdateRepresentation update) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
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
    public Response replaceStream(StreamConfigUpdateRepresentation update) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
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
    public Response deleteStream(@QueryParam("stream_id") String streamId) {

        if (!SsfAuthUtil.hasScope(Ssf.SCOPE_SSF_MANAGE)) {
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
