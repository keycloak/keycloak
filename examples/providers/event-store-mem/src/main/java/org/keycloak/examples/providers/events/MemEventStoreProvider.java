package org.keycloak.examples.providers.events;

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

    public MemEventStoreProvider(List<Event> events, Set<EventType> excludedEvents) {
        this.events = events;
        this.excludedEvents = excludedEvents;
    }

    @Override
    public EventQuery createQuery() {
        return new MemEventQuery(new LinkedList<Event>(events));
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
        events.add(0, event);
    }

    @Override
    public void close() {
    }

}
