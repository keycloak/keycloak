package org.keycloak.events.mongo;

import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoEventStoreProviderFactory implements EventStoreProviderFactory {

    protected static final Logger logger = Logger.getLogger(MongoEventStoreProviderFactory.class);

    public static final String ID = "mongo";

    private Set<EventType> includedEvents = new HashSet<EventType>();

    @Override
    public EventStoreProvider create(KeycloakSession session) {
        MongoConnectionProvider connection = session.getProvider(MongoConnectionProvider.class);

        DBCollection collection = connection.getDB().getCollection("events");
        collection.setWriteConcern(WriteConcern.UNACKNOWLEDGED);

        return new MongoEventStoreProvider(collection, includedEvents);
    }

    @Override
    public void init(Config.Scope config) {
        String[] include = config.getArray("include-events");
        if (include != null) {
            for (String i : include) {
                includedEvents.add(EventType.valueOf(i.toUpperCase()));
            }
        } else {
            for (EventType i : EventType.values()) {
                includedEvents.add(i);
            }
        }

        String[] exclude = config.getArray("exclude-events");
        if (exclude != null) {
            for (String e : exclude) {
                includedEvents.remove(EventType.valueOf(e.toUpperCase()));
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

}
