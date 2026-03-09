package org.keycloak.protocol.ssf.transmitter.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import org.keycloak.protocol.ssf.stream.StreamStatus;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;

/**
 * Endpoint for managing SSF stream status.
 */
public class StreamStatusResource {

    private static final Logger log = Logger.getLogger(StreamStatusResource.class);

    private final StreamService streamService;

    public StreamStatusResource(StreamService streamService) {
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
    public Response getStreamStatus(@QueryParam("stream_id") String streamId) {

        if (streamId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Stream ID is required").build();
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
    public Response updateStreamStatus(StreamStatus streamStatus) {

        if (streamStatus.getStreamId() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Stream ID is required").build();
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
}
