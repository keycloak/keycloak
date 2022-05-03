package org.keycloak.quarkus.runtime.configuration.mappers;


import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.Messages;

import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

final class HostnamePropertyMappers {

    private HostnamePropertyMappers(){}

    public static PropertyMapper[] getHostnamePropertyMappers() {
        return new PropertyMapper[] {
                builder().from("hostname")
                        .to("kc.spi-hostname-default-hostname")
                        .description("Hostname for the Keycloak server.")
                        .paramLabel("hostname")
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
                        .transformer(HostnamePropertyMappers::setStrictHttpsConditionally)
                        .type(Boolean.class)
                        .build(),
                builder().from("hostname-strict-backchannel")
                        .to("kc.spi-hostname-default-strict-backchannel")
                        .description("By default backchannel URLs are dynamically resolved from request headers to allow internal and external applications. If all applications use the public URL this option should be enabled.")
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

    private static String setStrictHttpsConditionally(String value, ConfigSourceInterceptorContext context) {
        ConfigValue proxyConfig = context.proceed("kc.proxy");
        ConfigValue httpEnabledConfig = context.proceed("kc.http-enabled");

        //no matter what other conditions, when proxy set to edge set strict-https to false
        // bc tls terminated at proxy level and http requests are ok
        if (isProxyEdge(proxyConfig)) {
            value = Boolean.FALSE.toString();
            return value;
        }

        //when start and http-enabled = true and proxy = none,passthrough or reencrypt and no TLS setup found throw exception bc invalid
        if (Environment.isProdMode()
                && isHttpEnabled(httpEnabledConfig)
                && isProxyNotEdge(proxyConfig)) {

            if(!HttpPropertyMappers.isTlsConfigured(context)) {
                addInitializationException(Messages.httpsConfigurationNotSet(proxyConfig == null ? "none" : proxyConfig.getValue()));
                return value;
            }
            //when start and http-enabled and proxy = none, but TLS setup found, set false and add warning
            Environment.setInsecureInProdMode(true);
        }

        //for other modes, just return true.
        return value;
    }

    private static boolean isHttpEnabled(ConfigValue httpEnabledConfig) {
        return httpEnabledConfig.getValue().equals("true");
    }

    private static boolean isProxyNotEdge(ConfigValue proxyConfig) {
        if (proxyConfig == null) {
            return true;
        }

        return !proxyConfig.getValue().equals("edge");
    }

    private static boolean isProxyEdge(ConfigValue proxyConfig) {
        if(proxyConfig == null) {
            return false;
        }
        return proxyConfig.getValue().equals("edge");
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.HOSTNAME);
    }
}
