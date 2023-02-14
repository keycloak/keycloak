package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.ConfigKeystoreOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ConfigKeystorePropertyMappers {

    private ConfigKeystorePropertyMappers() {
    }

    public static PropertyMapper[] getConfigKeystorePropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE)
                        .to("smallrye.config.source.keystore.kc-default.path")
                        .paramLabel("config-keystore")
                        .build(),
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE_PASSWORD)
                        .to("smallrye.config.source.keystore.kc-default.password")
                        .paramLabel("config-keystore-password")
                        .build(),
                fromOption(ConfigKeystoreOptions.CONFIG_KEYSTORE_TYPE)
                        .to("smallrye.config.source.keystore.kc-default.type")
                        .paramLabel("config-keystore-type")
                        .build()
        };
    }

}
