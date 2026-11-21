package org.keycloak.quarkus.runtime.services;

import java.security.Security;

import jakarta.inject.Singleton;

import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers;

import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.vertx.core.http.HttpServerOptions;
import org.jboss.logging.Logger;

// Customizes the HTTPS TLS engine and SNI handling.
@Singleton
public class KeycloakHttpServerOptionsCustomizer implements HttpServerOptionsCustomizer {
    private static final Logger LOG = Logger.getLogger(KeycloakHttpServerOptionsCustomizer.class);

    @Override
    public void customizeHttpsServer(HttpServerOptions options) {
        if (Security.getProvider("BC") != null && Security.getProvider("BCFIPS") == null) {
            options.setSslEngineOptions(new BouncyCastleTlsEngineOptions());
            // Let BC select certificates from the full keystore, including multiple
            // identities for one hostname. Its key manager also handles SNI.
            options.setSni(false);
            LOG.info("HTTPS TLS provider: BCJSSE");
            return;
        }
        if (isSniEnabled()) {
            options.setSni(true);
        }
    }

    private static boolean isSniEnabled() {
        return HttpPropertyMappers.isHttpsEnabled()
                && Configuration.getConfigValue(ProxyOptions.PROXY_HEADERS).getValue() == null;
    }
}
