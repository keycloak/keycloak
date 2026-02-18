package org.keycloak.protocol.ssf.event.processor;

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.endpoint.SsfSetPushDeliveryFailureResponse;
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
import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderConfig;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationException;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationState;
import org.keycloak.protocol.ssf.receiver.verification.SsfStreamVerificationStore;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;

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

        KeycloakSession session = eventContext.getSession();
        SsfReceiver receiver = eventContext.getReceiver();

        validateSecurityEventToken(securityEventToken, session, receiver);

        KeycloakContext keycloakContext = session.getContext();

        Map<String, SsfEvent> events = securityEventToken.getEvents();
        SsfReceiverRegistrationProviderConfig receiverProviderConfig = receiver.getConfig();

        RealmModel realm = keycloakContext.getRealm();
        String receiverAlias = receiver.getConfig().getAlias();
        LOG.debugf("Processing SSF events for security event token. realm=%s receiver=%s jti=%s streamId=%s eventCount=%s",
                realm.getName(), receiverAlias, securityEventToken.getId(), receiverProviderConfig.getStreamId(), events.size());

        int successfullyProcessedEventCounter = 0;
        for (var entry : events.entrySet()) {
            String eventId = securityEventToken.getId();
            String securityEventType = entry.getKey();
            SsfEvent securityEventData = entry.getValue();
            try {
                SsfEvent ssfEvent = narrowEventPayloadToSecurityEvent(securityEventType, securityEventData, securityEventToken);

                LOG.debugf("Processing SSF Event. realm=%s receiver=%s jti=%s streamId=%s eventType=%s",
                        realm.getName(), receiverAlias, securityEventToken.getId(), receiverProviderConfig.getStreamId(), ssfEvent.getEventType());

                if (ssfEvent instanceof VerificationEvent verificationEvent) {
                    // special case: handle verification event
                    // See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#name-verification
                    if (events.size() > 1) {
                        LOG.warnf("Found more than one security event for token with verification request. realm=%s receiver=%s jti=%s streamId=%s eventType=%s",
                                realm.getName(), receiverAlias, securityEventToken.getId(), receiverProviderConfig.getStreamId(), ssfEvent.getEventType());
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
        }

        boolean allEventsProcessedSuccessfully = successfullyProcessedEventCounter == events.size();
        eventContext.setProcessedSuccessfully(allEventsProcessedSuccessfully);
    }

    /**
     * Validate parsed Security Event Token.
     * @param securityEventToken
     * @param session
     * @param receiver
     */
    protected void validateSecurityEventToken(SecurityEventToken securityEventToken, KeycloakSession session, SsfReceiver receiver) {
        checkIssuer(session, receiver, securityEventToken, securityEventToken.getIssuer());
        checkAudience(session, receiver, securityEventToken, securityEventToken.getAudience());
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
            throw new SecurityEventTokenParsingException("Could not narrow security event", e);
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
        SsfReceiverRegistrationProviderConfig receiverProviderConfig = receiver.getConfig();
        String receiverAlias = receiverProviderConfig.getAlias();

        if (!receiverProviderConfig.getStreamId().equals(streamId)) {
            LOG.warnf("Stream Verification failed! StreamId mismatch. realm=%s  receiver=%s jti=%s eventType=%s expectedStreamId=%s actualStreamId=%s",
                    realm.getName(), receiverAlias, jti, verificationEvent.getEventType(), receiverProviderConfig.getStreamId(), streamId);
            return false;
        }

        SsfStreamVerificationState verificationState = getVerificationState(realm, receiver, receiverAlias, receiverProviderConfig.getStreamId());

        String givenState = verificationEvent.getState();
        String expectedState = verificationState == null ? null : verificationState.getState();

        if (expectedState != null && expectedState.equals(givenState)) {
            // Only clear verification state on successful verification
            verificationStore.clearVerificationState(realm, receiverAlias, receiverProviderConfig.getStreamId());
            LOG.debugf("Stream Verification successful. realm=%s receiver=%s jti=%s state=%s",
                    realm.getName(), receiverAlias, jti, givenState);
            return true;
        }

        LOG.warnf("Stream Verification failed. realm=%s receiver=%s jti=%s eventType=%s givenState=%s expectedState=%s",
                realm.getName(), receiverAlias, jti, verificationEvent.getEventType(), receiverProviderConfig.getStreamId(), streamId);
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
        String receiverAlias = eventContext.getReceiver().getConfig().getAlias();
        if (securityEventToken.getSubjectId() instanceof OpaqueSubjectId opaqueSubjectId) {
            // TODO handle stream status update, do we need to do anything here? currently streams are managed outside of Keycloak.
            LOG.debugf("Handled stream updated event. realm=%s receiver=%s jti=%s streamId=%s newStatus=%s",
                    realm.getName(), receiverAlias, jti, opaqueSubjectId.getId(), streamUpdatedEvent.getStatus());
        }

        return true;
    }

    /**
     * Delegate generic SSF event handling to {@link SsfEventListener}.
     *
     * @param
     * @param eventId
     * @param event
     */
    protected void handleEvent(SsfEventContext eventContext, String eventId, SsfEvent event) {
        ssfEventListener.onEvent(eventContext, eventId, event);
    }


    protected void checkIssuer(KeycloakSession session, SsfReceiver receiver, SecurityEventToken securityEventToken, String issuer) {

        String expectedIssuer = receiver.getConfig() != null ? receiver.getConfig().getIssuer() : null;

        if (!isValidIssuer(receiver, expectedIssuer, issuer)) {
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_ISSUER, "Invalid issuer");
        }
    }

    protected void checkAudience(KeycloakSession session, SsfReceiver receiver, SecurityEventToken securityEventToken, String[] audience) {

        Set<String> expectedAudience = receiver.getConfig() != null && receiver.getConfig().getStreamAudience() != null ? receiver.getConfig().streamAudience() : null;

        if (expectedAudience == null) {
            // No expected audience configured for receiver, fallback to realm issuer is no audience is set
            String fallbackAudience = getFallbackAudience(session);
            expectedAudience = Set.of(fallbackAudience);
        }

        if (!isValidAudience(receiver, expectedAudience, audience)) {
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_AUDIENCE, "Invalid audience");
        }
    }

    protected String getFallbackAudience(KeycloakSession session) {
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        return Urls.realmIssuer(frontendUriInfo.getBaseUri(), session.getContext().getRealm().getName());
    }

    protected boolean isValidIssuer(SsfReceiver receiver, String expectedIssuer, String issuer) {
        return expectedIssuer != null && expectedIssuer.equals(issuer);
    }

    protected boolean isValidAudience(SsfReceiver receiver, Set<String> expectedAudience, String[] audience) {
        if (audience == null || audience.length == 0) {
            return false;
        }
        // Check that at least one token audience matches an expected audience value
        for (String aud : audience) {
            if (expectedAudience.contains(aud)) {
                return true;
            }
        }
        return false;
    }
}
