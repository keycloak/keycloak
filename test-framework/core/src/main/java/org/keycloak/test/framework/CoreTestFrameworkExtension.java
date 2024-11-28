package org.keycloak.test.framework;

import org.keycloak.test.framework.admin.KeycloakAdminClientSupplier;
import org.keycloak.test.framework.database.DevFileDatabaseSupplier;
import org.keycloak.test.framework.database.DevMemDatabaseSupplier;
import org.keycloak.test.framework.database.TestDatabase;
import org.keycloak.test.framework.events.AdminEventsSupplier;
import org.keycloak.test.framework.events.EventsSupplier;
import org.keycloak.test.framework.events.SysLogServerSupplier;
import org.keycloak.test.framework.http.HttpClientSupplier;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.realm.ClientSupplier;
import org.keycloak.test.framework.realm.RealmSupplier;
import org.keycloak.test.framework.realm.UserSupplier;
import org.keycloak.test.framework.server.DistributionKeycloakServerSupplier;
import org.keycloak.test.framework.server.EmbeddedKeycloakServerSupplier;
import org.keycloak.test.framework.server.KeycloakServer;
import org.keycloak.test.framework.server.KeycloakUrlsSupplier;
import org.keycloak.test.framework.server.RemoteKeycloakServerSupplier;

import java.util.List;
import java.util.Map;

public class CoreTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new KeycloakAdminClientSupplier(),
                new ClientSupplier(),
                new RealmSupplier(),
                new UserSupplier(),
                new DistributionKeycloakServerSupplier(),
                new EmbeddedKeycloakServerSupplier(),
                new RemoteKeycloakServerSupplier(),
                new KeycloakUrlsSupplier(),
                new DevMemDatabaseSupplier(),
                new DevFileDatabaseSupplier(),
                new SysLogServerSupplier(),
                new EventsSupplier(),
                new AdminEventsSupplier(),
                new HttpClientSupplier()
        );
    }

    @Override
    public Map<Class<?>, String> valueTypeAliases() {
        return Map.of(
                KeycloakServer.class, "server",
                TestDatabase.class, "database"
        );
    }

}
