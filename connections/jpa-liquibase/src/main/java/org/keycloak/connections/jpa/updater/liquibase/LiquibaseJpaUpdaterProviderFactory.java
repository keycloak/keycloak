package org.keycloak.connections.jpa.updater.liquibase;

import org.keycloak.Config;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.JpaUpdaterProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LiquibaseJpaUpdaterProviderFactory implements JpaUpdaterProviderFactory {

    @Override
    public JpaUpdaterProvider create(KeycloakSession session) {
        return new LiquibaseJpaUpdaterProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "liquibase";
    }

}
