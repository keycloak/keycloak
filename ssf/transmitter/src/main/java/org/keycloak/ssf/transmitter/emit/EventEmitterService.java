package org.keycloak.ssf.transmitter.emit;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.SsfEvent;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectUserLookup;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.subject.SsfNotifyAttributes;
import org.keycloak.util.JsonSerialization;

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

    protected final SsfTransmitterProvider transmitter;

    public EventEmitterService(KeycloakSession session, SsfTransmitterProvider transmitter) {
        this.session = session;
        this.transmitter = transmitter;
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
        StreamConfig stream = transmitter.streamStore().getStreamForClient(receiverClient);
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
        //    just like for native events.
        Object eventPayload = deserializeEvent(registry, eventTypeUri, eventAttributes);
        if (eventPayload == null) {
            return EmitEventResult.dropped(EmitEventStatus.INVALID_REQUEST);
        }

        // 6. Build the SET (sub_id verbatim from the emitter) and hand
        //    off to the existing dispatcher.
        SecurityEventTokenMapper mapper = transmitter.securityEventTokenMapper();
        SsfSecurityEventToken token = mapper.generateSyntheticEvent(stream, eventTypeUri, eventPayload, subjectId);
        if (token == null) {
            return EmitEventResult.dropped(EmitEventStatus.INVALID_REQUEST);
        }

        SecurityEventTokenDispatcher dispatcher = transmitter.securityEventTokenDispatcher();
        // Filters above mirror what dispatcher.dispatchEvent would run,
        // so deliverEvent is the right entry point — same async outbox
        // path native events take, no double filtering.
        dispatcher.deliverEvent(token, stream);

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

    protected boolean isStreamEvent(String eventTypeUri) {
        return SsfStreamVerificationEvent.TYPE.equals(eventTypeUri)
                || SsfStreamUpdatedEvent.TYPE.equals(eventTypeUri);
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
                    && SsfNotifyAttributes.isUserExcluded(resolved.user(), receiverClientId);
            boolean orgExcluded = resolved.organization() != null
                    && SsfNotifyAttributes.isOrganizationExcluded(resolved.organization(), receiverClientId);
            return !userExcluded && !orgExcluded;
        }

        boolean userNotified = resolved.user() != null
                && SsfNotifyAttributes.isUserNotified(resolved.user(), receiverClientId);
        boolean orgNotified = resolved.organization() != null
                && SsfNotifyAttributes.isOrganizationNotified(resolved.organization(), receiverClientId);
        return userNotified || orgNotified;
    }

    protected Object deserializeEvent(SsfEventRegistry registry, String eventTypeUri, Map<String, Object> eventAttributes) {
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
        } catch (Exception e) {
            log.debugf(e, "Failed to deserialize event attributes for type %s", eventTypeUri);
            return null;
        }
    }

    protected SsfEventRegistry registryOrNull() {
        // Reach the registry via the same Ssf.events() entry point the
        // transmitter uses internally — keeps the emitter independent of
        // any future provider-level registry getter.
        try {
            return Ssf.events().getRegistry();
        } catch (Exception e) {
            log.warn("SSF event registry not available", e);
            return null;
        }
    }
}
