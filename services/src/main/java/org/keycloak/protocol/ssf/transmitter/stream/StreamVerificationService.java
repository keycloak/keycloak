package org.keycloak.protocol.ssf.transmitter.stream;

import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.protocol.ssf.transmitter.stream.storage.SsfStreamStore;

import org.jboss.logging.Logger;

/**
 * Service for handling SSF stream verification.
 */
public class StreamVerificationService {

    protected static final Logger log = Logger.getLogger(StreamVerificationService.class);

    protected final SsfStreamStore streamStore;

    protected final SecurityEventTokenMapper mapper;

    protected final SecurityEventTokenDispatcher dispatcher;

    public StreamVerificationService(SsfStreamStore streamStore,
                                     SecurityEventTokenMapper mapper,
                                     SecurityEventTokenDispatcher dispatcher) {
        this.streamStore = streamStore;
        this.mapper = mapper;
        this.dispatcher = dispatcher;
    }

    /**
     * Triggers a verification event for a stream.
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

        // Generate a verification event
        SsfSecurityEventToken verificationEventToken = mapper.generateVerificationEvent(stream, verificationRequest.getState());
        dispatcher.deliverEvent(verificationEventToken, stream);

        log.debugf("Delivered verification event for stream %s", streamId);

        return true;
    }
}
