package org.keycloak.quarkus.runtime.integration.tls;

import java.util.Collection;
import java.util.Comparator;

import jakarta.enterprise.event.Observes;

import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.truststore.TruststoreProviderFactory;
import org.keycloak.truststore.TruststoreReloadListener;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.tls.CertificateUpdatedEvent;

import static org.keycloak.config.TruststoreOptions.TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY;
import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import static io.quarkus.arc.properties.StringValueMatch.REGEX;

@IfBuildProperty(name = NS_KEYCLOAK_PREFIX + TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY, stringValue = ".*\\S.*", match = REGEX)
class SystemTruststoreReloadObserver {

    void onSystemTruststoreUpdated(@Observes CertificateUpdatedEvent event, QuarkusKeycloakSessionFactory sessionFactory,
                                   SystemTruststoreReload systemTruststoreReload) {
        if (!SystemTruststoreReload.TLS_BUCKET_NAME.equalsIgnoreCase(event.name())) {
            return;
        }
        if (!systemTruststoreReload.reloadIfChanged()) {
            return;
        }
        Collection<TruststoreReloadListener> listeners = sessionFactory
                .getProviderFactoriesStream()
                .<TruststoreReloadListener>mapMulti((providerFactory, consumer) -> {
                    if (providerFactory instanceof TruststoreReloadListener truststoreReloadListener) {
                        consumer.accept(truststoreReloadListener);
                    }
                })
                .sorted(Comparator.comparingInt(listener -> listener instanceof TruststoreProviderFactory ? 0 : 1))
                .toList();
        if (!listeners.isEmpty()) {
            runJobInTransaction(sessionFactory, session -> systemTruststoreReload.notifyConsumers(listeners, session));
        }
    }
}
