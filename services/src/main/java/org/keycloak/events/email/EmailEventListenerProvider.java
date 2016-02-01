package org.keycloak.events.email;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.logging.KeycloakLogger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailEventListenerProvider implements EventListenerProvider {

    private static final KeycloakLogger logger = Logger.getMessageLogger(KeycloakLogger.class, EmailEventListenerProvider.class.getName());

    private KeycloakSession session;
    private RealmProvider model;
    private EmailTemplateProvider emailTemplateProvider;
    private Set<EventType> includedEvents;

    public EmailEventListenerProvider(KeycloakSession session, EmailTemplateProvider emailTemplateProvider, Set<EventType> includedEvents) {
        this.session = session;
        this.model = session.realms();
        this.emailTemplateProvider = emailTemplateProvider;
        this.includedEvents = includedEvents;
    }

    @Override
    public void onEvent(Event event) {
        if (includedEvents.contains(event.getType())) {
            if (event.getRealmId() != null && event.getUserId() != null) {
                RealmModel realm = model.getRealm(event.getRealmId());
                UserModel user = session.users().getUserById(event.getUserId(), realm);
                if (user != null && user.getEmail() != null && user.isEmailVerified()) {
                    try {
                        emailTemplateProvider.setRealm(realm).setUser(user).sendEvent(event);
                    } catch (EmailException e) {
                        logger.EVENT.failedToSendTypeMail(e);
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {

    }

    @Override
    public void close() {
    }

}
