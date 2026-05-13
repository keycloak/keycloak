package org.keycloak.ssf.transmitter.stream;

import java.time.Duration;
import java.time.Instant;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;

import org.jboss.logging.Logger;

/**
 * Service for handling SSF stream verification.
 */
public class StreamVerificationService {

    protected static final Logger log = Logger.getLogger(StreamVerificationService.class);

    protected final KeycloakSession session;

    protected final SsfStreamStore streamStore;

    protected final SecurityEventTokenMapper mapper;

    protected final SecurityEventTokenDispatcher dispatcher;

    protected final SsfMetricsBinder metricsBinder;

    public StreamVerificationService(KeycloakSession session, SsfStreamStore streamStore,
                                     SecurityEventTokenMapper mapper,
                                     SecurityEventTokenDispatcher dispatcher,
                                     SsfMetricsBinder metricsBinder) {
        this.session = session;
        this.streamStore = streamStore;
        this.mapper = mapper;
        this.dispatcher = dispatcher;
        this.metricsBinder = metricsBinder == null ? SsfMetricsBinder.NOOP : metricsBinder;
    }

    /**
     * Back-compat overload defaulting the {@code initiator} label to
     * {@link SsfMetricsBinder.VerificationInitiator#RECEIVER receiver}.
     * Prefer {@link #triggerVerification(StreamVerificationRequest,
     * SsfMetricsBinder.VerificationInitiator)} so the metrics
     * {@code initiator} label correctly distinguishes admin /
     * transmitter-initiated dispatches.
     */
    public boolean triggerVerification(StreamVerificationRequest verificationRequest) {
        return triggerVerification(verificationRequest,
                SsfMetricsBinder.VerificationInitiator.RECEIVER);
    }

    /**
     * Triggers a verification event for a stream.
     *
     * <p>Serves as the single centralized entry point for all verification
     * flows — receiver-initiated (via {@code POST /streams/verify}),
     * admin-initiated (via the SSF admin resource), and transmitter-initiated
     * automatic post-create dispatch. Stamps {@code ssf.lastVerifiedAt}
     * on the receiver client for the explicit paths (receiver / admin)
     * only — transmitter-initiated auto-fires are skipped so they do not
     * consume the {@code min_verification_interval} rate-limit window.
     *
     * @param verificationRequest The verification request
     * @param initiator           Which entry point invoked this dispatch — used
     *                            as the {@code initiator} label on
     *                            {@code keycloak.ssf.verification.requests}.
     * @return true if the verification was triggered, false if the stream was not found
     */
    public boolean triggerVerification(StreamVerificationRequest verificationRequest,
                                       SsfMetricsBinder.VerificationInitiator initiator) {
        String streamId = verificationRequest.getStreamId();
        StreamConfig stream = streamStore.findStreamById(streamId);

        if (stream == null) {
            log.warnf("Stream not found for verification. streamId=%s", streamId);
            // Record the FAILED outcome with whatever realm/client we
            // can observe — the stream is gone so client_id is unknown.
            metricsBinder.recordVerification(currentRealmName(), "unknown",
                    initiator, SsfMetricsBinder.VerificationOutcome.FAILED, null);
            return false;
        }

        log.debugf("Triggering verification for stream %s", streamId);

        // Generate the verification event and push it synchronously so the
        // caller (admin Verify button / receiver POST /streams/verify /
        // post-create auto-fire) gets a real success/failure indication
        // rather than just "we scheduled it on the executor". Verification
        // is by nature an explicit, low-throughput interaction where the
        // caller wants to know the receiver's actual response — async
        // would mean reporting "OK" before the push has even started.
        SsfSecurityEventToken verificationEventToken = mapper.generateVerificationEvent(stream, verificationRequest.getState());
        Instant dispatchStart = Instant.now();
        boolean delivered = dispatcher.deliverEventSync(verificationEventToken, stream);
        Duration took = Duration.between(dispatchStart, Instant.now());

        // Record the dispatch timestamp only for explicitly requested
        // verifications (receiver POST /streams/verify or admin Verify
        // button). The transmitter-initiated post-create auto-fire is
        // routine setup, not a user-driven request, so it must not burn
        // the rate-limit window — otherwise a freshly-created stream
        // would 429 the receiver's first verify attempt until
        // min_verification_interval elapses. We still stamp on delivery
        // failure for the explicit paths because the rate-limit semantics
        // are "when did we last attempt this".
        if (initiator != SsfMetricsBinder.VerificationInitiator.TRANSMITTER) {
            streamStore.recordStreamVerification(streamId);
        }

        SsfMetricsBinder.VerificationOutcome outcome = delivered
                ? SsfMetricsBinder.VerificationOutcome.DELIVERED
                : SsfMetricsBinder.VerificationOutcome.FAILED;
        metricsBinder.recordVerification(currentRealmName(),
                stream.getClientClientId(), initiator, outcome, took);

        if (delivered) {
            log.debugf("Delivered verification event for stream %s", streamId);
        } else {
            log.warnf("Verification event push failed for stream %s — see prior errors for the underlying cause", streamId);
        }

        return delivered;
    }

    /**
     * Safe accessor for the current realm name — used as the
     * {@code realm} metric label so dashboards see {@code realm="ssf-poc"}
     * rather than the opaque realm UUID.
     */
    protected String currentRealmName() {
        try {
            return session.getContext().getRealm().getName();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
