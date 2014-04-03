package org.keycloak.account.freemarker.model;

import org.keycloak.audit.Event;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LogBean {

    private List<EventBean> events;

    public LogBean(List<Event> events) {
        this.events = new LinkedList<EventBean>();
        for (Event e : events) {
            this.events.add(new EventBean(e));
        }
    }

    public List<EventBean> getEvents() {
        return events;
    }

    public static class EventBean {

        private Event event;

        public EventBean(Event event) {
            this.event = event;
        }

        public Date getDate() {
            return new Date(event.getTime());
        }

        public String getEvent() {
            return event.getEvent().replace('_', ' ');
        }

        public String getClient() {
            return event.getClientId();
        }

        public String getIpAddress() {
            return event.getIpAddress();
        }

    }

}
