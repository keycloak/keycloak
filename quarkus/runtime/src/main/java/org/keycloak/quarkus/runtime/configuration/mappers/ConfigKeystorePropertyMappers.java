package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.ConfigKeystoreOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ConfigKeystorePropertyMappers {
    private static final String SMALLRYE_KEYSTORE_PATH = "smallrye.config.source.keystore.kc-default.path";
    private static final String SMALLRYE_KEYSTORE_PASSWORD = "smallrye.config.source.keystore.kc-default.password";


    private ConfigKeystorePropertyMappers() {
    }

    public static PropertyMapper[] getConfigKeystorePropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE)
                        .to(SMALLRYE_KEYSTORE_PATH)
                        .transformer(ConfigKeystorePropertyMappers::validatePath)
                        .paramLabel("config-keystore")
                        .build(),
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE_PASSWORD)
                        .to(SMALLRYE_KEYSTORE_PASSWORD)
                        .transformer(ConfigKeystorePropertyMappers::validatePassword)
                        .paramLabel("config-keystore-password")
                        .build(),
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE_TYPE)
                        .to("smallrye.config.source.keystore.kc-default.type")
                        .paramLabel("config-keystore-type")
                        .build()
        };
    }

    private static Optional<String> validatePath(Optional<String> option, ConfigSourceInterceptorContext context) {
        ConfigValue path = context.proceed(SMALLRYE_KEYSTORE_PATH);
        boolean isPasswordDefined = context.proceed(SMALLRYE_KEYSTORE_PASSWORD) != null;

        if (path == null) {
            throw new IllegalArgumentException("config-keystore must be specified");
        }

        if (!isPasswordDefined) {
            throw new IllegalArgumentException("config-keystore-password must be specified");
        }

        final Path realPath = Path.of(path.getValue()).toAbsolutePath().normalize();
        if (!Files.exists(realPath)) {
            throw new IllegalArgumentException("config-keystore path does not exist: " + realPath);
        }

        return Optional.of(realPath.toUri().toString());
    }

    private static Optional<String> validatePassword(Optional<String> option, ConfigSourceInterceptorContext context) {
        boolean isPasswordDefined = context.proceed(SMALLRYE_KEYSTORE_PASSWORD).getValue() != null;
        boolean isPathDefined = context.proceed(SMALLRYE_KEYSTORE_PATH) != null;

        if (!isPasswordDefined) {
            throw new IllegalArgumentException("config-keystore-password must be specified");
        }

        if (!isPathDefined) {
            throw new IllegalArgumentException("config-keystore must be specified");
        }

        return option;
    }

}
