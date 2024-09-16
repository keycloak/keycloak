package org.keycloak.test.framework.server;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.test.framework.injection.SupplierHelpers;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(List<String> rawOptions, Set<Class<? extends ProviderModule>> providerModules) {
        var builder = Keycloak.builder().setVersion(Version.VERSION);
        for(var it : providerModules) {
            var providerModule = SupplierHelpers.getInstance(it);
            builder.addDependency(providerModule.groupId(), providerModule.artifactId(), providerModule.version());
        }

        keycloak = builder.start(rawOptions);
    }

    @Override
    public void stop() {
        try {
            keycloak.stop();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
