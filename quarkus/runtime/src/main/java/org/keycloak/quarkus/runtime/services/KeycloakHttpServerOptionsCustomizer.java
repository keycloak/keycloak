package org.keycloak.quarkus.runtime.services;

import jakarta.inject.Singleton;

import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers;

import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.vertx.core.http.HttpServerOptions;

// Workaround for https://github.com/quarkusio/quarkus/issues/55943
// Once fixed, remove this class and use the key-store.sni config property via HttpPropertyMappers instead.
@Singleton
public class KeycloakHttpServerOptionsCustomizer implements HttpServerOptionsCustomizer {

    @Override
    public void customizeHttpsServer(HttpServerOptions options) {
        if (isSniEnabled()) {
            options.setSni(true);
        }
    }

    private static boolean isSniEnabled() {
        return HttpPropertyMappers.isHttpsEnabled()
                && Configuration.getConfigValue(ProxyOptions.PROXY_HEADERS).getValue() == null;
    }
}
