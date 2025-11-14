package org.keycloak.quarkus.runtime.configuration.mappers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.keycloak.config.ConfigKeystoreOptions;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class ConfigKeystorePropertyMappers implements PropertyMapperGrouping {
    private static final String SMALLRYE_KEYSTORE_PATH = "smallrye.config.source.keystore.kc-default.path";
    private static final String SMALLRYE_KEYSTORE_PASSWORD = "smallrye.config.source.keystore.kc-default.password";


    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE)
                        .to(SMALLRYE_KEYSTORE_PATH)
                        .transformer(ConfigKeystorePropertyMappers::validatePath)
                        .paramLabel("config-keystore")
                        .build(),
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE_PASSWORD)
                        .to(SMALLRYE_KEYSTORE_PASSWORD)
                        .transformer(ConfigKeystorePropertyMappers::validatePassword)
                        .paramLabel("config-keystore-password")
                        .isMasked(true)
                        .build(),
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE_TYPE)
                        .to("smallrye.config.source.keystore.kc-default.type")
                        .paramLabel("config-keystore-type")
                        .build()
        );
    }

    private static String validatePath(String option, ConfigSourceInterceptorContext context) {
        if (option == null) {
            return null;
        }
        boolean isPasswordDefined = context.proceed(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + ConfigKeystoreOptions.CONFIG_KEYSTORE_PASSWORD.getKey()) != null;

        if (!isPasswordDefined) {
            throw new IllegalArgumentException("config-keystore-password must be specified");
        }

        final Path realPath = Path.of(option).toAbsolutePath().normalize();
        if (!Files.exists(realPath)) {
            throw new IllegalArgumentException("config-keystore path does not exist: " + realPath);
        }

        return realPath.toUri().toString();
    }

    private static String validatePassword(String option, ConfigSourceInterceptorContext context) {
        if (option == null) {
            return null;
        }
        boolean isPathDefined = context.proceed(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + ConfigKeystoreOptions.CONFIG_KEYSTORE.getKey()) != null;

        if (!isPathDefined) {
            throw new IllegalArgumentException("config-keystore must be specified");
        }

        return option;
    }

}
