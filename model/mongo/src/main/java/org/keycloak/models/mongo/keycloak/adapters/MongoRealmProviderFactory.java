package org.keycloak.models.mongo.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RealmProviderFactory;

/**
 * KeycloakSessionFactory implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoRealmProviderFactory implements RealmProviderFactory {
    protected static final Logger logger = Logger.getLogger(MongoRealmProviderFactory.class);

    @Override
    public String getId() {
        return "mongo";
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public RealmProvider create(KeycloakSession session) {
        MongoConnectionProvider connection = session.getProvider(MongoConnectionProvider.class);
        return new MongoRealmProvider(session, connection.getMongoStore(), connection.getInvocationContext());
    }

    @Override
    public void close() {
    }

}

