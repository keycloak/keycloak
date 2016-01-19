package org.keycloak.events.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaEventStoreProviderFactory implements EventStoreProviderFactory {

    public static final String ID = "jpa";

    @Override
    public EventStoreProvider create(KeycloakSession session) {
        JpaConnectionProvider connection = session.getProvider(JpaConnectionProvider.class);
        return new JpaEventStoreProvider(connection.getEntityManager());
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
