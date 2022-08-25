package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import java.util.Optional;

import static java.util.Optional.of;
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
                        .transformer(ProxyPropertyMappers::getValidProxyModeValue)
                        .paramLabel("mode")
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HOST)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::getResolveEnableForwardedHost)
                        .build()
        };
    }

    private static Optional<String> getValidProxyModeValue(Optional<String> value, ConfigSourceInterceptorContext context) {
        String mode = value.get();

        switch (mode) {
            case "none":
                return of(Boolean.FALSE.toString());
            case "edge":
            case "reencrypt":
            case "passthrough":
                return of(Boolean.TRUE.toString());
            default:
                addInitializationException(Messages.invalidProxyMode(mode));
                return of(Boolean.FALSE.toString());
        }
    }

    private static Optional<String> getResolveEnableForwardedHost(Optional<String> proxy, ConfigSourceInterceptorContext context) {
        return of(String.valueOf(!ProxyOptions.Mode.none.name().equals(proxy)));
    }

}
