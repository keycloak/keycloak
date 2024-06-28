package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.ProxyOptions;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ProxyPropertyMappers {

    private ProxyPropertyMappers(){}

    public static PropertyMapper<?>[] getProxyPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ProxyOptions.PROXY_HEADERS)
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .transformer((v, c) -> proxyEnabled(null, v, c))
                        .paramLabel("headers")
                        .build(),
                fromOption(ProxyOptions.PROXY)
                        .paramLabel("mode")
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HOST)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom("proxy-headers")
                        .transformer((v, c) -> proxyEnabled(null, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-forwarded")
                        .mapFrom("proxy-headers")
                        .transformer((v, c) -> proxyEnabled(ProxyOptions.Headers.forwarded, v, c))
                        .build(),
                fromOption(ProxyOptions.PROXY_X_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-x-forwarded")
                        .mapFrom("proxy-headers")
                        .transformer((v, c) -> proxyEnabled(ProxyOptions.Headers.xforwarded, v, c))
                        .build()
        };
    }

    private static Optional<String> proxyEnabled(ProxyOptions.Headers testHeader, Optional<String> value, ConfigSourceInterceptorContext context) {
        boolean enabled;

        if (value.isPresent()) { // proxy-headers explicitly configured
            if (testHeader != null) {
                enabled = ProxyOptions.Headers.valueOf(value.get()).equals(testHeader);
            } else {
                enabled = true;
            }
        } else { // fallback to the deprecated proxy option
            String proxyKey = NS_KEYCLOAK_PREFIX + ProxyOptions.PROXY.getKey();
            ConfigValue proxyOptionConfigValue = context.proceed(proxyKey);

            ProxyOptions.Mode proxyMode;
            if (proxyOptionConfigValue == null) { // neither proxy-headers nor proxy options are configured, falling back to default proxy value which is "none"
                proxyMode = (ProxyOptions.Mode) PropertyMappers.getMapper(proxyKey).getDefaultValue().orElseThrow();
            } else {
                proxyMode = ProxyOptions.Mode.valueOf(proxyOptionConfigValue.getValue());
            }

            enabled = proxyMode.isProxyHeadersEnabled();
        }

        return Optional.of(String.valueOf(enabled));
    }

}
