package org.keycloak.audit.mongo;

import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.audit.EventType;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoAuditProviderFactory implements AuditProviderFactory {

    protected static final Logger logger = Logger.getLogger(MongoAuditProviderFactory.class);

    public static final String ID = "mongo";

    private Set<EventType> includedEvents = new HashSet<EventType>();

    @Override
    public AuditProvider create(KeycloakSession session) {
        MongoConnectionProvider connection = session.getProvider(MongoConnectionProvider.class);

        DBCollection collection = connection.getDB().getCollection("audit");
        collection.setWriteConcern(WriteConcern.UNACKNOWLEDGED);

        return new MongoAuditProvider(collection, includedEvents);
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
