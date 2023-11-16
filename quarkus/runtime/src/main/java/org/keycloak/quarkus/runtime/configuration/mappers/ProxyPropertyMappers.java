package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.Messages;

public final class ProxyPropertyMappers {

    private ProxyPropertyMappers(){}

    public static PropertyMapper[] getProxyPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ProxyOptions.PROXY)
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .transformer(ProxyPropertyMappers::getValidProxyModeValue)
                        .paramLabel("mode")
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HOST)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::getValidProxyModeValue)
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-forwarded")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::getValidProxyModeValue)
                        .build(),
                fromOption(ProxyOptions.PROXY_X_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-x-forwarded")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::getValidProxyModeValue)
                        .build()
        };
    }

    public static boolean getValidProxyModeValue(String mode) {
        try {
            switch (ProxyOptions.Mode.valueOf(mode)) {
                case none:
                case passthrough:
                    return false;
                default:
                    return true;
            }
        } catch (IllegalArgumentException e) {
            addInitializationException(Messages.invalidProxyMode(mode));
            return false;
        }
    }

    private static Optional<String> getValidProxyModeValue(Optional<String> value, ConfigSourceInterceptorContext context) {
        return Optional.of(String.valueOf(getValidProxyModeValue(value.get())));
    }

}
