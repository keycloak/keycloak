package org.keycloak.audit;

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
public class Audit {

    private static final Logger log = Logger.getLogger(Audit.class);

    private List<AuditListener> listeners;
    private Event event;

    public Audit(List<AuditListener> listeners, RealmModel realm, String ipAddress) {
        this.listeners = listeners;
        this.event = new Event();

        realm(realm);
        ipAddress(ipAddress);
    }

    Audit() {
    }

    public Audit realm(RealmModel realm) {
        event.setRealmId(realm.getId());
        return this;
    }

    public Audit realm(String realmId) {
        event.setRealmId(realmId);
        return this;
    }

    public Audit client(ClientModel client) {
        event.setClientId(client.getClientId());
        return this;
    }

    public Audit client(String clientId) {
        event.setClientId(clientId);
        return this;
    }

    public Audit user(UserModel user) {
        event.setUserId(user.getId());
        return this;
    }

    public Audit user(String userId) {
        event.setUserId(userId);
        return this;
    }

    public Audit session(UserSessionModel session) {
        event.setSessionId(session.getId());
        return this;
    }

    public Audit session(String sessionId) {
        event.setSessionId(sessionId);
        return this;
    }

    public Audit ipAddress(String ipAddress) {
        event.setIpAddress(ipAddress);
        return this;
    }

    public Audit event(EventType e) {
        event.setEvent(e);
        return this;
    }

    public Audit detail(String key, String value) {
        if (value == null || value.equals("")) {
            return this;
        }

        if (event.getDetails() == null) {
            event.setDetails(new HashMap<String, String>());
        }
        event.getDetails().put(key, value);
        return this;
    }

    public Audit removeDetail(String key) {
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
        event.setEvent(EventType.valueOf(event.getEvent().name() + "_ERROR"));
        event.setError(error);
        send();
    }

    public Audit clone() {
        Audit clone = new Audit();
        clone.listeners = listeners;
        clone.event = event.clone();
        return clone;
    }

    public Audit reset() {
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
            for (AuditListener l : listeners) {
                try {
                    l.onEvent(event);
                } catch (Throwable t) {
                    log.error("Failed to send event to " + l, t);
                }
            }
        }
    }

}
