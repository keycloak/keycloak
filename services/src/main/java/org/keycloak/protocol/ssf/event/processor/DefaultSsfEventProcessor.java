package org.keycloak.protocol.ssf.event.processor;

import java.util.Map;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.SsfStandardEvents;
import org.keycloak.protocol.ssf.event.listener.SsfEventListener;
import org.keycloak.protocol.ssf.event.parser.SecurityEventTokenParsingException;
import org.keycloak.protocol.ssf.event.subjects.OpaqueSubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.stream.StreamUpdatedEvent;
import org.keycloak.protocol.ssf.event.types.stream.VerificationEvent;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.SsfReceiverProviderConfig;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationException;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationState;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;

import org.jboss.logging.Logger;

/**
 * Default implementation of a {@link SsfEventProcessor}.
 * <p>
 * Handles processing of generic SSF events by delegation to {@link SsfEventListener SsfEventListener's} .
 * SSF stream related events like the {@link VerificationEvent} and {@link StreamUpdatedEvent} are handled directly by this processor.
 */
public class DefaultSsfEventProcessor implements SsfEventProcessor {

    protected static final Logger LOG = Logger.getLogger(DefaultSsfEventProcessor.class);

    protected final SsfEventListener ssfEventListener;

    protected final SsfStreamVerificationStore verificationStore;

    public DefaultSsfEventProcessor(SsfEventListener ssfEventListener, SsfStreamVerificationStore verificationStore) {
        this.ssfEventListener = ssfEventListener;
        this.verificationStore = verificationStore;
    }

    @Override
    public void processEvents(SecurityEventToken securityEventToken, SsfEventContext eventContext) {

        KeycloakContext keycloakContext = eventContext.getSession().getContext();

        Map<String, SsfEvent> events = securityEventToken.getEvents();
        SsfReceiverProviderConfig receiverProviderConfig = eventContext.getReceiver().getConfig();

        LOG.debugf("Processing SSF events for security event token. realm=%s jti=%s streamId=%s eventCount=%s", keycloakContext.getRealm().getName(), securityEventToken.getId(), receiverProviderConfig.getStreamId(), events.size());

        for (var entry : events.entrySet()) {
            String eventId = securityEventToken.getId();
            String securityEventType = entry.getKey();
            SsfEvent securityEventData = entry.getValue();

            int successfullyProcessedEventCounter = 0;
            try {
                SsfEvent ssfEvent = narrowEventPayloadToSecurityEvent(securityEventType, securityEventData, securityEventToken);

                if (ssfEvent instanceof VerificationEvent verificationEvent) {
                    // special case: handle verification event
                    // See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#name-verification
                    if (events.size() > 1) {
                        LOG.warnf("Found more than one security event for token with verification request. %s", eventId);
                    }

                    boolean verified = handleVerificationEvent(eventContext, verificationEvent, eventId);
                    if (verified) {
                        successfullyProcessedEventCounter++;
                        break;
                    }
                } else if (ssfEvent instanceof StreamUpdatedEvent streamUpdatedEvent) {
                    // special case: handle stream updated event, e.g. for stream enabled -> stream paused / disabled
                    // See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#name-stream-updated-event
                    boolean streamUpdated = handleStreamUpdatedEvent(eventContext, streamUpdatedEvent, eventId, securityEventToken);
                    eventContext.setProcessedSuccessfully(streamUpdated);
                    if (streamUpdated) {
                        successfullyProcessedEventCounter++;
                        break;
                    }
                } else {
                    // handle generic SSF event
                    handleEvent(eventContext, eventId, ssfEvent);
                    successfullyProcessedEventCounter++;
                }
            } catch (final SecurityEventTokenParsingException spe) {
                eventContext.setProcessedSuccessfully(false);
                throw spe;
            }

            boolean allEventsProcessedSuccessfully = successfullyProcessedEventCounter == events.size();
            eventContext.setProcessedSuccessfully(allEventsProcessedSuccessfully);
        }
    }

    protected SsfEvent narrowEventPayloadToSecurityEvent(String eventType, SsfEvent rawSsfEvent, SecurityEventToken securityEventToken) {

        Class<? extends SsfEvent> eventClass = getEventType(eventType);

        if (eventClass == null) {
            throw new SecurityEventTokenParsingException("Could not parse security event. Unknown event type: " + eventType);
        }

        try {
            SsfEvent ssfEvent = eventClass.cast(rawSsfEvent);
            ssfEvent.setEventType(eventType);
            if (ssfEvent.getSubjectId() == null) {
                // use subjectId from SET if none was provided for the event explicitly.
                ssfEvent.setSubjectId(securityEventToken.getSubjectId());
            }

            return ssfEvent;
        } catch (Exception e) {
            throw new SecurityEventTokenParsingException("Could not narrow security event.", e);
        }
    }

    protected Class<? extends SsfEvent> getEventType(String securityEventType) {
        return SsfStandardEvents.getSecurityEventType(securityEventType);
    }

    protected boolean handleVerificationEvent(SsfEventContext eventContext, VerificationEvent verificationEvent, String jti) {

        KeycloakContext keycloakContext = eventContext.getSession().getContext();

        String streamId = extractStreamIdFromVerificationEvent(eventContext, verificationEvent);

        RealmModel realm = keycloakContext.getRealm();
        SsfReceiver receiver = eventContext.getReceiver();
        SsfReceiverProviderConfig receiverProviderConfig = receiver.getConfig();

        if (!receiverProviderConfig.getStreamId().equals(streamId)) {
            LOG.debugf("Verification failed! StreamId mismatch. jti=%s expectedStreamId=%s actualStreamId=%s", jti, receiverProviderConfig.getStreamId(), streamId);
            return false;
        }

        SsfStreamVerificationState verificationState = getVerificationState(realm, receiver, receiverProviderConfig.getAlias(), receiverProviderConfig.getStreamId());

        String givenState = verificationEvent.getState();
        String expectedState = verificationState == null ? null : verificationState.getState();

        if (givenState.equals(expectedState)) {
            LOG.debugf("Verification successful. jti=%s state=%s", jti, givenState);
            verificationStore.clearVerificationState(realm, receiverProviderConfig.getAlias(), receiverProviderConfig.getStreamId());
            return true;
        }

        LOG.warnf("Verification failed. jti=%s state=%s", jti, givenState);
        return false;
    }

    protected String extractStreamIdFromVerificationEvent(SsfEventContext eventContext, SsfEvent ssfEvent) {
        // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-7.1.4.2

        String streamId = null;

        // See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-7.1.4.1
        // try to extract subjectId from securityEvent
        SubjectId subjectId = ssfEvent.getSubjectId();
        if (subjectId instanceof OpaqueSubjectId opaqueSubjectId) {
            streamId = opaqueSubjectId.getId();
        }

        if (streamId == null) {
            // as a fallback, try to extract subjectId from securityEventToken
            subjectId = eventContext.getSecurityEventToken().getSubjectId();
            if (subjectId instanceof OpaqueSubjectId opaqueSubjectId) {
                streamId = opaqueSubjectId.getId();
            }
        }

        // TODO find a reliable way to extract the streamId from the verification event
        if (streamId == null) {
            throw new SsfStreamVerificationException("Could not find stream id for verification request");
        }
        return streamId;
    }

    protected SsfStreamVerificationState getVerificationState(RealmModel realm, SsfReceiver receiver, String alias, String streamId) {
        return verificationStore.getVerificationState(realm, alias, streamId);
    }

    protected boolean handleStreamUpdatedEvent(SsfEventContext eventContext, StreamUpdatedEvent streamUpdatedEvent, String jti, SecurityEventToken securityEventToken) {

        KeycloakContext keycloakContext = eventContext.getSession().getContext();
        RealmModel realm = keycloakContext.getRealm();

        OpaqueSubjectId opaqueSubjectId = (OpaqueSubjectId) securityEventToken.getSubjectId();

        // TODO handle stream status update, do we need to do anything here? currently streams are managed outside of Keycloak.

        LOG.debugf("Handled stream updated event. realm=%s jti=%s streamId=%s newStatus=%s", realm.getName(), jti, opaqueSubjectId.getId(), streamUpdatedEvent.getStatus());

        return true;
    }

    /**
     * Deleagte generic SSF event handling to {@link SsfEventListener}.
     *
     * @param
     * @param eventId
     * @param event
     */
    protected void handleEvent(SsfEventContext eventContext, String eventId, SsfEvent event) {
        ssfEventListener.onEvent(eventContext, eventId, event);
    }
}
