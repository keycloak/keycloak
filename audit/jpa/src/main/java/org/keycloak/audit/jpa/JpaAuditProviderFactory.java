package org.keycloak.audit.jpa;

import org.keycloak.Config;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.audit.EventType;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaAuditProviderFactory implements AuditProviderFactory {

    public static final String ID = "jpa";

    private Set<EventType> includedEvents = new HashSet<EventType>();

    @Override
    public AuditProvider create(KeycloakSession session) {
        JpaConnectionProvider connection = session.getProvider(JpaConnectionProvider.class);
        return new JpaAuditProvider(connection.getEntityManager(), includedEvents);
    }

    @Override
    public void init(Config.Scope config) {
        String[] include = config.getArray("include-events");
        if (include != null) {
            for (String i : include) {
                includedEvents.add(EventType.valueOf(i.toUpperCase()));
            }
        } else {
            for (EventType i : EventType.values()) {
                includedEvents.add(i);
            }
        }

        String[] exclude = config.getArray("exclude-events");
        if (exclude != null) {
            for (String e : exclude) {
                includedEvents.remove(EventType.valueOf(e.toUpperCase()));
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

}
