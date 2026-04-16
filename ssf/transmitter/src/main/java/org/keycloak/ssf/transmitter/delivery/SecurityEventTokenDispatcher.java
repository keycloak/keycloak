package org.keycloak.ssf.transmitter.delivery;

import java.util.function.Function;

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
import org.keycloak.ssf.transmitter.outbox.SsfPendingEventStore;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.jboss.logging.Logger;

public class SecurityEventTokenDispatcher {

    protected static final Logger log = Logger.getLogger(SecurityEventTokenDispatcher.class);

    protected final KeycloakSession session;

    protected final SecurityEventTokenEncoder securityEventTokenEncoder;

    protected final PushDeliveryService pushDeliveryService;

    protected final SsfTransmitterConfig transmitterConfig;

    protected final Function<KeycloakSession, SsfPendingEventStore> pendingSsfEventStoreFactory;

    public SecurityEventTokenDispatcher(KeycloakSession session,
                                        SecurityEventTokenEncoder securityEventTokenEncoder,
                                        PushDeliveryService pushDeliveryService,
                                        SsfTransmitterConfig transmitterConfig,
                                        Function<KeycloakSession, SsfPendingEventStore> pendingSsfEventStoreFactory) {
        this.session = session;
        this.securityEventTokenEncoder = securityEventTokenEncoder;
        this.pushDeliveryService = pushDeliveryService;
        this.transmitterConfig = transmitterConfig;
        this.pendingSsfEventStoreFactory = pendingSsfEventStoreFactory;
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
     * Asynchronously delivers an event to the receiver by enqueuing the
     * signed SET into the durable {@link SsfPendingEventStore push outbox}.
     * The cluster-aware drainer picks the row up on its next tick and
     * pushes it to the receiver's endpoint, retrying with exponential
     * backoff and dead-lettering after the configured attempt budget is
     * exhausted.
     *
     * <p>Returns as soon as the row is enqueued — the receiver's
     * acknowledgement is observed asynchronously by the drainer, not by
     * this caller. Used by the event listener for user/admin SETs.
     *
     * <p>For verification SETs the caller needs the receiver's actual
     * accept/reject outcome inline, so use
     * {@link #deliverEventSync(SsfSecurityEventToken, StreamConfig)}
     * instead, which bypasses the outbox and pushes synchronously.
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
                        // Outbox row records the full, unnarrowed eventToken:
                        // the stored encodedEvent is already the signed,
                        // stream-narrowed payload that will go on the wire
                        // verbatim at drainer time, so the row-level fields
                        // (jti, eventType) are only used for logging,
                        // dedup, and per-event bookkeeping — indexed on the
                        // unnarrowed token so retries and admin queries see
                        // stable identifiers independent of what the
                        // narrowing step happened to strip for a given
                        // stream profile.
                        deliverEventAsync(stream, eventToken, encodedEvent);
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
     * Persists the already-signed SET to the push outbox. Once the row
     * is committed the cluster-aware drainer will pick it up on its
     * next tick and dispatch it with bounded retries — this method
     * does not perform any HTTP call itself.
     *
     * <p>The {@code clientId} on the stream is populated by
     * {@link org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore#extractStreamConfig
     * ClientStreamStore.extractStreamConfig}, so it is always set on
     * stream configs the dispatcher receives.
     */
    protected void deliverEventAsync(StreamConfig stream,
                                     SsfSecurityEventToken eventToken,
                                     String encodedEvent) {
        String realmId = session.getContext().getRealm().getId();
        String clientId = stream.getClientId();
        String streamId = stream.getStreamId();
        String jti = eventToken.getJti();
        String eventType = getEventType(eventToken);
        if (eventType == null) {
            // EVENT_TYPE is NOT NULL in the schema; an event token with no
            // events map shouldn't reach the dispatcher (dispatchEvent
            // already operates on it), but guard so a malformed call can't
            // poison the table with constraint failures.
            eventType = "<unknown>";
        }

        var pendingEventStore = pendingSsfEventStoreFactory.apply(session);
        pendingEventStore.enqueuePendingPush(realmId, clientId, streamId, jti, eventType, encodedEvent);
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

    /**
     * Narrows a {@link SsfSecurityEventToken} into a more general {@link SecurityEventToken}.
     *
     * @param eventToken The security event token to be narrowed.
     * @return The narrowed security event token.
     */
    protected SecurityEventToken narrowSsfEventToken(SsfSecurityEventToken eventToken) {
        return eventToken;
    }

    /**
     * Converts an {@link SsfSecurityEventToken} into a narrower {@link SseCaepSecurityEventToken}.
     * This method leverages the {@link SseCaepEventConverter} to transform the token according to
     * the Shared Signals and Events (SSE) standard, which may be required for compatibility with
     * legacy implementations.
     *
     * @param eventToken The security event token to be converted. Must not be null.
     * @return The converted {@link SseCaepSecurityEventToken} instance.
     */
    protected SseCaepSecurityEventToken narrowCaepSseEventToken(SsfSecurityEventToken eventToken) {
        return SseCaepEventConverter.convert(eventToken);
    }
}
