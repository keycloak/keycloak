package org.keycloak.events.mongo;

import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoEventStoreProviderFactory implements EventStoreProviderFactory {

    protected static final Logger logger = Logger.getLogger(MongoEventStoreProviderFactory.class);

    public static final String ID = "mongo";

    @Override
    public EventStoreProvider create(KeycloakSession session) {
        MongoConnectionProvider connection = session.getProvider(MongoConnectionProvider.class);

        DBCollection collection = connection.getDB().getCollection("events");
        collection.setWriteConcern(WriteConcern.UNACKNOWLEDGED);

        return new MongoEventStoreProvider(collection);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

}
