package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.common.Profile;
import org.keycloak.config.HostnameV1Options;

import java.util.List;
import java.util.stream.Stream;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class HostnameV1PropertyMappers {

    private HostnameV1PropertyMappers(){}

    public static PropertyMapper<?>[] getHostnamePropertyMappers() {
        return Stream.of(
                fromOption(HostnameV1Options.HOSTNAME)
                        .to("kc.spi-hostname-default-hostname")
                        .paramLabel("hostname"),
                fromOption(HostnameV1Options.HOSTNAME_URL)
                        .to("kc.spi-hostname-default-hostname-url")
                        .paramLabel("url"),
                fromOption(HostnameV1Options.HOSTNAME_ADMIN)
                        .to("kc.spi-hostname-default-admin")
                        .paramLabel("hostname"),
                fromOption(HostnameV1Options.HOSTNAME_ADMIN_URL)
                        .to("kc.spi-hostname-default-admin-url")
                        .paramLabel("url"),
                fromOption(HostnameV1Options.HOSTNAME_STRICT)
                        .to("kc.spi-hostname-default-strict"),
                fromOption(HostnameV1Options.HOSTNAME_STRICT_HTTPS)
                        .to("kc.spi-hostname-default-strict-https"),
                fromOption(HostnameV1Options.HOSTNAME_STRICT_BACKCHANNEL)
                        .to("kc.spi-hostname-default-strict-backchannel"),
                fromOption(HostnameV1Options.HOSTNAME_PATH)
                        .to("kc.spi-hostname-default-path")
                        .paramLabel("path"),
                fromOption(HostnameV1Options.HOSTNAME_PORT)
                        .to("kc.spi-hostname-default-hostname-port")
                        .paramLabel("port"),
                fromOption(HostnameV1Options.HOSTNAME_DEBUG)
                        .to("kc.spi-hostname-default-hostname-debug")
        )
        .map(b -> b.isEnabled(() -> Profile.isFeatureEnabled(Profile.Feature.HOSTNAME_V1), "hostname:v1 feature is enabled").build())
        .toArray(s -> new PropertyMapper<?>[s]);
    }

}
