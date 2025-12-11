package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.VaultOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class VaultPropertyMappers implements PropertyMapperGrouping {

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(VaultOptions.VAULT)
                        .paramLabel("provider")
                        .build(),
                fromOption(VaultOptions.VAULT_DIR)
                        .to("kc.spi-vault--file--dir")
                        .paramLabel("dir")
                        .build(),
                fromOption(VaultOptions.VAULT_FILE)
                        .to("kc.spi-vault--keystore--file")
                        .paramLabel("file")
                        .build(),
                fromOption(VaultOptions.VAULT_PASS)
                        .to("kc.spi-vault--keystore--pass")
                        .paramLabel("pass")
                        .build(),
                fromOption(VaultOptions.VAULT_TYPE)
                        .to("kc.spi-vault--keystore--type")
                        .paramLabel("type")
                        .build()
        );
    }

}
