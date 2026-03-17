package org.keycloak.protocol.ssf.transmitter.delivery;

import java.util.List;

import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.token.SecurityEventToken;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.stream.DeliveryMethod;
import org.keycloak.protocol.ssf.stream.StreamStatusValue;
import org.keycloak.protocol.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;

import org.jboss.logging.Logger;

public class SecurityEventTokenDispatcher {

    protected static final Logger log = Logger.getLogger(SecurityEventTokenDispatcher.class);

    private final StreamService streamService;
    private final SecurityEventTokenEncoder securityEventTokenEncoder;
    private final PushDeliveryService pushDeliveryService;

    public SecurityEventTokenDispatcher(StreamService streamService,
                                        SecurityEventTokenEncoder securityEventTokenEncoder,
                                        PushDeliveryService pushDeliveryService) {
        this.streamService = streamService;
        this.securityEventTokenEncoder = securityEventTokenEncoder;
        this.pushDeliveryService = pushDeliveryService;
    }

    /**
     * Gets the event type from a security event token.
     *
     * @param eventToken The security event token
     * @return The event type, or null if not found
     */
    protected String getEventType(SsfSecurityEventToken eventToken) {
        if (eventToken.getEvents() != null && !eventToken.getEvents().isEmpty()) {
            return eventToken.getEvents().keySet().iterator().next();
        }
        return null;
    }

    /**
     * Delivers an event to all applicable streams.
     *
     * @param eventToken The event to deliver
     */
    public void dispatchEvent(SsfSecurityEventToken eventToken) {

        List<StreamConfig> streams = streamService.findAllEnabledStreams();

        if (streams.isEmpty()) {
            log.warnf("No streams found. Discarding event %s", eventToken.getJti());
            return;
        }

        try {
            for (StreamConfig stream : streams) {
                dispatchEvent(eventToken, stream);
            }
        } catch (Exception e) {
            log.errorf(e, "Error delivering event %s", eventToken.getJti());
        }
    }

    public void dispatchEvent(SsfSecurityEventToken eventToken, StreamConfig stream) {

        if (eventToken == null || stream == null) {
            return;
        }

        if (!isStreamEnabled(stream)) {
            return;
        }

        // Check if the stream is interested in this event type
        String eventType = getEventType(eventToken);
        if (eventType != null && stream.getEventsRequested() != null &&
            !stream.getEventsRequested().contains(eventType)) {
            return;
        }

        deliverEvent(eventToken, stream);
    }

    public void deliverEvent(SsfSecurityEventToken eventToken, StreamConfig stream) {
        // Deliver the event based on the stream's delivery method
        var delivery = stream.getDelivery();
        String deliveryMethodUri = delivery.getMethod();
        DeliveryMethod deliveryMethod = DeliveryMethod.valueOfUri(deliveryMethodUri);

        switch (deliveryMethod) {
            case PUSH, RISC_PUSH -> {
                try {
                    SecurityEventToken narrowedEventToken = eventToken;

                    if (stream.getProfile() == null || Ssf.PROFILE_SSE_CAEP.equals(stream.getProfile())) {
                        narrowedEventToken = SseCaepEventConverter.convert(eventToken);
                    }

                    String encodedEvent = securityEventTokenEncoder.encode(narrowedEventToken);
                    pushDeliveryService.deliverEvent(stream, narrowedEventToken, encodedEvent);
                } catch (Exception e) {
                    log.errorf(e, "Error delivering event via PUSH to stream %s", stream.getStreamId());
                }
            }
            case POLL, RISC_POLL -> {
                log.warnf("Poll delivery not supported for %s", stream.getStreamId());
            }
        }
    }

    protected boolean isStreamEnabled(StreamConfig stream) {
        StreamStatusValue status = stream.getStatus();
        if (status == null) {
            // some SSF receivers don't provide a status
            return true;
        }
        return StreamStatusValue.enabled == status;
    }
}
