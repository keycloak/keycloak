package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.HostnameOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class HostnamePropertyMappers {

    private HostnamePropertyMappers(){}

    public static PropertyMapper[] getHostnamePropertyMappers() {
        return new PropertyMapper[] {
                fromOption(HostnameOptions.HOSTNAME)
                        .to("kc.spi-hostname-default-hostname")
                        .paramLabel("hostname")
                        .build(),
                fromOption(HostnameOptions.HOSTNAME_ADMIN)
                        .to("kc.spi-hostname-default-admin")
                        .paramLabel("hostname")
                        .build(),
                fromOption(HostnameOptions.HOSTNAME_STRICT)
                        .to("kc.spi-hostname-default-strict")
                        .build(),
                fromOption(HostnameOptions.HOSTNAME_STRICT_HTTPS)
                        .to("kc.spi-hostname-default-strict-https")
                        .build(),
                fromOption(HostnameOptions.HOSTNAME_STRICT_BACKCHANNEL)
                        .to("kc.spi-hostname-default-strict-backchannel")
                        .build(),
                fromOption(HostnameOptions.HOSTNAME_PATH)
                        .to("kc.spi-hostname-default-path")
                        .paramLabel("path")
                        .build(),
                fromOption(HostnameOptions.HOSTNAME_PORT)
                        .to("kc.spi-hostname-default-hostname-port")
                        .paramLabel("port")
                        .build()
        };
    }

}
