package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.ssf.transmitter.delivery.poll.PollDeliveryService;
import org.keycloak.ssf.transmitter.delivery.poll.PollErrorRepresentation;
import org.keycloak.ssf.transmitter.delivery.poll.PollRequest;
import org.keycloak.ssf.transmitter.delivery.poll.PollResponse;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;

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
 * RFC 8936 Poll-Based SET Delivery endpoint.
 *
 * <p>Mounted from {@link SsfTransmitterResource} at
 * {@code receivers/{clientId}/streams/{streamId}/poll} so the wire URL
 * matches the {@code delivery.endpoint_url} the transmitter writes back
 * into the stream-create response per SSF §6.1.2.
 *
 * <p>Authorization layers:
 * <ol>
 *     <li>{@link SsfAuthUtil#canRead()} — bearer token valid, ssf
 *         enabled on the calling client, {@code ssf.read} scope present,
 *         optional service-account / required-role checks.</li>
 *     <li>Path-vs-token ownership: {@code {clientId}} from the URL must
 *         match the bearer token's resolved client's clientId, and
 *         {@code {streamId}} must equal that client's registered stream
 *         id. Mismatch on either field collapses to a single silent
 *         {@code 404 stream_not_found} response so the URL surface
 *         doesn't leak which clients / streams exist.</li>
 * </ol>
 */
public class SsfStreamPollResource {

    private static final Logger log = Logger.getLogger(SsfStreamPollResource.class);

    protected final KeycloakSession session;

    protected final SsfStreamStore streamStore;

    protected final PollDeliveryService pollDeliveryService;

    public SsfStreamPollResource(KeycloakSession session, SsfStreamStore streamStore, PollDeliveryService pollDeliveryService) {
        this.session = session;
        this.streamStore = streamStore;
        this.pollDeliveryService = pollDeliveryService;
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Poll for pending events",
            description = "RFC 8936 polling endpoint. Acks the receiver's previously-acknowledged events (via the `ack` array) and returns the next batch of pending Security Event Tokens for the stream."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PollResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — caller lacks ssf.read scope or required role"),
            @APIResponse(responseCode = "404", description = "Stream not found, or path components don't belong to the calling client (silent — no enumeration oracle)")
    })
    public Response poll(
            @Parameter(description = "OAuth client_id of the receiver (must match the bearer token's client)")
            @PathParam("clientId") String clientId,
            @Parameter(description = "Identifier of the stream the receiver wants to poll (must belong to the calling client)")
            @PathParam("streamId") String streamId,
            PollRequest request) {

        // 1. Standard SSF receiver-facing auth — same gate the other
        //    transmitter endpoints use. Returns 401 on a bad token,
        //    missing scope, etc.
        if (!SsfAuthUtil.canRead()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        ClientModel callerClient = session.getContext().getClient();

        // 2. Path-vs-token ownership check. Both mismatches (client and
        //    stream) collapse to the same silent 404 to avoid telling
        //    a probing caller which clientIds / streamIds exist on the
        //    transmitter.
        if (callerClient == null
                || clientId == null
                || !clientId.equals(callerClient.getClientId())) {
            log.debugf("SSF poll denied: path clientId mismatch. pathClientId=%s tokenClientId=%s",
                    clientId, callerClient == null ? null : callerClient.getClientId());
            return streamNotFound();
        }

        String registeredStreamId = callerClient.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
        if (registeredStreamId == null || !registeredStreamId.equals(streamId)) {
            log.debugf("SSF poll denied: stream id mismatch. pathStreamId=%s registeredStreamId=%s clientId=%s",
                    streamId, registeredStreamId, callerClient.getClientId());
            return streamNotFound();
        }

        // 3. Stream must actually exist and be owned by this client.
        //    Belt-and-braces against a stale stream id attribute.
        StreamConfig stream = lookupStream(callerClient);
        if (stream == null || !streamId.equals(stream.getStreamId())) {
            log.debugf("SSF poll denied: stream lookup failed for clientId=%s streamId=%s",
                    callerClient.getClientId(), streamId);
            return streamNotFound();
        }

        PollRequest body = request != null ? request : new PollRequest();

        // 4. Cap the batch size of ack and setErrs at MAX_BATCH_CAP
        //    (1000 each). Bounds the request payload + the
        //    per-(client, jti) IN-clause query that follows. Receivers
        //    that need to ack/NACK more than the cap split into
        //    multiple polls.
        if (body.getAck() != null && body.getAck().size() > PollDeliveryService.MAX_BATCH_CAP) {
            return invalidRequest("ack array exceeds " + PollDeliveryService.MAX_BATCH_CAP
                    + " entries — split into multiple polls");
        }
        if (body.getSetErrs() != null && body.getSetErrs().size() > PollDeliveryService.MAX_BATCH_CAP) {
            return invalidRequest("setErrs object exceeds " + PollDeliveryService.MAX_BATCH_CAP
                    + " entries — split into multiple polls");
        }

        PollResponse response = pollDeliveryService.poll(callerClient, body);
        return Response.ok(response).build();
    }

    protected Response invalidRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new PollErrorRepresentation("invalid_request", message))
                .build();
    }

    protected StreamConfig lookupStream(ClientModel callerClient) {
        return streamStore.getStreamForClient(callerClient);
    }

    protected Response streamNotFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new PollErrorRepresentation("stream_not_found", "Stream not found"))
                .build();
    }
}
