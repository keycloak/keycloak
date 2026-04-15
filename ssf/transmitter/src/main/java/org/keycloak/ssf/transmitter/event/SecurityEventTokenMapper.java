package org.keycloak.ssf.transmitter.event;

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
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.jboss.logging.Logger;


/**
 * Generator for Security Event Tokens (SETs).
 */
public class SecurityEventTokenMapper {

    protected static final Logger log = Logger.getLogger(SecurityEventTokenMapper.class);

    protected static final Pattern USER_LOGGED_OUT_BY_ADMIN_PATH_PATTERN = Pattern.compile("^users/(.*)/logout$");

    private final String issuer;

    private final KeycloakSession session;

    private final SsfTransmitterConfig transmitterConfig;

    public SecurityEventTokenMapper(KeycloakSession session, String issuer, SsfTransmitterConfig transmitterConfig) {
        this.session = session;
        this.issuer = issuer;
        this.transmitterConfig = transmitterConfig;
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

            SubjectId userSubject = buildUserSubjectId(userId, stream);

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
            event.setSubjectId(buildUserSubjectId(userId, stream));

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

    /**
     * Builds the SSF subject identifier for a Keycloak user according to
     * the stream's configured user subject format (falling back to
     * {@link SsfUserSubjectFormats#DEFAULT iss_sub}). Invoked for every
     * event type whose {@code sub_id} carries a user identifier — both
     * as the {@code user} field of a {@link ComplexSubjectId} (e.g. for
     * {@link CaepSessionRevoked}) and as the top-level {@code sub_id}
     * of simpler events (e.g. {@link CaepCredentialChange}).
     *
     * <p>When the configured format is {@code email} and the user has
     * no email address on record (or the user itself cannot be resolved
     * — e.g. deleted mid-event), this method logs a warning and falls
     * back to {@code iss_sub} rather than dropping the event. Losing a
     * receiver signal is worse than delivering it with a less-preferred
     * subject format.
     */
    protected SubjectId buildUserSubjectId(String userId, StreamConfig stream) {

        String format = SsfUserSubjectFormats.resolveForStream(stream, transmitterConfig);

        if (EmailSubjectId.TYPE.equals(format)) {
            String email = lookupUserEmail(userId);
            if (email != null && !email.isBlank()) {
                EmailSubjectId emailSubject = new EmailSubjectId();
                emailSubject.setEmail(email);
                return emailSubject;
            }
            log.warnf("Configured user subject format is 'email' but no email is available for user. "
                    + "Falling back to 'iss_sub'. userId=%s streamId=%s", userId, stream != null ? stream.getStreamId() : null);
        }

        IssuerSubjectId issSubject = new IssuerSubjectId();
        issSubject.setIss(issuer);
        issSubject.setSub(userId);
        return issSubject;
    }

    /**
     * Resolves a Keycloak user's email address for the current realm.
     * Returns {@code null} when the session is not available, the user
     * cannot be resolved, or the user has no email — callers fall back
     * to {@code iss_sub} in that case.
     */
    protected String lookupUserEmail(String userId) {
        if (session == null || userId == null) {
            return null;
        }
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return null;
        }
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            return null;
        }
        return user.getEmail();
    }

    /**
     * Cheap predicate that returns {@code true} iff
     * {@link #toSecurityEvent(Event, StreamConfig)} would produce a
     * non-null SET for {@code event}, based only on event type + details
     * currently present on the event. Callers use this to short-circuit
     * the per-event stream lookup in {@code SsfTransmitterEventListener}
     * — if no mapping is possible, there is no point in hitting the
     * client store to find eligible streams.
     *
     * <p>The check deliberately mirrors the {@code switch} in
     * {@link #toSecurityEvent(Event, StreamConfig)} so the two stay in
     * sync. New event types added to the mapper must be reflected here
     * too, otherwise the listener will silently drop them.
     */
    public boolean canConvert(Event event) {
        if (event == null || shouldIgnoreEvent(event)) {
            return false;
        }
        return switch (event.getType()) {
            case LOGOUT -> !shouldIgnoreLogout(event);
            case UPDATE_CREDENTIAL -> !shouldIgnoreCredentialChange(event);
            default -> false;
        };
    }

    /**
     * Cheap predicate that returns {@code true} iff
     * {@link #toSecurityEvent(AdminEvent, StreamConfig)} would produce a
     * non-null SET for {@code adminEvent}. Currently the only mapped
     * admin operation is the "log out all user sessions" path
     * ({@code users/{userId}/logout}); everything else returns null and
     * should short-circuit before any stream lookup happens.
     */
    public boolean canConvert(AdminEvent adminEvent) {
        if (adminEvent == null) {
            return false;
        }
        if (!ResourceType.USER.equals(adminEvent.getResourceType())) {
            return false;
        }
        String path = adminEvent.getResourcePath();
        if (path == null) {
            return false;
        }
        return USER_LOGGED_OUT_BY_ADMIN_PATH_PATTERN.matcher(path).matches();
    }

    public SsfSecurityEventToken toSecurityEvent(Event event, StreamConfig stream) {

        SsfSecurityEventToken securityEvent = switch (event.getType()) {

            case LOGOUT -> {

                // ignore expired session cleanup, we only want to propagate real logouts!
                if (shouldIgnoreLogout(event)) {
                    yield null;
                }

                yield generateSessionRevokedEvent(event, stream, "User logout");
            }

            case UPDATE_CREDENTIAL -> {

                // ignore credential changes for credentials that are not used for authentication
                if (shouldIgnoreCredentialChange(event)) {
                    yield null;
                }

                yield generateCredentialChangeEvent(event, stream);
            }
            // Add more event mappings here as needed

            default -> {

                if (shouldIgnoreEvent(event)) {
                    yield null;
                }

                yield generateSecurityEventFromEvent(event, stream);
            }
        };
        // Map Keycloak events to SSF events

        return securityEvent;
    }

    protected boolean shouldIgnoreEvent(Event event) {
        return false;
    }

    protected boolean shouldIgnoreCredentialChange(Event event) {
        return false;
    }

    protected boolean shouldIgnoreLogout(Event event) {
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
