package org.keycloak.quarkus.runtime.integration.tls;

import jakarta.enterprise.event.Observes;

import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.tls.CertificateUpdatedEvent;

import static org.keycloak.config.TruststoreOptions.TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import static io.quarkus.arc.properties.StringValueMatch.REGEX;

@IfBuildProperty(name = NS_KEYCLOAK_PREFIX + TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY, stringValue = ".*\\S.*", match = REGEX)
class SystemTruststoreReloadObserver {

    void onSystemTruststoreUpdated(@Observes CertificateUpdatedEvent event, QuarkusKeycloakSessionFactory sessionFactory,
                                   SystemTruststoreReload systemTruststoreReload) {
        if (!SystemTruststoreReload.TLS_BUCKET_NAME.equalsIgnoreCase(event.name())) {
            return;
        }
        systemTruststoreReload.notifyConsumers(sessionFactory);
    }
}
