package org.keycloak.events;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventBuilder {

    private static final Logger log = Logger.getLogger(EventBuilder.class);

    private List<EventListenerProvider> listeners;
    private Event event;

    public EventBuilder(List<EventListenerProvider> listeners, RealmModel realm, String ipAddress) {
        this.listeners = listeners;
        this.event = new Event();

        realm(realm);
        ipAddress(ipAddress);
    }

    EventBuilder() {
    }

    public EventBuilder realm(RealmModel realm) {
        event.setRealmId(realm.getId());
        return this;
    }

    public EventBuilder realm(String realmId) {
        event.setRealmId(realmId);
        return this;
    }

    public EventBuilder client(ClientModel client) {
        event.setClientId(client.getClientId());
        return this;
    }

    public EventBuilder client(String clientId) {
        event.setClientId(clientId);
        return this;
    }

    public EventBuilder user(UserModel user) {
        event.setUserId(user.getId());
        return this;
    }

    public EventBuilder user(String userId) {
        event.setUserId(userId);
        return this;
    }

    public EventBuilder session(UserSessionModel session) {
        event.setSessionId(session.getId());
        return this;
    }

    public EventBuilder session(String sessionId) {
        event.setSessionId(sessionId);
        return this;
    }

    public EventBuilder ipAddress(String ipAddress) {
        event.setIpAddress(ipAddress);
        return this;
    }

    public EventBuilder event(EventType e) {
        event.setType(e);
        return this;
    }

    public EventBuilder detail(String key, String value) {
        if (value == null || value.equals("")) {
            return this;
        }

        if (event.getDetails() == null) {
            event.setDetails(new HashMap<String, String>());
        }
        event.getDetails().put(key, value);
        return this;
    }

    public EventBuilder removeDetail(String key) {
        if (event.getDetails() != null) {
            event.getDetails().remove(key);
        }
        return this;
    }

    public Event getEvent() {
        return event;
    }

    public void success() {
        send();
    }

    public void error(String error) {
        event.setType(EventType.valueOf(event.getType().name() + "_ERROR"));
        event.setError(error);
        send();
    }

    public EventBuilder clone() {
        EventBuilder clone = new EventBuilder();
        clone.listeners = listeners;
        clone.event = event.clone();
        return clone;
    }

    public EventBuilder reset() {
        Event old = event;

        event = new Event();
        event.setRealmId(old.getRealmId());
        event.setIpAddress(old.getIpAddress());
        event.setClientId(old.getClientId());
        event.setUserId(old.getUserId());

        return this;
    }

    private void send() {
        event.setTime(System.currentTimeMillis());

        if (listeners != null) {
            for (EventListenerProvider l : listeners) {
                try {
                    l.onEvent(event);
                } catch (Throwable t) {
                    log.error("Failed to send type to " + l, t);
                }
            }
        }
    }

}
