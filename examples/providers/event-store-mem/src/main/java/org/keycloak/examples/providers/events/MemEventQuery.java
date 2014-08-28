package org.keycloak.examples.providers.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventType;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemEventQuery implements EventQuery {

    private List<Event> events;

    private int first;
    private int max;

    public MemEventQuery(List<Event> events) {
        this.events = events;
    }

    @Override
    public EventQuery type(EventType... types) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            Event next = itr.next();
            boolean include = false;
            for (EventType e : types) {
                if (next.getType().equals(e)) {
                    include = true;
                    break;
                }
            }
            if (!include) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery realm(String realmId) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getRealmId().equals(realmId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery client(String clientId) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getClientId().equals(clientId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery user(String userId) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getUserId().equals(userId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery ipAddress(String ipAddress) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getIpAddress().equals(ipAddress)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery firstResult(int result) {
        this.first = result;
        return this;
    }

    @Override
    public EventQuery maxResults(int results) {
        this.max = results;
        return this;
    }

    @Override
    public List<Event> getResultList() {
        if (events.size() < first) {
            return Collections.emptyList();
        }
        int end = first + max <= events.size() ? first + max : events.size();

        return events.subList(first, end);
    }

}
