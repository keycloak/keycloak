package org.keycloak.quarkus.runtime.configuration.mappers;


import com.google.common.net.InternetDomainName;
import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.quarkus.runtime.Messages;

import java.util.Locale;

import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

final class HostnamePropertyMappers {

    private HostnamePropertyMappers(){}

    public static PropertyMapper[] getHostnamePropertyMappers() {
        return new PropertyMapper[] {
                builder().from("hostname")
                        .to("kc.spi-hostname-default-hostname")
                        .description("Hostname for the Keycloak server.")
                        .paramLabel("hostname")
                        .transformer(HostnamePropertyMappers::getValidatedLowercaseHostname)
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
                        .build(),
                builder().from("hostname-port")
                        .to("kc.spi-hostname-default-hostname-port")
                        .defaultValue("-1")
                        .description("The port used by the proxy when exposing the hostname. Set this option if the proxy uses a port other than the default HTTP and HTTPS ports.")
                        .paramLabel("port")
                        .build()
        };
    }

    private static String getValidatedLowercaseHostname(String value, ConfigSourceInterceptorContext configSourceInterceptorContext) {

        String hostnameVal = value.toLowerCase(Locale.ENGLISH);

        if (!InternetDomainName.isValid(hostnameVal)) {
            addInitializationException(Messages.hostnameNotValid(hostnameVal));
        }

        return hostnameVal;
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.HOSTNAME);
    }
}
