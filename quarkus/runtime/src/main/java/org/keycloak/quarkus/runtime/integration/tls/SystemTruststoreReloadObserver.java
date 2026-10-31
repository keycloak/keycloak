package org.keycloak.quarkus.runtime.integration.tls;

import java.util.List;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;

import org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.ManagementPropertyMappers;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import org.jboss.logging.Logger;

import static org.keycloak.config.TruststoreOptions.TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import static io.quarkus.arc.properties.StringValueMatch.REGEX;

@IfBuildProperty(name = NS_KEYCLOAK_PREFIX + TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY, stringValue = ".*\\S.*", match = REGEX)
class SystemTruststoreReloadObserver {

    private static final Logger LOGGER = Logger.getLogger(SystemTruststoreReloadObserver.class);

    private static final List<String> SERVER_TLS_CONFIGURATIONS = List.of(HttpPropertyMappers.TLS_BUCKET,
            ManagementPropertyMappers.MGMT_TLS_BUCKET);

    void onSystemTruststoreUpdated(@Observes CertificateUpdatedEvent event, QuarkusKeycloakSessionFactory sessionFactory,
                                   SystemTruststoreReload systemTruststoreReload, TlsConfigurationRegistry tlsConfigurationRegistry,
                                   Event<CertificateUpdatedEvent> certificateUpdatedEvent) {
        if (!SystemTruststoreReload.TLS_BUCKET_NAME.equalsIgnoreCase(event.name())) {
            return;
        }
        if (systemTruststoreReload.notifyConsumers(sessionFactory)) {
            refreshServersTrustingSystemTruststore(tlsConfigurationRegistry, certificateUpdatedEvent);
        }
    }

    private static void refreshServersTrustingSystemTruststore(TlsConfigurationRegistry tlsConfigurationRegistry,
                                                              Event<CertificateUpdatedEvent> certificateUpdatedEvent) {
        for (String name : SERVER_TLS_CONFIGURATIONS) {
            TlsConfiguration tlsConfiguration = tlsConfigurationRegistry.get(name).orElse(null);
            if (tlsConfiguration == null
                    || !(tlsConfiguration.getTrustStoreOptions() instanceof SystemTruststoreReload.SystemTrustOptions)) {
                continue;
            }
            // reloading picks up the reloaded system truststore and the event makes Quarkus update the server with it
            if (tlsConfiguration.reload()) {
                LOGGER.debugf("Refreshing the TLS configuration '%s' after the system truststore was reloaded", name);
                certificateUpdatedEvent.fire(new CertificateUpdatedEvent(name, tlsConfiguration));
            } else {
                LOGGER.warnf("Failed to reload the TLS configuration '%s' after the system truststore was reloaded; "
                        + "it keeps the previously loaded system truststore", name);
            }
        }
    }
}
