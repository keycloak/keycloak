package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.ConfigKeystoreOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ConfigKeystorePropertyMappers {

    private ConfigKeystorePropertyMappers() {
    }

    public static PropertyMapper[] getConfigKeystorePropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE)
                        .to("smallrye.config.source.keystore.kc-default.path")
                        .transformer(ConfigKeystorePropertyMappers::validatePath)
                        .paramLabel("config-keystore")
                        .build(),
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE_PASSWORD)
                        .to("smallrye.config.source.keystore.kc-default.password")
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
        ConfigValue path = context.proceed("smallrye.config.source.keystore.kc-default.path");

        if (path == null) {
            throw new IllegalArgumentException("config-keystore must be specified");
        }

        Optional<String> realPath = Optional.of(String.valueOf(Paths.get(path.getValue()).toAbsolutePath().normalize()));
        if (!Files.exists(Path.of(realPath.get()))) {
            throw new IllegalArgumentException("config-keystore path does not exist: " + realPath.get());
        }
        return realPath;
    }

    private static Optional<String> validatePassword(Optional<String> option, ConfigSourceInterceptorContext context) {
        ConfigValue password = context.proceed("smallrye.config.source.keystore.kc-default.password");

        if (password == null) {
            throw new IllegalArgumentException("config-keystore-password must be specified");
        }
        return option;
    }

}
