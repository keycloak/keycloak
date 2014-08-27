package org.keycloak.account.freemarker.model;

import org.keycloak.events.Event;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
            return event.getType().toString().toLowerCase().replace("_", " ");
        }

        public String getClient() {
            return event.getClientId();
        }

        public String getIpAddress() {
            return event.getIpAddress();
        }

        public List<DetailBean> getDetails() {
            List<DetailBean> details = new LinkedList<DetailBean>();
            if (event.getDetails() != null) {
                for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                    details.add(new DetailBean(e));
                }
            }
            return details;
        }

    }

    public static class DetailBean {

        private Map.Entry<String, String> entry;

        public DetailBean(Map.Entry<String, String> entry) {
            this.entry = entry;
        }

        public String getKey() {
            return entry.getKey();
        }

        public String getValue() {
            return entry.getValue().replace("_", " ");
        }

    }

}
