package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventsManager {

    private Logger log = Logger.getLogger(EventsManager.class);

    private RealmModel realm;
    private KeycloakSession session;
    private ClientConnection clientConnection;

    public EventsManager(RealmModel realm, KeycloakSession session, ClientConnection clientConnection) {
        this.realm = realm;
        this.session = session;
        this.clientConnection = clientConnection;
    }

    public EventBuilder createEventBuilder() {
        List<EventListenerProvider> listeners = new LinkedList<EventListenerProvider>();

        if (realm.isEventsEnabled()) {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            if (eventStore != null) {
                listeners.add(eventStore);
            } else {
                log.error("Events enabled, but no event store provider configured");
            }
        }

        if (realm.getEventsListeners() != null) {
            for (String id : realm.getEventsListeners()) {
                EventListenerProvider listener = session.getProvider(EventListenerProvider.class, id);
                if (listener != null) {
                    listeners.add(listener);
                } else {
                    log.error("Event listener '" + id + "' registered, but provider not found");
                }
            }
        }

        return new EventBuilder(listeners, realm, clientConnection.getRemoteAddr());
    }

}
