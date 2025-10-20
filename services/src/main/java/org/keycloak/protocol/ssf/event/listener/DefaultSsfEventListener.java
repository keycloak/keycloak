package org.keycloak.protocol.ssf.event.listener;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ssf.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectUserLookup;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.caep.SessionRevoked;

import java.util.List;

public class DefaultSsfEventListener implements SsfEventListener {

    protected static final Logger log = Logger.getLogger(DefaultSsfEventListener.class);

    protected final KeycloakSession session;

    public DefaultSsfEventListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(SsfEventContext eventContext, String eventId, SsfEvent event) {
        String eventType = event.getEventType();
        SubjectId subjectId = event.getSubjectId();
        var eventClass = event.getClass();
        log.infof("Security event received. eventId=%s eventType=%s subjectId=%s eventClass=%s", eventId, eventType, subjectId, eventClass.getName());

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        UserModel user = lookupUser(realm, subjectId);
        handleSecurityEvent(event, realm, subjectId, user);
    }

    protected UserModel lookupUser(RealmModel realm, SubjectId subjectId) {
        return SubjectUserLookup.lookupUser(session, realm, subjectId);
    }

    protected void handleSecurityEvent(SsfEvent ssfEvent, RealmModel realm, SubjectId subjectId, UserModel user) {

        if (user == null) {
            return;
        }

        if (ssfEvent instanceof SessionRevoked) {
            List<UserSessionModel> sessions = session.sessions().getUserSessionsStream(realm, user).toList();
            if (!sessions.isEmpty()) {
                for (var userSession : sessions) {
                    session.sessions().removeUserSession(realm, userSession);
                }
                log.debugf("Removed %s sessions for user. realm=%s userId=%s", sessions.size(), realm.getName(), user.getId());
            }
        }
    }

}
