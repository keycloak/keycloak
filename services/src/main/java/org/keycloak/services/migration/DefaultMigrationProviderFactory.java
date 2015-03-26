package org.keycloak.services.migration;

import org.keycloak.Config;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.MigrationProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultMigrationProviderFactory implements MigrationProviderFactory {

    @Override
    public MigrationProvider create(KeycloakSession session) {
        return new DefaultMigrationProvider(session);
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
        return "default";
    }
}
