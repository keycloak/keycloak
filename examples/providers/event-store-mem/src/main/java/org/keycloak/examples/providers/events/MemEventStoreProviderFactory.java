package org.keycloak.examples.providers.events;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemEventStoreProviderFactory implements EventStoreProviderFactory {

    private List<Event> events;

    private Set<EventType> excludedEvents;

    @Override
    public EventStoreProvider create(KeycloakSession session) {
        return new MemEventStoreProvider(events, excludedEvents);
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
