package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.Optional;

import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.ClassLoaderOptions;
import org.keycloak.config.SecurityOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

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
            ConfigValue fipsEnabled = Configuration.getConfigValue(
                    MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + SecurityOptions.FIPS_MODE.getKey());

            if (fipsEnabled != null && FipsMode.valueOf(fipsEnabled.getValue()).isFipsEnabled()) {
                return Optional.of(
                        "org.bouncycastle:bcprov-jdk15on,org.bouncycastle:bcpkix-jdk15on,org.bouncycastle:bcutil-jdk15on,org.keycloak:keycloak-crypto-default");
            }

            return Optional.of(
                    "org.keycloak:keycloak-crypto-fips1402,org.bouncycastle:bc-fips,org.bouncycastle:bctls-fips,org.bouncycastle:bcpkix-fips");
        }

        return value;
    }
}
