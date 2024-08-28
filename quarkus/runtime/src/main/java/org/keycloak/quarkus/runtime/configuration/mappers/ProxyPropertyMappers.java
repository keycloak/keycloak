package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.config.ProxyOptions;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ProxyPropertyMappers {

    private ProxyPropertyMappers(){}

    public static PropertyMapper<?>[] getProxyPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ProxyOptions.PROXY_HEADERS)
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .transformer((v, c) -> proxyEnabled(null, v, c))
                        .paramLabel("headers")
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HOST)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom("proxy-headers")
                        .transformer((v, c) -> proxyEnabled(null, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-forwarded")
                        .mapFrom("proxy-headers")
                        .transformer((v, c) -> proxyEnabled(ProxyOptions.Headers.forwarded, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_X_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-x-forwarded")
                        .mapFrom("proxy-headers")
                        .transformer((v, c) -> proxyEnabled(ProxyOptions.Headers.xforwarded, v, c))
                        .build()
        };
    }

    private static Optional<String> proxyEnabled(ProxyOptions.Headers testHeader, Optional<String> value, ConfigSourceInterceptorContext context) {
        boolean enabled = false;

        if (value.isPresent()) { // proxy-headers explicitly configured
            if (testHeader != null) {
                enabled = ProxyOptions.Headers.valueOf(value.get()).equals(testHeader);
            } else {
                enabled = true;
            }
        }

        return Optional.of(String.valueOf(enabled));
    }

}
