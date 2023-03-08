package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.VaultOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class VaultPlainTextPropertyMappers {

    private VaultPlainTextPropertyMappers() {
    }

    public static PropertyMapper[] getVaultPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(VaultOptions.VAULT)
                        .paramLabel("provider")
                        .build(),
                fromOption(VaultOptions.VAULT_DIR)
                        .to("kc.spi-vault-file-dir")
                        .paramLabel("dir")
                        .build()
        };
    }

}
