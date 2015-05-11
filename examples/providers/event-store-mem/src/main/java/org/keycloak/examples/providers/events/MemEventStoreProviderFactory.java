package org.keycloak.examples.providers.events;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

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
    private List<AdminEvent> adminEvents;
    private Set<OperationType> excludedOperations;

    @Override
    public EventStoreProvider create(KeycloakSession session) {
        return new MemEventStoreProvider(events, excludedEvents, adminEvents, excludedOperations);
    }

    @Override
    public void init(Config.Scope config) {
        events = Collections.synchronizedList(new LinkedList<Event>());

        String excludes = config.get("excludes");
        if (excludes != null) {
            excludedEvents = new HashSet<>();
            for (String e : excludes.split(",")) {
                excludedEvents.add(EventType.valueOf(e));
            }
        }
        
        String excludesOperations = config.get("excludesOperations");
        if (excludesOperations != null) {
            excludedOperations = new HashSet<>();
            for (String e : excludesOperations.split(",")) {
                excludedOperations.add(OperationType.valueOf(e));
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }
    @Override
    public void close() {
        events = null;
        excludedEvents = null;
        adminEvents = null;
        excludedOperations = null;
    }

    @Override
    public String getId() {
        return "in-mem";
    }
}
