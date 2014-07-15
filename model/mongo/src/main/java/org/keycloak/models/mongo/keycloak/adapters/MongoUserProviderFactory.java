package org.keycloak.models.mongo.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserProviderFactory;

/**
 * KeycloakSessionFactory implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoUserProviderFactory implements UserProviderFactory {
    protected static final Logger logger = Logger.getLogger(MongoUserProviderFactory.class);

    @Override
    public String getId() {
        return "mongo";
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public UserProvider create(KeycloakSession session) {
        MongoConnectionProvider connection = session.getProvider(MongoConnectionProvider.class);
        return new MongoUserProvider(session, connection.getMongoStore(), connection.getInvocationContext());
    }

    @Override
    public void close() {
    }

}

