package org.keycloak.quarkus.runtime.configuration.mappers;


final class HostnamePropertyMappers {

    private HostnamePropertyMappers(){}

    public static PropertyMapper[] getHostnamePropertyMappers() {
        return new PropertyMapper[] {
                builder().from("hostname")
                        .to("kc.spi-hostname-default-hostname")
                        .description("Hostname for the Keycloak server.")
                        .paramLabel("hostname")
                        .build(),
                builder().from("hostname-admin")
                        .to("kc.spi-hostname-default-admin")
                        .description("Overrides the hostname for the admin console and APIs.")
                        .paramLabel("url")
                        .build(),
                builder().from("hostname-strict")
                        .to("kc.spi-hostname-default-strict")
                        .description("Disables dynamically resolving the hostname from request headers. Should always be set to true in production, unless proxy verifies the Host header.")
                        .type(Boolean.class)
                        .defaultValue(Boolean.TRUE.toString())
                        .build(),
                builder().from("hostname-strict-https")
                        .to("kc.spi-hostname-default-strict-https")
                        .description("Forces URLs to use HTTPS. Only needed if proxy does not properly set the X-Forwarded-Proto header.")
                        .hidden(true)
                        .defaultValue(Boolean.TRUE.toString())
                        .type(Boolean.class)
                        .build(),
                builder().from("hostname-strict-backchannel")
                        .to("kc.spi-hostname-default-strict-backchannel")
                        .description("By default backchannel URLs are dynamically resolved from request headers to allow internal an external applications. If all applications use the public URL this option should be enabled.")
                        .type(Boolean.class)
                        .build(),
                builder().from("hostname-path")
                        .to("kc.spi-hostname-default-path")
                        .description("This should be set if proxy uses a different context-path for Keycloak.")
                        .paramLabel("path")
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.HOSTNAME);
    }
}
