package org.keycloak.quarkus.runtime.integration.tls;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.tls.TrustStoreAndTrustOptions;
import io.quarkus.tls.TrustStoreProvider;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

import static org.keycloak.config.TruststoreOptions.TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import static io.quarkus.arc.properties.StringValueMatch.REGEX;

@IfBuildProperty(name = NS_KEYCLOAK_PREFIX + TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY, stringValue = ".*\\S.*", match = REGEX)
@Identifier(SystemTruststoreReload.TLS_BUCKET_NAME)
@ApplicationScoped
class SystemTruststoreProvider implements TrustStoreProvider {

    private final SystemTruststoreReload systemTruststoreReload;

    SystemTruststoreProvider(SystemTruststoreReload systemTruststoreReload) {
        this.systemTruststoreReload = systemTruststoreReload;
    }

    @Override
    public TrustStoreAndTrustOptions getTrustStore(Vertx vertx) {
        TrustStoreAndTrustOptions systemTruststore = systemTruststoreReload.getSystemTruststore();
        if (systemTruststore == null) {
            throw new IllegalStateException("System truststore does not exist, but trust store reload period was configured");
        }
        return systemTruststore;
    }
}
