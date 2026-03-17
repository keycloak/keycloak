package org.keycloak.protocol.ssf.transmitter.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.protocol.ssf.event.InitiatingEntity;
import org.keycloak.protocol.ssf.event.caep.CaepCredentialChange;
import org.keycloak.protocol.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.protocol.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.protocol.ssf.event.subjects.ComplexSubjectId;
import org.keycloak.protocol.ssf.event.subjects.IssuerSubjectId;
import org.keycloak.protocol.ssf.event.subjects.OpaqueSubjectId;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;

import org.jboss.logging.Logger;


/**
 * Generator for Security Event Tokens (SETs).
 */
public class SecurityEventTokenMapper {

    protected static final Logger log = Logger.getLogger(SecurityEventTokenMapper.class);

    protected static final Pattern USER_LOGGED_OUT_BY_ADMIN_PATH_PATTERN = Pattern.compile("^users/(.*)/logout$");

    private final String issuer;

    public SecurityEventTokenMapper(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Generates a verification event for a stream.
     *
     * @param stream The stream configuration
     * @param state  The verification state
     * @return The verification event as a JSON string
     */
    public SsfSecurityEventToken generateVerificationEvent(StreamConfig stream, String state) {
        try {
            SsfSecurityEventToken verificationEventToken = newSecurityEventToken(stream);

            // Set transaction ID
            verificationEventToken.setTxn(UUID.randomUUID().toString());

            // Set subject ID
            OpaqueSubjectId subId = new OpaqueSubjectId();
            subId.setId(stream.getStreamId());
            verificationEventToken.setSubjectId(subId);

            // Set events
            Map<String, Object> events = new HashMap<>();
            SsfStreamVerificationEvent verificationEvent = new SsfStreamVerificationEvent();
            verificationEvent.setState(state);
            events.put(SsfStreamVerificationEvent.TYPE, verificationEvent);
            verificationEventToken.setEvents(events);

            return verificationEventToken;
        } catch (Exception e) {
            log.error("Error generating verification event", e);
            return null;
        }
    }

    protected SsfSecurityEventToken newSecurityEventToken(StreamConfig stream) {
        SsfSecurityEventToken securityEventToken = new SsfSecurityEventToken();

        securityEventToken.setJti(SecretGenerator.getInstance().generateSecureID());
        securityEventToken.setIss(issuer);
        securityEventToken.setIat(Time.currentTime());

        // Set the SET audience to the stream's audience
        if (stream.getDelivery() != null && stream.getDelivery().getEndpointUrl() != null) {
            securityEventToken.setAud(stream.getAudience().toArray(new String[0]));
        }

        return securityEventToken;
    }


    /**
     * Generates a session revoked event.
     *
     * @param event
     * @param sessionId The ID of the revoked session
     * @param userId    The ID of the user
     * @param stream
     * @param reason    The reason for the revocation
     * @return The session revoked event as a SecurityEventToken
     */
    public SsfSecurityEventToken generateSessionRevokedEvent(Event userEvent, StreamConfig stream, String reason) {
        try {

            String sessionId = userEvent.getSessionId();
            String userId = userEvent.getUserId();

            SsfSecurityEventToken eventToken = newSecurityEventToken(stream);
            eventToken.setTxn(UUID.randomUUID().toString());

            // Set subject ID (complex subject with user and session)
            ComplexSubjectId subId = new ComplexSubjectId();

            IssuerSubjectId userSubject = new IssuerSubjectId();
            userSubject.setIss(issuer);
            userSubject.setSub(userId);

            OpaqueSubjectId sessionSubject = new OpaqueSubjectId();
            sessionSubject.setId(sessionId);

            subId.setUser(userSubject);
            subId.setSession(sessionSubject);
            eventToken.setSubjectId(subId);

            // Set events
            Map<String, Object> events = new HashMap<>();
            CaepSessionRevoked sessionRevokedEvent = new CaepSessionRevoked();

            if (reason != null) {
                sessionRevokedEvent.setReasonAdmin(Map.of("en", reason));
            }
            sessionRevokedEvent.setEventTimestamp(Time.currentTime());

            events.put(CaepSessionRevoked.TYPE, sessionRevokedEvent);
            eventToken.setEvents(events);

            return eventToken;
        } catch (Exception e) {
            log.error("Error generating session revoked event", e);
            return null;
        }
    }

    /**
     * Generates a credential change event.
     *
     * @param userId         The ID of the user
     * @param credentialType The type of credential that changed
     * @param stream
     * @return The credential change event as a SecurityEventToken
     */
    public SsfSecurityEventToken generateCredentialChangeEvent(Event userEvent, StreamConfig stream) {
        try {

            String userId = userEvent.getUserId();
            String credentialType = userEvent.getDetails().get("credential_type");

            SsfSecurityEventToken event = newSecurityEventToken(stream);
            event.setTxn(UUID.randomUUID().toString());

            // Set subject ID
            IssuerSubjectId subId = new IssuerSubjectId();
            subId.setIss(issuer);
            subId.setSub(userId);
            event.setSubjectId(subId);

            String caepCredentialType = narrowCaepCredentialType(credentialType);

            // Set events
            Map<String, Object> events = new HashMap<>();
            CaepCredentialChange credentialChangeEvent = new CaepCredentialChange();
            credentialChangeEvent.setChangeType(CaepCredentialChange.ChangeType.UPDATE);
            credentialChangeEvent.setEventTimestamp(Time.currentTime());
            credentialChangeEvent.setCredentialType(caepCredentialType);
            credentialChangeEvent.setInitiatingEntity(InitiatingEntity.USER);

            events.put(CaepCredentialChange.TYPE, credentialChangeEvent);
            event.setEvents(events);

            return event;
        } catch (Exception e) {
            log.error("Error generating credential change event", e);
            return null;
        }
    }

    protected String narrowCaepCredentialType(String credentialType) {
        return credentialType;
    }

    public boolean isSupportedEvent(Event event, StreamConfig stream) {
        return switch (event.getType()) {
            case LOGOUT, UPDATE_CREDENTIAL -> true;
            default -> false;
        };
    }

    public SsfSecurityEventToken toSecurityEvent(Event event, StreamConfig stream) {

        if (!isSupportedEvent(event, stream)) {
            return null;
        }

        SsfSecurityEventToken securityEvent = switch (event.getType()) {

            case LOGOUT -> {

                // ignore expired session cleanup, we only want to propagate real logouts!
                if (shouldIgnoreLogout(event, stream)) {
                    yield null;
                }

                yield generateSessionRevokedEvent(event, stream, "User logout");
            }

            case UPDATE_CREDENTIAL -> {

                // ignore credential changes for credentials that are not used for authentication
                if (shouldIgnoreCredentialChange(event, stream)) {
                    yield null;
                }

                yield generateCredentialChangeEvent(event, stream);
            }
            // Add more event mappings here as needed

            default -> {

                if (shouldIgnoreEvent(event, stream)) {
                    yield null;
                }

                yield generateSecurityEventFromEvent(event, stream);
            }
        };
        // Map Keycloak events to SSF events

        return securityEvent;
    }

    protected boolean shouldIgnoreEvent(Event event, StreamConfig stream) {
        return false;
    }

    protected boolean shouldIgnoreCredentialChange(Event event, StreamConfig stream) {
        return false;
    }

    protected boolean shouldIgnoreLogout(Event event, StreamConfig stream) {
        String reason = event.getDetails().get(Details.REASON);
        return Details.USER_SESSION_EXPIRED_REASON.equals(reason) || Details.INVALID_USER_SESSION_REMEMBER_ME_REASON.equals(reason);
    }

    protected static SsfSecurityEventToken generateSecurityEventFromEvent(Event event, StreamConfig stream) {
        return null;
    }

    public SsfSecurityEventToken toSecurityEvent(AdminEvent adminEvent, StreamConfig stream) {

        String path = adminEvent.getResourcePath();
        Matcher matcher = USER_LOGGED_OUT_BY_ADMIN_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {

            String userId = matcher.group(1);
            if (userId == null) {
                return null;
            }

            return generateLogoutEventForAdminLogoutAllUserSessions(userId, adminEvent, stream);
        }

        return null;
    }

    protected SsfSecurityEventToken generateLogoutEventForAdminLogoutAllUserSessions(String userId, AdminEvent adminEvent, StreamConfig stream) {

        Event event = new Event();
        event.setType(EventType.LOGOUT);
        event.setUserId(userId);
        event.setSessionId("ALL"); // all sessions
        event.setDetails(new HashMap<>());
        event.getDetails().put("admin", "true");
        event.getDetails().put(Details.REASON, "logout_all_user_sessions");

        return toSecurityEvent(event, stream);
    }
}
