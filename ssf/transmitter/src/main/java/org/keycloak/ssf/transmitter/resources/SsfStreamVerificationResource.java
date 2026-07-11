package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.stream.StreamVerificationRequest;
import org.keycloak.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.ssf.transmitter.support.SsfErrorRepresentation;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * Endpoint for SSF stream verification.
 */
public class SsfStreamVerificationResource {

    private static final Logger log = Logger.getLogger(SsfStreamVerificationResource.class);

    protected final KeycloakSession session;

    protected final StreamVerificationService verificationService;

    protected final SsfTransmitterConfig transmitterConfig;

    protected final ClientStreamStore clientStreamStore;

    protected final SsfMetricsBinder metricsBinder;

    public SsfStreamVerificationResource(KeycloakSession session, StreamVerificationService verificationService,
                                         SsfTransmitterConfig transmitterConfig, ClientStreamStore clientStreamStore,
                                         SsfMetricsBinder metricsBinder) {
        this.session = session;
        this.verificationService = verificationService;
        this.transmitterConfig = transmitterConfig;
        this.clientStreamStore = clientStreamStore;
        this.metricsBinder = metricsBinder;
    }

    /**
     * Triggers a verification event for a stream.
     *
     * @param verificationRequest The verification request
     * @return A response indicating success or failure
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Trigger stream verification",
            description = "Triggers the transmitter to push a verification Security Event Token (SET) to the stream's delivery endpoint (SSF 1.0 §7.1.3)."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found"),
            @APIResponse(responseCode = "429", description = "Too Many Requests — minimum verification interval not yet elapsed")
    })
    public Response triggerVerification(StreamVerificationRequest verificationRequest) {
        try {

            if (!SsfAuthUtil.canManage()) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String streamId = verificationRequest.getStreamId();
            ClientModel client = session.getContext().getClient();
            if (streamId == null) {
                // use streamId from client attributes
                streamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
                verificationRequest.setStreamId(streamId);
            }

            if (streamId == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new SsfErrorRepresentation("stream_verification_error", "Stream ID is required"))
                        .build();
            }

            // Ensure the receiver client can only verify its own stream
            String clientStreamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
            if (clientStreamId == null || !clientStreamId.equals(streamId)) {
                log.debugf("Stream verification denied. clientId=%s requestedStreamId=%s clientStreamId=%s", client.getClientId(), streamId, clientStreamId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new SsfErrorRepresentation("stream_verification_error", "Stream not found"))
                        .build();
            }

            try {
                checkMinVerificationInterval(client);
            } catch (WebApplicationException rateLimited) {
                if (rateLimited.getResponse() != null
                        && rateLimited.getResponse().getStatus() == Response.Status.TOO_MANY_REQUESTS.getStatusCode()) {
                    metricsBinder.recordVerification(currentRealmName(),
                            client.getClientId(),
                            SsfMetricsBinder.VerificationInitiator.RECEIVER,
                            SsfMetricsBinder.VerificationOutcome.RATE_LIMITED,
                            null);
                }
                throw rateLimited;
            }

            boolean success = verificationService.triggerVerification(verificationRequest,
                    SsfMetricsBinder.VerificationInitiator.RECEIVER);

            if (!success) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new SsfErrorRepresentation("stream_verification_error", "Stream verification failed"))
                        .build();
            }

            return Response.noContent()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        } catch (WebApplicationException wae) {
            // Let JAX-RS mapped status responses (e.g. 429 from the min
            // verification interval check) propagate unchanged instead of
            // rewriting them to a generic 500.
            throw wae;
        } catch (Exception e) {
            log.error("Error triggering verification", e);
            return Response.serverError().build();
        }
    }

    protected String currentRealmName() {
        try {
            return session.getContext().getRealm().getName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    protected void checkMinVerificationInterval(ClientModel client) {

        // Per-stream / per-client override takes precedence over the
        // transmitter-wide default. The stream config includes overlays
        // applied by ClientStreamStore.applyReceiverAttributeOverlays
        // (reads ssf.minVerificationInterval from the client attribute).
        var streamConfig = clientStreamStore.getStreamForClient(client);
        Integer streamInterval = streamConfig != null ? streamConfig.getMinVerificationInterval() : null;

        int minVerificationIntervalSeconds = streamInterval != null
                ? streamInterval
                : transmitterConfig.getMinVerificationIntervalSeconds();

        if (minVerificationIntervalSeconds <= 0) {
            // Rate limiting disabled.
            return;
        }

        String lastVerifiedAt = client.getAttribute(ClientStreamStore.SSF_LAST_VERIFIED_AT_KEY);
        long currentTime = Time.currentTime();

        if (lastVerifiedAt != null) {
            long lastVerifiedAtTime = Long.parseLong(lastVerifiedAt);
            long timeSinceLastVerification = currentTime - lastVerifiedAtTime;
            if (timeSinceLastVerification < minVerificationIntervalSeconds) {
                throw new WebApplicationException(Response.status(Response.Status.TOO_MANY_REQUESTS)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(new SsfErrorRepresentation("too_many_requests",
                                "Wait at least " + (minVerificationIntervalSeconds - timeSinceLastVerification)
                                        + " seconds before triggering another verification"))
                        .build());
            }
        }

        client.setAttribute(ClientStreamStore.SSF_LAST_VERIFIED_AT_KEY, String.valueOf(currentTime));
    }
}
