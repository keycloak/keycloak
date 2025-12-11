package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.ClassLoaderOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.IgnoredArtifacts;

import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.config.ClassLoaderOptions.QUARKUS_REMOVED_ARTIFACTS_PROPERTY;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ClassLoaderPropertyMappers implements PropertyMapperGrouping {

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(ClassLoaderOptions.IGNORE_ARTIFACTS)
                        .to(QUARKUS_REMOVED_ARTIFACTS_PROPERTY)
                        .transformer(ClassLoaderPropertyMappers::resolveIgnoredArtifacts)
                        .build()
        );
    }

    private static String resolveIgnoredArtifacts(String value, ConfigSourceInterceptorContext context) {
        if (Environment.isRebuildCheck() || Environment.isRebuild()) {
            return String.join(",", IgnoredArtifacts.getDefaultIgnoredArtifacts());
        }

        return value;
    }
}
