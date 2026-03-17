package org.keycloak.protocol.ssf.receiver.event.listener;

import java.io.IOException;
import java.util.List;

import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ssf.event.SsfEvent;
import org.keycloak.protocol.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectUserLookup;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.receiver.event.processor.SsfEventContext;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

/**
 * Default {@link SsfReceiverEventListener} implementation.
 */
public class DefaultSsfReceiverEventListener implements SsfReceiverEventListener {

    protected static final Logger LOG = Logger.getLogger(DefaultSsfReceiverEventListener.class);

    protected final KeycloakSession session;

    public DefaultSsfReceiverEventListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(SsfEventContext eventContext, String eventId, SsfEvent event) {
        String eventType = event.getEventType();
        SubjectId subjectId = event.getSubjectId();
        var eventClass = event.getClass();
        LOG.debugf("SSF event received. eventId=%s eventType=%s subjectId=%s eventClass=%s", eventId, eventType, subjectId, eventClass.getName());

        handleSsfEvent(eventContext, event, subjectId);
    }

    protected void handleSsfEvent(SsfEventContext eventContext, SsfEvent ssfEvent, SubjectId subjectId) {

        if (ssfEvent instanceof CaepSessionRevoked sessionRevoked) {
            handleSessionRevokedEvent(eventContext, subjectId, sessionRevoked);
        }
    }

    protected void handleSessionRevokedEvent(SsfEventContext eventContext, SubjectId subjectId, CaepSessionRevoked ssfEvent) {

        RealmModel realm = eventContext.getRealm();

        // TODO subject is usually refering to a user, but could also be UserSession, all users of an IdentityProvider or Organization etc. so we might need to be more flexible here
        List<UserSessionModel> userSessions = getUserSessions(realm, subjectId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }

        // TODO should this only affect online sessions or also offline sessions?
        EventBuilder eventBuilder = new EventBuilder(realm, session);
        UserModel user = userSessions.get(0).getUser();
        for (var userSession : userSessions) {

            if (!shouldRemoveUserSession(userSession, eventContext)) {
                continue;
            }

            removeUserSession(userSession, eventContext);

            if (isUserEventRecordingEnabled(realm, EventType.USER_SESSION_DELETED)) {
                fireUserEvent(eventContext, ssfEvent, userSession, eventBuilder, user);
            }
        }

        LOG.debugf("Removed %s sessions for user. realm=%s userId=%s for SessionRevoked event. reasonAdmin=%s reasonUser=%s",
                userSessions.size(), realm.getName(), user.getId(), ssfEvent.getReasonAdmin(), ssfEvent.getReasonUser());
    }

    protected void removeUserSession(UserSessionModel userSession, SsfEventContext eventContext) {
        session.sessions().removeUserSession(eventContext.getRealm(), userSession);
    }

    protected boolean shouldRemoveUserSession(UserSessionModel userSession, SsfEventContext eventContext) {
        return true;
    }

    protected void fireUserEvent(SsfEventContext eventContext, SsfEvent ssfEvent, UserSessionModel userSession, EventBuilder eventBuilder, UserModel user) {

        SsfSecurityEventToken securityEventToken = eventContext.getSecurityEventToken();
        String rawSubject = extractRawSubjectAsString(securityEventToken);
        String rawSecurityEvent = extractSecurityEventAsString(ssfEvent);
        eventBuilder.event(EventType.USER_SESSION_DELETED)
                .user(user)
                .session(userSession.getId())
                .detail(Details.REASON, "user_session_revoked")
                .detail("ssf_set_jti", securityEventToken.getJti())
                .detail("ssf_set_txn", securityEventToken.getTxn())
                .detail("ssf_set_event_type", ssfEvent.getEventType())
                .detail("ssf_set_issuer", securityEventToken.getIss())
                .detail("ssf_set_event", rawSecurityEvent)
                .detail("ssf_set_sub_id", rawSubject)
                .detail("ssf_receiver_alias", eventContext.getReceiver().getConfig().getAlias())
                .success();
    }

    protected String extractSecurityEventAsString(SsfEvent ssfEvent) {
        String rawSecurityEvent;
        try {
            rawSecurityEvent = JsonSerialization.writeValueAsString(ssfEvent);
        } catch (IOException e) {
            LOG.warn("Failed to serialize SecurityEventToken", e);
            rawSecurityEvent = "Failed to serialize SecurityEventToken";
        }
        return rawSecurityEvent;
    }

    protected String extractRawSubjectAsString(SsfSecurityEventToken securityEventToken) {
        String rawSubject;
        try {
            rawSubject = JsonSerialization.writeValueAsString(securityEventToken.getSubjectId());
        } catch (IOException e) {
            LOG.warn("Failed to serialize SubjectId", e);
            rawSubject = "Failed to serialize SubjectId";
        }
        return rawSubject;
    }

    protected boolean isUserEventRecordingEnabled(RealmModel realm, EventType eventType) {
        return realm.isEventsEnabled() && realm.getEnabledEventTypesStream().anyMatch(type -> eventType.name().equals(type));
    }

    /**
     * Should return the list of user sessions for the user identified via the {@link SubjectId}.
     *
     * @param realm
     * @param subjectId
     * @return
     */
    protected List<UserSessionModel> getUserSessions(RealmModel realm, SubjectId subjectId) {
        UserModel user = resolveUser(realm, subjectId);
        if (user == null) {
            return null;
        }
        return session.sessions().getUserSessionsStream(realm, user).toList();
    }

    /**
     * Resolve {@UserModel} from {@link SubjectId}.
     *
     * @param realm
     * @param subjectId
     * @return
     */
    protected UserModel resolveUser(RealmModel realm, SubjectId subjectId) {
        return SubjectUserLookup.lookupUser(session, realm, subjectId);
    }
}
