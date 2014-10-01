package org.keycloak.events.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoEventStoreProvider implements EventStoreProvider {

    private DBCollection events;
    private Set<EventType> includedEvents;

    public MongoEventStoreProvider(DBCollection events, Set<EventType> includedEvents) {
        this.events = events;
        this.includedEvents = includedEvents;
    }

    @Override
    public EventQuery createQuery() {
        return new MongoEventQuery(events);
    }

    @Override
    public void clear() {
        events.remove(new BasicDBObject());
    }

    @Override
    public void clear(String realmId) {
        events.remove(new BasicDBObject("realmId", realmId));
    }

    @Override
    public void clear(String realmId, long olderThan) {
        BasicDBObject q = new BasicDBObject();
        q.put("realmId", realmId);
        q.put("time", new BasicDBObject("$lt", olderThan));
        events.remove(q);
    }

    @Override
    public void onEvent(Event event) {
        if (includedEvents.contains(event.getType())) {
            events.insert(convert(event));
        }
    }

    @Override
    public void close() {
    }

    static DBObject convert(Event o) {
        BasicDBObject e = new BasicDBObject();
        e.put("time", o.getTime());
        e.put("type", o.getType().toString());
        e.put("realmId", o.getRealmId());
        e.put("clientId", o.getClientId());
        e.put("userId", o.getUserId());
        e.put("sessionId", o.getSessionId());
        e.put("ipAddress", o.getIpAddress());
        e.put("error", o.getError());

        BasicDBObject details = new BasicDBObject();
        if (o.getDetails() != null) {
            for (Map.Entry<String, String> entry : o.getDetails().entrySet()) {
                details.put(entry.getKey(), entry.getValue());
            }
        }
        e.put("details", details);

        return e;
    }

    static Event convert(BasicDBObject o) {
        Event e = new Event();
        e.setTime(o.getLong("time"));
        e.setType(EventType.valueOf(o.getString("type")));
        e.setRealmId(o.getString("realmId"));
        e.setClientId(o.getString("clientId"));
        e.setUserId(o.getString("userId"));
        e.setSessionId(o.getString("sessionId"));
        e.setIpAddress(o.getString("ipAddress"));
        e.setError(o.getString("error"));

        BasicDBObject d = (BasicDBObject) o.get("details");
        if (d != null) {
            Map<String, String> details = new HashMap<String, String>();
            for (Object k : d.keySet()) {
                details.put((String) k, d.getString((String) k));
            }
            e.setDetails(details);
        }

        return e;
    }

}
