package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.Config;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.session.UserSessionPersisterProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoUserSessionPersisterProviderFactory implements UserSessionPersisterProviderFactory {

    public static final String ID = "mongo";

    @Override
    public UserSessionPersisterProvider create(KeycloakSession session) {
        MongoConnectionProvider connection = session.getProvider(MongoConnectionProvider.class);
        return new MongoUserSessionPersisterProvider(session, connection.getInvocationContext());
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
