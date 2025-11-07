package org.keycloak.protocol.ssf.event.processor;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.SecurityEvents;
import org.keycloak.protocol.ssf.event.listener.SsfEventListener;
import org.keycloak.protocol.ssf.event.parser.SecurityEventTokenParsingException;
import org.keycloak.protocol.ssf.event.subjects.OpaqueSubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.StreamUpdatedEvent;
import org.keycloak.protocol.ssf.event.types.VerificationEvent;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationException;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationState;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.util.JsonSerialization;

import java.util.Map;

public class DefaultSsfEventProcessor implements SsfEventProcessor {

    protected static final Logger log = Logger.getLogger(DefaultSsfEventProcessor.class);

    protected final SsfEventListener ssfEventListener;

    protected final SsfStreamVerificationStore verificationStore;

    public DefaultSsfEventProcessor(SsfProvider ssfProvider, SsfEventListener ssfEventListener, SsfStreamVerificationStore verificationStore) {
        this.ssfEventListener = ssfEventListener;
        this.verificationStore = verificationStore;
    }

    @Override
    public void processSecurityEvents(SsfSecurityEventContext securityEventContext) {

        SecurityEventToken securityEventToken = securityEventContext.getSecurityEventToken();
        KeycloakContext keycloakContext = securityEventContext.getSession().getContext();

        Map<String, Map<String, Object>> events = securityEventToken.getEvents();
        SsfReceiverModel receiverModel = securityEventContext.getReceiver().getReceiverModel();

        log.debugf("Processing SSF events for security event token. realm=%s jti=%s streamId=%s eventCount=%s",
                keycloakContext.getRealm().getName(), securityEventToken.getId(), receiverModel.getStreamId(), events.size());

        for (var entry : events.entrySet()) {
            String eventId = securityEventToken.getId();
            String securityEventType = entry.getKey();
            Map<String, Object> securityEventData = entry.getValue();

            int successfullyProcessedEventCounter = 0;
            try {
                SsfEvent ssfEvent = convertEventPayloadToSecurityEvent(securityEventType, securityEventData, securityEventToken);

                if (ssfEvent instanceof VerificationEvent verificationEvent) {
                    // special case: handle verification event
                    // See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#name-verification
                    if (events.size() > 1) {
                        log.warnf("Found more than one security event for token with verification request. %s", eventId);
                    }

                    boolean verified = handleVerificationEvent(securityEventContext, verificationEvent, eventId);
                    if (verified) {
                        successfullyProcessedEventCounter++;
                        break;
                    }
                } else if (ssfEvent instanceof StreamUpdatedEvent streamUpdatedEvent) {
                    // special case: handle stream updated event, e.g. for stream enabled -> stream paused / disabled
                    // See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#name-stream-updated-event
                    boolean streamUpdated = handleStreamUpdatedEvent(securityEventContext, streamUpdatedEvent, eventId);
                    securityEventContext.setProcessedSuccessfully(streamUpdated);
                    if (streamUpdated) {
                        successfullyProcessedEventCounter++;
                        break;
                    }
                } else {
                    // handle generic SSF event
                    handleEvent(securityEventContext, eventId, ssfEvent);
                    successfullyProcessedEventCounter++;
                }
            } catch (final SecurityEventTokenParsingException spe) {
                securityEventContext.setProcessedSuccessfully(false);
                throw spe;
            }

            boolean allEventsProcessedSuccessfully = successfullyProcessedEventCounter == events.size();
            securityEventContext.setProcessedSuccessfully(allEventsProcessedSuccessfully);
        }
    }

    protected SsfEvent convertEventPayloadToSecurityEvent(String securityEventType, Map<String, Object> securityEventData, SecurityEventToken securityEventToken) {

        Class<? extends SsfEvent> eventClass = getEventType(securityEventType);

        if (eventClass == null) {
            throw new SecurityEventTokenParsingException("Could not parse security event. Unknown event type: " + securityEventType);
        }

        try {
            SsfEvent ssfEvent = convertEventDataToEvent(securityEventData, eventClass);
            ssfEvent.setEventType(securityEventType);
            if (ssfEvent.getSubjectId() == null) {
                // use subjectId from SET if none was provided for the event explicitly.
                ssfEvent.setSubjectId(securityEventToken.getSubjectId());
            }

            return ssfEvent;
        } catch (Exception e) {
            throw new SecurityEventTokenParsingException("Could not parse security event.", e);
        }
    }

    protected SsfEvent convertEventDataToEvent(Map<String, Object> securityEventData, Class<? extends SsfEvent> eventClass) {
        return JsonSerialization.mapper.convertValue(securityEventData, eventClass);
    }

    protected Class<? extends SsfEvent> getEventType(String securityEventType) {
        return SecurityEvents.getSecurityEventType(securityEventType);
    }

    protected boolean handleVerificationEvent(SsfSecurityEventContext securityEventContext, VerificationEvent verificationEvent, String jti) {

        KeycloakContext keycloakContext = securityEventContext.getSession().getContext();

        String streamId = extractStreamIdFromVerificationEvent(securityEventContext, verificationEvent);

        RealmModel realm = keycloakContext.getRealm();
        SsfReceiverModel receiverModel = securityEventContext.getReceiver().getReceiverModel();

        if (!receiverModel.getStreamId().equals(streamId)) {
            log.debugf("Verification failed! StreamId mismatch. jti=%s expectedStreamId=%s actualStreamId=%s", jti, receiverModel.getStreamId(), streamId);
            return false;
        }

        SsfStreamVerificationState verificationState = getVerificationState(realm, receiverModel);

        String givenState = verificationEvent.getState();
        String expectedState = verificationState == null ? null : verificationState.getState();

        if (givenState.equals(expectedState)) {
            log.debugf("Verification successful!. jti=%s state=%s", jti, givenState);
            verificationStore.clearVerificationState(realm, receiverModel.getAlias(), receiverModel.getStreamId());
            return true;
        }

        log.warnf("Verification failed. jti=%s state=%s", jti, givenState);
        return false;
    }

    protected boolean handleStreamUpdatedEvent(SsfSecurityEventContext securityEventContext, StreamUpdatedEvent streamUpdatedEvent, String jti) {

        KeycloakContext keycloakContext = securityEventContext.getSession().getContext();
        RealmModel realm = keycloakContext.getRealm();

        SecurityEventToken securityEventToken = securityEventContext.getSecurityEventToken();
        OpaqueSubjectId opaqueSubjectId = (OpaqueSubjectId) securityEventToken.getSubjectId();

        securityEventContext.getReceiver().updateStreamStatus(streamUpdatedEvent.getStatus());

        log.debugf("Handled stream updated event. realm=%s jti=%s streamId=%s newStatus=%s", realm.getName(), jti, opaqueSubjectId.getId(), streamUpdatedEvent.getStatus());

        return true;
    }


    protected SsfStreamVerificationState getVerificationState(RealmModel realm, SsfReceiverModel receiverModel) {
        return verificationStore.getVerificationState(realm, receiverModel.getAlias(), receiverModel.getStreamId());
    }

    protected String extractStreamIdFromVerificationEvent(SsfSecurityEventContext securityEventContext, SsfEvent ssfEvent) {
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
            subjectId = securityEventContext.getSecurityEventToken().getSubjectId();
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

    protected void handleEvent(SsfSecurityEventContext securityEventContext, String eventId, SsfEvent event) {
        ssfEventListener.onEvent(securityEventContext, eventId, event);
    }
}
