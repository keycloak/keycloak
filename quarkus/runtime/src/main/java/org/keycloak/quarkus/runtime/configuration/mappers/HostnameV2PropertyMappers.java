package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.config.HostnameV2Options;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public final class HostnameV2PropertyMappers {

    private static final Logger LOGGER = Logger.getLogger(PropertyMappers.class);
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

    public static void validateConfig() {
        List<String> inUse = REMOVED_OPTIONS.stream().filter(s -> Configuration.getOptionalKcValue(s).isPresent()).toList();

        if (!inUse.isEmpty()) {
            LOGGER.errorf("Hostname v1 options %s are still in use, please review your configuration", inUse);
        }

        String host = Configuration.getConfigValue(HttpOptions.HTTP_HOST).getValue();
        String proxyHeaders = Configuration.getConfigValue(ProxyOptions.PROXY_HEADERS).getValue();
        if (host == null) {
            // hostname-strict=false, generally proxy-headers should be set, but it's not required
            return;
        }
        if (proxyHeaders != null) {
            // must not be tls passthrough, but there's no way of knowing the intention
            return;
        }
        if (HttpPropertyMappers.isHttpsEnabled()) {
            // must be tls passthrough, but there's no way of knowing the intention
            return;
        }
        try {
            new URL(host);
            return;
        } catch (MalformedURLException e) {

        }
        // https is not enabled, and we're using just a hostname
        // WARNING in dev?
        // ERROR in prod? - cors check, nor secure context detection will work as expected unless hostname is localhost
    }

}
