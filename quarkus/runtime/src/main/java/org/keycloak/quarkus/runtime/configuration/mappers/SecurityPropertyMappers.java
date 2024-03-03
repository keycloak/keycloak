package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.Optional;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.SecurityOptions;

import io.smallrye.config.ConfigSourceInterceptorContext;

final class SecurityPropertyMappers {

    private SecurityPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[] {
                fromOption(SecurityOptions.FIPS_MODE).transformer(SecurityPropertyMappers::resolveFipsMode)
                        .paramLabel("mode")
                        .build(),
        };
    }

    private static Optional<String> resolveFipsMode(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (value.isEmpty()) {
            if (Profile.isFeatureEnabled(Feature.FIPS)) {
                return of(FipsMode.NON_STRICT.toString());
            }

            return of(FipsMode.DISABLED.toString());
        }

        return of(FipsMode.valueOfOption(value.get()).toString());
    }
}
