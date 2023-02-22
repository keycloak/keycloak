package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import io.smallrye.config.ConfigSourceInterceptorContext;
import java.util.Optional;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesFileProfileConfigResolver;
import org.keycloak.config.ClassLoaderOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.QuarkusProfileConfigResolver;

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
            Profile profile = Profile.configure(new QuarkusProfileConfigResolver(), new PropertiesFileProfileConfigResolver());

            if (profile.getFeatures().get(Profile.Feature.FIPS)) {
                return Optional.of(
                        "org.bouncycastle:bcprov-jdk15on,org.bouncycastle:bcpkix-jdk15on,org.bouncycastle:bcutil-jdk15on,org.keycloak:keycloak-crypto-default");
            }

            return Optional.of(
                    "org.keycloak:keycloak-crypto-fips1402,org.bouncycastle:bc-fips,org.bouncycastle:bctls-fips,org.bouncycastle:bcpkix-fips");
        }

        return value;
    }
}
