package org.keycloak.ssf.transmitter.delivery;

import java.util.Set;
import java.util.function.Function;

import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.SsfProfile;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.event.token.SecurityEventToken;
import org.keycloak.ssf.event.token.SseCaepSecurityEventToken;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.stream.DeliveryMethod;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.ssf.transmitter.event.SsfSignatureAlgorithms;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.outbox.SsfOutboxKinds;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.subject.SsfSubjectInclusionResolver;
import org.keycloak.ssf.transmitter.subject.SubjectSubscriptionFilter;

import org.jboss.logging.Logger;

public class SecurityEventTokenDispatcher {

    private static final Logger log = Logger.getLogger(SecurityEventTokenDispatcher.class);

    /**
     * Fallback filter used when no transmitter config is available
     * (e.g. legacy / test subclasses constructing the dispatcher
     * directly). Builds a filter with the §9.3 grace disabled — the
     * factory path replaces this in {@link #createSubjectSubscriptionFilter()}
     * with a config-aware instance.
     */
    protected static final SubjectSubscriptionFilter DEFAULT_SUBJECT_SUBSCRIPTION_FILTER = new SubjectSubscriptionFilter();

    protected final KeycloakSession session;

    protected final SecurityEventTokenEncoder securityEventTokenEncoder;

    protected final PushDeliveryService pushDeliveryService;

    protected final SsfTransmitterConfig transmitterConfig;

    protected final Function<KeycloakSession, OutboxStore> outboxStoreFactory;

    protected final SubjectSubscriptionFilter subjectSubscriptionFilter;

    protected final SsfMetricsBinder metricsBinder;

    protected final SsfSubjectInclusionResolver subjectInclusionResolver;

    public SecurityEventTokenDispatcher(KeycloakSession session,
                                        SecurityEventTokenEncoder securityEventTokenEncoder,
                                        PushDeliveryService pushDeliveryService,
                                        SsfTransmitterConfig transmitterConfig,
                                        Function<KeycloakSession, OutboxStore> outboxStoreFactory) {
        this(session, securityEventTokenEncoder, pushDeliveryService, transmitterConfig, outboxStoreFactory,
                SsfMetricsBinder.NOOP, null);
    }

    public SecurityEventTokenDispatcher(KeycloakSession session,
                                        SecurityEventTokenEncoder securityEventTokenEncoder,
                                        PushDeliveryService pushDeliveryService,
                                        SsfTransmitterConfig transmitterConfig,
                                        Function<KeycloakSession, OutboxStore> outboxStoreFactory,
                                        SsfMetricsBinder metricsBinder) {
        this(session, securityEventTokenEncoder, pushDeliveryService, transmitterConfig, outboxStoreFactory,
                metricsBinder, null);
    }

    public SecurityEventTokenDispatcher(KeycloakSession session,
                                        SecurityEventTokenEncoder securityEventTokenEncoder,
                                        PushDeliveryService pushDeliveryService,
                                        SsfTransmitterConfig transmitterConfig,
                                        Function<KeycloakSession, OutboxStore> outboxStoreFactory,
                                        SsfMetricsBinder metricsBinder,
                                        SsfSubjectInclusionResolver subjectInclusionResolver) {
        this.session = session;
        this.securityEventTokenEncoder = securityEventTokenEncoder;
        this.pushDeliveryService = pushDeliveryService;
        this.transmitterConfig = transmitterConfig;
        this.outboxStoreFactory = outboxStoreFactory;
        this.subjectInclusionResolver = subjectInclusionResolver;
        this.subjectSubscriptionFilter = createSubjectSubscriptionFilter();
        this.metricsBinder = metricsBinder == null ? SsfMetricsBinder.NOOP : metricsBinder;
    }

    protected SubjectSubscriptionFilter createSubjectSubscriptionFilter() {
        if (transmitterConfig == null) {
            return DEFAULT_SUBJECT_SUBSCRIPTION_FILTER;
        }
        return new SubjectSubscriptionFilter(transmitterConfig.getSubjectRemovalGraceSeconds(),
                subjectInclusionResolver);
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

        StreamStatusValue status = stream.getStatus();

        if (status == StreamStatusValue.disabled) {
            // SSF: disabled streams drop events outright.
            log.debugf("Dropping event for disabled stream. clientId=%s streamId=%s jti=%s",
                    stream.getClientClientId(), stream.getStreamId(), eventToken.getJti());
            metricsBinder.recordSuppressed(currentRealmName(), stream.getClientClientId(),
                    SsfMetricsBinder.SuppressReason.STATUS_DISABLED);
            return;
        }

        // Check if the stream is interested in this event type
        if (!isEventRequestedByStream(eventToken, stream)) {
            log.debugf("Skipping event delivery for stream because of unsupported event. clientId=%s streamId=%s jti=%s events=%s",
                    stream.getClientClientId(), stream.getStreamId(), eventToken.getJti(), eventToken.getEvents());
            metricsBinder.recordSuppressed(currentRealmName(), stream.getClientClientId(),
                    SsfMetricsBinder.SuppressReason.EVENT_NOT_REQUESTED);
            return;
        }

        if (!shouldDispatchForSubject(eventToken, stream)) {
            metricsBinder.recordSuppressed(currentRealmName(), stream.getClientClientId(),
                    SsfMetricsBinder.SuppressReason.SUBJECT_GATE);
            return;
        }

        if (status == StreamStatusValue.paused) {
            // SSF: paused streams hold events; they're released when
            // the stream is resumed (status returns to enabled).
            metricsBinder.recordSuppressed(currentRealmName(), stream.getClientClientId(),
                    SsfMetricsBinder.SuppressReason.STATUS_PAUSED_HELD);
            holdEvent(eventToken, stream);
            return;
        }

        deliverEvent(eventToken, stream);
    }

    /**
     * Safe accessor for the current realm <em>name</em> — used as the
     * {@code realm} metric label so operators see {@code realm="ssf-poc"}
     * rather than the opaque realm UUID. During dispatch the session
     * always carries a realm context, but we guard against a malformed
     * call path poisoning the metrics labels with a null.
     */
    protected String currentRealmName() {
        try {
            return session.getContext().getRealm().getName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Resolves the receiver-friendly event alias (e.g.
     * {@code CaepCredentialChange}) for the full event type URI. Falls
     * back to the URI when the event type isn't registered — keeps
     * unknown / custom event types observable on the metrics side
     * without losing the increment.
     */
    protected String resolveEventAlias(String eventType) {
        if (eventType == null) {
            return null;
        }
        String alias = Ssf.events().getRegistry().resolveAliasForEventType(eventType);
        return alias != null ? alias : eventType;
    }

    /**
     * Resolves the value to write into the outbox row's
     * {@code entry_type} column. Aliases are the lingua franca elsewhere
     * in the SSF subsystem (admin UI, client attributes, metrics labels,
     * stream-config {@code events_delivered} when admin-created), so
     * storing the alias keeps the outbox column consistent with the
     * sets it is filtered against — most notably
     * {@link org.keycloak.ssf.transmitter.stream.StreamService#evictPendingEventsOutsideDeliveredSet
     * evictPendingEventsOutsideDeliveredSet}.
     *
     * <p>Falls back to the URI for custom events that have no alias
     * registered, and to {@code <unknown>} for malformed tokens (the
     * column is NOT NULL, so we always have to write something).
     */
    protected String resolveEntryType(SsfSecurityEventToken eventToken) {
        String eventType = getEventType(eventToken);
        if (eventType == null) {
            return "<unknown>";
        }
        return resolveEventAlias(eventType);
    }

    /**
     * Subject subscription filter. Returns {@code true} if the event
     * should be delivered to the given stream based on the stream's
     * {@code default_subjects} setting and the presence of
     * {@code ssf.notify.<clientId>} attributes on the event's subject.
     *
     * <p>Protected so subclasses can override the filtering logic — e.g.
     * to add custom subject resolution or to unconditionally bypass the
     * check for certain event types.
     */
    protected boolean shouldDispatchForSubject(SsfSecurityEventToken eventToken, StreamConfig stream) {
        return subjectSubscriptionFilter.shouldDispatch(eventToken, stream, stream.getClientClientId(), session);
    }

    /**
     * Pre-token subject gate callable before the mapper runs. Returns
     * {@code true} if the user could receive *any* event on this
     * stream under the current subscription state. Lets the event
     * listener short-circuit streams whose subject is not subscribed
     * before paying for {@code toSecurityEventToken}. The full token-based
     * gate still runs inside {@link #dispatchEvent}, so any mismatch
     * between {@code event.getUserId()} and the final token subject
     * (complex subjects, impersonation) stays safe.
     */
    public boolean shouldDispatchForUser(UserModel user, StreamConfig stream) {
        return subjectSubscriptionFilter.shouldDispatchForUser(user, stream, stream.getClientClientId(), session);
    }

    /**
     * Returns {@code true} if the event token's type is part of the
     * stream's {@code events_requested} set — i.e. the receiver wants
     * this event delivered. Fail-open when either side is missing
     * (token without event-type info, or stream that hasn't narrowed
     * its subscription).
     *
     * <p>Canonicalizes each {@code events_requested} entry to its URI
     * form before comparing with the SET token's URI, because the set
     * may carry either form depending on who created the stream:
     * <ul>
     *   <li>Admin-UI-created streams store aliases (the form the UI
     *       speaks).</li>
     *   <li>Receiver-created streams store URIs (per the SSF/CAEP
     *       spec).</li>
     * </ul>
     * Without this normalization, admin-UI-narrowed streams would
     * never match incoming events and every dispatch would be
     * suppressed.
     */
    protected boolean isEventRequestedByStream(SsfSecurityEventToken eventToken, StreamConfig stream) {
        String eventTypeUri = getEventType(eventToken);
        Set<String> eventsRequested = stream.getEventsRequested();
        if (eventTypeUri == null || eventsRequested == null) {
            return true;
        }
        SsfEventRegistry registry = Ssf.events().getRegistry();
        for (String requested : eventsRequested) {
            if (requested == null) {
                continue;
            }
            String canonical = registry.resolveEventTypeForAlias(requested);
            if (canonical == null) {
                // Either it's already a URI (resolveEventTypeForAlias
                // only knows aliases) or a custom unregistered string;
                // in both cases use the literal for comparison.
                canonical = requested;
            }
            if (eventTypeUri.equals(canonical)) {
                return true;
            }
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
     * Holds an event for a paused stream by signing it and writing it to
     * the outbox in
     * {@link org.keycloak.models.jpa.entities.OutboxEntryStatus#HELD HELD}
     * status. The drainer / POLL endpoint skip HELD rows; they're
     * released to {@code PENDING} when the stream is resumed
     * ({@link org.keycloak.events.outbox.OutboxStore#releaseHeldForOwner releaseHeldForOwner}).
     */
    protected void holdEvent(SsfSecurityEventToken eventToken, StreamConfig stream) {
        var delivery = stream.getDelivery();
        DeliveryMethod deliveryMethod = DeliveryMethod.valueOfUri(delivery.getMethod());

        try {
            SecurityEventToken narrowedEventToken = getNarrowedEventToken(eventToken, stream);
            String signatureAlgorithm = SsfSignatureAlgorithms.resolveForStream(stream, transmitterConfig);
            String encodedEvent = securityEventTokenEncoder.encode(narrowedEventToken, signatureAlgorithm);

            String realmId = session.getContext().getRealm().getId();
            String clientId = stream.getClientId();
            String streamId = stream.getStreamId();
            String jti = eventToken.getJti();
            String entryType = resolveEntryType(eventToken);

            OutboxStore outboxStore = outboxStoreFactory.apply(session);
            switch (deliveryMethod) {
                case PUSH, RISC_PUSH ->
                        outboxStore.enqueueHeld(SsfOutboxKinds.PUSH, realmId, clientId, streamId, jti, entryType, encodedEvent, null);
                case POLL, RISC_POLL ->
                        outboxStore.enqueueHeld(SsfOutboxKinds.POLL, realmId, clientId, streamId, jti, entryType, encodedEvent, null);
            }

            // HELD is a distinct outcome from normal delivery — we
            // already counted it under SuppressReason.STATUS_PAUSED_HELD
            // on the dispatch gate. No additional enqueued counter bump
            // here so the two signals ("would have fired but paused"
            // vs "actually enqueued for wire delivery") stay separable.

            log.debugf("Held event for paused stream. clientId=%s streamId=%s jti=%s deliveryMethod=%s",
                    stream.getClientClientId(), streamId, jti, deliveryMethod.name());
        } catch (Exception e) {
            log.errorf(e, "Error holding event for paused stream. clientId=%s streamId=%s",
                    stream.getClientClientId(), stream.getStreamId());
        }
    }

    /**
     * Asynchronously delivers an event to the receiver by enqueuing the
     * signed SET into the durable {@link OutboxStore push outbox}.
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

                    log.debugf("Delivering event to stream via %s. clientId=%s streamId=%s jti=%s async=%s",
                            deliveryMethod.name(), stream.getClientClientId(), stream.getStreamId(), eventToken.getJti(), async);

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
                    return pushDeliveryService.deliverEvent(stream, narrowedEventToken, encodedEvent).delivered();
                } catch (Exception e) {
                    log.errorf(e, "Error delivering event via PUSH to stream. clientId=%s streamId=%s"
                            ,stream.getClientClientId(), stream.getStreamId());
                    return false;
                }
            }
            case POLL, RISC_POLL -> {
                try {
                    // Poll path mirrors the async PUSH branch: narrow per
                    // stream profile, sign once, persist the encoded SET
                    // into the outbox. The poll endpoint reads + acks
                    // these rows on the receiver's schedule — there is no
                    // drainer that pushes them out. The `async` flag is
                    // ignored: poll has no synchronous-delivery option
                    // because there is nobody to push to until the
                    // receiver shows up.
                    SecurityEventToken narrowedEventToken = getNarrowedEventToken(eventToken, stream);

                    String signatureAlgorithm = SsfSignatureAlgorithms.resolveForStream(stream, transmitterConfig);
                    String encodedEvent = securityEventTokenEncoder.encode(narrowedEventToken, signatureAlgorithm);

                    log.debugf("Enqueuing event for poll delivery. clientId=%s streamId=%s jti=%s"
                            ,stream.getClientClientId(), stream.getStreamId(), eventToken.getJti());

                    enqueueForPoll(stream, eventToken, encodedEvent);
                    return true;
                } catch (Exception e) {
                    log.errorf(e, "Error enqueuing event for POLL delivery to stream. clientId=%s streamId=%s"
                            ,stream.getClientClientId(), stream.getStreamId());
                    return false;
                }
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
        String entryType = resolveEntryType(eventToken);

        OutboxStore outboxStore = outboxStoreFactory.apply(session);
        outboxStore.enqueuePending(SsfOutboxKinds.PUSH, realmId, clientId, streamId, jti, entryType, encodedEvent, null);
        metricsBinder.recordEnqueued(currentRealmName(), stream.getClientClientId(), "PUSH", entryType);
    }

    /**
     * Persists a signed SET to the outbox tagged for poll delivery. The
     * receiver pulls it via the poll endpoint
     * ({@code POST /ssf/transmitter/receivers/{clientId}/streams/{stream_id}/poll})
     * and acks it; no drainer task touches POLL rows.
     */
    protected void enqueueForPoll(StreamConfig stream,
                                  SsfSecurityEventToken eventToken,
                                  String encodedEvent) {
        String realmId = session.getContext().getRealm().getId();
        String clientId = stream.getClientId();
        String streamId = stream.getStreamId();
        String jti = eventToken.getJti();
        String entryType = resolveEntryType(eventToken);

        OutboxStore outboxStore = outboxStoreFactory.apply(session);
        outboxStore.enqueuePending(SsfOutboxKinds.POLL, realmId, clientId, streamId, jti, entryType, encodedEvent, null);
        metricsBinder.recordEnqueued(currentRealmName(), stream.getClientClientId(), "POLL", entryType);
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
