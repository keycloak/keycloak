package org.keycloak.quarkus.runtime.configuration.mappers;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.config.Option;
import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.smallrye.common.net.Inet;
import io.smallrye.config.ConfigSourceInterceptorContext;
import org.jboss.logging.Logger;

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
                        .transformer((value, context) -> normalizeAddresses(value))
                        .addValidateEnabled(() -> !Configuration.isBlank(ProxyOptions.PROXY_HEADERS), "proxy-headers is set")
                        .paramLabel("trusted proxies")
                        .build()
        );
    }

    private static void validateAddress(String address) {
        if (Inet.parseCidrAddress(address) != null) {
            return;
        }

        InetAddress directParse = Inet.parseInetAddress(address);
        if (directParse != null) {
            if (directParse instanceof Inet6Address && !address.startsWith("[")) {
                log.warnf("proxy-trusted-addresses value '%s' is a bare IPv6 address and was automatically "
                        + "converted to '[%s]'. Consider using square brackets for IPv6 addresses (e.g. [::1]) "
                        + "or CIDR notation (e.g. ::1/128).", address, address);
            }
            return;
        }

        String host = extractHost(address);
        if (host == null) {
            throw new PropertyException(proxyAddressError(address));
        }

        if (Inet.parseInetAddress(host) != null) {
            return;
        }

        if (!isValidHostname(host)) {
            throw new PropertyException(proxyAddressError(address));
        }

        if ("localhost".equals(host)) {
            return;
        }

        log.warnf("proxy-trusted-addresses value '%s' is a hostname. "
                + "Using hostnames is deprecated and will be removed in a future version. "
                + "The behavior of hostnames is unreliable: they are resolved to IP addresses at startup "
                + "and will not track changes to DNS records. If the hostname cannot be resolved at startup, "
                + "a DNS lookup is performed on every request, which slows down request processing. "
                + "Use an IP address or CIDR notation instead.", address);
    }

    private static String extractHost(String address) {
        int lastColon = address.lastIndexOf(':');
        int lastBracket = address.lastIndexOf(']');

        String host;
        if (lastColon == -1 || (lastBracket != -1 && lastColon < lastBracket)) {
            host = address;
        } else {
            host = address.substring(0, lastColon);
            String portStr = address.substring(lastColon + 1);
            try {
                int port = Integer.parseInt(portStr);
                if (port < 0 || port > 65535) {
                    return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        return host.isEmpty() ? null : host;
    }

    private static boolean isValidHostname(String host) {
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isEmpty() || host.length() > 253) {
            return false;
        }
        for (String label : host.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63
                    || !label.matches("[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?")) {
                return false;
            }
        }
        return true;
    }

    private static String proxyAddressError(String address) {
        return address + " is not a valid proxy address. Supported formats: "
                + "IP address (e.g. 127.0.0.1), IPv6 in square brackets (e.g. [::1]), "
                + "CIDR notation (e.g. 10.0.0.0/8), hostname (e.g. localhost), "
                + "or an IP address or hostname with a :port suffix (e.g. 127.0.0.1:8084).";
    }

    private static String normalizeAddresses(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(value.split(","))
                .map(ProxyPropertyMappers::normalizeAddress)
                .collect(Collectors.joining(","));
    }

    private static String normalizeAddress(String address) {
        String trimmed = address.trim();

        if (Inet.parseCidrAddress(trimmed) != null) {
            return trimmed;
        }

        InetAddress parsed = Inet.parseInetAddress(trimmed);
        if (parsed instanceof Inet6Address && !trimmed.startsWith("[")) {
            return "[" + trimmed + "]";
        }

        return trimmed;
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
