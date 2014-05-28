package org.keycloak.examples.providers.audit;

import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.Event;
import org.keycloak.audit.EventQuery;
import org.keycloak.audit.EventType;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemAuditProvider implements AuditProvider {
    private final List<Event> events;
    private final Set<EventType> excludedEvents;

    public MemAuditProvider(List<Event> events, Set<EventType> excludedEvents) {
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
        events.add(event);
    }

    @Override
    public void close() {
    }

}
