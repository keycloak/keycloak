package org.keycloak.examples.providers.events;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemEventStoreProvider implements EventStoreProvider {
    private final List<Event> events;
    private final Set<EventType> excludedEvents;
    private final List<AdminEvent> adminEvents;
    private final Set<OperationType> excludedOperations;

    public MemEventStoreProvider(List<Event> events, Set<EventType> excludedEvents, 
            List<AdminEvent> adminEvents, Set<OperationType> excludedOperations) {
        this.events = events;
        this.excludedEvents = excludedEvents;
        
        this.adminEvents = adminEvents;
        this.excludedOperations = excludedOperations;
    }

    @Override
    public EventQuery createQuery() {
        return new MemEventQuery(new LinkedList<>(events));
    }

    @Override
    public void clear() {

    }

    @Override
    public void clear(String realmId) {
        synchronized(events) {
            Iterator<Event> itr = events.iterator();
            while (itr.hasNext()) {
                if (itr.next().getRealmId().equals(realmId)) {
                    itr.remove();
                }
            }
        }
    }

    @Override
    public void clear(String realmId, long olderThan) {
        synchronized(events) {
            Iterator<Event> itr = events.iterator();
            while (itr.hasNext()) {
                Event e = itr.next();
                if (e.getRealmId().equals(realmId) && e.getTime() < olderThan) {
                    itr.remove();
                }
            }
        }
    }

    @Override
    public void onEvent(Event event) {
        if (excludedEvents == null || !excludedEvents.contains(event.getType())) {
            events.add(0, event);
        }
    }

    @Override
    public AdminEventQuery createAdminQuery() {
        return new MemAdminEventQuery(new LinkedList<>(adminEvents));
    }

    @Override
    public void clearAdmin() {

    }

    @Override
    public void clearAdmin(String realmId) {
        synchronized(adminEvents) {
            Iterator<AdminEvent> itr = adminEvents.iterator();
            while (itr.hasNext()) {
                if (itr.next().getAuthDetails().getRealmId().equals(realmId)) {
                    itr.remove();
                }
            }
        }
    }

    @Override
    public void clearAdmin(String realmId, long olderThan) {
        synchronized(adminEvents) {
            Iterator<AdminEvent> itr = adminEvents.iterator();
            while (itr.hasNext()) {
                AdminEvent e = itr.next();
                if (e.getAuthDetails().getRealmId().equals(realmId) && e.getTime() < olderThan) {
                    itr.remove();
                }
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        if (excludedOperations == null || !excludedOperations.contains(adminEvent.getOperationType())) {
            adminEvents.add(0, adminEvent);
        }
    }

    @Override
    public void close() {
    }

}
