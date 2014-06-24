package org.keycloak.examples.providers.audit;

import org.keycloak.Config;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.audit.Event;
import org.keycloak.audit.EventType;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemAuditProviderFactory implements AuditProviderFactory {

    private List<Event> events;

    private Set<EventType> excludedEvents;

    @Override
    public AuditProvider create(KeycloakSession session) {
        return new MemAuditProvider(events, excludedEvents);
    }

    @Override
    public void init(Config.Scope config) {
        events = Collections.synchronizedList(new LinkedList<Event>());

        String excludes = config.get("excludes");
        if (excludes != null) {
            excludedEvents = new HashSet<EventType>();
            for (String e : excludes.split(",")) {
                excludedEvents.add(EventType.valueOf(e));
            }
        }
    }

    @Override
    public void close() {
        events = null;
        excludedEvents = null;
    }

    @Override
    public String getId() {
        return "in-mem";
    }
}
