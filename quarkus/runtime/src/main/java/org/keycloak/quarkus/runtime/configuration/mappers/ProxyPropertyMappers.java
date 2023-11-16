package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.Messages;

final class ProxyPropertyMappers {

    private ProxyPropertyMappers(){}

    public static PropertyMapper[] getProxyPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ProxyOptions.PROXY)
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .transformer(ProxyPropertyMappers::isProxyHeadersEnabled)
                        .paramLabel("mode")
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HOST)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::isProxyHeadersEnabled)
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-forwarded")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::isProxyHeadersEnabled)
                        .build(),
                fromOption(ProxyOptions.PROXY_X_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-x-forwarded")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::isProxyHeadersEnabled)
                        .build()
        };
    }

    private static Optional<String> isProxyHeadersEnabled(Optional<String> value, ConfigSourceInterceptorContext context) {
        try {
            return Optional.of(String.valueOf(ProxyOptions.Mode.valueOf(value.get()).isProxyHeadersEnabled()));
        } catch (IllegalArgumentException iae) {
            addInitializationException(Messages.invalidProxyMode(value.get()));
            return Optional.of(Boolean.FALSE.toString());
        }
    }

}
