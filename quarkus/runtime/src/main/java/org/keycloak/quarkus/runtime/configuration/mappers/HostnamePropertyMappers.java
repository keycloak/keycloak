package org.keycloak.quarkus.runtime.configuration.mappers;


import java.util.Arrays;

final class HostnamePropertyMappers {

    private HostnamePropertyMappers(){}

    public static PropertyMapper[] getHostnamePropertyMappers() {
        return new PropertyMapper[] {
                builder().from("hostname-frontend-url")
                        .to("kc.spi.hostname.default.frontend-url")
                        .description("The URL that should be used to serve frontend requests that are usually sent through a public domain.")
                        .paramLabel("url")
                        .build(),
                builder().from("hostname-admin-url")
                        .to("kc.spi.hostname.default.admin-url")
                        .description("The URL that should be used to expose the admin endpoints and console.")
                        .paramLabel("url")
                        .build(),
                builder().from("hostname-force-backend-url-to-frontend-url")
                        .to("kc.spi.hostname.default.force-backend-url-to-frontend-url")
                        .description("Forces backend requests to go through the URL defined as the frontend-url. Defaults to false. Possible values are true or false.")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .expectedValues(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()))
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.HOSTNAME);
    }
}
