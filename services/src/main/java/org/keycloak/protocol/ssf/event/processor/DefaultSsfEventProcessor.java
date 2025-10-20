package org.keycloak.protocol.ssf.event.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.SecurityEvents;
import org.keycloak.protocol.ssf.event.listener.SsfEventListener;
import org.keycloak.protocol.ssf.event.parser.SsfParsingException;
import org.keycloak.protocol.ssf.event.subjects.OpaqueSubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.StreamUpdatedEvent;
import org.keycloak.protocol.ssf.event.types.VerificationEvent;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationException;
import org.keycloak.protocol.ssf.receiver.verification.VerificationState;
import org.keycloak.protocol.ssf.receiver.verification.VerificationStore;
import org.keycloak.protocol.ssf.spi.SsfProvider;

import java.util.Map;

public class DefaultSsfEventProcessor implements SsfEventProcessor {

    protected static final Logger log = Logger.getLogger(DefaultSsfEventProcessor.class);

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected final SsfEventListener ssfEventListener;

    protected final VerificationStore verificationStore;

    public DefaultSsfEventProcessor(SsfProvider ssfProvider, SsfEventListener ssfEventListener, VerificationStore verificationStore) {
        this.ssfEventListener = ssfEventListener;
        this.verificationStore = verificationStore;
    }

    @Override
    public void processSecurityEvents(SsfEventContext eventContext) {

        SecurityEventToken securityEventToken = eventContext.getSecurityEventToken();
        Map<String, Map<String, Object>> events = securityEventToken.getEvents();
        for (var entry : events.entrySet()) {
            String eventId = securityEventToken.getId();
            String securityEventType = entry.getKey();
            Map<String, Object> securityEventData = entry.getValue();

            try {
                SsfEvent ssfEvent = convertEventPayloadToSecurityEvent(securityEventType, securityEventData, securityEventToken);

                if (ssfEvent instanceof VerificationEvent verificationEvent) {
                    // handle verification event

                    if (events.size() > 1) {
                        log.warnf("Found more than one security event for token with verification request. %s", eventId);
                    }

                    boolean verified = handleVerificationEvent(eventContext, verificationEvent, eventId);
                    if (verified) {
                        break;
                    }
                } else if (ssfEvent instanceof StreamUpdatedEvent streamUpdatedEvent) {
                    // handle stream updated event
                    boolean streamUpdated = handleStreamUpdatedEvent(eventContext, streamUpdatedEvent, eventId);
                    if (streamUpdated) {
                        break;
                    }
                } else {
                    // handle generic SSF event
                    handleEvent(eventContext, eventId, ssfEvent);
                }
            } catch (final SsfParsingException spe) {
                eventContext.setProcessedSuccessfully(false);
                throw spe;
            }
        }

        eventContext.setProcessedSuccessfully(true);
    }

    protected SsfEvent convertEventPayloadToSecurityEvent(String securityEventType, Map<String, Object> securityEventData, SecurityEventToken securityEventToken) {

        Class<? extends SsfEvent> eventClass = getEventType(securityEventType);

        if (eventClass == null) {
            throw new SsfParsingException("Could not parse security event. Unknown event type: " + securityEventType);
        }

        try {
            SsfEvent ssfEvent = convertToSsfEvent(securityEventData, eventClass);
            ssfEvent.setEventType(securityEventType);
            if (ssfEvent.getSubjectId() == null) {
                // use subjectId from SET if none was provided for the event explicitly.
                ssfEvent.setSubjectId(securityEventToken.getSubjectId());
            }

            return ssfEvent;
        } catch (Exception e) {
            throw new SsfParsingException("Could not parse security event.", e);
        }
    }

    protected SsfEvent convertToSsfEvent(Map<String, Object> securityEventData, Class<? extends SsfEvent> eventClass) {
        return OBJECT_MAPPER.convertValue(securityEventData, eventClass);
    }

    protected Class<? extends SsfEvent> getEventType(String securityEventType) {
        return SecurityEvents.getSecurityEventType(securityEventType);
    }

    protected boolean handleVerificationEvent(SsfEventContext processingContext, VerificationEvent verificationEvent, String jti) {

        KeycloakContext keycloakContext = processingContext.getSession().getContext();

        String streamId = extractStreamIdFromVerificationEvent(processingContext, verificationEvent);

        RealmModel realm = keycloakContext.getRealm();
        ReceiverModel receiverModel = processingContext.getReceiver().getReceiverModel();

        if (!receiverModel.getStreamId().equals(streamId)) {
            log.debugf("Verification failed! StreamId mismatch. jti=%s expectedStreamId=%s actualStreamId=%s", jti, receiverModel.getStreamId(), streamId);
            return false;
        }

        VerificationState verificationState = getVerificationState(realm, receiverModel);

        String givenState = verificationEvent.getState();
        String expectedState = verificationState == null ? null : verificationState.getState();

        if (givenState.equals(expectedState)) {
            log.debugf("Verification successful!. jti=%s state=%s", jti, givenState);
            verificationStore.clearVerificationState(realm, receiverModel);
            return true;
        }

        log.warnf("Verification failed. jti=%s state=%s", jti, givenState);
        throw new SsfStreamVerificationException("Verification state mismatch.");
    }

    protected boolean handleStreamUpdatedEvent(SsfEventContext processingContext, StreamUpdatedEvent streamUpdatedEvent, String jti) {

        KeycloakContext keycloakContext = processingContext.getSession().getContext();
        RealmModel realm = keycloakContext.getRealm();

        OpaqueSubjectId opaqueSubjectId = (OpaqueSubjectId) processingContext.getSecurityEventToken().getSubjectId();

        log.debugf("Handling stream updated event. realm=%s jti=%s streamId=%s newStatus=%s", realm.getName(), jti, opaqueSubjectId.getId(), streamUpdatedEvent.getStatus());

        return false;
    }


    protected VerificationState getVerificationState(RealmModel realm, ReceiverModel receiverModel) {
        return verificationStore.getVerificationState(realm, receiverModel);
    }

    protected String extractStreamIdFromVerificationEvent(SsfEventContext processingContext, SsfEvent ssfEvent) {
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
            subjectId = processingContext.getSecurityEventToken().getSubjectId();
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

    protected void handleEvent(SsfEventContext eventContext, String eventId, SsfEvent event) {
        ssfEventListener.onEvent(eventContext, eventId, event);
    }
}
