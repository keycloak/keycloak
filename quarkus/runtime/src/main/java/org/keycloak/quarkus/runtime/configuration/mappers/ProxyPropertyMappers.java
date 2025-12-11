package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.smallrye.common.net.Inet;
import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ProxyPropertyMappers implements PropertyMapperGrouping{

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(ProxyOptions.PROXY_HEADERS)
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .transformer((v, c) -> proxyEnabled(null, v, c))
                        .paramLabel("headers")
                        .build(),
                fromOption(ProxyOptions.PROXY_PROTOCOL_ENABLED)
                        .to("quarkus.http.proxy.use-proxy-protocol")
                        .validator(v -> {
                            if (Boolean.parseBoolean(v) && Configuration.getOptionalKcValue(ProxyOptions.PROXY_HEADERS).isPresent()) {
                                throw new PropertyException("proxy protocol cannot be enabled when using the `proxy-headers` option");
                            }
                        })
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HOST)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(null, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-forwarded")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(ProxyOptions.Headers.forwarded, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_X_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-x-forwarded")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(ProxyOptions.Headers.xforwarded, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_TRUSTED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.enable-trusted-proxy-header")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(null, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_TRUSTED_ADDRESSES)
                        .to("quarkus.http.proxy.trusted-proxies")
                        .validator(ProxyPropertyMappers::validateAddress)
                        .addValidateEnabled(() -> !Configuration.isBlank(ProxyOptions.PROXY_HEADERS), "proxy-headers is set")
                        .paramLabel("trusted proxies")
                        .build()
        );
    }

    private static void validateAddress(String address) {
        if (Inet.parseCidrAddress(address) != null) {
            return;
        }
        if (Inet.parseInetAddress(address) == null) {
            throw new PropertyException(address + " is not a valid IP address (IPv4 or IPv6) nor valid CIDR notation.");
        }
    }

    private static String proxyEnabled(ProxyOptions.Headers testHeader, String value, ConfigSourceInterceptorContext context) {
        boolean enabled = false;

        if (value != null) { // proxy-headers explicitly configured
            if (testHeader != null) {
                enabled = ProxyOptions.Headers.valueOf(value).equals(testHeader);
            } else {
                enabled = true;
            }
        }

        return String.valueOf(enabled);
    }

}
