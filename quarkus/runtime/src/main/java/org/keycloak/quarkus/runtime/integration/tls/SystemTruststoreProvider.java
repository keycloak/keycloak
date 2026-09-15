package org.keycloak.quarkus.runtime.integration.tls;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.TrustManagerFactory;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.tls.TrustStoreAndTrustOptions;
import io.quarkus.tls.TrustStoreProvider;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

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
        KeyStore ks = systemTruststoreReload.getSystemTruststore();
        if (ks == null) {
            throw new IllegalStateException("System truststore does not exist, but trust store reload period was configured");
        }
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            return new TrustStoreAndTrustOptions(ks, TrustOptions.wrap(tmf));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
