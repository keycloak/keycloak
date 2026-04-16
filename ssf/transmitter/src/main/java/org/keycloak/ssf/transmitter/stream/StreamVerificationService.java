package org.keycloak.ssf.transmitter.stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
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

    public StreamVerificationService(KeycloakSession session, SsfStreamStore streamStore,
                                     SecurityEventTokenMapper mapper,
                                     SecurityEventTokenDispatcher dispatcher) {
        this.session = session;
        this.streamStore = streamStore;
        this.mapper = mapper;
        this.dispatcher = dispatcher;
    }

    /**
     * Triggers a verification event for a stream.
     *
     * <p>Serves as the single centralized entry point for all verification
     * flows — receiver-initiated (via {@code POST /streams/verify}),
     * admin-initiated (via the SSF admin resource), and transmitter-initiated
     * automatic post-create dispatch. Stamps
     * {@code ssf.lastVerifiedAt} on the receiver client after dispatch so
     * the admin UI and the rate-limit check see a consistent "most recent
     * verification" timestamp regardless of which caller triggered it.
     *
     * @param verificationRequest The verification request
     * @return true if the verification was triggered, false if the stream was not found
     */
    public boolean triggerVerification(StreamVerificationRequest verificationRequest) {
        String streamId = verificationRequest.getStreamId();
        StreamConfig stream = streamStore.findStreamById(streamId);

        if (stream == null) {
            log.warnf("Stream not found for verification. streamId=%s", streamId);
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
        boolean delivered = dispatcher.deliverEventSync(verificationEventToken, stream);

        // Record the dispatch timestamp on the receiver client so the admin
        // UI's "last verified" field and the rate-limit check see the same
        // value regardless of whether the dispatch originated from a
        // receiver, an admin, or the post-create auto-trigger. We stamp
        // even on a delivery failure — the rate-limit semantics are
        // "when did we last attempt this", and the failure detail surfaces
        // separately via the boolean return.
        streamStore.recordStreamVerification(streamId);

        if (delivered) {
            log.debugf("Delivered verification event for stream %s", streamId);
        } else {
            log.warnf("Verification event push failed for stream %s — see prior errors for the underlying cause", streamId);
        }

        return delivered;
    }
}
