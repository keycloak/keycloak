package org.keycloak.events.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventType;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoEventQuery implements EventQuery {

    private Integer firstResult;
    private Integer maxResults;
    private DBCollection audit;
    private final BasicDBObject query;

    public MongoEventQuery(DBCollection audit) {
        this.audit = audit;
        query = new BasicDBObject();
    }

    @Override
    public EventQuery type(EventType... types) {
        List<String> eventStrings = new LinkedList<String>();
        for (EventType e : types) {
            eventStrings.add(e.toString());
        }
        query.put("type", new BasicDBObject("$in", eventStrings));
        return this;
    }

    @Override
    public EventQuery realm(String realmId) {
        query.put("realmId", realmId);
        return this;
    }

    @Override
    public EventQuery client(String clientId) {
        query.put("clientId", clientId);
        return this;
    }

    @Override
    public EventQuery user(String userId) {
        query.put("userId", userId);
        return this;
    }

    @Override
    public EventQuery ipAddress(String ipAddress) {
        query.put("ipAddress", ipAddress);
        return this;
    }

    @Override
    public EventQuery firstResult(int firstResult) {
        this.firstResult = firstResult;
        return this;
    }

    @Override
    public EventQuery maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public List<Event> getResultList() {
        DBCursor cur = audit.find(query).sort(new BasicDBObject("time", -1));
        if (firstResult != null) {
            cur.skip(firstResult);
        }
        if (maxResults != null) {
            cur.limit(maxResults);
        }

        List<Event> events = new LinkedList<Event>();
        while (cur.hasNext()) {
            events.add(MongoEventStoreProvider.convert((BasicDBObject) cur.next()));
        }

        return events;
    }

}
