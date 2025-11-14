package org.keycloak.testframework;

import java.util.List;
import java.util.Map;

import org.keycloak.testframework.admin.AdminClientFactorySupplier;
import org.keycloak.testframework.admin.AdminClientSupplier;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.testframework.crypto.CryptoHelperSupplier;
import org.keycloak.testframework.database.DevFileDatabaseSupplier;
import org.keycloak.testframework.database.DevMemDatabaseSupplier;
import org.keycloak.testframework.database.RemoteDatabaseSupplier;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.events.AdminEventsSupplier;
import org.keycloak.testframework.events.EventsSupplier;
import org.keycloak.testframework.events.SysLogServerSupplier;
import org.keycloak.testframework.http.HttpClientSupplier;
import org.keycloak.testframework.http.HttpServerSupplier;
import org.keycloak.testframework.http.SimpleHttpSupplier;
import org.keycloak.testframework.https.CertificatesSupplier;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.infinispan.InfinispanExternalServerSupplier;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.realm.ClientSupplier;
import org.keycloak.testframework.realm.RealmSupplier;
import org.keycloak.testframework.realm.UserSupplier;
import org.keycloak.testframework.server.DistributionKeycloakServerSupplier;
import org.keycloak.testframework.server.EmbeddedKeycloakServerSupplier;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.server.KeycloakUrlsSupplier;
import org.keycloak.testframework.server.RemoteKeycloakServerSupplier;

public class CoreTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new AdminClientSupplier(),
                new AdminClientFactorySupplier(),
                new ClientSupplier(),
                new RealmSupplier(),
                new UserSupplier(),
                new DistributionKeycloakServerSupplier(),
                new EmbeddedKeycloakServerSupplier(),
                new RemoteKeycloakServerSupplier(),
                new KeycloakUrlsSupplier(),
                new DevMemDatabaseSupplier(),
                new DevFileDatabaseSupplier(),
                new RemoteDatabaseSupplier(),
                new SysLogServerSupplier(),
                new EventsSupplier(),
                new AdminEventsSupplier(),
                new HttpClientSupplier(),
                new HttpServerSupplier(),
                new InfinispanExternalServerSupplier(),
                new SimpleHttpSupplier(),
                new CertificatesSupplier(),
                new CryptoHelperSupplier()
        );
    }

    @Override
    public Map<Class<?>, String> valueTypeAliases() {
        return Map.of(
                KeycloakServer.class, "server",
                TestDatabase.class, "database",
                ManagedCertificates.class, "certificates",
                CryptoHelper.class, "crypto"
        );
    }

    @Override
    public List<Class<?>> alwaysEnabledValueTypes() {
        return List.of(CryptoHelper.class);
    }
}
