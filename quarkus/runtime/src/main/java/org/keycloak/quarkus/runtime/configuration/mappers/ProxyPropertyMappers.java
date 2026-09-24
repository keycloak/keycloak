package org.keycloak.quarkus.runtime.configuration.mappers;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.config.Option;
import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.smallrye.common.net.Inet;
import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ProxyPropertyMappers implements PropertyMapperGrouping {

    private static final Logger log = Logger.getLogger(ProxyPropertyMappers.class);

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        Option<?> syntheticOption = ProxyOptions.PROXY_HEADERS.toBuilder().synthetic().build();
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
                fromOption(syntheticOption)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(null, v, c))
                        .build(),
                fromOption(syntheticOption)
                        .to("quarkus.http.proxy.allow-forwarded")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(ProxyOptions.Headers.forwarded, v, c))
                        .build(),
                fromOption(syntheticOption)
                        .to("quarkus.http.proxy.allow-x-forwarded")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(ProxyOptions.Headers.xforwarded, v, c))
                        .build(),
                fromOption(syntheticOption)
                        .to("quarkus.http.proxy.enable-forwarded-prefix")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(ProxyOptions.Headers.xforwarded, v, c))
                        .build(),
                fromOption(syntheticOption)
                        .to("quarkus.http.proxy.enable-trusted-proxy-header")
                        .mapFrom(ProxyOptions.PROXY_HEADERS, (v, c) -> proxyEnabled(null, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_TRUSTED_ADDRESSES)
                        .to("quarkus.http.proxy.trusted-proxies")
                        .validator(ProxyPropertyMappers::validateAddress)
                        .transformer((value, context) -> normalizeToCidr(value))
                        .addValidateEnabled(() -> !Configuration.isBlank(ProxyOptions.PROXY_HEADERS), "proxy-headers is set")
                        .paramLabel("trusted proxies")
                        .build()
        );
    }

    private static void validateAddress(String address) {
        if (Inet.parseCidrAddress(address) != null) {
            return;
        }
        InetAddress parsed = Inet.parseInetAddress(address);
        if (parsed == null) {
            throw new PropertyException(address + " is not a valid IP address (IPv4 or IPv6) nor valid CIDR notation.");
        }
        String cidrSuffix = parsed instanceof Inet6Address ? "/128" : "/32";
        log.warnf("proxy-trusted-addresses value '%s' was automatically converted to '%s%s'. "
                + "Consider updating your configuration to use CIDR notation.", address, address, cidrSuffix);
    }

    private static String normalizeToCidr(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(value.split(","))
                .map(ProxyPropertyMappers::normalizeAddressToCidr)
                .collect(Collectors.joining(","));
    }

    private static String normalizeAddressToCidr(String address) {
        if (Inet.parseCidrAddress(address) != null) {
            return address;
        }
        InetAddress parsed = Inet.parseInetAddress(address);
        if (parsed == null) {
            return address;
        }
        return address + (parsed instanceof Inet6Address ? "/128" : "/32");
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
