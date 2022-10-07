package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.Optional;
import org.keycloak.config.SecurityOptions;

import io.smallrye.config.ConfigSourceInterceptorContext;

final class SecurityPropertyMappers {

    private SecurityPropertyMappers() {
    }

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                fromOption(SecurityOptions.FIPS_MODE).transformer(SecurityPropertyMappers::resolveFipsMode)
                        .paramLabel("mode")
                        .build()
        };
    }

    private static Optional<String> resolveFipsMode(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (value.isEmpty()) {
            return of(SecurityOptions.FipsMode.disabled.toString());
        }

        return of(SecurityOptions.FipsMode.valueOf(value.get()).toString());
    }

    private static Optional<String> resolveSecurityProvider(Optional<String> value,
            ConfigSourceInterceptorContext configSourceInterceptorContext) {
        SecurityOptions.FipsMode fipsMode = value.map(SecurityOptions.FipsMode::valueOf)
                .orElse(SecurityOptions.FipsMode.disabled);

        if (fipsMode.isFipsEnabled()) {
            return of("BCFIPS");
        }

        return value;
    }
}
