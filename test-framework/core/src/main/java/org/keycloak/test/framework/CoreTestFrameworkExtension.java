package org.keycloak.test.framework;

import org.keycloak.test.framework.admin.KeycloakAdminClientSupplier;
import org.keycloak.test.framework.database.DevFileDatabaseSupplier;
import org.keycloak.test.framework.database.DevMemDatabaseSupplier;
import org.keycloak.test.framework.database.TestDatabase;
import org.keycloak.test.framework.events.AdminEventsSupplier;
import org.keycloak.test.framework.events.EventsSupplier;
import org.keycloak.test.framework.events.SysLogServerSupplier;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.oauth.OAuthClientSupplier;
import org.keycloak.test.framework.realm.ClientSupplier;
import org.keycloak.test.framework.realm.RealmSupplier;
import org.keycloak.test.framework.realm.UserSupplier;
import org.keycloak.test.framework.server.DistributionKeycloakTestServerSupplier;
import org.keycloak.test.framework.server.EmbeddedKeycloakTestServerSupplier;
import org.keycloak.test.framework.server.KeycloakTestServer;
import org.keycloak.test.framework.server.RemoteKeycloakTestServerSupplier;

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
                new DistributionKeycloakTestServerSupplier(),
                new EmbeddedKeycloakTestServerSupplier(),
                new RemoteKeycloakTestServerSupplier(),
                new DevMemDatabaseSupplier(),
                new DevFileDatabaseSupplier(),
                new OAuthClientSupplier(),
                new SysLogServerSupplier(),
                new EventsSupplier(),
                new AdminEventsSupplier()
        );
    }

    @Override
    public Map<Class<?>, String> valueTypeAliases() {
        return Map.of(
                KeycloakTestServer.class, "server",
                TestDatabase.class, "database"
        );
    }

}
