package org.keycloak.protocol.ssf.transmitter.delivery;

import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.SsfProfile;
import org.keycloak.protocol.ssf.event.token.SecurityEventToken;
import org.keycloak.protocol.ssf.event.token.SseCaepSecurityEventToken;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.stream.DeliveryMethod;
import org.keycloak.protocol.ssf.stream.StreamStatusValue;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.protocol.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.protocol.ssf.transmitter.event.SsfSignatureAlgorithms;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;

import org.jboss.logging.Logger;

public class SecurityEventTokenDispatcher {

    protected static final Logger log = Logger.getLogger(SecurityEventTokenDispatcher.class);

    private final KeycloakSession session;
    private final SecurityEventTokenEncoder securityEventTokenEncoder;
    private final PushDeliveryService pushDeliveryService;
    private final SsfTransmitterConfig transmitterConfig;

    public SecurityEventTokenDispatcher(KeycloakSession session,
                                        SecurityEventTokenEncoder securityEventTokenEncoder,
                                        PushDeliveryService pushDeliveryService,
                                        SsfTransmitterConfig transmitterConfig) {
        this.session = session;
        this.securityEventTokenEncoder = securityEventTokenEncoder;
        this.pushDeliveryService = pushDeliveryService;
        this.transmitterConfig = transmitterConfig;
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
                    SecurityEventToken narrowedEventToken = getNarrowedEventToken(eventToken, stream);

                    String signatureAlgorithm = SsfSignatureAlgorithms.resolveForStream(stream, transmitterConfig);
                    String encodedEvent = securityEventTokenEncoder.encode(narrowedEventToken, signatureAlgorithm);

                    deliverEvent(stream, narrowedEventToken, encodedEvent);
                } catch (Exception e) {
                    log.errorf(e, "Error delivering event via PUSH to stream %s", stream.getStreamId());
                }
            }
            case POLL, RISC_POLL -> {
                log.warnf("Poll delivery not supported for %s", stream.getStreamId());
            }
        }
    }

    protected void deliverEvent(StreamConfig stream, SecurityEventToken narrowedEventToken, String encodedEvent) {

        ExecutorsProvider executorsProvider = session.getProvider(ExecutorsProvider.class);
        var executor = executorsProvider.getExecutor("ssf-push-event-dispatcher");
        // FIXME use a virtual thread to deliver events via push
        executor.execute(() -> pushDeliveryService.deliverEvent(stream, narrowedEventToken, encodedEvent));
    }

    protected SecurityEventToken getNarrowedEventToken(SsfSecurityEventToken eventToken, StreamConfig stream) {
        // StreamConfig.getProfile() is an SsfProfile enum — comparing it against
        // the String constants in Ssf (Ssf.PROFILE_SSF_1_0 etc.) always returned
        // false, so neither branch ever fired and the SSE CAEP converter was
        // silently skipped for Apple Business Manager. Compare the enum
        // directly instead.
        SsfProfile profile = stream.getProfile();
        if (profile == null) {
            profile = SsfProfile.SSF_1_0;
        }

        return switch (profile) {
            case SSF_1_0 -> narrowSsfEventToken(eventToken);
            // if legacy CAEP SSE profile is requested convert the event to old format
            // this is currently required for compatibility with apple business manager
            case SSE_CAEP -> narrowCaepSseEventToken(eventToken);
        };
    }

    protected SecurityEventToken narrowSsfEventToken(SsfSecurityEventToken eventToken) {
        return eventToken;
    }

    protected SseCaepSecurityEventToken narrowCaepSseEventToken(SsfSecurityEventToken eventToken) {
        return SseCaepEventConverter.convert(eventToken);
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
