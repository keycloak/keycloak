package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import io.smallrye.config.ConfigSourceInterceptorContext;
import java.util.Optional;

import org.keycloak.config.ClassLoaderOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.IgnoredArtifacts;

final class ClassLoaderPropertyMappers {

    private ClassLoaderPropertyMappers(){}

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                fromOption(ClassLoaderOptions.IGNORE_ARTIFACTS)
                        .to("quarkus.class-loading.removed-artifacts")
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
