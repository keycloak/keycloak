package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.config.ClassLoaderOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.IgnoredArtifacts;

import java.util.Optional;

import static org.keycloak.config.ClassLoaderOptions.QUARKUS_REMOVED_ARTIFACTS_PROPERTY;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ClassLoaderPropertyMappers {

    private ClassLoaderPropertyMappers(){}

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[] {
                fromOption(ClassLoaderOptions.IGNORE_ARTIFACTS)
                        .to(QUARKUS_REMOVED_ARTIFACTS_PROPERTY)
                        .transformer(ClassLoaderPropertyMappers::resolveIgnoredArtifacts)
                        .build()
        };
    }

    private static Optional<String> resolveIgnoredArtifacts(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (Environment.isRebuildCheck() || Environment.isRebuild()) {
            return Optional.of(String.join(",", IgnoredArtifacts.getDefaultIgnoredArtifacts()));
        }

        return value;
    }
}
