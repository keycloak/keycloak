package org.keycloak.ssf.transmitter.delivery;

import java.util.concurrent.ExecutorService;

import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.SsfProfile;
import org.keycloak.ssf.event.token.SecurityEventToken;
import org.keycloak.ssf.event.token.SseCaepSecurityEventToken;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.stream.DeliveryMethod;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.ssf.transmitter.event.SsfSignatureAlgorithms;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

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
            log.debugf("Skipping event delivery for stream because of unsupported stream status. streamId=%s jti=%s status=%s",
                    stream.getStreamId(), eventToken.getJti(), stream.getStatus());
            return;
        }

        // Check if the stream is interested in this event type
        if (isEventEnabledForStream(eventToken, stream)){
            log.debugf("Skipping event delivery for stream because of unsupported event. streamId=%s jti=%s events=%s",
                    stream.getStreamId(), eventToken.getJti(), eventToken.getEvents());
            return;
        }

        deliverEvent(eventToken, stream);
    }

    protected boolean isEventEnabledForStream(SsfSecurityEventToken eventToken, StreamConfig stream) {
        String eventType = getEventType(eventToken);
        if (eventType != null && stream.getEventsRequested() != null &&
            !stream.getEventsRequested().contains(eventType)) {
            return true;
        }
        return false;
    }

    protected boolean isStreamEnabled(StreamConfig stream) {
        StreamStatusValue status = stream.getStatus();
        if (status == null) {
            // some SSF receivers don't provide a status
            return true;
        }
        return StreamStatusValue.enabled == status;
    }

    /**
     * Asynchronously delivers an event to the receiver. The push runs on a
     * dedicated virtual thread so a slow or unresponsive receiver doesn't
     * block other dispatches and doesn't tie up a platform-thread pool.
     * Fire-and-forget — the absence of a return value reflects "we handed
     * the push off to its own thread", not "the receiver acknowledged".
     *
     * <p>Used by the event listener for user/admin SETs where best-effort
     * delivery is appropriate. For verification SETs the caller wants to
     * know whether the receiver actually accepted the push, so use
     * {@link #deliverEventSync(SsfSecurityEventToken, StreamConfig)}.
     */
    public void deliverEvent(SsfSecurityEventToken eventToken, StreamConfig stream) {
        deliverEventInternal(eventToken, stream, true);
    }

    /**
     * Synchronously delivers an event to the receiver and returns the
     * result. Runs on the caller's thread — used by the verification path
     * so the admin "Verify" button and the receiver-initiated
     * {@code POST /streams/verify} endpoint can report a real success or
     * failure to the caller, not just "we scheduled it".
     *
     * @return {@code true} if the receiver accepted the push,
     *         {@code false} on any delivery error.
     */
    public boolean deliverEventSync(SsfSecurityEventToken eventToken, StreamConfig stream) {
        return deliverEventInternal(eventToken, stream, false);
    }

    private boolean deliverEventInternal(SsfSecurityEventToken eventToken, StreamConfig stream, boolean async) {
        var delivery = stream.getDelivery();
        String deliveryMethodUri = delivery.getMethod();
        DeliveryMethod deliveryMethod = DeliveryMethod.valueOfUri(deliveryMethodUri);

        switch (deliveryMethod) {
            case PUSH, RISC_PUSH -> {
                try {
                    SecurityEventToken narrowedEventToken = getNarrowedEventToken(eventToken, stream);

                    String signatureAlgorithm = SsfSignatureAlgorithms.resolveForStream(stream, transmitterConfig);
                    String encodedEvent = securityEventTokenEncoder.encode(narrowedEventToken, signatureAlgorithm);

                    log.debugf("Delivering event to stream via %s. streamId=%s jti=%s async=%s",
                            deliveryMethod.name(), stream.getStreamId(), eventToken.getJti(), async);

                    if (async) {
                        deliverEventAsync(stream, narrowedEventToken, encodedEvent);
                        return true;
                    }
                    return pushDeliveryService.deliverEvent(stream, narrowedEventToken, encodedEvent);
                } catch (Exception e) {
                    log.errorf(e, "Error delivering event via PUSH to stream %s", stream.getStreamId());
                    return false;
                }
            }
            case POLL, RISC_POLL -> {
                log.warnf("Poll delivery not supported for %s", stream.getStreamId());
                return false;
            }
        }
        return false;
    }

    /**
     * Submits the push to Keycloak's managed executor for asynchronous
     * delivery. Virtual threads ({@code Thread.ofVirtual()}) would be a
     * better fit here — one thread per push, naturally handling blocking
     * I/O and isolating slow receivers — but the project compiles with
     * {@code --release 17} and the API is JDK 21+, so we stay on the
     * shared bounded pool for now. Worth revisiting once Keycloak bumps
     * its source level.
     */
    protected void deliverEventAsync(StreamConfig stream, SecurityEventToken narrowedEventToken, String encodedEvent) {
        ExecutorService executor = getPushExecutorService();
        executor.execute(() -> pushDeliveryService.deliverEvent(stream, narrowedEventToken, encodedEvent));
    }

    protected ExecutorService getPushExecutorService() {
        ExecutorsProvider executorsProvider = getExecutorsProvider();
        ExecutorService executor = executorsProvider.getExecutor("ssf-push-event");
        return executor;
    }

    protected ExecutorsProvider getExecutorsProvider() {
        return session.getProvider(ExecutorsProvider.class);
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
}
