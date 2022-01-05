package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import java.util.Arrays;
import java.util.function.BiFunction;

import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

import org.keycloak.quarkus.runtime.Messages;

final class ProxyPropertyMappers {

    private static final String[] possibleProxyValues = {"none", "edge", "reencrypt", "passthrough"};

    private ProxyPropertyMappers(){}

    public static PropertyMapper[] getProxyPropertyMappers() {
        return new PropertyMapper[] {
                builder().from("proxy")
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .defaultValue("none")
                        .transformer(getValidProxyModeValue())
                        .expectedValues(Arrays.asList(possibleProxyValues))
                        .description("The proxy address forwarding mode if the server is behind a reverse proxy. " +
                                "Possible values are: " + String.join(",",possibleProxyValues))
                        .paramLabel("mode")
                        .category(ConfigCategory.PROXY)
                        .build()
        };
    }

    private static BiFunction<String, ConfigSourceInterceptorContext, String> getValidProxyModeValue() {
        return (mode, context) -> {
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
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.PROXY);
    }
}
