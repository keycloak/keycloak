package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.config.HostnameV2Options;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.SecureContextResolver;

public final class HostnameV2PropertyMappers {

    private static final List<String> REMOVED_OPTIONS = Arrays.asList("hostname-admin-url", "hostname-path", "hostname-port", "hostname-strict-backchannel", "hostname-url", "proxy", "hostname-strict-https");

    private HostnameV2PropertyMappers(){}

    public static PropertyMapper<?>[] getHostnamePropertyMappers() {
        return Stream.of(
                fromOption(HostnameV2Options.HOSTNAME)
                        .to("kc.spi-hostname-v2-hostname")
                        .paramLabel("hostname|URL"),
                fromOption(HostnameV2Options.HOSTNAME_ADMIN)
                        .to("kc.spi-hostname-v2-hostname-admin")
                        .paramLabel("URL"),
                fromOption(HostnameV2Options.HOSTNAME_BACKCHANNEL_DYNAMIC)
                        .to("kc.spi-hostname-v2-hostname-backchannel-dynamic"),
                fromOption(HostnameV2Options.HOSTNAME_STRICT)
                        .to("kc.spi-hostname-v2-hostname-strict"),
                fromOption(HostnameV2Options.HOSTNAME_DEBUG)
        )
        .map(b -> b.isEnabled(() -> Profile.isFeatureEnabled(Profile.Feature.HOSTNAME_V2), "hostname:v2 feature is enabled").build())
        .toArray(s -> new PropertyMapper<?>[s]);
    }

    public static void validateConfig(Picocli picocli) {
        List<String> inUse = REMOVED_OPTIONS.stream().filter(s -> Configuration.getOptionalKcValue(s).isPresent()).toList();

        if (!inUse.isEmpty()) {
            picocli.warn("Hostname v1 options %s are still in use, please review your configuration".formatted(inUse));
        }
        boolean httpsEnabled = HttpPropertyMappers.isHttpsEnabled();
        String host = Configuration.getConfigValue(HostnameV2Options.HOSTNAME).getValue();
        if (host != null && validateFullHostname(httpsEnabled, host, picocli)) {
            return;
        }
        if (httpsEnabled) {
            // must be tls passthrough, but there's no way of knowing the intention
            return;
        }
        String proxyHeaders = Configuration.getConfigValue(ProxyOptions.PROXY_HEADERS).getValue();
        if (proxyHeaders != null) {
            // must not be tls passthrough, but there's no way of knowing the intention
            return;
        }
        if (host == null) {
            String message = "With HTTPS not enabled and hostname-strict=false, cross-origin cookies will not be allowed.";
            if (Environment.isDevProfile()) {
                picocli.info(message);
            } else {
                picocli.warn(message);
            }
        } else if (!SecureContextResolver.isLocal(host)) {
            picocli.warn("HTTPS is not enabled on the server, either the `hostname` should be set to a full URL or `proxy-headers` should be used. This is likely a misconfiguration.");
        } // else warn on prod
    }

    static boolean validateFullHostname(boolean httpsEnabled, String host, Picocli picocli) {
        try {
            URL url = new URL(host);

            if (!url.getProtocol().toUpperCase().equals("HTTPS")) {
                if (!SecureContextResolver.isLocal(url.getHost())) {
                    picocli.warn("`hostname` is set to an HTTP hostname, cross-origin cookies will not be allowed. This is likely a misconfiguration.");

                    // TODO: any hostname-admin specific validation?

                } // else warn on prod?
                if (httpsEnabled) {
                    picocli.warn("HTTPS is enabled on the server, but `hostname` specifies HTTP. This is likely a misconfiguration.");
                }
            }

            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

}
