package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import java.util.function.BiFunction;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.Messages;

final class ProxyPropertyMappers {

    private ProxyPropertyMappers(){}

    public static PropertyMapper[] getProxyPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ProxyOptions.proxy)
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .transformer(ProxyPropertyMappers::getValidProxyModeValue)
                        .paramLabel("mode")
                        .build(),
                fromOption(ProxyOptions.proxyForwardedHost)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom("proxy")
                        .transformer(ProxyPropertyMappers::getResolveEnableForwardedHost)
                        .build()
        };
    }

    private static String getValidProxyModeValue(String mode, ConfigSourceInterceptorContext context) {
        switch (mode) {
            case "none":
                return "false";
            case "edge":
            case "reencrypt":
            case "passthrough":
                return "true";
            default:
                addInitializationException(Messages.invalidProxyMode(mode));
                return "false";
        }
    }

    private static String getResolveEnableForwardedHost(String proxy, ConfigSourceInterceptorContext context) {
        return String.valueOf(!"none".equals(proxy));
    }

}
