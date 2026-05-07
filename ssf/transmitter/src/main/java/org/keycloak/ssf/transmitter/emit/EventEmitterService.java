package org.keycloak.ssf.transmitter.emit;

import java.util.Map;
import java.util.function.Supplier;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.event.SsfEvent;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.event.SsfEventValidationException;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectUserLookup;
import org.keycloak.ssf.transmitter.SsfTransmitter;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.SsfSubjectInclusionResolver;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

/**
 * Pushes synthetic SSF events injected by a trusted IAM management
 * client through the normal transmitter dispatch pipeline.
 *
 * <p>Used by the admin-facing {@code /admin/realms/{realm}/ssf/clients/{id}/events/emit}
 * endpoint so external systems (e.g. the LDAP/IdM that actually owns
 * password changes) can forward credential-change and other events to
 * SSF receivers as if Keycloak had observed them itself. The signed SET
 * is built with the receiver's stream identity (iss/aud/format) and is
 * dispatched through the same outbox + push pipeline as native events.
 *
 * <p>Filter guards applied here mirror the dispatcher's: only events the
 * receiver subscribed to (via {@code events_requested}) and only subjects
 * the receiver is interested in (via {@code ssf.notify.<clientId>} and
 * {@code default_subjects}) are dispatched. Drop reasons are reported
 * back to the caller so emitter integrations can debug their wiring.
 */
public class EventEmitterService {

    private static final Logger log = Logger.getLogger(EventEmitterService.class);

    protected final KeycloakSession session;

    protected final ClientStreamStore streamStore;

    protected final SecurityEventTokenMapper eventTokenMapper;

    protected final SecurityEventTokenDispatcher eventTokenDispatcher;

    protected final SsfSubjectInclusionResolver subjectInclusionResolver;

    public EventEmitterService(KeycloakSession session,
                               ClientStreamStore streamStore,
                               SecurityEventTokenMapper eventTokenMapper,
                               SecurityEventTokenDispatcher eventTokenDispatcher,
                               SsfSubjectInclusionResolver subjectInclusionResolver) {
        this.session = session;
        this.streamStore = streamStore;
        this.eventTokenMapper = eventTokenMapper;
        this.eventTokenDispatcher = eventTokenDispatcher;
        this.subjectInclusionResolver = subjectInclusionResolver;
    }

    /**
     * Resolves the request, runs the receiver's dispatch filters, and
     * pushes a single signed SET via the existing outbox path.
     *
     * <p>The {@code subjectId} is an RFC 9493 Subject Identifier — one of
     * {@code EmailSubjectId}, {@code IssuerSubjectId}, {@code OpaqueSubjectId},
     * or {@code ComplexSubjectId}. For complex subjects the service drills
     * into {@link ComplexSubjectId#getUser()} to run the subscription
     * check, but passes the whole {@code sub_id} through verbatim to the
     * SET so the receiver sees the exact shape the emitter supplied.
     *
     * <p>Stream-management event types (verification, stream-updated)
     * bypass the subject-subscription filter the way the native
     * dispatcher does — those events are about the stream itself, not a
     * user.
     *
     * @return the dispatch outcome — never {@code null}.
     */
    public EmitEventResult emit(ClientModel receiverClient,
                                String eventTypeAliasOrUri,
                                SubjectId subjectId,
                                Map<String, Object> eventAttributes) {

        if (receiverClient == null) {
            return EmitEventResult.dropped(EmitEventStatus.STREAM_NOT_FOUND);
        }
        // Programmatic callers can pass any ClientModel they hold —
        // guard early so an extension that picks the wrong client gets
        // a clear error instead of a misleading STREAM_NOT_FOUND once
        // the stream lookup later returns null. The REST emit path
        // routes through the same gate, so a request targeting a
        // non-SSF client surfaces this as a 500 with the message —
        // intentional: the caller's configuration is wrong.
        if (!isSsfReceiverClient(receiverClient)) {
            throw new SsfException("Client '" + receiverClient.getClientId()
                    + "' is not an SSF Receiver");
        }
        if (eventTypeAliasOrUri == null || eventTypeAliasOrUri.isBlank()) {
            return EmitEventResult.dropped(EmitEventStatus.INVALID_REQUEST);
        }
        if (subjectId == null) {
            return EmitEventResult.dropped(EmitEventStatus.INVALID_REQUEST);
        }

        // 1. Resolve event type alias / URI to canonical URI.
        SsfEventRegistry registry = registryOrNull();
        if (registry == null) {
            return EmitEventResult.dropped(EmitEventStatus.UNKNOWN_EVENT_TYPE);
        }
        String eventTypeUri = registry.resolveEventTypeForAlias(eventTypeAliasOrUri);
        if (eventTypeUri == null && registry.getEventClassByType(eventTypeAliasOrUri).isPresent()) {
            eventTypeUri = eventTypeAliasOrUri;
        }
        if (eventTypeUri == null) {
            return EmitEventResult.dropped(EmitEventStatus.UNKNOWN_EVENT_TYPE);
        }

        // 2. Stream-management events (verification, stream-updated)
        //    are protocol-internal lifecycle signals the transmitter
        //    itself owns. Letting an external emitter forge them would
        //    let it spoof transmitter behaviour towards the receiver,
        //    so reject up front.
        if (isStreamEvent(eventTypeUri)) {
            return EmitEventResult.dropped(EmitEventStatus.EVENT_TYPE_NOT_EMITTABLE);
        }

        // 3. Receiver must have a registered SSF stream.
        StreamConfig stream = streamStore.getStreamForClient(receiverClient);
        if (stream == null) {
            return EmitEventResult.dropped(EmitEventStatus.STREAM_NOT_FOUND);
        }

        // 4. Event type must be in the receiver's events_requested
        //    set. Receivers that pass null opt into everything.
        if (stream.getEventsRequested() != null && !stream.getEventsRequested().contains(eventTypeUri)) {
            return EmitEventResult.dropped(EmitEventStatus.DROPPED_FILTERED);
        }

        // 5. Subject resolution + subscription filter. ComplexSubjectId
        //    can carry a user, an org (via the tenant slot), or both —
        //    we accept any resolvable combination and apply the
        //    receiver's notify-attribute subscription on whichever
        //    facets resolve.
        EmitSubjectResolution resolved = resolveSubject(subjectId);
        if (resolved.user() == null && resolved.organization() == null) {
            return EmitEventResult.dropped(EmitEventStatus.SUBJECT_NOT_FOUND);
        }
        // Drop early so the emitter sees a clean status without
        // paying the SET signing cost for a filtered subject.
        if (!isSubjectDispatchable(resolved, stream, receiverClient)) {
            return EmitEventResult.dropped(EmitEventStatus.DROPPED_UNSUBSCRIBED);
        }

        // 5. Deserialize the event payload into the registry's typed
        //    event class so the dispatcher's per-event narrowing (e.g.
        //    SSE_CAEP conversion for Apple Business Manager) applies
        //    just like for native events. On failure, surface the
        //    Jackson error message via the result so the admin caller
        //    can see exactly which field shape is wrong instead of a
        //    generic invalid_request.
        Object eventPayload;
        try {
            eventPayload = deserializeEventOrThrow(registry, eventTypeUri, eventAttributes);
        } catch (EventPayloadDeserializationException e) {
            return EmitEventResult.dropped(EmitEventStatus.INVALID_REQUEST, e.getMessage());
        }
        if (eventPayload == null) {
            return EmitEventResult.dropped(EmitEventStatus.INVALID_REQUEST,
                    "No registered event class for eventType=" + eventTypeUri);
        }

        // Per-event spec validation. Default SsfEvent.validate() is a
        // no-op; CAEP / RISC / custom event subclasses override it to
        // enforce spec-required fields (e.g. change_type on
        // CaepCredentialChange). Run after Jackson conversion so the
        // typed field values are populated by the time we look at them.
        // The exception's getMessage() composition matches the wire
        // status enum (invalid_event_data) so callers get one stable
        // identifier that names both the failure category and the
        // offending alias.field — they can localise from there.
        if (eventPayload instanceof SsfEvent typedEvent) {
            try {
                typedEvent.validate();
            } catch (SsfEventValidationException e) {
                return EmitEventResult.dropped(EmitEventStatus.INVALID_EVENT_DATA, e.getMessage());
            }
        }

        // 6. Build the SET (sub_id verbatim from the emitter) and hand
        //    off to the existing dispatcher.
        SsfSecurityEventToken token = eventTokenMapper.generateSyntheticEvent(stream, eventTypeUri, eventPayload, subjectId);
        if (token == null) {
            return EmitEventResult.dropped(EmitEventStatus.INVALID_REQUEST);
        }

        // Filters above mirror what dispatcher.dispatchEvent would run,
        // so deliverEvent is the right entry point — same async outbox
        // path native events take, no double filtering.
        eventTokenDispatcher.deliverEvent(token, stream);

        log.debugf("SSF synthetic event dispatched. receiverClientId=%s streamId=%s eventType=%s jti=%s",
                receiverClient.getClientId(), stream.getStreamId(), eventTypeUri, token.getJti());

        return EmitEventResult.dispatched(token.getJti());
    }

    /**
     * Resolves the entities referenced by the emitter's {@code sub_id}.
     * For a {@link ComplexSubjectId} we drill into both
     * {@link ComplexSubjectId#getUser()} and {@link ComplexSubjectId#getTenant()}
     * — the user facet drives the per-user notify subscription and the
     * tenant facet drives the org-level notify subscription. For a
     * non-complex {@link SubjectId} only the user is resolved.
     *
     * <p>Org resolution treats an {@link OpaqueSubjectId} {@code id} as
     * the org's alias first, falling back to the org UUID. Other
     * {@link SubjectId} formats in the tenant slot are not currently
     * understood — they resolve to no organization.
     */
    protected EmitSubjectResolution resolveSubject(SubjectId subjectId) {
        RealmModel realm = session.getContext().getRealm();

        if (subjectId instanceof ComplexSubjectId complex) {
            UserModel user = complex.getUser() != null
                    ? SubjectUserLookup.lookupUser(session, realm, complex.getUser())
                    : null;
            OrganizationModel org = resolveOrganization(complex.getTenant());
            return new EmitSubjectResolution(user, org);
        }

        UserModel user = SubjectUserLookup.lookupUser(session, realm, subjectId);
        return new EmitSubjectResolution(user, null);
    }

    protected OrganizationModel resolveOrganization(SubjectId tenantFacet) {
        if (tenantFacet == null || !Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return null;
        }
        if (!(tenantFacet instanceof OpaqueSubjectId opaque) || opaque.getId() == null) {
            return null;
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return null;
        }
        // Prefer alias (matches the admin shorthand 'org-alias' convention),
        // then fall back to UUID for emitters that prefer stable identifiers.
        OrganizationModel org = orgProvider.getByAlias(opaque.getId());
        if (org == null) {
            org = orgProvider.getById(opaque.getId());
        }
        return org;
    }

    protected boolean isSsfReceiverClient(ClientModel client) {
        // Delegates to the public helper so anyone (REST callers,
        // extensions, tests) shares one definition of "is this client
        // an SSF Receiver."
        return SsfTransmitter.isReceiverClient(client);
    }

    protected boolean isStreamEvent(String eventTypeUri) {
        // Single source of truth for the protocol-internal lifecycle
        // event list — shared with SsfEventRegistry#getReceiverRequestableEventTypes
        // so the admin UI's "available supported events" list and
        // this gate can never drift apart.
        return SsfEventRegistry.STREAM_LIFECYCLE_EVENT_TYPES.contains(eventTypeUri);
    }

    /**
     * Subscription gate that mirrors the native dispatcher's
     * {@code SubjectSubscriptionFilter} but operates on a pre-resolved
     * user / org pair so the emitter can also emit org-only events.
     *
     * <ul>
     *     <li>{@code default_subjects=ALL}: deliver unless either the
     *         user or the org is explicitly excluded.</li>
     *     <li>{@code default_subjects=NONE}: deliver only when at least
     *         one of the user / org facets is explicitly notified.</li>
     * </ul>
     */
    protected boolean isSubjectDispatchable(EmitSubjectResolution resolved,
                                            StreamConfig stream,
                                            ClientModel receiverClient) {
        String receiverClientId = receiverClient.getClientId();
        DefaultSubjects defaultSubjects = stream.getDefaultSubjects();

        if (defaultSubjects == DefaultSubjects.ALL) {
            boolean userExcluded = resolved.user() != null
                    && subjectInclusionResolver.isUserExcluded(session, resolved.user(), receiverClientId);
            boolean orgExcluded = resolved.organization() != null
                    && subjectInclusionResolver.isOrganizationExcluded(session, resolved.organization(), receiverClientId);
            return !userExcluded && !orgExcluded;
        }

        boolean userNotified = resolved.user() != null
                && subjectInclusionResolver.isUserNotified(session, resolved.user(), receiverClientId);
        boolean orgNotified = resolved.organization() != null
                && subjectInclusionResolver.isOrganizationNotified(session, resolved.organization(), receiverClientId);
        return userNotified || orgNotified;
    }

    /**
     * Deserializes the raw event attributes into the typed event
     * class registered for the given URI, propagating the Jackson
     * conversion error as an {@link EventPayloadDeserializationException}
     * so callers can surface the message back to the operator.
     */
    protected Object deserializeEventOrThrow(SsfEventRegistry registry,
                                             String eventTypeUri,
                                             Map<String, Object> eventAttributes) {
        Class<? extends SsfEvent> eventClass = registry.getEventClassByType(eventTypeUri).orElse(null);
        if (eventClass == null) {
            return null;
        }
        if (eventAttributes == null) {
            // Empty event body — mint a default instance via the
            // registry's factory (contributed as a SomeEvent::new method
            // reference) so some CAEP events that have no required
            // fields can still be emitted with the event type URI alone
            // carrying the signal. No runtime reflection.
            return registry.getEventFactoryByType(eventTypeUri)
                    .map(Supplier::get)
                    .orElse(null);
        }
        try {
            return JsonSerialization.mapper.convertValue(eventAttributes, eventClass);
        } catch (IllegalArgumentException e) {
            // Jackson wraps mismatch errors in IllegalArgumentException
            // when called via convertValue — its cause is the typed
            // JsonMappingException with the field-pointer details.
            String detail = e.getCause() != null && e.getCause().getMessage() != null
                    ? e.getCause().getMessage()
                    : e.getMessage();
            throw new EventPayloadDeserializationException(
                    "Event payload does not match the schema for " + eventTypeUri + ": " + detail, e);
        }
    }

    /**
     * Internal exception type used to ferry a Jackson conversion
     * failure back to the admin endpoint without leaking the raw
     * stack to the wire.
     */
    protected static class EventPayloadDeserializationException extends RuntimeException {
        public EventPayloadDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    protected SsfEventRegistry registryOrNull() {
        // Reach the registry via the same Ssf.events() entry point the
        // transmitter uses internally — keeps the emitter independent of
        // any future provider-level registry getter.
        try {
            return SsfEventRegistry.of(session);
        } catch (Exception e) {
            log.warn("SSF event registry not available", e);
            return null;
        }
    }
}
