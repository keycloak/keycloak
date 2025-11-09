package org.keycloak.protocol.ssf.event.listener;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectUserLookup;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.caep.SessionRevoked;

import java.util.List;

/**
 * Default {@link SsfEventListener} implementation.
 */
public class DefaultSsfEventListener implements SsfEventListener {

    protected static final Logger log = Logger.getLogger(DefaultSsfEventListener.class);

    protected final KeycloakSession session;

    public DefaultSsfEventListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(SsfSecurityEventContext eventContext, String eventId, SsfEvent event) {
        String eventType = event.getEventType();
        SubjectId subjectId = event.getSubjectId();
        var eventClass = event.getClass();
        log.debugf("Security event received. eventId=%s eventType=%s subjectId=%s eventClass=%s", eventId, eventType, subjectId, eventClass.getName());

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        handleSecurityEvent(eventContext, event, realm, subjectId);
    }

    protected void handleSecurityEvent(SsfSecurityEventContext eventContext, SsfEvent ssfEvent, RealmModel realm, SubjectId subjectId) {

        if (ssfEvent instanceof SessionRevoked sessionRevoked) {
            handleSessionRevokedEvent(eventContext, realm, subjectId, sessionRevoked);
        }
    }

    protected void handleSessionRevokedEvent(SsfSecurityEventContext eventContext, RealmModel realm, SubjectId subjectId, SessionRevoked sessionRevoked) {

        // TODO subject is usually refering to a user, but could also be UserSession, an IdentityProvider, Organization etc. so we might need to be more flexible here

        List<UserSessionModel> userSessions = getUserSessions(realm, subjectId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }

        // TODO should this only affect online sessions or also offline sessions?
        UserModel user = userSessions.get(0).getUser();
        for (var userSession : userSessions) {
            session.sessions().removeUserSession(realm, userSession);
        }

        log.debugf("Removed %s sessions for user. realm=%s userId=%s for SessionRevoked event. reasonAdmin=%s reasonUser=%s",
                userSessions.size(), realm.getName(), user.getId(), sessionRevoked.getReasonAdmin(), sessionRevoked.getReasonUser());
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
