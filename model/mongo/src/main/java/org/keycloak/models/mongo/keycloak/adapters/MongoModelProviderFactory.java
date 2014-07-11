package org.keycloak.models.mongo.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.ModelProviderFactory;

/**
 * KeycloakSessionFactory implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoModelProviderFactory implements ModelProviderFactory {
    protected static final Logger logger = Logger.getLogger(MongoModelProviderFactory.class);

    @Override
    public String getId() {
        return "mongo";
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public ModelProvider create(KeycloakSession session) {
        MongoConnectionProvider connection = session.getProvider(MongoConnectionProvider.class);
        return new MongoModelProvider(session, connection.getMongoStore(), connection.getInvocationContext());
    }

    @Override
    public void close() {
    }

}

